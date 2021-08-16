/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8024707
 * @summary Test for XSLT extension function with 1 element sized nodelist
 * @compile TestFunc.java XSLT.java
 * @run main/othervm XSLT
 * @author aleksej.efimov@oracle.com
 */

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.ByteArrayOutputStream;

public class XSLT {
    static final String XMLTOTRANSFORM = "/in.xml";
    static final String XSLTRANSFORMER = "/test.xsl";
    static final String EXPECTEDRESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>inp1_1";

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream resStream = new ByteArrayOutputStream();
        TransformerFactory trf = TransformerFactory.newInstance();
        Transformer tr = trf.newTransformer( new StreamSource(System.getProperty("test.src", ".")+XSLTRANSFORMER));
        tr.transform( new StreamSource(System.getProperty("test.src", ".")+XMLTOTRANSFORM), new StreamResult(resStream));
        System.out.println("Transformation completed. Result:"+resStream.toString());
        if (!resStream.toString().equals(EXPECTEDRESULT))
            throw new RuntimeException("Incorrect transformation result");
    }
}
