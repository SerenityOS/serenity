/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 *  This class serves as a bridge between the Wily JUnit-style tests and the
 *  Sun test framework.
 *
 *  This is a replacement for the JUnit TestCase base class. Provides surrogate
 *  functionality for the setup/teardown calls, and for all the verification and
 *  assertion services.
 *
 *  The Sun framework relies on each test case being a separate class with a separate main,
 *  which throws if the test fails and does not throw if the test succeeds.
 */


public abstract class ATestCaseScaffold {
    private String      fName;
    private boolean     fVerbose;


    protected
    ATestCaseScaffold(String name) {
        fName = name;
        fVerbose = false;
    }

    public final void
    runTest()
        throws Throwable {
        Throwable toRethrow = null;

        setUp();

        try {
            doRunTest();
        }
        finally {
            tearDown();
        }

    }

    protected void
    setUp()
        throws Exception {
    }

    protected void
    tearDown()
        throws Exception {
    }

    protected abstract void
    doRunTest()
        throws Throwable;

    /**
     * Be verbose: print out what happens after this
     */
    public void
    beVerbose()
    {
        fVerbose = true;
    }

    /**
     * Print a string, if and only if verbose printing is enabled.
     */
    public void
    verbosePrint(String message)
    {
        if (fVerbose)
        {
            System.out.println("Debugging message: " + message);
        }
    }

    /*
     *  Replacement verification methods
     *  Shaped the same as the JUnit ones to make reusing the JUnit test possible
     *  Didn't implement them all, only the ones our existing tests use.
     */

    public final void
    fail() {
        throw new TestCaseScaffoldException();
    }

    public final void
    fail(String message) {
        throw new TestCaseScaffoldException(message);
    }

    public final void
    assertTrue(boolean condition) {
        if ( !condition ) {
            fail();
        }
    }

    public final void
    assertTrue(String message, boolean condition) {
        if ( !condition ) {
            fail(message);
        }
    }

    public final void
    assertNotNull(Object o) {
        assertTrue(o != null);
    }

    public final void
    assertNotNull(String message, Object o) {
        assertTrue(message, o != null);
    }

    public final void
    assertEquals(String message, Object expected, Object actual) {
        if ( (expected == null) && (actual == null) ) {
            return;
        }
        else if ( (expected != null) && (expected.equals(actual)) ) {
            return;
        }
        else {
            throw new TestCaseScaffoldException(message + ". Expected: '" + expected +
                                                "'. Actual: '" + actual + "'.");
        }
    }

    public final void
    assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    public final void
    assertEquals(String message, int expected, int actual) {
        assertEquals(message, new Integer(expected), new Integer(actual));
    }

    public final void
    assertEquals(int expected, int actual) {
        assertEquals("Expected equality", expected, actual);
    }

    public static final class
    TestCaseScaffoldException extends RuntimeException {
        public
        TestCaseScaffoldException() {
            super();
        }

        public
        TestCaseScaffoldException(String m) {
            super(m);
        }

    }

}
