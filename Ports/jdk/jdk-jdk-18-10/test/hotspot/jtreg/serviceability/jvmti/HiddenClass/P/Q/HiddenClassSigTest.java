/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 * @compile HiddenClassSigTest.java
 * @run main/othervm/native -agentlib:HiddenClassSigTest P.Q.HiddenClassSigTest
 */

package P.Q;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Utils;


interface HCInterf<T> {
    String hcMethod(T t);
}

class HiddenClassSig<T> implements HCInterf<T> {
    private String realTest() { return "HiddenClassSig: "; }

    public String hcMethod(T t) {
        String str = realTest();
        return str + t.toString();
    }
}

public class HiddenClassSigTest {
    private static void log(String str) { System.out.println(str); }

    private static final String HCName = "P/Q/HiddenClassSig.class";
    private static final Path CLASSES_DIR = Paths.get(Utils.TEST_CLASSES);
    private static final String LOG_PREFIX = "HiddenClassSigTest: ";

    static native void checkHiddenClass(Class klass, String sig);
    static native void checkHiddenClassArray(Class array, String sig);
    static native boolean checkFailed(); // get native agent failing status

    static {
        try {
            System.loadLibrary("HiddenClassSigTest");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load HiddenClassSigTest library");
            System.err.println("java.library.path: "
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static Class<?> defineHiddenClass(String classFileName) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(classFileName));
        Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
        return hc;
    }

    // print all name variations
    static void logClassInfo(Class<?> klass) {
        log("\n### Testing class: " + klass);
        log(LOG_PREFIX + "isHidden:  " + klass.isHidden());
        log(LOG_PREFIX + "getName:   " + klass.getName());
        log(LOG_PREFIX + "typeName:  " + klass.getTypeName());
        log(LOG_PREFIX + "toString:  " + klass.toString());
        log(LOG_PREFIX + "toGenStr:  " + klass.toGenericString());
        log(LOG_PREFIX + "elem type: " + klass.componentType());
    }

    private static final String HC_NAME = "P.Q.HiddenClassSig";
    private static final String HC_SUFFIX_REGEX = "0x[0-9a-f]+";

    static boolean checkName(Class<?> klass, String name, String toString) {
        boolean failed = false;
        String regex = "";
        Class<?> c = klass;

        // for an array add the prefix "[" for each dimension
        while (c.isArray()) {
            regex = "\\[" + regex;
            c = c.componentType();
        }
        // construct the expected name
        if (klass.isArray()) {
            regex += "L" + HC_NAME + "/" + HC_SUFFIX_REGEX + ";";
        } else {
            regex = HC_NAME + "/" + HC_SUFFIX_REGEX;
        }
        // check the name matches the expected
        if (!name.matches(regex)) {
            log("Test FAIL: result of Class::getName" + " \"" + name + "\" does not match " + regex);
            failed = true;
        }
        // check the string name matches the expected
        if (!toString.matches("class " + regex)) {
            log("Test FAIL: result of Class::toString" + " \"" + name + "\" does not match " + regex);
            failed = true;
        }
        return failed;
    }

    static boolean checkTypeName(Class<?> klass, String name) {
        boolean failed = false;
        // construct the expected type name
        String regex = HC_NAME + "/" + HC_SUFFIX_REGEX;
        Class<?> c = klass;

        // for an array add the suffix "[]" for each dimension
        while (c.isArray()) {
            c = c.componentType();
            regex = regex + "\\[\\]";
        }
        // check the type name matches the expected
        if (!name.matches(regex)) {
            log("Test FAIL: result of Class::getTypeName" + " \"" + name + "\" does not match " + regex);
            failed = true;
        }
        return failed;
    }

    static boolean checkGenericString(Class<?> klass, String name) {
        boolean failed = false;
        Class<?> c = klass;
        // construct the expected generic string
        String regex = HC_NAME + "/" + HC_SUFFIX_REGEX + "<T>";

        // add the expected name prefix for a non-array class
        if (!klass.isArray()) {
            regex = "class " + regex;
        }
        // for an array get the bottom component class
        while (c.isArray()) {
            c = c.componentType();
            regex = regex + "\\[\\]";
        }
        // check the generic string matches the expected
        if (!name.matches(regex)) {
            log("Test FAIL: result of Class::toGenericString" + " \"" + name + "\" does not match " + regex);
            failed = true;
        }
        return failed;
    }

    static boolean checkDescriptorString(Class<?> klass, String name) {
        boolean failed = false;
        // construct the expected descriptor string
        String regex = "L" + HC_NAME.replace('.', '/') + "." + HC_SUFFIX_REGEX + ";";
        Class<?> c = klass;

        // for array get the bottom component class
        while (c.isArray()) {
            regex = "\\[" + regex;
            c = c.componentType();
        }
        // check the descriptor string matches the expected
        if (!name.matches(regex)) {
            log("Test FAIL: result of Class::descriptorString" + " \"" + name + "\" does not match " + regex);
            failed = true;
        }
        return failed;
    }

    static boolean testClass(Class<?> klass) {
        boolean failed = false;
        logClassInfo(klass);

        // verify all name variations
        failed |= checkName(klass, klass.getName(), klass.toString());
        failed |= checkTypeName(klass, klass.getTypeName());
        failed |= checkGenericString(klass, klass.toGenericString());
        failed |= checkDescriptorString(klass, klass.descriptorString());

        // an array class is never hidden
        if (klass.isArray() && klass.isHidden()) {
            log("Test FAIL: an array class is never hidden");
            failed = true;
        }

        // verify hidden class array or class by the native agent
        if (klass.isArray()) {
            checkHiddenClassArray(klass, klass.descriptorString());
        } else {
            checkHiddenClass(klass, klass.descriptorString());
        }
        return failed;
    }

    public static void main(String args[]) throws Exception {
        log(LOG_PREFIX + "started");

        // define a hidden class to test
        Class<?> hc = defineHiddenClass(HCName);

        // allocate a hidden class instance to test
        HCInterf<String> testObj = (HCInterf<String>)hc.newInstance();

        String str = testObj.hcMethod("Test generic hidden class");
        log(LOG_PREFIX + "hc.hcMethod() returned string: " + str);

        // test all hidden class name/signature variations
        boolean failed = testClass(hc);

        // test all hidden class array name/signature variations
        Class<?> hcArr = hc.arrayType();
        failed |= testClass(hcArr);

        // test all hidden class double array name/signature variations
        Class<?> hcArrArr = hcArr.arrayType();
        failed |= testClass(hcArrArr);

        if (failed) { // check the java part failing status
          throw new RuntimeException("FAIL: failed status from java part");
        }
        if (checkFailed()) { // check the native agent failing status
          throw new RuntimeException("FAIL: failed status from native agent");
        }
        log(LOG_PREFIX + "finished");
    }
}
