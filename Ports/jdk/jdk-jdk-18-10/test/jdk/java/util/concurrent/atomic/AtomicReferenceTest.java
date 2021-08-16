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

import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/*
 * @test
 * @summary Test Map default methods
 * @run testng AtomicReferenceTest
 * @author Jim Gish <jim.gish@oracle.com>
 */
public class AtomicReferenceTest {

    /**
     * Test of updateAndGet method, of class AtomicReference.
     */
    @Test
    public void testUpdateAndGet() {
        AtomicReference<Integer> instance = new AtomicReference<>(3);
        assertEquals((int) instance.get(), 3);
        assertEquals((int) instance.updateAndGet(x -> x + 2), 5);
        assertEquals((int) instance.get(), 5);
    }

    /**
     * Test of getAndUpdate method, of class AtomicReference.
     */
    @Test
    public void testGetAndUpdate() {
        AtomicReference<Integer> instance = new AtomicReference<>(3);
        assertEquals((int) instance.get(), 3);
        assertEquals((int) instance.getAndUpdate(x -> x + 3), 3);
        assertEquals((int) instance.get(), 6);
    }
}
