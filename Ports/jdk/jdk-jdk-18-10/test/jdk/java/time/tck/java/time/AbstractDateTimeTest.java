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
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.List;

import org.testng.annotations.Test;
import test.java.time.temporal.MockFieldNoValue;

/**
 * Base test class for {@code Temporal}.
 */
public abstract class AbstractDateTimeTest extends AbstractTCKTest {

    /**
     * Sample {@code Temporal} objects.
     * @return the objects, not null
     */
    protected abstract List<TemporalAccessor> samples();

    /**
     * List of valid supported fields.
     * @return the fields, not null
     */
    protected abstract List<TemporalField> validFields();

    /**
     * List of invalid unsupported fields.
     * @return the fields, not null
     */
    protected abstract List<TemporalField> invalidFields();

    //-----------------------------------------------------------------------
    // isSupported(TemporalField)
    //-----------------------------------------------------------------------
    @Test()
    public void basicTest_isSupported_TemporalField_supported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : validFields()) {
                assertEquals(sample.isSupported(field), true, "Failed on " + sample + " " + field);
            }
        }
    }

    @Test()
    public void basicTest_isSupported_TemporalField_unsupported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : invalidFields()) {
                assertEquals(sample.isSupported(field), false, "Failed on " + sample + " " + field);
            }
        }
    }

    @Test()
    public void basicTest_isSupported_TemporalField_null() {
        for (TemporalAccessor sample : samples()) {
            assertEquals(sample.isSupported(null), false, "Failed on " + sample);
        }
    }

    //-----------------------------------------------------------------------
    // range(TemporalField)
    //-----------------------------------------------------------------------
    @Test()
    public void basicTest_range_TemporalField_supported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : validFields()) {
                sample.range(field);  // no exception
            }
        }
    }

    @Test()
    public void basicTest_range_TemporalField_unsupported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : invalidFields()) {
                try {
                    sample.range(field);
                    fail("Failed on " + sample + " " + field);
                } catch (DateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test()
    public void basicTest_range_TemporalField_null() {
        for (TemporalAccessor sample : samples()) {
            try {
                sample.range(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test()
    public void basicTest_get_TemporalField_supported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : validFields()) {
                if (sample.range(field).isIntValue()) {
                    sample.get(field);  // no exception
                } else {
                    try {
                        sample.get(field);
                        fail("Failed on " + sample + " " + field);
                    } catch (DateTimeException ex) {
                        // expected
                    }
                }
            }
        }
    }

    @Test()
    public void basicTest_get_TemporalField_unsupported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : invalidFields()) {
                try {
                    sample.get(field);
                    fail("Failed on " + sample + " " + field);
                } catch (DateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_get_TemporalField_invalidField() {
        for (TemporalAccessor sample : samples()) {
            sample.get(MockFieldNoValue.INSTANCE);
        }
    }

    @Test()
    public void basicTest_get_TemporalField_null() {
        for (TemporalAccessor sample : samples()) {
            try {
                sample.get(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    // getLong(TemporalField)
    //-----------------------------------------------------------------------
    @Test()
    public void basicTest_getLong_TemporalField_supported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : validFields()) {
                sample.getLong(field);  // no exception
            }
        }
    }

    @Test()
    public void basicTest_getLong_TemporalField_unsupported() {
        for (TemporalAccessor sample : samples()) {
            for (TemporalField field : invalidFields()) {
                try {
                    sample.getLong(field);
                    fail("Failed on " + sample + " " + field);
                } catch (DateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_getLong_TemporalField_invalidField() {
        for (TemporalAccessor sample : samples()) {
            sample.getLong(MockFieldNoValue.INSTANCE);
        }
    }

    @Test()
    public void basicTest_getLong_TemporalField_null() {
        for (TemporalAccessor sample : samples()) {
            try {
                sample.getLong(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void basicTest_query() {
        for (TemporalAccessor sample : samples()) {
            assertEquals(sample.query(new TemporalQuery<String>() {
                @Override
                public String queryFrom(TemporalAccessor temporal) {
                    return "foo";
                }
            }), "foo");
        }
    }

}
