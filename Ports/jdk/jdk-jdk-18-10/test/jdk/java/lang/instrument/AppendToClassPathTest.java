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
 * @summary simple test for the Class-Path manifest attribute
 * @author Gabriel Adauto, Wily Technology; Robert Field, Sun Microsystems
 *
 * @run build AppendToClassPathTest
 * @run shell AppendToClassPathSetUp.sh
 * @run shell MakeJAR.sh classpathAgent
 * @run main/othervm -javaagent:classpathAgent.jar AppendToClassPathTest AppendToClassPathTest
 */

import java.io.*;

public class
AppendToClassPathTest
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for AppendToClassPathTest.
     * @param name
     */
    public AppendToClassPathTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new AppendToClassPathTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testAppendToClassPath();
    }


    public void
    testAppendToClassPath()
        throws  IOException,
                ClassNotFoundException
    {
        Class sentinelClass;
        ClassLoader loader = this.getClass().getClassLoader();

        // load the "hidden" class, it should be loaded by the system loader
        sentinelClass = loader.loadClass("ExampleForClassPath");
        assertTrue(sentinelClass.getClassLoader() == ClassLoader.getSystemClassLoader());
    }

}
