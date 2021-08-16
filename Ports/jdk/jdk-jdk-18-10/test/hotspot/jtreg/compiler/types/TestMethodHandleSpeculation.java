/*
 * Copyright (c) 2021, Red Hat, Inc.. All rights reserved.
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
 * @bug 8269285
 * @summary Crash/miscompile in CallGenerator::for_method_handle_inline after JDK-8191998
 * @requires vm.compMode == "Xmixed" & vm.flavor == "server"
 *
 * @run main/othervm
 *        -Xcomp -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,compiler.types.TestMethodHandleSpeculation::main
 *        compiler.types.TestMethodHandleSpeculation
 */

package compiler.types;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Supplier;

public class TestMethodHandleSpeculation {

    public static void main(String... args) {
        byte[] serObj = {1};
        MyClass<byte[]> obj = new MyClass<>();
        for (int i = 0; i < 100_000; i++) {
            boolean test = obj.test(serObj);
            if (test) {
                throw new IllegalStateException("Cannot be null");
            }
        }
    }

    static class MyClass<V extends Serializable> {
        boolean test(V obj) {
            Supplier<Boolean> supp = () -> (obj == null);
            return supp.get();
        }
    }

}
