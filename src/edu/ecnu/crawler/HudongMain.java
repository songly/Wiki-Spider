package edu.ecnu.crawler;

import edu.ecnu.crawler.jobs.HudongCrawlJob;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by leyi on 14/11/5.
 */
public class HudongMain {
    private static Logger logger = Logger.getLogger(BaikeMain.class.getName());

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Need arguments: start ID, end ID");
            return;
        }
        int start = Integer.valueOf(args[0]);
        int end = Integer.valueOf(args[1]);
        HudongCrawlJob pages = new HudongCrawlJob("." + File.separator + "hudonginfo" + File.separator);
        try {
            pages.doCrawl(start, end);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Spider Crashes!", e);
        }
    }
}
