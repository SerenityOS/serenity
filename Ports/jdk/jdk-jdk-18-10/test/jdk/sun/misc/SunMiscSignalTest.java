/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/*
 * @test
 * @library /test/lib
 * @modules jdk.unsupported
 *          java.base/jdk.internal.misc
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run testng/othervm -Xrs -DXrs=true SunMiscSignalTest
 * @run testng/othervm SunMiscSignalTest
 * @summary sun.misc.Signal test
 */

@Test
public class SunMiscSignalTest {

    // Set to true to enable additional debug output
    static boolean debug = true;

    // True to test while running with -Xrs
    static boolean RUNNING_WITH_Xrs = Boolean.getBoolean("Xrs");

    /**
     * Print a debug message if enabled.
     *
     * @param format the format
     * @param args   the arguments
     */
    static void printf(String format, Object... args) {
        if (debug) {
            System.out.printf("    " + format, args);
        }
    }

    enum IsSupported {NO, YES}

    enum CanRegister {NO, YES}

    enum CanRaise {NO, YES}

    enum Invoked {NO, YES}

    enum RestrictedSignals {NORMAL, XRS}

    @BeforeSuite
    static void setup() {
        System.out.printf("-Xrs: %s%n", RUNNING_WITH_Xrs);
    }

    // Provider of signals to be tested with variations for -Xrs and
    // platform dependencies
    // -Xrs restricted signals signals the VM will not handle SIGINT, SIGTERM, SIGHUP and others
    @DataProvider(name = "supportedSignals")
    static Object[][] supportedSignals() {
        RestrictedSignals rs = RUNNING_WITH_Xrs ? RestrictedSignals.XRS : RestrictedSignals.NORMAL;
        CanRegister registerXrs = RUNNING_WITH_Xrs ? CanRegister.NO : CanRegister.YES;
        CanRaise raiseXrs = RUNNING_WITH_Xrs ? CanRaise.NO : CanRaise.YES;
        Invoked invokedXrs = RUNNING_WITH_Xrs ? Invoked.NO : Invoked.YES;

        Object[][] commonSignals = new Object[][]{
                {"INT",  IsSupported.YES, registerXrs, raiseXrs, invokedXrs},
                {"TERM", IsSupported.YES, registerXrs, raiseXrs, invokedXrs},
                {"ABRT", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
        };

        Object[][] posixSignals = {
                {"HUP",  IsSupported.YES, registerXrs, raiseXrs, invokedXrs},
                {"QUIT", IsSupported.YES, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"USR1", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"USR2", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"PIPE", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"ALRM", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"CHLD", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"CONT", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"TSTP", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"TTIN", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"TTOU", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"URG",  IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"XCPU", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"XFSZ", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"VTALRM", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"PROF", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"WINCH", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"IO",   IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"SYS",   IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
        };

        Object[][] posixNonOSXSignals = {
                {"BUS",  IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
                {"INFO", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
        };

        Object[][] posixOSXSignals = {
                {"BUS",  IsSupported.YES, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"INFO", IsSupported.YES, CanRegister.YES, CanRaise.YES, invokedXrs},
        };

        Object[][] windowsSignals = {
                {"HUP",  IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"QUIT", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"BUS",  IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"USR1", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"USR2", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"PIPE", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"ALRM", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"CHLD", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"CONT", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"TSTP", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"TTIN", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"TTOU", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"URG",  IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"XCPU", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"XFSZ", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"VTALRM", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"PROF", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"WINCH", IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"IO",   IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
                {"SYS",  IsSupported.NO, CanRegister.NO, CanRaise.NO, Invoked.NO},
        };

        Object[][] combinedPosixSignals = concatArrays(posixSignals,
                                                       (Platform.isOSX() ? posixOSXSignals : posixNonOSXSignals));
        return concatArrays(commonSignals, (Platform.isWindows() ? windowsSignals : combinedPosixSignals));
    }

    // Provider of invalid signal names
    @DataProvider(name = "invalidSunMiscSignalNames")
    Object[][] invalidSunMiscSignalNames() {
        return new Object[][]{
                {""},
                {"I"},
                {"SIG"},
                {"SIGabc"},
                {"SIGINT"},     // prefix not allowed
                {"abc"},
        };
    }

    static Object[][] concatArrays(Object[][]... arrays) {
        int l = 0;
        for (Object[][] a : arrays) {
            l += a.length;
        }

        Object[][] newArray = new Object[l][];
        l = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, newArray, l, arrays[i].length);
            l += arrays[i].length;
        }

        return newArray;
    }

    // Return true if the signal is one of the shutdown signals known to the VM
    private static boolean isShutdownSignal(Signal signal) {
        String name = signal.getName();
        return name.equals("INT") || name.equals("HUP") || name.equals("TERM");
    }

    /**
     * Quick verification of supported signals using sun.misc.Signal.
     *
     * @param name the signal name
     * @throws InterruptedException would be an error if thrown
     */
    @Test(dataProvider = "supportedSignals")
    static void testSunMisc(String name, IsSupported supported, CanRegister register,
                            CanRaise raise, Invoked invoked) throws InterruptedException {
        Handler h = new Handler();
        SignalHandler orig = null;
        Signal signal = null;
        try {
            signal = new Signal(name);
            Assert.assertEquals(supported, IsSupported.YES, "Unexpected support for " + name);

            Assert.assertEquals(signal.getName(), name, "getName() mismatch, ");

            Assert.assertEquals(signal.toString(), "SIG" + name, "toString() mismatch, ");

            try {
                orig = Signal.handle(signal, h);
                printf("oldHandler: %s%n", orig);
                Assert.assertEquals(CanRegister.YES, register, "Unexpected handle succeeded " + name);
                try {
                    Signal.raise(signal);
                    Assert.assertEquals(CanRaise.YES, raise, "Unexpected raise success for " + name);
                    Invoked inv = h.semaphore().tryAcquire(Utils.adjustTimeout(100L),
                            TimeUnit.MILLISECONDS) ? Invoked.YES : Invoked.NO;
                    if (!isShutdownSignal(signal)) {
                        // Normal case
                        Assert.assertEquals(inv, invoked, "handler not invoked;");
                    } else {
                        if (orig == SignalHandler.SIG_IGN) {
                            Assert.assertEquals(inv, Invoked.NO, "handler should not be invoked");
                        } else {
                            Assert.assertEquals(inv, invoked, "handler not invoked;");
                        }
                    }
                } catch (IllegalArgumentException uoe3) {
                    Assert.assertNotEquals(CanRaise.YES, raise, "raise failed for " + name +
                            ": " + uoe3.getMessage());
                }
            } catch (IllegalArgumentException uoe2) {
                Assert.assertNotEquals(CanRegister.YES, register, "handle failed for: " + name +
                        ": " + uoe2.getMessage());
            }
        } catch (IllegalArgumentException uoe) {
            Assert.assertNotEquals(IsSupported.YES, supported, "Support missing for " + name +
                    ": " + uoe.getMessage());
            return;
        } finally {
            // Restore original signal handler
            if (orig != null && signal != null) {
                Signal.handle(signal, orig);
            }
        }
    }

    // Test Signal is equal to itself and not equals to others
    @Test(dataProvider = "supportedSignals")
    static void testEquals(String name, IsSupported supported, CanRegister register,
                           CanRaise raise, Invoked invoked) {
        Object[][] data = supportedSignals();
        for (int i = 0; i < data.length; i++) {
            IsSupported otherSupported = (IsSupported) data[i][1];
            if (supported == IsSupported.NO || otherSupported == IsSupported.NO) {
                continue;
            }
            String otherName = (String) data[i][0];

            Signal sig1 = new Signal(name);
            Signal sig2 = new Signal(otherName);
            if (name.equals(otherName)) {
                Assert.assertEquals(sig1, sig2, "Equals failed; ");
                Assert.assertEquals(sig1.hashCode(), sig2.hashCode(), "HashCode wrong; ");
            } else {
                Assert.assertNotEquals(sig1, sig2, "NotEquals failed; ");
                Assert.assertNotEquals(sig1.hashCode(), sig2.hashCode(), "HashCode wrong; ");
            }
        }
    }

    @Test(dataProvider = "invalidSunMiscSignalNames")
    static void testSunMiscIAE(String name) {
        try {
            new Signal(name);
            Assert.fail("Should have thrown IAE for signal: " + name);
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals(iae.getMessage(), "Unknown signal: " + name, "getMessage() incorrect; ");
        }
    }

    // Note: JDK 8 did not check/throw NPE, passing null resulted in a segv
    @Test(expectedExceptions = NullPointerException.class)
    static void nullSignal() {
        new Signal(null);
    }

    // Test expected exception when raising a signal when no handler defined
    @Test
    static void testRaiseNoConsumer() {
        Signal signal = new Signal("INT");
        SignalHandler orig = null;
        try {
            orig = Signal.handle(signal, SignalHandler.SIG_DFL);
            printf("oldHandler: %s%n", orig);
            if (orig == SignalHandler.SIG_IGN) {
                // SIG_IGN for TERM means it cannot be handled
                return;
            }
            Signal.raise(signal);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            printf("IAE message: %s%n", iae.getMessage());
        } finally {
            // Restore original signal handler
            if (orig != null && signal != null) {
                Signal.handle(signal, orig);
            }
        }
    }

    /**
     * The thread that runs the handler for sun.misc.Signal should be a
     * Daemon thread.
     */
    @Test
    static void isDaemonThread() throws InterruptedException {
        if (RUNNING_WITH_Xrs) {
            return;
        }
        Handler handler = new Handler();
        Signal signal = new Signal("INT");
        SignalHandler orig = Signal.handle(signal, handler);
        printf("oldHandler: %s%n", orig);
        if (orig == SignalHandler.SIG_IGN) {
            // SIG_IGN for INT means it cannot be handled
            return;
        }

        Signal.raise(signal);
        boolean handled = handler.semaphore()
                .tryAcquire(Utils.adjustTimeout(100L), TimeUnit.MILLISECONDS);
        if (!handled) {
            // For debug try again
            printf("Second try to see signal");
            handled = handler.semaphore()
                    .tryAcquire(Utils.adjustTimeout(2L), TimeUnit.SECONDS);
        }
        Assert.assertEquals(handled, !RUNNING_WITH_Xrs,
                "raising s.m.Signal did not get a callback;");

        Assert.assertTrue(handler.wasDaemon(), "Thread.isDaemon running the handler; ");
    }

    // Check that trying to invoke SIG_DFL.handle throws UnsupportedOperationException.
    @Test(expectedExceptions = UnsupportedOperationException.class)
    static void cannotHandleSIGDFL() {
        Signal signal = new Signal("INT");
        Assert.assertNotNull(SignalHandler.SIG_DFL, "SIG_DFL null; ");
        SignalHandler.SIG_DFL.handle(signal);
    }

    // Check that trying to invoke SIG_IGN.handle throws UnsupportedOperationException.
    @Test(expectedExceptions = UnsupportedOperationException.class)
    static void cannotHandleSIGIGN() {
        Signal signal = new Signal("INT");
        Assert.assertNotNull(SignalHandler.SIG_IGN, "SIG_IGN null; ");
        SignalHandler.SIG_IGN.handle(signal);
    }

    // Check that setting a Signal handler returns the previous handler.
    @Test()
    static void checkLastHandler() {
        if (RUNNING_WITH_Xrs) {
            return;
        }
        Signal signal = new Signal("TERM");
        Handler h1 = new Handler();
        Handler h2 = new Handler();
        SignalHandler orig = Signal.handle(signal, h1);
        if (orig == SignalHandler.SIG_IGN) {
            // SIG_IGN for TERM means it cannot be handled
            return;
        }

        try {
            SignalHandler prev = Signal.handle(signal, h2);
            Assert.assertSame(prev, h1, "prev handler mismatch");

            prev = Signal.handle(signal, h1);
            Assert.assertSame(prev, h2, "prev handler mismatch");
        } finally {
            if (orig != null && signal != null) {
                Signal.handle(signal, orig);
            }
        }
    }

    /**
     * Test Handler, a SignalHandler for Signal notifications.
     * Signals a semaphore when invoked and records whether
     * the thread calling the Handler was a daemon.
     */
    static class Handler implements SignalHandler {
        // A semaphore to check for accept being called
        Semaphore sema = new Semaphore(0);

        Boolean wasDaemon = null;

        Semaphore semaphore() {
            return sema;
        }

        synchronized Boolean wasDaemon() {
            return wasDaemon;
        }

        /**
         * Releases the semaphore when called as SignalHandler.handle.
         *
         * @param signal the Signal that occurred
         */
        @Override
        public void handle(Signal signal) {
            synchronized (this) {
                wasDaemon = Thread.currentThread().isDaemon();
            }
            sema.release();
            printf("sun.misc.handle sig: %s, num: %d%n", signal.getName(), signal.getNumber());
        }

        public String toString() {
            return "Handler: sem: " + sema.getQueueLength() +
                    ", wasDaemon: " + Objects.toString(wasDaemon());
        }
    }

    // Main can be used to run the tests from the command line with only testng.jar.
    @SuppressWarnings("raw_types")
    @Test(enabled = false)
    public static void main(String[] args) {
        Class<?>[] testclass = {SunMiscSignalTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.run();
    }

}
