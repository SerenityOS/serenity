/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GTestResultParser {
    private final List<String> _failedTests;

    public GTestResultParser(Path file) {
        List<String> failedTests = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(file)) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            XMLStreamReader xmlReader = factory.createXMLStreamReader(r);
            String testSuite = null;
            String testCase = null;
            while (xmlReader.hasNext()) {
                int code = xmlReader.next();
                if (code == XMLStreamConstants.START_ELEMENT) {
                    switch (xmlReader.getLocalName()) {
                        case "testsuite":
                            testSuite = xmlReader.getAttributeValue("", "name");
                            break;
                        case "testcase":
                            testCase = xmlReader.getAttributeValue("", "name");
                            break;
                        case "failure":
                            failedTests.add(testSuite + "::" + testCase);
                            break;
                        default:
                            // ignore
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("can't open parse xml " + file, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("can't open result file " + file, e);
        }
        _failedTests = Collections.unmodifiableList(failedTests);
    }

    public List<String> failedTests() {
        return _failedTests;
    }
}
