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
 * @summary Subscribe tests
 * @build KullaTesting TestingInputStream
 * @run testng SnippetStatusListenerTest
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.JShell.Subscription;
import jdk.jshell.SnippetEvent;
import jdk.jshell.TypeDeclSnippet;
import org.testng.annotations.Test;

import static jdk.jshell.Snippet.Status.*;
import static org.testng.Assert.assertEquals;

@Test
public class SnippetStatusListenerTest extends KullaTesting {

    public void testTwoSnippetEventListeners() {
        SnippetListener listener1 = new SnippetListener();
        SnippetListener listener2 = new SnippetListener();

        Subscription subscription1 = getState().onSnippetEvent(listener1);
        getState().onSnippetEvent(listener2);

        TypeDeclSnippet a = classKey(assertEval("class A {}"));
        assertEval("interface A {}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, null));
        DeclarationSnippet f = (DeclarationSnippet) assertDeclareFail("double f() { }", "compiler.err.missing.ret.stmt");
        assertEvalException("throw new RuntimeException();");
        assertEval("int a = 0;");

        List<SnippetEvent> events1 = Collections.unmodifiableList(listener1.getEvents());
        assertEquals(events1, listener2.getEvents(), "Checking got events");
        getState().unsubscribe(subscription1);

        assertDrop(f, DiagCheck.DIAG_IGNORE, DiagCheck.DIAG_IGNORE, ste(f, REJECTED, DROPPED, false, null));
        assertEval("void f() { }", added(VALID));
        assertEvalException("throw new RuntimeException();");
        assertEquals(listener1.getEvents(), events1, "Checking that unsubscribed listener does not get events");

        List<SnippetEvent> events2 = new ArrayList<>(listener2.getEvents());
        events2.removeAll(events1);

        assertEquals(events2.size(), 3, "The second listener got events");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullCallback() {
        getState().onSnippetEvent(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testSubscriptionAfterClose() {
        getState().close();
        getState().onSnippetEvent(e -> {});
    }

    @Test(expectedExceptions = IllegalStateException.class,
          enabled = false) //TODO 8139873
    public void testSubscriptionAfterShutdown() {
        assertEval("System.exit(0);");
        getState().onSnippetEvent(e -> {});
    }

    public void testSubscriptionToAnotherState() {
        SnippetListener listener = new SnippetListener();
        Subscription subscription = getState().onSnippetEvent(listener);
        tearDown();
        setUp();
        assertEval("int x;");
        assertEquals(Collections.emptyList(), listener.getEvents(), "No events");
        getState().unsubscribe(subscription);
    }

    private static class SnippetListener implements Consumer<SnippetEvent> {
        private final List<SnippetEvent> events = new ArrayList<>();

        @Override
        public void accept(SnippetEvent event) {
            events.add(event);
        }

        public List<SnippetEvent> getEvents() {
            return events;
        }
    }
}
