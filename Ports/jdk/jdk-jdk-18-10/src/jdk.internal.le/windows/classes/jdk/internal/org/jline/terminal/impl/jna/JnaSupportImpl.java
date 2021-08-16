/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl.jna;

import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
import jdk.internal.org.jline.terminal.spi.JnaSupport;
import jdk.internal.org.jline.terminal.spi.Pty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

public class JnaSupportImpl implements JnaSupport {
    @Override
    public Pty current() throws IOException {
//        return JnaNativePty.current();
        throw new UnsupportedOperationException();
    }

    @Override
    public Pty open(Attributes attributes, Size size) throws IOException {
//        return JnaNativePty.open(attributes, size);
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler) throws IOException {
        return winSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, false);
    }

    @Override
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused) throws IOException {
        return winSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused, input -> input);
    }

    @Override
    public Terminal winSysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused, Function<InputStream, InputStream> inputStreamWrapper) throws IOException {
        return JnaWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused, inputStreamWrapper);
    }
}
