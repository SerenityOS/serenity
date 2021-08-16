/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng DropLookupModeTest
 * @summary Basic unit tests Lookup::dropLookupMode
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class DropLookupModeTest {

    /**
     * Basic test of dropLookupMode
     */
    public void testBasic() {
        final Lookup fullPowerLookup = MethodHandles.lookup();
        final Class<?> lc = fullPowerLookup.lookupClass();
        assertTrue(fullPowerLookup.lookupModes() == (PUBLIC|MODULE|PACKAGE|PROTECTED|PRIVATE|ORIGINAL));

        Lookup lookup = fullPowerLookup.dropLookupMode(PRIVATE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE));

        lookup = fullPowerLookup.dropLookupMode(PROTECTED);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE|PRIVATE));

        lookup = fullPowerLookup.dropLookupMode(PACKAGE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE));

        lookup = fullPowerLookup.dropLookupMode(MODULE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC));

        lookup = fullPowerLookup.dropLookupMode(PUBLIC);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == 0);

        lookup = fullPowerLookup.dropLookupMode(UNCONDITIONAL);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE|PRIVATE));
    }

    /**
     * Starting with a full power Lookup, use dropLookupMode to create new Lookups
     * with reduced access.
     */
    public void testReducingAccess() {
        Lookup lookup = MethodHandles.lookup();
        final Class<?> lc = lookup.lookupClass();
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE|PROTECTED|PRIVATE|ORIGINAL));

        lookup = lookup.dropLookupMode(PROTECTED);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE|PRIVATE));

        lookup = lookup.dropLookupMode(PRIVATE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE|PACKAGE));

        lookup = lookup.dropLookupMode(PACKAGE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == (PUBLIC|MODULE));

        lookup = lookup.dropLookupMode(MODULE);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == PUBLIC);

        lookup = lookup.dropLookupMode(PUBLIC);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == 0);

        // repeat with lookup has no access
        lookup = lookup.dropLookupMode(PUBLIC);
        assertTrue(lookup.lookupClass() == lc);
        assertTrue(lookup.lookupModes() == 0);
    }

    @DataProvider(name = "unconditionals")
    public Object[][] unconditionals() {
        Lookup publicLookup = MethodHandles.publicLookup();
        return new Object[][] {
            { publicLookup, Object.class },
            { publicLookup.in(String.class), String.class },
            { publicLookup.in(DropLookupModeTest.class), DropLookupModeTest.class },
        };
    }

    /**
     * Test dropLookupMode on the lookup with public lookup
     * and UNCONDITIONAL
     */
    @Test(dataProvider = "unconditionals")
    public void testUnconditionalLookup(Lookup unconditionalLookup, Class<?> expected) {
        assertTrue(unconditionalLookup.lookupModes() == UNCONDITIONAL);

        assertPublicLookup(unconditionalLookup.dropLookupMode(PRIVATE), expected);
        assertPublicLookup(unconditionalLookup.dropLookupMode(PROTECTED), expected);
        assertPublicLookup(unconditionalLookup.dropLookupMode(PACKAGE), expected);
        assertPublicLookup(unconditionalLookup.dropLookupMode(MODULE), expected);
        assertPublicLookup(unconditionalLookup.dropLookupMode(PUBLIC), expected);

        // drop all access
        Lookup lookup = unconditionalLookup.dropLookupMode(UNCONDITIONAL);
        assertTrue(lookup.lookupClass() == expected);
        assertTrue(lookup.lookupModes() == 0);
    }

    private void assertPublicLookup(Lookup lookup, Class<?> expected) {
        assertTrue(lookup.lookupClass() == expected);
        assertTrue(lookup.lookupModes() == UNCONDITIONAL);
    }

    @DataProvider(name = "badInput")
    public Object[][] badInput() {
        return new Object[][] {
                { 0,                        null },
                { (PACKAGE|PRIVATE),        null },    // two modes
                { Integer.MAX_VALUE,        null },
                { Integer.MIN_VALUE,        null },
        };
    }

    /**
     * Check that IllegalArgumentException is thrown for bad input
     */
    @Test(dataProvider = "badInput", expectedExceptions = {IllegalArgumentException.class})
    public void testBadInput(Integer modeToDrop, Object ignore) {
        MethodHandles.lookup().dropLookupMode(modeToDrop);
    }

}
