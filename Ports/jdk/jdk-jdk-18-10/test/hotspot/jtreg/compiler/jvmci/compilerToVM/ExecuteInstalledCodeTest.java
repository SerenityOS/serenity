/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @library ../common/patches
 * @ignore 8249621
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-BackgroundCompilation
 *                   compiler.jvmci.compilerToVM.ExecuteInstalledCodeTest
 */

package compiler.jvmci.compilerToVM;

import jdk.test.lib.Asserts;
import jdk.test.lib.util.Pair;
import jdk.test.lib.Utils;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import sun.hotspot.code.NMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ExecuteInstalledCodeTest {

    public static void main(String[] args) {
        ExecuteInstalledCodeTest test = new ExecuteInstalledCodeTest();
        List<CompileCodeTestCase> testCases = new ArrayList<>();
        testCases.addAll(CompileCodeTestCase.generate(/* bci = */ -1));
        testCases .stream()
                // ignore <init> of abstract class -- 8138793
                .filter(e -> !(e.executable instanceof Constructor
                        && Modifier.isAbstract(
                                e.executable.getDeclaringClass()
                                        .getModifiers())))
                .forEach(test::checkSanity);
    }

    private void checkSanity(CompileCodeTestCase testCase) {
        System.out.println(testCase);
        // to have a clean state
        testCase.deoptimize();
        Pair<Object, ? extends Throwable> reflectionResult;
        Object[] args = Utils.getNullValues(
                testCase.executable.getParameterTypes());
        reflectionResult = testCase.invoke(args);
        NMethod nMethod = testCase.compile();
        if (nMethod == null) {
            throw new Error(testCase + " : nmethod is null");
        }
        InstalledCode installedCode = testCase.toInstalledCode();
        Object result = null;
        Throwable expectedException = reflectionResult.second;
        boolean gotException = true;
        try {
            args = addReceiver(testCase, args);
            result = CompilerToVMHelper.executeInstalledCode(
                    args, installedCode);
            if (testCase.executable instanceof Constructor) {
                // <init> doesn't have return value, it changes receiver
                result = args[0];
            }
            gotException = false;
        } catch (InvalidInstalledCodeException e) {
            throw new AssertionError(
                    testCase + " : unexpected InvalidInstalledCodeException", e);
        } catch (Throwable t) {
            if (expectedException == null) {
                throw new AssertionError(testCase
                        + " : got unexpected execption : " + t.getMessage(), t);
            }

            if (expectedException.getClass() != t.getClass()) {
                System.err.println("exception from CompilerToVM:");
                t.printStackTrace();
                System.err.println("exception from reflection:");
                expectedException.printStackTrace();
                throw new AssertionError(String.format(
                        "%s : got unexpected different exceptions : %s != %s",
                        testCase, expectedException.getClass(), t.getClass()));
            }
        }

        Asserts.assertEQ(reflectionResult.first, result, testCase
                + " : different return value");
        if (!gotException) {
            Asserts.assertNull(expectedException, testCase
                    + " : expected exception hasn't been thrown");
        }
    }

    private Object[] addReceiver(CompileCodeTestCase testCase, Object[] args) {
        if (!Modifier.isStatic(testCase.executable.getModifiers())) {
            // add instance as 0th arg
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = testCase.receiver;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }
        return args;
    }
}
