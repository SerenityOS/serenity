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

import java.lang.instrument.Instrumentation;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * A JPLIS agent that makes the Instrumentation available via a static accessor.
 * Used so that unit test frameworks that run as apps can exercise the Instrumentation--
 * configure this guy as a JPLIS agent, then call the Instrumentation fetcher from the test case.
 *
 */
public class InstrumentationHandoff
{
    private static Instrumentation      sInstrumentation;

    // disallow construction
    private
    InstrumentationHandoff()
        {
        }

    public static void
    premain(String options, Instrumentation inst)
    {
        System.out.println("InstrumentationHandoff JPLIS agent initialized");
        sInstrumentation = inst;
    }

    // may return null
    public static Instrumentation
    getInstrumentation()
    {
        return sInstrumentation;
    }

    public static Instrumentation
    getInstrumentationOrThrow()
    {
        Instrumentation result = getInstrumentation();
        if ( result == null )
            {
            throw new NullPointerException("instrumentation instance not initialized");
            }
        return result;
    }


}
