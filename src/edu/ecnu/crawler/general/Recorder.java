package edu.ecnu.crawler.general;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.regex.Matcher;

public class Recorder {
    protected static String defaultPath = "." + File.separator + "default";
    protected String mPath;
    protected File mFile;

    public Recorder() {
        mPath = this.formatPath(defaultPath);
        mFile = this.createFile(mPath);
    }

    public Recorder(String path) {
        mPath = this.formatPath(path);
        mFile = this.createFile(mPath);
    }

    protected String formatPath(String path) {
        path = path.replaceAll(Matcher.quoteReplacement(File.separator), "defaultSep");
        path = path.replaceAll(" |:|\\*|\\?|\"|<|>|/|\\\\|", "");
        path = path.replaceAll("defaultSep", Matcher.quoteReplacement(File.separator));
        int count = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == File.separatorChar) {
                count++;
            }
        }
        if (path == null || path.equals("") || count == path.length()) {
            return defaultPath;
        }
        return path;
    }

    public File getFile() {
        return mFile;
    }

    protected File createFile(String path) {
        path = this.formatPath(path);
        File file = null;
        try {
            if (path.lastIndexOf(File.separator) >= 0) {
                file = new File(path.substring(0, path.lastIndexOf(File.separator)));
                file.mkdirs();
            }
            file = new File(path);
            file.createNewFile();
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
        return file;
    }

    public void writeRecord(String message) {
        try {
            Writer out = new FileWriter(mFile, true);
            out.write(message);
            out.close();
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    public void writeRecord(String message, Boolean coverage) {
        try {
            Writer out = new FileWriter(mFile, !coverage);
            out.write(message);
            out.close();
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    public static String formatFileName(String path) {
        String filename = path;
        if (path.lastIndexOf(File.separator) >= 0) {
            filename = path.substring(path.lastIndexOf(File.separator) + 1);
            path = path.substring(0, path.lastIndexOf(File.separator) + 1);
        }
        filename = filename.replaceAll("\\W", "-");

        while (filename.indexOf("--") >= 0) {
            filename = filename.replaceAll("\\-\\-", "-");
        }
        return path + filename;
    }

}
