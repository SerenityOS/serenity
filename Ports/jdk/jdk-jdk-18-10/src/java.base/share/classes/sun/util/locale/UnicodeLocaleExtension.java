
/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package sun.util.locale;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringJoiner;

public class UnicodeLocaleExtension extends Extension {
    public static final char SINGLETON = 'u';

    private final Set<String> attributes;
    private final Map<String, String> keywords;

    public static final UnicodeLocaleExtension CA_JAPANESE
        = new UnicodeLocaleExtension("ca", "japanese");
    public static final UnicodeLocaleExtension NU_THAI
        = new UnicodeLocaleExtension("nu", "thai");

    private UnicodeLocaleExtension(String key, String value) {
        super(SINGLETON, key + "-" + value);
        attributes = Collections.emptySet();
        keywords = Collections.singletonMap(key, value);
    }

    UnicodeLocaleExtension(SortedSet<String> attributes, SortedMap<String, String> keywords) {
        super(SINGLETON);
        if (attributes != null) {
            this.attributes = attributes;
        } else {
            this.attributes = Collections.emptySet();
        }
        if (keywords != null) {
            this.keywords = keywords;
        } else {
            this.keywords = Collections.emptyMap();
        }

        if (!this.attributes.isEmpty() || !this.keywords.isEmpty()) {
            StringJoiner sj = new StringJoiner(LanguageTag.SEP);
            for (String attribute : this.attributes) {
                sj.add(attribute);
            }
            for (Entry<String, String> keyword : this.keywords.entrySet()) {
                String key = keyword.getKey();
                String value = keyword.getValue();

                sj.add(key);
                if (!value.isEmpty()) {
                    sj.add(value);
                }
            }
            setValue(sj.toString());
        }
    }

    public Set<String> getUnicodeLocaleAttributes() {
        if (attributes == Collections.EMPTY_SET) {
            return attributes;
        }
        return Collections.unmodifiableSet(attributes);
    }

    public Set<String> getUnicodeLocaleKeys() {
        if (keywords == Collections.EMPTY_MAP) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(keywords.keySet());
    }

    public String getUnicodeLocaleType(String unicodeLocaleKey) {
        return keywords.get(unicodeLocaleKey);
    }

    public static boolean isSingletonChar(char c) {
        return (SINGLETON == LocaleUtils.toLower(c));
    }

    public static boolean isAttribute(String s) {
        // 3*8alphanum
        int len = s.length();
        return (len >= 3) && (len <= 8) && LocaleUtils.isAlphaNumericString(s);
    }

    public static boolean isKey(String s) {
        // 2alphanum
        return (s.length() == 2) && LocaleUtils.isAlphaNumericString(s);
    }

    public static boolean isTypeSubtag(String s) {
        // 3*8alphanum
        int len = s.length();
        return (len >= 3) && (len <= 8) && LocaleUtils.isAlphaNumericString(s);
    }
}
