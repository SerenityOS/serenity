/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/ShortCircuitTest.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.ShortCircuitTest.ShortCircuitTest
 */

package jit.FloatingPoint.gen_math.ShortCircuitTest;

////////////////////////////////////////////////////////////////////////////////
// A complete set of tests for the binary Java operators {&&, ||, &, |} to
// ensure that (C-style) short circuit evaluation is used where required, and
// also is not used where prohibited.  Checks has been constructed carefully to
// be independent of the operators under scrutiny, so that passing these tests
// constitutes validation of those operators.
//
// The exit status is the number of errors found, at most sixteen.
////////////////////////////////////////////////////////////////////////////////

// If the first test below, a check such as
//
//   if(t == 2 && f == 0){...
//
// would be inappropriate, since it contains the item under scrutiny, namely
// short circuit evaluation of AND (&&).  Avoid it by slightly tedious
// if-then-else statements, and similarly for the rest of the file.

import nsk.share.TestFailure;

public class ShortCircuitTest {
    private static int f = 0, t = 0;

    // constr
    public ShortCircuitTest(){
    }

    public boolean f(){
        f++;
        return false;
    }

    public boolean t(){
        t++;
        return true;
    }

    public static void main(String[] args){
        ShortCircuitTest s = new ShortCircuitTest();
        int errors = 0;
        ////////////////////////////////////////////////////////////////////////
        // &&
        f = t = 0;
        if(s.t() && s.t()){
            if(t == 2){
                if(f == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true && true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true && true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true && true");
            errors++;
        }

        f = t = 0;
        if(s.t() && s.f()){
            System.out.println("Short circuit error: true && false");
            errors++;
        }else{
            if(t == 1){
                if(f == 1){
                    //  System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true && false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true && false");
                errors++;
            }
        }

        f = t = 0;
        if(s.f() && s.t()){
            System.out.println("Short circuit error: false && true");
            errors++;
        }else{
            if(f == 1){
                if(t == 0){
                    //  System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false && true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false && true");
                errors++;
            }
        }

        f = t = 0;
        if(s.f() && s.f()){
            System.out.println("Short circuit error: false && false");
            errors++;
        }else{
            if(f == 1){
                if(t == 0){
                    //  System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false && false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false && false");
                errors++;
            }
        }
        ////////////////////////////////////////////////////////////////////////
        // ||
        f = t = 0;
        if(s.t() || s.t()){
            if(t == 1){
                if(f == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true || true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true || true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true || true");
            errors++;
        }

        f = t = 0;
        if(s.t() || s.f()){
            if(t == 1){
                if(f == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true || false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true || false");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true || false");
            errors++;
        }

        f = t = 0;
        if(s.f() || s.t()){
            if(f == 1){
                if(t == 1){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false || true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false || true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: false || true");
            errors++;
        }

        f = t = 0;
        if(s.f() || s.f()){
            System.out.println("Short circuit error: false || false");
            errors++;
        }else{
            if(f == 2){
                if(t == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false || false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false || false");
                errors++;
            }
        }
        ////////////////////////////////////////////////////////////////////////
        // &
        f = t = 0;
        if(s.t() & s.t()){
            if(t == 2){
                if(f == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true & true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true & true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true & true");
            errors++;
        }

        f = t = 0;
        if(s.t() & s.f()){
            System.out.println("Short circuit error: true & false");
            errors++;
        }else{
            if(t == 1){
                if(f == 1){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true & false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true & false");
                errors++;
            }
        }

        f = t = 0;
        if(s.f() & s.t()){
            System.out.println("Short circuit error: false & true");
            errors++;
        }else{
            if(f == 1){
                if(t == 1){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false & true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false & true");
                errors++;
            }
        }

        f = t = 0;
        if(s.f() & s.f()){
            System.out.println("Short circuit error: false & false");
            errors++;
        }else{
            if(f == 2){
                if(t == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false & false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false & false");
                errors++;
            }
        }
        ////////////////////////////////////////////////////////////////////////
        // |
        f = t = 0;
        if(s.t() | s.t()){
            if(t == 2){
                if(f == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true | true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true | true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true | true");
            errors++;
        }

        f = t = 0;
        if(s.t() | s.f()){
            if(t == 1){
                if(f == 1){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: true | false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: true | false");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: true | false");
            errors++;
        }

        f = t = 0;
        if(s.f() | s.t()){
            if(f == 1){
                if(t == 1){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false | true");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false | true");
                errors++;
            }
        }else{
            System.out.println("Short circuit error: false | true");
            errors++;
        }

        f = t = 0;
        if(s.f() | s.f()){
            System.out.println("Short circuit error: false | false");
            errors++;
        }else{
            if(f == 2){
                if(t == 0){
                    // System.out.println("PASS");
                }else{
                    System.out.println("Short circuit error: false | false");
                    errors++;
                }
            }else{
                System.out.println("Short circuit error: false | false");
                errors++;
            }
        }
        if (errors > 0)
            throw new TestFailure("Test failed: got " + errors + "errors");
    }
}
