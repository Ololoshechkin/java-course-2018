package ru.ifmo.ctddev.kgeorgiy.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Calendar;
import java.util.regex.Pattern;


public class BookDownloader {

	static final String BASE_SITE = "http://e.lanbook.com/book";

	public static void main(String[] args) {
		try {
			WebCrawler crawler = new WebCrawler(new CachingDownloader(), 32, 32, 32);
		    Result result = crawler.download(BASE_SITE, 30);
		    crawler.close();
		    System.out.println("download: " + result.getDownloaded());
		    System.out.println("errs: " + result.getErrors());
		    // parse();
		} catch (Throwable ignored) {

		}
	}












	private static void parse() throws FileNotFoundException, URISyntaxException {
        PrintWriter out = new PrintWriter(System.out);
        String[] bookPages = DIR.toFile().list();
        BufferedReader in;

        for (String book : bookPages) {
        	System.out.println(book);
            in = new BufferedReader(new FileReader(DIR.resolve(book).toFile()));
            String html = in.lines().collect(Collectors.joining());
            Matcher info = infoPattern.matcher(html);
            Matcher year = yearPattern.matcher(html);
            while (year.find()) {
                String curYear = html.substring(year.start() + PRE_YEAR.length(),
                        year.end()).trim();
                curYear = curYear.substring(4, curYear.length() - 5);
                System.out.println(curYear);
                if (Integer.parseInt(curYear) > MIN_YEAR && info.find()) {
                    out.println(html.substring
                            (info.start() + PRE_BIBL_RECORD.length(),
                                    +info.end() - POST_BIBL_RECORD.length()).trim());
                }
            }
        }
        out.close();
    }

    static final String MAIN_PAGE = "https://e.lanbook.com/books";
	static final String HEADER_URL = "https://e.lanbook.com/books/%d";
	static final String PAGE_URL = "https://e.lanbook.com/books/%d?page=";
	static final String BOOK_URL = "https://e.lanbook.com/book/";
	static final Path DIR = Paths.get("Data");
	static final int CODES[] = new int[] {917, 918, 1537};
	static final String BIBLIOGRAPHIC_RECORD_TXT = "bibliographic_record.txt";
	static final String PRE_YEAR = "<dt>Год:</dt>";
	static final String PRE_BIBL_RECORD = "<div id=\"bibliographic_record\">";
	static final String POST_BIBL_RECORD = "</div>";
	static final int MIN_YEAR = Calendar.getInstance().get(Calendar.YEAR) - 5;
	static final Pattern infoPattern = Pattern.compile(PRE_BIBL_RECORD + "[^<]*" + POST_BIBL_RECORD);
    static final Pattern yearPattern = Pattern.compile(PRE_YEAR + "\\s*<dd>\\d*</dd>");

}