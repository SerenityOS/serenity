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
 * @summary test transformer add/remove pairs in sequence
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build TransformMethodTest
 * @run shell MakeJAR.sh redefineAgent
 * @run main/othervm -javaagent:redefineAgent.jar TransformMethodTest TransformMethodTest
 * @key randomness
 */
import java.lang.instrument.*;

public class
TransformMethodTest
    extends ATransformerManagementTestCase
{

    /**
     * Constructor for TransformMethodTest.
     * @param name
     */
    public TransformMethodTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new TransformMethodTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testTransform();
    }

    /**
     * Verify that the transformers can be added and removed correctly
     */
    public void
    testTransform()
    {
        for (int i = 0; i < kTransformerSamples.length; i++)
        {
            ClassFileTransformer transformer = getRandomTransformer();
            addTransformerToManager(fInst, transformer);
            verifyTransformers(fInst);
            removeTransformerFromManager(fInst, transformer, true);
        }
    }

}
