package ru.ifmo.ctddev.kgeorgiy.crawler;

import com.sun.javafx.binding.StringFormatter;
import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;
    private final Map<String, TaskPoolPerHost> tasksPerHost = new ConcurrentHashMap<>();

    private TaskPoolPerHost getTaskPoolForHost(String hostname) {
        synchronized (tasksPerHost) {
            if (!tasksPerHost.containsKey(hostname)) {
                tasksPerHost.put(hostname, new TaskPoolPerHost());
            }
            return tasksPerHost.get(hostname);
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    @Override
    public Result download(String url, int depth) {
        ResultWrapper resultWrapper = new ResultWrapper();
        Phaser phaser = new Phaser(1);
        downloadImplDfs(url, depth, resultWrapper, phaser);
        phaser.arriveAndAwaitAdvance();
        return resultWrapper.toResult();
    }

    private static class ResultWrapper {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();

        Result toResult() {
            downloaded.removeAll(errors.keySet());
            return new Result(new ArrayList<>(downloaded), new HashMap<>(errors));
        }
    }

    private void downloadImplDfs(String url, int depth, ResultWrapper resultWrapper, Phaser phaser) {
        if (depth > 0 && resultWrapper.downloaded.add(url)) {
            submitToDownloaderWithHostBarier(url, resultWrapper, phaser, () -> {
                try {
                    Document dock = downloader.download(url);
                    phaser.register();
                    extractorsPool.submit(() -> {
                        try {
                            dock
                                    .extractLinks()
                                    .forEach(link -> downloadImplDfs(link, depth - 1, resultWrapper, phaser));
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
            submitToDownloaderWithHostBarierImpl(taskPoolPerHost, resultWrapper, phaser, task);
        } catch (MalformedURLException e) {
            resultWrapper.errors.put(url, e);
        }
    }

    private Random rnd = new Random();

    private int getId() {
        return Math.abs(rnd.nextInt()) % 100;
    }

    private void submitToDownloaderWithHostBarierImpl(
            TaskPoolPerHost taskPoolPerHost,
            ResultWrapper resultWrapper,
            Phaser phaser,
            Runnable task
    ) {
        int _id_ = getId();
        synchronized (taskPoolPerHost.suspendedTasks) {
            if (taskPoolPerHost.threadCount < perHost) {
                taskPoolPerHost.threadCount++;
                downloadersPool.submit(transformedTask(taskPoolPerHost, task, phaser));
            } else {
                taskPoolPerHost.suspendedTasks.add(task);
                phaser.arrive();
            }
        }
    }

    private Runnable transformedTask(TaskPoolPerHost taskPoolPerHost, Runnable task, Phaser phaser) {
        return () -> {
            task.run();
            synchronized (taskPoolPerHost.suspendedTasks) {
                if (!taskPoolPerHost.suspendedTasks.isEmpty()) {
                    phaser.register();
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

    private class TaskPoolPerHost {
        int threadCount = 0;
        final Queue<Runnable> suspendedTasks = new ArrayDeque<>();
    }

}
