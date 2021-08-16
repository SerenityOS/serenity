/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.invoke.util;

/**
 * Utility routines for dealing with bytecode-level names.
 * Includes universal mangling rules for the JVM.
 *
 * <h3>Avoiding Dangerous Characters </h3>
 *
 * <p>
 * The JVM defines a very small set of characters which are illegal
 * in name spellings.  We will slightly extend and regularize this set
 * into a group of <cite>dangerous characters</cite>.
 * These characters will then be replaced, in mangled names, by escape sequences.
 * In addition, accidental escape sequences must be further escaped.
 * Finally, a special prefix will be applied if and only if
 * the mangling would otherwise fail to begin with the escape character.
 * This happens to cover the corner case of the null string,
 * and also clearly marks symbols which need demangling.
 * </p>
 * <p>
 * Dangerous characters are the union of all characters forbidden
 * or otherwise restricted by the JVM specification,
 * plus their mates, if they are brackets
 * (<code><big><b>[</b></big></code> and <code><big><b>]</b></big></code>,
 * <code><big><b>&lt;</b></big></code> and <code><big><b>&gt;</b></big></code>),
 * plus, arbitrarily, the colon character <code><big><b>:</b></big></code>.
 * There is no distinction between type, method, and field names.
 * This makes it easier to convert between mangled names of different
 * types, since they do not need to be decoded (demangled).
 * </p>
 * <p>
 * The escape character is backslash <code><big><b>\</b></big></code>
 * (also known as reverse solidus).
 * This character is, until now, unheard of in bytecode names,
 * but traditional in the proposed role.
 *
 * </p>
 * <h3> Replacement Characters </h3>
 *
 *
 * <p>
 * Every escape sequence is two characters
 * (in fact, two UTF8 bytes) beginning with
 * the escape character and followed by a
 * <cite>replacement character</cite>.
 * (Since the replacement character is never a backslash,
 * iterated manglings do not double in size.)
 * </p>
 * <p>
 * Each dangerous character has some rough visual similarity
 * to its corresponding replacement character.
 * This makes mangled symbols easier to recognize by sight.
 * </p>
 * <p>
 * The dangerous characters are
 * <code><big><b>/</b></big></code> (forward slash, used to delimit package components),
 * <code><big><b>.</b></big></code> (dot, also a package delimiter),
 * <code><big><b>;</b></big></code> (semicolon, used in signatures),
 * <code><big><b>$</b></big></code> (dollar, used in inner classes and synthetic members),
 * <code><big><b>&lt;</b></big></code> (left angle),
 * <code><big><b>&gt;</b></big></code> (right angle),
 * <code><big><b>[</b></big></code> (left square bracket, used in array types),
 * <code><big><b>]</b></big></code> (right square bracket, reserved in this scheme for language use),
 * and <code><big><b>:</b></big></code> (colon, reserved in this scheme for language use).
 * Their replacements are, respectively,
 * <code><big><b>|</b></big></code> (vertical bar),
 * <code><big><b>,</b></big></code> (comma),
 * <code><big><b>?</b></big></code> (question mark),
 * <code><big><b>%</b></big></code> (percent),
 * <code><big><b>^</b></big></code> (caret),
 * <code><big><b>_</b></big></code> (underscore), and
 * <code><big><b>{</b></big></code> (left curly bracket),
 * <code><big><b>}</b></big></code> (right curly bracket),
 * <code><big><b>!</b></big></code> (exclamation mark).
 * In addition, the replacement character for the escape character itself is
 * <code><big><b>-</b></big></code> (hyphen),
 * and the replacement character for the null prefix is
 * <code><big><b>=</b></big></code> (equal sign).
 * </p>
 * <p>
 * An escape character <code><big><b>\</b></big></code>
 * followed by any of these replacement characters
 * is an escape sequence, and there are no other escape sequences.
 * An equal sign is only part of an escape sequence
 * if it is the second character in the whole string, following a backslash.
 * Two consecutive backslashes do <em>not</em> form an escape sequence.
 * </p>
 * <p>
 * Each escape sequence replaces a so-called <cite>original character</cite>
 * which is either one of the dangerous characters or the escape character.
 * A null prefix replaces an initial null string, not a character.
 * </p>
 * <p>
 * All this implies that escape sequences cannot overlap and may be
 * determined all at once for a whole string.  Note that a spelling
 * string can contain <cite>accidental escapes</cite>, apparent escape
 * sequences which must not be interpreted as manglings.
 * These are disabled by replacing their leading backslash with an
 * escape sequence (<code><big><b>\-</b></big></code>).  To mangle a string, three logical steps
 * are required, though they may be carried out in one pass:
 * </p>
 * <ol>
 *   <li>In each accidental escape, replace the backslash with an escape sequence
 * (<code><big><b>\-</b></big></code>).</li>
 *   <li>Replace each dangerous character with an escape sequence
 * (<code><big><b>\|</b></big></code> for <code><big><b>/</b></big></code>, etc.).</li>
 *   <li>If the first two steps introduced any change, <em>and</em>
 * if the string does not already begin with a backslash, prepend a null prefix (<code><big><b>\=</b></big></code>).</li>
 * </ol>
 *
 * To demangle a mangled string that begins with an escape,
 * remove any null prefix, and then replace (in parallel)
 * each escape sequence by its original character.
 * <p>Spelling strings which contain accidental
 * escapes <em>must</em> have them replaced, even if those
 * strings do not contain dangerous characters.
 * This restriction means that mangling a string always
 * requires a scan of the string for escapes.
 * But then, a scan would be required anyway,
 * to check for dangerous characters.
 *
 * </p>
 * <h3> Nice Properties </h3>
 *
 * <p>
 * If a bytecode name does not contain any escape sequence,
 * demangling is a no-op:  The string demangles to itself.
 * Such a string is called <cite>self-mangling</cite>.
 * Almost all strings are self-mangling.
 * In practice, to demangle almost any name &ldquo;found in nature&rdquo;,
 * simply verify that it does not begin with a backslash.
 * </p>
 * <p>
 * Mangling is a one-to-one function, while demangling
 * is a many-to-one function.
 * A mangled string is defined as <cite>validly mangled</cite> if
 * it is in fact the unique mangling of its spelling string.
 * Three examples of invalidly mangled strings are <code><big><b>\=foo</b></big></code>,
 * <code><big><b>\-bar</b></big></code>, and <code><big><b>baz\!</b></big></code>, which demangle to <code><big><b>foo</b></big></code>, <code><big><b>\bar</b></big></code>, and
 * <code><big><b>baz\!</b></big></code>, but then remangle to <code><big><b>foo</b></big></code>, <code><big><b>\bar</b></big></code>, and <code><big><b>\=baz\-!</b></big></code>.
 * If a language back-end or runtime is using mangled names,
 * it should never present an invalidly mangled bytecode
 * name to the JVM.  If the runtime encounters one,
 * it should also report an error, since such an occurrence
 * probably indicates a bug in name encoding which
 * will lead to errors in linkage.
 * However, this note does not propose that the JVM verifier
 * detect invalidly mangled names.
 * </p>
 * <p>
 * As a result of these rules, it is a simple matter to
 * compute validly mangled substrings and concatenations
 * of validly mangled strings, and (with a little care)
 * these correspond to corresponding operations on their
 * spelling strings.
 * </p>
 * <ul>
 *   <li>Any prefix of a validly mangled string is also validly mangled,
 * although a null prefix may need to be removed.</li>
 *   <li>Any suffix of a validly mangled string is also validly mangled,
 * although a null prefix may need to be added.</li>
 *   <li>Two validly mangled strings, when concatenated,
 * are also validly mangled, although any null prefix
 * must be removed from the second string,
 * and a trailing backslash on the first string may need escaping,
 * if it would participate in an accidental escape when followed
 * by the first character of the second string.</li>
 * </ul>
 * <p>If languages that include non-Java symbol spellings use this
 * mangling convention, they will enjoy the following advantages:
 * </p>
 * <ul>
 *   <li>They can interoperate via symbols they share in common.</li>
 *   <li>Low-level tools, such as backtrace printers, will have readable displays.</li>
 *   <li>Future JVM and language extensions can safely use the dangerous characters
 * for structuring symbols, but will never interfere with valid spellings.</li>
 *   <li>Runtimes and compilers can use standard libraries for mangling and demangling.</li>
 *   <li>Occasional transliterations and name composition will be simple and regular,
 * for classes, methods, and fields.</li>
 *   <li>Bytecode names will continue to be compact.
 * When mangled, spellings will at most double in length, either in
 * UTF8 or UTF16 format, and most will not change at all.</li>
 * </ul>
 *
 *
 * <h3> Suggestions for Human Readable Presentations </h3>
 *
 *
 * <p>
 * For human readable displays of symbols,
 * it will be better to present a string-like quoted
 * representation of the spelling, because JVM users
 * are generally familiar with such tokens.
 * We suggest using single or double quotes before and after
 * mangled symbols which are not valid Java identifiers,
 * with quotes, backslashes, and non-printing characters
 * escaped as if for literals in the Java language.
 * </p>
 * <p>
 * For example, an HTML-like spelling
 * <code><big><b>&lt;pre&gt;</b></big></code> mangles to
 * <code><big><b>\^pre\_</b></big></code> and could
 * display more cleanly as
 * <code><big><b>'&lt;pre&gt;'</b></big></code>,
 * with the quotes included.
 * Such string-like conventions are <em>not</em> suitable
 * for mangled bytecode names, in part because
 * dangerous characters must be eliminated, rather
 * than just quoted.  Otherwise internally structured
 * strings like package prefixes and method signatures
 * could not be reliably parsed.
 * </p>
 * <p>
 * In such human-readable displays, invalidly mangled
 * names should <em>not</em> be demangled and quoted,
 * for this would be misleading.  Likewise, JVM symbols
 * which contain dangerous characters (like dots in field
 * names or brackets in method names) should not be
 * simply quoted.  The bytecode names
 * <code><big><b>\=phase\,1</b></big></code> and
 * <code><big><b>phase.1</b></big></code> are distinct,
 * and in demangled displays they should be presented as
 * <code><big><b>'phase.1'</b></big></code> and something like
 * <code><big><b>'phase'.1</b></big></code>, respectively.
 * </p>
 *
 * @author John Rose
 * @version 1.2, 02/06/2008
 * @see http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
 */
public class BytecodeName {
    private BytecodeName() { }  // static only class

    /** Given a source name, produce the corresponding bytecode name.
     * The source name should not be qualified, because any syntactic
     * markers (dots, slashes, dollar signs, colons, etc.) will be mangled.
     * @param s the source name
     * @return a valid bytecode name which represents the source name
     */
    public static String toBytecodeName(String s) {
        String bn = mangle(s);
        assert((Object)bn == s || looksMangled(bn)) : bn;
        assert(s.equals(toSourceName(bn))) : s;
        return bn;
    }

    /** Given an unqualified bytecode name, produce the corresponding source name.
     * The bytecode name must not contain dangerous characters.
     * In particular, it must not be qualified or segmented by colon {@code ':'}.
     * @param s the bytecode name
     * @return the source name, which may possibly have unsafe characters
     * @throws IllegalArgumentException if the bytecode name is not {@link #isSafeBytecodeName safe}
     * @see #isSafeBytecodeName(java.lang.String)
     */
    public static String toSourceName(String s) {
        checkSafeBytecodeName(s);
        String sn = s;
        if (looksMangled(s)) {
            sn = demangle(s);
            assert(s.equals(mangle(sn))) : s+" => "+sn+" => "+mangle(sn);
        }
        return sn;
    }

    /**
     * Given a bytecode name from a classfile, separate it into
     * components delimited by dangerous characters.
     * Each resulting array element will be either a dangerous character,
     * or else a safe bytecode name.
     * (The safe name might possibly be mangled to hide further dangerous characters.)
     * For example, the qualified class name {@code java/lang/String}
     * will be parsed into the array {@code {"java", '/', "lang", '/', "String"}}.
     * The name {@code <init>} will be parsed into {@code {'<', "init", '>'}}.
     * The name {@code foo/bar$:baz} will be parsed into
     * {@code {"foo", '/', "bar", '$', ':', "baz"}}.
     * The name {@code ::\=:foo:\=bar\!baz} will be parsed into
     * {@code {':', ':', "", ':', "foo", ':', "bar:baz"}}.
     */
    public static Object[] parseBytecodeName(String s) {
        int slen = s.length();
        Object[] res = null;
        for (int pass = 0; pass <= 1; pass++) {
            int fillp = 0;
            int lasti = 0;
            for (int i = 0; i <= slen; i++) {
                int whichDC = -1;
                if (i < slen) {
                    whichDC = DANGEROUS_CHARS.indexOf(s.charAt(i));
                    if (whichDC < DANGEROUS_CHAR_FIRST_INDEX)  continue;
                }
                // got to end of string or next dangerous char
                if (lasti < i) {
                    // normal component
                    if (pass != 0)
                        res[fillp] = toSourceName(s.substring(lasti, i));
                    fillp++;
                    lasti = i+1;
                }
                if (whichDC >= DANGEROUS_CHAR_FIRST_INDEX) {
                    if (pass != 0)
                        res[fillp] = DANGEROUS_CHARS_CA[whichDC];
                    fillp++;
                    lasti = i+1;
                }
            }
            if (pass != 0)  break;
            // between passes, build the result array
            res = new Object[fillp];
            if (fillp <= 1 && lasti == 0) {
                if (fillp != 0)  res[0] = toSourceName(s);
                break;
            }
        }
        return res;
    }

    /**
     * Given a series of components, create a bytecode name for a classfile.
     * This is the inverse of {@link #parseBytecodeName(java.lang.String)}.
     * Each component must either be an interned one-character string of
     * a dangerous character, or else a safe bytecode name.
     * @param components a series of name components
     * @return the concatenation of all components
     * @throws IllegalArgumentException if any component contains an unsafe
     *          character, and is not an interned one-character string
     * @throws NullPointerException if any component is null
     */
    public static String unparseBytecodeName(Object[] components) {
        Object[] components0 = components;
        for (int i = 0; i < components.length; i++) {
            Object c = components[i];
            if (c instanceof String) {
                String mc = toBytecodeName((String) c);
                if (i == 0 && components.length == 1)
                    return mc;  // usual case
                if ((Object)mc != c) {
                    if (components == components0)
                        components = components.clone();
                    components[i] = c = mc;
                }
            }
        }
        return appendAll(components);
    }
    private static String appendAll(Object[] components) {
        if (components.length <= 1) {
            if (components.length == 1) {
                return String.valueOf(components[0]);
            }
            return "";
        }
        int slen = 0;
        for (Object c : components) {
            if (c instanceof String)
                slen += String.valueOf(c).length();
            else
                slen += 1;
        }
        StringBuilder sb = new StringBuilder(slen);
        for (Object c : components) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Given a bytecode name, produce the corresponding display name.
     * This is the source name, plus quotes if needed.
     * If the bytecode name contains dangerous characters,
     * assume that they are being used as punctuation,
     * and pass them through unchanged.
     * Non-empty runs of non-dangerous characters are demangled
     * if necessary, and the resulting names are quoted if
     * they are not already valid Java identifiers, or if
     * they contain a dangerous character (i.e., dollar sign "$").
     * Single quotes are used when quoting.
     * Within quoted names, embedded single quotes and backslashes
     * are further escaped by prepended backslashes.
     *
     * @param s the original bytecode name (which may be qualified)
     * @return a human-readable presentation
     */
    public static String toDisplayName(String s) {
        Object[] components = parseBytecodeName(s);
        for (int i = 0; i < components.length; i++) {
            if (!(components[i] instanceof String))
                continue;
            String sn = (String) components[i];
            // note that the name is already demangled!
            //sn = toSourceName(sn);
            if (!isJavaIdent(sn) || sn.indexOf('$') >=0 ) {
                components[i] = quoteDisplay(sn);
            }
        }
        return appendAll(components);
    }
    private static boolean isJavaIdent(String s) {
        int slen = s.length();
        if (slen == 0)  return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0)))
            return false;
        for (int i = 1; i < slen; i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i)))
                return false;
        }
        return true;
    }
    private static String quoteDisplay(String s) {
        // TO DO:  Replace wierd characters in s by C-style escapes.
        return "'"+s.replaceAll("['\\\\]", "\\\\$0")+"'";
    }

    private static void checkSafeBytecodeName(String s)
            throws IllegalArgumentException {
        if (!isSafeBytecodeName(s)) {
            throw new IllegalArgumentException(s);
        }
    }

    /**
     * Report whether a simple name is safe as a bytecode name.
     * Such names are acceptable in class files as class, method, and field names.
     * Additionally, they are free of "dangerous" characters, even if those
     * characters are legal in some (or all) names in class files.
     * @param s the proposed bytecode name
     * @return true if the name is non-empty and all of its characters are safe
     */
    public static boolean isSafeBytecodeName(String s) {
        if (s.isEmpty())  return false;
        // check occurrences of each DANGEROUS char
        for (char xc : DANGEROUS_CHARS_A) {
            if (xc == ESCAPE_C)  continue;  // not really that dangerous
            if (s.indexOf(xc) >= 0)  return false;
        }
        return true;
    }

    /**
     * Report whether a character is safe in a bytecode name.
     * This is true of any unicode character except the following
     * <em>dangerous characters</em>: {@code ".;:$[]<>/"}.
     * @param c the proposed character
     * @return true if the character is safe to use in classfiles
     */
    public static boolean isSafeBytecodeChar(char c) {
        return DANGEROUS_CHARS.indexOf(c) < DANGEROUS_CHAR_FIRST_INDEX;
    }

    private static boolean looksMangled(String s) {
        return s.charAt(0) == ESCAPE_C;
    }

    private static String mangle(String s) {
        if (s.isEmpty())
            return NULL_ESCAPE;

        // build this lazily, when we first need an escape:
        StringBuilder sb = null;

        for (int i = 0, slen = s.length(); i < slen; i++) {
            char c = s.charAt(i);

            boolean needEscape = false;
            if (c == ESCAPE_C) {
                if (i+1 < slen) {
                    char c1 = s.charAt(i+1);
                    if ((i == 0 && c1 == NULL_ESCAPE_C)
                        || c1 != originalOfReplacement(c1)) {
                        // an accidental escape
                        needEscape = true;
                    }
                }
            } else {
                needEscape = isDangerous(c);
            }

            if (!needEscape) {
                if (sb != null)  sb.append(c);
                continue;
            }

            // build sb if this is the first escape
            if (sb == null) {
                sb = new StringBuilder(s.length()+10);
                // mangled names must begin with a backslash:
                if (s.charAt(0) != ESCAPE_C && i > 0)
                    sb.append(NULL_ESCAPE);
                // append the string so far, which is unremarkable:
                sb.append(s, 0, i);
            }

            // rewrite \ to \-, / to \|, etc.
            sb.append(ESCAPE_C);
            sb.append(replacementOf(c));
        }

        if (sb != null)   return sb.toString();

        return s;
    }

    private static String demangle(String s) {
        // build this lazily, when we first meet an escape:
        StringBuilder sb = null;

        int stringStart = 0;
        if (s.startsWith(NULL_ESCAPE))
            stringStart = 2;

        for (int i = stringStart, slen = s.length(); i < slen; i++) {
            char c = s.charAt(i);

            if (c == ESCAPE_C && i+1 < slen) {
                // might be an escape sequence
                char rc = s.charAt(i+1);
                char oc = originalOfReplacement(rc);
                if (oc != rc) {
                    // build sb if this is the first escape
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                        // append the string so far, which is unremarkable:
                        sb.append(s, stringStart, i);
                    }
                    ++i;  // skip both characters
                    c = oc;
                }
            }

            if (sb != null)
                sb.append(c);
        }

        if (sb != null)   return sb.toString();

        return s.substring(stringStart);
    }

    static char ESCAPE_C = '\\';
    // empty escape sequence to avoid a null name or illegal prefix
    static char NULL_ESCAPE_C = '=';
    static String NULL_ESCAPE = ESCAPE_C+""+NULL_ESCAPE_C;

    static final String DANGEROUS_CHARS   = "\\/.;:$[]<>"; // \\ must be first
    static final String REPLACEMENT_CHARS =  "-|,?!%{}^_";
    static final int DANGEROUS_CHAR_FIRST_INDEX = 1; // index after \\
    static char[] DANGEROUS_CHARS_A   = DANGEROUS_CHARS.toCharArray();
    static char[] REPLACEMENT_CHARS_A = REPLACEMENT_CHARS.toCharArray();
    static final Character[] DANGEROUS_CHARS_CA;
    static {
        Character[] dcca = new Character[DANGEROUS_CHARS.length()];
        for (int i = 0; i < dcca.length; i++)
            dcca[i] = Character.valueOf(DANGEROUS_CHARS.charAt(i));
        DANGEROUS_CHARS_CA = dcca;
    }

    static final long[] SPECIAL_BITMAP = new long[2];  // 128 bits
    static {
        String SPECIAL = DANGEROUS_CHARS + REPLACEMENT_CHARS;
        //System.out.println("SPECIAL = "+SPECIAL);
        for (char c : SPECIAL.toCharArray()) {
            SPECIAL_BITMAP[c >>> 6] |= 1L << c;
        }
    }
    static boolean isSpecial(char c) {
        if ((c >>> 6) < SPECIAL_BITMAP.length)
            return ((SPECIAL_BITMAP[c >>> 6] >> c) & 1) != 0;
        else
            return false;
    }
    static char replacementOf(char c) {
        if (!isSpecial(c))  return c;
        int i = DANGEROUS_CHARS.indexOf(c);
        if (i < 0)  return c;
        return REPLACEMENT_CHARS.charAt(i);
    }
    static char originalOfReplacement(char c) {
        if (!isSpecial(c))  return c;
        int i = REPLACEMENT_CHARS.indexOf(c);
        if (i < 0)  return c;
        return DANGEROUS_CHARS.charAt(i);
    }
    static boolean isDangerous(char c) {
        if (!isSpecial(c))  return false;
        return (DANGEROUS_CHARS.indexOf(c) >= DANGEROUS_CHAR_FIRST_INDEX);
    }
    static int indexOfDangerousChar(String s, int from) {
        for (int i = from, slen = s.length(); i < slen; i++) {
            if (isDangerous(s.charAt(i)))
                return i;
        }
        return -1;
    }
    static int lastIndexOfDangerousChar(String s, int from) {
        for (int i = Math.min(from, s.length()-1); i >= 0; i--) {
            if (isDangerous(s.charAt(i)))
                return i;
        }
        return -1;
    }


}
