/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.bmi;

/**
 * Expression that should be replaced by particular instrinsic
 * or intruction during compilation.
 */
public abstract class Expr {

    public static class MemI {
        public MemI(int i) {
            this.value = i;
        }

        public int value;
    }

    public static class MemL {
        public MemL(long l) {
            this.value = l;
        }

        public long value;
    }

    public boolean isUnaryArgumentSupported() {
        return false;
    }

    public boolean isIntExprSupported() {
        return false;
    }

    public boolean isBinaryArgumentSupported() {
        return false;
    }

    public boolean isLongExprSupported() {
        return false;
    }

    public boolean isIntToLongExprSupported() {
        return false;
    }

    public boolean isMemExprSupported() {
        return false;
    }

    public int intExpr(int reg) {
        throw new UnsupportedOperationException();
    }

    public int intExpr(MemI mem) {
        throw new UnsupportedOperationException();
    }

    public int intExpr(int a, int b) {
        throw new UnsupportedOperationException();
    }

    public int intExpr(int a, MemI b) {
        throw new UnsupportedOperationException();
    }

    public int intExpr(MemI a, int b) {
        throw new UnsupportedOperationException();
    }

    public int intExpr(MemI a, MemI b) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(long reg) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(MemL mem) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(long a, long b) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(long a, MemL b) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(MemL a, long b) {
        throw new UnsupportedOperationException();
    }

    public long longExpr(MemL a, MemL b) {
        throw new UnsupportedOperationException();
    }

    public long intToLongExpr(int reg) {
        throw new UnsupportedOperationException();
    }

    public static class BMIExpr extends Expr {

        public boolean isMemExprSupported() {
            return true;
        }
    }

    public static class BMIBinaryExpr extends BMIExpr {

        public boolean isBinaryArgumentSupported() {
            return true;
        }

    }

    public static class BMIUnaryExpr extends BMIExpr {
        public boolean isUnaryArgumentSupported() {
            return true;
        }
    }

    public static class BMIBinaryIntExpr extends BMIBinaryExpr {
        public boolean isIntExprSupported() {
            return true;
        }
    }

    public static class BMIBinaryLongExpr extends BMIBinaryExpr {
        public boolean isLongExprSupported() {
            return true;
        }
    }

    public static class BMIUnaryIntExpr extends BMIUnaryExpr {
        public boolean isIntExprSupported() {
            return true;
        }
    }

    public static class BMIUnaryLongExpr extends BMIUnaryExpr {
        public boolean isLongExprSupported() {
            return true;
        }
    }

    public static class BMIUnaryIntToLongExpr extends BMIUnaryExpr {
        public boolean isIntToLongExprSupported() {
            return true;
        }
    }


    public static class BitCountingExpr extends Expr {
        public boolean isUnaryArgumentSupported() {
            return true;
        }
    }

    public static class BitCountingIntExpr extends BitCountingExpr {
        public boolean isIntExprSupported() {
            return true;
        }
    }

    public static class BitCountingLongExpr extends BitCountingExpr {
        public boolean isLongExprSupported() {
            return true;
        }
    }
}
