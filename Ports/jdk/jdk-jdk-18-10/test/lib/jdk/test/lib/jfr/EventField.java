/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedObject;
import jdk.test.lib.Asserts;


public final class EventField {
    public final RecordedObject event;
    public final ValueDescriptor desc;

    public EventField(RecordedObject event, ValueDescriptor valueDescriptor) {
        this.event = event;
        this.desc = valueDescriptor;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> boolean isEqual(T value) {
        return value == (T)getValue();
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField equal(T value) {
        doAssert(()-> Asserts.assertEquals((T)getValue(), value, getErrMsg("Value not equal to " + value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField notEqual(T value) {
        doAssert(()-> Asserts.assertNotEquals((T)getValue(), value, getErrMsg("Value equal to " + value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField above(T value) {
        doAssert(()-> Asserts.assertGreaterThan((T)getValue(), value, getErrMsg("Value not above " + value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField below(T value) {
        doAssert(()-> Asserts.assertLessThan((T)getValue(), value, getErrMsg("Value not below " + value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField atLeast(T value) {
        doAssert(()-> Asserts.assertGreaterThanOrEqual((T)getValue(), value, getErrMsg("Value not atLeast" + value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EventField atMost(T value) {
        doAssert(()-> Asserts.assertLessThanOrEqual((T)getValue(), value, getErrMsg("Value not atMost " + value)));
        return this;
    }

    public <T extends Comparable<T>> EventField instring(String part) {
        final String value = getValue();
        doAssert(()-> Asserts.assertTrue(value.contains(part), getErrMsg("Value does not contain '" + part +"'")));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T)event.getValue(desc.getName());
    }

    public EventField notNull() {
        doAssert(()-> Asserts.assertNotNull(getValue(), getErrMsg("Field is null")));
        return this;
    }

    public EventField isNull() {
        doAssert(()-> Asserts.assertNull(getValue(), getErrMsg("Field is not null")));
        return this;
    }

    public EventField notEmpty() {
        notNull();
        final String s = getValue();
        doAssert(()-> Asserts.assertFalse(s.isEmpty(), getErrMsg("Field is empty")));
        return this;
    }

    private void doAssert(AssertFunction f) {
        try {
            f.doAssert();
        } catch (RuntimeException e) {
            System.out.printf("Error: %s%nFailed event:%n%s%n", e.getMessage(), event.toString());
            throw e;
        }
    }

    public EventField containsAny(String... allowed) {
        final String value = getValue();
        final List<String> allowedValues = Arrays.asList(allowed);
        boolean contains = false;
        for(String allowedValue : allowed) {
            if (value.contains(allowedValue))  {
                contains = true;
            }
        }
        if (!contains) {
            doAssert(()-> Asserts.fail(getErrMsg(String.format("Value not in (%s)",
                allowedValues.stream().collect(Collectors.joining(", "))))));
        }
        return this;
    }

    private String getErrMsg(String msg) {
        final String name = desc.getName();
        final Object value = event.getValue(name);
        return String.format("%s, field='%s', value='%s'", msg, name, value);
    }

    @FunctionalInterface
    public interface AssertFunction {
        void doAssert();
    }
}
