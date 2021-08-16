package parsers;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @test
 * @bug 8072081
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.SupplementaryChars
 * @run testng/othervm parsers.SupplementaryChars
 * @summary verifies that supplementary characters are supported as character
 * data in xml 1.0, and also names in xml 1.1.
 *
 * Joe Wang (huizhe.wang@oracle.com)
 */

@Listeners({jaxp.library.BasePolicy.class})
public class SupplementaryChars {

    @Test(dataProvider = "supported")
    public void test(String xml) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        getParser().parse(stream, new DefaultHandler());
        stream.close();
    }

    @Test(dataProvider = "unsupported", expectedExceptions = SAXParseException.class)
    public void testInvalid(String xml) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        getParser().parse(stream, new DefaultHandler());
        stream.close();
    }

    @DataProvider(name = "supported")
    public Object[][] supported() {

        return new Object[][] {
            {"<?xml version=\"1.0\"?><tag>\uD840\uDC0B</tag>"},
            {"<?xml version=\"1.0\"?><!-- \uD840\uDC0B --><tag/>"},
            {"<?xml version=\"1.1\"?><tag\uD840\uDC0B>in tag name</tag\uD840\uDC0B>"},
            {"<?xml version=\"1.1\"?><tag attr\uD840\uDC0B=\"in attribute\">in attribute name</tag>"},
            {"<?xml version=\"1.1\"?><tag>\uD840\uDC0B</tag>"},
            {"<?xml version=\"1.1\"?><!-- \uD840\uDC0B --><dontCare/>"}
        };
    }

    @DataProvider(name = "unsupported")
    public Object[][] unsupported() {
        return new Object[][] {
            {"<?xml version=\"1.0\"?><tag\uD840\uDC0B>in tag name</tag\uD840\uDC0B>"},
            {"<?xml version=\"1.0\"?><tag attr\uD840\uDC0B=\"in attribute\">in attribute name</tag>"}
        };
    }

    private SAXParser getParser() {
        SAXParser parser = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return parser;
    }
}
