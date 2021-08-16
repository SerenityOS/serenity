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
 *
 */

/*
 * @test id=default
 * @requires vm.gc.Shenandoah
 * @bug 8268127
 * @summary when heap is too small for regions to align to large page size, should fallback to regular page size
 *
 * @run main/othervm -XX:+UseShenandoahGC -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -Xms17m         TestLargePagesWithSmallHeap
 *
 * @run main/othervm -XX:+UseShenandoahGC -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -Xms17m         TestLargePagesWithSmallHeap
 */

/*
 * @test id=lp
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms17m         TestLargePagesWithSmallHeap
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms17m         TestLargePagesWithSmallHeap
 */

/*
 * @test id=thp
 * @requires vm.gc.Shenandoah
 * @requires os.family == "linux"
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms17m         TestLargePagesWithSmallHeap
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms17m -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages         -Xmx17m TestLargePagesWithSmallHeap
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms17m         TestLargePagesWithSmallHeap
 */

public class TestLargePagesWithSmallHeap {
    public static void main(String[] args) {
        // Everything is checked on initialization
    }
}
