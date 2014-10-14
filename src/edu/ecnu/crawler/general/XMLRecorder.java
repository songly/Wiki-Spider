package edu.ecnu.crawler.general;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;


public class XMLRecorder extends Recorder {
    private String mKeyWord;

    public XMLRecorder(String keyword) {
        super("." + File.separator + "XmlRecorder.xml");
        mKeyWord = keyword;
        writeHead();
    }

    public XMLRecorder(String keyword, String path) {
        super(path);
        mKeyWord = keyword;
        writeHead();
    }

    public XMLRecorder(String keyword, String path, Boolean coverage) {
        super(path);
        mKeyWord = keyword;
        if (coverage)
            writeHead();
    }

    public void writeEnd() {
        try {
            Writer out = new FileWriter(mFile, true);
            out.write("</" + mKeyWord + ">" + "\r\n");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeHead() {
        try {
            Writer out = new FileWriter(mFile);
            out.write("<?xml version=\"1.0\" encoding=\"GBK\"?>" + "\r\n"
                    + "<!DOCTYPE " + mKeyWord + " SYSTEM \"" + mKeyWord + ".dtd\">"
                    + "\r\n" + "<" + mKeyWord + ">" + "\r\n");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeRecord(String message, Boolean coverage) {
        writeRecord(message);
    }

    public static String formatXML(String message) {
        return message.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;").replaceAll("\"", "&quot;")
                .replaceAll("'", "&apos;");
    }
}
