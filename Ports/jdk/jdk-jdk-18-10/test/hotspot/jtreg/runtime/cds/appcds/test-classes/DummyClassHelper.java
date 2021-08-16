/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.*;
import java.lang.reflect.*;
import jdk.test.lib.cds.CDSTestUtils;
import sun.hotspot.WhiteBox;

public class DummyClassHelper {
    static void checkDummyMethod(Class<?> cls, String className) {
        Method m = null;
        try {
            m = cls.getMethod("thisClassIsDummy");
            throw new java.lang.RuntimeException(className +
                " should be loaded from the jimage and should not have the thisClassIsDummy() method.");
        } catch(NoSuchMethodException ex) {
            System.out.println(ex.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        String[] classNames = {args[0], args[1]};
        Class cls = null;
        boolean doWBCheck = (args.length == 3);
        WhiteBox wb = null;
        if (doWBCheck) {
            wb = WhiteBox.getWhiteBox();
        }
        for (int i = 0; i < classNames.length; i++) {
            cls = Class.forName(classNames[i]);
            checkDummyMethod(cls, classNames[i]);
            if (doWBCheck) {
                // FIXME: for dynamic archive, the class loaded from the
                // bootclasspath jar during dump time is not loaded from the
                // archive during run time.
                if (!CDSTestUtils.isDynamicArchive()) {
                    if (!wb.isSharedClass(cls)) {
                        throw new java.lang.RuntimeException(classNames[i] +
                            ".class should be in shared space.");
                    }
                }
            }
        }
    }
}
