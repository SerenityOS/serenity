/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080679
 * @summary Verify the conversion from key events to escape sequences works properly.
 * @modules jdk.internal.le/jdk.internal.org.jline.terminal
 *          jdk.internal.le/jdk.internal.org.jline.terminal.impl
 */

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.impl.AbstractWindowsTerminal;

public class KeyConversionTest {
    public static void main(String... args) throws Exception {
        new KeyConversionTest().run();
    }

    void run() throws Exception {
        checkKeyConversion(new KeyEvent(true, (short) 37, '\0', 256), "\033OD"); //LEFT
        checkKeyConversion(new KeyEvent(true, (short) 37, '\0', 264), "\033[1;5D"); //Ctrl-LEFT
        checkKeyConversion(new KeyEvent(true, (short) 37, '\0', 258), "\033[1;3D"); //Alt-LEFT
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 256), "\033OP"); //F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 264), "\033[1;5P"); //Ctrl-F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 258), "\033[1;3P"); //Alt-F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 272), "\033[1;2P"); //Shift-F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 280), "\033[1;6P"); //Ctrl-Shift-F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 274), "\033[1;4P"); //Alt-Shift-F1
        checkKeyConversion(new KeyEvent(true, (short) 112, '\0', 282), "\033[1;8P"); //Ctrl-Alt-Shift-F1
        checkKeyConversion(new KeyEvent(true, (short) 67, '\003', 8), "\003"); //Ctrl-C
    }

    void checkKeyConversion(KeyEvent event, String expected) throws IOException {
        StringBuilder result = new StringBuilder();
        new AbstractWindowsTerminal(new StringWriter(), "", "windows", Charset.forName("UTF-8"),
                                    0, true, null, in -> in) {
            @Override
            protected int getConsoleOutputCP() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            protected int getConsoleMode() {
                return 0;
            }
            @Override
            protected void setConsoleMode(int mode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            protected boolean processConsoleInput() throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            public Size getSize() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            public void processInputChar(char c) throws IOException {
                result.append(c);
            }
            @Override
            public void processKeyEvent(boolean isKeyDown, short virtualKeyCode,
                                        char ch, int controlKeyState) throws IOException {
                super.processKeyEvent(isKeyDown, virtualKeyCode, ch, controlKeyState);
            }
        }.processKeyEvent(event.isKeyDown, event.virtualKeyCode, event.ch, event.controlKeyState);
        String actual = result.toString();

        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + "; actual: " + actual);
        }
    }

    public static class KeyEvent {
        public final boolean isKeyDown;
        public final short virtualKeyCode;
        public final char ch;
        public final int controlKeyState;

        public KeyEvent(boolean isKeyDown, short virtualKeyCode, char ch, int controlKeyState) {
            this.isKeyDown = isKeyDown;
            this.virtualKeyCode = virtualKeyCode;
            this.ch = ch;
            this.controlKeyState = controlKeyState;
        }

    }
}
