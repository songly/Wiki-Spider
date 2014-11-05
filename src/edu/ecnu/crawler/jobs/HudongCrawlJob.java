package edu.ecnu.crawler.jobs;

import com.google.common.collect.Lists;
import edu.ecnu.crawler.general.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Crawl pages in the dictionary
 * Collect wiki page href candidates
 * Parse page into XML format
 * Created by leyi on 14/10/31.
 */
public class HudongCrawlJob {

    protected InfoRecorder mInfoRecorder;
    protected InfoRecorder mWarningRecorder;
    protected ErrorRecorder mErrorRecorder;

    protected XMLRecorder mXmlRecorder;
    protected Recorder rollRecorder; //store candidate url for rolling
    protected String mPath;

    private List<String> crawledURL;
    private int count;

    public HudongCrawlJob(String mPath) {
        this.mPath = mPath;
        mInfoRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "info.log");
        mWarningRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "brief.log");
        mErrorRecorder = new ErrorRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "error.log");
    }

    public boolean writeTxt(String context, String filePath) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(
                filePath, true), "UTF-8");

        osw.write(context, 0, context.length());
        osw.flush();
        osw.close();
        return true;
    }

    public void doCrawl(int start, int end) {
        Crawler crawler = new Crawler();
        initialize();

        count = start;
        Recorder mBreakpoint = new Recorder(mPath + "breakpoint", false);
        Scanner scanner;
        try {
            scanner = new Scanner(mBreakpoint.getFile());
            if (scanner.hasNextInt()) {
                count = scanner.nextInt();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (count > 0 && count >= start && count <= end) {
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
            mXmlRecorder = new XMLRecorder("data", mPath + "data.xml");
        }
        mBreakpoint.writeRecord("" + count, true);

        //跳过空页面和测试页面

        for (int i = count; i <= end; i++) {
            //String number=String.format("%06d", i);
            String number = String.valueOf(i);
            String currentUrl = crawledURL.get(i - 1);

            crawlPage(crawler, currentUrl, number, mPath + "wiki" + number);

            mBreakpoint.writeRecord("" + (i + 1), true);

            int chance = new Random().nextInt(3);
            //Take the opportunity to sleep
            if (chance != 0) {
                sleep();
            }
        }

    }

    public void sleep() {
        long sleepTime = System.currentTimeMillis() % 4500 + 300;
        System.out.println("Sleep " + sleepTime + "ms");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            mErrorRecorder.writeRecord("Thread sleep interrupted!");
        }
    }

    private void initialize() {
        crawledURL = Lists.newArrayList();
        Scanner scanner;
        try {
            scanner = new Scanner(new File("hudong/hudong.dic"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] items = line.split("\t");
                if (items.length > 2) {
                    crawledURL.add(items[2]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public void crawlPage(Crawler crawler, String url, String id, String filename) {
        url = urlFilter(url);
        crawler.setUrl(url);
        mInfoRecorder.writeRecord("Start Crawl Baike URL: " + url);
        String content = "";
        try {
            content = crawler.getContent();
        } catch (StringIndexOutOfBoundsException e) {
            mErrorRecorder.writeRecord("String Index Out of Bound during crawling");
        }

        //解析
        if (content.contains("共搜索到约") && content.contains("个结果")) {
            return;
        }
        if (content.contains("测试页面")) {
            return;
        }

        try {
            String word = url.replace("http://www.baike.com/wiki/", "");
            writeTxt(content, filename + ".htm");
        } catch (IOException e) {
            mErrorRecorder.writeRecord("write to file error");
        }

        //createDocAuth.do 未创建词条

        Document doc = null;
        try {
            doc = Jsoup.parse(new File(filename + ".htm"), "UTF-8", "");
        } catch (IOException e) {
            mErrorRecorder.writeRecord("Get Wrong Content " + filename);
        }
        if (doc != null) {
            rollRecorder = new Recorder(mPath + File.separator + "candidate.dic", false);
            Elements elements = doc.select("a[href]");
            for (Element ele : elements) {
                String href = ele.attr("href");
                href = urlFilter(href);
                if (href.contains("http://www.baike.com/wiki/") && !href.equals(url)) {
                    if (!crawledURL.contains(href)) {
                        crawledURL.add(href);
                        rollRecorder.writeRecordUTF8(ele.text() + "\t" + href + "\r\n");
                    }
                }
                if (ele.hasClass("innerlink") && href.contains("http://www.baike.com/sowiki/")) {
                    href = href.replace("sowiki", "wiki");
                    if (!crawledURL.contains(href)) {
                        crawledURL.add(href);
                        rollRecorder.writeRecordUTF8(ele.text() + "\t" + href + "\r\n");
                    }
                }
            }
        }
    }

    public static String urlFilter(String url) {
        if (url != null && url.contains("?")) {
            url = url.substring(0, url.lastIndexOf("?"));
        }
        if (url != null && url.contains("#")) {
            url = url.substring(0, url.lastIndexOf("#"));
        }
        return url;
    }
}
