/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;


/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * Simple tests for the Instrumentation
 *
 */
public abstract class
ASimpleInstrumentationTestCase
    extends AInstrumentationTestCase
{

    /**
     * Constructor for ASimpleInstrumentationTestCase.
     */
    public ASimpleInstrumentationTestCase(String name)
    {
        super(name);

    }

    protected void
    assertClassArrayContainsClass(Class[] list, Class target)
        {
        boolean inList = false;
        for ( int x = 0; x < list.length; x++ )
            {
            if ( list[x] == target )
                {
                inList = true;
                }
            }
        assertTrue(inList);
        }

    protected void
    assertClassArrayDoesNotContainClassByName(Class[] list, String name)
        {
        boolean inList = false;
        for ( int x = 0; x < list.length; x++ )
            {
            if ( list[x].getName().equals(name) )
                {
                fail();
                }
            }
        }

}
