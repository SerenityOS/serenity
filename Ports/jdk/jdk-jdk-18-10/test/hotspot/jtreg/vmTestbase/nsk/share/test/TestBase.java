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

package nsk.share.test;

import nsk.share.log.Log;
import nsk.share.log.LogAware;
import nsk.share.Consts;
import vm.share.options.OptionSupport;
import vm.share.options.Option;
import vm.share.options.Options;

public abstract class TestBase implements Test, LogAware, TestExitCode {
        @Option
        protected Log log;
        protected volatile int exitCode = 0;

        public final void setLog(Log log) {
                this.log = log;
        }

        public final int getExitCode() {
                return exitCode;
        }

        public final void setExitCode(int exitCode) {
                this.exitCode = exitCode;
        }

        public final void setFailed(boolean failed) {
                setExitCode(Consts.JCK_STATUS_BASE + (failed ? Consts.TEST_FAILED : Consts.TEST_PASSED));
        }

        public final boolean isFailed() {
                return exitCode != 0 && exitCode != 95;
        }

        public static void runTest(TestBase test, String[] args) {
                OptionSupport.setup(test, args);
                test.run();
                int exitCode = test.getExitCode();
                if (exitCode != 0)
                        System.exit(exitCode);
                else
                        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
        }
}
