package edu.ecnu.crawler.parser;

import org.jsoup.nodes.Document;

/**
 * Create XML dump from HTML document
 * Created by leyi on 14/10/23.
 */
public interface IDumpProcessor {
    public void process(Document doc, String originFile);

    public void close();
}
