/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8264360
 * @summary Loop strip mining verification fails with "should be on the backedge"
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation TestLoadOnBackedgeWithPrec
 *
 */


class a {
    int g = 20;
    float h = 2;
    long b = 6;
}

public class TestLoadOnBackedgeWithPrec {
    int c ;
    a[] i = {new a()};
    float j() {
        a k = new a();
        float l = 5;
        for (int d = 0; d < 8; ++d) {
            for (int e = 0; e < 9; ++e) {
                k = k;
                l *= k.g;
            }
            for (int f = 0; f < 9; ++f) {
                new a();
            }
            {
                a[] m = {
                    new a(), new a(), new a(),
                    new a(), new a(), new a(),
                    new a(), new a(), new a()};
                c = i[0].g + k.g;
            }
        }
        return k.h;
    }
    public static void main(String[] args) {
        TestLoadOnBackedgeWithPrec n = new TestLoadOnBackedgeWithPrec();
        for (int i = 0; i < 5_000; i++) {
            n.j();
        }
    }
}

