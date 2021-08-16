/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * The MIT License
 *
 * Copyright (c) 2004-2015 Paul R. Holser, Jr.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package jdk.internal.joptsimple.internal;

import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A map whose keys are strings; when a key/value pair is added to the map, the longest unique abbreviations of that
 * key are added as well, and associated with the value. Thus:</p>
 *
 * <pre>
 *   <code>
 *   abbreviations.put( "good", "bye" );
 *   </code>
 * </pre>
 *
 * <p>would make it such that you could retrieve the value {@code "bye"} from the map using the keys {@code "good"},
 * {@code "goo"}, {@code "go"}, and {@code "g"}. A subsequent invocation of:</p>
 * <pre>
 *   <code>
 *   abbreviations.put( "go", "fish" );
 *   </code>
 * </pre>
 *
 * <p>would make it such that you could retrieve the value {@code "bye"} using the keys {@code "good"} and
 * {@code "goo"}, and the value {@code "fish"} using the key {@code "go"}.  The key {@code "g"} would yield
 * {@code null}, since it would no longer be a unique abbreviation.</p>
 *
 * <p>The data structure is much like a "trie".</p>
 *
 * @param <V> a constraint on the types of the values in the map
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 * @see <a href="http://perldoc.perl.org/Text/Abbrev.html">Perl's Text::Abbrev module</a>
 * @see <a href="https://en.wikipedia.org/wiki/Radix_tree">Radix tree</a>
 */
public class AbbreviationMap<V> implements OptionNameMap<V> {
    private final Map<Character, AbbreviationMap<V>> children = new TreeMap<>();

    private String key;
    private V value;
    private int keysBeyond;

    /**
     * <p>Tells whether the given key is in the map, or whether the given key is a unique
     * abbreviation of a key that is in the map.</p>
     *
     * @param key key to look up
     * @return {@code true} if {@code key} is present in the map
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @Override
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * <p>Answers the value associated with the given key.  The key can be a unique
     * abbreviation of a key that is in the map. </p>
     *
     * @param key key to look up
     * @return the value associated with {@code aKey}; or {@code null} if there is no
     * such value or {@code aKey} is not a unique abbreviation of a key in the map
     * @throws NullPointerException if {@code aKey} is {@code null}
     */
    @Override
    public V get( String key ) {
        char[] chars = charsOf( key );

        AbbreviationMap<V> child = this;
        for ( char each : chars ) {
            child = child.children.get( each );
            if ( child == null )
                return null;
        }

        return child.value;
    }

    /**
     * <p>Associates a given value with a given key.  If there was a previous
     * association, the old value is replaced with the new one.</p>
     *
     * @param key key to create in the map
     * @param newValue value to associate with the key
     * @throws NullPointerException if {@code aKey} or {@code newValue} is {@code null}
     * @throws IllegalArgumentException if {@code aKey} is a zero-length string
     */
    @Override
    public void put( String key, V newValue ) {
        if ( newValue == null )
            throw new NullPointerException();
        if ( key.length() == 0 )
            throw new IllegalArgumentException();

        char[] chars = charsOf(key);
        add( chars, newValue, 0, chars.length );
    }

    /**
     * <p>Associates a given value with a given set of keys.  If there was a previous
     * association, the old value is replaced with the new one.</p>
     *
     * @param keys keys to create in the map
     * @param newValue value to associate with the key
     * @throws NullPointerException if {@code keys} or {@code newValue} is {@code null}
     * @throws IllegalArgumentException if any of {@code keys} is a zero-length string
     */
    @Override
    public void putAll( Iterable<String> keys, V newValue ) {
        for ( String each : keys )
            put( each, newValue );
    }

    private boolean add( char[] chars, V newValue, int offset, int length ) {
        if ( offset == length ) {
            value = newValue;
            boolean wasAlreadyAKey = key != null;
            key = new String( chars );
            return !wasAlreadyAKey;
        }

        char nextChar = chars[ offset ];
        AbbreviationMap<V> child = children.get( nextChar );
        if ( child == null ) {
            child = new AbbreviationMap<>();
            children.put( nextChar, child );
        }

        boolean newKeyAdded = child.add( chars, newValue, offset + 1, length );

        if ( newKeyAdded )
            ++keysBeyond;

        if ( key == null )
            value = keysBeyond > 1 ? null : newValue;

        return newKeyAdded;
    }

    /**
     * <p>If the map contains the given key, dissociates the key from its value.</p>
     *
     * @param key key to remove
     * @throws NullPointerException if {@code aKey} is {@code null}
     * @throws IllegalArgumentException if {@code aKey} is a zero-length string
     */
    @Override
    public void remove( String key ) {
        if ( key.length() == 0 )
            throw new IllegalArgumentException();

        char[] keyChars = charsOf(key);
        remove( keyChars, 0, keyChars.length );
    }

    private boolean remove( char[] aKey, int offset, int length ) {
        if ( offset == length )
            return removeAtEndOfKey();

        char nextChar = aKey[ offset ];
        AbbreviationMap<V> child = children.get( nextChar );
        if ( child == null || !child.remove( aKey, offset + 1, length ) )
            return false;

        --keysBeyond;
        if ( child.keysBeyond == 0 )
            children.remove( nextChar );
        if ( keysBeyond == 1 && key == null )
            setValueToThatOfOnlyChild();

        return true;
    }

    private void setValueToThatOfOnlyChild() {
        Map.Entry<Character, AbbreviationMap<V>> entry = children.entrySet().iterator().next();
        AbbreviationMap<V> onlyChild = entry.getValue();
        value = onlyChild.value;
    }

    private boolean removeAtEndOfKey() {
        if ( key == null )
            return false;

        key = null;
        if ( keysBeyond == 1 )
            setValueToThatOfOnlyChild();
        else
            value = null;

        return true;
    }

    /**
     * Gives a Java map representation of this abbreviation map.
     *
     * @return a Java map corresponding to this abbreviation map
     */
    @Override
    public Map<String, V> toJavaUtilMap() {
        Map<String, V> mappings = new TreeMap<>();
        addToMappings( mappings );
        return mappings;
    }

    private void addToMappings( Map<String, V> mappings ) {
        if ( key != null )
            mappings.put( key, value );

        for ( AbbreviationMap<V> each : children.values() )
            each.addToMappings( mappings );
    }

    private static char[] charsOf( String aKey ) {
        char[] chars = new char[ aKey.length() ];
        aKey.getChars( 0, aKey.length(), chars, 0 );
        return chars;
    }
}
