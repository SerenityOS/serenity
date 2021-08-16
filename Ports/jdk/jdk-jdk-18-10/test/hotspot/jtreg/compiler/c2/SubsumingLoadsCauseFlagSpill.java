/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @bug 8209639
 * @summary assert failure in coalesce.cpp: attempted to spill a non-spillable item
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,SubsumingLoadsCauseFlagSpill::not_inlined -Xmx1024m SubsumingLoadsCauseFlagSpill
 *
 */

public class SubsumingLoadsCauseFlagSpill {
    private static Object field;
    private static boolean do_throw;
    private static volatile boolean barrier;

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            do_throw = true;
            field = null;
            test(0);
            do_throw = false;
            field = new Object();
            test(0);
        }
    }

    private static float test(float f) {
        Object v = null;
        try {
            not_inlined();
            v = field;
        } catch (MyException me) {
            v = field;
            barrier = true;
        }
        if (v == null) {
            return f * f;
        }
        return f;
    }

    private static void not_inlined() throws MyException{
        if (do_throw) {
            throw new MyException();
        }
    }

    private static class MyException extends Throwable {
    }
}
