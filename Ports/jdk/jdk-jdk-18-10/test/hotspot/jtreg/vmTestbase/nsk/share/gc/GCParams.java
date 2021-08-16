/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc;

import java.io.PrintStream;

public class GCParams {
        private String garbageProducerId;
        private String garbageProducer1Id;
        private String memoryStrategyId;
        private String lockersId;

        public final String getGarbageProducerId() {
                return garbageProducerId;
        }

        public final void setGarbageProducerId(String garbageProducerId) {
                this.garbageProducerId = garbageProducerId;
        }

        public final String getGarbageProducer1Id() {
                return garbageProducer1Id;
        }

        public final void setGarbageProducer1Id(String garbageProducer1Id) {
                this.garbageProducer1Id = garbageProducer1Id;
        }

        public final String getMemoryStrategyId() {
                return memoryStrategyId;
        }

        public final void setMemoryStrategyId(String memoryStrategyId) {
                this.memoryStrategyId = memoryStrategyId;
        }

        public final String getLockersId() {
                return lockersId;
        }

        public final void setLockersId(String lockersId) {
                this.lockersId = lockersId;
        }

        public void parseCommandLine(String[] args) {
                if (args == null)
                        return;
                for (int i = 0; i < args.length; ++i) {
                        if (args[i].equals("-gp"))
                                garbageProducerId = args[++i];
                        else if (args[i].equals("-gp1"))
                                garbageProducer1Id = args[++i];
                        else if (args[i].equals("-ms"))
                                memoryStrategyId = args[++i];
                        else if (args[i].equals("-lockers"))
                                lockersId = args[++i];
                }
                printConfig(System.out);
        }

        public void prinUsage() {
        }

        public void printConfig(PrintStream out) {
        }

        private static GCParams instance;

        public static GCParams getInstance() {
                synchronized (GCParams.class) {
                        if (instance == null)
                                instance = new GCParams();
                        return instance;
                }
        }

        public static void setInstance(GCParams gcParams) {
                synchronized (GCParams.class) {
                        instance = gcParams;
                }
        }
}
