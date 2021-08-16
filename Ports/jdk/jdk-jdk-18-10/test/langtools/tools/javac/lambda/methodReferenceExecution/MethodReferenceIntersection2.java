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

/**
 * @test
 * @bug 8058112
 * @summary Invalid BootstrapMethod for constructor/method reference
 */

import java.util.function.Function;

public class MethodReferenceIntersection2 {

    interface B { }

    interface A { }

    static class C implements A, B { }

    static class Info {
        <H extends A & B> Info(H h) { }

        static <H extends A & B> Info info(H h) {
            return new Info(h);
        }
    }

    public static void main(String[] args) {
        test();
    }

    // Note the switch in order compared to that on Info
    static <H extends B & A> void test() {
        Function<H, Info> f1L = _h -> new Info(_h);
        Function<H, Info> f1 = Info::new;
        Function<H, Info> f2L = _h -> Info.info(_h);
        Function<H, Info> f2 = Info::info;
        H c = (H) new C();
        if(f1.apply(c) instanceof Info &&
           f2.apply(c) instanceof Info) {
           System.out.println("Passes.");
        } else {
           throw new AssertionError();
        }
    }
}
