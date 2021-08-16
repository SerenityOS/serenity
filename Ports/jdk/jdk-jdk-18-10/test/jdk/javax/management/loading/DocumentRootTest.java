/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6500139
 * @summary Test parsing error when the mlet file is
 *          located in the web server's document root.
 * @author Luis-Miguel Alventosa
 *
 * @run clean DocumentRootTest
 * @run build DocumentRootTest
 * @run main DocumentRootTest
 */

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.management.loading.MLetContent;

public class DocumentRootTest {
    public static int test(URL documentBase, URL codeBase) {
        int error = 0;
        MLetContent mc = new MLetContent(
                documentBase,
                new HashMap<String,String>(),
                new ArrayList<String>(),
                new ArrayList<String>());
        System.out.println("\nACTUAL   DOCUMENT BASE = " + mc.getDocumentBase());
        System.out.println("EXPECTED DOCUMENT BASE = " + documentBase);
        if (!documentBase.equals(mc.getDocumentBase())) {
            System.out.println("ERROR: Wrong document base");
            error++;
        };
        System.out.println("ACTUAL   CODEBASE = " + mc.getCodeBase());
        System.out.println("EXPECTED CODEBASE = " + codeBase);
        if (!codeBase.equals(mc.getCodeBase())) {
            System.out.println("ERROR: Wrong code base");
            error++;
        };
        return error;
    }
    public static void main(String[] args) throws Exception {
        int error = 0;
        error += test(new URL("file:/mlet.txt"), new URL("file:/"));
        error += test(new URL("http://localhost/mlet.txt"), new URL("http://localhost/"));
        if (error > 0) {
            System.out.println("\nTest FAILED!\n");
            throw new IllegalArgumentException("Test FAILED!");
        } else {
            System.out.println("\nTest PASSED!\n");
        }
    }
}
