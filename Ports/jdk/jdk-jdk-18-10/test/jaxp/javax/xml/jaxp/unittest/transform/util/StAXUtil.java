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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.testng.Assert;

import transform.TransformerUtilFactory;
import transform.VersionEventWriter;

public class StAXUtil extends TransformerUtil {

    private static StAXUtil instance = null;

    /** Creates a new instance of StAXUtil */
    private StAXUtil() {
    }

    public static synchronized StAXUtil getInstance() throws Exception {
        if (instance == null)
            instance = new StAXUtil();
        return instance;
    }

    public Source prepareSource(InputStream is) throws Exception {
        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(is);
        return new StAXSource(reader);
    }

    public Result prepareResult() throws Exception {
        VersionEventWriter writer = new VersionEventWriter();
        return new StAXResult(writer);
    }

    public void checkResult(Result staxResult, String version) throws Exception {
        VersionEventWriter writer = (VersionEventWriter) ((StAXResult) staxResult).getXMLEventWriter();
        Assert.assertTrue(writer.getVersion().equals(version), "Expected XML Version is 1.1, but actual version " + writer.getVersion());
    }

    public void checkResult(Result staxResult, String version, String encoding) throws Exception {
        VersionEventWriter writer = (VersionEventWriter) ((StAXResult) staxResult).getXMLEventWriter();
        Assert.assertTrue(writer.getVersion().equals(version), "Expected XML Version is 1.1, but actual version " + writer.getVersion());
        Assert.assertTrue(writer.getEncoding().equals(encoding), "Expected encoding is " + encoding + ", but actual encoding " + writer.getEncoding());
    }

    public Source prepareStreamSource(InputStream is) throws Exception {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
        return new StAXSource(reader);
    }

    public Result prepareStreamResult() throws Exception {
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(TEMP_FILE));
        return new StAXResult(writer);
    }

    public void checkStreamResult(Result staxResult, String version) throws Exception {
        ((StAXResult) staxResult).getXMLStreamWriter().close();
        ((StreamUtil) TransformerUtilFactory.getUtil(TransformerUtilFactory.STREAM)).checkStream(new FileInputStream(TEMP_FILE), version);
    }
}
