/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 * @test id=default
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC          TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx16m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx32m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx64m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx128m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx256m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx512m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xmx1g   TestObjectAlignment
 */

/*
 * @test id=16b
 * @requires vm.gc.Shenandoah
 * @requires vm.bits == "64"
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16          TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx16m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx32m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx64m  TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx128m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx256m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx512m TestObjectAlignment
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ObjectAlignmentInBytes=16 -Xmx1g   TestObjectAlignment
 */

public class TestObjectAlignment {

    public static void main(String[] args) throws Exception {
        // Testing the checking code on startup
    }

}
