package edu.ecnu.crawler.jobs;

import com.google.common.collect.Sets;
import edu.ecnu.crawler.general.Crawler;
import edu.ecnu.crawler.general.Recorder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by leyi on 14/10/29.
 */
public class HudongDictJob {

    static String fenlei = "http://fenlei.baike.com/";

    protected String mPath;
    protected Recorder dicRecorder;
    protected Recorder flRecorder;

    Set<String> wordSet;
    Set<String> cateSet;
    Set<String> urlCrawled;
    Crawler crawler;

    public HudongDictJob(String mPath) {
        this.mPath = mPath;
        this.dicRecorder = new Recorder(mPath + "dict" + File.separator + "hudong.dic", false);
        this.flRecorder = new Recorder(mPath + "dict" + File.separator + "flSeed.dic", false);
        initialize();
        crawler = new Crawler();
    }

    private void initialize() {
        urlCrawled = Sets.newHashSet();
        wordSet = Sets.newHashSet();
        cateSet = Sets.newHashSet();
        File cateFile = flRecorder.getFile();
        File wordFile = dicRecorder.getFile();

        Scanner scanner;
        try {
            scanner = new Scanner(cateFile);
            String cate;
            while (scanner.hasNextLine()) {
                cate = scanner.nextLine();
                String[] flurl = cate.split("\t");
                if (flurl.length > 1) {
                    cateSet.add(flurl[0]);
                    urlCrawled.add(flurl[1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Scanner scanner2;
        try {
            scanner2 = new Scanner(wordFile);
            String word;
            while (scanner2.hasNextLine()) {
                word = scanner2.nextLine();
                String[] words = word.split("\t");
                if (words.length > 2) {
                    wordSet.add(words[1]);
                    urlCrawled.add(words[2]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Load crawled category:" + cateSet.size() + " & crawled words:" + wordSet.size());

    }

    public void runJob() throws IOException {
        parseDoc(Jsoup.parse(new File("./hudong/fenlei.html"), "UTF-8"), "baike.com/fenlei.html");
        parseDoc(Jsoup.parse(new File("./hudong/fenlei2.html"), "UTF-8"), "baike.com/fenlei.html");
        parseDoc(Jsoup.parse(new File("./hudong/fenlei3.html"), "UTF-8"), "baike.com/fenlei.html");
        parseDoc(Jsoup.parse(new File("./hudong/fenlei4.html"), "UTF-8"), "baike.com/fenlei.html");
        parseDoc(Jsoup.parse(new File("./hudong/fenlei5.html"), "UTF-8"), "baike.com/fenlei.html");

        System.out.println("Finished! Count: " + wordSet.size());
        System.out.println("Fenlei count: " + cateSet.size());
    }

    private void parseDoc(Document doc, String url) {
        try {
            Elements eles = doc.select("a[href]");
            for (Element ele : eles) {
                String href = ele.attr("href");

                if (href.contains(fenlei) && !fenlei.equals(href)) {
                    if (href.contains("?")) {
                        href = href.substring(0, href.lastIndexOf("?"));
                    }
                    if (href.equals(fenlei)) {
                        continue;
                    }
                    if (!cateSet.contains(ele.text())) {
                        if (!href.endsWith("/")) {
                            href = href + "/";
                        }
                        if (urlCrawled.contains(href)) {
                            continue;
                        }
                        System.out.println("Parse href:" + href);
                        urlCrawled.add(href);
                        urlCrawled.add(href + "list/");
                        cateSet.add(ele.text());
                        flRecorder.writeRecordUTF8(ele.text() + "\t" + href + "\r\n");
                        System.out.println("Start Crawl fenlei: " + ele.text());
                        doCrawl(href + "list/");
                        //doCrawl(href);
                    }

                } else if (href.contains("http://www.baike.com/wiki/")) {
                    String word = ele.text();

                    if (!wordSet.contains(word) && !urlCrawled.contains(href)) {
                        urlCrawled.add(href);
                        wordSet.add(word);
                        dicRecorder.writeRecordUTF8(wordSet.size() + "\t" + word + "\t" + href + "\r\n");
                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doCrawl(String url) {
        crawler.setUrlnoCheck(url);
        String content = crawler.getContent();
        Document docnew = Jsoup.parse(content);
        parseDoc(docnew, url);
    }

    public static void main(String[] args) throws IOException {
        HudongDictJob job = new HudongDictJob("." + File.separator + "hudong" + File.separator);
        job.runJob();
    }
}
