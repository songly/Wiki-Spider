package edu.ecnu.crawler.netease;

import edu.ecnu.crawler.general.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PageContents {
    protected InfoRecorder mInfoRecorder;
    protected InfoRecorder mWarningRecorder;
    protected ErrorRecorder mErrorRecorder;
    protected XMLRecorder mXmlRecorder;
    protected String mPath;

    static final Logger logger = Logger.getLogger(PageContents.class.getName());
    final String baike = "http://baike.baidu.com";

    public boolean writeTxt(String context, String filePath) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(
                filePath, true));

        osw.write(context, 0, context.length());
        osw.flush();
        osw.close();
        return true;
    }

    public PageContents() {
        mPath = "." + File.separator;
        mInfoRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "info.log");
        mWarningRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "brief.log");
        mErrorRecorder = new ErrorRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "error.log");
    }

    public PageContents(String path) {
        mPath = path;
        mInfoRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "info.log");
        mWarningRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "brief.log");
        mErrorRecorder = new ErrorRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "error.log");
    }

    public void doCrawl(int start, int end) throws IOException {
        Crawler crawler = new Crawler();
        mInfoRecorder.writeRecord("Baidu Baike Crawler");
        int count = start;
        Recorder mBreakpoint = new Recorder(mPath + "breakpoint");
        Scanner scanner;
        try {
            scanner = new Scanner(mBreakpoint.getFile());
            if (scanner.hasNextInt()) {
                count = scanner.nextInt();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (count > 1) {
            mInfoRecorder.writeRecord("Continue from Breakpoint:" + count);
            mXmlRecorder = new XMLRecorder("companies", mPath + "data.xml", false);
        } else {
            if (count <= 0) {
                mInfoRecorder.writeRecord("Failed.");
                mWarningRecorder.writeRecord("Failed.");
                @SuppressWarnings("resource")
                Scanner in = new Scanner(System.in);
                if (!in.next().equals("y")) {
                    System.exit(0);
                }
                count = 1;
            }
            mXmlRecorder = new XMLRecorder("companies", mPath + "data.xml");
        }
        mBreakpoint.writeRecord("" + count, true);
        for (int i = count; i <= end; i++) {
            //String number=String.format("%06d", i);
            String number = String.valueOf(i);
            String currentUrl = baike + "/view/" + number + ".htm";

            crawlPage(crawler, currentUrl, number, mPath + "view" + number);

            mBreakpoint.writeRecord("" + (i + 1), true);

            int chance = new Random().nextInt(3);
            //Take the opportunity to sleep
            if (chance != 0) {
                sleep();
            }
        }
        mInfoRecorder.writeEnd();
        mWarningRecorder.writeEnd();
        mErrorRecorder.writeEnd();
        mXmlRecorder.writeEnd();
    }

    public void sleep() {
        long sleepTime = System.currentTimeMillis() % 4900 + 1000;
        System.out.println("Sleep " + sleepTime + "ms");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread sleep interrupted!", e);
        }
    }


    public void crawlPage(Crawler crawler, String url, String id, String filename) {
        crawler.setUrl(url);
        mInfoRecorder.writeRecord("Start Crawl Baike URL: " + url);
        String content = "";
        try {
            content = crawler.getContent();
        } catch (StringIndexOutOfBoundsException e) {
            logger.log(Level.SEVERE, "Crawl web page error!", e);
        }

        //多义词
        if (content.contains(">多义词</a>") && !filename.contains("-")) {
            URL parent = null;
            try {
                parent = new URL(url);
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Malformed URL!" + e);
            }

            URLConnection urlConnection = null;

            try {
                urlConnection = parent.openConnection();

                try {
                    InputStream input = urlConnection.getInputStream();
                    Document doc = Jsoup.parse(input, "UTF-8", "");

                    Elements elements = doc.select("a[href]");
                    int sub = 0;
                    for (Element element : elements) {
                        String linkUrl = element.attr("href");
                        if (linkUrl.contains("/subview/" + id)) {
                            sub++;
                            String normalizeUrl = baike + linkUrl;
                            if (normalizeUrl.contains("#")) {
                                normalizeUrl = normalizeUrl.substring(0, normalizeUrl.lastIndexOf("#"));
                            }
                            crawlPage(crawler, normalizeUrl, id, filename + "-" + sub);
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Read sub view page error!", e);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "open url error!", e);
            }

        } else {
            try {
                writeTxt(content, filename + ".htm");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Write page error!", e);
            }
        }
    }


}
