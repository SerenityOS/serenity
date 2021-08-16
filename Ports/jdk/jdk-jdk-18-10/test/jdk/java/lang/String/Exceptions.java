/*
 * Copyright (c) 2002, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4472841 4703640 4705681 4705683 4833095 5005831
 * @summary Verify that constructor exceptions are thrown as expected.
 */

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Exceptions {
    private static final byte [] b = { 0x48, 0x69, 0x2c, 0x20,
                                       0x44, 0x75, 0x6b, 0x65, 0x21 };

    private static final char [] c
        = "Attack of the Killer Tomatoes!".toCharArray();

    private static boolean ok = true;

    private static void fail(Class ex, String s) {
        ok = false;
        System.err.println("expected " + ex.getName() + " for " + s
                               + " - FAILED");
    }

    private static void pass(String s) {
        System.out.println(s + " -- OK");
    }

    private static void tryCatch(String s, Class ex, Runnable thunk) {
        Throwable t = null;
        try {
            thunk.run();
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                t = x;
            else
                x.printStackTrace();
        }
        if ((t == null) && (ex != null))
            fail(ex, s);
        else
            pass(s);
    }

    // -- Constructors --

    private static void noArgs() {
        System.out.println("String()");
        tryCatch("  default ctor", null, new Runnable() {
                public void run() {
                    new String();
                }});
    }

    private static void string() {
        System.out.println("String(String original)");
        tryCatch("  \"foo\"", null, new Runnable() {
                public void run() {
                    new String("foo");
                }});
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((String) null);
                }});
    }

    private static void charArray() {
        System.out.println("String(char value[])");
        tryCatch("  char [] = \"Duke says \"Hi!\"\"", null, new Runnable() {
                public void run() {
                    new String("Duke says \"Hi!\"".toCharArray());
                }});
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((char []) null);
                }});
    }

    private static void charArrayOffCount() {
        System.out.println("String(char value[], int offset, int count)");
        tryCatch("  c, 0, 3", null, new Runnable() {
                public void run() {
                    new String(c, 0, 3);
                }});
        tryCatch("  null, 1, 2", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((char []) null, 1, 2);
                }});
        tryCatch("  c, -1, 4", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(c, -1, 4);
                         }});
        tryCatch("  c, 1, -1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(c, 1, -1);
                         }});
        tryCatch("  c, c.lengh + 1, 1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(c, c.length + 1, 1);
                         }});
        tryCatch("  c, 0, c.length + 1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(c, 0, c.length + 1);
                         }});
    }

    private static void byteArrayHiOffCount() {
        System.out.println("String(byte ascii[], int hibyte, int offset, "
                           + "int count)");
        tryCatch("  b, 0, 0, b.length", null, new Runnable() {
                public void run() {
                    System.out.println(new String(b, 0, 0, b.length));
                }});

        tryCatch("  b, -1, 4, 4", null, new Runnable() {
                public void run() {
                    new String(b, -1, 4, 4);
                }});
        tryCatch("  null, 0, 0, 0", NullPointerException.class,
                 new Runnable() {
                         public void run() {
                             new String((byte[]) null, 0, 0, 0);
                         }});
        tryCatch("  b, 0, -1, r", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, -1, 4);
                         }});
        tryCatch("  b, 0, 4, -1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, 4, -1);
                         }});
        tryCatch("  b, 0, b.length + 1, 1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, b.length + 1, 1);
                         }});
        tryCatch("  b, 0, 0, b.length + 1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, 0, b.length + 1);
                         }});
    }

    private static void byteArrayHi() {
        System.out.println("String(byte ascii[], int hibyte)");
        tryCatch("  b, 0", null, new Runnable() {
                public void run() {
                    new String(b, 0);
                }});
        tryCatch("  null, 0", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((byte []) null, 0);
                }});
    }

    private static void byteArrayOffLengthCharset0(String s, Class ex,
                                                   byte [] b, int off,
                                                   int len, Object cs)
    {
        Throwable t = null;
        try {
            if (cs instanceof String)
                new String(b, off, len, (String)cs);
            else // (cs instanceof Charset)
                new String(b, off, len, (Charset)cs);
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                t = x;
            else
                x.printStackTrace();
        }
        if ((t == null) && (ex != null))
            fail(ex, s);
        else
            pass(s);
    }

    private static void byteArrayOffLengthCharsetName() {
        System.out.println("String(byte bytes[], int offset, int length, "
                           + "String charsetName)");
        System.out.println("  throws UnsupportedEncodingException");
        String enc = "UTF-8";
        byteArrayOffLengthCharset0("  b, 0, 0," + enc, null, b, 0, 0, enc);
        byteArrayOffLengthCharset0("  null, 0, 0," + enc,
                                   NullPointerException.class,
                                   (byte []) null, 0, 0, enc);
        byteArrayOffLengthCharset0("  b, -1, 0, " + enc,
                                   IndexOutOfBoundsException.class,
                                   b, -1, 0, enc);
        byteArrayOffLengthCharset0("  b, 0, -1, " + enc,
                                   IndexOutOfBoundsException.class,
                                   b, 0, -1, enc);
        byteArrayOffLengthCharset0("  b, b.length + 1, 1, " + enc,
                                   IndexOutOfBoundsException.class,
                                   b, b.length + 1, 1, enc);
        byteArrayOffLengthCharset0("  b, 0, b.length + 1 " + enc,
                                   IndexOutOfBoundsException.class,
                                   b, 0, b.length + 1, enc);
        byteArrayOffLengthCharset0("  b, -1, 0, null",
                                   NullPointerException.class,
                                   b, -1, 0, null);
        byteArrayOffLengthCharset0("  b, 0, b.length, foo",
                                   UnsupportedEncodingException.class,
                                   b, 0, b.length, "foo");
    }

    private static void byteArrayOffLengthCharset() {
        System.out.println("String(byte bytes[], int offset, int length, "
                           + "Charset charset)");
        Charset cs = Charset.forName("UTF-16BE");
        byteArrayOffLengthCharset0("  b, 0, 0," + cs, null, b, 0, 0, cs);
        byteArrayOffLengthCharset0("  null, 0, 0," + cs,
                                   NullPointerException.class,
                                   (byte []) null, 0, 0, cs);
        byteArrayOffLengthCharset0("  b, -1, 0, " + cs,
                                   IndexOutOfBoundsException.class,
                                   b, -1, 0, cs);
        byteArrayOffLengthCharset0("  b, 0, -1, " + cs,
                                   IndexOutOfBoundsException.class,
                                   b, 0, -1, cs);
        byteArrayOffLengthCharset0("  b, b.length + 1, 1, " + cs,
                                   IndexOutOfBoundsException.class,
                                   b, b.length + 1, 1, cs);
        byteArrayOffLengthCharset0("  b, 0, b.length + 1 " + cs,
                                   IndexOutOfBoundsException.class,
                                   b, 0, b.length + 1, cs);
        byteArrayOffLengthCharset0("  b, -1, 0, null",
                                   NullPointerException.class,
                                   b, -1, 0, null);
    }

    private static void byteArrayCharset0(String s, Class ex, byte [] b,
                                          Object cs)
    {
        Throwable t = null;
        try {
            if (cs instanceof String)
                new String(b, (String)cs);
            else // (cs instanceof Charset)
                new String(b, (Charset)cs);
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                t = x;
            else
                x.printStackTrace();
        }
        if ((t == null) && (ex != null))
            fail(ex, s);
        else
            pass(s);
    }

    private static void byteArrayCharsetName() {
        System.out.println("String(byte bytes[], String charsetName)");
        System.out.println("  throws UnsupportedEncodingException");
        String enc = "US-ASCII";
        byteArrayCharset0("  b, " + enc, null, b, enc);
        byteArrayCharset0("  null, " + enc, NullPointerException.class,
                          (byte []) null, enc);
        byteArrayCharset0("  b, null", NullPointerException.class, b, null);
        byteArrayCharset0("  null, null", NullPointerException.class,
                          (byte []) null, null);
        byteArrayCharset0("  b, bar", UnsupportedEncodingException.class,
                          b, "bar");
    }

    private static void byteArrayCharset() {
        System.out.println("String(byte bytes[], Charset charset)");
        Charset cs = Charset.forName("ISO-8859-1");
        byteArrayCharset0("  b, " + cs, null, b, cs);
        byteArrayCharset0("  null, " + cs, NullPointerException.class,
                          (byte []) null, cs);
        byteArrayCharset0("  b, null", NullPointerException.class, b, null);
        byteArrayCharset0("  null, null", NullPointerException.class,
                          (byte []) null, null);
    }

    private static void byteArrayOffLength() {
        System.out.println("String(byte bytes[], int offset, int length)");
        tryCatch("  b, 0, b.length", null, new Runnable() {
                public void run() {
                    new String(b, 0, b.length);
                }});
        tryCatch("  null, 0, 0", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((byte []) null, 0, 0);
                }});
        tryCatch("  b, -1, b.length", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, -1, b.length);
                         }});
        tryCatch("  b, 0, -1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, -1);
                         }});
        tryCatch("  b, b.length + 1, 1", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, b.length + 1, 1);
                         }});
        tryCatch("  b, 0, b.length", IndexOutOfBoundsException.class,
                 new Runnable() {
                         public void run() {
                             new String(b, 0, b.length + 1);
                         }});
    }

    private static void byteArray() {
        System.out.println("String(byte bytes[])");
        tryCatch("  b", null, new Runnable() {
                public void run() {
                    new String(b);
                }});
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((byte []) null);
                }});
    }

    private static void stringBuffer() {
        System.out.println("String(StringBuffer buffer)");
        tryCatch("  \"bar\"", null, new Runnable() {
                public void run() {
                    new String(new StringBuffer("bar"));
                }});
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    new String((StringBuffer) null);
                }});
    }

    // -- Methods --

        private static void getChars() {
        System.out.println("getChars.(int srcBegin, int srcEnd, char dst[], "
                           + " int dstBegin");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".getChars(1, 2, null, 1);
                }});
    }

    private static void getBytes() {
        System.out.println("getChars.(int srcBegin, int srcEnd, char dst[], "
                           + " int dstBegin");
        tryCatch("  1, 2, null, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".getBytes(1, 2, null, 1);
                }});

        System.out.println("getBytes.(String charsetName)"
                           + " throws UnsupportedEncodingException");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    try {
                        "foo".getBytes((String)null);
                    } catch (UnsupportedEncodingException x) {
                        throw new RuntimeException(x);
                    }
                }});

        System.out.println("getBytes.(Charset charset)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".getBytes((Charset)null);
                }});
    }

    private static void contentEquals() {
        System.out.println("contentEquals(StringBuffer sb)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".contentEquals(null);
                }});
    }

    private static void compareTo() {
        System.out.println("compareTo(String anotherString)");
        tryCatch("  (String) null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".compareTo((String) null);
                }});

        /* 4830291 (javac generics bug) causes this test to fail
        System.out.println("compareTo(Object o)");
        tryCatch("  (Object) null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".compareTo((Object) null);
                }});
        */
    }

    private static void compareToIgnoreCase() {
        System.out.println("compareToIgnoreCase(String anotherString)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".compareToIgnoreCase((String) null);
                }});
    }

    private static void regionMatches() {
        System.out.println("regionMatches(int toffset, String other,"
                           + " int ooffset, int len)");
        tryCatch("  1, null, 1, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".regionMatches(1, null, 1, 1);
                }});

        System.out.println("regionMatches(boolean ignore, int toffset,"
                           + " String other, int ooffset, int len)");
        tryCatch("  true, 1, null, 1, 1", NullPointerException.class,
                 new Runnable() {
                         public void run() {
                             "foo".regionMatches(true, 1, null, 1, 1);
                         }});
    }

    private static void startsWith() {
        System.out.println("startsWith(String prefix, int toffset)");
        tryCatch("  null, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".startsWith(null, 1);
                }});

        System.out.println("startsWith(String prefix)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".startsWith(null);
                }});
    }

    private static void endsWith() {
        System.out.println("endsWith(String suffix)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".endsWith(null);
                }});
    }

    private static void indexOf() {
        System.out.println("indexOf(String str)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".indexOf(null);
                }});

        System.out.println("indexOf(String str, int fromIndex)");
        tryCatch("  null, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".indexOf(null, 1);
                }});
    }

    private static void lastIndexOf() {
        System.out.println("lastIndexOf(String str)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".lastIndexOf(null);
                }});

        System.out.println("lastIndexOf(String str, int fromIndex)");
        tryCatch("  null, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".lastIndexOf(null, 1);
                }});
    }

    private static void concat() {
        System.out.println("concat(String str)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".concat(null);
                }});
    }

    private static void matches() {
        System.out.println("matches(String regex)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".matches(null);
                }});
    }

    private static void replaceFirst() {
        System.out.println("replaceFirst(String regex, String replacement)");
        tryCatch("  \".\", null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".replaceFirst(".", null);
                }});
        tryCatch("  null, \"-\"", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".replaceFirst(null, "-");
                }});
    }

    private static void replaceAll() {
        System.out.println("replaceAll(String regex, String replacement)");
        tryCatch("  \".\", null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".replaceAll(".", null);
                }});
        tryCatch("  null, \"-\"", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".replaceAll(null, "-");
                }});
    }

    private static void split() {
        System.out.println("split(String regex, int limit)");
        tryCatch("  null, 1", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".split(null, 1);
                }});

        System.out.println("split(String regex, int limit)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".split(null);
                }});
    }

    private static void toLowerCase() {
        System.out.println("toLowerCase(Locale locale)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".toLowerCase(null);
                }});
    }

    private static void toUpperCase() {
        System.out.println("toUpperCase(Locale locale)");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".toUpperCase(null);
                }});
    }

    private static void valueOf() {
        System.out.println("valueOf(Object obj)");
        tryCatch("  null", null, new Runnable() {
                public void run() {
                    String.valueOf((Object) null);
                }});

        System.out.println("valueOf(char data[])");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    String.valueOf((char []) null);
                }});

        System.out.println("valueOf(char data[], int offset, int count)");
        tryCatch("  null, 1, 2", NullPointerException.class, new Runnable() {
                public void run() {
                    String.valueOf((char []) null, 1, 2);
                }});

    }

    private static void copyValueOf() {
        System.out.println("copyValueOf(char data[], int offset, int count)");
        tryCatch("  null, 1, 2", NullPointerException.class, new Runnable() {
                public void run() {
                    "foo".copyValueOf((char []) null, 1, 2);
                }});

        System.out.println("copyVlueOf(char data[])");
        tryCatch("  null", NullPointerException.class, new Runnable() {
                public void run() {
                    String.copyValueOf((char []) null);
                }});
    }

    public static void main(String [] args) {

        // -- Constructors --

        noArgs();             // String()
        string();             // String(String original)
        charArray();          // String(char value[])
        charArrayOffCount();  // String(char value[], int offset, int count)

        // String(byte ascii[], int hibyte, int offset, int count)
        byteArrayHiOffCount();

        byteArrayHi();        // String(byte ascii[], int hibyte)

        // String(byte bytes[], int offset, int length, String charsetName)
        //   throws UnsupportedEncodingException
        byteArrayOffLengthCharsetName();

        // String(byte bytes[], int offset, int length, Charset charset)
        byteArrayOffLengthCharset();

        // String(byte bytes[], String charsetName)
        //   throws UnsupportedEncodingException
        byteArrayCharsetName();

        // String(byte bytes[], Charset charset)
        byteArrayCharset();

        byteArrayOffLength(); // String(byte bytes[], int offset, int length)
        byteArray();          // String(byte bytes[])
        stringBuffer();       // String(StringBuffer buffer)

        // -- Methods --

        getChars();           // getChars(int, int. char [], int)
        getBytes();           // getBytes(int, int, byte [], int),
                              //   getBytes(Locale)
                              //   getBytes(String)
                              //   getBytes(Charset)
        contentEquals();      // contentEquals(StringBuffer)
        compareTo();          // compareTo(String), compareTo(Object)
        compareToIgnoreCase();// compareToIgnoreCase(String)
        regionMatches();      // regionMatches(int, String, int, int)
                              //   regionMatches(boolean, int, String, int, int)
        startsWith();         // startsWith(String, int), startsWith(String)
        endsWith();           // endsWith(String)
        indexOf();            // indexOf(String), indexOf(String, int),
        lastIndexOf();        // lastIndexOf(String), lastIndexOf(String, int)
        concat();             // concat(String)
        matches();            // matches(String)
        replaceFirst();       // replaceFirst(String, String)
        replaceAll();         // replaceAll(String, String)
        split();              // split(String, int), split(String)
        toLowerCase();        // toLowerCase(Locale)
        toUpperCase();        // toUpperCase(Locale)
        valueOf();            // valueOf(Object), valueOf(char []),
                              //   valueOf(char [], int, int)
        copyValueOf();        // copyValueOf(char [], int, int),
                              //    copyValueOf(char [])

        if (!ok)
            throw new RuntimeException("Some tests FAILED");
        else
            System.out.println("All tests PASSED");
    }
}
