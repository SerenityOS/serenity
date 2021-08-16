/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A CompositeData for StackTraceElement for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class StackTraceElementCompositeData extends LazyCompositeData {
    private final StackTraceElement ste;

    private StackTraceElementCompositeData(StackTraceElement ste) {
        this.ste = ste;
    }

    public StackTraceElement getStackTraceElement() {
        return ste;
    }

    public static StackTraceElement from(CompositeData cd) {
        validateCompositeData(cd);

        if (STACK_TRACE_ELEMENT_COMPOSITE_TYPE.equals(cd.getCompositeType())) {
            return new StackTraceElement(getString(cd, CLASS_LOADER_NAME),
                                         getString(cd, MODULE_NAME),
                                         getString(cd, MODULE_VERSION),
                                         getString(cd, CLASS_NAME),
                                         getString(cd, METHOD_NAME),
                                         getString(cd, FILE_NAME),
                                         getInt(cd, LINE_NUMBER));
        } else {
            return new StackTraceElement(getString(cd, CLASS_NAME),
                                         getString(cd, METHOD_NAME),
                                         getString(cd, FILE_NAME),
                                         getInt(cd, LINE_NUMBER));

        }
    }

    public static CompositeData toCompositeData(StackTraceElement ste) {
        StackTraceElementCompositeData cd = new StackTraceElementCompositeData(ste);
        return cd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // values may be null; so can't use Map.of
        Map<String,Object> items = new HashMap<>();
        items.put(CLASS_LOADER_NAME, ste.getClassLoaderName());
        items.put(MODULE_NAME,       ste.getModuleName());
        items.put(MODULE_VERSION,    ste.getModuleVersion());
        items.put(CLASS_NAME,        ste.getClassName());
        items.put(METHOD_NAME,       ste.getMethodName());
        items.put(FILE_NAME,         ste.getFileName());
        items.put(LINE_NUMBER,       ste.getLineNumber());
        items.put(NATIVE_METHOD,     ste.isNativeMethod());

        try {
            return new CompositeDataSupport(STACK_TRACE_ELEMENT_COMPOSITE_TYPE, items);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    // Attribute names
    private static final String CLASS_LOADER_NAME = "classLoaderName";
    private static final String MODULE_NAME       = "moduleName";
    private static final String MODULE_VERSION    = "moduleVersion";
    private static final String CLASS_NAME        = "className";
    private static final String METHOD_NAME       = "methodName";
    private static final String FILE_NAME         = "fileName";
    private static final String LINE_NUMBER       = "lineNumber";
    private static final String NATIVE_METHOD     = "nativeMethod";

    private static final String[] V5_ATTRIBUTES = {
        CLASS_NAME,
        METHOD_NAME,
        FILE_NAME,
        LINE_NUMBER,
        NATIVE_METHOD,
    };

    private static final String[] V9_ATTRIBUTES = {
        CLASS_LOADER_NAME,
        MODULE_NAME,
        MODULE_VERSION,
    };

    private static final CompositeType STACK_TRACE_ELEMENT_COMPOSITE_TYPE;
    private static final CompositeType V5_COMPOSITE_TYPE;
    static {
        try {
            STACK_TRACE_ELEMENT_COMPOSITE_TYPE = (CompositeType)
                MappedMXBeanType.toOpenType(StackTraceElement.class);

            OpenType<?>[] types = new OpenType<?>[V5_ATTRIBUTES.length];
            for (int i=0; i < V5_ATTRIBUTES.length; i++) {
                String name = V5_ATTRIBUTES[i];
                types[i] = STACK_TRACE_ELEMENT_COMPOSITE_TYPE.getType(name);
            }
            V5_COMPOSITE_TYPE = new CompositeType("StackTraceElement",
                                                  "JDK 5 StackTraceElement",
                                                  V5_ATTRIBUTES,
                                                  V5_ATTRIBUTES,
                                                  types);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    static CompositeType v5CompositeType() {
        return V5_COMPOSITE_TYPE;
    }

    /**
     * Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        CompositeType ct = cd.getCompositeType();
        if (!isTypeMatched(STACK_TRACE_ELEMENT_COMPOSITE_TYPE, ct) &&
            !isTypeMatched(V5_COMPOSITE_TYPE, ct)) {
            throw new IllegalArgumentException(
                "Unexpected composite type for StackTraceElement");
        }
    }
    private static final long serialVersionUID = -2704607706598396827L;
}
