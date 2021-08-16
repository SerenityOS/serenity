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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
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
package test.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.Duration;

import org.testng.annotations.Test;

/**
 * Test Duration.
 */
@Test
public class TestDuration extends AbstractTest {

    //-----------------------------------------------------------------------
    @Test
    public void test_immutable() {
        assertImmutable(Duration.class);
    }

    @Test
    public void plus_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(-1);
        assertSame(t.plus(Duration.ZERO), t);
    }

    @Test
    public void plus_zeroSingleton() {
        Duration t = Duration.ofSeconds(-1);
        assertSame(t.plus(Duration.ofSeconds(1)), Duration.ZERO);
    }

    @Test
    public void plusSeconds_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(-1);
        assertSame(t.plusSeconds(0), t);
    }

    @Test
    public void plusSeconds_zeroSingleton() {
        Duration t = Duration.ofSeconds(-1);
        assertSame(t.plusSeconds(1), Duration.ZERO);
    }

    @Test
    public void plusMillis_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(-1, 2000000);
        assertSame(t.plusMillis(0), t);
    }

    @Test
    public void plusMillis_zeroSingleton() {
        Duration t = Duration.ofSeconds(-1, 2000000);
        assertSame(t.plusMillis(998), Duration.ZERO);
    }

    @Test
    public void plusNanos_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(-1, 2000000);
        assertSame(t.plusNanos(0), t);
    }

    @Test
    public void plusNanos_zeroSingleton() {
        Duration t = Duration.ofSeconds(-1, 2000000);
        assertSame(t.plusNanos(998000000), Duration.ZERO);
    }

    @Test
    public void minus_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(1);
        assertSame(t.minus(Duration.ZERO), t);
    }

    @Test
    public void minus_zeroSingleton() {
        Duration t = Duration.ofSeconds(1);
        assertSame(t.minus(Duration.ofSeconds(1)), Duration.ZERO);
    }

    @Test
    public void minusSeconds_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(1);
        assertSame(t.minusSeconds(0), t);
    }

    @Test
    public void minusSeconds_zeroSingleton() {
        Duration t = Duration.ofSeconds(1);
        assertSame(t.minusSeconds(1), Duration.ZERO);
    }

    @Test
    public void minusMillis_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(1, 2000000);
        assertSame(t.minusMillis(0), t);
    }

    @Test
    public void minusMillis_zeroSingleton() {
        Duration t = Duration.ofSeconds(1, 2000000);
        assertSame(t.minusMillis(1002), Duration.ZERO);
    }

    @Test
    public void minusNanos_zeroReturnsThis() {
        Duration t = Duration.ofSeconds(1, 2000000);
        assertSame(t.minusNanos(0), t);
    }

    @Test
    public void minusNanos_zeroSingleton() {
        Duration t = Duration.ofSeconds(1, 2000000);
        assertSame(t.minusNanos(1002000000), Duration.ZERO);
    }

    @Test
    public void test_abs_same() {
        Duration base = Duration.ofSeconds(12);
        assertSame(base.abs(), base);
    }

    void doTest_comparisons_Duration(Duration... durations) {
        for (int i = 0; i < durations.length; i++) {
            Duration a = durations[i];
            for (int j = 0; j < durations.length; j++) {
                Duration b = durations[j];
                if (i < j) {
                    assertEquals(a.compareTo(b)< 0, true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertEquals(a.compareTo(b) > 0, true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

}
