/*
 * Copyright (c) 2016, Red Hat Inc.
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

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class TestLogHandler extends Handler {

    private final String illegal;
    private boolean testFailed;

    public TestLogHandler(String illegal) {
        this.illegal = illegal;
        this.testFailed = false;
    }

    @Override
    public void publish(LogRecord record) {
        String msg = record.getMessage();
        String method = record.getSourceMethodName();
        String className = record.getSourceClassName();
        if (msg.contains(illegal)) {
            testFailed = true;
        }
        if (msg.contains("attribute names=")) {
            System.err.println("LOG: " + className + "." + method + ": " + msg);
        }
    }

    @Override
    public void flush() {
        // nothing
    }

    @Override
    public void close() throws SecurityException {
        // nothing
    }

    public boolean testFailed() {
        return testFailed;
    }

}
