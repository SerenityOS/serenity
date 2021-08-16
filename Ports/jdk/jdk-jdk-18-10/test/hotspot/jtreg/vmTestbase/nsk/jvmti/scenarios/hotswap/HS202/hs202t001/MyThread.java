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
package nsk.jvmti.scenarios.hotswap.HS202.hs202t001;
public class MyThread extends Thread {
        MyObject myObject;
        public MyThread(MyObject obj) {
                this.myObject = obj;
        }
        public void run() {
                playWithThis();
        }

        public void playWithThis() {
                while(!myObject.isStopped()) {
                        try {
                                display();
                        } catch(java.lang.InterruptedException ie) {
                                ie.printStackTrace();
                        }
                }
        }
        private void display() throws InterruptedException {
                synchronized(myObject) {
                        if (myObject.isUpdated()) {
                                int i=0;
                                System.out.println(" Value is updated and waiting and got these values ..");
                                i = myObject.getAge();
                                System.out.println("Waiting over.. "+i);
                        } else {
                                System.out.println(" It is not updated yet");
                        }
                        myObject.leaveMonitor();
                }
                this.sleep(100);
        }
}
