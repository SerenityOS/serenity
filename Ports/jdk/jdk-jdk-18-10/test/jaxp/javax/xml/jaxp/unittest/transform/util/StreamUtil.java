/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package transform.util;

import static jaxp.library.JAXPTestUtilities.runWithTmpPermission;
import static jaxp.library.JAXPTestUtilities.tryRunWithTmpPermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.PropertyPermission;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.w3c.dom.Document;

import transform.VersionDefaultHandler;

public class StreamUtil extends TransformerUtil {

    DocumentBuilder docBuilder = null;

    private static StreamUtil instance = null;

    /** Creates a new instance of StreamUtil */
    private StreamUtil() throws Exception {
        if (docBuilder == null)
            docBuilder = getDomParser();
    }

    public static synchronized StreamUtil getInstance() throws Exception {
        if (instance == null)
            instance = new StreamUtil();
        return instance;
    }

    public Source prepareSource(InputStream is) throws Exception {
        return new StreamSource(is);
    }

    public Result prepareResult() throws Exception {
        FileOutputStream fos = new FileOutputStream(TEMP_FILE);
        return runWithTmpPermission(() -> new StreamResult(fos), new PropertyPermission("user.dir", "read"));
    }

    public void checkResult(Result result, String inputVersion) throws Exception {
        ((StreamResult) result).getOutputStream().close();
        FileInputStream fis = new FileInputStream(TEMP_FILE);
        checkStream(fis, inputVersion);
    }

    public void checkStream(FileInputStream fis, String inputVersion) throws Exception {
        docBuilder.reset();
        Document output = docBuilder.parse(fis);
        String version = output.getXmlVersion();
        Assert.assertTrue(inputVersion.equals(version), "Expected XML Version is 1.1, but actual version " + version);
    }

    public void checkResult(Result result, String version, String encoding) throws Exception {
        // use sax parser, as encoding info cannot be set on DOM document
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        VersionDefaultHandler dh = new VersionDefaultHandler();
        tryRunWithTmpPermission(() -> parser.parse(new File(TEMP_FILE), dh), new PropertyPermission("user.dir", "read"));
        Assert.assertTrue(dh.getVersion().equals(version), "Expected version is " + version + " actual version " + dh.getVersion());
        Assert.assertTrue(dh.getEncoding().equals(encoding), "Expected version is " + encoding + " actual version " + dh.getEncoding());
    }
}
