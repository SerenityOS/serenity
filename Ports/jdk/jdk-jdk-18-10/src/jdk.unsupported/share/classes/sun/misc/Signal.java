/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.misc;

import java.util.Objects;

/**
 * This class provides ANSI/ISO C signal support. A Java program can register
 * signal handlers for the current process. There are two restrictions:
 * <ul>
 * <li>
 * Java code cannot register a handler for signals that are already used
 * by the Java VM implementation. The <code>Signal.handle</code>
 * function raises an <code>IllegalArgumentException</code> if such an attempt
 * is made.
 * <li>
 * When <code>Signal.handle</code> is called, the VM internally registers a
 * special C signal handler. There is no way to force the Java signal handler
 * to run synchronously before the C signal handler returns. Instead, when the
 * VM receives a signal, the special C signal handler creates a new thread
 * (at priority <code>Thread.MAX_PRIORITY</code>) to
 * run the registered Java signal handler. The C signal handler immediately
 * returns. Note that because the Java signal handler runs in a newly created
 * thread, it may not actually be executed until some time after the C signal
 * handler returns.
 * </ul>
 * <p>
 * Signal objects are created based on their names. For example:
 * <blockquote><pre>
 * new Signal("INT");
 * </pre></blockquote>
 * constructs a signal object corresponding to <code>SIGINT</code>, which is
 * typically produced when the user presses <code>Ctrl-C</code> at the command line.
 * The <code>Signal</code> constructor throws <code>IllegalArgumentException</code>
 * when it is passed an unknown signal.
 * <p>
 * This is an example of how Java code handles <code>SIGINT</code>:
 * <blockquote><pre>
 * SignalHandler handler = new SignalHandler () {
 *     public void handle(Signal sig) {
 *       ... // handle SIGINT
 *     }
 * };
 * Signal.handle(new Signal("INT"), handler);
 * </pre></blockquote>
 *
 * @author   Sheng Liang
 * @author   Bill Shannon
 * @see     sun.misc.SignalHandler
 * @since    1.2
 */
public final class Signal {

    // Delegate to jdk.internal.misc.Signal.
    private final jdk.internal.misc.Signal iSignal;

    /* Returns the signal number */
    public int getNumber() {
        return iSignal.getNumber();
    }

    /**
     * Returns the signal name.
     *
     * @return the name of the signal.
     * @see sun.misc.Signal#Signal(String name)
     */
    public String getName() {
        return iSignal.getName();
    }

    /**
     * Compares the equality of two <code>Signal</code> objects.
     *
     * @param other the object to compare with.
     * @return whether two <code>Signal</code> objects are equal.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof Signal)) {
            return false;
        }
        Signal other1 = (Signal)other;
        return iSignal.equals(other1.iSignal);
    }

    /**
     * Returns a hashcode for this Signal.
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return getNumber();
    }

    /**
     * Returns a string representation of this signal. For example, "SIGINT"
     * for an object constructed using <code>new Signal ("INT")</code>.
     *
     * @return a string representation of the signal
     */
    public String toString() {
        return iSignal.toString();
    }

    /**
     * Constructs a signal from its name.
     *
     * @param name the name of the signal.
     * @exception IllegalArgumentException unknown signal
     * @see sun.misc.Signal#getName()
     */
    public Signal(String name) {
        iSignal = new jdk.internal.misc.Signal(name);
    }

    /**
     * Registers a signal handler.
     *
     * @param sig a signal
     * @param handler the handler to be registered with the given signal.
     * @return the old handler
     * @exception IllegalArgumentException the signal is in use by the VM
     * @see sun.misc.Signal#raise(Signal sig)
     * @see sun.misc.SignalHandler
     * @see sun.misc.SignalHandler#SIG_DFL
     * @see sun.misc.SignalHandler#SIG_IGN
     */
    public static synchronized SignalHandler handle(Signal sig,
                                                    SignalHandler handler)
        throws IllegalArgumentException {
        jdk.internal.misc.Signal.Handler oldHandler = jdk.internal.misc.Signal.handle(sig.iSignal,
                InternalMiscHandler.of(sig, handler));
        return SunMiscHandler.of(sig.iSignal, oldHandler);
    }

    /**
     * Raises a signal in the current process.
     *
     * @param sig a signal
     * @see sun.misc.Signal#handle(Signal sig, SignalHandler handler)
     */
    public static void raise(Signal sig) throws IllegalArgumentException {
        jdk.internal.misc.Signal.raise(sig.iSignal);
    }

    /*
     * Wrapper class to proxy a SignalHandler to a jdk.internal.misc.Signal.Handler.
     */
    static final class InternalMiscHandler implements jdk.internal.misc.Signal.Handler {
        private final SignalHandler handler;
        private final Signal signal;

        static jdk.internal.misc.Signal.Handler of(Signal signal, SignalHandler handler) {
            if (handler == SignalHandler.SIG_DFL) {
                return jdk.internal.misc.Signal.Handler.SIG_DFL;
            } else if (handler == SignalHandler.SIG_IGN) {
                return jdk.internal.misc.Signal.Handler.SIG_IGN;
            } else if (handler instanceof SunMiscHandler) {
                return ((SunMiscHandler)handler).iHandler;
            } else {
                return new InternalMiscHandler(signal, handler);
            }
        }

        private InternalMiscHandler(Signal signal, SignalHandler handler) {
            this.handler = handler;
            this.signal = signal;
        }

        @Override
        public void handle(jdk.internal.misc.Signal ignore) {
            handler.handle(signal);
        }
    }

    /*
     * Wrapper class to proxy a jdk.internal.misc.Signal.Handler to a SignalHandler.
     */
    static final class SunMiscHandler implements SignalHandler {
        private final jdk.internal.misc.Signal iSignal;
        private final jdk.internal.misc.Signal.Handler iHandler;

        static SignalHandler of(jdk.internal.misc.Signal signal, jdk.internal.misc.Signal.Handler handler) {
            if (handler == jdk.internal.misc.Signal.Handler.SIG_DFL) {
                return SignalHandler.SIG_DFL;
            } else if (handler == jdk.internal.misc.Signal.Handler.SIG_IGN) {
                return SignalHandler.SIG_IGN;
            } else if (handler instanceof InternalMiscHandler) {
                return ((InternalMiscHandler) handler).handler;
            } else {
                return new SunMiscHandler(signal, handler);
            }
        }

        SunMiscHandler(jdk.internal.misc.Signal iSignal, jdk.internal.misc.Signal.Handler iHandler) {
            this.iSignal = iSignal;
            this.iHandler = iHandler;
        }

        @Override
        public void handle(Signal sig) {
            iHandler.handle(iSignal);
        }

        public String toString() {
            return iHandler.toString();
        }
    }
}
