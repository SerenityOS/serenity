/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Shutdown tests
 * @build KullaTesting TestingInputStream
 * @run testng ShutdownTest
 */

import java.util.function.Consumer;

import jdk.jshell.JShell;
import jdk.jshell.JShell.Subscription;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class ShutdownTest extends KullaTesting {

    int shutdownCount;

    void shutdownCounter(JShell state) {
        ++shutdownCount;
    }

    @Test(enabled = false) //TODO 8139873
    public void testExit() {
        shutdownCount = 0;
        getState().onShutdown(this::shutdownCounter);
        assertEval("System.exit(1);");
        assertEquals(shutdownCount, 1);
    }

    public void testCloseCallback() {
        shutdownCount = 0;
        getState().onShutdown(this::shutdownCounter);
        getState().close();
        assertEquals(shutdownCount, 1);
    }

    public void testCloseUnsubscribe() {
        shutdownCount = 0;
        Subscription token = getState().onShutdown(this::shutdownCounter);
        getState().unsubscribe(token);
        getState().close();
        assertEquals(shutdownCount, 0);
    }

    public void testTwoShutdownListeners() {
        ShutdownListener listener1 = new ShutdownListener();
        ShutdownListener listener2 = new ShutdownListener();
        Subscription subscription1 = getState().onShutdown(listener1);
        Subscription subscription2 = getState().onShutdown(listener2);
        getState().unsubscribe(subscription1);
        getState().close();

        assertEquals(listener1.getEvents(), 0, "Checking got events");
        assertEquals(listener2.getEvents(), 1, "Checking got events");

        getState().close();

        assertEquals(listener1.getEvents(), 0, "Checking got events");
        assertEquals(listener2.getEvents(), 1, "Checking got events");

        getState().unsubscribe(subscription2);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testCloseException() {
        getState().close();
        getState().eval("45");
    }

    @Test(expectedExceptions = IllegalStateException.class,
          enabled = false) //TODO 8139873
    public void testShutdownException() {
        assertEval("System.exit(0);");
        getState().eval("45");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullCallback() {
        getState().onShutdown(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testSubscriptionAfterClose() {
        getState().close();
        getState().onShutdown(e -> {});
    }

    @Test(expectedExceptions = IllegalStateException.class,
          enabled = false) //TODO 8139873
    public void testSubscriptionAfterShutdown() {
        assertEval("System.exit(0);");
        getState().onShutdown(e -> {});
    }

    private static class ShutdownListener implements Consumer<JShell> {
        private int count;

        @Override
        public void accept(JShell shell) {
            ++count;
        }

        public int getEvents() {
            return count;
        }
    }
}
