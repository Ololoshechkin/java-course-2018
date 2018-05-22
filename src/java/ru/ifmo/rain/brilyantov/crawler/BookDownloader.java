package ru.ifmo.rain.brilyantov.crawler;


import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.ifmo.rain.brilyantov.implementor.Implementor.clearDirs;

public class BookDownloader {

    private static final int DOWNLOADERS_COUNT = 4;
    private static final int EXTRACTORS_COUNT = 4;
    private static final int CONNECTIONS_PER_HOST = 4;

    private final static String BOOK_SITE_MAIN = "https://e.lanbook.com/books";
    private final static String BOOK_URL = "https://e.lanbook.com/book/";
    private final static int CHAPTERS[] = {917, 918, 1537};
    private static final String YEAR_PREFIX = "<dt>Год:</dt>";
    private static final String BIBLIOGRAPHIC_RECORD_PREFIX = "<div id=\"bibliographic_record\">";
    private static final String BIBLIOGRAPHIC_RECORD_SUFIX = "</div>";
    private static final String HEADER_TEMPLATE = "https://e.lanbook.com/books/%d";
    private static final String PAGE_TEMPLATE = "https://e.lanbook.com/books/%d?page=";

    private static int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR);
    private static int MIN_YEAR = MAX_YEAR - 4;

    private static final Pattern infoPattern = Pattern.compile(
            BIBLIOGRAPHIC_RECORD_PREFIX + "[^<]*" + BIBLIOGRAPHIC_RECORD_SUFIX
    );
    private static final Pattern yearPattern = Pattern.compile(
            YEAR_PREFIX + "\\s*<dd>\\d*</dd>"
    );

    private static class DownloaderException extends Exception {
        DownloaderException(String message) {
            super(message);
        }
    }

    private static void tryDeleteTempFile(Path tempFile) throws DownloaderException {
        try {
            clearDirs(tempFile);
        } catch (IOException e1) {
            throw new DownloaderException("failed to delete created temp directory");
        }

    }

    private static Path download() throws DownloaderException {
        Path downloadsPath;
        try {
            downloadsPath = Files.createTempDirectory(
                    Paths.get(System.getProperty("java.io.tmpdir")),
                    "CrawlerBookDownloads"
            );
        } catch (IOException e) {
            throw new DownloaderException("failed to create temp directory for downloaded site, aborting...");
        }
        String mainPageHost;
        try {
            mainPageHost = URLUtils.getHost(BOOK_SITE_MAIN);
        } catch (MalformedURLException e) {
            tryDeleteTempFile(downloadsPath);
            throw new DownloaderException("failed to resolve host \"" + BOOK_SITE_MAIN + "\", aborting...");
        }
        Predicate<String> hostFilter = url -> {
            try {
                return URLUtils
                        .getHost(url)
                        .equals(mainPageHost)
                        && (url.equals(BOOK_SITE_MAIN) ||
                        url.contains(BOOK_URL) ||
                        Arrays
                                .stream(CHAPTERS)
                                .anyMatch(code ->
                                        url.equals(HEADER_TEMPLATE + code) ||
                                                url.startsWith(PAGE_TEMPLATE + code)
                                ));
            } catch (MalformedURLException e) {
                return false;
            }
        };
        System.out.println("test : " + hostFilter.test(BOOK_SITE_MAIN));
        try (
                WebCrawler bookCrawler = new WebCrawler(
                        new CachingDownloader(downloadsPath),
                        DOWNLOADERS_COUNT,
                        EXTRACTORS_COUNT,
                        CONNECTIONS_PER_HOST
                )
        ) {
            bookCrawler.download(
                    BOOK_SITE_MAIN,
                    Integer.MAX_VALUE,
                    hostFilter
            );
        } catch (IOException e) {
            tryDeleteTempFile(downloadsPath);
            throw new DownloaderException("Failed to download some stuff, " + e.getMessage());
        }
        return downloadsPath;
    }

    private static void parse(Path resultFilePath, Path downloadsPath) throws DownloaderException {
        System.out.println("downloadsPath : " + downloadsPath);
        System.out.println("resultFilePath : " + resultFilePath);
//        Files.createDirectories(resultFilePath)
        try (PrintWriter output = new PrintWriter(resultFilePath.toFile())) {
            System.out.println("books : ");
            Files.list(downloadsPath).forEach(book -> {
                System.out.println("book : " + book);
                try (BufferedReader input = Files.newBufferedReader(book.getFileName())) {
                    String html = input.lines().collect(Collectors.joining());
                    Matcher info = infoPattern.matcher(html);
                    Matcher year = yearPattern.matcher(html);
                    while (year.find()) {
                        String curYear = html.substring(
                                year.start() + YEAR_PREFIX.length(),
                                year.end()
                        ).trim();
                        curYear = curYear.substring(4, curYear.length() - 5);
                        if (Integer.parseInt(curYear) > MIN_YEAR && info.find()) {
                            output.println(html.substring
                                    (info.start() + BIBLIOGRAPHIC_RECORD_PREFIX.length(),
                                            +info.end() - BIBLIOGRAPHIC_RECORD_SUFIX.length()).trim());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("failed to process file : " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.out.println("failed to list files while parsing, " + e.getMessage());
//            tryDeleteTempFile(downloadsPath);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1 || args[0].isEmpty()) {
            System.out.println("Expected 1 argument : non-empty path to output");
            return;
        }
        try {
            parse(Paths.get(args[0]), download());
        } catch (DownloaderException e) {
            System.out.println(e.getMessage());
        } catch (InvalidPathException e) {
            System.out.println("failed to create outputPath");
        }
    }
}