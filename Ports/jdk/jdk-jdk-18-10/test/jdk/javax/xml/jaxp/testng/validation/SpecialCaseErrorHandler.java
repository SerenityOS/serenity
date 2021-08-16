package validation;

import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SpecialCaseErrorHandler implements ErrorHandler {
    public static final boolean DEBUG = false;

    private HashMap errors;

    public SpecialCaseErrorHandler(String[] specialCases) {
        errors = new HashMap();
        for (int i = 0; i < specialCases.length; ++i) {
            errors.put(specialCases[i], Boolean.FALSE);
        }
    }

    public void reset() {
        for (Iterator iter = errors.keySet().iterator(); iter.hasNext();) {
            String error = (String) iter.next();
            errors.put(error, Boolean.FALSE);
        }
    }

    public void warning(SAXParseException arg0) throws SAXException {
        if (DEBUG) {
            System.err.println(arg0.getMessage());
        }
    }

    public void error(SAXParseException arg0) throws SAXException {
        if (DEBUG) {
            System.err.println(arg0.getMessage());
        }
        for (Iterator iter = errors.keySet().iterator(); iter.hasNext();) {
            String error = (String) iter.next();
            if (arg0.getMessage().startsWith(error)) {
                errors.put(error, Boolean.TRUE);
            }
        }
    }

    public void fatalError(SAXParseException arg0) throws SAXException {
        throw arg0;
    }

    public boolean specialCaseFound(String key) {
        return ((Boolean) errors.get(key)).booleanValue();
    }
}
