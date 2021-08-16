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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.testng.Assert;
import org.xml.sax.InputSource;

import transform.VersionDefaultHandler;

public class SAXUtil extends TransformerUtil {

    private static SAXUtil instance = null;

    /** Creates a new instance of SAXUtil */
    private SAXUtil() {

    }

    public static synchronized SAXUtil getInstance() throws Exception {
        if (instance == null)
            instance = new SAXUtil();
        return instance;
    }

    public Source prepareSource(InputStream is) throws Exception {
        return new SAXSource(new InputSource(is));
    }

    public Result prepareResult() throws Exception {
        VersionDefaultHandler dh = new VersionDefaultHandler();
        return new SAXResult(dh);
    }

    public void checkResult(Result result, String inputVersion) throws Exception {
        String version = ((VersionDefaultHandler) ((SAXResult) result).getHandler()).getVersion();
        Assert.assertTrue(inputVersion.equals(version), "Expected XML Version is 1.1, but actual version " + version);
    }

    public void checkResult(Result result, String inputVersion, String encoding) throws Exception {
        checkResult(result, inputVersion);
        String resultEncoding = ((VersionDefaultHandler) ((SAXResult) result).getHandler()).getEncoding();
        Assert.assertTrue(encoding.equals(resultEncoding), "Expected XML Version is " + encoding + " , but actual  encoding " + resultEncoding);
    }
}
