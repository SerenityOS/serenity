/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Properties;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * @test
 * @bug 8066132
 * @author Sergey Bylokhov
 */
public final class GetPropertyNames {

    static BufferedImage defaultProps = new BufferedImage(1, 1, TYPE_INT_ARGB);

    public static void main(final String[] args) {
        // default result is null
        if (defaultProps.getPropertyNames() != null) {
            throw new RuntimeException("PropertyNames should be null");
        }
        // for null properties result is null
        final BufferedImage emptyProps = getBufferedImage(null);
        if (emptyProps.getPropertyNames() != null) {
            throw new RuntimeException("PropertyNames should be null");
        }
        // for empty properties result is null
        final BufferedImage nullProps = getBufferedImage(new Properties());
        if (nullProps.getPropertyNames() != null) {
            throw new RuntimeException("PropertyNames should be null");
        }
        // for non-string keys result is null
        final Properties properties = new Properties();
        properties.put(1, 1);
        properties.put(2, 2);
        properties.put(3, 3);
        final BufferedImage nonStringProps = getBufferedImage(properties);
        if (nonStringProps.getPropertyNames() != null) {
            throw new RuntimeException("PropertyNames should be null");
        }
        // for string keys result is not null
        properties.clear();
        properties.setProperty("1", "1");
        properties.setProperty("2", "2");
        validate(getBufferedImage(properties), 2);
        // for the mix of strings and objects result is not null
        properties.clear();
        properties.put(1, 1);
        properties.put(2, 2);
        properties.put(3, 3);
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        final BufferedImage mixProps = getBufferedImage(properties);
        validate(mixProps, 2);
        if (!"value1".equals(mixProps.getProperty("key1"))
            || !"value2".equals(mixProps.getProperty("key2"))) {
            throw new RuntimeException("Wrong key-value pair");
        }
    }


    private static BufferedImage getBufferedImage(final Properties properties) {
        return new BufferedImage(defaultProps.getColorModel(),
                                 defaultProps.getRaster(),
                                 defaultProps.isAlphaPremultiplied(),
                                 properties);
    }

    private static void validate(final BufferedImage bi, final int expected) {
        final String[] names = bi.getPropertyNames();
        if (names.length != expected) {
            throw new RuntimeException("Wrong number of names");
        }
        for (final String name : names) {
            final Object property = bi.getProperty(name);
            if (property == Image.UndefinedProperty || property == null) {
                throw new RuntimeException("Unexpected property");
            }
        }
    }
}
