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

package jdk.internal.misc;

import java.util.Hashtable;
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
 * Signal.Handler handler = new Signal.Handler () {
 *     public void handle(Signal sig) {
 *       ... // handle SIGINT
 *     }
 * };
 * Signal.handle(new Signal("INT"), handler);
 * </pre></blockquote>
 *
 * @since    9
 */
public final class Signal {
    private static Hashtable<Signal, Signal.Handler> handlers = new Hashtable<>(4);
    private static Hashtable<Integer, Signal> signals = new Hashtable<>(4);

    private int number;
    private String name;

    /* Returns the signal number */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the signal name.
     *
     * @return the name of the signal.
     * @see jdk.internal.misc.Signal#Signal(String name)
     */
    public String getName() {
        return name;
    }

    /**
     * Compares the equality of two <code>Signal</code> objects.
     *
     * @param obj the object to compare with.
     * @return whether two <code>Signal</code> objects are equal.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Signal other) {
            return name.equals(other.name) && (number == other.number);
        }
        return false;
    }

    /**
     * Returns a hashcode for this Signal.
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return number;
    }

    /**
     * Returns a string representation of this signal. For example, "SIGINT"
     * for an object constructed using <code>new Signal ("INT")</code>.
     *
     * @return a string representation of the signal
     */
    public String toString() {
        return "SIG" + name;
    }

    /**
     * Constructs a signal from its name.
     *
     * @param name the name of the signal.
     * @exception IllegalArgumentException unknown signal
     * @see jdk.internal.misc.Signal#getName()
     */
    public Signal(String name) {
        Objects.requireNonNull(name, "name");
        // Signal names are the short names;
        // the "SIG" prefix is not used for compatibility with previous JDKs
        if (name.startsWith("SIG")) {
            throw new IllegalArgumentException("Unknown signal: " + name);
        }
        this.name = name;
        number = findSignal0(name);
        if (number < 0) {
            throw new IllegalArgumentException("Unknown signal: " + name);
        }
    }

    /**
     * Registers a signal handler.
     *
     * @param sig a signal
     * @param handler the handler to be registered with the given signal.
     * @return the old handler
     * @exception IllegalArgumentException the signal is in use by the VM
     * @see jdk.internal.misc.Signal#raise(Signal sig)
     * @see jdk.internal.misc.Signal.Handler
     * @see jdk.internal.misc.Signal.Handler#SIG_DFL
     * @see jdk.internal.misc.Signal.Handler#SIG_IGN
     */
    public static synchronized Signal.Handler handle(Signal sig,
                                                    Signal.Handler handler)
        throws IllegalArgumentException {
        Objects.requireNonNull(sig, "sig");
        Objects.requireNonNull(handler, "handler");
        long newH = (handler instanceof NativeHandler) ?
                      ((NativeHandler)handler).getHandler() : 2;
        long oldH = handle0(sig.number, newH);
        if (oldH == -1) {
            throw new IllegalArgumentException
                ("Signal already used by VM or OS: " + sig);
        }
        signals.put(sig.number, sig);
        synchronized (handlers) {
            Signal.Handler oldHandler = handlers.get(sig);
            handlers.remove(sig);
            if (newH == 2) {
                handlers.put(sig, handler);
            }
            if (oldH == 0) {
                return Signal.Handler.SIG_DFL;
            } else if (oldH == 1) {
                return Signal.Handler.SIG_IGN;
            } else if (oldH == 2) {
                return oldHandler;
            } else {
                return new NativeHandler(oldH);
            }
        }
    }

    /**
     * Raises a signal in the current process.
     *
     * @param sig a signal
     * @see jdk.internal.misc.Signal#handle(Signal sig, Signal.Handler handler)
     */
    public static void raise(Signal sig) throws IllegalArgumentException {
        Objects.requireNonNull(sig, "sig");
        if (handlers.get(sig) == null) {
            throw new IllegalArgumentException("Unhandled signal: " + sig);
        }
        raise0(sig.number);
    }

    /* Called by the VM to execute Java signal handlers. */
    private static void dispatch(final int number) {
        final Signal sig = signals.get(number);
        final Signal.Handler handler = handlers.get(sig);

        Runnable runnable = new Runnable () {
            public void run() {
              // Don't bother to reset the priority. Signal handler will
              // run at maximum priority inherited from the VM signal
              // dispatch thread.
              // Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                handler.handle(sig);
            }
        };
        if (handler != null) {
            new Thread(null, runnable, sig + " handler", 0, false).start();
        }
    }

    /* Find the signal number, given a name. Returns -1 for unknown signals. */
    private static native int findSignal0(String sigName);
    /* Registers a native signal handler, and returns the old handler.
     * Handler values:
     *   0     default handler
     *   1     ignore the signal
     *   2     call back to Signal.dispatch
     *   other arbitrary native signal handlers
     */
    private static native long handle0(int sig, long nativeH);
    /* Raise a given signal number */
    private static native void raise0(int sig);

    /**
     * This is the signal handler interface expected in <code>Signal.handle</code>.
     */
    public interface Handler {

        /**
         * The default signal handler
         */
        public static final Signal.Handler SIG_DFL = new NativeHandler(0);
        /**
         * Ignore the signal
         */
        public static final Signal.Handler SIG_IGN = new NativeHandler(1);

        /**
         * Handle the given signal
         *
         * @param sig a signal object
         */
        public void handle(Signal sig);
    }


    /*
     * A package-private class implementing a signal handler in native code.
     */
    static final class NativeHandler implements Signal.Handler {

        private final long handler;

        long getHandler() {
            return handler;
        }

        NativeHandler(long handler) {
            this.handler = handler;
        }

        public void handle(Signal sig) {
            throw new UnsupportedOperationException("invoking native signal handle not supported");
        }

        public String toString() {
            return this == SIG_DFL ? "SIG_DFL" :
                    (this == SIG_IGN ? "SIG_IGN" : super.toString());
        }
    }

}
