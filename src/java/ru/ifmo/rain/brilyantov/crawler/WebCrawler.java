package ru.ifmo.rain.brilyantov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.function.Predicate;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;
    private final Map<String, TaskPoolPerHost> tasksPerHost = new ConcurrentHashMap<>();

    private TaskPoolPerHost getTaskPoolForHost(String hostname) {
        synchronized (tasksPerHost) {
            tasksPerHost.putIfAbsent(hostname, new TaskPoolPerHost());
            return tasksPerHost.get(hostname);
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    public Result download(String url, int depth, Predicate<String> filter) {
        ResultWrapper resultWrapper = new ResultWrapper();
        Phaser phaser = new Phaser(1);
        downloadImplDfs(url, depth, resultWrapper, phaser, filter);
        phaser.arriveAndAwaitAdvance();
        return resultWrapper.toResult();
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, site -> true);
    }

    private static class ResultWrapper {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();

        Result toResult() {
            downloaded.removeAll(errors.keySet());
            return new Result(new ArrayList<>(downloaded), new HashMap<>(errors));
        }
    }

    private void downloadImplDfs(
            String url,
            int depth,
            ResultWrapper resultWrapper,
            Phaser phaser,
            Predicate<String> filter
    ) {
        if (depth > 0 && filter.test(url) && resultWrapper.downloaded.add(url)) {
            submitToDownloaderWithHostBarier(url, resultWrapper, phaser, () -> {
                try {
                    Document dock = downloader.download(url);
                    phaser.register();
                    extractorsPool.submit(() -> {
                        try {
                            dock
                                    .extractLinks()
                                    .forEach(link -> downloadImplDfs(
                                            link,
                                            depth - 1,
                                            resultWrapper,
                                            phaser,
                                            filter
                                    ));
                        } catch (IOException e) {
                            resultWrapper.errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (IOException e) {
                    resultWrapper.errors.put(url, e);
                }
            });
        }
    }

    private void submitToDownloaderWithHostBarier(
            String url,
            ResultWrapper resultWrapper,
            Phaser phaser,
            Runnable task
    ) {
        try {
            String host = URLUtils.getHost(url);
            phaser.register();
            TaskPoolPerHost taskPoolPerHost = getTaskPoolForHost(host);
            taskPoolPerHost.submitToDownloaderWithHostBarierImpl(phaser, task, this);
        } catch (MalformedURLException e) {
            resultWrapper.errors.put(url, e);
        }
    }

    private Runnable transformedTask(TaskPoolPerHost taskPoolPerHost, Runnable task, Phaser phaser) {
        return () -> {
            task.run();
            synchronized (taskPoolPerHost.suspendedTasks) {
                if (!taskPoolPerHost.suspendedTasks.isEmpty()) {
                    downloadersPool.submit(transformedTask(
                            taskPoolPerHost,
                            taskPoolPerHost.suspendedTasks.poll(),
                            phaser
                    ));
                } else {
                    taskPoolPerHost.threadCount--;
                }
            }
            phaser.arrive();
        };
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
    }

    private static class TaskPoolPerHost {
        int threadCount = 0;
        int threadLimit;
        final Queue<Runnable> suspendedTasks = new ArrayDeque<>();

        private void submitToDownloaderWithHostBarierImpl(
                Phaser phaser,
                Runnable task,
                WebCrawler webCrawler
        ) {
            synchronized (suspendedTasks) {
                if (threadCount < webCrawler.perHost) {
                    threadCount++;
                    webCrawler.downloadersPool.submit(webCrawler.transformedTask(this, task, phaser));
                } else {
                    suspendedTasks.add(task);
                }
            }
        }
    }

}