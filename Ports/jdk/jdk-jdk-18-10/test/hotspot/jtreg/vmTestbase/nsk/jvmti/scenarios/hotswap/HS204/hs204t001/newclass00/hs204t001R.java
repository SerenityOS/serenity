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
package nsk.jvmti.scenarios.hotswap.HS204.hs204t001;
import java.util.concurrent.atomic.AtomicBoolean;
public class hs204t001R extends Thread {
      static public AtomicBoolean suspend = new AtomicBoolean(false);
      static public AtomicBoolean run = new AtomicBoolean(true);

        private  int index=0;

        public hs204t001R() {
                setName("hs204t001R");
                System.out.println("hs204t001R");
        }

        public void run() {
                System.out.println(" started running thread..");
                doInThisThread();
                System.out.println(" comming out ..");
        }

        private void doInThisThread() {
                System.out.println("... Inside doThisThread..");
                while(hs204t001R.run.get()) {
                                index+=10;
                        if (index == 1500) {
                                hs204t001R.suspend.set(true);
                        }
                }
        }

        public int getIndex() {
                return index;
        }

}
