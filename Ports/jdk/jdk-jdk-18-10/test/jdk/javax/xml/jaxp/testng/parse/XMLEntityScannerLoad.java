package parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * JDK-8059327: XML parser returns corrupt attribute value
 * https://bugs.openjdk.java.net/browse/JDK-8059327
 *
 * Also:
 * JDK-8061550: XMLEntityScanner can corrupt corrupt content during parsing
 * https://bugs.openjdk.java.net/browse/JDK-8061550
 *
 * @Summary: verify that the character cache in XMLEntityScanner is reset properly
 */

public class XMLEntityScannerLoad {

    @Test(dataProvider = "xmls")
    public void test(String xml) throws SAXException, IOException, ParserConfigurationException {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ChunkInputStream(xml));
        String value = d.getDocumentElement().getAttribute("a1");
        assertEquals(value, "w");
    }

    static class ChunkInputStream extends ByteArrayInputStream {
        ChunkInputStream(String xml) {
            super(xml.getBytes());
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, 7);
        }
    }

    @DataProvider(name = "xmls")
    private Object[][] xmls() {
        return new Object[][] {
            {"<?xml version=\"1.0\"?><element a1=\"w\" a2=\"&quot;&quot;\"/>"},
            {"<?xml version=\"1.1\"?><element a1=\"w\" a2=\"&quot;&quot;\"/>"}
        };
    }
}
