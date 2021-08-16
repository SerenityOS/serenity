/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6206780
 * @summary  Test forwarding of methods to super in StringBuffer
 * @author Jim Gish <jim.gish@oracle.com>
 */

import java.util.ArrayList;
import java.util.List;

public class BufferForwarding {
    private static final String A_STRING_BUFFER_VAL = "aStringBuffer";
    private static final String A_STRING_BUILDER_VAL = "aStringBuilder";
    private static final String A_STRING_VAL = "aString";
    private static final String NON_EMPTY_VAL = "NonEmpty";

    public BufferForwarding() {
        System.out.println( "Starting BufferForwarding");
    }

    public static void main(String... args) {
        new BufferForwarding().executeTestMethods();
    }

    public void executeTestMethods() {
        appendCharSequence();
        indexOfString();
        indexOfStringIntNull();
        indexOfStringNull();
        indexOfStringint();
        insertintCharSequence();
        insertintObject();
        insertintboolean();
        insertintchar();
        insertintdouble();
        insertintfloat();
        insertintint();
        insertintlong();
        lastIndexOfString();
        lastIndexOfStringint();
    }

    public void appendCharSequence() {
        // three different flavors of CharSequence
        CharSequence aString = A_STRING_VAL;
        CharSequence aStringBuilder = new StringBuilder(A_STRING_BUILDER_VAL);
        CharSequence aStringBuffer = new StringBuffer(A_STRING_BUFFER_VAL);

        assertEquals( /*actual*/ new StringBuilder().append(aString).toString(), /*expected*/ A_STRING_VAL );
        assertEquals( new StringBuilder().append(aStringBuilder).toString(), A_STRING_BUILDER_VAL );
        assertEquals( new StringBuilder().append(aStringBuffer).toString(), A_STRING_BUFFER_VAL );

        assertEquals( /*actual*/ new StringBuilder(NON_EMPTY_VAL).append(aString).toString(), NON_EMPTY_VAL+A_STRING_VAL );
        assertEquals( new StringBuilder(NON_EMPTY_VAL).append(aStringBuilder).toString(), NON_EMPTY_VAL+A_STRING_BUILDER_VAL );
        assertEquals( new StringBuilder(NON_EMPTY_VAL).append(aStringBuffer).toString(), NON_EMPTY_VAL+A_STRING_BUFFER_VAL );
    }

    void indexOfString() {
        StringBuffer sb = new StringBuffer("xyz");
        assertEquals( sb.indexOf("y"), 1 );
        assertEquals( sb.indexOf("not found"), -1 );
    }


    public void indexOfStringint() {
        StringBuffer sb = new StringBuffer("xyyz");
        assertEquals( sb.indexOf("y",0), 1 );
        assertEquals( sb.indexOf("y",1), 1 );
        assertEquals( sb.indexOf("y",2), 2 );
        assertEquals( sb.indexOf("not found"), -1 );
    }


    public void indexOfStringIntNull() {
        StringBuffer sb = new StringBuffer();
        // should be NPE if null passed
        try {
            sb.indexOf(null,1);
            throw new RuntimeException("Test failed: should have thrown NPE");
        } catch (NullPointerException npe) {
            // expected: passed
        } catch (Throwable t) {
            throw new RuntimeException("Test failed: should have thrown NPE. Instead threw "
                    + t);
        }
    }


    public void indexOfStringNull() {
        StringBuffer sb = new StringBuffer();

        // should be NPE if null passed
        try {
            sb.indexOf(null);
            throw new RuntimeException("Test failed: should have thrown NPE");
        } catch (NullPointerException npe) {
            // expected: passed
        } catch (Throwable t) {
            throw new RuntimeException("Test failed: should have thrown NPE. Instead threw "
                    + t);
        }
    }


    public void insertintboolean() {
        boolean b = true;
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, b).toString(), "01true2345");
    }


    public void insertintchar() {
        char c = 'C';
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, c ).toString(), "01C2345");
    }


    public void insertintCharSequence() {
        final String initString = "012345";
        // three different flavors of CharSequence
        CharSequence aString = A_STRING_VAL;
        CharSequence aStringBuilder = new StringBuilder(A_STRING_BUILDER_VAL);
        CharSequence aStringBuffer = new StringBuffer(A_STRING_BUFFER_VAL);

        assertEquals( new StringBuffer(initString).insert(2, aString).toString(), "01"+A_STRING_VAL+"2345" );

        assertEquals( new StringBuffer(initString).insert(2, aStringBuilder).toString(), "01"+A_STRING_BUILDER_VAL+"2345" );

        assertEquals( new StringBuffer(initString).insert(2, aStringBuffer).toString(), "01"+A_STRING_BUFFER_VAL+"2345" );

        try {
            new StringBuffer(initString).insert(7, aString);
            throw new RuntimeException("Test failed: should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException soob) {
            // expected: passed
        } catch (Throwable t) {
            throw new RuntimeException("Test failed: should have thrown IndexOutOfBoundsException, but instead threw " + t.getMessage());

        }
    }

    public void insertintdouble() {
        double d = 99d;
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, d ).toString(), "0199.02345");    }


    public void insertintfloat() {
        float f = 99.0f;
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, f ).toString(), "0199.02345");    }


    public void insertintint() {
        int i = 99;
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, i ).toString(), "01992345");
    }


    public void insertintlong() {
        long l = 99;
        StringBuffer sb = new StringBuffer("012345");
        assertEquals( sb.insert( 2, l ).toString(), "01992345");    }

    public void insertintObject() {
        StringBuffer sb = new StringBuffer("012345");
        List<String> ls = new ArrayList<String>();
        ls.add("A"); ls.add("B");
        String lsString = ls.toString();
        assertEquals( sb.insert(2, ls).toString(), "01"+lsString+"2345");

        try {
            sb.insert(sb.length()+1, ls);
            throw new RuntimeException("Test failed: should have thrown StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException soob) {
            // expected: passed
        } catch (Throwable t) {
            throw new RuntimeException("Test failed: should have thrown StringIndexOutOfBoundsException, but instead threw:"
                    + t);

        }
    }



    public void lastIndexOfString() {
        String xyz = "xyz";
        String xyz3 = "xyzxyzxyz";
        StringBuffer sb = new StringBuffer(xyz3);
        int pos = sb.lastIndexOf("xyz");
        assertEquals( pos, 2*xyz.length() );
    }

    public void lastIndexOfStringint() {
        StringBuffer sb = new StringBuffer("xyzxyzxyz");
        int pos = sb.lastIndexOf("xyz",5);
        assertEquals( pos, 3 );
        pos = sb.lastIndexOf("xyz", 6);
        assertEquals( pos, 6 );
    }

    public void assertEquals( String actual, String expected) {
        if (!actual.equals( expected )) {
            throw new RuntimeException( "Test failed: actual = '" + actual +
                    "', expected = '" + expected + "'");
        }
    }

    public void assertEquals( int actual, int expected) {
        if (actual != expected) {
            throw new RuntimeException( "Test failed: actual = '" + actual +
                    "', expected = '" + expected + "'");
        }
    }
}
