package ru.ifmo.ctddev.kgeorgiy.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.*;

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
        semaphorePerHost.putIfAbsent(host, new Semaphore(perHost));
        return semaphorePerHost.get(host);
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
        System.out.println("url:" + url);
        if (depth > 0 && resultWrapper.downloaded.add(url)) {
            submitToDownloaderWithHostBarier(url, resultWrapper, phaser, () -> {
                try {
                     System.out.println("5_url:" + url);
                    Document dock = downloader.download(url);
                     System.out.println("6_url:" + url);
                    phaser.register();
                     System.out.println("7_url:" + url);
                    extractorsPool.submit(() -> {
                        System.out.println("8_url:" + url);
                        try {
                            dock
                            .extractLinks()
                            .forEach(link -> {
                                System.out.println("link:" + link);
                                downloadImplDfs(link, depth - 1, resultWrapper, phaser)
                            });
                        } catch (IOException e) {
                            System.out.println("e_url:" + url);
                            resultWrapper.errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (IOException e) {
                    System.out.println("e_url:" + url);
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
            System.out.println("host:" + host);
            phaser.register();
            Semaphore semaphore = addSemaphoreIfNeeded(host);
             System.out.println("1_url:" + url);
            downloadersPool.submit(() -> {
                try {
                    System.out.println("2_url:" + url);
                    semaphore.acquire();
                    System.out.println("3_url:" + url);
                    task.run();
                    System.out.println("4_url:" + url);
                } catch (InterruptedException ignored) {
                    System.out.println("e_url:" + url);
                } finally {
                    semaphore.release();
                }
            });
        } catch (MalformedURLException e) {
            System.out.println("e_url:" + url);
            resultWrapper.errors.put(url, e);
        }
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
    }

}
