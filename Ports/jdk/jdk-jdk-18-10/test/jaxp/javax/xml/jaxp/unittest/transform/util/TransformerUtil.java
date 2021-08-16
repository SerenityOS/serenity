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

import static jaxp.library.JAXPTestUtilities.USER_DIR;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

public abstract class TransformerUtil {

    protected String type;

    protected final String TEMP_FILE = USER_DIR + "tmp.xml";

    public abstract Source prepareSource(InputStream is) throws Exception;

    public abstract Result prepareResult() throws Exception;

    public abstract void checkResult(Result result, String version) throws Exception;

    public void checkResult(Result result, String version, String encoding) throws Exception {
        checkResult(result, version);
    }

    public DocumentBuilder getDomParser() throws Exception {
        DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
        return docBF.newDocumentBuilder();
    }
}
