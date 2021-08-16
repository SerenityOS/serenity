/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 *  Cheesy class to run all of our test cases in a hard-coded fashion.
 *  Will be replaced by Sun's iterator that uses the source decorations to figure out what to do.
 */

import  java.lang.reflect.*;

public class FakeTestDriver  {

    private String[] fTestList = {
        "javafake.lang.instrument.AddTransformerTest",
        "javafake.lang.instrument.AppendToBootstrapClassPathTest",
        "javafake.lang.instrument.AppendToClassPathTest",
        "javafake.lang.instrument.GetAllLoadedClassesTest",
        "javafake.lang.instrument.GetInitiatedClassesTest",
        "javafake.lang.instrument.GetObjectSizeTest",
        "javafake.lang.instrument.NoTransformerAddedTest",
        "javafake.lang.instrument.NullTransformerAddTest",
        "javafake.lang.instrument.RedefineClassesTests",
        "javafake.lang.instrument.RemoveAbsentTransformerTest",
        "javafake.lang.instrument.RemoveTransformerTest",
        "javafake.lang.instrument.SingleTransformerTest",
        "javafake.lang.instrument.TransformerManagementThreadAddTests",
        "javafake.lang.instrument.TransformerManagementThreadRemoveTests",
        "javafake.lang.instrument.TransformMethodTest",
    };

    public static void
    main(String[] args) {
        (new FakeTestDriver()).runSuppliedTests(args);
    }

    private
    FakeTestDriver() {
    }

    private void
    runAllTests() {
        runSuppliedTests(fTestList);
    }

    private void
    runSuppliedTests(String[] classnames) {
        for (int x = 0; x < classnames.length; x++ ) {
            loadAndRunOneTest(classnames[x]);
        }
    }

    private void
    loadAndRunOneTest(String classname) {
        log("trying to run: " + classname);

        Class testclass = loadOneTest(classname);

        if ( testclass != null ) {
            boolean result = runOneTest(testclass);
            if ( result ) {
                log(classname + " SUCCEEDED");
            }
            else {
                log(classname + " FAILED");
            }
        }
        else {
            log(classname + " could not be loaded");
        }
    }

    private Class
    loadOneTest(String classname) {
        Class result = null;

        try {
            result = Class.forName(classname);
        }
        catch (Throwable t) {
            t.printStackTrace();
            result = null;
        }
        return result;
    }

    private boolean
    runOneTest(Class testclass) {
        Method mainMethod = null;

        try {
            String[]    forType = new String[0];
            mainMethod = testclass.getMethod("main",
                                                    new Class[] {
                                                        forType.getClass()
                                                     });
        }
        catch (Throwable t) {
            log(testclass.getName() + " is malformed");
            t.printStackTrace();
            return false;
        }

        try {
            mainMethod.invoke(null, new Object[] {new String[] {testclass.getName()}});
            return true;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    private void
    log(String m) {
        System.out.println(m);
    }
}
