/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4882798
 * @summary simple tests for getInitiatedClasses (does a newly loaded class show up?)
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build GetInitiatedClassesTest DummyClass
 * @run shell MakeJAR.sh basicAgent
 * @run main/othervm -javaagent:basicAgent.jar GetInitiatedClassesTest GetInitiatedClassesTest
 */

public class
GetInitiatedClassesTest
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for GetInitiatedClassesTest.
     * @param name
     */
    public GetInitiatedClassesTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new GetInitiatedClassesTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testGetInitiatedClasses();
    }

    public void
    testGetInitiatedClasses()
        throws  Throwable
    {
        ClassLoader loader = getClass().getClassLoader();

        Class[] classes = fInst.getInitiatedClasses(loader);
        assertNotNull(classes);

        Class dummy = loader.loadClass("DummyClass");
        assertEquals("DummyClass", dummy.getName());

        // double check that we can make an instance (just to prove the loader is kosher)
        Object testInstance = dummy.newInstance();

        Class[] classes2 = fInst.getInitiatedClasses(loader);
        assertNotNull(classes2);
        assertClassArrayContainsClass(classes2, dummy);
    }

}
