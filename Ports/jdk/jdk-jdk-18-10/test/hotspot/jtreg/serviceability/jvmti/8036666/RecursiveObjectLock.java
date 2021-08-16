/*
 * Copyright (c) 2014 SAP SE. All rights reserved.
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

public class RecursiveObjectLock {

    public void testMethod() {
        synchronized (this) {
            nestedLock1();
        }
    }

    public void nestedLock1() {
        synchronized (this) {
            nestedLock2();
        }
    }

    public void nestedLock2() {
        synchronized (this) {
            callWait();
        }
    }

    public void callWait(){
        try {
            this.wait(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        breakpoint1();
    }

    public static void breakpoint1() {
        // purpose: hold a breakpoint
    }

    public static void main(String[] args) {
        RecursiveObjectLock ro = new RecursiveObjectLock();
        ro.testMethod();
        System.out.println("ready");
    }

}
