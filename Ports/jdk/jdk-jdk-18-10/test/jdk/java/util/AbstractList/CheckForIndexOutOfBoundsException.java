/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8161558
 * @summary ListIterator should not discard cause on exception
 * @run testng CheckForIndexOutOfBoundsException
 */

import java.util.List;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

// Fixed size list containing two elements

class MyList extends AbstractList<String> {

    private static final int SIZE = 2;

    public String get(int i) {
        if (i >= 0 && i < SIZE) {
            return "x";
        } else {
            throw new IndexOutOfBoundsException(i);
        }
    }

    public int size() {
        return SIZE;
    }
}

@Test
public class CheckForIndexOutOfBoundsException {

    List<String> list = new MyList();


    @Test
    public void checkIteratorNext() {
        var iterator = list.iterator(); // position at start
        try {
            for (int i = 0; i <= list.size(); i++) {
                iterator.next();
            }
            fail("Failing checkIteratorNext() - NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            checkAssertOnException(e);
        }
    }

    @Test
    public void checkListIteratorNext() {
        var iterator = list.listIterator(list.size()); // position at end
        try {
            iterator.next();
            fail("Failing checkListIteratorNext() - NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            checkAssertOnException(e);
        }
    }

    @Test
    public void checkListIteratorPrevious() {
        var iterator = list.listIterator(0); // position at start
        try {
            iterator.previous();
            fail("Failing checkListIteratorPrevious() - NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            checkAssertOnException(e);
        }
    }

    private void checkAssertOnException(NoSuchElementException e) {
        var cause = e.getCause();
        assertNotNull(cause, "Exception.getCause()");
        assertTrue(cause instanceof IndexOutOfBoundsException, "Exception.getCause() should be an " +
                "IndexOutOfBoundsException");
        var msg = e.getMessage();
        assertNotNull(msg, "Exception.getMessage()");
        assertTrue(msg.contains("IndexOutOfBoundsException"), "Exception.getMessage() should " +
                "contain the string 'IndexOutOfBoundsException'");
    }
}

