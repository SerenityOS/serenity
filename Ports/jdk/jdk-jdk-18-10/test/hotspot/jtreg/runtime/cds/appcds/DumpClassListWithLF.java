/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Tests the format checking of LF_RESOLVE in classlist.
 *
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run driver DumpClassListWithLF
 */

public class DumpClassListWithLF extends ClassListFormatBase {
    static final String REPLACE_OK = "Replaced class java/lang/invoke/DirectMethodHandle$Holder";

    public static void main(String[] args) throws Throwable {
        String appJar = JarBuilder.getOrCreateHelloJar();
        //
        // Note the class regeneration via jdk/internal/misc/CDS.generateLambdaFormHolderClasses(String[] lines)
        // Whether the regeneration successes or fails, the dump should pass. Only the message can be checked for result.
        //
        // 1. With correct line format.
        dumpShouldPass(
            "TESTCASE 1: With correct line format",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic LL_I"),
                REPLACE_OK);

        // 2. The line with incorrect (less) number of items.
        dumpShouldPass(
            "TESTCASE 2: With incorrect (less) number of items",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic"),
                "Incorrect number of items in the line: 3");
        // 3. The two lines with non existed class name, since only 4 holder classes recognizable, all other names will be rejected.
        dumpShouldPass(
            "TESTCASE 3: With incorrect class name will be rejected",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] my.nonexist.package.MyNonExistClassName$holder invokeStatic LL_I"),
                "Invalid holder class name: my.nonexist.package.MyNonExistClassName$holder" );
        // 4. The two lines with arbitrary invoke names is OK. The method type will not be added.
        dumpShouldPass(
            "TESTCASE 4: With incorrect invoke names is OK",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeNothing LL_I"),
                REPLACE_OK);
        // 5. The line with worng signature format of return type, will be rejected
        dumpShouldPass(
            "TESTCASE 5: With incorrect signature format of return type will be rejected",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic LL_G"),
                "Invalid method type: LL_G");
        // 6. The line with worng signature format of arg types, will be rejected
        dumpShouldPass(
            "TESTCASE 6: With incorrect signature format of arg types will be rejected",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic MGLL_I"),
                "Invalid method type: MGLL_I");
        // 7. The line with worng prefix will ge rejected
        dumpShouldPass(
            "TESTCASE 7: With incorrect LF format, the line will be rejected",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [LF_XYRESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic LL_I"),
                "Wrong prefix: [LF_XYRESOLVE]");
        // 8. The line with correct species format
        dumpShouldPass(
            "TESTCASE 8: With correct correct species format",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [SPECIES_RESOLVE] java.lang.invoke.BoundMethodHandle$Species_L"),
                REPLACE_OK);
       // 9. The line with incorrect species length is not OK
        dumpShouldPass(
            "TESTCASE 9: With incorrect species length is not OK",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker [SPECIES_RESOLVE] java.lang.invoke.BoundMethodHandle$Species_L L"),
                "Incorrect number of items in the line: 3");
        // 10. The line with incorrect (less) number of items.
        dumpShouldFail(
            "TESTCASE 10: With incorrect @lambda-form-invoker tag",
            appJar, classlist(
                "Hello",
                "@lambda-form-invoker-xxx [LF_RESOLVE] java.lang.invoke.DirectMethodHandle$Holder invokeStatic"),
                "Invalid @ tag at the beginning of line \"@lambda-form-invoker-xxx\" line #2");
    }
}
