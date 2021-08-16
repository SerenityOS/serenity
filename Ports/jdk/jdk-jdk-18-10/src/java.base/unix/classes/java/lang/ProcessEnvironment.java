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

/* We use APIs that access the standard Unix environ array, which
 * is defined by UNIX98 to look like:
 *
 *    char **environ;
 *
 * These are unsorted, case-sensitive, null-terminated arrays of bytes
 * of the form FOO=BAR\000 which are usually encoded in the user's
 * default encoding (file.encoding is an excellent choice for
 * encoding/decoding these).  However, even though the user cannot
 * directly access the underlying byte representation, we take pains
 * to pass on the child the exact byte representation we inherit from
 * the parent process for any environment name or value not created by
 * Javaland.  So we keep track of all the byte representations.
 *
 * Internally, we define the types Variable and Value that exhibit
 * String/byteArray duality.  The internal representation of the
 * environment then looks like a Map<Variable,Value>.  But we don't
 * expose this to the user -- we only provide a Map<String,String>
 * view, although we could also provide a Map<byte[],byte[]> view.
 *
 * The non-private methods in this class are not for general use even
 * within this package.  Instead, they are the system-dependent parts
 * of the system-independent method of the same name.  Don't even
 * think of using this class unless your method's name appears below.
 *
 * @author  Martin Buchholz
 * @since   1.5
 */

package java.lang;

import java.io.*;
import java.util.*;


final class ProcessEnvironment
{
    private static final HashMap<Variable,Value> theEnvironment;
    private static final Map<String,String> theUnmodifiableEnvironment;
    static final int MIN_NAME_LENGTH = 0;

    static {
        // We cache the C environment.  This means that subsequent calls
        // to putenv/setenv from C will not be visible from Java code.
        byte[][] environ = environ();
        theEnvironment = new HashMap<>(environ.length/2 + 3);
        // Read environment variables back to front,
        // so that earlier variables override later ones.
        for (int i = environ.length-1; i > 0; i-=2)
            theEnvironment.put(Variable.valueOf(environ[i-1]),
                               Value.valueOf(environ[i]));

        theUnmodifiableEnvironment
            = Collections.unmodifiableMap
            (new StringEnvironment(theEnvironment));
    }

    /* Only for use by System.getenv(String) */
    static String getenv(String name) {
        return theUnmodifiableEnvironment.get(name);
    }

    /* Only for use by System.getenv() */
    static Map<String,String> getenv() {
        return theUnmodifiableEnvironment;
    }

    /* Only for use by ProcessBuilder.environment() */
    @SuppressWarnings("unchecked")
    static Map<String,String> environment() {
        return new StringEnvironment
            ((Map<Variable,Value>)(theEnvironment.clone()));
    }

    /* Only for use by Runtime.exec(...String[]envp...) */
    static Map<String,String> emptyEnvironment(int capacity) {
        return new StringEnvironment(new HashMap<>(capacity));
    }

    private static native byte[][] environ();

    // This class is not instantiable.
    private ProcessEnvironment() {}

    // Check that name is suitable for insertion into Environment map
    private static void validateVariable(String name) {
        if (name.indexOf('=')      != -1 ||
            name.indexOf('\u0000') != -1)
            throw new IllegalArgumentException
                ("Invalid environment variable name: \"" + name + "\"");
    }

    // Check that value is suitable for insertion into Environment map
    private static void validateValue(String value) {
        if (value.indexOf('\u0000') != -1)
            throw new IllegalArgumentException
                ("Invalid environment variable value: \"" + value + "\"");
    }

    // A class hiding the byteArray-String duality of
    // text data on Unixoid operating systems.
    private abstract static class ExternalData {
        protected final String str;
        protected final byte[] bytes;

        protected ExternalData(String str, byte[] bytes) {
            this.str = str;
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String toString() {
            return str;
        }

        public boolean equals(Object o) {
            return o instanceof ExternalData
                && arrayEquals(getBytes(), ((ExternalData) o).getBytes());
        }

        public int hashCode() {
            return arrayHash(getBytes());
        }
    }

    private static class Variable
        extends ExternalData implements Comparable<Variable>
    {
        protected Variable(String str, byte[] bytes) {
            super(str, bytes);
        }

        public static Variable valueOfQueryOnly(Object str) {
            return valueOfQueryOnly((String) str);
        }

        public static Variable valueOfQueryOnly(String str) {
            return new Variable(str, str.getBytes());
        }

        public static Variable valueOf(String str) {
            validateVariable(str);
            return valueOfQueryOnly(str);
        }

        public static Variable valueOf(byte[] bytes) {
            return new Variable(new String(bytes), bytes);
        }

        public int compareTo(Variable variable) {
            return arrayCompare(getBytes(), variable.getBytes());
        }

        public boolean equals(Object o) {
            return o instanceof Variable && super.equals(o);
        }
    }

    private static class Value
        extends ExternalData implements Comparable<Value>
    {
        protected Value(String str, byte[] bytes) {
            super(str, bytes);
        }

        public static Value valueOfQueryOnly(Object str) {
            return valueOfQueryOnly((String) str);
        }

        public static Value valueOfQueryOnly(String str) {
            return new Value(str, str.getBytes());
        }

        public static Value valueOf(String str) {
            validateValue(str);
            return valueOfQueryOnly(str);
        }

        public static Value valueOf(byte[] bytes) {
            return new Value(new String(bytes), bytes);
        }

        public int compareTo(Value value) {
            return arrayCompare(getBytes(), value.getBytes());
        }

        public boolean equals(Object o) {
            return o instanceof Value && super.equals(o);
        }
    }

    // This implements the String map view the user sees.
    private static class StringEnvironment
        extends AbstractMap<String,String>
    {
        private Map<Variable,Value> m;
        private static String toString(Value v) {
            return v == null ? null : v.toString();
        }
        public StringEnvironment(Map<Variable,Value> m) {this.m = m;}
        public int size()        {return m.size();}
        public boolean isEmpty() {return m.isEmpty();}
        public void clear()      {       m.clear();}
        public boolean containsKey(Object key) {
            return m.containsKey(Variable.valueOfQueryOnly(key));
        }
        public boolean containsValue(Object value) {
            return m.containsValue(Value.valueOfQueryOnly(value));
        }
        public String get(Object key) {
            return toString(m.get(Variable.valueOfQueryOnly(key)));
        }
        public String put(String key, String value) {
            return toString(m.put(Variable.valueOf(key),
                                  Value.valueOf(value)));
        }
        public String remove(Object key) {
            return toString(m.remove(Variable.valueOfQueryOnly(key)));
        }
        public Set<String> keySet() {
            return new StringKeySet(m.keySet());
        }
        public Set<Map.Entry<String,String>> entrySet() {
            return new StringEntrySet(m.entrySet());
        }
        public Collection<String> values() {
            return new StringValues(m.values());
        }

        // It is technically feasible to provide a byte-oriented view
        // as follows:
        //      public Map<byte[],byte[]> asByteArrayMap() {
        //          return new ByteArrayEnvironment(m);
        //      }


        // Convert to Unix style environ as a monolithic byte array
        // inspired by the Windows Environment Block, except we work
        // exclusively with bytes instead of chars, and we need only
        // one trailing NUL on Unix.
        // This keeps the JNI as simple and efficient as possible.
        public byte[] toEnvironmentBlock(int[]envc) {
            int count = m.size() * 2; // For added '=' and NUL
            for (Map.Entry<Variable,Value> entry : m.entrySet()) {
                count += entry.getKey().getBytes().length;
                count += entry.getValue().getBytes().length;
            }

            byte[] block = new byte[count];

            int i = 0;
            for (Map.Entry<Variable,Value> entry : m.entrySet()) {
                byte[] key   = entry.getKey  ().getBytes();
                byte[] value = entry.getValue().getBytes();
                System.arraycopy(key, 0, block, i, key.length);
                i+=key.length;
                block[i++] = (byte) '=';
                System.arraycopy(value, 0, block, i, value.length);
                i+=value.length + 1;
                // No need to write NUL byte explicitly
                //block[i++] = (byte) '\u0000';
            }
            envc[0] = m.size();
            return block;
        }
    }

    static byte[] toEnvironmentBlock(Map<String,String> map, int[]envc) {
        return map == null ? null :
            ((StringEnvironment)map).toEnvironmentBlock(envc);
    }


    private static class StringEntry
        implements Map.Entry<String,String>
    {
        private final Map.Entry<Variable,Value> e;
        public StringEntry(Map.Entry<Variable,Value> e) {this.e = e;}
        public String getKey()   {return e.getKey().toString();}
        public String getValue() {return e.getValue().toString();}
        public String setValue(String newValue) {
            return e.setValue(Value.valueOf(newValue)).toString();
        }
        public String toString() {return getKey() + "=" + getValue();}
        public boolean equals(Object o) {
            return o instanceof StringEntry
                && e.equals(((StringEntry)o).e);
        }
        public int hashCode()    {return e.hashCode();}
    }

    private static class StringEntrySet
        extends AbstractSet<Map.Entry<String,String>>
    {
        private final Set<Map.Entry<Variable,Value>> s;
        public StringEntrySet(Set<Map.Entry<Variable,Value>> s) {this.s = s;}
        public int size()        {return s.size();}
        public boolean isEmpty() {return s.isEmpty();}
        public void clear()      {       s.clear();}
        public Iterator<Map.Entry<String,String>> iterator() {
            return new Iterator<Map.Entry<String,String>>() {
                Iterator<Map.Entry<Variable,Value>> i = s.iterator();
                public boolean hasNext() {return i.hasNext();}
                public Map.Entry<String,String> next() {
                    return new StringEntry(i.next());
                }
                public void remove() {i.remove();}
            };
        }
        private static Map.Entry<Variable,Value> vvEntry(final Object o) {
            if (o instanceof StringEntry)
                return ((StringEntry)o).e;
            return new Map.Entry<Variable,Value>() {
                public Variable getKey() {
                    return Variable.valueOfQueryOnly(((Map.Entry)o).getKey());
                }
                public Value getValue() {
                    return Value.valueOfQueryOnly(((Map.Entry)o).getValue());
                }
                public Value setValue(Value value) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        public boolean contains(Object o) { return s.contains(vvEntry(o)); }
        public boolean remove(Object o)   { return s.remove(vvEntry(o)); }
        public boolean equals(Object o) {
            return o instanceof StringEntrySet
                && s.equals(((StringEntrySet) o).s);
        }
        public int hashCode() {return s.hashCode();}
    }

    private static class StringValues
          extends AbstractCollection<String>
    {
        private final Collection<Value> c;
        public StringValues(Collection<Value> c) {this.c = c;}
        public int size()        {return c.size();}
        public boolean isEmpty() {return c.isEmpty();}
        public void clear()      {       c.clear();}
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                Iterator<Value> i = c.iterator();
                public boolean hasNext() {return i.hasNext();}
                public String next()     {return i.next().toString();}
                public void remove()     {i.remove();}
            };
        }
        public boolean contains(Object o) {
            return c.contains(Value.valueOfQueryOnly(o));
        }
        public boolean remove(Object o) {
            return c.remove(Value.valueOfQueryOnly(o));
        }
        public boolean equals(Object o) {
            return o instanceof StringValues
                && c.equals(((StringValues)o).c);
        }
        public int hashCode() {return c.hashCode();}
    }

    private static class StringKeySet extends AbstractSet<String> {
        private final Set<Variable> s;
        public StringKeySet(Set<Variable> s) {this.s = s;}
        public int size()        {return s.size();}
        public boolean isEmpty() {return s.isEmpty();}
        public void clear()      {       s.clear();}
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                Iterator<Variable> i = s.iterator();
                public boolean hasNext() {return i.hasNext();}
                public String next()     {return i.next().toString();}
                public void remove()     {       i.remove();}
            };
        }
        public boolean contains(Object o) {
            return s.contains(Variable.valueOfQueryOnly(o));
        }
        public boolean remove(Object o) {
            return s.remove(Variable.valueOfQueryOnly(o));
        }
    }

    // Replace with general purpose method someday
    private static int arrayCompare(byte[]x, byte[] y) {
        int min = x.length < y.length ? x.length : y.length;
        for (int i = 0; i < min; i++)
            if (x[i] != y[i])
                return x[i] - y[i];
        return x.length - y.length;
    }

    // Replace with general purpose method someday
    private static boolean arrayEquals(byte[] x, byte[] y) {
        if (x.length != y.length)
            return false;
        for (int i = 0; i < x.length; i++)
            if (x[i] != y[i])
                return false;
        return true;
    }

    // Replace with general purpose method someday
    private static int arrayHash(byte[] x) {
        int hash = 0;
        for (int i = 0; i < x.length; i++)
            hash = 31 * hash + x[i];
        return hash;
    }

}
