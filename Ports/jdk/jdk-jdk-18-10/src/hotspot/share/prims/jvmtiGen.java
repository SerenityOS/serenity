/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

public class jvmtiGen
{
    private static final int EXIT_FAILURE_ERROR = 1;
    private static final int EXIT_FAILURE_BADARGUMENTS = 2;

    private static boolean verbose = false;

    /**
     * Write out usage and exit.
     */
    private static void showUsage() {
        System.err.println("usage:");
        System.err.println("  java jvmtiGen " +
                           "[-verbose] " +
                           "-IN <input XML file name> " +
                           "-XSL <XSL file> " +
                           "-OUT <output file name> " +
                           "[-PARAM <name> <expression> ...]");
        System.exit(EXIT_FAILURE_BADARGUMENTS); // There is no returning from showUsage()
    }

    public static void main (String argv []) {
        String inFileName = null;
        String xslFileName = null;
        String outFileName = null;
        final List<String> params = new ArrayList<String>();
        for (int ii = 0; ii < argv.length; ii++) {
            if (argv[ii].equals("-verbose")) {
                verbose = true;
            } else if (argv[ii].equals("-IN")) {
                inFileName = argv[++ii];
            } else if (argv[ii].equals("-XSL")) {
                xslFileName = argv[++ii];
            } else if (argv[ii].equals("-OUT")) {
                outFileName = argv[++ii];
            } else if (argv[ii].equals("-PARAM")) {
                if (ii + 2 < argv.length) {
                    final String name = argv[++ii];
                    params.add(name);
                    final String expression = argv[++ii];
                    params.add(expression);
                } else {
                    showUsage();
                }
            } else {
                showUsage();
            }
        }
        if (inFileName == null || xslFileName == null || outFileName == null) {
            showUsage();
        }

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setXIncludeAware(true);

        final File datafile   = new File(inFileName);
        final File stylesheet = new File(xslFileName);

        try (
            final OutputStream os = new BufferedOutputStream(new FileOutputStream(outFileName));
        ) {
            final StreamSource stylesource = new StreamSource(stylesheet);
            // Use a Transformer for output
            final Transformer transformer =
                TransformerFactory.newInstance().newTransformer(stylesource);
            for (int ii = 0; ii < params.size(); ii += 2) {
                transformer.setParameter(params.get(ii), params.get(ii + 1));
            }
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                    public void fatalError(SAXParseException exn) throws SAXException {
                        throw new SAXException(exn);
                    }
                    public void error(SAXParseException exn) throws SAXException {
                        fatalError(exn);
                    }
                    public void warning(SAXParseException exn) throws SAXException {
                        if (verbose) {
                            System.err.println("jvmtiGen warning: " + exn.getMessage());
                        }
                    }
                });
            final Document document = builder.parse(datafile);
            final DOMSource source = new DOMSource(document);
            final StreamResult result = new StreamResult(os);
            transformer.transform(source, result);
        } catch (IOException
            | ParserConfigurationException
            | SAXException
            | TransformerException exn) {
            System.err.print("jvmtiGen error: " + exn.getMessage());
            exn.printStackTrace(System.err);
            System.exit(EXIT_FAILURE_ERROR);
        }
    } // main
}
