/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

// key: compiler.err.cant.ref.non.effectively.final.var
// key: compiler.misc.inner.cls
// key: compiler.misc.lambda
// key: compiler.misc.guard
// key: compiler.misc.feature.pattern.switch
// key: compiler.warn.preview.feature.use.plural
// options: --enable-preview -source ${jdk.version} -Xlint:preview

class CantRefNonEffectivelyFinalVar {
    void test() {
        int i = 0;
        new Object() { int j = i; };
        i = 2;
    }

    interface SAM {
        void m();
    }

    void test2() {
        int i = 0;
        SAM s = ()-> { int j = i; };
        i++;
    }

    void test3(Object o, int i) {
        switch (o) {
            case String s && s.length() == i++: break;
            default: break;
        }
    }
}
