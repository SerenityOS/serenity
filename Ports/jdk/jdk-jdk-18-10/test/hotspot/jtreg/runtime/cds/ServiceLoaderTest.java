/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244778
 * @summary Make sure that the ServicesCatalogs for boot/platform/app loaders are properly archived.
 * @requires vm.cds
 * @modules java.naming
 * @library /test/lib
 * @run driver ServiceLoaderTest
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import javax.naming.spi.InitialContextFactory;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;

public class ServiceLoaderTest {
    public static void main(String[] args) throws Exception {
        CDSOptions opts = new CDSOptions();

        CDSTestUtils.createArchiveAndCheck(opts);

        // Some mach5 tiers run with -vmoptions:-Xlog:cds=debug. This would cause the outputs to mismatch.
        // Force -Xlog:cds=warning to supress the CDS logs.
        opts.setUseVersion(false);
        opts.addSuffix("-showversion", "-Xlog:cds=warning", "ServiceLoaderApp");
        OutputAnalyzer out1 = CDSTestUtils.runWithArchive(opts);

        opts.setXShareMode("off");
        OutputAnalyzer out2 = CDSTestUtils.runWithArchive(opts);

        compare(out1, out2);
    }

    static void compare(OutputAnalyzer out1, OutputAnalyzer out2) {
        String[] arr1 = splitLines(out1);
        String[] arr2 = splitLines(out2);

        int max = arr1.length > arr2.length ? arr1.length : arr2.length;
        for (int i = 0; i < max; i++) {
            if (i >= arr1.length) {
                mismatch(i, "<EOF>", arr2[i]);
            }
            if (i >= arr2.length) {
                mismatch(i, arr1[i], "<EOF>");
            }
            if (!arr1[i].equals(arr2[i])) {
                mismatch(i, arr1[i], arr2[i]);
            }
        }
    }

    static String[] splitLines(OutputAnalyzer out) {
        return out.getStdout().split("\n");
    }

    static void mismatch(int i, String s1, String s2) {
        System.out.println("Mismatched line: " + i);
        System.out.println("cds on : " + s1);
        System.out.println("cds off: " + s2);
        throw new RuntimeException("Mismatched line " + i + ": \"" + s1 + "\" vs \"" + s2 + "\"");
    }
}

class ServiceLoaderApp {
    public static void main(String args[]) throws Exception {
        doTest(ToolProvider.class);
        doTest(InitialContextFactory.class);
    }

    static void doTest(Class c) throws Exception {
        System.out.println("============================================================");
        System.out.println("Testing : " + c.getName());
        System.out.println("============================================================");

        print_loader("default",         ServiceLoader.load(c));
        print_loader("null loader",     ServiceLoader.load(c, null));
        print_loader("platform loader", ServiceLoader.load(c, ServiceLoaderApp.class.getClassLoader().getParent()));
        print_loader("system loader",   ServiceLoader.load(c, ServiceLoaderApp.class.getClassLoader()));
    }

    static void print_loader(String testCase, ServiceLoader loader) throws Exception {
        System.out.println("[TEST CASE] " + testCase);
        System.out.println("[svcloader] " + asString(loader));
        Iterator it = loader.iterator();
        ArrayList<String> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(asString(it.next().toString()));
        }
        Collections.sort(list);
        for (String s : list) {
            System.out.println(s);
        }
    }

    static String asString(Object o) {
        String s = o.toString();
        int n = s.indexOf("@");
        if (n >= 0) {
            s = s.substring(0, n);
        }
        return s;
    }
}
