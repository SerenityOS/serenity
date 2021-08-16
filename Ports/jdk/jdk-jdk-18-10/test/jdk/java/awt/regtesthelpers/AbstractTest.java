/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * <p>This is the base class for automatic main tests.
 * <p>When using jtreg you would include this class into
 * the build list via something like:
 * <pre>
     @library ../../../regtesthelpers
     @build AbstractTest
     @run main YourTest
   </pre>
 * Note that if you are about to create a test based on
 * Applet-template, then put those lines into html-file, not in java-file.
 * <p> And put an
 * import test.java.awt.regtesthelpers.AbstractTest;
 * into the java source.
 */

package test.java.awt.regtesthelpers;

public abstract class AbstractTest
{
    public static void pass()
    {
        Sysout.println( "The test passed." );
        Sysout.println( "The test is over, hit  Ctl-C to stop Java VM" );
    }//pass()

    public static void fail()
    {
        //test writer didn't specify why test failed, so give generic
        fail("no reason given.");
    }

    public static void fail( String whyFailed )
    {
        Sysout.println( "The test failed: " + whyFailed );
        Sysout.println( "The test is over, hit  Ctl-C to stop Java VM" );
        throw new RuntimeException( whyFailed );
    }

    public static void fail(Exception ex) throws Exception
    {
        Sysout.println( "The test failed with exception:" );
        ex.printStackTrace();
        Sysout.println( "The test is over, hit  Ctl-C to stop Java VM" );
        throw ex;
    }
}
