/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.monitoring.data;

import java.io.Serializable;
import java.lang.management.MemoryUsage;
import nsk.share.log.Log;

public class MemoryUsageData implements Serializable {
        private long init;
        private long used;
        private long committed;
        private long max;

        public MemoryUsageData(long init, long used, long committed, long max) {
                this.init = init;
                this.used = used;
                this.committed = committed;
                this.max = max;
        }

        public MemoryUsageData(MemoryUsage usage) {
                this.init = usage.getInit();
                this.used = usage.getUsed();
                this.committed = usage.getCommitted();
                this.max = usage.getMax();
        }

        public MemoryUsageData(MemoryUsageData usage, MemoryUsageData usage1) {
                this.init = usage.getInit() + usage1.getInit();
                this.used = usage.getUsed() + usage1.getUsed();
                this.committed = usage.getCommitted() + usage1.getCommitted();
                this.max = usage.getMax() + usage1.getMax();
        }

        public long getInit() {
                return init;
        }

        public long getUsed() {
                return used;
        }

        public long getMax() {
                return max;
        }

        public long getFree() {
                return committed - used;
        }

        public long getCommitted() {
                return committed;
        }

        public void log(Log log) {
                log.info("    Init: " + init);
                log.info("    Used: " + used);
                log.info("    Committed: " + committed);
                log.info("    Max: " + max);
        }
}
