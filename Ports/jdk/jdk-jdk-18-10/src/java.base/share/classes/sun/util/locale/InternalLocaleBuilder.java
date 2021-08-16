/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InternalLocaleBuilder {

    private static final CaseInsensitiveChar PRIVATEUSE_KEY
        = new CaseInsensitiveChar(LanguageTag.PRIVATEUSE);

    private String language = "";
    private String script = "";
    private String region = "";
    private String variant = "";

    private Map<CaseInsensitiveChar, String> extensions;
    private Set<CaseInsensitiveString> uattributes;
    private Map<CaseInsensitiveString, String> ukeywords;


    public InternalLocaleBuilder() {
    }

    public InternalLocaleBuilder setLanguage(String language) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(language)) {
            this.language = "";
        } else {
            if (!LanguageTag.isLanguage(language)) {
                throw new LocaleSyntaxException("Ill-formed language: " + language, 0);
            }
            this.language = language;
        }
        return this;
    }

    public InternalLocaleBuilder setScript(String script) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(script)) {
            this.script = "";
        } else {
            if (!LanguageTag.isScript(script)) {
                throw new LocaleSyntaxException("Ill-formed script: " + script, 0);
            }
            this.script = script;
        }
        return this;
    }

    public InternalLocaleBuilder setRegion(String region) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(region)) {
            this.region = "";
        } else {
            if (!LanguageTag.isRegion(region)) {
                throw new LocaleSyntaxException("Ill-formed region: " + region, 0);
            }
            this.region = region;
        }
        return this;
    }

    public InternalLocaleBuilder setVariant(String variant) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(variant)) {
            this.variant = "";
        } else {
            // normalize separators to "_"
            String var = variant.replaceAll(LanguageTag.SEP, BaseLocale.SEP);
            int errIdx = checkVariants(var, BaseLocale.SEP);
            if (errIdx != -1) {
                throw new LocaleSyntaxException("Ill-formed variant: " + variant, errIdx);
            }
            this.variant = var;
        }
        return this;
    }

    public InternalLocaleBuilder addUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
        if (!UnicodeLocaleExtension.isAttribute(attribute)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale attribute: " + attribute);
        }
        // Use case insensitive string to prevent duplication
        if (uattributes == null) {
            uattributes = new HashSet<>(4);
        }
        uattributes.add(new CaseInsensitiveString(attribute));
        return this;
    }

    public InternalLocaleBuilder removeUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
        if (attribute == null || !UnicodeLocaleExtension.isAttribute(attribute)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale attribute: " + attribute);
        }
        if (uattributes != null) {
            uattributes.remove(new CaseInsensitiveString(attribute));
        }
        return this;
    }

    public InternalLocaleBuilder setUnicodeLocaleKeyword(String key, String type) throws LocaleSyntaxException {
        if (!UnicodeLocaleExtension.isKey(key)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale keyword key: " + key);
        }

        CaseInsensitiveString cikey = new CaseInsensitiveString(key);
        if (type == null) {
            if (ukeywords != null) {
                // null type is used for remove the key
                ukeywords.remove(cikey);
            }
        } else {
            if (type.length() != 0) {
                // normalize separator to "-"
                String tp = type.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
                // validate
                StringTokenIterator itr = new StringTokenIterator(tp, LanguageTag.SEP);
                while (!itr.isDone()) {
                    String s = itr.current();
                    if (!UnicodeLocaleExtension.isTypeSubtag(s)) {
                        throw new LocaleSyntaxException("Ill-formed Unicode locale keyword type: "
                                                        + type,
                                                        itr.currentStart());
                    }
                    itr.next();
                }
            }
            if (ukeywords == null) {
                ukeywords = new HashMap<>(4);
            }
            ukeywords.put(cikey, type);
        }
        return this;
    }

    public InternalLocaleBuilder setExtension(char singleton, String value) throws LocaleSyntaxException {
        // validate key
        boolean isBcpPrivateuse = LanguageTag.isPrivateusePrefixChar(singleton);
        if (!isBcpPrivateuse && !LanguageTag.isExtensionSingletonChar(singleton)) {
            throw new LocaleSyntaxException("Ill-formed extension key: " + singleton);
        }

        boolean remove = LocaleUtils.isEmpty(value);
        CaseInsensitiveChar key = new CaseInsensitiveChar(singleton);

        if (remove) {
            if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                // clear entire Unicode locale extension
                if (uattributes != null) {
                    uattributes.clear();
                }
                if (ukeywords != null) {
                    ukeywords.clear();
                }
            } else {
                if (extensions != null && extensions.containsKey(key)) {
                    extensions.remove(key);
                }
            }
        } else {
            // validate value
            String val = value.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
            StringTokenIterator itr = new StringTokenIterator(val, LanguageTag.SEP);
            while (!itr.isDone()) {
                String s = itr.current();
                boolean validSubtag;
                if (isBcpPrivateuse) {
                    validSubtag = LanguageTag.isPrivateuseSubtag(s);
                } else {
                    validSubtag = LanguageTag.isExtensionSubtag(s);
                }
                if (!validSubtag) {
                    throw new LocaleSyntaxException("Ill-formed extension value: " + s,
                                                    itr.currentStart());
                }
                itr.next();
            }

            if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                setUnicodeLocaleExtension(val);
            } else {
                if (extensions == null) {
                    extensions = new HashMap<>(4);
                }
                extensions.put(key, val);
            }
        }
        return this;
    }

    /*
     * Set extension/private subtags in a single string representation
     */
    public InternalLocaleBuilder setExtensions(String subtags) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(subtags)) {
            clearExtensions();
            return this;
        }
        subtags = subtags.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
        StringTokenIterator itr = new StringTokenIterator(subtags, LanguageTag.SEP);

        List<String> extensions = null;
        String privateuse = null;

        int parsed = 0;
        int start;

        // Make a list of extension subtags
        while (!itr.isDone()) {
            String s = itr.current();
            if (LanguageTag.isExtensionSingleton(s)) {
                start = itr.currentStart();
                String singleton = s;
                StringBuilder sb = new StringBuilder(singleton);

                itr.next();
                while (!itr.isDone()) {
                    s = itr.current();
                    if (LanguageTag.isExtensionSubtag(s)) {
                        sb.append(LanguageTag.SEP).append(s);
                        parsed = itr.currentEnd();
                    } else {
                        break;
                    }
                    itr.next();
                }

                if (parsed < start) {
                    throw new LocaleSyntaxException("Incomplete extension '" + singleton + "'",
                                                    start);
                }

                if (extensions == null) {
                    extensions = new ArrayList<>(4);
                }
                extensions.add(sb.toString());
            } else {
                break;
            }
        }
        if (!itr.isDone()) {
            String s = itr.current();
            if (LanguageTag.isPrivateusePrefix(s)) {
                start = itr.currentStart();
                StringBuilder sb = new StringBuilder(s);

                itr.next();
                while (!itr.isDone()) {
                    s = itr.current();
                    if (!LanguageTag.isPrivateuseSubtag(s)) {
                        break;
                    }
                    sb.append(LanguageTag.SEP).append(s);
                    parsed = itr.currentEnd();

                    itr.next();
                }
                if (parsed <= start) {
                    throw new LocaleSyntaxException("Incomplete privateuse:"
                                                    + subtags.substring(start),
                                                    start);
                } else {
                    privateuse = sb.toString();
                }
            }
        }

        if (!itr.isDone()) {
            throw new LocaleSyntaxException("Ill-formed extension subtags:"
                                            + subtags.substring(itr.currentStart()),
                                            itr.currentStart());
        }

        return setExtensions(extensions, privateuse);
    }

    /*
     * Set a list of BCP47 extensions and private use subtags
     * BCP47 extensions are already validated and well-formed, but may contain duplicates
     */
    private InternalLocaleBuilder setExtensions(List<String> bcpExtensions, String privateuse) {
        clearExtensions();

        if (!LocaleUtils.isEmpty(bcpExtensions)) {
            Set<CaseInsensitiveChar> done = new HashSet<>(bcpExtensions.size());
            for (String bcpExt : bcpExtensions) {
                CaseInsensitiveChar key = new CaseInsensitiveChar(bcpExt);
                // ignore duplicates
                if (!done.contains(key)) {
                    // each extension string contains singleton, e.g. "a-abc-def"
                    if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                        setUnicodeLocaleExtension(bcpExt.substring(2));
                    } else {
                        if (extensions == null) {
                            extensions = new HashMap<>(4);
                        }
                        extensions.put(key, bcpExt.substring(2));
                    }
                }
                done.add(key);
            }
        }
        if (privateuse != null && !privateuse.isEmpty()) {
            // privateuse string contains prefix, e.g. "x-abc-def"
            if (extensions == null) {
                extensions = new HashMap<>(1);
            }
            extensions.put(new CaseInsensitiveChar(privateuse), privateuse.substring(2));
        }

        return this;
    }

    /*
     * Reset Builder's internal state with the given language tag
     */
    public InternalLocaleBuilder setLanguageTag(LanguageTag langtag) {
        clear();
        if (!langtag.getExtlangs().isEmpty()) {
            language = langtag.getExtlangs().get(0);
        } else {
            String lang = langtag.getLanguage();
            if (!lang.equals(LanguageTag.UNDETERMINED)) {
                language = lang;
            }
        }
        script = langtag.getScript();
        region = langtag.getRegion();

        List<String> bcpVariants = langtag.getVariants();
        if (!bcpVariants.isEmpty()) {
            StringBuilder var = new StringBuilder(bcpVariants.get(0));
            int size = bcpVariants.size();
            for (int i = 1; i < size; i++) {
                var.append(BaseLocale.SEP).append(bcpVariants.get(i));
            }
            variant = var.toString();
        }

        setExtensions(langtag.getExtensions(), langtag.getPrivateuse());

        return this;
    }

    public InternalLocaleBuilder setLocale(BaseLocale base, LocaleExtensions localeExtensions) throws LocaleSyntaxException {
        String language = base.getLanguage();
        String script = base.getScript();
        String region = base.getRegion();
        String variant = base.getVariant();

        // Special backward compatibility support

        // Exception 1 - ja_JP_JP
        if (language.equals("ja") && region.equals("JP") && variant.equals("JP")) {
            // When locale ja_JP_JP is created, ca-japanese is always there.
            // The builder ignores the variant "JP"
            assert("japanese".equals(localeExtensions.getUnicodeLocaleType("ca")));
            variant = "";
        }
        // Exception 2 - th_TH_TH
        else if (language.equals("th") && region.equals("TH") && variant.equals("TH")) {
            // When locale th_TH_TH is created, nu-thai is always there.
            // The builder ignores the variant "TH"
            assert("thai".equals(localeExtensions.getUnicodeLocaleType("nu")));
            variant = "";
        }
        // Exception 3 - no_NO_NY
        else if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            // no_NO_NY is a valid locale and used by Java 6 or older versions.
            // The build ignores the variant "NY" and change the language to "nn".
            language = "nn";
            variant = "";
        }

        // Validate base locale fields before updating internal state.
        // LocaleExtensions always store validated/canonicalized values,
        // so no checks are necessary.
        if (!language.isEmpty() && !LanguageTag.isLanguage(language)) {
            throw new LocaleSyntaxException("Ill-formed language: " + language);
        }

        if (!script.isEmpty() && !LanguageTag.isScript(script)) {
            throw new LocaleSyntaxException("Ill-formed script: " + script);
        }

        if (!region.isEmpty() && !LanguageTag.isRegion(region)) {
            throw new LocaleSyntaxException("Ill-formed region: " + region);
        }

        if (!variant.isEmpty()) {
            int errIdx = checkVariants(variant, BaseLocale.SEP);
            if (errIdx != -1) {
                throw new LocaleSyntaxException("Ill-formed variant: " + variant, errIdx);
            }
        }

        // The input locale is validated at this point.
        // Now, updating builder's internal fields.
        this.language = language;
        this.script = script;
        this.region = region;
        this.variant = variant;
        clearExtensions();

        Set<Character> extKeys = (localeExtensions == null) ? null : localeExtensions.getKeys();
        if (extKeys != null) {
            // map localeExtensions back to builder's internal format
            for (Character key : extKeys) {
                Extension e = localeExtensions.getExtension(key);
                if (e instanceof UnicodeLocaleExtension) {
                    UnicodeLocaleExtension ue = (UnicodeLocaleExtension)e;
                    for (String uatr : ue.getUnicodeLocaleAttributes()) {
                        if (uattributes == null) {
                            uattributes = new HashSet<>(4);
                        }
                        uattributes.add(new CaseInsensitiveString(uatr));
                    }
                    for (String ukey : ue.getUnicodeLocaleKeys()) {
                        if (ukeywords == null) {
                            ukeywords = new HashMap<>(4);
                        }
                        ukeywords.put(new CaseInsensitiveString(ukey), ue.getUnicodeLocaleType(ukey));
                    }
                } else {
                    if (extensions == null) {
                        extensions = new HashMap<>(4);
                    }
                    extensions.put(new CaseInsensitiveChar(key), e.getValue());
                }
            }
        }
        return this;
    }

    public InternalLocaleBuilder clear() {
        language = "";
        script = "";
        region = "";
        variant = "";
        clearExtensions();
        return this;
    }

    public InternalLocaleBuilder clearExtensions() {
        if (extensions != null) {
            extensions.clear();
        }
        if (uattributes != null) {
            uattributes.clear();
        }
        if (ukeywords != null) {
            ukeywords.clear();
        }
        return this;
    }

    public BaseLocale getBaseLocale() {
        String language = this.language;
        String script = this.script;
        String region = this.region;
        String variant = this.variant;

        // Special private use subtag sequence identified by "lvariant" will be
        // interpreted as Java variant.
        if (extensions != null) {
            String privuse = extensions.get(PRIVATEUSE_KEY);
            if (privuse != null) {
                StringTokenIterator itr = new StringTokenIterator(privuse, LanguageTag.SEP);
                boolean sawPrefix = false;
                int privVarStart = -1;
                while (!itr.isDone()) {
                    if (sawPrefix) {
                        privVarStart = itr.currentStart();
                        break;
                    }
                    if (LocaleUtils.caseIgnoreMatch(itr.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                        sawPrefix = true;
                    }
                    itr.next();
                }
                if (privVarStart != -1) {
                    StringBuilder sb = new StringBuilder(variant);
                    if (sb.length() != 0) {
                        sb.append(BaseLocale.SEP);
                    }
                    sb.append(privuse.substring(privVarStart).replaceAll(LanguageTag.SEP,
                                                                         BaseLocale.SEP));
                    variant = sb.toString();
                }
            }
        }

        return BaseLocale.getInstance(language, script, region, variant);
    }

    public LocaleExtensions getLocaleExtensions() {
        if (LocaleUtils.isEmpty(extensions) && LocaleUtils.isEmpty(uattributes)
            && LocaleUtils.isEmpty(ukeywords)) {
            return null;
        }

        LocaleExtensions lext = new LocaleExtensions(extensions, uattributes, ukeywords);
        return lext.isEmpty() ? null : lext;
    }

    /*
     * Remove special private use subtag sequence identified by "lvariant"
     * and return the rest. Only used by LocaleExtensions
     */
    static String removePrivateuseVariant(String privuseVal) {
        StringTokenIterator itr = new StringTokenIterator(privuseVal, LanguageTag.SEP);

        // Note: privateuse value "abc-lvariant" is unchanged
        // because no subtags after "lvariant".

        int prefixStart = -1;
        boolean sawPrivuseVar = false;
        while (!itr.isDone()) {
            if (prefixStart != -1) {
                // Note: privateuse value "abc-lvariant" is unchanged
                // because no subtags after "lvariant".
                sawPrivuseVar = true;
                break;
            }
            if (LocaleUtils.caseIgnoreMatch(itr.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                prefixStart = itr.currentStart();
            }
            itr.next();
        }
        if (!sawPrivuseVar) {
            return privuseVal;
        }

        assert(prefixStart == 0 || prefixStart > 1);
        return (prefixStart == 0) ? null : privuseVal.substring(0, prefixStart -1);
    }

    /*
     * Check if the given variant subtags separated by the given
     * separator(s) are valid
     */
    private int checkVariants(String variants, String sep) {
        StringTokenIterator itr = new StringTokenIterator(variants, sep);
        while (!itr.isDone()) {
            String s = itr.current();
            if (!LanguageTag.isVariant(s)) {
                return itr.currentStart();
            }
            itr.next();
        }
        return -1;
    }

    /*
     * Private methods parsing Unicode Locale Extension subtags.
     * Duplicated attributes/keywords will be ignored.
     * The input must be a valid extension subtags (excluding singleton).
     */
    private void setUnicodeLocaleExtension(String subtags) {
        // wipe out existing attributes/keywords
        if (uattributes != null) {
            uattributes.clear();
        }
        if (ukeywords != null) {
            ukeywords.clear();
        }

        StringTokenIterator itr = new StringTokenIterator(subtags, LanguageTag.SEP);

        // parse attributes
        while (!itr.isDone()) {
            if (!UnicodeLocaleExtension.isAttribute(itr.current())) {
                break;
            }
            if (uattributes == null) {
                uattributes = new HashSet<>(4);
            }
            uattributes.add(new CaseInsensitiveString(itr.current()));
            itr.next();
        }

        // parse keywords
        CaseInsensitiveString key = null;
        String type;
        int typeStart = -1;
        int typeEnd = -1;
        while (!itr.isDone()) {
            if (key != null) {
                if (UnicodeLocaleExtension.isKey(itr.current())) {
                    // next keyword - emit previous one
                    assert(typeStart == -1 || typeEnd != -1);
                    type = (typeStart == -1) ? "" : subtags.substring(typeStart, typeEnd);
                    if (ukeywords == null) {
                        ukeywords = new HashMap<>(4);
                    }
                    ukeywords.put(key, type);

                    // reset keyword info
                    CaseInsensitiveString tmpKey = new CaseInsensitiveString(itr.current());
                    key = ukeywords.containsKey(tmpKey) ? null : tmpKey;
                    typeStart = typeEnd = -1;
                } else {
                    if (typeStart == -1) {
                        typeStart = itr.currentStart();
                    }
                    typeEnd = itr.currentEnd();
                }
            } else if (UnicodeLocaleExtension.isKey(itr.current())) {
                // 1. first keyword or
                // 2. next keyword, but previous one was duplicate
                key = new CaseInsensitiveString(itr.current());
                if (ukeywords != null && ukeywords.containsKey(key)) {
                    // duplicate
                    key = null;
                }
            }

            if (!itr.hasNext()) {
                if (key != null) {
                    // last keyword
                    assert(typeStart == -1 || typeEnd != -1);
                    type = (typeStart == -1) ? "" : subtags.substring(typeStart, typeEnd);
                    if (ukeywords == null) {
                        ukeywords = new HashMap<>(4);
                    }
                    ukeywords.put(key, type);
                }
                break;
            }

            itr.next();
        }
    }

    static final class CaseInsensitiveString {
        private final String str, lowerStr;

        CaseInsensitiveString(String s) {
            str = s;
            lowerStr = LocaleUtils.toLowerString(s);
        }

        public String value() {
            return str;
        }

        @Override
        public int hashCode() {
            return lowerStr.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CaseInsensitiveString)) {
                return false;
            }
            return lowerStr.equals(((CaseInsensitiveString)obj).lowerStr);
        }
    }

    static final class CaseInsensitiveChar {
        private final char ch, lowerCh;

        /**
         * Constructs a CaseInsensitiveChar with the first char of the
         * given s.
         */
        private CaseInsensitiveChar(String s) {
            this(s.charAt(0));
        }

        CaseInsensitiveChar(char c) {
            ch = c;
            lowerCh = LocaleUtils.toLower(ch);
        }

        public char value() {
            return ch;
        }

        @Override
        public int hashCode() {
            return lowerCh;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CaseInsensitiveChar)) {
                return false;
            }
            return lowerCh == ((CaseInsensitiveChar)obj).lowerCh;
        }
    }
}
