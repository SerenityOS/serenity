/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032909
 * @summary Test for XSLT string-length function with complementary chars
 * @compile XSLT.java
 * @run main/othervm XSLT a_utf16.xml a_utf16.xsl 1270
 * @run main/othervm XSLT a_utf8.xml a_utf8.xsl 130
 * @run main/othervm XSLT a_windows1252.xml a_windows1252.xsl 200
 */

import java.io.ByteArrayOutputStream;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

public class XSLT {
    public static void main(String[] args) throws Exception {

        ByteArrayOutputStream resStream = new ByteArrayOutputStream();
        TransformerFactory trf = TransformerFactory.newInstance();
        Transformer tr = trf.newTransformer(new StreamSource(System.getProperty("test.src", ".")+"/"+args[1]));
        String res, expectedRes;
        tr.transform( new StreamSource(System.getProperty("test.src", ".")+"/"+args[0]), new StreamResult(resStream));
        res = resStream.toString();
        System.out.println("Transformation completed. Result:"+res);

        if (!res.replaceAll("\\s","").equals(args[2]))
            throw new RuntimeException("Incorrect transformation result. Expected:"+args[2]+" Observed:"+res);
    }
}
