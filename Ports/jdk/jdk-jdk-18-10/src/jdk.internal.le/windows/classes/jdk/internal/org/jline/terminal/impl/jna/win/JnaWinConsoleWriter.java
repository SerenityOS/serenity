/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl.jna.win;

//import com.sun.jna.LastErrorException;
//import com.sun.jna.Pointer;
//import com.sun.jna.ptr.IntByReference;
import jdk.internal.org.jline.terminal.impl.AbstractWindowsConsoleWriter;

import java.io.IOException;

class JnaWinConsoleWriter extends AbstractWindowsConsoleWriter {

    private final Pointer consoleHandle;
    private final IntByReference writtenChars = new IntByReference();

    JnaWinConsoleWriter(Pointer consoleHandle) {
        this.consoleHandle = consoleHandle;
    }

    @Override
    protected void writeConsole(char[] text, int len) throws IOException {
        try {
            Kernel32.INSTANCE.WriteConsoleW(this.consoleHandle, text, len, this.writtenChars, null);
        } catch (LastErrorException e) {
            throw new IOException("Failed to write to console", e);
        }
    }

}
