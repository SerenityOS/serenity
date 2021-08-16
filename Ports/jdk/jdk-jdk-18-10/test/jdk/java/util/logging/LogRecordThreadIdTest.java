/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8245302
 * @summary test the relationship between
 * thread id long and int methods
 * @build LogRecordThreadIdTest
 * @run testng/othervm  LogRecordThreadIdTest
 */

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;



public class LogRecordThreadIdTest {

    LogRecord record, record1, record2;

    @BeforeTest
    public void setUp() throws Exception {
        record  = new LogRecord(Level.INFO, "record");
        record1 = new LogRecord(Level.INFO, "record1");
        record2 = new LogRecord(Level.INFO, "record2");
    }

    /**
     * Tests threadID setter methods for consistency
     * with longThreadID
     */
    @Test
    public void testSetThreadId() {
        record.setThreadID(Integer.MAX_VALUE - 20);
        record1.setThreadID(Integer.MAX_VALUE - 1);
        assertEquals(record.getLongThreadID(), Integer.MAX_VALUE - 20L);
        assertEquals(record.getThreadID(), Integer.MAX_VALUE - 20);
        assertEquals(record1.getThreadID(), Integer.MAX_VALUE - 1);
        assertEquals(record1.getLongThreadID(), Integer.MAX_VALUE - 1);
    }

    /**
     * Tests longThreadID methods for consistency
     * with threadID
     */
    @Test
    public void testSetLongThreadId() {
      record.setLongThreadID(Integer.MAX_VALUE - 20L);
      record1.setLongThreadID(Integer.MAX_VALUE + 10L);
      record2.setLongThreadID(Integer.MAX_VALUE);
      assertEquals(record.getThreadID(), Integer.MAX_VALUE - 20);
      assertEquals(record.getLongThreadID(), Integer.MAX_VALUE - 20L);
      assertNotEquals(record1.getThreadID(), Integer.MAX_VALUE + 10L);
      assertEquals(record1.getLongThreadID(), Integer.MAX_VALUE + 10L);
      assertEquals(record2.getThreadID(), Integer.MAX_VALUE);
      assertEquals(record2.getLongThreadID(), Integer.MAX_VALUE);

    }
}
