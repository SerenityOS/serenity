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
 * @summary round-trip test for getObjectSize (does it return, and is the result non-zero?)
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build GetObjectSizeTest
 * @run shell MakeJAR.sh basicAgent
 * @run main/othervm -javaagent:basicAgent.jar GetObjectSizeTest GetObjectSizeTest
 */
import java.util.*;

public class
GetObjectSizeTest
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for GetObjectSizeTest.
     * @param name
     */
    public GetObjectSizeTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new GetObjectSizeTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testGetObjectSize();
    }

    /*
     *  Lame test just to show we can do the roundtrip
     */
    public void
    testGetObjectSize()
    {
        Object[] objects = new Object[] {
            "Hello World",
            new Integer(8),
            this,
            new StringBuffer("Another test object"),
            new Vector(99),
            // Add more objects here
            };
        for (int i = 0; i < objects.length; i++)
        {
            Object o = objects[i];
            long size = fInst.getObjectSize(o);
            assertTrue(size > 0);
        }
    }

}
