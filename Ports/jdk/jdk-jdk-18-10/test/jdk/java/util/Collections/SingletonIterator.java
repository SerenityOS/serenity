/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024500 8166446
 * @run testng SingletonIterator
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

@Test(groups = "unit")
public class SingletonIterator {
    static void assertIteratorExhausted(Iterator<?> it) {
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("should have thrown NoSuchElementException");
        } catch (NoSuchElementException success) { }
        it.forEachRemaining(e -> { throw new AssertionError("action called incorrectly"); });
    }

    public void testForEachRemaining() {
        Iterator<String> it = Collections.singleton("TheOne").iterator();
        AtomicInteger cnt = new AtomicInteger(0);

        it.forEachRemaining(s -> {
            assertEquals("TheOne", s);
            cnt.incrementAndGet();
        });

        assertEquals(cnt.get(), 1);
        assertIteratorExhausted(it);
    }

    static class SingletonException extends RuntimeException { }

    public void testThrowFromForEachRemaining() {
        Iterator<String> it = Collections.singleton("TheOne").iterator();

        try {
            it.forEachRemaining(s -> { throw new SingletonException(); });
        } catch (SingletonException ignore) { }

        assertIteratorExhausted(it);
    }
}
