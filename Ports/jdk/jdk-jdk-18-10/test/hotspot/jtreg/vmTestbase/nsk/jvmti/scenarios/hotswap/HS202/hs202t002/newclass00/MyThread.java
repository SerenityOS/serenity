/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.scenarios.hotswap.HS202.hs202t002;
public class MyThread extends Thread {
    private int val = 100;

    public void run() {
        playWithThis();
    }

    public void playWithThis() {
        try {
            display();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void display() throws Exception {
        // increment val by 10 in the redefined version
        // of display(). The original version only incremented by 1.
        val += 10;
        throw new Exception(" Dummy Exception...");
    }

    public int getValue() {
        return val;
    }
}
