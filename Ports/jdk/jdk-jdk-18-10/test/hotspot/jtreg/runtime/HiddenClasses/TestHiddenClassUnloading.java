/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test unloading of hidden classes.
 * @modules java.management
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:-BackgroundCompilation
 *      compiler.classUnloading.hiddenClass.TestHiddenClassUnloading
 */

package compiler.classUnloading.hiddenClass;

import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import compiler.whitebox.CompilerWhiteBoxTest;

public class TestHiddenClassUnloading {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * We override hashCode here to be able to access this implementation
     * via an Object reference (we cannot cast to TestHiddenClassUnloading).
     */
    @Override
    public int hashCode() {
        return 42;
    }

    /**
     * Does some work by using the hiddenClass.
     * @param hiddenClass Class performing some work (will be unloaded)
     */
    static private void doWork(Class<?> hiddenClass) throws InstantiationException, IllegalAccessException {
        // Create a new instance
        Object anon = hiddenClass.newInstance();
        // We would like to call a method of hiddenClass here but we cannot cast because the class
        // was loaded by a different class loader. One solution would be to use reflection but since
        // we want C2 to implement the call as an IC we call Object::hashCode() here which actually
        // calls hiddenClass::hashCode(). C2 will then implement this call as an IC.
        if (anon.hashCode() != 42) {
            new RuntimeException("Work not done");
        }
    }

    /**
     * Makes sure that method is compiled by forcing compilation if not yet compiled.
     * @param m Method to be checked
     */
    static private void makeSureIsCompiled(Method m) {
        // Make sure background compilation is disabled
        if (WHITE_BOX.getBooleanVMFlag("BackgroundCompilation")) {
            throw new RuntimeException("Background compilation enabled");
        }

        // Check if already compiled
        if (!WHITE_BOX.isMethodCompiled(m)) {
            // If not, try to compile it with C2
            if(!WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
                // C2 compiler not available, try to compile with C1
                WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE);
            }
            // Because background compilation is disabled, method should now be compiled
            if(!WHITE_BOX.isMethodCompiled(m)) {
                throw new RuntimeException(m + " not compiled");
            }
        }
    }

    /**
     * This test creates stale Klass* metadata referenced by a compiled IC.
     *
     * The following steps are performed:
     * (1) A hidden version of TestHiddenClassUnloading is loaded by a custom class loader
     * (2) The method doWork that calls a method of the hidden class is compiled. The call
     *     is implemented as an IC referencing Klass* metadata of the hidden class.
     * (3) Unloading of the hidden class is enforced. The IC now references dead metadata.
     */
    static public void main(String[] args) throws Exception {
        // (1) Load a hidden version of this class using method lookup.defineHiddenClass().
        String rn = TestHiddenClassUnloading.class.getSimpleName() + ".class";
        URL classUrl = TestHiddenClassUnloading.class.getResource(rn);
        URLConnection connection = classUrl.openConnection();

        int length = connection.getContentLength();
        byte[] classBytes = connection.getInputStream().readAllBytes();
        if (length != -1 && classBytes.length != length) {
            throw new IOException("Expected:" + length + ", actual: " + classBytes.length);
        }

        Lookup lookup = MethodHandles.lookup();
        Class<?> hiddenClass = lookup.defineHiddenClass(classBytes, true, NESTMATE).lookupClass();

        // (2) Make sure all paths of doWork are profiled and compiled
        for (int i = 0; i < 100000; ++i) {
            doWork(hiddenClass);
        }

        // Make sure doWork is compiled now
        Method doWork = TestHiddenClassUnloading.class.getDeclaredMethod("doWork", Class.class);
        makeSureIsCompiled(doWork);

        // (3) Throw away reference to hiddenClass to allow unloading
        hiddenClass = null;

        // Force garbage collection to trigger unloading of hiddenClass
        WHITE_BOX.fullGC();
    }
}
