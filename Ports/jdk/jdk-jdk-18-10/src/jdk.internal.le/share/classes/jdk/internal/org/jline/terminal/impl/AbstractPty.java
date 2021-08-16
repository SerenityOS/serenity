/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl;

import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.spi.Pty;
import jdk.internal.org.jline.utils.NonBlockingInputStream;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import static jdk.internal.org.jline.terminal.TerminalBuilder.PROP_NON_BLOCKING_READS;

public abstract class AbstractPty implements Pty {

    private Attributes current;

    @Override
    public void setAttr(Attributes attr) throws IOException {
        current = new Attributes(attr);
        doSetAttr(attr);
    }

    @Override
    public InputStream getSlaveInput() throws IOException {
        InputStream si = doGetSlaveInput();
        if (Boolean.parseBoolean(System.getProperty(PROP_NON_BLOCKING_READS, "true"))) {
            return new PtyInputStream(si);
        } else {
            return si;
        }
    }

    protected abstract void doSetAttr(Attributes attr) throws IOException;

    protected abstract InputStream doGetSlaveInput() throws IOException;

    protected void checkInterrupted() throws InterruptedIOException {
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
    }

    class PtyInputStream extends NonBlockingInputStream {
        final InputStream in;
        int c = 0;

        PtyInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            checkInterrupted();
            if (c != 0) {
                int r = c;
                if (!isPeek) {
                    c = 0;
                }
                return r;
            } else {
                setNonBlocking();
                long start = System.currentTimeMillis();
                while (true) {
                    int r = in.read();
                    if (r >= 0) {
                        if (isPeek) {
                            c = r;
                        }
                        return r;
                    }
                    checkInterrupted();
                    long cur = System.currentTimeMillis();
                    if (timeout > 0 && cur - start > timeout) {
                        return NonBlockingInputStream.READ_EXPIRED;
                    }
                }
            }
        }

        @Override
        public int readBuffered(byte[] b) throws IOException {
            return in.read(b);
        }

        private void setNonBlocking() {
            if (current == null
                    || current.getControlChar(Attributes.ControlChar.VMIN) != 0
                    || current.getControlChar(Attributes.ControlChar.VTIME) != 1) {
                try {
                    Attributes attr = getAttr();
                    attr.setControlChar(Attributes.ControlChar.VMIN, 0);
                    attr.setControlChar(Attributes.ControlChar.VTIME, 1);
                    setAttr(attr);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
    }

}
