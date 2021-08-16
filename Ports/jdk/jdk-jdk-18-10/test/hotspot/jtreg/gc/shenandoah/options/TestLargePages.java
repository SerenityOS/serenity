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
 *
 * @run main/othervm -XX:+UseShenandoahGC -Xms128m -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC          -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -Xms128m          TestLargePages
 *
 * @run main/othervm -XX:+UseShenandoahGC -Xms131m -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC          -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -Xms131m          TestLargePages
 */

/*
 * @test id=lp
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms128m -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages          -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms128m          TestLargePages
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms131m -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages          -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseLargePages -Xms131m          TestLargePages
 */

/*
 * @test id=thp
 * @requires vm.gc.Shenandoah
 * @requires os.family == "linux"
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms128m -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages          -Xmx128m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms128m          TestLargePages
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms131m -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages          -Xmx131m TestLargePages
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseTransparentHugePages -Xms131m          TestLargePages
 */

public class TestLargePages {
    public static void main(String[] args) {
        // Everything is checked on initialization
    }
}
