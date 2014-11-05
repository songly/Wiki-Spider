package edu.ecnu.crawler.jobs;

import com.google.common.collect.Sets;
import edu.ecnu.crawler.general.ErrorRecorder;
import edu.ecnu.crawler.general.InfoRecorder;
import edu.ecnu.crawler.general.NowTime;
import edu.ecnu.crawler.general.Recorder;
import edu.ecnu.crawler.parser.BaikePageProcessor;
import edu.ecnu.crawler.parser.IDumpProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by leyi on 14/10/27.
 */
public class DumpParseJob {
    protected InfoRecorder mInfoRecorder;
    protected InfoRecorder mWarningRecorder;
    protected ErrorRecorder mErrorRecorder;
    protected String mPath;

    static final Logger logger = Logger.getLogger(BaikeCrawlJob.class.getName());

    public DumpParseJob(String mPath) {
        this.mPath = mPath;
        mInfoRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "info.log");
        mWarningRecorder = new InfoRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "brief.log");
        mErrorRecorder = new ErrorRecorder(mPath + "logs" + File.separator + Recorder.formatFileName(NowTime.getNowTime()) + "error.log");

    }

    public void dump(String dir, String encoding) {
        IDumpProcessor parser = new BaikePageProcessor(mPath + "data.xml", mPath + "data.dic", mPath + "polyset");
        Set<String> finished = Sets.newHashSet();
        Recorder mFinished = new Recorder(mPath + "finished", false);

        Scanner scanner;
        try {
            scanner = new Scanner(mFinished.getFile());
            while (scanner.hasNextLine()) {
                String file = scanner.nextLine();
                finished.add(file);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File folder = new File(dir);
        File[] files = folder.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                String s1;
                s1 = extractView(file.getName());
                String s2 = extractView(file2.getName());
                int i1 = Integer.parseInt(s1);
                int i2 = Integer.parseInt(s2);
                return i1 - i2;
            }
        });
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (finished.contains(fileName)) {
                    continue;
                }

                if (fileName.endsWith("htm") && fileName.contains("view")) {
                    System.out.println(fileName);
                    Document doc = null;
                    try {
                        if (fileName.endsWith("-0.htm")) {
                            doc = Jsoup.parse(file, "GBK", "");
                        } else {
                            doc = Jsoup.parse(file, encoding, "");
                        }
                    } catch (IOException e) {
                        System.out.println(file.getName() + " Wrong Content!");
                        logger.log(Level.ALL, "Could not parse file " + file.getName(), e);
                    }

                    parser.process(doc, fileName);
                    mFinished.writeRecordUTF8(fileName + "\r\n");
                }
            }
        }

        parser.close();
    }

    public static String extractView(String file) {
        if (!file.endsWith(".htm")) return "-1";
        if (file.contains("-")) {
            return file.substring(4, file.lastIndexOf("-"));
        }
        return file.substring(4, file.lastIndexOf(".htm"));
    }
}
