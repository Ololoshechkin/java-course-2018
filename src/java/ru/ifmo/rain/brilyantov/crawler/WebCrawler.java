package ru.ifmo.rain.brilyantov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;
    private final Map<String, Semaphore> semaphorePerHost = new ConcurrentHashMap<>();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private Semaphore addSemaphoreIfNeeded(String host) {
        Semaphore result = new Semaphore(perHost);
        semaphorePerHost.putIfAbsent(host, result);
        return result;
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
                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    private void submitToDownloaderWithHostBarier(String url, ResultWrapper resultWrapper, Phaser phaser, Runnable task) {
        try {
            String host = URLUtils.getHost(url);
            phaser.register();
            Semaphore semaphore = addSemaphoreIfNeeded(host);
            try {
                semaphore.acquire();
                downloadersPool.submit(task);
            } catch (InterruptedException ignored) {
            } finally {
                semaphore.release();
            }
        } catch (MalformedURLException e) {
            resultWrapper.errors.put(url, e);
        }
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
    }
}
