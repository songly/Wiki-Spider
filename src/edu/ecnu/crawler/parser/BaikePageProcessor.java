package edu.ecnu.crawler.parser;

import com.google.common.collect.Sets;
import edu.ecnu.crawler.general.Recorder;
import edu.ecnu.crawler.general.XMLRecorder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process Baidu Baike Page
 * Created by leyi on 14/10/23.
 */
public class BaikePageProcessor implements IDumpProcessor {
    private XMLRecorder xmlRecorder;
    private Recorder dicRecorder;
    private Recorder synRecorder;
    static final Logger logger = Logger.getLogger(BaikePageProcessor.class.getName());

    Set<String> synsetKeys = Sets.newHashSet();

    public void setSynsetKeys(Set<String> synsetKeys) {
        this.synsetKeys = synsetKeys;
    }

    /**
     * Initialize Processor
     *
     * @param xmlFileName e.g.baikedump001.xml
     */
    public BaikePageProcessor(String xmlFileName, String dicFileName, String synFileName) {
        xmlRecorder = new XMLRecorder("data", xmlFileName);
        dicRecorder = new Recorder(dicFileName);
        synRecorder = new Recorder(synFileName, false);

        Scanner scanner;
        try {
            scanner = new Scanner(synRecorder.getFile());
            while (scanner.hasNextLine()) {
                String key = scanner.nextLine();
                synsetKeys.add(key);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(Document doc, String originFile) {
        logger.log(Level.ALL, "page:" + originFile);
        if (!validate(doc)) return;

        StringBuffer data = new StringBuffer("");
        StringBuffer dictLine = new StringBuffer("");
        data.append("<page>\r\n");

        String titleStr = getTitle(doc, originFile);
        data.append("<title>" + reformat(titleStr) + "</title>\r\n");
        dictLine.append(titleStr).append("\t");

        String urlStr = getUrl(doc, originFile);
        data.append("<url>" + urlStr + "</url>\r\n");
        dictLine.append(urlStr).append("\t");

        String polyStr = getPolyseme(doc);
        data.append("<polyseme>" + reformat(polyStr) + "</polyseme>\r\n");
        dictLine.append(polyStr).append("\t");

        String infobox = getInfoBox(doc);

        data.append("<infobox>" + infobox + "</infobox>\r\n");

        data.append("<desc>" + reformat(getDescription(doc)) + "</desc>\r\n");

        data.append("<text>" + reformat(getText(doc)) + "</text>\r\n");

        String cateDoc = reformat(getCategory(doc));
        data.append("<cate>" + reformat(getCategory(doc)) + "</cate>\r\n");
        dictLine.append(cateDoc).append("\t");

        data.append("<tables>" + reformat(getTables(doc)) + "</tables>\r\n");

        data.append("</page>\r\n");
        dictLine.append("\n");

        if (!"".equals(polyStr) && !synsetKeys.contains(titleStr)) {
            String polyset = getPolyset(doc);
            synsetKeys.add(titleStr);
            synRecorder.writeRecordUTF8(titleStr + "\t" + polyset + "\r\n");
        }

        xmlRecorder.writeRecordUTF8(data.toString());
        dicRecorder.writeRecordUTF8(dictLine.toString());

    }

    private String getPolyset(Document doc) {
        String synset = "";
        Element syns = doc.getElementsByClass("polysemeBodyCon").first();
        if (syns != null) {
            Element current = syns.getElementsByClass("polysemeTitle").first();
            if (current != null) {
                synset += current.text() + ";";
            }
            Elements subs = syns.select("li > a");
            for (Element ele : subs) {
                synset += ele.text() + ";";
            }
        }
        return reformat(synset);
    }

    private String getTitle(Document doc, String origin) {
        String titleStr = "";
        Element searchbox = doc.getElementById("word");
        if (searchbox != null) {
            titleStr = searchbox.attr("value");
        }
        if ("".equals(titleStr)) {
            Element input = doc.getElementById("titleVal");
            if (input != null) {
                titleStr = input.val();
            } else {
                Element titleH1 = doc.getElementsByClass("lemmaTitleH1").first();
                if (titleH1 == null) {
                    System.out.println("Error! No title for: " + origin);
                }
                if (titleH1 != null && titleH1.childNodeSize() > 1) {
                    titleStr = titleH1.childNode(0).toString();
                } else if (titleH1 != null) {
                    titleStr = titleH1.text();
                }
            }
        }

        return titleStr;
    }

    private String getUrl(Document doc, String originFile) {
        String urlStr = "";
        doc.getElementsByAttributeStarting("window.shareLemmaUrl");
        int j = originFile.lastIndexOf(File.separator);
        //!subview
        if (!originFile.contains("-")) {
            urlStr = "/view/" + originFile.substring(j + 5);
        } else {
            int i = originFile.lastIndexOf("-");
            Element sub = doc.select("h1.maintitle").first();
            if (sub == null) {
                sub = doc.select("h1.title").first();
            }
            String id = "";
            String mainid = originFile.substring(j + 5, i);

            if (sub != null) {
                id = sub.attr("id");
            }

            //ID missed - fake id
            if (id == null || "".equals(id)) {
                id = originFile.substring(i + 1, originFile.indexOf(".htm"));
            }

            urlStr = "/subview/" + mainid + "/" + id + ".htm";

        }

        if (urlStr == null || "".equals(urlStr)) {
            logger.log(Level.INFO, "Get URL failed - " + originFile);
        }
        return urlStr;
    }

    private String getPolyseme(Document doc) {
        String polyStr = "";
        Elements polyseme = doc.getElementsByClass("polysemeTitle");
        if (polyseme.size() > 0) {
            polyStr = polyseme.text();
        }
        return polyStr;
    }

    private String getInfoBox(Document doc) {
        String infoStr = "";
        Elements infobox = doc.select("#baseInfoWrapDom");
        for (Element info : infobox) {
            Elements attributes = info.select("div.biItemInner");
            for (Element attr : attributes) {
                infoStr += "<attr>";
                infoStr += "<key>";
                infoStr += reformat(attr.select("span.biTitle").first().text().replaceAll(" ", "").replaceAll("　", "").replaceAll(" ", ""));
                infoStr += "</key>\r\n";
                infoStr += "<value>";
                infoStr += reformat(filter(attr.select("div.biContent").first()).text().replaceAll(" ", "").replaceAll("　", "").replaceAll(" ", ""));
                infoStr += "</value>\r\n";
                infoStr += "</attr>\r\n";
            }
        }
        return infoStr;
    }

    /**
     * Get Entity Description
     *
     * @param doc
     * @return
     */
    private String getDescription(Document doc) {
        String descStr = "";
        Element desc = doc.select("div.card-summary-content").first();
        if (desc == null) {
            desc = doc.select("p.summary-p").first();
        }
        if (desc == null) {
            desc = doc.select("dd.desc").first();
        }
        if (desc == null) {
            desc = doc.select("div.summary-pnl").first();
        }

        if (desc != null) {
            descStr = print(desc);
        }
        return descStr;
    }

    private String getText(Document doc) {
        String textStr = "";
        Element text = doc.select("#lemmaContent-0").first();
        if (text != null) {
            textStr = print(text);
        }
        return textStr;
    }

    private String getCategory(Document doc) {
        String cateStr = "";
        Elements cates = doc.select("a.open-tag");
        if (cates != null)
            for (Element cate : cates) {
                cateStr += cate.text().replaceAll(" ", "").replaceAll("　", "").replaceAll("，", "");
                cateStr += ";";
            }
        return cateStr;
    }

    private String getTables(Document doc) {
        String tblStr = "";
        Elements tables = doc.select("table");
        if (tables == null) return tblStr;

        for (Element table : tables) {
            tblStr += printTbl(table);
        }

        return tblStr;
    }


    private String print(Element ele) {
        String result = "";
        if (ele.hasClass("headline-1")) {
            result = "== " + filter(ele).text() + " ==\r\n";
        } else if (ele.hasClass("headline-2")) {
            result = "=== " + filter(ele).text() + " ===\r\n";
        } else if (ele.hasClass("headline-3")) {
            result = "==== " + filter(ele).text() + " ====\r\n";
        } else if (ele.hasClass("para")) {
            Elements links = filter(ele).select("a[href]");
            for (Element link : links) {
                String target = link.attr("href");
                if (target.contains("/view/") || target.contains("/subview/")) {
                    link.replaceWith(new TextNode("[[" + link.text() + "]]", link.baseUri()));
                } else {
                    link.remove();
                }
            }
            result = ele.text() + "\r\n";
        } else if (ele.hasClass("table-view") || ele.tagName().equals("table")) {
            result = printTbl(ele) + "\r\n";
        } else {
            String text = "";
            Elements eles = ele.children();
            if (eles.size() < 1) {
                return text;
            }
            for (Element el : eles) {
                String temp = print(el);
                if (temp == null || temp.length() <= 1)
                    continue;
                text += print(el) + "\r\n";
            }
            result = text;
        }
        return reformat(result);
    }

    /**
     * Reformat String literal
     *
     * @param result
     * @return
     */
    public static String reformat(String result) {
        StringBuffer sb = new StringBuffer("");

        result = result.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;").replace("^", "").replace("\b", "");

        for (int i = 0; i < result.length(); i++) {
            char ch = result.charAt(i);
            if ((ch == 0x9) || (ch == 0xA) || (ch == 0xD)
                    || ((ch >= 0x20) && (ch <= 0xD7FF))
                    || ((ch >= 0xE000) && (ch <= 0xFFFD))
                    || ((ch >= 0x10000) && (ch <= 0x10FFFF)))
                sb.append(ch);
        }
//        result=result.replaceAll("[\u4E00\u9FA5\u3000\u303F\uFF00\uFFEF\u0000\u007F\u201c\u201d\u001C\u001D\u0006]", " ");
        return sb.toString();
    }

    /**
     * Content filter, Tag filter
     *
     * @param element
     * @return
     */
    public Element filter(Element element) {
        Elements children = element.children();

        Elements brs = children.select("br");
        for (Element br : brs) {
            br.replaceWith(new TextNode(",", ""));
        }

        Elements imgChars = children.select("img.pchar");
        for (Element imgChar : imgChars) {
            imgChar.replaceWith(new TextNode(",", ""));
        }

        if (children.size() > 1) {
            for (Element ele : children) {
                if (ele.hasClass("editable-title")) {
                    ele.remove();
                }
                //TODO: Add more filter rules
            }
        }

        return element;
    }

    public String printTbl(Element element) {
        String tblStr = "";
        if (element.hasClass("topuser") || element.text().equals("参考资料"))
            return tblStr;
        tblStr += "{|\r\n";//table start
        Elements trs = element.select("tr");
        for (Element tr : trs) {
            Elements ths = tr.select("th");
            for (Element th : ths) {
                if (th.toString().contains("colspan") || th.toString().contains("rowspan")) {
                    tblStr += "|";
                    if (th.toString().contains("colspan")) {
                        tblStr += "colspan=" + th.attr("colspan") + " ";
                    }
                    if (th.toString().contains("rowspan")) {
                        tblStr += "rowspan=" + th.attr("rowspan") + " ";
                    }
                }
                tblStr += "!";
                tblStr += th.text();
                tblStr += "\r\n";
            }
            Elements tds = tr.select("td");
            for (Element td : tds) {
                if (td.toString().contains("colspan") || td.toString().contains("rowspan")) {
                    tblStr += "|";
                    if (td.toString().contains("colspan")) {
                        tblStr += "colspan=" + td.attr("colspan") + " ";
                    }
                    if (td.toString().contains("rowspan")) {
                        tblStr += "rowspan=" + td.attr("rowspan") + " ";
                    }
                }
                tblStr += "|";
                tblStr += td.text();
                tblStr += "\r\n";
            }
            tblStr += "|-";
            tblStr += "\r\n";
        }
        tblStr += "|}\r\n";

        return tblStr;
    }

    public void close() {
        xmlRecorder.writeEnd();
    }

    /**
     * Check if the html valid baike page
     *
     * @param doc page html
     * @return if false, not valid baike page
     */
    private boolean validate(Document doc) {
        String text = doc.body().text();
        if (text.length() < 100 && (text.contains("ERROR") && text.contains("Empty page"))) {
            logger.log(Level.ALL, "Empty page");
            return false;
        }
        return true;
    }
}
