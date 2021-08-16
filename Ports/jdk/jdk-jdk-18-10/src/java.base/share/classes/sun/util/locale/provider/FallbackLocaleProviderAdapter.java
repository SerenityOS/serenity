/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * FallbackProviderAdapter implementation.
 *
 * @author Naoto Sato
 */
public class FallbackLocaleProviderAdapter extends JRELocaleProviderAdapter {

    /**
     * Supported language tag set.
     */
    private static final Set<String> rootTagSet =
        Collections.singleton(Locale.ROOT.toLanguageTag());

    /**
     * Fallback provider only provides the ROOT locale data.
     */
    private final LocaleResources rootLocaleResources =
        new LocaleResources(this, Locale.ROOT);

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return Type.FALLBACK;
    }

    @Override
    public LocaleResources getLocaleResources(Locale locale) {
        return rootLocaleResources;
    }

    @Override
    protected Set<String> createLanguageTagSet(String category) {
        return rootTagSet;
    }

    @Override
    public boolean isSupportedProviderLocale(Locale locale, Set<String>langtags) {
        return Locale.ROOT.equals(locale);
    }
}
