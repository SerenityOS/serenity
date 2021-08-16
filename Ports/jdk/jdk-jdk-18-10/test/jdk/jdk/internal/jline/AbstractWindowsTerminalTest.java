/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218287
 * @summary Verify the wrapper input stream is used when using Terminal.reader()
 * @modules jdk.internal.le/jdk.internal.org.jline.terminal
 *          jdk.internal.le/jdk.internal.org.jline.terminal.impl
 *          jdk.internal.le/jdk.internal.org.jline.utils
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.Terminal.SignalHandler;
import jdk.internal.org.jline.terminal.impl.AbstractWindowsTerminal;


public class AbstractWindowsTerminalTest {
    public static void main(String... args) throws IOException {
        new AbstractWindowsTerminalTest().run();
    }

    void run() throws IOException {
        var out = new StringWriter();
        AtomicBoolean called = new AtomicBoolean();
        Function<InputStream, InputStream> isWrapper = is -> new InputStream() {
            @Override
            public int read() throws IOException {
                called.set(true);
                return is.read();
            }
        };
        var t = new AbstractWindowsTerminal(out, "test", "vt100", null, -1, false, SignalHandler.SIG_DFL, isWrapper) {
            @Override
            protected int getConsoleOutputCP() {
                throw new UnsupportedOperationException("unexpected.");
            }

            @Override
            protected int getConsoleMode() {
                return -1;
            }

            @Override
            protected void setConsoleMode(int mode) {
                throw new UnsupportedOperationException("unexpected.");
            }

            @Override
            protected boolean processConsoleInput() throws IOException {
                throw new UnsupportedOperationException("unexpected.");
            }

            @Override
            public Size getSize() {
                throw new UnsupportedOperationException("unexpected.");
            }
        };
        t.processInputChar(' ');
        if (t.reader().read() != ' ') {
            throw new AssertionError("Unexpected input!");
        }
        if (!called.get()) {
            throw new AssertionError("The wrapper was not called!");
        }
    }
}
