/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset;

import jdk.internal.misc.VM;
import sun.nio.cs.ThreadLocalCoders;
import sun.security.action.GetPropertyAction;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.spi.CharsetProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * A named mapping between sequences of sixteen-bit Unicode <a
 * href="../../lang/Character.html#unicode">code units</a> and sequences of
 * bytes.  This class defines methods for creating decoders and encoders and
 * for retrieving the various names associated with a charset.  Instances of
 * this class are immutable.
 *
 * <p> This class also defines static methods for testing whether a particular
 * charset is supported, for locating charset instances by name, and for
 * constructing a map that contains every charset for which support is
 * available in the current Java virtual machine.  Support for new charsets can
 * be added via the service-provider interface defined in the {@link
 * java.nio.charset.spi.CharsetProvider} class.
 *
 * <p> All of the methods defined in this class are safe for use by multiple
 * concurrent threads.
 *
 *
 * <h2><a id="names">Charset names</a></h2>
 *
 * <p> Charsets are named by strings composed of the following characters:
 *
 * <ul>
 *
 *   <li> The uppercase letters {@code 'A'} through {@code 'Z'}
 *        (<code>'&#92;u0041'</code>&nbsp;through&nbsp;<code>'&#92;u005a'</code>),
 *
 *   <li> The lowercase letters {@code 'a'} through {@code 'z'}
 *        (<code>'&#92;u0061'</code>&nbsp;through&nbsp;<code>'&#92;u007a'</code>),
 *
 *   <li> The digits {@code '0'} through {@code '9'}
 *        (<code>'&#92;u0030'</code>&nbsp;through&nbsp;<code>'&#92;u0039'</code>),
 *
 *   <li> The dash character {@code '-'}
 *        (<code>'&#92;u002d'</code>,&nbsp;<small>HYPHEN-MINUS</small>),
 *
 *   <li> The plus character {@code '+'}
 *        (<code>'&#92;u002b'</code>,&nbsp;<small>PLUS SIGN</small>),
 *
 *   <li> The period character {@code '.'}
 *        (<code>'&#92;u002e'</code>,&nbsp;<small>FULL STOP</small>),
 *
 *   <li> The colon character {@code ':'}
 *        (<code>'&#92;u003a'</code>,&nbsp;<small>COLON</small>), and
 *
 *   <li> The underscore character {@code '_'}
 *        (<code>'&#92;u005f'</code>,&nbsp;<small>LOW&nbsp;LINE</small>).
 *
 * </ul>
 *
 * A charset name must begin with either a letter or a digit.  The empty string
 * is not a legal charset name.  Charset names are not case-sensitive; that is,
 * case is always ignored when comparing charset names.  Charset names
 * generally follow the conventions documented in <a
 * href="http://www.ietf.org/rfc/rfc2278.txt"><i>RFC&nbsp;2278:&nbsp;IANA Charset
 * Registration Procedures</i></a>.
 *
 * <p> Every charset has a <i>canonical name</i> and may also have one or more
 * <i>aliases</i>.  The canonical name is returned by the {@link #name() name} method
 * of this class.  Canonical names are, by convention, usually in upper case.
 * The aliases of a charset are returned by the {@link #aliases() aliases}
 * method.
 *
 * <p><a id="hn">Some charsets have an <i>historical name</i> that is defined for
 * compatibility with previous versions of the Java platform.</a>  A charset's
 * historical name is either its canonical name or one of its aliases.  The
 * historical name is returned by the {@code getEncoding()} methods of the
 * {@link java.io.InputStreamReader#getEncoding InputStreamReader} and {@link
 * java.io.OutputStreamWriter#getEncoding OutputStreamWriter} classes.
 *
 * <p><a id="iana"> </a>If a charset listed in the <a
 * href="http://www.iana.org/assignments/character-sets"><i>IANA Charset
 * Registry</i></a> is supported by an implementation of the Java platform then
 * its canonical name must be the name listed in the registry. Many charsets
 * are given more than one name in the registry, in which case the registry
 * identifies one of the names as <i>MIME-preferred</i>.  If a charset has more
 * than one registry name then its canonical name must be the MIME-preferred
 * name and the other names in the registry must be valid aliases.  If a
 * supported charset is not listed in the IANA registry then its canonical name
 * must begin with one of the strings {@code "X-"} or {@code "x-"}.
 *
 * <p> The IANA charset registry does change over time, and so the canonical
 * name and the aliases of a particular charset may also change over time.  To
 * ensure compatibility it is recommended that no alias ever be removed from a
 * charset, and that if the canonical name of a charset is changed then its
 * previous canonical name be made into an alias.
 *
 *
 * <h2><a id="standard">Standard charsets</a></h2>
 *
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard charsets.  Consult the release documentation for your
 * implementation to see if any other charsets are supported.  The behavior
 * of such optional charsets may differ between implementations.
 *
 * <blockquote><table class="striped" style="width:80%">
 * <caption style="display:none">Description of standard charsets</caption>
 * <thead>
 * <tr><th scope="col" style="text-align:left">Charset</th><th scope="col" style="text-align:left">Description</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row" style="vertical-align:top">{@code US-ASCII}</th>
 *     <td>Seven-bit ASCII, a.k.a. {@code ISO646-US},
 *         a.k.a. the Basic Latin block of the Unicode character set</td></tr>
 * <tr><th scope="row" style="vertical-align:top"><code>ISO-8859-1&nbsp;&nbsp;</code></th>
 *     <td>ISO Latin Alphabet No. 1, a.k.a. {@code ISO-LATIN-1}</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-8}</th>
 *     <td>Eight-bit UCS Transformation Format</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16BE}</th>
 *     <td>Sixteen-bit UCS Transformation Format,
 *         big-endian byte&nbsp;order</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16LE}</th>
 *     <td>Sixteen-bit UCS Transformation Format,
 *         little-endian byte&nbsp;order</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16}</th>
 *     <td>Sixteen-bit UCS Transformation Format,
 *         byte&nbsp;order identified by an optional byte-order mark</td></tr>
 * </tbody>
 * </table></blockquote>
 *
 * <p> The {@code UTF-8} charset is specified by <a
 * href="http://www.ietf.org/rfc/rfc2279.txt"><i>RFC&nbsp;2279</i></a>; the
 * transformation format upon which it is based is specified in
 * Amendment&nbsp;2 of ISO&nbsp;10646-1 and is also described in the <a
 * href="http://www.unicode.org/standard/standard.html"><i>Unicode
 * Standard</i></a>.
 *
 * <p> The {@code UTF-16} charsets are specified by <a
 * href="http://www.ietf.org/rfc/rfc2781.txt"><i>RFC&nbsp;2781</i></a>; the
 * transformation formats upon which they are based are specified in
 * Amendment&nbsp;1 of ISO&nbsp;10646-1 and are also described in the <a
 * href="http://www.unicode.org/standard/standard.html"><i>Unicode
 * Standard</i></a>.
 *
 * <p> The {@code UTF-16} charsets use sixteen-bit quantities and are
 * therefore sensitive to byte order.  In these encodings the byte order of a
 * stream may be indicated by an initial <i>byte-order mark</i> represented by
 * the Unicode character <code>'&#92;uFEFF'</code>.  Byte-order marks are handled
 * as follows:
 *
 * <ul>
 *
 *   <li><p> When decoding, the {@code UTF-16BE} and {@code UTF-16LE}
 *   charsets interpret the initial byte-order marks as a <small>ZERO-WIDTH
 *   NON-BREAKING SPACE</small>; when encoding, they do not write
 *   byte-order marks. </p></li>
 *
 *   <li><p> When decoding, the {@code UTF-16} charset interprets the
 *   byte-order mark at the beginning of the input stream to indicate the
 *   byte-order of the stream but defaults to big-endian if there is no
 *   byte-order mark; when encoding, it uses big-endian byte order and writes
 *   a big-endian byte-order mark. </p></li>
 *
 * </ul>
 *
 * In any case, byte order marks occurring after the first element of an
 * input sequence are not omitted since the same code is used to represent
 * <small>ZERO-WIDTH NON-BREAKING SPACE</small>.
 *
 * <p> Every instance of the Java virtual machine has a default charset, which
 * may or may not be one of the standard charsets.  The default charset is
 * determined during virtual-machine startup and typically depends upon the
 * locale and charset being used by the underlying operating system. </p>
 *
 * <p> The {@link StandardCharsets} class defines constants for each of the
 * standard charsets.
 *
 * <h2>Terminology</h2>
 *
 * <p> The name of this class is taken from the terms used in
 * <a href="http://www.ietf.org/rfc/rfc2278.txt"><i>RFC&nbsp;2278</i></a>.
 * In that document a <i>charset</i> is defined as the combination of
 * one or more coded character sets and a character-encoding scheme.
 * (This definition is confusing; some other software systems define
 * <i>charset</i> as a synonym for <i>coded character set</i>.)
 *
 * <p> A <i>coded character set</i> is a mapping between a set of abstract
 * characters and a set of integers.  US-ASCII, ISO&nbsp;8859-1,
 * JIS&nbsp;X&nbsp;0201, and Unicode are examples of coded character sets.
 *
 * <p> Some standards have defined a <i>character set</i> to be simply a
 * set of abstract characters without an associated assigned numbering.
 * An alphabet is an example of such a character set.  However, the subtle
 * distinction between <i>character set</i> and <i>coded character set</i>
 * is rarely used in practice; the former has become a short form for the
 * latter, including in the Java API specification.
 *
 * <p> A <i>character-encoding scheme</i> is a mapping between one or more
 * coded character sets and a set of octet (eight-bit byte) sequences.
 * UTF-8, UTF-16, ISO&nbsp;2022, and EUC are examples of
 * character-encoding schemes.  Encoding schemes are often associated with
 * a particular coded character set; UTF-8, for example, is used only to
 * encode Unicode.  Some schemes, however, are associated with multiple
 * coded character sets; EUC, for example, can be used to encode
 * characters in a variety of Asian coded character sets.
 *
 * <p> When a coded character set is used exclusively with a single
 * character-encoding scheme then the corresponding charset is usually
 * named for the coded character set; otherwise a charset is usually named
 * for the encoding scheme and, possibly, the locale of the coded
 * character sets that it supports.  Hence {@code US-ASCII} is both the
 * name of a coded character set and of the charset that encodes it, while
 * {@code EUC-JP} is the name of the charset that encodes the
 * JIS&nbsp;X&nbsp;0201, JIS&nbsp;X&nbsp;0208, and JIS&nbsp;X&nbsp;0212
 * coded character sets for the Japanese language.
 *
 * <p> The native character encoding of the Java programming language is
 * UTF-16.  A charset in the Java platform therefore defines a mapping
 * between sequences of sixteen-bit UTF-16 code units (that is, sequences
 * of chars) and sequences of bytes. </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see CharsetDecoder
 * @see CharsetEncoder
 * @see java.nio.charset.spi.CharsetProvider
 * @see java.lang.Character
 */

public abstract class Charset
    implements Comparable<Charset>
{

    /* -- Static methods -- */

    /**
     * Checks that the given string is a legal charset name. </p>
     *
     * @param  s
     *         A purported charset name
     *
     * @throws  IllegalCharsetNameException
     *          If the given name is not a legal charset name
     */
    private static void checkName(String s) {
        int n = s.length();
        if (n == 0) {
            throw new IllegalCharsetNameException(s);
        }
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') continue;
            if (c >= 'a' && c <= 'z') continue;
            if (c >= '0' && c <= '9') continue;
            if (c == '-' && i != 0) continue;
            if (c == '+' && i != 0) continue;
            if (c == ':' && i != 0) continue;
            if (c == '_' && i != 0) continue;
            if (c == '.' && i != 0) continue;
            throw new IllegalCharsetNameException(s);
        }
    }

    /* The standard set of charsets */
    private static final CharsetProvider standardProvider
        = new sun.nio.cs.StandardCharsets();

    private static final String[] zeroAliases = new String[0];

    // Cache of the most-recently-returned charsets,
    // along with the names that were used to find them
    //
    private static volatile Object[] cache1; // "Level 1" cache
    private static volatile Object[] cache2; // "Level 2" cache

    private static void cache(String charsetName, Charset cs) {
        cache2 = cache1;
        cache1 = new Object[] { charsetName, cs };
    }

    // Creates an iterator that walks over the available providers, ignoring
    // those whose lookup or instantiation causes a security exception to be
    // thrown.  Should be invoked with full privileges.
    //
    private static Iterator<CharsetProvider> providers() {
        return new Iterator<>() {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                ServiceLoader<CharsetProvider> sl =
                    ServiceLoader.load(CharsetProvider.class, cl);
                Iterator<CharsetProvider> i = sl.iterator();
                CharsetProvider next = null;

                private boolean getNext() {
                    while (next == null) {
                        try {
                            if (!i.hasNext())
                                return false;
                            next = i.next();
                        } catch (ServiceConfigurationError sce) {
                            if (sce.getCause() instanceof SecurityException) {
                                // Ignore security exceptions
                                continue;
                            }
                            throw sce;
                        }
                    }
                    return true;
                }

                public boolean hasNext() {
                    return getNext();
                }

                public CharsetProvider next() {
                    if (!getNext())
                        throw new NoSuchElementException();
                    CharsetProvider n = next;
                    next = null;
                    return n;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

            };
    }

    // Thread-local gate to prevent recursive provider lookups
    private static ThreadLocal<ThreadLocal<?>> gate =
            new ThreadLocal<ThreadLocal<?>>();

    @SuppressWarnings("removal")
    private static Charset lookupViaProviders(final String charsetName) {

        // The runtime startup sequence looks up standard charsets as a
        // consequence of the VM's invocation of System.initializeSystemClass
        // in order to, e.g., set system properties and encode filenames.  At
        // that point the application class loader has not been initialized,
        // however, so we can't look for providers because doing so will cause
        // that loader to be prematurely initialized with incomplete
        // information.
        //
        if (!VM.isBooted())
            return null;

        if (gate.get() != null)
            // Avoid recursive provider lookups
            return null;
        try {
            gate.set(gate);

            return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public Charset run() {
                        for (Iterator<CharsetProvider> i = providers();
                             i.hasNext();) {
                            CharsetProvider cp = i.next();
                            Charset cs = cp.charsetForName(charsetName);
                            if (cs != null)
                                return cs;
                        }
                        return null;
                    }
                });

        } finally {
            gate.set(null);
        }
    }

    /* The extended set of charsets */
    private static class ExtendedProviderHolder {
        static final CharsetProvider[] extendedProviders = extendedProviders();
        // returns ExtendedProvider, if installed
        @SuppressWarnings("removal")
        private static CharsetProvider[] extendedProviders() {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                    public CharsetProvider[] run() {
                        CharsetProvider[] cps = new CharsetProvider[1];
                        int n = 0;
                        ServiceLoader<CharsetProvider> sl =
                            ServiceLoader.loadInstalled(CharsetProvider.class);
                        for (CharsetProvider cp : sl) {
                            if (n + 1 > cps.length) {
                                cps = Arrays.copyOf(cps, cps.length << 1);
                            }
                            cps[n++] = cp;
                        }
                        return n == cps.length ? cps : Arrays.copyOf(cps, n);
                    }});
        }
    }

    private static Charset lookupExtendedCharset(String charsetName) {
        if (!VM.isBooted())  // see lookupViaProviders()
            return null;
        CharsetProvider[] ecps = ExtendedProviderHolder.extendedProviders;
        for (CharsetProvider cp : ecps) {
            Charset cs = cp.charsetForName(charsetName);
            if (cs != null)
                return cs;
        }
        return null;
    }

    private static Charset lookup(String charsetName) {
        if (charsetName == null)
            throw new IllegalArgumentException("Null charset name");
        Object[] a;
        if ((a = cache1) != null && charsetName.equals(a[0]))
            return (Charset)a[1];
        // We expect most programs to use one Charset repeatedly.
        // We convey a hint to this effect to the VM by putting the
        // level 1 cache miss code in a separate method.
        return lookup2(charsetName);
    }

    private static Charset lookup2(String charsetName) {
        Object[] a;
        if ((a = cache2) != null && charsetName.equals(a[0])) {
            cache2 = cache1;
            cache1 = a;
            return (Charset)a[1];
        }
        Charset cs;
        if ((cs = standardProvider.charsetForName(charsetName)) != null ||
            (cs = lookupExtendedCharset(charsetName))           != null ||
            (cs = lookupViaProviders(charsetName))              != null)
        {
            cache(charsetName, cs);
            return cs;
        }

        /* Only need to check the name if we didn't find a charset for it */
        checkName(charsetName);
        return null;
    }

    /**
     * Tells whether the named charset is supported.
     *
     * @param  charsetName
     *         The name of the requested charset; may be either
     *         a canonical name or an alias
     *
     * @return  {@code true} if, and only if, support for the named charset
     *          is available in the current Java virtual machine
     *
     * @throws IllegalCharsetNameException
     *         If the given charset name is illegal
     *
     * @throws  IllegalArgumentException
     *          If the given {@code charsetName} is null
     */
    public static boolean isSupported(String charsetName) {
        return (lookup(charsetName) != null);
    }

    /**
     * Returns a charset object for the named charset.
     *
     * @param  charsetName
     *         The name of the requested charset; may be either
     *         a canonical name or an alias
     *
     * @return  A charset object for the named charset
     *
     * @throws  IllegalCharsetNameException
     *          If the given charset name is illegal
     *
     * @throws  IllegalArgumentException
     *          If the given {@code charsetName} is null
     *
     * @throws  UnsupportedCharsetException
     *          If no support for the named charset is available
     *          in this instance of the Java virtual machine
     */
    public static Charset forName(String charsetName) {
        Charset cs = lookup(charsetName);
        if (cs != null)
            return cs;
        throw new UnsupportedCharsetException(charsetName);
    }

    // Fold charsets from the given iterator into the given map, ignoring
    // charsets whose names already have entries in the map.
    //
    private static void put(Iterator<Charset> i, Map<String,Charset> m) {
        while (i.hasNext()) {
            Charset cs = i.next();
            if (!m.containsKey(cs.name()))
                m.put(cs.name(), cs);
        }
    }

    /**
     * Constructs a sorted map from canonical charset names to charset objects.
     *
     * <p> The map returned by this method will have one entry for each charset
     * for which support is available in the current Java virtual machine.  If
     * two or more supported charsets have the same canonical name then the
     * resulting map will contain just one of them; which one it will contain
     * is not specified. </p>
     *
     * <p> The invocation of this method, and the subsequent use of the
     * resulting map, may cause time-consuming disk or network I/O operations
     * to occur.  This method is provided for applications that need to
     * enumerate all of the available charsets, for example to allow user
     * charset selection.  This method is not used by the {@link #forName
     * forName} method, which instead employs an efficient incremental lookup
     * algorithm.
     *
     * <p> This method may return different results at different times if new
     * charset providers are dynamically made available to the current Java
     * virtual machine.  In the absence of such changes, the charsets returned
     * by this method are exactly those that can be retrieved via the {@link
     * #forName forName} method.  </p>
     *
     * @return An immutable, case-insensitive map from canonical charset names
     *         to charset objects
     */
    @SuppressWarnings("removal")
    public static SortedMap<String,Charset> availableCharsets() {
        return AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public SortedMap<String,Charset> run() {
                    TreeMap<String,Charset> m =
                        new TreeMap<>(
                            String.CASE_INSENSITIVE_ORDER);
                    put(standardProvider.charsets(), m);
                    CharsetProvider[] ecps = ExtendedProviderHolder.extendedProviders;
                    for (CharsetProvider ecp :ecps) {
                        put(ecp.charsets(), m);
                    }
                    for (Iterator<CharsetProvider> i = providers(); i.hasNext();) {
                        CharsetProvider cp = i.next();
                        put(cp.charsets(), m);
                    }
                    return Collections.unmodifiableSortedMap(m);
                }
            });
    }

    private static volatile Charset defaultCharset;

    /**
     * Returns the default charset of this Java virtual machine.
     *
     * <p> The default charset is determined during virtual-machine startup and
     * typically depends upon the locale and charset of the underlying
     * operating system.
     *
     * @return  A charset object for the default charset
     *
     * @since 1.5
     */
    public static Charset defaultCharset() {
        if (defaultCharset == null) {
            synchronized (Charset.class) {
                String csn = GetPropertyAction
                        .privilegedGetProperty("file.encoding");
                Charset cs = lookup(csn);
                if (cs != null)
                    defaultCharset = cs;
                else
                    defaultCharset = sun.nio.cs.UTF_8.INSTANCE;
            }
        }
        return defaultCharset;
    }


    /* -- Instance fields and methods -- */

    private final String name;          // tickles a bug in oldjavac
    private final String[] aliases;     // tickles a bug in oldjavac
    private Set<String> aliasSet = null;

    /**
     * Initializes a new charset with the given canonical name and alias
     * set.
     *
     * @param  canonicalName
     *         The canonical name of this charset
     *
     * @param  aliases
     *         An array of this charset's aliases, or null if it has no aliases
     *
     * @throws IllegalCharsetNameException
     *         If the canonical name or any of the aliases are illegal
     */
    protected Charset(String canonicalName, String[] aliases) {
        String[] as = Objects.requireNonNullElse(aliases, zeroAliases);

        // Skip checks for the standard, built-in Charsets we always load
        // during initialization.
        if (canonicalName != "ISO-8859-1"
                && canonicalName != "US-ASCII"
                && canonicalName != "UTF-8") {
            checkName(canonicalName);
            for (int i = 0; i < as.length; i++) {
                checkName(as[i]);
            }
        }
        this.name = canonicalName;
        this.aliases = as;
    }

    /**
     * Returns this charset's canonical name.
     *
     * @return  The canonical name of this charset
     */
    public final String name() {
        return name;
    }

    /**
     * Returns a set containing this charset's aliases.
     *
     * @return  An immutable set of this charset's aliases
     */
    public final Set<String> aliases() {
        if (aliasSet != null)
            return aliasSet;
        int n = aliases.length;
        HashSet<String> hs = new HashSet<>(n);
        for (int i = 0; i < n; i++)
            hs.add(aliases[i]);
        aliasSet = Collections.unmodifiableSet(hs);
        return aliasSet;
    }

    /**
     * Returns this charset's human-readable name for the default locale.
     *
     * <p> The default implementation of this method simply returns this
     * charset's canonical name.  Concrete subclasses of this class may
     * override this method in order to provide a localized display name. </p>
     *
     * @return  The display name of this charset in the default locale
     */
    public String displayName() {
        return name;
    }

    /**
     * Tells whether or not this charset is registered in the <a
     * href="http://www.iana.org/assignments/character-sets">IANA Charset
     * Registry</a>.
     *
     * @return  {@code true} if, and only if, this charset is known by its
     *          implementor to be registered with the IANA
     */
    public final boolean isRegistered() {
        return !name.startsWith("X-") && !name.startsWith("x-");
    }

    /**
     * Returns this charset's human-readable name for the given locale.
     *
     * <p> The default implementation of this method simply returns this
     * charset's canonical name.  Concrete subclasses of this class may
     * override this method in order to provide a localized display name. </p>
     *
     * @param  locale
     *         The locale for which the display name is to be retrieved
     *
     * @return  The display name of this charset in the given locale
     */
    public String displayName(Locale locale) {
        return name;
    }

    /**
     * Tells whether or not this charset contains the given charset.
     *
     * <p> A charset <i>C</i> is said to <i>contain</i> a charset <i>D</i> if,
     * and only if, every character representable in <i>D</i> is also
     * representable in <i>C</i>.  If this relationship holds then it is
     * guaranteed that every string that can be encoded in <i>D</i> can also be
     * encoded in <i>C</i> without performing any replacements.
     *
     * <p> That <i>C</i> contains <i>D</i> does not imply that each character
     * representable in <i>C</i> by a particular byte sequence is represented
     * in <i>D</i> by the same byte sequence, although sometimes this is the
     * case.
     *
     * <p> Every charset contains itself.
     *
     * <p> This method computes an approximation of the containment relation:
     * If it returns {@code true} then the given charset is known to be
     * contained by this charset; if it returns {@code false}, however, then
     * it is not necessarily the case that the given charset is not contained
     * in this charset.
     *
     * @param   cs
     *          The given charset
     *
     * @return  {@code true} if the given charset is contained in this charset
     */
    public abstract boolean contains(Charset cs);

    /**
     * Constructs a new decoder for this charset.
     *
     * @return  A new decoder for this charset
     */
    public abstract CharsetDecoder newDecoder();

    /**
     * Constructs a new encoder for this charset.
     *
     * @return  A new encoder for this charset
     *
     * @throws  UnsupportedOperationException
     *          If this charset does not support encoding
     */
    public abstract CharsetEncoder newEncoder();

    /**
     * Tells whether or not this charset supports encoding.
     *
     * <p> Nearly all charsets support encoding.  The primary exceptions are
     * special-purpose <i>auto-detect</i> charsets whose decoders can determine
     * which of several possible encoding schemes is in use by examining the
     * input byte sequence.  Such charsets do not support encoding because
     * there is no way to determine which encoding should be used on output.
     * Implementations of such charsets should override this method to return
     * {@code false}. </p>
     *
     * @return  {@code true} if, and only if, this charset supports encoding
     */
    public boolean canEncode() {
        return true;
    }

    /**
     * Convenience method that decodes bytes in this charset into Unicode
     * characters.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.newDecoder()
     *       .onMalformedInput(CodingErrorAction.REPLACE)
     *       .onUnmappableCharacter(CodingErrorAction.REPLACE)
     *       .decode(bb); </pre>
     *
     * except that it is potentially more efficient because it can cache
     * decoders between successive invocations.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  In order
     * to detect such sequences, use the {@link
     * CharsetDecoder#decode(java.nio.ByteBuffer)} method directly.  </p>
     *
     * @param  bb  The byte buffer to be decoded
     *
     * @return  A char buffer containing the decoded characters
     */
    public final CharBuffer decode(ByteBuffer bb) {
        try {
            return ThreadLocalCoders.decoderFor(this)
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(bb);
        } catch (CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }

    /**
     * Convenience method that encodes Unicode characters into bytes in this
     * charset.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.newEncoder()
     *       .onMalformedInput(CodingErrorAction.REPLACE)
     *       .onUnmappableCharacter(CodingErrorAction.REPLACE)
     *       .encode(bb); </pre>
     *
     * except that it is potentially more efficient because it can cache
     * encoders between successive invocations.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  In order to
     * detect such sequences, use the {@link
     * CharsetEncoder#encode(java.nio.CharBuffer)} method directly.  </p>
     *
     * @param  cb  The char buffer to be encoded
     *
     * @return  A byte buffer containing the encoded characters
     */
    public final ByteBuffer encode(CharBuffer cb) {
        try {
            return ThreadLocalCoders.encoderFor(this)
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(cb);
        } catch (CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }

    /**
     * Convenience method that encodes a string into bytes in this charset.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.encode(CharBuffer.wrap(s)); </pre>
     *
     * @param  str  The string to be encoded
     *
     * @return  A byte buffer containing the encoded characters
     */
    public final ByteBuffer encode(String str) {
        return encode(CharBuffer.wrap(str));
    }

    /**
     * Compares this charset to another.
     *
     * <p> Charsets are ordered by their canonical names, without regard to
     * case. </p>
     *
     * @param  that
     *         The charset to which this charset is to be compared
     *
     * @return A negative integer, zero, or a positive integer as this charset
     *         is less than, equal to, or greater than the specified charset
     */
    public final int compareTo(Charset that) {
        return (name().compareToIgnoreCase(that.name()));
    }

    /**
     * Computes a hashcode for this charset.
     *
     * @return  An integer hashcode
     */
    public final int hashCode() {
        return name().hashCode();
    }

    /**
     * Tells whether or not this object is equal to another.
     *
     * <p> Two charsets are equal if, and only if, they have the same canonical
     * names.  A charset is never equal to any other type of object.  </p>
     *
     * @return  {@code true} if, and only if, this charset is equal to the
     *          given object
     */
    public final boolean equals(Object ob) {
        if (!(ob instanceof Charset))
            return false;
        if (this == ob)
            return true;
        return name.equals(((Charset)ob).name());
    }

    /**
     * Returns a string describing this charset.
     *
     * @return  A string describing this charset
     */
    public final String toString() {
        return name();
    }

}
