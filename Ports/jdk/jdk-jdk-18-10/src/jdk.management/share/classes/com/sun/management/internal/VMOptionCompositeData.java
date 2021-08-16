/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import com.sun.management.VMOption;
import com.sun.management.VMOption.Origin;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import sun.management.LazyCompositeData;
import sun.management.MappedMXBeanType;

/**
 * A CompositeData for VMOption for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class VMOptionCompositeData extends LazyCompositeData {
    private final VMOption option;

    private VMOptionCompositeData(VMOption option) {
        this.option = option;
    }

    public VMOption getVMOption() {
        return option;
    }

    public static CompositeData toCompositeData(VMOption option) {
        VMOptionCompositeData vcd = new VMOptionCompositeData(option);
        return vcd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // vmOptionItemNames!
        final Object[] vmOptionItemValues = {
            option.getName(),
            option.getValue(),
            option.isWriteable(),
            option.getOrigin().toString(),
        };

        try {
            return new CompositeDataSupport(vmOptionCompositeType,
                                            vmOptionItemNames,
                                            vmOptionItemValues);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final CompositeType vmOptionCompositeType;
    static {
        try {
            vmOptionCompositeType = (CompositeType)
                MappedMXBeanType.toOpenType(VMOption.class);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    static CompositeType getVMOptionCompositeType() {
        return vmOptionCompositeType;
    }

    private static final String NAME      = "name";
    private static final String VALUE     = "value";
    private static final String WRITEABLE = "writeable";
    private static final String ORIGIN    = "origin";

    private static final String[] vmOptionItemNames = {
        NAME,
        VALUE,
        WRITEABLE,
        ORIGIN,
    };

    public static String getName(CompositeData cd) {
        return getString(cd, NAME);
    }
    public static String getValue(CompositeData cd) {
        return getString(cd, VALUE);
    }
    public static Origin getOrigin(CompositeData cd) {
        String o = getString(cd, ORIGIN);
        return Enum.valueOf(Origin.class, o);
    }
    public static boolean isWriteable(CompositeData cd) {
        return getBoolean(cd, WRITEABLE);
    }

    /** Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched(vmOptionCompositeType, cd.getCompositeType())) {
            throw new IllegalArgumentException(
                "Unexpected composite type for VMOption");
        }
    }

    private static final long serialVersionUID = -2395573975093578470L;
}
