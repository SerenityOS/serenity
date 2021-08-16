/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PlatformVersionTest {

    public PlatformVersionTest(Function<String, DottedVersion> parser,
            String version, boolean valid) {
        this.parser = parser;
        this.version = version;
        this.valid = valid;
    }

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        addTo(data, WIN_MSI_PRODUCT_VERSION_PARSER, true,
            "0.0",
            "255.255",
            "0.0.0",
            "255.255.65535",
            "0.0.0.0",
            "255.255.65535.999999"
        );

        addTo(data, WIN_MSI_PRODUCT_VERSION_PARSER, false,
            "0",
            "256.01",
            "255.256",
            "255.255.65536",
            "1.2.3.4.5"
        );

        addTo(data, MAC_CFBUNDLE_VERSION_PARSER, true,
            "1",
            "1.2",
            "1.2.3"
        );

        addTo(data, MAC_CFBUNDLE_VERSION_PARSER, false,
            "0",
            "0.1",
            "1.2.3.4"
        );

        return data;
    }

    private static void addTo(List<Object[]> data,
            Function<String, DottedVersion> parser, boolean valid,
            String... values) {
        if (parser != null) {
            data.addAll(Stream.of(values).map(version -> new Object[]{parser,
                version, valid}).collect(Collectors.toList()));
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testIt() {
        if (valid) {
            assertEquals(parser.apply(version).toString(), version);
        } else {
            exceptionRule.expect(IllegalArgumentException.class);
            parser.apply(version);
        }
    }

    private final Function<String, DottedVersion> parser;
    private final String version;
    private final boolean valid;

    private final static Function<String, DottedVersion> MAC_CFBUNDLE_VERSION_PARSER = findParser(
            "jdk.jpackage.internal.CFBundleVersion");
    private final static Function<String, DottedVersion> WIN_MSI_PRODUCT_VERSION_PARSER = findParser(
            "jdk.jpackage.internal.MsiVersion");

    private static Function<String, DottedVersion> findParser(String className) {
        try {
            Method method = Class.forName(className).getDeclaredMethod("of",
                    String.class);
            return (str) -> {
                try {
                    return (DottedVersion) method.invoke(null, str);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    Throwable causeEx = ex.getCause();
                    if (causeEx instanceof RuntimeException) {
                        throw (RuntimeException)causeEx;
                    }
                    throw new RuntimeException(causeEx);
                }
            };
        } catch (ClassNotFoundException e) {
            return null;
        } catch (SecurityException | NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
