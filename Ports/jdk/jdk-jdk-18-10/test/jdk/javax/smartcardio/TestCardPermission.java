/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6293767 6469513 8255546
 * @summary Test for the CardPermission class
 * @author Andreas Sterbenz
 * @run testng TestCardPermission
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.smartcardio.*;
import java.security.Permission;

import static org.testng.Assert.*;

public class TestCardPermission {

    @DataProvider(name = "actions")
    Object[][] getActions() {
        return new Object[][]{
                {"*"},
                {"connect"},
                {"reset"},
                {"exclusive"},
                {"transmitControl"},
                {"getBasicChannel"},
                {"openLogicalChannel"},
                {"connect,reset"}
        };
    }

    @DataProvider(name = "actionsCanon")
    Object[][] getActionsCanon() {
        return new Object[][]{
                {"Reset,coNnect", "connect,reset"},
                {"exclusive,*,connect", "*"},
                {"connect,reset,exclusive,transmitControl,getBasicChannel,openLogicalChannel", "*"},
                {null, null}
        };
    }

    @DataProvider(name = "invalidActions")
    Object[][] getInvalidActions() {
        return new Object[][]{
                {""},
                {"foo"},
                {"connect, reset"},
                {"connect,,reset"},
                {"connect,"},
                {",connect"}
        };
    }

    @Test(dataProvider = "actions")
    public void testActions(String actions) throws Exception {
        testActions(actions, actions);
    }

    @Test(dataProvider = "actionsCanon")
    public void testActionsCanon(String actions, String canon) throws Exception {
        testActions(actions, canon);
    }

    @Test(dataProvider = "invalidActions")
    public void testInvalidActions(String actions) {
        assertThrows(IllegalArgumentException.class, () -> new CardPermission("*", actions));
    }

    // Should return false since p2 is not a CardPermission instance
    @Test
    public void testImpliesNotCardPermissionInstance() {
        String actions = "connect";
        CardPermission p1 = new CardPermission("*", actions);
        Permission p2 = new Permission(actions) {
            @Override public boolean implies(Permission permission) { return false; }
            @Override public boolean equals(Object obj) { return false; }
            @Override public int hashCode() { return 0; }
            @Override public String getActions() { return null; }
        };
        assertFalse(p1.implies(p2));
    }

    // Should return false since p2 actions are not a subset of p1
    @Test
    public void testImpliesNotSubsetCardPermission() {
        CardPermission p1 = new CardPermission("*", "connect,reset");
        CardPermission p2 = new CardPermission("*", "transmitControl");
        assertFalse(p1.implies(p2));
    }

    // Should return true since p1 name is * and p2 actions are a subset of p1
    @Test
    public void testImpliesNameEqualsAll() {
        CardPermission p1 = new CardPermission("*", "connect,reset");
        CardPermission p2 = new CardPermission("None", "reset");
        assertTrue(p1.implies(p2));
    }

    // Should return true since p1 and p2 names are equal
    @Test
    public void testImpliesBothSameNameNotAll() {
        CardPermission p1 = new CardPermission("None", "connect,reset");
        CardPermission p2 = new CardPermission("None", "reset");
        assertTrue(p1.implies(p2));
    }

    // Should return false since p1 and p2 names are not equal
    @Test
    public void testImpliesNameNotSameNotAll() {
        CardPermission p1 = new CardPermission("None", "connect,reset");
        CardPermission p2 = new CardPermission("Other", "reset");
        assertFalse(p1.implies(p2));
    }

    private void testActions(String actions, String canon) throws Exception {
        CardPermission p = new CardPermission("*", actions);
        System.out.println(p);
        String a = p.getActions();
        if (canon != null && !canon.equals(a)) {
            throw new Exception("Canonical actions mismatch: " + canon + " != " + a);
        }
    }
}
