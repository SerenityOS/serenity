/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2009-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tck.java.time.zone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.testng.annotations.Test;

/**
 * Test ZoneRulesProvider.
 */
@Test
public class TCKZoneRulesProvider {

    private static String TZDB_VERSION = "2012i";

    //-----------------------------------------------------------------------
    // getAvailableZoneIds()
    //-----------------------------------------------------------------------
    @Test
    public void test_getAvailableGroupIds() {
        Set<String> zoneIds = ZoneRulesProvider.getAvailableZoneIds();
        assertEquals(zoneIds.contains("Europe/London"), true);
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getAvailableGroupIds_modifyZoneId() {
        ZoneRulesProvider.getAvailableZoneIds().clear();
    }

    //-----------------------------------------------------------------------
    // getRules(String, boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_getRules_StringBoolean() {
        ZoneRules rules = ZoneRulesProvider.getRules("Europe/London", false);
        assertNotNull(rules);
        ZoneRules rules2 = ZoneRulesProvider.getRules("Europe/London", false);
        assertEquals(rules2, rules);
    }

    @Test(expectedExceptions=ZoneRulesException.class)
    public void test_getRules_StringBoolean_unknownId() {
        ZoneRulesProvider.getRules("Europe/Lon", false);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_getRules_StringBoolean_null() {
        ZoneRulesProvider.getRules(null, false);
    }

    @Test
    public void test_getRules_StringBoolean_dynamic() {
        MockDynamicProvider dynamicProvider = new MockDynamicProvider();
        ZoneRulesProvider.registerProvider(dynamicProvider);

        assertEquals(dynamicProvider.count, 0);
        ZoneRules rules1 = ZoneId.of("DynamicLocation").getRules();
        assertEquals(dynamicProvider.count, 2);
        assertEquals(rules1, dynamicProvider.BASE);
        ZoneRules rules2 = ZoneId.of("DynamicLocation").getRules();
        assertEquals(dynamicProvider.count, 4);
        assertEquals(rules2, dynamicProvider.ALTERNATE);
    }

    //-----------------------------------------------------------------------
    // getVersions(String)
    //-----------------------------------------------------------------------
    @Test
    public void test_getVersions_String() {
        NavigableMap<String, ZoneRules> versions = ZoneRulesProvider.getVersions("Europe/London");
        assertTrue(versions.size() >= 1);
        ZoneRules rules = ZoneRulesProvider.getRules("Europe/London", false);
        assertEquals(versions.lastEntry().getValue(), rules);

        NavigableMap<String, ZoneRules> copy = new TreeMap<>(versions);
        versions.clear();
        assertEquals(versions.size(), 0);
        NavigableMap<String, ZoneRules> versions2 = ZoneRulesProvider.getVersions("Europe/London");
        assertEquals(versions2, copy);
    }

    @Test(expectedExceptions=ZoneRulesException.class)
    public void test_getVersions_String_unknownId() {
        ZoneRulesProvider.getVersions("Europe/Lon");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_getVersions_String_null() {
        ZoneRulesProvider.getVersions(null);
    }

    //-----------------------------------------------------------------------
    // refresh()
    //-----------------------------------------------------------------------
    @Test
    public void test_refresh() {
        assertEquals(ZoneRulesProvider.refresh(), false);
    }

    //-----------------------------------------------------------------------
    // registerProvider()
    //-----------------------------------------------------------------------
    @Test
    public void test_registerProvider() {
        Set<String> pre = ZoneRulesProvider.getAvailableZoneIds();
        assertEquals(pre.contains("FooLocation"), false);
        ZoneRulesProvider.registerProvider(new MockTempProvider());
        assertEquals(pre.contains("FooLocation"), false);
        Set<String> post = ZoneRulesProvider.getAvailableZoneIds();
        assertEquals(post.contains("FooLocation"), true);
        assertEquals(ZoneRulesProvider.getRules("FooLocation", false), ZoneOffset.of("+01:45").getRules());
    }

    static class MockTempProvider extends ZoneRulesProvider {
        final ZoneRules rules = ZoneOffset.of("+01:45").getRules();
        @Override
        public Set<String> provideZoneIds() {
            return new HashSet<String>(Collections.singleton("FooLocation"));
        }
        @Override
        protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
            NavigableMap<String, ZoneRules> result = new TreeMap<>();
            result.put("BarVersion", rules);
            return result;
        }
        @Override
        protected ZoneRules provideRules(String zoneId, boolean forCaching) {
            if (zoneId.equals("FooLocation")) {
                return rules;
            }
            throw new ZoneRulesException("Invalid");
        }
    }

    static class MockDynamicProvider extends ZoneRulesProvider {
        final ZoneRules BASE = ZoneOffset.of("+01:15").getRules();
        final ZoneRules ALTERNATE = ZoneOffset.of("+01:30").getRules();
        int count = 0;
        @Override
        public Set<String> provideZoneIds() {
            return new HashSet<>(Collections.singleton("DynamicLocation"));
        }
        @Override
        protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
            NavigableMap<String, ZoneRules> result = new TreeMap<>();
            result.put("DynamicVersion1", BASE);
            if (count > 2) {
                result.put("DynamicVersion2", ALTERNATE);
            }
            return result;
        }
        @Override
        protected ZoneRules provideRules(String zoneId, boolean forCaching) {
            count++;
            if (zoneId.equals("DynamicLocation")) {
                return (forCaching ? null : (count > 2 ? ALTERNATE : BASE));
            }
            throw new ZoneRulesException("Invalid");
        }
    }

}
