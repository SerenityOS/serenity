/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4990825
 * @summary test that VmIdentifier objects get created as expected
 *
 * @modules java.xml
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 */

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import sun.jvmstat.monitor.*;

public class VmIdentifierCreateResolve {

    public static void main(String args[]) throws Exception {
        File testcases =
                new File(System.getProperty("test.src", "."), "testcases");

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        DefaultHandler dh = new VmIdentifierTestHandler();
        sp.parse(testcases, dh);
    }
}

class VmIdentifierTestHandler extends DefaultHandler {
    private static final boolean debug = false;
    private static final int START                     = 0;
    private static final int VMIDENTIFIER_TESTS        = 1;
    private static final int TESTCASE                  = 2;
    private static final int DESCRIPTION               = 3;
    private static final int VMIDENTIFIER              = 4;
    private static final int HOSTIDENTIFIER            = 5;
    private static final int RESOLVED                  = 6;

    private TestCase test;
    private String value = null;
    private int state;
    private Attributes attributes;

    public VmIdentifierTestHandler() {
        super();
    }

    public void characters(char[] ch, int start, int length) {
        String s = new String(ch, start, length);
        if (debug) {
            System.out.println("characters: start = " + start +
                               " length = " + length +
                               " chars = " + s);
        }

        if (value == null) {
            value = s.trim();
        } else {
            value = value + s.trim();
            if (debug) {
                System.out.println("characters: appended characters to "
                                   + "previous value: new value = " + value);
            }
        }
    }

    public void endDocument() {
        if (debug) {
            System.out.println("endDocument()");
        }
    }

    public void endElement(String namespaceURI, String localName,
                           String qName)
                throws SAXException {
        if (debug) {
            System.out.println("endElement(): namespaceURI = " + namespaceURI
                               + " localName = " + localName
                               + " qName = " + qName
                               + " state = " + state);
        }

        switch (state) {
        case START:
            throw new RuntimeException("Unexpected state: " + state);

        case VMIDENTIFIER_TESTS:
            state = START;
            break;

        case TESTCASE:
            if (test == null) {
                throw new RuntimeException("Unexpected thread state");
            }
            try {
              System.out.println("running test case " + test.id);
              test.run();            // run the test
            }
            catch (Exception e) {
              throw new SAXException(e);
            }
            state = VMIDENTIFIER_TESTS;
            test = null;
            value = null;
            break;

        case DESCRIPTION:
            test.setDescription(value);
            state = TESTCASE;
            value = null;
            break;

        case VMIDENTIFIER:
            test.setExpectedVmIdentifier(value);
            state = TESTCASE;
            value = null;
            break;

        case HOSTIDENTIFIER:
            test.setExpectedHostIdentifier(value);
            state = TESTCASE;
            value = null;
            break;

        case RESOLVED:
            test.setResolvedVmIdentifier(value);
            state = TESTCASE;
            value = null;
            break;

        default:
            throw new RuntimeException("Unexpected state: " + state);
        }
    }

    public void endPrefixMapping(String prefix) {
        if (debug) {
            System.out.println("endPrefixMapping(): prefix = " + prefix);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length) {
        if (debug) {
            System.out.println("ignoreableWhitespace():"
                               + " ch = " + new String(ch, start, length)
                               + " start = " + start
                               + " length = " + length);
        }
    }

    public void processingInstruction(String target, String data) {
        if (debug) {
            System.out.println("processingInstruction():"
                               + " target = " + target
                               + " data = " + data);
        }
    }

    public void setDocumentLocator(Locator locator) {
        if (debug) {
            System.out.println("setDocumentLocator(): locator = " + locator);
        }
    }

    public void skippedEntity(String name) {
        if (debug) {
            System.out.println("skippedEntity(): name = " + name);
        }
    }

    public void startDocument() {
        if (debug) {
            System.out.println("startDocument():");
        }
    }

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attributes) {
        if (debug) {
            System.out.println("startElement():"
                               + " namespaceURI = " + namespaceURI
                               + " localName = " + localName
                               + " qName = " + qName
                               + " state = " + state);

            System.out.println("   Attributes(" + attributes.getLength() + ")");
            for (int i = 0; i < attributes.getLength(); i++) {
                System.out.println("     name = " + attributes.getQName(i)
                                   + " value = " + attributes.getValue(i));
            }
        }

        this.attributes = attributes;

        switch (state) {
        case START:
            if (qName.compareTo("VmIdentifierTests") == 0) {
                state = VMIDENTIFIER_TESTS;
            } else {
                System.err.println("unexpected input: state = " + state
                                   + " input = " + qName);
            }
            break;

        case VMIDENTIFIER_TESTS:
            if (qName.compareTo("testcase") == 0) {
                state = TESTCASE;
                int id_n = attributes.getIndex("id");

                if (id_n == -1) {
                    throw new RuntimeException("id attribute expected");
                }

                int vmid_n = attributes.getIndex("VmIdentifierInput");
                if (vmid_n == -1) {
                    throw new RuntimeException(
                            "VmIdentifier attribute expected");
                }

                String hostid_input = null;
                int hostid_n = attributes.getIndex("HostIdentifierInput");
                if (hostid_n != -1) {
                    hostid_input = attributes.getValue(hostid_n);
                }

                String vmid_input = attributes.getValue(vmid_n);
                String id = attributes.getValue(id_n);

                test = new TestCase(id, vmid_input, hostid_input);
            } else {
                System.err.println("unexpected input: state = " + state
                                   + " input = " + qName);
            }
            break;

        case TESTCASE:
            if (test == null) {
                throw new RuntimeException("TestCase null");
            }
            value = null;
            if (qName.compareTo("description") == 0) {
                state = DESCRIPTION;

            } else if (qName.compareTo("VmIdentifier") == 0) {
                state = VMIDENTIFIER;

            } else if (qName.compareTo("HostIdentifier") == 0) {
                state = HOSTIDENTIFIER;

            } else if (qName.compareTo("Resolved") == 0) {
                state = RESOLVED;

            } else {
                System.err.println("unexpected input: state = " + state
                                   + " input = " + qName);
            }
            break;

        case DESCRIPTION:
        case VMIDENTIFIER:
        case HOSTIDENTIFIER:
        case RESOLVED:
            if (test == null) {
                throw new RuntimeException("TestCase null");
            }
            break;

        default:
            System.err.println("Unexpected state: " + state);
            break;
        }
    }

    public void startPrefixMapping(String prefix, String uri) {
        if (debug) {
            System.out.println("startPrefixMapping():"
                               + " prefix = " + prefix
                               + " uri = " + uri);
        }
    }
}

class VmIdentifierException extends Exception {
    String result;
    TestCase test;

    VmIdentifierException(TestCase test, String result) {
        this.test = test;
        this.result = result;
    }

    public String getMessage() {
        return "Test " + test.id + " " + "Failed: "
               + "Expected = " + test.expectedVmIdentifier + " "
               + "Actual = " + result;
    }
}

class HostIdentifierException extends Exception {
    String result;
    TestCase test;

    HostIdentifierException(TestCase test, String result) {
        this.test = test;
        this.result = result;
    }

    public String getMessage() {
        return "Test " + test.id + " " + "Failed: "
               + "Expected = " + test.expectedHostIdentifier + " "
               + "Actual = " + result;
    }
}

class ResolvedVmIdentifierException extends Exception {
    String result;
    TestCase test;

    ResolvedVmIdentifierException(TestCase test, String result) {
        this.test = test;
        this.result = result;
    }
    public String getMessage() {
        return "Test " + test.id + " " + "Failed: "
               + "Expected = " + test.resolvedVmIdentifier + " "
               + "Actual = " + result;
    }
}

class TestCase {
    private static final boolean debug = false;

    String id;
    String vmid;
    String hostid;
    String expectedVmIdentifier;
    String expectedHostIdentifier;
    String resolvedVmIdentifier;
    String description;

    public TestCase(String id, String vmid, String hostid) {
        this.id = id;
        this.vmid = vmid;
        this.hostid = hostid;
    }

    public void run() throws Exception {
        if (expectedVmIdentifier == null || expectedHostIdentifier == null
                || resolvedVmIdentifier == null) {
            throw new IllegalArgumentException(
                    "expected values not initialized");
        }

        VmIdentifier test_vmid = null;
        HostIdentifier test_hostid = null;
        VmIdentifier resolved_vmid =  null;

        if (debug) {
            System.out.println("creating VmIdentifier");
        }

        test_vmid = new VmIdentifier(vmid);

        if (debug) {
            System.out.println("creating HostIdentifier");
        }

        if (hostid != null) {
            test_hostid = new HostIdentifier(hostid);
        } else {
            test_hostid = new HostIdentifier(test_vmid);
        }

        if (debug) {
            System.out.println("resolving VmIdentifier");
        }

        resolved_vmid =  test_hostid.resolve(test_vmid);

        String test_vmid_str = test_vmid.toString();
        String test_hostid_str = test_hostid.toString();
        String resolved_vmid_str = resolved_vmid.toString();

        if (debug) {
            System.out.println("comparing VmIdentifier result");
        }

        if (test_vmid_str.compareTo(expectedVmIdentifier) != 0) {
            throw new VmIdentifierException(this, test_vmid_str);
        }

        if (debug) {
            System.out.println("comparing HostIdentifier result");
        }

        if (test_hostid_str.compareTo(expectedHostIdentifier) != 0) {
            throw new HostIdentifierException(this, test_hostid_str);
        }

        if (debug) {
            System.out.println("comparing VmIdentifier result");
        }

        if (resolved_vmid_str.compareTo(resolvedVmIdentifier) != 0) {
            throw new ResolvedVmIdentifierException(this, resolved_vmid_str);
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExpectedVmIdentifier(String expectedVmIdentifier) {
        if (debug) {
            System.out.println("setting vmidentifier string to " + vmid);
        }
        this.expectedVmIdentifier = expectedVmIdentifier;
    }

    public void setExpectedHostIdentifier(String expectedHostIdentifier) {
        this.expectedHostIdentifier = expectedHostIdentifier;
    }

    public void setResolvedVmIdentifier(String resolvedVmIdentifier) {
        this.resolvedVmIdentifier = resolvedVmIdentifier;
    }
}
