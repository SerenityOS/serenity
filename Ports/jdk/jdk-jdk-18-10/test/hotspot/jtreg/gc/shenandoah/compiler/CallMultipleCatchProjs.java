/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8231405
 * @summary barrier expansion breaks if barrier is right after call to rethrow stub
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:CompileOnly=CallMultipleCatchProjs::test -Xcomp -Xverify:none -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC CallMultipleCatchProjs
 *
 */

public class CallMultipleCatchProjs {
    private static A field = new A();

    public static void main(String[] args) throws Exception {
        Exception3 exception3 = new Exception3();
        test(new Exception2());
    }

    static int test(Exception exception) throws Exception {
        try {
            throw exception;
        } catch (Exception1 e1) {
            return 1;
        } catch (Exception2 e2) {
            return field.i + 2;
        } catch (Exception3 e3) {
            return field.i + 3;
        }
    }

    private static class Exception1 extends Exception {
    }

    private static class Exception2 extends Exception {
    }

    private static class Exception3 extends Exception {
    }

    private static class A {
        public int i;
    }
}
