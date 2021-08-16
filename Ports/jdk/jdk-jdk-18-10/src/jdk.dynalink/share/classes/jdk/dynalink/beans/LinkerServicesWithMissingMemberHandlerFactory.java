/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;
import jdk.dynalink.SecureLookupSupplier;
import jdk.dynalink.linker.ConversionComparator.Comparison;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

final class LinkerServicesWithMissingMemberHandlerFactory implements LinkerServices {
    final LinkerServices linkerServices;
    final MissingMemberHandlerFactory missingMemberHandlerFactory;

    static LinkerServices get(final LinkerServices linkerServices, final MissingMemberHandlerFactory missingMemberHandlerFactory) {
        if (missingMemberHandlerFactory == null) {
            return linkerServices;
        }
        return new LinkerServicesWithMissingMemberHandlerFactory(linkerServices, missingMemberHandlerFactory);
    }

    private LinkerServicesWithMissingMemberHandlerFactory(final LinkerServices linkerServices, final MissingMemberHandlerFactory missingMemberHandlerFactory) {
        this.linkerServices = linkerServices;
        this.missingMemberHandlerFactory = missingMemberHandlerFactory;
    }

    @Override
    public MethodHandle asType(final MethodHandle handle, final MethodType fromType) {
        return linkerServices.asType(handle, fromType);
    }

    @Override
    public MethodHandle getTypeConverter(final Class<?> sourceType, final Class<?> targetType) {
        return linkerServices.getTypeConverter(sourceType, targetType);
    }

    @Override
    public boolean canConvert(final Class<?> from, final Class<?> to) {
        return linkerServices.canConvert(from, to);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest) throws Exception {
        return linkerServices.getGuardedInvocation(linkRequest);
    }

    @Override
    public Comparison compareConversion(final Class<?> sourceType, final Class<?> targetType1, final Class<?> targetType2) {
        return linkerServices.compareConversion(sourceType, targetType1, targetType2);
    }

    @Override
    public MethodHandle filterInternalObjects(final MethodHandle target) {
        return linkerServices.filterInternalObjects(target);
    }

    @Override
    public <T> T getWithLookup(final Supplier<T> operation, final SecureLookupSupplier lookupSupplier) {
        return linkerServices.getWithLookup(operation, lookupSupplier);
    }
}
