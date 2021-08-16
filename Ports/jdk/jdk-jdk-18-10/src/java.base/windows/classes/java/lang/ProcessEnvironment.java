/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

/* We use APIs that access a so-called Windows "Environment Block",
 * which looks like an array of jchars like this:
 *
 * FOO=BAR\u0000 ... GORP=QUUX\u0000\u0000
 *
 * This data structure has a number of peculiarities we must contend with:
 * (see: http://windowssdk.msdn.microsoft.com/en-us/library/ms682009.aspx)
 * - The NUL jchar separators, and a double NUL jchar terminator.
 *   It appears that the Windows implementation requires double NUL
 *   termination even if the environment is empty.  We should always
 *   generate environments with double NUL termination, while accepting
 *   empty environments consisting of a single NUL.
 * - on Windows9x, this is actually an array of 8-bit chars, not jchars,
 *   encoded in the system default encoding.
 * - The block must be sorted by Unicode value, case-insensitively,
 *   as if folded to upper case.
 * - There are magic environment variables maintained by Windows
 *   that start with a `=' (!) character.  These are used for
 *   Windows drive current directory (e.g. "=C:=C:\WINNT") or the
 *   exit code of the last command (e.g. "=ExitCode=0000001").
 *
 * Since Java and non-9x Windows speak the same character set, and
 * even the same encoding, we don't have to deal with unreliable
 * conversion to byte streams.  Just add a few NUL terminators.
 *
 * System.getenv(String) is case-insensitive, while System.getenv()
 * returns a map that is case-sensitive, which is consistent with
 * native Windows APIs.
 *
 * The non-private methods in this class are not for general use even
 * within this package.  Instead, they are the system-dependent parts
 * of the system-independent method of the same name.  Don't even
 * think of using this class unless your method's name appears below.
 *
 * @author Martin Buchholz
 * @since 1.5
 */

package java.lang;

import java.io.*;
import java.util.*;

final class ProcessEnvironment extends HashMap<String,String>
{

    private static final long serialVersionUID = -8017839552603542824L;

    private static String validateName(String name) {
        // An initial `=' indicates a magic Windows variable name -- OK
        if (name.indexOf('=', 1)   != -1 ||
            name.indexOf('\u0000') != -1)
            throw new IllegalArgumentException
                ("Invalid environment variable name: \"" + name + "\"");
        return name;
    }

    private static String validateValue(String value) {
        if (value.indexOf('\u0000') != -1)
            throw new IllegalArgumentException
                ("Invalid environment variable value: \"" + value + "\"");
        return value;
    }

    private static String nonNullString(Object o) {
        if (o == null)
            throw new NullPointerException();
        return (String) o;
    }

    public String put(String key, String value) {
        return super.put(validateName(key), validateValue(value));
    }

    public String get(Object key) {
        return super.get(nonNullString(key));
    }

    public boolean containsKey(Object key) {
        return super.containsKey(nonNullString(key));
    }

    public boolean containsValue(Object value) {
        return super.containsValue(nonNullString(value));
    }

    public String remove(Object key) {
        return super.remove(nonNullString(key));
    }

    private static class CheckedEntry
        implements Map.Entry<String,String>
    {
        private final Map.Entry<String,String> e;
        public CheckedEntry(Map.Entry<String,String> e) {this.e = e;}
        public String getKey()   { return e.getKey();}
        public String getValue() { return e.getValue();}
        public String setValue(String value) {
            return e.setValue(validateValue(value));
        }
        public String toString() { return getKey() + "=" + getValue();}
        public boolean equals(Object o) {return e.equals(o);}
        public int hashCode()    {return e.hashCode();}
    }

    private static class CheckedEntrySet
        extends AbstractSet<Map.Entry<String,String>>
    {
        private final Set<Map.Entry<String,String>> s;
        public CheckedEntrySet(Set<Map.Entry<String,String>> s) {this.s = s;}
        public int size()        {return s.size();}
        public boolean isEmpty() {return s.isEmpty();}
        public void clear()      {       s.clear();}
        public Iterator<Map.Entry<String,String>> iterator() {
            return new Iterator<Map.Entry<String,String>>() {
                Iterator<Map.Entry<String,String>> i = s.iterator();
                public boolean hasNext() { return i.hasNext();}
                public Map.Entry<String,String> next() {
                    return new CheckedEntry(i.next());
                }
                public void remove() { i.remove();}
            };
        }
        private static Map.Entry<String,String> checkedEntry(Object o) {
            @SuppressWarnings("unchecked")
            Map.Entry<String,String> e = (Map.Entry<String,String>) o;
            nonNullString(e.getKey());
            nonNullString(e.getValue());
            return e;
        }
        public boolean contains(Object o) {return s.contains(checkedEntry(o));}
        public boolean remove(Object o)   {return s.remove(checkedEntry(o));}
    }

    private static class CheckedValues extends AbstractCollection<String> {
        private final Collection<String> c;
        public CheckedValues(Collection<String> c) {this.c = c;}
        public int size()                  {return c.size();}
        public boolean isEmpty()           {return c.isEmpty();}
        public void clear()                {       c.clear();}
        public Iterator<String> iterator() {return c.iterator();}
        public boolean contains(Object o)  {return c.contains(nonNullString(o));}
        public boolean remove(Object o)    {return c.remove(nonNullString(o));}
    }

    private static class CheckedKeySet extends AbstractSet<String> {
        private final Set<String> s;
        public CheckedKeySet(Set<String> s) {this.s = s;}
        public int size()                  {return s.size();}
        public boolean isEmpty()           {return s.isEmpty();}
        public void clear()                {       s.clear();}
        public Iterator<String> iterator() {return s.iterator();}
        public boolean contains(Object o)  {return s.contains(nonNullString(o));}
        public boolean remove(Object o)    {return s.remove(nonNullString(o));}
    }

    public Set<String> keySet() {
        return new CheckedKeySet(super.keySet());
    }

    public Collection<String> values() {
        return new CheckedValues(super.values());
    }

    public Set<Map.Entry<String,String>> entrySet() {
        return new CheckedEntrySet(super.entrySet());
    }


    private static final class NameComparator
        implements Comparator<String> {
        public int compare(String s1, String s2) {
            // We can't use String.compareToIgnoreCase since it
            // canonicalizes to lower case, while Windows
            // canonicalizes to upper case!  For example, "_" should
            // sort *after* "Z", not before.
            int n1 = s1.length();
            int n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2)
                        // No overflow because of numeric promotion
                        return c1 - c2;
                }
            }
            return n1 - n2;
        }
    }

    private static final class EntryComparator
        implements Comparator<Map.Entry<String,String>> {
        public int compare(Map.Entry<String,String> e1,
                           Map.Entry<String,String> e2) {
            return nameComparator.compare(e1.getKey(), e2.getKey());
        }
    }

    // Allow `=' as first char in name, e.g. =C:=C:\DIR
    static final int MIN_NAME_LENGTH = 1;

    private static final NameComparator nameComparator;
    private static final EntryComparator entryComparator;
    private static final ProcessEnvironment theEnvironment;
    private static final Map<String,String> theUnmodifiableEnvironment;
    private static final Map<String,String> theCaseInsensitiveEnvironment;

    static {
        nameComparator  = new NameComparator();
        entryComparator = new EntryComparator();
        theEnvironment  = new ProcessEnvironment();
        theUnmodifiableEnvironment
            = Collections.unmodifiableMap(theEnvironment);

        String envblock = environmentBlock();
        int beg, end, eql;
        for (beg = 0;
             ((end = envblock.indexOf('\u0000', beg  )) != -1 &&
              // An initial `=' indicates a magic Windows variable name -- OK
              (eql = envblock.indexOf('='     , beg+1)) != -1);
             beg = end + 1) {
            // Ignore corrupted environment strings.
            if (eql < end)
                theEnvironment.put(envblock.substring(beg, eql),
                                   envblock.substring(eql+1,end));
        }

        theCaseInsensitiveEnvironment = new TreeMap<>(nameComparator);
        theCaseInsensitiveEnvironment.putAll(theEnvironment);
    }

    private ProcessEnvironment() {
        super();
    }

    private ProcessEnvironment(int capacity) {
        super(capacity);
    }

    // Only for use by System.getenv(String)
    static String getenv(String name) {
        // The original implementation used a native call to _wgetenv,
        // but it turns out that _wgetenv is only consistent with
        // GetEnvironmentStringsW (for non-ASCII) if `wmain' is used
        // instead of `main', even in a process created using
        // CREATE_UNICODE_ENVIRONMENT.  Instead we perform the
        // case-insensitive comparison ourselves.  At least this
        // guarantees that System.getenv().get(String) will be
        // consistent with System.getenv(String).
        return theCaseInsensitiveEnvironment.get(name);
    }

    // Only for use by System.getenv()
    static Map<String,String> getenv() {
        return theUnmodifiableEnvironment;
    }

    // Only for use by ProcessBuilder.environment()
    @SuppressWarnings("unchecked")
    static Map<String,String> environment() {
        return (Map<String,String>) theEnvironment.clone();
    }

    // Only for use by ProcessBuilder.environment(String[] envp)
    static Map<String,String> emptyEnvironment(int capacity) {
        return new ProcessEnvironment(capacity);
    }

    private static native String environmentBlock();

    // Only for use by ProcessImpl.start()
    String toEnvironmentBlock() {
        // Sort Unicode-case-insensitively by name
        List<Map.Entry<String,String>> list = new ArrayList<>(entrySet());
        Collections.sort(list, entryComparator);

        StringBuilder sb = new StringBuilder(size()*30);
        int cmp = -1;

        // Some versions of MSVCRT.DLL require SystemRoot to be set.
        // So, we make sure that it is always set, even if not provided
        // by the caller.
        final String SYSTEMROOT = "SystemRoot";

        for (Map.Entry<String,String> e : list) {
            String key = e.getKey();
            String value = e.getValue();
            if (cmp < 0 && (cmp = nameComparator.compare(key, SYSTEMROOT)) > 0) {
                // Not set, so add it here
                addToEnvIfSet(sb, SYSTEMROOT);
            }
            addToEnv(sb, key, value);
        }
        if (cmp < 0) {
            // Got to end of list and still not found
            addToEnvIfSet(sb, SYSTEMROOT);
        }
        if (sb.length() == 0) {
            // Environment was empty and SystemRoot not set in parent
            sb.append('\u0000');
        }
        // Block is double NUL terminated
        sb.append('\u0000');
        return sb.toString();
    }

    // add the environment variable to the child, if it exists in parent
    private static void addToEnvIfSet(StringBuilder sb, String name) {
        String s = getenv(name);
        if (s != null)
            addToEnv(sb, name, s);
    }

    private static void addToEnv(StringBuilder sb, String name, String val) {
        sb.append(name).append('=').append(val).append('\u0000');
    }

    static String toEnvironmentBlock(Map<String,String> map) {
        return map == null ? null :
            ((ProcessEnvironment)map).toEnvironmentBlock();
    }
}
