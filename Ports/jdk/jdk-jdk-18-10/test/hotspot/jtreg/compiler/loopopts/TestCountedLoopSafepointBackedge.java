/*
 * Copyright (c) 2016, Red Hat, Inc. All rights reserved.
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
 * @bug 8161147
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Safepoint on backedge breaks UseCountedLoopSafepoints
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:+UseCountedLoopSafepoints TestCountedLoopSafepointBackedge
 *
 */

public class TestCountedLoopSafepointBackedge {
    static void test(int[] arr, int inc) {
        int i = 0;
        for (;;) {
            for (int j = 0; j < 10; j++);
            arr[i] = i;
            i++;
            if (i >= 100) {
                break;
            }
            for (int j = 0; j < 10; j++);
        }
    }

    static public void main(String[] args) {
        int[] arr = new int[100];
        for (int i = 0; i < 20000; i++) {
             test(arr, 1);
        }
    }
}
