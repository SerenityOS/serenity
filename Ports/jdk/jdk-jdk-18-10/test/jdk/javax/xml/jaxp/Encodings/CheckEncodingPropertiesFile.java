/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8008738 8065138
 * @summary checks that the mapping implemented by
 *      com.sun.org.apache.xml.internal.serializer.Encodings
 *      correctly identifies valid Charset names and
 *      correctly maps them to their preferred mime names.
 *      Also checks that the Encodings.properties resource file
 *      is consistent.
 * @modules java.xml/com.sun.org.apache.xml.internal.serializer:+open
 * @compile -XDignore.symbol.file CheckEncodingPropertiesFile.java
 * @run main CheckEncodingPropertiesFile
 * @author Daniel Fuchs
 */

import com.sun.org.apache.xml.internal.serializer.EncodingInfo;
import com.sun.org.apache.xml.internal.serializer.Encodings;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class CheckEncodingPropertiesFile {

    private static final String ENCODINGS_FILE = "com/sun/org/apache/xml/internal/serializer/Encodings.properties";

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        Module xmlModule = EncodingInfo.class.getModule();
        try (InputStreamReader is = new InputStreamReader(xmlModule.getResourceAsStream(ENCODINGS_FILE))) {
            props.load(is);
        }

       if (!props.containsKey("UTF8")) {
           // If the test fails here - it may indicate that you stumbled on an
           // issue similar to that fixed by JDK-8065138.
           // Check that the content of the Encodings.properties included in
           // the tested build image matches the content of the file in the source
           // jaxp tree of the jdk forest.
           throw new RuntimeException("UTF8 key missing in " + ENCODINGS_FILE);
       }

        //printAllCharsets();

        test(props);
    }


    private static final class CheckCharsetMapping {

        /**
         * A map that maps Java or XML name to canonical charset names.
         * key:    upper cased value of Java or XML name.
         * value:  case-sensitive canonical name of charset.
         */
        private final Map<String, String> charsetMap = new HashMap<>();

        private final Map<String, String> preferredMime = new HashMap<>();

        /**
         * Unresolved alias names.
         * For a given set of names pointing to the same unresolved charset,
         * this map will contain, for each alias in the set, a mapping
         * with the alias.toUpperValue() as key and the set of known aliases
         * as value.
         */
        private final Map<String, Collection<String>> unresolved = new HashMap<>();

        public final static class ConflictingCharsetError extends Error {
            ConflictingCharsetError(String a, String cs1, String cs2) {
                super("Conflicting charset mapping for '"+a+"': '"+cs1+"' and '"+cs2+"'");
            }
        }

        public final static class MissingValidCharsetNameError extends Error {
            MissingValidCharsetNameError(String name, Collection<String> aliases) {
                super(name+": Line "+aliases+" has no recognized charset alias");
            }
        }

        public final static class ConflictingPreferredMimeNameError extends Error {
            ConflictingPreferredMimeNameError(String a, String cs1, String cs2) {
                super("Conflicting preferred mime name for '"+a+"': '"+cs1+"' and '"+cs2+"'");
            }
        }

        /**
         * For each alias in aliases, attempt to find the canonical
         * charset name.
         * All names in aliases are supposed to point to the same charset.
         * Names in aliases can be java names or XML names, indifferently.
         * @param aliases list of names (aliases) for a given charset.
         * @return The canonical name of the charset, if found, null otherwise.
         */
        private String findCharsetNameFor(String[] aliases) {
            String cs = null;
            String res = null;
            for (String a : aliases) {
                final String k = a.toUpperCase();
                String cachedCs = charsetMap.get(k);
                if (cs == null) {
                    cs = cachedCs;
                }
                if (cachedCs != null && cs != null
                        && !Charset.forName(cachedCs).name().equals(Charset.forName(cs).name())) {
                    throw new ConflictingCharsetError(a,cs,cachedCs);
                }
                try {
                    final String rcs = Charset.forName(a).name();
                    if (cs != null && !Charset.forName(cs).name().equals(rcs)) {
                        throw new ConflictingCharsetError(a,cs,rcs);
                    }
                    if (res == null) {
                        if (a.equals(aliases[0])) {
                            res = a;
                        } else {
                            res = cs;
                        }
                    }
                    cs = rcs;
                    charsetMap.put(k, res == null ? cs : res);
                } catch (Exception x) {
                    continue;
                }
            }
            return res == null ? cs : res;
        }

        /**
         * Register a canonical charset name for a given set of aliases.
         *
         * @param charsetName the canonical charset name.
         * @param aliases a list of aliases for the given charset.
         */
        private void registerCharsetNameFor(String charsetName, String[] aliases) {
            if (charsetName == null) throw new NullPointerException();

            for (String a : aliases) {
                String k = a.toUpperCase();
                String csv = charsetMap.get(k);
                if (csv == null) {
                    charsetMap.put(k, charsetName);
                    csv = charsetName;
                } else if (!csv.equals(charsetName)) {
                    throw new ConflictingCharsetError(a,charsetName,csv);
                }

                final Collection<String> c = unresolved.get(k);
                if (c != null) {
                    for (String aa : c) {
                        k = aa.toUpperCase();
                        String csvv = charsetMap.get(k);
                        if (csvv == null) charsetMap.put(k, csv);
                        unresolved.remove(k);
                    }
                    throw new MissingValidCharsetNameError(charsetName,c);
                }
            }
        }

        /**
         * Register a set of aliases as being unresolved.
         * @param names    the list of names - this should be what is returned by
         *                 nameSet.toArray(new String[nameSet.size()])
         * @param nameSet  the set of unresolved aliases.
         */
        private void registerUnresolvedNamesFor(String[] names, Collection<String> nameSet) {
            // This is not necessarily an error: it could happen that some
            //    charsets are simply not supported on some OS/Arch
            System.err.println("Warning: unresolved charset names: '"+ nameSet
                    + "' This is not necessarily an error "
                    + "- this charset may not be supported on this platform.");
            for (String a : names) {
                final String k = a.toUpperCase();
                final Collection<String> c = unresolved.get(k);
                if (c != null) {
                    //System.out.println("Found: "+a+" -> "+c);
                    //System.out.println("\t merging "+ c + " with " + nameSet);
                    nameSet.addAll(c);
                    for (String aa : c) {
                        unresolved.put(aa.toUpperCase(), nameSet);
                    }
                }
                unresolved.put(k, nameSet);
            }
        }


        /**
         * Add a new charset name mapping
         * @param javaName the (supposedly) java name of the charset.
         * @param xmlNames a list of corresponding XML names for that charset.
         */
        void addMapping(String javaName, Collection<String> xmlNames) {
            final LinkedHashSet<String> aliasNames = new LinkedHashSet<>();
            aliasNames.add(javaName);
            aliasNames.addAll(xmlNames);
            final String[] aliases = aliasNames.toArray(new String[aliasNames.size()]);
            final String cs = findCharsetNameFor(aliases);
            if (cs != null) {
                registerCharsetNameFor(cs, aliases);
                if (xmlNames.size() > 0) {
                    String preferred = xmlNames.iterator().next();
                    String cachedPreferred = preferredMime.get(cs.toUpperCase());
                    if (cachedPreferred != null && !cachedPreferred.equals(preferred)) {
                        throw new ConflictingPreferredMimeNameError(cs, cachedPreferred, preferred);
                    }
                    preferredMime.put(cs.toUpperCase(), preferred);
                }
            } else {
                registerUnresolvedNamesFor(aliases, aliasNames);
            }
        }

        /**
         * Returns the canonical name of the charset for the given Java or XML
         * alias name.
         * @param alias the alias name
         * @return the canonical charset name - or null if unknown.
         */
        public String getCharsetNameFor(String alias) {
            return charsetMap.get(alias.toUpperCase());
        }

    }

    public static void test(Properties props) throws Exception {

        // First, build a mapping from the properties read from the resource
        // file.
        // We're going to check the consistency of the resource file
        // while building this mapping, and throw errors if the file
        // does not meet our assumptions.
        //
        Map<String, Collection<String>> lines = new HashMap<>();
        final CheckCharsetMapping mapping = new CheckCharsetMapping();

        for (String key : props.stringPropertyNames()) {
            Collection<String> values = getValues(props.getProperty(key));
            lines.put(key, values);
            mapping.addMapping(key, values);
        }

        // Then build maps of EncodingInfos, and print along debugging
        // information that should help understand the content of the
        // resource file and the mapping it defines.
        //
        Map<String, EncodingInfo> javaInfos = new HashMap<>(); // Map indexed by java names
        Map<String, EncodingInfo> xmlMap = new HashMap<>();    // Map indexed by XML names
        Map<String, String> preferred =
                new HashMap<>(mapping.preferredMime);          // Java Name -> Preferred Mime Name
        List<EncodingInfo> all = new ArrayList<>();            // unused...
        for (Entry<String, Collection<String>> e : lines.entrySet()) {
            final String charsetName = mapping.getCharsetNameFor(e.getKey());
            if (charsetName == null) {
                System.out.println("!! No charset for: "+e.getKey()+ " "+ e.getValue());
                continue;
            }
            Charset c = Charset.forName(charsetName);
            EncodingInfo info;
            final String k = e.getKey().toUpperCase();
            final String kc = charsetName.toUpperCase();
            StringBuilder sb = new StringBuilder();
            for (String xml : e.getValue()) {
                final String kx = xml.toUpperCase();
                info = xmlMap.get(kx);
                if (info == null) {
                    info = new EncodingInfo(xml, charsetName);
                    System.out.println("** XML: "+xml+" -> "+charsetName);
                    xmlMap.put(kx, info);
                    all.add(info);
                }
                if (!javaInfos.containsKey(k)) {
                    javaInfos.put(k, info);
                    if (!preferred.containsKey(k)) {
                        preferred.put(k, xml);
                    }
                    sb.append("** Java: ").append(k).append(" -> ")
                            .append(xml).append(" (charset: ")
                            .append(charsetName).append(")\n");
                }
                if (!javaInfos.containsKey(kc)) {
                    if (!preferred.containsKey(kc)) {
                        preferred.put(kc, xml);
                    }
                    javaInfos.put(kc, info);
                    sb.append("** Java: ").append(kc).append(" -> ")
                            .append(xml).append(" (charset: ")
                            .append(charsetName).append(")\n");
                }
                if (!javaInfos.containsKey(c.name().toUpperCase())) {
                    if (!preferred.containsKey(c.name().toUpperCase())) {
                        preferred.put(c.name().toUpperCase(), xml);
                    }
                    javaInfos.put(c.name().toUpperCase(), info);
                    sb.append("** Java: ").append(c.name().toUpperCase()).append(" -> ")
                            .append(xml).append(" (charset: ")
                            .append(charsetName).append(")\n");
                }
            }
            if (sb.length() == 0) {
                System.out.println("Nothing new for "+charsetName+": "+e.getKey()+" -> "+e.getValue());
            } else {
                System.out.print(sb);
            }

        }

        // Now we're going to verify that Encodings.java has done its job
        // correctly. We're going to ask Encodings to convert java names to mime
        // names and mime names to java names - and verify that the returned
        // java names do map to recognized charsets.
        //
        // We're also going to verify that Encodings has recorded the preferred
        // mime name correctly.

        Method m = Encodings.class.getDeclaredMethod("getMimeEncoding", String.class);
        m.setAccessible(true);

        Set<String> xNames = new HashSet<>();
        Set<String> jNames = new HashSet<>();
        for (String name: xmlMap.keySet()) {
            final String javaName = checkConvertMime2Java(name);
            checkPreferredMime(m, javaName, preferred);
            jNames.add(javaName);
            xNames.add(name);
        }


        for (String javaName : lines.keySet()) {
            final String javaCharsetName = mapping.getCharsetNameFor(javaName.toUpperCase());
            if (javaCharsetName == null) continue;
            if (!jNames.contains(javaName)) {
                checkPreferredMime(m, javaName, preferred);
                jNames.add(javaName);
            }
            for (String xml : lines.get(javaName)) {
                if (xNames.contains(xml)) continue;
                final String jName = checkConvertMime2Java(xml);
                xNames.add(xml);
                if (jNames.contains(jName)) continue;
                checkPreferredMime(m, jName, preferred);
            }
        }
    }

    private static String checkConvertMime2Java(String xml) {
        final String jName = Encodings.convertMime2JavaEncoding(xml);
        final String jCharsetName;
        try {
            jCharsetName = Charset.forName(jName).name();
        } catch (Exception x) {
            throw new Error("Unrecognized charset returned by Encodings.convertMime2JavaEncoding(\""+xml+"\")", x);
        }
        System.out.println("Encodings.convertMime2JavaEncoding(\""+xml+"\") = \""+jName+"\" ("+jCharsetName+")");
        return jName;
    }

    private static void checkPreferredMime(Method m, String javaName, Map<String,String> preferred)
            throws Exception {
        final String mime = (String) m.invoke(null, javaName);
        final String expected = preferred.get(javaName.toUpperCase());
        if (Arrays.deepEquals(new String[] {mime}, new String[] {expected})) {
            System.out.println("Encodings.getMimeEncoding(\""+javaName+"\") = \""+mime+"\"");
        } else {
            throw new Error("Bad preferred mime type for: '"+javaName+"': expected '"+
                expected+"' but got '"+mime+"'");
        }
    }

    private static Collection<String> getValues(String val) {
        int pos = val.indexOf(' ');
        if (pos < 0) {
            return Collections.singletonList(val);
        }
        //lastPrintable =
        //    Integer.decode(val.substring(pos).trim()).intValue();
        StringTokenizer st =
            new StringTokenizer(val.substring(0, pos), ",");
        final List<String> values = new ArrayList<>(st.countTokens());
        while (st.hasMoreTokens()) {
            values.add(st.nextToken());
        }
        return values;
    }

    // can be called in main() to help debugging.
    // Prints out all available charsets and their recognized aliases
    // as returned by the Charset API.
    private static void printAllCharsets() {
        Map<String, Charset> all = Charset.availableCharsets();
        System.out.println("\n=========================================\n");
        for (String can : all.keySet()) {
            System.out.println(can + ": " + all.get(can).aliases());
        }
    }
}
