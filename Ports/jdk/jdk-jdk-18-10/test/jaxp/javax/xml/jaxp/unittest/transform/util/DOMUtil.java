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

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.testng.Assert;
import org.w3c.dom.Document;

public class DOMUtil extends TransformerUtil {

    DocumentBuilder docBuilder = null;

    private static DOMUtil instance = null;

    /** Creates a new instance of DOMUtil */
    private DOMUtil() throws Exception {
        if (docBuilder == null)
            docBuilder = getDomParser();
    }

    public static synchronized DOMUtil getInstance() throws Exception {
        if (instance == null)
            instance = new DOMUtil();
        return instance;
    }

    public Source prepareSource(InputStream is) throws Exception {
        docBuilder.reset();
        Document doc1 = docBuilder.parse(is);
        return new DOMSource(doc1);
    }

    public Result prepareResult() {
        Document target = docBuilder.newDocument();
        return new DOMResult(target);
    }

    public void checkResult(Result result, String version) {
        String resultVersion = ((Document) ((DOMResult) result).getNode()).getXmlVersion();
        Assert.assertTrue(version.equals(resultVersion), "Expected XML Version is 1.1, but actual version " + resultVersion);
    }
}
