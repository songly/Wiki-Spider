package edu.ecnu.crawler;

import edu.ecnu.crawler.parser.BaikePageProcessor;
import edu.ecnu.crawler.parser.IDumpProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;

/**
 * Created by leyi on 14/10/23.
 */
public class TestMain {

    String dir = "baikeinfo";

    public void test() throws IOException {

        File folder = new File(dir);
        File[] files = folder.listFiles();
        String lastView = "";
        IDumpProcessor parser = new BaikePageProcessor("test.xml", "test.dic", "");
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();


                if (fileName.endsWith("htm") && fileName.contains("view")) {
                    System.out.println(fileName);
                    Document doc = Jsoup.parse(file, "UTF-8", "");
                    parser.process(doc, fileName);
                }
            }
        }

        parser.close();
    }

    public void testCharRep() {

    }

    public static void main(String[] args) throws IOException {

        new TestMain().test();

    }
}
