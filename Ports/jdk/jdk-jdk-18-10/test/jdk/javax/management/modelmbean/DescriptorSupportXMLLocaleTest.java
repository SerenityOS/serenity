/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7065236
 * @summary Test for locale insensitive strings in DescriptorSupport class
 * @author Harsha Wardhana B
 *
 * @run clean DescriptorSupportXMLLocaleTest
 * @run build DescriptorSupportXMLLocaleTest
 * @run main DescriptorSupportXMLLocaleTest
 */
import java.util.Locale;
import javax.management.modelmbean.DescriptorSupport;

public class DescriptorSupportXMLLocaleTest {

    public static void main(String[] args) throws Exception {
        boolean failed = false;
        String xmlDesc = "<DESCRIPTOR>"
                + "<FIELD name=\"field1\" value=\"dummy\">"
                + "</FIELD>"
                + "</DESCRIPTOR>";
        Locale loc = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            new DescriptorSupport(xmlDesc);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            failed = true;
        }finally{
            Locale.setDefault(loc);
        }

        if (!failed) {
            System.out.println("OK: all tests passed");
        } else {
            System.out.println("TEST FAILED");
            throw new IllegalArgumentException("Test Failed");
        }
    }
}

