/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.locale.provider;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ServiceConfigurationError;
import java.util.spi.LocaleServiceProvider;

/**
 * LocaleProviderAdapter implementation for the host locale data.
 * Currently it is only implemented on Windows Vista or later.
 *
 * @author Naoto Sato
 */
public class HostLocaleProviderAdapter extends AuxLocaleProviderAdapter {

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return LocaleProviderAdapter.Type.HOST;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends LocaleServiceProvider> P findInstalledProvider(final Class<P> c) {
        try {
            return (P)Class.forName(
                        "sun.util.locale.provider.HostLocaleProviderAdapterImpl")
                    .getMethod("get" + c.getSimpleName(), (Class<?>[]) null)
                    .invoke(null, (Object[]) null);
        } catch (ClassNotFoundException |
                 NoSuchMethodException ex) {
            // permissible exceptions as platform may not support host adapter
            return null;
        } catch (IllegalAccessException |
                 IllegalArgumentException |
                 InvocationTargetException ex) {
            throw new ServiceConfigurationError(
                "Host locale provider cannot be located.", ex);
        }
    }

    /**
     * Utility to make the decimal format specific to integer, called
     * by the platform dependent adapter implementations.
     *
     * @param df A DecimalFormat object
     * @return The same DecimalFormat object in the argument, modified
     *          to allow integer formatting/parsing only.
     */
    static DecimalFormat makeIntegerFormatter(DecimalFormat df) {
        df.setMaximumFractionDigits(0);
        df.setDecimalSeparatorAlwaysShown(false);
        df.setParseIntegerOnly(true);
        return df;
    }
}
