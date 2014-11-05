package edu.ecnu.crawler;

import edu.ecnu.crawler.jobs.DumpParseJob;

import java.io.File;

/**
 * Created by leyi on 14/10/27.
 */
public class ParserMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Need arguments: folder path + encoding");
            return;
        }
        DumpParseJob parseJob = new DumpParseJob("." + File.separator + "baikedump" + File.separator);
        parseJob.dump(args[0], args[1]);
    }
}
