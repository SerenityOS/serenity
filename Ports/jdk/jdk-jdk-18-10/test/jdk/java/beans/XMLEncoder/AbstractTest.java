/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.nio.charset.Charset;

import java.lang.reflect.Field;

abstract class AbstractTest<T> implements ExceptionListener {
    final BeanValidator validator = new BeanValidator();

    public final void exceptionThrown(Exception exception) {
        throw new Error("unexpected exception", exception);
    }

    /**
     * Returns an object to test.
     * This object will be encoded and decoded
     * and the object creation will be tested.
     *
     * @return an object to test
     */
    protected abstract T getObject();

    /**
     * Returns a different object to test.
     * If this object is not {@code null}
     * it will be encoded and decoded
     * and the object updating will be tested.
     *
     * @return a different object to test
     */
    protected T getAnotherObject() {
        return null;
    }

    /**
     * This method should be overridden
     * if specified encoder should be initialized.
     *
     * @param encoder  the XML encoder to initialize
     */
    protected void initialize(XMLEncoder encoder) {
    }

    /**
     * This method should be overridden
     * if specified decoder should be initialized.
     *
     * @param decoder  the XML decoder to initialize
     */
    protected void initialize(XMLDecoder decoder) {
    }

    /**
     * This method should be overridden
     * for test-specific comparison.
     *
     * @param before  the object before encoding
     * @param after   the object after decoding
     */
    protected void validate(T before, T after) {
        this.validator.validate(before, after);
    }

    /**
     * This is entry point to start testing.
     *
     * @param security  use {@code true} to start
     *                  second pass in secure context
     */
    final void test(boolean security) {
        Bean.DEFAULT = null;
        T object = getObject();

        System.out.println("Test object");
        validate(object, testObject(object));

        System.out.println("Test object creating");
        validate(object, testBean(object));

        Bean.DEFAULT = object;
        object = getAnotherObject();
        if (object != null) {
            System.out.println("Test another object");
            validate(object, testObject(object));

            System.out.println("Test object updating");
            validate(object, testBean(object));
        }
        if (security) {
            System.setSecurityManager(new SecurityManager());
            test(false);
        }
    }

    private T testBean(T object) {
        Bean bean = new Bean();
        bean.setValue(object);
        bean = testObject(bean);
        return (T) bean.getValue();
    }

    private <Z> Z testObject(Z object) {
        byte[] array = writeObject(object);
        System.out.print(new String(array, Charset.forName("UTF-8")));
        return (Z) readObject(array);
    }

    private byte[] writeObject(Object object) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(output);
        encoder.setExceptionListener(this);
        initialize(encoder);
        encoder.writeObject(object);
        encoder.close();
        return output.toByteArray();
    }

    private Object readObject(byte[] array) {
        ByteArrayInputStream input = new ByteArrayInputStream(array);
        XMLDecoder decoder = new XMLDecoder(input);
        decoder.setExceptionListener(this);
        initialize(decoder);
        Object object = decoder.readObject();
        decoder.close();
        return object;
    }

    static Field getField(String name) {
        try {
            int index = name.lastIndexOf('.');
            String className = name.substring(0, index);
            String fieldName = name.substring(1 + index);
            Field field = Class.forName(className).getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        }
        catch (Exception exception) {
            throw new Error(exception);
        }
    }
}
