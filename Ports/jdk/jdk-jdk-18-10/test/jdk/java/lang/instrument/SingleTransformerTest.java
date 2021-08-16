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
 * @summary simple test with one transformer (makes sure it gets called)
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build SingleTransformerTest
 * @run shell MakeJAR.sh redefineAgent
 * @run main/othervm -javaagent:redefineAgent.jar SingleTransformerTest SingleTransformerTest
 * @key randomness
 */
public class
SingleTransformerTest
    extends ATransformerManagementTestCase
{

    /**
     * Constructor for SingleTransformerTest.
     * @param name
     */
    public SingleTransformerTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new SingleTransformerTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        beVerbose(); // We are seeing problems on this test -- print what is happenning
        testOneTransformer();
    }


    /**
     * Add and check just one transformer to the manager
     */
    public void
    testOneTransformer()
    {
        addTransformerToManager(fInst, getRandomTransformer());
        verifyTransformers(fInst);
    }

}
