/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.process;

import nsk.share.log.Log;

/*
 * StreamListener that logs everything to Log at debug priority.
 */
public class StreamLogger implements StreamListener {
        private String prefix;
        private Log log;

        public StreamLogger(String prefix, Log log) {
                this.prefix = prefix;
                this.log = log;
        }

        public void onStart() {
                // do nothing
        }

        public void onFinish() {
                // do nothing
        }

        public void onRead(String s) {
                log.debug(prefix + s);
        }

        public void onException(Throwable e) {
                log.debug(prefix + "Exception");
                log.debug(e);
        }
}
