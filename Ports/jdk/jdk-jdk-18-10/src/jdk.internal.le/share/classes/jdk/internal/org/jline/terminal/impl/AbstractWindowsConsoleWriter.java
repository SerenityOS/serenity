/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl;

import java.io.IOException;
import java.io.Writer;

public abstract class AbstractWindowsConsoleWriter extends Writer {

    protected abstract void writeConsole(char[] text, int len) throws IOException;

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        char[] text = cbuf;
        if (off != 0) {
            text = new char[len];
            System.arraycopy(cbuf, off, text, 0, len);
        }

        synchronized (this.lock) {
            writeConsole(text, len);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
