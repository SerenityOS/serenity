/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.Set;
import sun.text.spi.JavaTimeDateTimePatternProvider;

/**
 * Concrete implementation of the {@link sun.text.spi.JavaTimeDateTimePatternProvider
 * } class for the JRE LocaleProviderAdapter.
 *
 */
public class JavaTimeDateTimePatternImpl extends JavaTimeDateTimePatternProvider implements AvailableLanguageTags {

    private final LocaleProviderAdapter.Type type;
    private final Set<String> langtags;

    public JavaTimeDateTimePatternImpl(LocaleProviderAdapter.Type type, Set<String> langtags) {
        this.type = type;
        this.langtags = langtags;
    }

    /**
     * Returns an array of all locales for which this locale service provider
     * can provide localized objects or names.
     *
     * @return An array of all locales for which this locale service provider
     * can provide localized objects or names.
     */
    @Override
    public Locale[] getAvailableLocales() {
        return LocaleProviderAdapter.toLocaleArray(langtags);
    }

    @Override
    public boolean isSupportedLocale(Locale locale) {
        return LocaleProviderAdapter.forType(type).isSupportedProviderLocale(locale, langtags);
    }

    @Override
    public String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType, Locale locale) {
        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(locale);
        String pattern = lr.getJavaTimeDateTimePattern(
                timeStyle, dateStyle, calType);
        return pattern;

    }

    @Override
    public Set<String> getAvailableLanguageTags() {
        return langtags;
    }
}
