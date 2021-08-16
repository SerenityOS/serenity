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
 * @test
 * @summary Test Shenandoah with different log levels
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms256M -Xmx1G -Xlog:gc*=error   TestWithLogLevel
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms256M -Xmx1G -Xlog:gc*=warning TestWithLogLevel
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms256M -Xmx1G -Xlog:gc*=info    TestWithLogLevel
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms256M -Xmx1G -Xlog:gc*=debug   TestWithLogLevel
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms256M -Xmx1G -Xlog:gc*=trace   TestWithLogLevel
 */

import java.util.*;

public class TestWithLogLevel {
    public static void main(String[] args) {
        ArrayList<Object> list = new ArrayList<>();
        long count = 300 * 1024 * 1024 / 16; // 300MB allocation
        for (long index = 0; index < count; index++) {
            Object sink = new Object();
            list.add(sink);
        }
    }
}
