/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @library /test/lib /java/lang/invoke/common
 * @compile MethodHandlesTest.java MethodHandlesInsertArgumentsTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesInsertArgumentsTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.MethodType.methodType;

import static org.junit.Assert.*;

public class MethodHandlesInsertArgumentsTest extends MethodHandlesTest {

    @Test // SLOW
    public void testInsertArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testInsertArguments0);
    }

    public void testInsertArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("insertArguments");
        for (int nargs = 0; nargs < 50; nargs++) {
            if (CAN_TEST_LIGHTLY && nargs > 11)  break;
            for (int ins = 0; ins <= nargs; ins++) {
                if (nargs > 10 && ins > 4 && ins < nargs-4 && ins % 10 != 3)
                    continue;
                for (int pos = 0; pos <= nargs; pos++) {
                    if (nargs > 10 && pos > 4 && pos < nargs-4 && pos % 10 != 3)
                        continue;
                    if (CAN_TEST_LIGHTLY && pos > 2 && pos < nargs-2)  continue;
                    testInsertArguments(nargs, pos, ins);
                }
            }
        }
    }

    void testInsertArguments(int nargs, int pos, int ins) throws Throwable {
        countTest();
        MethodHandle target = varargsArray(nargs + ins);
        Object[] args = randomArgs(target.type().parameterArray());
        List<Object> resList = Arrays.asList(args);
        List<Object> argsToPass = new ArrayList<>(resList);
        List<Object> argsToInsert = argsToPass.subList(pos, pos + ins);
        if (verbosity >= 3)
            System.out.println("insert: "+argsToInsert+" @"+pos+" into "+target);
        @SuppressWarnings("cast")  // cast to spread Object... is helpful
        MethodHandle target2 = MethodHandles.insertArguments(target, pos,
                (Object[]/*...*/) argsToInsert.toArray());
        argsToInsert.clear();  // remove from argsToInsert
        Object res2 = target2.invokeWithArguments(argsToPass);
        Object res2List = Arrays.asList((Object[])res2);
        if (verbosity >= 3)
            System.out.println("result: "+res2List);
        assertEquals(resList, res2List);
    }

    private static MethodHandle methodHandle = null;
    static {
        try {
            methodHandle = MethodHandles.lookup().findVirtual(
                                               MethodHandlesInsertArgumentsTest.class,
                                               "testMethod",
                                               methodType(void.class, String.class, String.class));
        } catch(ReflectiveOperationException ex) {
            throw new InternalError(ex);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertArgumentsInvalidPos() {
        countTest();
        MethodHandles.insertArguments(methodHandle, -1, "First", "Second");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertArgumentsTooManyParams() {
        countTest();
        MethodHandles.insertArguments(methodHandle, 1, "First", "Second", "Third");
    }

    @Test(expected = ClassCastException.class)
    public void testInsertArgumentsPosZero() {
        countTest();
        MethodHandles.insertArguments(methodHandle, 0, "First");
    }

    @Test(expected = ClassCastException.class)
    public void testInsertArgumentsIncorrectParam() {
        countTest();
        MethodHandles.insertArguments(methodHandle, 1, "First", new Object());
    }

    void testMethod(String a, String b) {
    }
}
