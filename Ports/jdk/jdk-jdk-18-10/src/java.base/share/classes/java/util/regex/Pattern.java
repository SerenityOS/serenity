/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.util.ArraysSupport;

/**
 * A compiled representation of a regular expression.
 *
 * <p> A regular expression, specified as a string, must first be compiled into
 * an instance of this class.  The resulting pattern can then be used to create
 * a {@link Matcher} object that can match arbitrary {@linkplain
 * java.lang.CharSequence character sequences} against the regular
 * expression.  All of the state involved in performing a match resides in the
 * matcher, so many matchers can share the same pattern.
 *
 * <p> A typical invocation sequence is thus
 *
 * <blockquote><pre>
 * Pattern p = Pattern.{@link #compile compile}("a*b");
 * Matcher m = p.{@link #matcher matcher}("aaaaab");
 * boolean b = m.{@link Matcher#matches matches}();</pre></blockquote>
 *
 * <p> A {@link #matches matches} method is defined by this class as a
 * convenience for when a regular expression is used just once.  This method
 * compiles an expression and matches an input sequence against it in a single
 * invocation.  The statement
 *
 * <blockquote><pre>
 * boolean b = Pattern.matches("a*b", "aaaaab");</pre></blockquote>
 *
 * is equivalent to the three statements above, though for repeated matches it
 * is less efficient since it does not allow the compiled pattern to be reused.
 *
 * <p> Instances of this class are immutable and are safe for use by multiple
 * concurrent threads.  Instances of the {@link Matcher} class are not safe for
 * such use.
 *
 *
 * <h2><a id="sum">Summary of regular-expression constructs</a></h2>
 *
 * <table class="borderless">
 * <caption style="display:none">Regular expression constructs, and what they match</caption>
 * <thead style="text-align:left">
 * <tr>
 * <th id="construct">Construct</th>
 * <th id="matches">Matches</th>
 * </tr>
 * </thead>
 * <tbody style="text-align:left">
 *
 * <tr><th colspan="2" style="padding-top:20px" id="characters">Characters</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight: normal" id="x"><i>x</i></th>
 *     <td headers="matches characters x">The character <i>x</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="backslash">{@code \\}</th>
 *     <td headers="matches characters backslash">The backslash character</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_n">{@code \0}<i>n</i></th>
 *     <td headers="matches characters octal_n">The character with octal value {@code 0}<i>n</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_nn">{@code \0}<i>nn</i></th>
 *     <td headers="matches characters octal_nn">The character with octal value {@code 0}<i>nn</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_nnn">{@code \0}<i>mnn</i></th>
 *     <td headers="matches characters octal_nnn">The character with octal value {@code 0}<i>mnn</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>m</i>&nbsp;{@code <=}&nbsp;3,
 *         0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_hh">{@code \x}<i>hh</i></th>
 *     <td headers="matches characters hex_hh">The character with hexadecimal value {@code 0x}<i>hh</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_hhhh"><code>&#92;u</code><i>hhhh</i></th>
 *     <td headers="matches characters hex_hhhh">The character with hexadecimal&nbsp;value&nbsp;{@code 0x}<i>hhhh</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_h_h"><code>&#92;x</code><i>{h...h}</i></th>
 *     <td headers="matches characters hex_h_h">The character with hexadecimal value {@code 0x}<i>h...h</i>
 *         ({@link java.lang.Character#MIN_CODE_POINT Character.MIN_CODE_POINT}
 *         &nbsp;&lt;=&nbsp;{@code 0x}<i>h...h</i>&nbsp;&lt;=&nbsp;
 *          {@link java.lang.Character#MAX_CODE_POINT Character.MAX_CODE_POINT})</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="unicode_name"><code>&#92;N{</code><i>name</i><code>}</code></th>
 *     <td headers="matches characters unicode_name">The character with Unicode character name <i>'name'</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="tab">{@code \t}</th>
 *     <td headers="matches characters tab">The tab character (<code>'&#92;u0009'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="newline">{@code \n}</th>
 *     <td headers="matches characters newline">The newline (line feed) character (<code>'&#92;u000A'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="return">{@code \r}</th>
 *     <td headers="matches characters return">The carriage-return character (<code>'&#92;u000D'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="form_feed">{@code \f}</th>
 *     <td headers="matches characters form_feed">The form-feed character (<code>'&#92;u000C'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="bell">{@code \a}</th>
 *     <td headers="matches characters bell">The alert (bell) character (<code>'&#92;u0007'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="escape">{@code \e}</th>
 *     <td headers="matches characters escape">The escape character (<code>'&#92;u001B'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="ctrl_x">{@code \c}<i>x</i></th>
 *     <td headers="matches characters ctrl_x">The control character corresponding to <i>x</i></td></tr>
 *
 *  <tr><th colspan="2" style="padding-top:20px" id="classes">Character classes</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="simple">{@code [abc]}</th>
 *     <td headers="matches classes simple">{@code a}, {@code b}, or {@code c} (simple class)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="negation">{@code [^abc]}</th>
 *     <td headers="matches classes negation">Any character except {@code a}, {@code b}, or {@code c} (negation)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="range">{@code [a-zA-Z]}</th>
 *     <td headers="matches classes range">{@code a} through {@code z}
 *         or {@code A} through {@code Z}, inclusive (range)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="union">{@code [a-d[m-p]]}</th>
 *     <td headers="matches classes union">{@code a} through {@code d},
 *      or {@code m} through {@code p}: {@code [a-dm-p]} (union)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="intersection">{@code [a-z&&[def]]}</th>
 *     <td headers="matches classes intersection">{@code d}, {@code e}, or {@code f} (intersection)</tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="subtraction1">{@code [a-z&&[^bc]]}</th>
 *     <td headers="matches classes subtraction1">{@code a} through {@code z},
 *         except for {@code b} and {@code c}: {@code [ad-z]} (subtraction)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="subtraction2">{@code [a-z&&[^m-p]]}</th>
 *     <td headers="matches classes subtraction2">{@code a} through {@code z},
 *          and not {@code m} through {@code p}: {@code [a-lq-z]}(subtraction)</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="predef">Predefined character classes</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="any">{@code .}</th>
 *     <td headers="matches predef any">Any character (may or may not match <a href="#lt">line terminators</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="digit">{@code \d}</th>
 *     <td headers="matches predef digit">A digit: {@code [0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_digit">{@code \D}</th>
 *     <td headers="matches predef non_digit">A non-digit: {@code [^0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="horiz_white">{@code \h}</th>
 *     <td headers="matches predef horiz_white">A horizontal whitespace character:
 *     <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_horiz_white">{@code \H}</th>
 *     <td headers="matches predef non_horiz_white">A non-horizontal whitespace character: {@code [^\h]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="white">{@code \s}</th>
 *     <td headers="matches predef white">A whitespace character: {@code [ \t\n\x0B\f\r]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_white">{@code \S}</th>
 *     <td headers="matches predef non_white">A non-whitespace character: {@code [^\s]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="vert_white">{@code \v}</th>
 *     <td headers="matches predef vert_white">A vertical whitespace character: <code>[\n\x0B\f\r\x85&#92;u2028&#92;u2029]</code>
 *     </td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_vert_white">{@code \V}</th>
 *     <td headers="matches predef non_vert_white">A non-vertical whitespace character: {@code [^\v]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="word">{@code \w}</th>
 *     <td headers="matches predef word">A word character: {@code [a-zA-Z_0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_word">{@code \W}</th>
 *     <td headers="matches predef non_word">A non-word character: {@code [^\w]}</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="posix"><b>POSIX character classes (US-ASCII only)</b></th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="Lower">{@code \p{Lower}}</th>
 *     <td headers="matches posix Lower">A lower-case alphabetic character: {@code [a-z]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Upper">{@code \p{Upper}}</th>
 *     <td headers="matches posix Upper">An upper-case alphabetic character:{@code [A-Z]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="ASCII">{@code \p{ASCII}}</th>
 *     <td headers="matches posix ASCII">All ASCII:{@code [\x00-\x7F]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Alpha">{@code \p{Alpha}}</th>
 *     <td headers="matches posix Alpha">An alphabetic character:{@code [\p{Lower}\p{Upper}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Digit">{@code \p{Digit}}</th>
 *     <td headers="matches posix Digit">A decimal digit: {@code [0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Alnum">{@code \p{Alnum}}</th>
 *     <td headers="matches posix Alnum">An alphanumeric character:{@code [\p{Alpha}\p{Digit}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Punct">{@code \p{Punct}}</th>
 *     <td headers="matches posix Punct">Punctuation: One of {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~}</td></tr>
 *     <!-- {@code [\!"#\$%&'\(\)\*\+,\-\./:;\<=\>\?@\[\\\]\^_`\{\|\}~]}
 *          {@code [\X21-\X2F\X31-\X40\X5B-\X60\X7B-\X7E]} -->
 * <tr><th style="vertical-align:top; font-weight:normal" id="Graph">{@code \p{Graph}}</th>
 *     <td headers="matches posix Graph">A visible character: {@code [\p{Alnum}\p{Punct}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Print">{@code \p{Print}}</th>
 *     <td headers="matches posix Print">A printable character: {@code [\p{Graph}\x20]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Blank">{@code \p{Blank}}</th>
 *     <td headers="matches posix Blank">A space or a tab: {@code [ \t]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Cntrl">{@code \p{Cntrl}}</th>
 *     <td headers="matches posix Cntrl">A control character: {@code [\x00-\x1F\x7F]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="XDigit">{@code \p{XDigit}}</th>
 *     <td headers="matches posix XDigit">A hexadecimal digit: {@code [0-9a-fA-F]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Space">{@code \p{Space}}</th>
 *     <td headers="matches posix Space">A whitespace character: {@code [ \t\n\x0B\f\r]}</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="java">java.lang.Character classes (simple <a href="#jcc">java character type</a>)</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaLowerCase">{@code \p{javaLowerCase}}</th>
 *     <td headers="matches java javaLowerCase">Equivalent to java.lang.Character.isLowerCase()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaUpperCase">{@code \p{javaUpperCase}}</th>
 *     <td headers="matches java javaUpperCase">Equivalent to java.lang.Character.isUpperCase()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaWhitespace">{@code \p{javaWhitespace}}</th>
 *     <td headers="matches java javaWhitespace">Equivalent to java.lang.Character.isWhitespace()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaMirrored">{@code \p{javaMirrored}}</th>
 *     <td headers="matches java javaMirrored">Equivalent to java.lang.Character.isMirrored()</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px"  id="unicode">Classes for Unicode scripts, blocks, categories and binary properties</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="IsLatin">{@code \p{IsLatin}}</th>
 *     <td headers="matches unicode IsLatin">A Latin&nbsp;script character (<a href="#usc">script</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="InGreek">{@code \p{InGreek}}</th>
 *     <td headers="matches unicode InGreek">A character in the Greek&nbsp;block (<a href="#ubc">block</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Lu">{@code \p{Lu}}</th>
 *     <td headers="matches unicode Lu">An uppercase letter (<a href="#ucc">category</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="IsAlphabetic">{@code \p{IsAlphabetic}}</th>
 *     <td headers="matches unicode IsAlphabetic">An alphabetic character (<a href="#ubpc">binary property</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Sc">{@code \p{Sc}}</th>
 *     <td headers="matches unicode Sc">A currency symbol</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="not_InGreek">{@code \P{InGreek}}</th>
 *     <td headers="matches unicode not_InGreek">Any character except one in the Greek block (negation)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="not_uppercase">{@code [\p{L}&&[^\p{Lu}]]}</th>
 *     <td headers="matches unicode not_uppercase">Any letter except an uppercase letter (subtraction)</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="bounds">Boundary matchers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="begin_line">{@code ^}</th>
 *     <td headers="matches bounds begin_line">The beginning of a line</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_line">{@code $}</th>
 *     <td headers="matches bounds end_line">The end of a line</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="word_boundary">{@code \b}</th>
 *     <td headers="matches bounds word_boundary">A word boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="grapheme_cluster_boundary">{@code \b{g}}</th>
 *     <td headers="matches bounds grapheme_cluster_boundary">A Unicode extended grapheme cluster boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_word_boundary">{@code \B}</th>
 *     <td headers="matches bounds non_word_boundary">A non-word boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="begin_input">{@code \A}</th>
 *     <td headers="matches bounds begin_input">The beginning of the input</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_prev_match">{@code \G}</th>
 *     <td headers="matches bounds end_prev_match">The end of the previous match</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_input_except_term">{@code \Z}</th>
 *     <td headers="matches bounds end_input_except_term">The end of the input but for the final
 *         <a href="#lt">terminator</a>, if&nbsp;any</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_input">{@code \z}</th>
 *     <td headers="matches bounds end_input">The end of the input</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="linebreak">Linebreak matcher</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="any_unicode_linebreak">{@code \R}</th>
 *     <td headers="matches linebreak any_unicode_linebreak">Any Unicode linebreak sequence, is equivalent to
 *     <code>&#92;u000D&#92;u000A|[&#92;u000A&#92;u000B&#92;u000C&#92;u000D&#92;u0085&#92;u2028&#92;u2029]
 *     </code></td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="grapheme">Unicode Extended Grapheme matcher</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="grapheme_any">{@code \X}</th>
 *     <td headers="matches grapheme grapheme_any">Any Unicode extended grapheme cluster</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="greedy">Greedy quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_once_or_not"><i>X</i>{@code ?}</th>
 *     <td headers="matches greedy greedy_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_zero_or_more"><i>X</i>{@code *}</th>
 *     <td headers="matches greedy greedy_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_one_or_more"><i>X</i>{@code +}</th>
 *     <td headers="matches greedy greedy_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_exactly"><i>X</i><code>{</code><i>n</i><code>}</code></th>
 *     <td headers="matches greedy greedy_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_at_least"><i>X</i><code>{</code><i>n</i>{@code ,}}</th>
 *     <td headers="matches greedy greedy_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}</code></th>
 *     <td headers="matches greedy greedy_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="reluc">Reluctant quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_once_or_not"><i>X</i>{@code ??}</th>
 *     <td headers="matches reluc reluc_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_zero_or_more"><i>X</i>{@code *?}</th>
 *     <td headers="matches reluc reluc_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_one_or_more"><i>X</i>{@code +?}</th>
 *     <td headers="matches reluc reluc_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_exactly"><i>X</i><code>{</code><i>n</i><code>}?</code></th>
 *     <td headers="matches reluc reluc_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_at_least"><i>X</i><code>{</code><i>n</i><code>,}?</code></th>
 *     <td headers="matches reluc reluc_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}?</code></th>
 *     <td headers="matches reluc reluc_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="poss">Possessive quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_once_or_not"><i>X</i>{@code ?+}</th>
 *     <td headers="matches poss poss_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_zero_or_more"><i>X</i>{@code *+}</th>
 *     <td headers="matches poss poss_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_one_or_more"><i>X</i>{@code ++}</th>
 *     <td headers="matches poss poss_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_exactly"><i>X</i><code>{</code><i>n</i><code>}+</code></th>
 *     <td headers="matches poss poss_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_at_least"><i>X</i><code>{</code><i>n</i><code>,}+</code></th>
 *     <td headers="matches poss poss_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}+</code></th>
 *     <td headers="matches poss poss_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="logical">Logical operators</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="concat"><i>XY</i></th>
 *     <td headers="matches logical concat"><i>X</i> followed by <i>Y</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="alternate"><i>X</i>{@code |}<i>Y</i></th>
 *     <td headers="matches logical alternate">Either <i>X</i> or <i>Y</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="group">{@code (}<i>X</i>{@code )}</th>
 *     <td headers="matches logical group">X, as a <a href="#cg">capturing group</a></td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="backref">Back references</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="back_nth">{@code \}<i>n</i></th>
 *     <td headers="matches backref back_nth">Whatever the <i>n</i><sup>th</sup>
 *     <a href="#cg">capturing group</a> matched</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="back_named">{@code \}<i>k</i>&lt;<i>name</i>&gt;</th>
 *     <td headers="matches backref back_named">Whatever the
 *     <a href="#groupname">named-capturing group</a> "name" matched</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="quote">Quotation</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_follow">{@code \}</th>
 *     <td headers="matches quote quote_follow">Nothing, but quotes the following character</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_begin">{@code \Q}</th>
 *     <td headers="matches quote quote_begin">Nothing, but quotes all characters until {@code \E}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_end">{@code \E}</th>
 *     <td headers="matches quote quote_end">Nothing, but ends quoting started by {@code \Q}</td></tr>
 *     <!-- Metachars: !$()*+.<>?[\]^{|} -->
 *
 * <tr><th colspan="2" style="padding-top:20px" id="special">Special constructs (named-capturing and non-capturing)</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="named_group"><code>(?&lt;<a href="#groupname">name</a>&gt;</code><i>X</i>{@code )}</th>
 *     <td headers="matches special named_group"><i>X</i>, as a named-capturing group</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_capture_group">{@code (?:}<i>X</i>{@code )}</th>
 *     <td headers="matches special non_capture_group"><i>X</i>, as a non-capturing group</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="flags"><code>(?idmsuxU-idmsuxU)&nbsp;</code></th>
 *     <td headers="matches special flags">Nothing, but turns match flags <a href="#CASE_INSENSITIVE">i</a>
 * <a href="#UNIX_LINES">d</a> <a href="#MULTILINE">m</a> <a href="#DOTALL">s</a>
 * <a href="#UNICODE_CASE">u</a> <a href="#COMMENTS">x</a> <a href="#UNICODE_CHARACTER_CLASS">U</a>
 * on - off</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_capture_group_flags">{@code (?idmsuxU-idmsuxU:}<i>X</i>{@code )}&nbsp;&nbsp;</th>
 *     <td headers="matches special non_capture_group_flags"><i>X</i>, as a <a href="#cg">non-capturing group</a> with the
 *         given flags <a href="#CASE_INSENSITIVE">i</a> <a href="#UNIX_LINES">d</a>
 * <a href="#MULTILINE">m</a> <a href="#DOTALL">s</a> <a href="#UNICODE_CASE">u</a >
 * <a href="#COMMENTS">x</a> <a href="#UNICODE_CHARACTER_CLASS">U</a> on - off</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="pos_lookahead">{@code (?=}<i>X</i>{@code )}</th>
 *     <td headers="matches special pos_lookahead"><i>X</i>, via zero-width positive lookahead</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="neg_lookahead">{@code (?!}<i>X</i>{@code )}</th>
 *     <td headers="matches special neg_lookahead"><i>X</i>, via zero-width negative lookahead</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="pos_lookbehind">{@code (?<=}<i>X</i>{@code )}</th>
 *     <td headers="matches special pos_lookbehind"><i>X</i>, via zero-width positive lookbehind</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="neg_lookbehind">{@code (?<!}<i>X</i>{@code )}</th>
 *     <td headers="matches special neg_lookbehind"><i>X</i>, via zero-width negative lookbehind</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="indep_non_capture_group">{@code (?>}<i>X</i>{@code )}</th>
 *     <td headers="matches special indep_non_capture_group"><i>X</i>, as an independent, non-capturing group</td></tr>
 *
 * </tbody>
 * </table>
 *
 * <hr>
 *
 *
 * <h2><a id="bs">Backslashes, escapes, and quoting</a></h2>
 *
 * <p> The backslash character ({@code '\'}) serves to introduce escaped
 * constructs, as defined in the table above, as well as to quote characters
 * that otherwise would be interpreted as unescaped constructs.  Thus the
 * expression {@code \\} matches a single backslash and <code>\{</code> matches a
 * left brace.
 *
 * <p> It is an error to use a backslash prior to any alphabetic character that
 * does not denote an escaped construct; these are reserved for future
 * extensions to the regular-expression language.  A backslash may be used
 * prior to a non-alphabetic character regardless of whether that character is
 * part of an unescaped construct.
 *
 * <p> Backslashes within string literals in Java source code are interpreted
 * as required by
 * <cite>The Java Language Specification</cite>
 * as either Unicode escapes (section {@jls 3.3}) or other character escapes (section {@jls 3.10.6})
 * It is therefore necessary to double backslashes in string
 * literals that represent regular expressions to protect them from
 * interpretation by the Java bytecode compiler.  The string literal
 * <code>"&#92;b"</code>, for example, matches a single backspace character when
 * interpreted as a regular expression, while {@code "\\b"} matches a
 * word boundary.  The string literal {@code "\(hello\)"} is illegal
 * and leads to a compile-time error; in order to match the string
 * {@code (hello)} the string literal {@code "\\(hello\\)"}
 * must be used.
 *
 * <h2><a id="cc">Character Classes</a></h2>
 *
 *    <p> Character classes may appear within other character classes, and
 *    may be composed by the union operator (implicit) and the intersection
 *    operator ({@code &&}).
 *    The union operator denotes a class that contains every character that is
 *    in at least one of its operand classes.  The intersection operator
 *    denotes a class that contains every character that is in both of its
 *    operand classes.
 *
 *    <p> The precedence of character-class operators is as follows, from
 *    highest to lowest:
 *
 *    <table class="striped" style="margin-left: 2em;">
 *      <caption style="display:none">Precedence of character class operators.</caption>
 *      <thead>
 *      <tr><th scope="col">Precedence<th scope="col">Name<th scope="col">Example
 *      </thead>
 *      <tbody>
 *      <tr><th scope="row">1</th>
 *        <td>Literal escape&nbsp;&nbsp;&nbsp;&nbsp;</td>
 *        <td>{@code \x}</td></tr>
 *     <tr><th scope="row">2</th>
 *        <td>Grouping</td>
 *        <td>{@code [...]}</td></tr>
 *     <tr><th scope="row">3</th>
 *        <td>Range</td>
 *        <td>{@code a-z}</td></tr>
 *      <tr><th scope="row">4</th>
 *        <td>Union</td>
 *        <td>{@code [a-e][i-u]}</td></tr>
 *      <tr><th scope="row">5</th>
 *        <td>Intersection</td>
 *        <td>{@code [a-z&&[aeiou]]}</td></tr>
 *      </tbody>
 *    </table>
 *
 *    <p> Note that a different set of metacharacters are in effect inside
 *    a character class than outside a character class. For instance, the
 *    regular expression {@code .} loses its special meaning inside a
 *    character class, while the expression {@code -} becomes a range
 *    forming metacharacter.
 *
 * <h2><a id="lt">Line terminators</a></h2>
 *
 * <p> A <i>line terminator</i> is a one- or two-character sequence that marks
 * the end of a line of the input character sequence.  The following are
 * recognized as line terminators:
 *
 * <ul>
 *
 *   <li> A newline (line feed) character ({@code '\n'}),
 *
 *   <li> A carriage-return character followed immediately by a newline
 *   character ({@code "\r\n"}),
 *
 *   <li> A standalone carriage-return character ({@code '\r'}),
 *
 *   <li> A next-line character (<code>'&#92;u0085'</code>),
 *
 *   <li> A line-separator character (<code>'&#92;u2028'</code>), or
 *
 *   <li> A paragraph-separator character (<code>'&#92;u2029'</code>).
 *
 * </ul>
 * <p>If {@link #UNIX_LINES} mode is activated, then the only line terminators
 * recognized are newline characters.
 *
 * <p> The regular expression {@code .} matches any character except a line
 * terminator unless the {@link #DOTALL} flag is specified.
 *
 * <p> By default, the regular expressions {@code ^} and {@code $} ignore
 * line terminators and only match at the beginning and the end, respectively,
 * of the entire input sequence. If {@link #MULTILINE} mode is activated then
 * {@code ^} matches at the beginning of input and after any line terminator
 * except at the end of input. When in {@link #MULTILINE} mode {@code $}
 * matches just before a line terminator or the end of the input sequence.
 *
 * <h2><a id="cg">Groups and capturing</a></h2>
 *
 * <h3><a id="gnumber">Group number</a></h3>
 * <p> Capturing groups are numbered by counting their opening parentheses from
 * left to right.  In the expression {@code ((A)(B(C)))}, for example, there
 * are four such groups: </p>
 *
 * <ol style="margin-left:2em;">
 *   <li> {@code ((A)(B(C)))}
 *   <li> {@code (A)}
 *   <li> {@code (B(C))}
 *   <li> {@code (C)}
 * </ol>
 *
 * <p> Group zero always stands for the entire expression.
 *
 * <p> Capturing groups are so named because, during a match, each subsequence
 * of the input sequence that matches such a group is saved.  The captured
 * subsequence may be used later in the expression, via a back reference, and
 * may also be retrieved from the matcher once the match operation is complete.
 *
 * <h3><a id="groupname">Group name</a></h3>
 * <p>A capturing group can also be assigned a "name", a {@code named-capturing group},
 * and then be back-referenced later by the "name". Group names are composed of
 * the following characters. The first character must be a {@code letter}.
 *
 * <ul>
 *   <li> The uppercase letters {@code 'A'} through {@code 'Z'}
 *        (<code>'&#92;u0041'</code>&nbsp;through&nbsp;<code>'&#92;u005a'</code>),
 *   <li> The lowercase letters {@code 'a'} through {@code 'z'}
 *        (<code>'&#92;u0061'</code>&nbsp;through&nbsp;<code>'&#92;u007a'</code>),
 *   <li> The digits {@code '0'} through {@code '9'}
 *        (<code>'&#92;u0030'</code>&nbsp;through&nbsp;<code>'&#92;u0039'</code>),
 * </ul>
 *
 * <p> A {@code named-capturing group} is still numbered as described in
 * <a href="#gnumber">Group number</a>.
 *
 * <p> The captured input associated with a group is always the subsequence
 * that the group most recently matched.  If a group is evaluated a second time
 * because of quantification then its previously-captured value, if any, will
 * be retained if the second evaluation fails.  Matching the string
 * {@code "aba"} against the expression {@code (a(b)?)+}, for example, leaves
 * group two set to {@code "b"}.  All captured input is discarded at the
 * beginning of each match.
 *
 * <p> Groups beginning with {@code (?} are either pure, <i>non-capturing</i> groups
 * that do not capture text and do not count towards the group total, or
 * <i>named-capturing</i> group.
 *
 * <h2> Unicode support </h2>
 *
 * <p> This class is in conformance with Level 1 of <a
 * href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
 * Standard #18: Unicode Regular Expressions</i></a>, plus RL2.1
 * Canonical Equivalents and RL2.2 Extended Grapheme Clusters.
 * <p>
 * <b>Unicode escape sequences</b> such as <code>&#92;u2014</code> in Java source code
 * are processed as described in section {@jls 3.3} of
 * <cite>The Java Language Specification</cite>.
 * Such escape sequences are also implemented directly by the regular-expression
 * parser so that Unicode escapes can be used in expressions that are read from
 * files or from the keyboard.  Thus the strings <code>"&#92;u2014"</code> and
 * {@code "\\u2014"}, while not equal, compile into the same pattern, which
 * matches the character with hexadecimal value {@code 0x2014}.
 * <p>
 * A Unicode character can also be represented by using its <b>Hex notation</b>
 * (hexadecimal code point value) directly as described in construct
 * <code>&#92;x{...}</code>, for example a supplementary character U+2011F can be
 * specified as <code>&#92;x{2011F}</code>, instead of two consecutive Unicode escape
 * sequences of the surrogate pair <code>&#92;uD840</code><code>&#92;uDD1F</code>.
 * <p>
 * <b>Unicode character names</b> are supported by the named character construct
 * <code>\N{</code>...<code>}</code>, for example, <code>\N{WHITE SMILING FACE}</code>
 * specifies character <code>&#92;u263A</code>. The character names supported
 * by this class are the valid Unicode character names matched by
 * {@link java.lang.Character#codePointOf(String) Character.codePointOf(name)}.
 * <p>
 * <a href="http://www.unicode.org/reports/tr18/#Default_Grapheme_Clusters">
 * <b>Unicode extended grapheme clusters</b></a> are supported by the grapheme
 * cluster matcher {@code \X} and the corresponding boundary matcher {@code \b{g}}.
 * <p>
 * Unicode scripts, blocks, categories and binary properties are written with
 * the {@code \p} and {@code \P} constructs as in Perl.
 * <code>\p{</code><i>prop</i><code>}</code> matches if
 * the input has the property <i>prop</i>, while <code>\P{</code><i>prop</i><code>}</code>
 * does not match if the input has that property.
 * <p>
 * Scripts, blocks, categories and binary properties can be used both inside
 * and outside of a character class.
 *
 * <p>
 * <b><a id="usc">Scripts</a></b> are specified either with the prefix {@code Is}, as in
 * {@code IsHiragana}, or by using  the {@code script} keyword (or its short
 * form {@code sc}) as in {@code script=Hiragana} or {@code sc=Hiragana}.
 * <p>
 * The script names supported by {@code Pattern} are the valid script names
 * accepted and defined by
 * {@link java.lang.Character.UnicodeScript#forName(String) UnicodeScript.forName}.
 *
 * <p>
 * <b><a id="ubc">Blocks</a></b> are specified with the prefix {@code In}, as in
 * {@code InMongolian}, or by using the keyword {@code block} (or its short
 * form {@code blk}) as in {@code block=Mongolian} or {@code blk=Mongolian}.
 * <p>
 * The block names supported by {@code Pattern} are the valid block names
 * accepted and defined by
 * {@link java.lang.Character.UnicodeBlock#forName(String) UnicodeBlock.forName}.
 * <p>
 *
 * <b><a id="ucc">Categories</a></b> may be specified with the optional prefix {@code Is}:
 * Both {@code \p{L}} and {@code \p{IsL}} denote the category of Unicode
 * letters. Same as scripts and blocks, categories can also be specified
 * by using the keyword {@code general_category} (or its short form
 * {@code gc}) as in {@code general_category=Lu} or {@code gc=Lu}.
 * <p>
 * The supported categories are those of
 * <a href="http://www.unicode.org/standard/standard.html">
 * <i>The Unicode Standard</i></a> in the version specified by the
 * {@link java.lang.Character Character} class. The category names are those
 * defined in the Standard, both normative and informative.
 * <p>
 *
 * <b><a id="ubpc">Binary properties</a></b> are specified with the prefix {@code Is}, as in
 * {@code IsAlphabetic}. The supported binary properties by {@code Pattern}
 * are
 * <ul>
 *   <li> Alphabetic
 *   <li> Ideographic
 *   <li> Letter
 *   <li> Lowercase
 *   <li> Uppercase
 *   <li> Titlecase
 *   <li> Punctuation
 *   <Li> Control
 *   <li> White_Space
 *   <li> Digit
 *   <li> Hex_Digit
 *   <li> Join_Control
 *   <li> Noncharacter_Code_Point
 *   <li> Assigned
 * </ul>
 * <p>
 * The following <b>Predefined Character classes</b> and <b>POSIX character classes</b>
 * are in conformance with the recommendation of <i>Annex C: Compatibility Properties</i>
 * of <a href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical Standard #18:
 * Unicode Regular Expressions</i></a>, when {@link #UNICODE_CHARACTER_CLASS} flag is specified.
 *
 * <table class="striped">
 * <caption style="display:none">predefined and posix character classes in Unicode mode</caption>
 * <thead>
 * <tr>
 * <th scope="col" id="predef_classes">Classes</th>
 * <th scope="col" id="predef_matches">Matches</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row">{@code \p{Lower}}</th>
 *     <td>A lowercase character:{@code \p{IsLowercase}}</td></tr>
 * <tr><th scope="row">{@code \p{Upper}}</th>
 *     <td>An uppercase character:{@code \p{IsUppercase}}</td></tr>
 * <tr><th scope="row">{@code \p{ASCII}}</th>
 *     <td>All ASCII:{@code [\x00-\x7F]}</td></tr>
 * <tr><th scope="row">{@code \p{Alpha}}</th>
 *     <td>An alphabetic character:{@code \p{IsAlphabetic}}</td></tr>
 * <tr><th scope="row">{@code \p{Digit}}</th>
 *     <td>A decimal digit character:{@code \p{IsDigit}}</td></tr>
 * <tr><th scope="row">{@code \p{Alnum}}</th>
 *     <td>An alphanumeric character:{@code [\p{IsAlphabetic}\p{IsDigit}]}</td></tr>
 * <tr><th scope="row">{@code \p{Punct}}</th>
 *     <td>A punctuation character:{@code \p{IsPunctuation}}</td></tr>
 * <tr><th scope="row">{@code \p{Graph}}</th>
 *     <td>A visible character: {@code [^\p{IsWhite_Space}\p{gc=Cc}\p{gc=Cs}\p{gc=Cn}]}</td></tr>
 * <tr><th scope="row">{@code \p{Print}}</th>
 *     <td>A printable character: {@code [\p{Graph}\p{Blank}&&[^\p{Cntrl}]]}</td></tr>
 * <tr><th scope="row">{@code \p{Blank}}</th>
 *     <td>A space or a tab: {@code [\p{IsWhite_Space}&&[^\p{gc=Zl}\p{gc=Zp}\x0a\x0b\x0c\x0d\x85]]}</td></tr>
 * <tr><th scope="row">{@code \p{Cntrl}}</th>
 *     <td>A control character: {@code \p{gc=Cc}}</td></tr>
 * <tr><th scope="row">{@code \p{XDigit}}</th>
 *     <td>A hexadecimal digit: {@code [\p{gc=Nd}\p{IsHex_Digit}]}</td></tr>
 * <tr><th scope="row">{@code \p{Space}}</th>
 *     <td>A whitespace character:{@code \p{IsWhite_Space}}</td></tr>
 * <tr><th scope="row">{@code \d}</th>
 *     <td>A digit: {@code \p{IsDigit}}</td></tr>
 * <tr><th scope="row">{@code \D}</th>
 *     <td>A non-digit: {@code [^\d]}</td></tr>
 * <tr><th scope="row">{@code \s}</th>
 *     <td>A whitespace character: {@code \p{IsWhite_Space}}</td></tr>
 * <tr><th scope="row">{@code \S}</th>
 *     <td>A non-whitespace character: {@code [^\s]}</td></tr>
 * <tr><th scope="row">{@code \w}</th>
 *     <td>A word character: {@code [\p{Alpha}\p{gc=Mn}\p{gc=Me}\p{gc=Mc}\p{Digit}\p{gc=Pc}\p{IsJoin_Control}]}</td></tr>
 * <tr><th scope="row">{@code \W}</th>
 *     <td>A non-word character: {@code [^\w]}</td></tr>
 * </tbody>
 * </table>
 * <p>
 * <a id="jcc">
 * Categories that behave like the java.lang.Character
 * boolean is<i>methodname</i> methods (except for the deprecated ones) are
 * available through the same <code>\p{</code><i>prop</i><code>}</code> syntax where
 * the specified property has the name <code>java<i>methodname</i></code></a>.
 *
 * <h2> Comparison to Perl 5 </h2>
 *
 * <p>The {@code Pattern} engine performs traditional NFA-based matching
 * with ordered alternation as occurs in Perl 5.
 *
 * <p> Perl constructs not supported by this class: </p>
 *
 * <ul>
 *    <li><p> The backreference constructs, <code>\g{</code><i>n</i><code>}</code> for
 *    the <i>n</i><sup>th</sup><a href="#cg">capturing group</a> and
 *    <code>\g{</code><i>name</i><code>}</code> for
 *    <a href="#groupname">named-capturing group</a>.
 *    </p></li>
 *
 *    <li><p> The conditional constructs
 *    {@code (?(}<i>condition</i>{@code )}<i>X</i>{@code )} and
 *    {@code (?(}<i>condition</i>{@code )}<i>X</i>{@code |}<i>Y</i>{@code )},
 *    </p></li>
 *
 *    <li><p> The embedded code constructs <code>(?{</code><i>code</i><code>})</code>
 *    and <code>(??{</code><i>code</i><code>})</code>,</p></li>
 *
 *    <li><p> The embedded comment syntax {@code (?#comment)}, and </p></li>
 *
 *    <li><p> The preprocessing operations {@code \l} <code>&#92;u</code>,
 *    {@code \L}, and {@code \U}.  </p></li>
 *
 * </ul>
 *
 * <p> Constructs supported by this class but not by Perl: </p>
 *
 * <ul>
 *
 *    <li><p> Character-class union and intersection as described
 *    <a href="#cc">above</a>.</p></li>
 *
 * </ul>
 *
 * <p> Notable differences from Perl: </p>
 *
 * <ul>
 *
 *    <li><p> In Perl, {@code \1} through {@code \9} are always interpreted
 *    as back references; a backslash-escaped number greater than {@code 9} is
 *    treated as a back reference if at least that many subexpressions exist,
 *    otherwise it is interpreted, if possible, as an octal escape.  In this
 *    class octal escapes must always begin with a zero. In this class,
 *    {@code \1} through {@code \9} are always interpreted as back
 *    references, and a larger number is accepted as a back reference if at
 *    least that many subexpressions exist at that point in the regular
 *    expression, otherwise the parser will drop digits until the number is
 *    smaller or equal to the existing number of groups or it is one digit.
 *    </p></li>
 *
 *    <li><p> Perl uses the {@code g} flag to request a match that resumes
 *    where the last match left off.  This functionality is provided implicitly
 *    by the {@link Matcher} class: Repeated invocations of the {@link
 *    Matcher#find find} method will resume where the last match left off,
 *    unless the matcher is reset.  </p></li>
 *
 *    <li><p> In Perl, embedded flags at the top level of an expression affect
 *    the whole expression.  In this class, embedded flags always take effect
 *    at the point at which they appear, whether they are at the top level or
 *    within a group; in the latter case, flags are restored at the end of the
 *    group just as in Perl.  </p></li>
 *
 *    <li><p><i>Free-spacing mode</i> in Perl (called <i>comments
 *    mode</i> in this class) denoted by {@code (?x)} in the regular
 *    expression (or by the {@link Pattern#COMMENTS} flag when compiling
 *    the expression) will not ignore whitespace inside of character classes. In
 *    this class, whitespace inside of character classes must be escaped to be
 *    considered as part of the regular expression when in comments mode.
 *    </p></li>
 *
 * </ul>
 *
 *
 * <p> For a more precise description of the behavior of regular expression
 * constructs, please see <a href="http://www.oreilly.com/catalog/regex3/">
 * <i>Mastering Regular Expressions, 3rd Edition</i>, Jeffrey E. F. Friedl,
 * O'Reilly and Associates, 2006.</a>
 * </p>
 *
 * @see java.lang.String#split(String, int)
 * @see java.lang.String#split(String)
 *
 * @author      Mike McCloskey
 * @author      Mark Reinhold
 * @author      JSR-51 Expert Group
 * @since       1.4
 */

public final class Pattern
    implements java.io.Serializable
{

    /*
     * Regular expression modifier values.  Instead of being passed as
     * arguments, they can also be passed as inline modifiers.
     * For example, the following statements have the same effect.
     *
     *   Pattern p1 = Pattern.compile("abc", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
     *   Pattern p2 = Pattern.compile("(?im)abc", 0);
     */

    /**
     * Enables Unix lines mode.
     *
     * <p> In this mode, only the {@code '\n'} line terminator is recognized
     * in the behavior of {@code .}, {@code ^}, and {@code $}.
     *
     * <p> Unix lines mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?d)}.
     */
    public static final int UNIX_LINES = 0x01;

    /**
     * Enables case-insensitive matching.
     *
     * <p> By default, case-insensitive matching assumes that only characters
     * in the US-ASCII charset are being matched.  Unicode-aware
     * case-insensitive matching can be enabled by specifying the {@link
     * #UNICODE_CASE} flag in conjunction with this flag.
     *
     * <p> Case-insensitive matching can also be enabled via the embedded flag
     * expression&nbsp;{@code (?i)}.
     *
     * <p> Specifying this flag may impose a slight performance penalty.  </p>
     */
    public static final int CASE_INSENSITIVE = 0x02;

    /**
     * Permits whitespace and comments in pattern.
     *
     * <p> In this mode, whitespace is ignored, and embedded comments starting
     * with {@code #} are ignored until the end of a line. Comments mode ignores
     * whitespace within a character class contained in a pattern string. Such
     * whitespace must be escaped in order to be considered significant.  </p>
     *
     * <p> Comments mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?x)}.
     */
    public static final int COMMENTS = 0x04;

    /**
     * Enables multiline mode.
     *
     * <p> In multiline mode the expressions {@code ^} and {@code $} match
     * just after or just before, respectively, a line terminator or the end of
     * the input sequence.  By default these expressions only match at the
     * beginning and the end of the entire input sequence.
     *
     * <p> Multiline mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?m)}.  </p>
     */
    public static final int MULTILINE = 0x08;

    /**
     * Enables literal parsing of the pattern.
     *
     * <p> When this flag is specified then the input string that specifies
     * the pattern is treated as a sequence of literal characters.
     * Metacharacters or escape sequences in the input sequence will be
     * given no special meaning.
     *
     * <p>The flags CASE_INSENSITIVE and UNICODE_CASE retain their impact on
     * matching when used in conjunction with this flag. The other flags
     * become superfluous.
     *
     * <p> There is no embedded flag character for enabling literal parsing.
     * @since 1.5
     */
    public static final int LITERAL = 0x10;

    /**
     * Enables dotall mode.
     *
     * <p> In dotall mode, the expression {@code .} matches any character,
     * including a line terminator.  By default this expression does not match
     * line terminators.
     *
     * <p> Dotall mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?s)}.  (The {@code s} is a mnemonic for
     * "single-line" mode, which is what this is called in Perl.)  </p>
     */
    public static final int DOTALL = 0x20;

    /**
     * Enables Unicode-aware case folding.
     *
     * <p> When this flag is specified then case-insensitive matching, when
     * enabled by the {@link #CASE_INSENSITIVE} flag, is done in a manner
     * consistent with the Unicode Standard.  By default, case-insensitive
     * matching assumes that only characters in the US-ASCII charset are being
     * matched.
     *
     * <p> Unicode-aware case folding can also be enabled via the embedded flag
     * expression&nbsp;{@code (?u)}.
     *
     * <p> Specifying this flag may impose a performance penalty.  </p>
     */
    public static final int UNICODE_CASE = 0x40;

    /**
     * Enables canonical equivalence.
     *
     * <p> When this flag is specified then two characters will be considered
     * to match if, and only if, their full canonical decompositions match.
     * The expression <code>"a&#92;u030A"</code>, for example, will match the
     * string <code>"&#92;u00E5"</code> when this flag is specified.  By default,
     * matching does not take canonical equivalence into account.
     *
     * <p> There is no embedded flag character for enabling canonical
     * equivalence.
     *
     * <p> Specifying this flag may impose a performance penalty.  </p>
     */
    public static final int CANON_EQ = 0x80;

    /**
     * Enables the Unicode version of <i>Predefined character classes</i> and
     * <i>POSIX character classes</i>.
     *
     * <p> When this flag is specified then the (US-ASCII only)
     * <i>Predefined character classes</i> and <i>POSIX character classes</i>
     * are in conformance with
     * <a href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
     * Standard #18: Unicode Regular Expressions</i></a>
     * <i>Annex C: Compatibility Properties</i>.
     * <p>
     * The UNICODE_CHARACTER_CLASS mode can also be enabled via the embedded
     * flag expression&nbsp;{@code (?U)}.
     * <p>
     * The flag implies UNICODE_CASE, that is, it enables Unicode-aware case
     * folding.
     * <p>
     * Specifying this flag may impose a performance penalty.  </p>
     * @since 1.7
     */
    public static final int UNICODE_CHARACTER_CLASS = 0x100;

    /**
     * Contains all possible flags for compile(regex, flags).
     */
    private static final int ALL_FLAGS = CASE_INSENSITIVE | MULTILINE |
            DOTALL | UNICODE_CASE | CANON_EQ | UNIX_LINES | LITERAL |
            UNICODE_CHARACTER_CLASS | COMMENTS;

    /* Pattern has only two serialized components: The pattern string
     * and the flags, which are all that is needed to recompile the pattern
     * when it is deserialized.
     */

    /** use serialVersionUID from Merlin b59 for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = 5073258162644648461L;

    /**
     * The original regular-expression pattern string.
     *
     * @serial
     */
    private String pattern;

    /**
     * The original pattern flags.
     *
     * @serial
     */
    private int flags;

    /**
     * The temporary pattern flags used during compiling. The flags might be turn
     * on and off by embedded flag.
     */
    private transient int flags0;

    /**
     * Boolean indicating this Pattern is compiled; this is necessary in order
     * to lazily compile deserialized Patterns.
     */
    private transient volatile boolean compiled;

    /**
     * The normalized pattern string.
     */
    private transient String normalizedPattern;

    /**
     * The starting point of state machine for the find operation.  This allows
     * a match to start anywhere in the input.
     */
    transient Node root;

    /**
     * The root of object tree for a match operation.  The pattern is matched
     * at the beginning.  This may include a find that uses BnM or a First
     * node.
     */
    transient Node matchRoot;

    /**
     * Temporary storage used by parsing pattern slice.
     */
    transient int[] buffer;

    /**
     * A temporary storage used for predicate for double return.
     */
    transient CharPredicate predicate;

    /**
     * Map the "name" of the "named capturing group" to its group id
     * node.
     */
    transient volatile Map<String, Integer> namedGroups;

    /**
     * Temporary storage used while parsing group references.
     */
    transient GroupHead[] groupNodes;

    /**
     * Temporary storage used to store the top level closure nodes.
     */
    transient List<Node> topClosureNodes;

    /**
     * The number of top greedy closure nodes in this Pattern. Used by
     * matchers to allocate storage needed for a IntHashSet to keep the
     * beginning pos {@code i} of all failed match.
     */
    transient int localTCNCount;

    /*
     * Turn off the stop-exponential-backtracking optimization if there
     * is a group ref in the pattern.
     */
    transient boolean hasGroupRef;

    /**
     * Temporary null terminated code point array used by pattern compiling.
     */
    private transient int[] temp;

    /**
     * The number of capturing groups in this Pattern. Used by matchers to
     * allocate storage needed to perform a match.
     */
    transient int capturingGroupCount;

    /**
     * The local variable count used by parsing tree. Used by matchers to
     * allocate storage needed to perform a match.
     */
    transient int localCount;

    /**
     * Index into the pattern string that keeps track of how much has been
     * parsed.
     */
    private transient int cursor;

    /**
     * Holds the length of the pattern string.
     */
    private transient int patternLength;

    /**
     * If the Start node might possibly match supplementary or surrogate
     * code points.
     * It is set to true during compiling if
     * (1) There is supplementary or surrogate code point in pattern, or
     * (2) There is complement node of a "family" CharProperty
     */
    private transient boolean hasSupplementary;

    /**
     * Compiles the given regular expression into a pattern.
     *
     * @param  regex
     *         The expression to be compiled
     * @return the given regular expression compiled into a pattern
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static Pattern compile(String regex) {
        return new Pattern(regex, 0);
    }

    /**
     * Compiles the given regular expression into a pattern with the given
     * flags.
     *
     * @param  regex
     *         The expression to be compiled
     *
     * @param  flags
     *         Match flags, a bit mask that may include
     *         {@link #CASE_INSENSITIVE}, {@link #MULTILINE}, {@link #DOTALL},
     *         {@link #UNICODE_CASE}, {@link #CANON_EQ}, {@link #UNIX_LINES},
     *         {@link #LITERAL}, {@link #UNICODE_CHARACTER_CLASS}
     *         and {@link #COMMENTS}
     *
     * @return the given regular expression compiled into a pattern with the given flags
     * @throws  IllegalArgumentException
     *          If bit values other than those corresponding to the defined
     *          match flags are set in {@code flags}
     *
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, flags);
    }

    /**
     * Returns the regular expression from which this pattern was compiled.
     *
     * @return  The source of this pattern
     */
    public String pattern() {
        return pattern;
    }

    /**
     * <p>Returns the string representation of this pattern. This
     * is the regular expression from which this pattern was
     * compiled.</p>
     *
     * @return  The string representation of this pattern
     * @since 1.5
     */
    public String toString() {
        return pattern;
    }

    /**
     * Creates a matcher that will match the given input against this pattern.
     *
     * @param  input
     *         The character sequence to be matched
     *
     * @return  A new matcher for this pattern
     */
    public Matcher matcher(CharSequence input) {
        if (!compiled) {
            synchronized(this) {
                if (!compiled)
                    compile();
            }
        }
        Matcher m = new Matcher(this, input);
        return m;
    }

    /**
     * Returns this pattern's match flags.
     *
     * @return  The match flags specified when this pattern was compiled
     */
    public int flags() {
        return flags0;
    }

    /**
     * Compiles the given regular expression and attempts to match the given
     * input against it.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * Pattern.matches(regex, input);</pre></blockquote>
     *
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>
     * Pattern.compile(regex).matcher(input).matches()</pre></blockquote>
     *
     * <p> If a pattern is to be used multiple times, compiling it once and reusing
     * it will be more efficient than invoking this method each time.  </p>
     *
     * @param  regex
     *         The expression to be compiled
     *
     * @param  input
     *         The character sequence to be matched
     * @return whether or not the regular expression matches on the input
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static boolean matches(String regex, CharSequence input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.matches();
    }

    /**
     * Splits the given input sequence around matches of this pattern.
     *
     * <p> The array returned by this method contains each substring of the
     * input sequence that is terminated by another subsequence that matches
     * this pattern or is terminated by the end of the input sequence.  The
     * substrings in the array are in the order in which they occur in the
     * input. If this pattern does not match any subsequence of the input then
     * the resulting array has just one element, namely the input sequence in
     * string form.
     *
     * <p> When there is a positive-width match at the beginning of the input
     * sequence then an empty leading substring is included at the beginning
     * of the resulting array. A zero-width match at the beginning however
     * never produces such empty leading substring.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.
     * <ul>
     *    <li><p>
     *    If the <i>limit</i> is positive then the pattern will be applied
     *    at most <i>limit</i>&nbsp;-&nbsp;1 times, the array's length will be
     *    no greater than <i>limit</i>, and the array's last entry will contain
     *    all input beyond the last matched delimiter.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is zero then the pattern will be applied as
     *    many times as possible, the array can have any length, and trailing
     *    empty strings will be discarded.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is negative then the pattern will be applied
     *    as many times as possible and the array can have any length.</p></li>
     * </ul>
     *
     * <p> The input {@code "boo:and:foo"}, for example, yields the following
     * results with these parameters:
     *
     * <table class="plain" style="margin-left:2em;">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     *     <th scope="col">Regex</th>
     *     <th scope="col">Limit</th>
     *     <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     *     <td>{@code { "boo", "and:foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table>
     *
     * @param  input
     *         The character sequence to be split
     *
     * @param  limit
     *         The result threshold, as described above
     *
     * @return  The array of strings computed by splitting the input
     *          around matches of this pattern
     */
    public String[] split(CharSequence input, int limit) {
        int index = 0;
        boolean matchLimited = limit > 0;
        ArrayList<String> matchList = new ArrayList<>();
        Matcher m = matcher(input);

        // Add segments before each match found
        while(m.find()) {
            if (!matchLimited || matchList.size() < limit - 1) {
                if (index == 0 && index == m.start() && m.start() == m.end()) {
                    // no empty leading substring included for zero-width match
                    // at the beginning of the input char sequence.
                    continue;
                }
                String match = input.subSequence(index, m.start()).toString();
                matchList.add(match);
                index = m.end();
            } else if (matchList.size() == limit - 1) { // last one
                String match = input.subSequence(index,
                                                 input.length()).toString();
                matchList.add(match);
                index = m.end();
            }
        }

        // If no match was found, return this
        if (index == 0)
            return new String[] {input.toString()};

        // Add remaining segment
        if (!matchLimited || matchList.size() < limit)
            matchList.add(input.subSequence(index, input.length()).toString());

        // Construct result
        int resultSize = matchList.size();
        if (limit == 0)
            while (resultSize > 0 && matchList.get(resultSize-1).isEmpty())
                resultSize--;
        String[] result = new String[resultSize];
        return matchList.subList(0, resultSize).toArray(result);
    }

    /**
     * Splits the given input sequence around matches of this pattern.
     *
     * <p> This method works as if by invoking the two-argument {@link
     * #split(java.lang.CharSequence, int) split} method with the given input
     * sequence and a limit argument of zero.  Trailing empty strings are
     * therefore not included in the resulting array. </p>
     *
     * <p> The input {@code "boo:and:foo"}, for example, yields the following
     * results with these expressions:
     *
     * <table class="plain" style="margin-left:2em">
     * <caption style="display:none">Split examples showing regex and result</caption>
     * <thead>
     * <tr>
     *  <th scope="col">Regex</th>
     *  <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="text-weight:normal">:</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" style="text-weight:normal">o</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table>
     *
     *
     * @param  input
     *         The character sequence to be split
     *
     * @return  The array of strings computed by splitting the input
     *          around matches of this pattern
     */
    public String[] split(CharSequence input) {
        return split(input, 0);
    }

    /**
     * Returns a literal pattern {@code String} for the specified
     * {@code String}.
     *
     * <p>This method produces a {@code String} that can be used to
     * create a {@code Pattern} that would match the string
     * {@code s} as if it were a literal pattern.</p> Metacharacters
     * or escape sequences in the input sequence will be given no special
     * meaning.
     *
     * @param  s The string to be literalized
     * @return  A literal string replacement
     * @since 1.5
     */
    public static String quote(String s) {
        int slashEIndex = s.indexOf("\\E");
        if (slashEIndex == -1)
            return "\\Q" + s + "\\E";

        int lenHint = s.length();
        lenHint = (lenHint < Integer.MAX_VALUE - 8 - lenHint) ?
                (lenHint << 1) : (Integer.MAX_VALUE - 8);

        StringBuilder sb = new StringBuilder(lenHint);
        sb.append("\\Q");
        int current = 0;
        do {
            sb.append(s, current, slashEIndex)
                    .append("\\E\\\\E\\Q");
            current = slashEIndex + 2;
        } while ((slashEIndex = s.indexOf("\\E", current)) != -1);

        return sb.append(s, current, s.length())
                .append("\\E")
                .toString();
    }

    /**
     * Recompile the Pattern instance from a stream.  The original pattern
     * string is read in and the object tree is recompiled from it.
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {

        // Read in all fields
        s.defaultReadObject();

        // reset the flags
        flags0 = flags;

        // Initialize counts
        capturingGroupCount = 1;
        localCount = 0;
        localTCNCount = 0;

        // if length > 0, the Pattern is lazily compiled
        if (pattern.isEmpty()) {
            root = new Start(lastAccept);
            matchRoot = lastAccept;
            compiled = true;
        }
    }

    /**
     * This private constructor is used to create all Patterns. The pattern
     * string and match flags are all that is needed to completely describe
     * a Pattern. An empty pattern string results in an object tree with
     * only a Start node and a LastNode node.
     */
    private Pattern(String p, int f) {
        if ((f & ~ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Unknown flag 0x"
                                               + Integer.toHexString(f));
        }
        pattern = p;
        flags = f;

        // to use UNICODE_CASE if UNICODE_CHARACTER_CLASS present
        if ((flags & UNICODE_CHARACTER_CLASS) != 0)
            flags |= UNICODE_CASE;

        // 'flags' for compiling
        flags0 = flags;

        // Reset group index count
        capturingGroupCount = 1;
        localCount = 0;
        localTCNCount = 0;

        if (!pattern.isEmpty()) {
            try {
                compile();
            } catch (StackOverflowError soe) {
                throw error("Stack overflow during pattern compilation");
            }
        } else {
            root = new Start(lastAccept);
            matchRoot = lastAccept;
        }
    }

    /**
     * The pattern is converted to normalized form ({@link
     * java.text.Normalizer.Form#NFC NFC}, canonical decomposition,
     * followed by canonical composition for the character class
     * part, and {@link java.text.Normalizer.Form#NFD NFD},
     * canonical decomposition for the rest), and then a pure
     * group is constructed to match canonical equivalences of the
     * characters.
     */
    private static String normalize(String pattern) {
        int plen = pattern.length();
        StringBuilder pbuf = new StringBuilder(plen);
        char last = 0;
        int lastStart = 0;
        char cc = 0;
        for (int i = 0; i < plen;) {
            char c = pattern.charAt(i);
            if (cc == 0 &&    // top level
                c == '\\' && i + 1 < plen && pattern.charAt(i + 1) == '\\') {
                i += 2; last = 0;
                continue;
            }
            if (c == '[' && last != '\\') {
                if (cc == 0) {
                    if (lastStart < i)
                        normalizeSlice(pattern, lastStart, i, pbuf);
                    lastStart = i;
                }
                cc++;
            } else if (c == ']' && last != '\\') {
                cc--;
                if (cc == 0) {
                    normalizeClazz(pattern, lastStart, i + 1, pbuf);
                    lastStart = i + 1;
                }
            }
            last = c;
            i++;
        }
        assert (cc == 0);
        if (lastStart < plen)
            normalizeSlice(pattern, lastStart, plen, pbuf);
        return pbuf.toString();
    }

    private static void normalizeSlice(String src, int off, int limit,
                                       StringBuilder dst)
    {
        int len = src.length();
        int off0 = off;
        while (off < limit && ASCII.isAscii(src.charAt(off))) {
            off++;
        }
        if (off == limit) {
            dst.append(src, off0, limit);
            return;
        }
        off--;
        if (off < off0)
            off = off0;
        else
            dst.append(src, off0, off);
        while (off < limit) {
            int ch0 = src.codePointAt(off);
            if (".$|()[]{}^?*+\\".indexOf(ch0) != -1) {
                dst.append((char)ch0);
                off++;
                continue;
            }
            int j = Grapheme.nextBoundary(src, off, limit);
            int ch1;
            String seq = src.substring(off, j);
            String nfd = Normalizer.normalize(seq, Normalizer.Form.NFD);
            off = j;
            if (nfd.codePointCount(0, nfd.length()) > 1) {
                ch0 = nfd.codePointAt(0);
                ch1 = nfd.codePointAt(Character.charCount(ch0));
                if (Character.getType(ch1) == Character.NON_SPACING_MARK) {
                    Set<String> altns = new LinkedHashSet<>();
                    altns.add(seq);
                    produceEquivalentAlternation(nfd, altns);
                    dst.append("(?:");
                    altns.forEach( s -> dst.append(s).append('|'));
                    dst.delete(dst.length() - 1, dst.length());
                    dst.append(")");
                    continue;
                }
            }
            String nfc = Normalizer.normalize(seq, Normalizer.Form.NFC);
            if (!seq.equals(nfc) && !nfd.equals(nfc))
                dst.append("(?:" + seq + "|" + nfd  + "|" + nfc + ")");
            else if (!seq.equals(nfd))
                dst.append("(?:" + seq + "|" + nfd + ")");
            else
                dst.append(seq);
        }
    }

    private static void normalizeClazz(String src, int off, int limit,
                                       StringBuilder dst)
    {
        dst.append(Normalizer.normalize(src.substring(off, limit), Form.NFC));
    }

    /**
     * Given a specific sequence composed of a regular character and
     * combining marks that follow it, produce the alternation that will
     * match all canonical equivalences of that sequence.
     */
    private static void produceEquivalentAlternation(String src,
                                                     Set<String> dst)
    {
        int len = countChars(src, 0, 1);
        if (src.length() == len) {
            dst.add(src);  // source has one character.
            return;
        }
        String base = src.substring(0,len);
        String combiningMarks = src.substring(len);
        String[] perms = producePermutations(combiningMarks);
        // Add combined permutations
        for(int x = 0; x < perms.length; x++) {
            String next = base + perms[x];
            dst.add(next);
            next = composeOneStep(next);
            if (next != null) {
                produceEquivalentAlternation(next, dst);
            }
        }
    }

    /**
     * Returns an array of strings that have all the possible
     * permutations of the characters in the input string.
     * This is used to get a list of all possible orderings
     * of a set of combining marks. Note that some of the permutations
     * are invalid because of combining class collisions, and these
     * possibilities must be removed because they are not canonically
     * equivalent.
     */
    private static String[] producePermutations(String input) {
        if (input.length() == countChars(input, 0, 1))
            return new String[] {input};

        if (input.length() == countChars(input, 0, 2)) {
            int c0 = Character.codePointAt(input, 0);
            int c1 = Character.codePointAt(input, Character.charCount(c0));
            if (getClass(c1) == getClass(c0)) {
                return new String[] {input};
            }
            String[] result = new String[2];
            result[0] = input;
            StringBuilder sb = new StringBuilder(2);
            sb.appendCodePoint(c1);
            sb.appendCodePoint(c0);
            result[1] = sb.toString();
            return result;
        }

        int length = 1;
        int nCodePoints = countCodePoints(input);
        for(int x=1; x<nCodePoints; x++)
            length = length * (x+1);

        String[] temp = new String[length];

        int combClass[] = new int[nCodePoints];
        for(int x=0, i=0; x<nCodePoints; x++) {
            int c = Character.codePointAt(input, i);
            combClass[x] = getClass(c);
            i +=  Character.charCount(c);
        }

        // For each char, take it out and add the permutations
        // of the remaining chars
        int index = 0;
        int len;
        // offset maintains the index in code units.
loop:   for(int x=0, offset=0; x<nCodePoints; x++, offset+=len) {
            len = countChars(input, offset, 1);
            for(int y=x-1; y>=0; y--) {
                if (combClass[y] == combClass[x]) {
                    continue loop;
                }
            }
            StringBuilder sb = new StringBuilder(input);
            String otherChars = sb.delete(offset, offset+len).toString();
            String[] subResult = producePermutations(otherChars);

            String prefix = input.substring(offset, offset+len);
            for (String sre : subResult)
                temp[index++] = prefix + sre;
        }
        String[] result = new String[index];
        System.arraycopy(temp, 0, result, 0, index);
        return result;
    }

    private static int getClass(int c) {
        return sun.text.Normalizer.getCombiningClass(c);
    }

    /**
     * Attempts to compose input by combining the first character
     * with the first combining mark following it. Returns a String
     * that is the composition of the leading character with its first
     * combining mark followed by the remaining combining marks. Returns
     * null if the first two characters cannot be further composed.
     */
    private static String composeOneStep(String input) {
        int len = countChars(input, 0, 2);
        String firstTwoCharacters = input.substring(0, len);
        String result = Normalizer.normalize(firstTwoCharacters, Normalizer.Form.NFC);
        if (result.equals(firstTwoCharacters))
            return null;
        else {
            String remainder = input.substring(len);
            return result + remainder;
        }
    }

    /**
     * Preprocess any \Q...\E sequences in `temp', meta-quoting them.
     * See the description of `quotemeta' in perlfunc(1).
     */
    private void RemoveQEQuoting() {
        final int pLen = patternLength;
        int i = 0;
        while (i < pLen-1) {
            if (temp[i] != '\\')
                i += 1;
            else if (temp[i + 1] != 'Q')
                i += 2;
            else
                break;
        }
        if (i >= pLen - 1)    // No \Q sequence found
            return;
        int j = i;
        i += 2;
        int newTempLen;
        try {
            newTempLen = Math.addExact(j + 2, Math.multiplyExact(3, pLen - i));
        } catch (ArithmeticException ae) {
            throw new OutOfMemoryError("Required pattern length too large");
        }
        int[] newtemp = new int[newTempLen];
        System.arraycopy(temp, 0, newtemp, 0, j);

        boolean inQuote = true;
        boolean beginQuote = true;
        while (i < pLen) {
            int c = temp[i++];
            if (!ASCII.isAscii(c) || ASCII.isAlpha(c)) {
                newtemp[j++] = c;
            } else if (ASCII.isDigit(c)) {
                if (beginQuote) {
                    /*
                     * A unicode escape \[0xu] could be before this quote,
                     * and we don't want this numeric char to processed as
                     * part of the escape.
                     */
                    newtemp[j++] = '\\';
                    newtemp[j++] = 'x';
                    newtemp[j++] = '3';
                }
                newtemp[j++] = c;
            } else if (c != '\\') {
                if (inQuote) newtemp[j++] = '\\';
                newtemp[j++] = c;
            } else if (inQuote) {
                if (temp[i] == 'E') {
                    i++;
                    inQuote = false;
                } else {
                    newtemp[j++] = '\\';
                    newtemp[j++] = '\\';
                }
            } else {
                if (temp[i] == 'Q') {
                    i++;
                    inQuote = true;
                    beginQuote = true;
                    continue;
                } else {
                    newtemp[j++] = c;
                    if (i != pLen)
                        newtemp[j++] = temp[i++];
                }
            }

            beginQuote = false;
        }

        patternLength = j;
        temp = Arrays.copyOf(newtemp, j + 2); // double zero termination
    }

    /**
     * Copies regular expression to an int array and invokes the parsing
     * of the expression which will create the object tree.
     */
    private void compile() {
        // Handle canonical equivalences
        if (has(CANON_EQ) && !has(LITERAL)) {
            normalizedPattern = normalize(pattern);
        } else {
            normalizedPattern = pattern;
        }
        patternLength = normalizedPattern.length();

        // Copy pattern to int array for convenience
        // Use double zero to terminate pattern
        temp = new int[patternLength + 2];

        hasSupplementary = false;
        int c, count = 0;
        // Convert all chars into code points
        for (int x = 0; x < patternLength; x += Character.charCount(c)) {
            c = normalizedPattern.codePointAt(x);
            if (isSupplementary(c)) {
                hasSupplementary = true;
            }
            temp[count++] = c;
        }

        patternLength = count;   // patternLength now in code points

        if (! has(LITERAL))
            RemoveQEQuoting();

        // Allocate all temporary objects here.
        buffer = new int[32];
        groupNodes = new GroupHead[10];
        namedGroups = null;
        topClosureNodes = new ArrayList<>(10);

        if (has(LITERAL)) {
            // Literal pattern handling
            matchRoot = newSlice(temp, patternLength, hasSupplementary);
            matchRoot.next = lastAccept;
        } else {
            // Start recursive descent parsing
            matchRoot = expr(lastAccept);
            // Check extra pattern characters
            if (patternLength != cursor) {
                if (peek() == ')') {
                    throw error("Unmatched closing ')'");
                } else {
                    throw error("Unexpected internal error");
                }
            }
        }

        // Peephole optimization
        if (matchRoot instanceof Slice) {
            root = BnM.optimize(matchRoot);
            if (root == matchRoot) {
                root = hasSupplementary ? new StartS(matchRoot) : new Start(matchRoot);
            }
        } else if (matchRoot instanceof Begin || matchRoot instanceof First) {
            root = matchRoot;
        } else {
            root = hasSupplementary ? new StartS(matchRoot) : new Start(matchRoot);
        }

        // Optimize the greedy Loop to prevent exponential backtracking, IF there
        // is no group ref in this pattern. With a non-negative localTCNCount value,
        // the greedy type Loop, Curly will skip the backtracking for any starting
        // position "i" that failed in the past.
        if (!hasGroupRef) {
            for (Node node : topClosureNodes) {
                if (node instanceof Loop) {
                    // non-deterministic-greedy-group
                    ((Loop)node).posIndex = localTCNCount++;
                }
            }
        }

        // Release temporary storage
        temp = null;
        buffer = null;
        groupNodes = null;
        patternLength = 0;
        compiled = true;
        topClosureNodes = null;
    }

    Map<String, Integer> namedGroups() {
        Map<String, Integer> groups = namedGroups;
        if (groups == null) {
            namedGroups = groups = new HashMap<>(2);
        }
        return groups;
    }

    /**
     * Used to accumulate information about a subtree of the object graph
     * so that optimizations can be applied to the subtree.
     */
    static final class TreeInfo {
        int minLength;
        int maxLength;
        boolean maxValid;
        boolean deterministic;

        TreeInfo() {
            reset();
        }
        void reset() {
            minLength = 0;
            maxLength = 0;
            maxValid = true;
            deterministic = true;
        }
    }

    /*
     * The following private methods are mainly used to improve the
     * readability of the code. In order to let the Java compiler easily
     * inline them, we should not put many assertions or error checks in them.
     */

    /**
     * Indicates whether a particular flag is set or not.
     */
    private boolean has(int f) {
        return (flags0 & f) != 0;
    }

    /**
     * Match next character, signal error if failed.
     */
    private void accept(int ch, String s) {
        int testChar = temp[cursor++];
        if (has(COMMENTS))
            testChar = parsePastWhitespace(testChar);
        if (ch != testChar) {
            throw error(s);
        }
    }

    /**
     * Mark the end of pattern with a specific character.
     */
    private void mark(int c) {
        temp[patternLength] = c;
    }

    /**
     * Peek the next character, and do not advance the cursor.
     */
    private int peek() {
        int ch = temp[cursor];
        if (has(COMMENTS))
            ch = peekPastWhitespace(ch);
        return ch;
    }

    /**
     * Read the next character, and advance the cursor by one.
     */
    private int read() {
        int ch = temp[cursor++];
        if (has(COMMENTS))
            ch = parsePastWhitespace(ch);
        return ch;
    }

    /**
     * Read the next character, and advance the cursor by one,
     * ignoring the COMMENTS setting
     */
    private int readEscaped() {
        int ch = temp[cursor++];
        return ch;
    }

    /**
     * Advance the cursor by one, and peek the next character.
     */
    private int next() {
        int ch = temp[++cursor];
        if (has(COMMENTS))
            ch = peekPastWhitespace(ch);
        return ch;
    }

    /**
     * Advance the cursor by one, and peek the next character,
     * ignoring the COMMENTS setting
     */
    private int nextEscaped() {
        int ch = temp[++cursor];
        return ch;
    }

    /**
     * If in xmode peek past whitespace and comments.
     */
    private int peekPastWhitespace(int ch) {
        while (ASCII.isSpace(ch) || ch == '#') {
            while (ASCII.isSpace(ch))
                ch = temp[++cursor];
            if (ch == '#') {
                ch = peekPastLine();
            }
        }
        return ch;
    }

    /**
     * If in xmode parse past whitespace and comments.
     */
    private int parsePastWhitespace(int ch) {
        while (ASCII.isSpace(ch) || ch == '#') {
            while (ASCII.isSpace(ch))
                ch = temp[cursor++];
            if (ch == '#')
                ch = parsePastLine();
        }
        return ch;
    }

    /**
     * xmode parse past comment to end of line.
     */
    private int parsePastLine() {
        int ch = temp[cursor++];
        while (ch != 0 && !isLineSeparator(ch))
            ch = temp[cursor++];
        if (ch == 0 && cursor > patternLength) {
            cursor = patternLength;
            ch = temp[cursor++];
        }
        return ch;
    }

    /**
     * xmode peek past comment to end of line.
     */
    private int peekPastLine() {
        int ch = temp[++cursor];
        while (ch != 0 && !isLineSeparator(ch))
            ch = temp[++cursor];
        if (ch == 0 && cursor > patternLength) {
            cursor = patternLength;
            ch = temp[cursor];
        }
        return ch;
    }

    /**
     * Determines if character is a line separator in the current mode
     */
    private boolean isLineSeparator(int ch) {
        if (has(UNIX_LINES)) {
            return ch == '\n';
        } else {
            return (ch == '\n' ||
                    ch == '\r' ||
                    (ch|1) == '\u2029' ||
                    ch == '\u0085');
        }
    }

    /**
     * Read the character after the next one, and advance the cursor by two.
     */
    private int skip() {
        int i = cursor;
        int ch = temp[i+1];
        cursor = i + 2;
        return ch;
    }

    /**
     * Unread one next character, and retreat cursor by one.
     */
    private void unread() {
        cursor--;
    }

    /**
     * Internal method used for handling all syntax errors. The pattern is
     * displayed with a pointer to aid in locating the syntax error.
     */
    private PatternSyntaxException error(String s) {
        return new PatternSyntaxException(s, normalizedPattern,  cursor - 1);
    }

    /**
     * Determines if there is any supplementary character or unpaired
     * surrogate in the specified range.
     */
    private boolean findSupplementary(int start, int end) {
        for (int i = start; i < end; i++) {
            if (isSupplementary(temp[i]))
                return true;
        }
        return false;
    }

    /**
     * Determines if the specified code point is a supplementary
     * character or unpaired surrogate.
     */
    private static final boolean isSupplementary(int ch) {
        return ch >= Character.MIN_SUPPLEMENTARY_CODE_POINT ||
               Character.isSurrogate((char)ch);
    }

    /**
     *  The following methods handle the main parsing. They are sorted
     *  according to their precedence order, the lowest one first.
     */

    /**
     * The expression is parsed with branch nodes added for alternations.
     * This may be called recursively to parse sub expressions that may
     * contain alternations.
     */
    private Node expr(Node end) {
        Node prev = null;
        Node firstTail = null;
        Branch branch = null;
        Node branchConn = null;

        for (;;) {
            Node node = sequence(end);
            Node nodeTail = root;      //double return
            if (prev == null) {
                prev = node;
                firstTail = nodeTail;
            } else {
                // Branch
                if (branchConn == null) {
                    branchConn = new BranchConn();
                    branchConn.next = end;
                }
                if (node == end) {
                    // if the node returned from sequence() is "end"
                    // we have an empty expr, set a null atom into
                    // the branch to indicate to go "next" directly.
                    node = null;
                } else {
                    // the "tail.next" of each atom goes to branchConn
                    nodeTail.next = branchConn;
                }
                if (prev == branch) {
                    branch.add(node);
                } else {
                    if (prev == end) {
                        prev = null;
                    } else {
                        // replace the "end" with "branchConn" at its tail.next
                        // when put the "prev" into the branch as the first atom.
                        firstTail.next = branchConn;
                    }
                    prev = branch = new Branch(prev, node, branchConn);
                }
            }
            if (peek() != '|') {
                return prev;
            }
            next();
        }
    }

    @SuppressWarnings("fallthrough")
    /**
     * Parsing of sequences between alternations.
     */
    private Node sequence(Node end) {
        Node head = null;
        Node tail = null;
        Node node;
    LOOP:
        for (;;) {
            int ch = peek();
            switch (ch) {
            case '(':
                // Because group handles its own closure,
                // we need to treat it differently
                node = group0();
                // Check for comment or flag group
                if (node == null)
                    continue;
                if (head == null)
                    head = node;
                else
                    tail.next = node;
                // Double return: Tail was returned in root
                tail = root;
                continue;
            case '[':
                if (has(CANON_EQ) && !has(LITERAL))
                    node = new NFCCharProperty(clazz(true));
                else
                    node = newCharProperty(clazz(true));
                break;
            case '\\':
                ch = nextEscaped();
                if (ch == 'p' || ch == 'P') {
                    boolean oneLetter = true;
                    boolean comp = (ch == 'P');
                    ch = next(); // Consume { if present
                    if (ch != '{') {
                        unread();
                    } else {
                        oneLetter = false;
                    }
                    // node = newCharProperty(family(oneLetter, comp));
                    if (has(CANON_EQ) && !has(LITERAL))
                        node = new NFCCharProperty(family(oneLetter, comp));
                    else
                        node = newCharProperty(family(oneLetter, comp));
                } else {
                    unread();
                    node = atom();
                }
                break;
            case '^':
                next();
                if (has(MULTILINE)) {
                    if (has(UNIX_LINES))
                        node = new UnixCaret();
                    else
                        node = new Caret();
                } else {
                    node = new Begin();
                }
                break;
            case '$':
                next();
                if (has(UNIX_LINES))
                    node = new UnixDollar(has(MULTILINE));
                else
                    node = new Dollar(has(MULTILINE));
                break;
            case '.':
                next();
                if (has(DOTALL)) {
                    node = new CharProperty(ALL());
                } else {
                    if (has(UNIX_LINES)) {
                        node = new CharProperty(UNIXDOT());
                    } else {
                        node = new CharProperty(DOT());
                    }
                }
                break;
            case '|':
            case ')':
                break LOOP;
            case ']': // Now interpreting dangling ] and } as literals
            case '}':
                node = atom();
                break;
            case '?':
            case '*':
            case '+':
                next();
                throw error("Dangling meta character '" + ((char)ch) + "'");
            case 0:
                if (cursor >= patternLength) {
                    break LOOP;
                }
                // Fall through
            default:
                node = atom();
                break;
            }

            node = closure(node);
            /* save the top dot-greedy nodes (.*, .+) as well
            if (node instanceof GreedyCharProperty &&
                ((GreedyCharProperty)node).cp instanceof Dot) {
                topClosureNodes.add(node);
            }
            */
            if (head == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
        }
        if (head == null) {
            return end;
        }
        tail.next = end;
        root = tail;      //double return
        return head;
    }

    @SuppressWarnings("fallthrough")
    /**
     * Parse and add a new Single or Slice.
     */
    private Node atom() {
        int first = 0;
        int prev = -1;
        boolean hasSupplementary = false;
        int ch = peek();
        for (;;) {
            switch (ch) {
            case '*':
            case '+':
            case '?':
            case '{':
                if (first > 1) {
                    cursor = prev;    // Unwind one character
                    first--;
                }
                break;
            case '$':
            case '.':
            case '^':
            case '(':
            case '[':
            case '|':
            case ')':
                break;
            case '\\':
                ch = nextEscaped();
                if (ch == 'p' || ch == 'P') { // Property
                    if (first > 0) { // Slice is waiting; handle it first
                        unread();
                        break;
                    } else { // No slice; just return the family node
                        boolean comp = (ch == 'P');
                        boolean oneLetter = true;
                        ch = next(); // Consume { if present
                        if (ch != '{')
                            unread();
                        else
                            oneLetter = false;
                        if (has(CANON_EQ) && !has(LITERAL))
                            return new NFCCharProperty(family(oneLetter, comp));
                        else
                            return newCharProperty(family(oneLetter, comp));
                    }
                }
                unread();
                prev = cursor;
                ch = escape(false, first == 0, false);
                if (ch >= 0) {
                    append(ch, first);
                    first++;
                    if (isSupplementary(ch)) {
                        hasSupplementary = true;
                    }
                    ch = peek();
                    continue;
                } else if (first == 0) {
                    return root;
                }
                // Unwind meta escape sequence
                cursor = prev;
                break;
            case 0:
                if (cursor >= patternLength) {
                    break;
                }
                // Fall through
            default:
                prev = cursor;
                append(ch, first);
                first++;
                if (isSupplementary(ch)) {
                    hasSupplementary = true;
                }
                ch = next();
                continue;
            }
            break;
        }
        if (first == 1) {
            return newCharProperty(single(buffer[0]));
        } else {
            return newSlice(buffer, first, hasSupplementary);
        }
    }

    private void append(int ch, int index) {
        int len = buffer.length;
        if (index - len >= 0) {
            len = ArraysSupport.newLength(len,
                    1 + index - len, /* minimum growth */
                    len              /* preferred growth */);
            buffer = Arrays.copyOf(buffer, len);
        }
        buffer[index] = ch;
    }

    /**
     * Parses a backref greedily, taking as many numbers as it
     * can. The first digit is always treated as a backref, but
     * multi digit numbers are only treated as a backref if at
     * least that many backrefs exist at this point in the regex.
     */
    private Node ref(int refNum) {
        boolean done = false;
        while(!done) {
            int ch = peek();
            switch (ch) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    int newRefNum = (refNum * 10) + (ch - '0');
                    // Add another number if it doesn't make a group
                    // that doesn't exist
                    if (capturingGroupCount - 1 < newRefNum) {
                        done = true;
                        break;
                    }
                    refNum = newRefNum;
                    read();
                }
                default -> done = true;
            }
        }
        hasGroupRef = true;
        if (has(CASE_INSENSITIVE))
            return new CIBackRef(refNum, has(UNICODE_CASE));
        else
            return new BackRef(refNum);
    }

    /**
     * Parses an escape sequence to determine the actual value that needs
     * to be matched.
     * If -1 is returned and create was true a new object was added to the tree
     * to handle the escape sequence.
     * If the returned value is greater than zero, it is the value that
     * matches the escape sequence.
     */
    private int escape(boolean inclass, boolean create, boolean isrange) {
        int ch = skip();
        switch (ch) {
        case '0':
            return o();
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            if (inclass) break;
            if (create) {
                root = ref((ch - '0'));
            }
            return -1;
        case 'A':
            if (inclass) break;
            if (create) root = new Begin();
            return -1;
        case 'B':
            if (inclass) break;
            if (create) root = new Bound(Bound.NONE, has(UNICODE_CHARACTER_CLASS));
            return -1;
        case 'C':
            break;
        case 'D':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.DIGIT() : CharPredicates.ASCII_DIGIT();
                predicate = predicate.negate();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'E':
        case 'F':
            break;
        case 'G':
            if (inclass) break;
            if (create) root = new LastMatch();
            return -1;
        case 'H':
            if (create) {
                predicate = HorizWS().negate();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
            break;
        case 'N':
            return N();
        case 'O':
        case 'P':
        case 'Q':
            break;
        case 'R':
            if (inclass) break;
            if (create) root = new LineEnding();
            return -1;
        case 'S':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.WHITE_SPACE() : CharPredicates.ASCII_SPACE();
                predicate = predicate.negate();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'T':
        case 'U':
            break;
        case 'V':
            if (create) {
                predicate = VertWS().negate();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'W':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.WORD() : CharPredicates.ASCII_WORD();
                predicate = predicate.negate();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'X':
            if (inclass) break;
            if (create) {
                root = new XGrapheme();
            }
            return -1;
        case 'Y':
            break;
        case 'Z':
            if (inclass) break;
            if (create) {
                if (has(UNIX_LINES))
                    root = new UnixDollar(false);
                else
                    root = new Dollar(false);
            }
            return -1;
        case 'a':
            return '\007';
        case 'b':
            if (inclass) break;
            if (create) {
                if (peek() == '{') {
                    if (skip() == 'g') {
                        if (read() == '}') {
                            root = new GraphemeBound();
                            return -1;
                        }
                        break;  // error missing trailing }
                    }
                    unread(); unread();
                }
                root = new Bound(Bound.BOTH, has(UNICODE_CHARACTER_CLASS));
            }
            return -1;
        case 'c':
            return c();
        case 'd':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.DIGIT() : CharPredicates.ASCII_DIGIT();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'e':
            return '\033';
        case 'f':
            return '\f';
        case 'g':
            break;
        case 'h':
            if (create) {
                predicate = HorizWS();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'i':
        case 'j':
            break;
        case 'k':
            if (inclass)
                break;
            if (read() != '<')
                throw error("\\k is not followed by '<' for named capturing group");
            String name = groupname(read());
            if (!namedGroups().containsKey(name))
                throw error("named capturing group <" + name + "> does not exist");
            if (create) {
                hasGroupRef = true;
                if (has(CASE_INSENSITIVE))
                    root = new CIBackRef(namedGroups().get(name), has(UNICODE_CASE));
                else
                    root = new BackRef(namedGroups().get(name));
            }
            return -1;
        case 'l':
        case 'm':
            break;
        case 'n':
            return '\n';
        case 'o':
        case 'p':
        case 'q':
            break;
        case 'r':
            return '\r';
        case 's':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.WHITE_SPACE() : CharPredicates.ASCII_SPACE();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 't':
            return '\t';
        case 'u':
            return u();
        case 'v':
            // '\v' was implemented as VT/0x0B in releases < 1.8 (though
            // undocumented). In JDK8 '\v' is specified as a predefined
            // character class for all vertical whitespace characters.
            // So [-1, root=VertWS node] pair is returned (instead of a
            // single 0x0B). This breaks the range if '\v' is used as
            // the start or end value, such as [\v-...] or [...-\v], in
            // which a single definite value (0x0B) is expected. For
            // compatibility concern '\013'/0x0B is returned if isrange.
            if (isrange)
                return '\013';
            if (create) {
                predicate = VertWS();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'w':
            if (create) {
                predicate = has(UNICODE_CHARACTER_CLASS) ?
                            CharPredicates.WORD() : CharPredicates.ASCII_WORD();
                if (!inclass)
                    root = newCharProperty(predicate);
            }
            return -1;
        case 'x':
            return x();
        case 'y':
            break;
        case 'z':
            if (inclass) break;
            if (create) root = new End();
            return -1;
        default:
            return ch;
        }
        throw error("Illegal/unsupported escape sequence");
    }

    /**
     * Parse a character class, and return the node that matches it.
     *
     * Consumes a ] on the way out if consume is true. Usually consume
     * is true except for the case of [abc&&def] where def is a separate
     * right hand node with "understood" brackets.
     */
    private CharPredicate clazz(boolean consume) {
        CharPredicate prev = null;
        CharPredicate curr = null;
        BitClass bits = new BitClass();

        boolean isNeg = false;
        boolean hasBits = false;
        int ch = next();

        // Negates if first char in a class, otherwise literal
        if (ch == '^' && temp[cursor-1] == '[') {
            ch = next();
            isNeg = true;
        }
        for (;;) {
            switch (ch) {
                case '[':
                    curr = clazz(true);
                    if (prev == null)
                        prev = curr;
                    else
                        prev = prev.union(curr);
                    ch = peek();
                    continue;
                case '&':
                    ch = next();
                    if (ch == '&') {
                        ch = next();
                        CharPredicate right = null;
                        while (ch != ']' && ch != '&') {
                            if (ch == '[') {
                                if (right == null)
                                    right = clazz(true);
                                else
                                    right = right.union(clazz(true));
                            } else { // abc&&def
                                unread();
                                if (right == null) {
                                    right = clazz(false);
                                } else {
                                    right = right.union(clazz(false));
                                }
                            }
                            ch = peek();
                        }
                        if (hasBits) {
                            // bits used, union has high precedence
                            if (prev == null) {
                                prev = curr = bits;
                            } else {
                                prev = prev.union(bits);
                            }
                            hasBits = false;
                        }
                        if (right != null)
                            curr = right;
                        if (prev == null) {
                            if (right == null)
                                throw error("Bad class syntax");
                            else
                                prev = right;
                        } else {
                            prev = prev.and(curr);
                        }
                    } else {
                        // treat as a literal &
                        unread();
                        break;
                    }
                    continue;
                case 0:
                    if (cursor >= patternLength)
                        throw error("Unclosed character class");
                    break;
                case ']':
                    if (prev != null || hasBits) {
                        if (consume)
                            next();
                        if (prev == null)
                            prev = bits;
                        else if (hasBits)
                            prev = prev.union(bits);
                        if (isNeg)
                            return prev.negate();
                        return prev;
                    }
                    break;
                default:
                    break;
            }
            curr = range(bits);
            if (curr == null) {    // the bits used
                hasBits = true;
            } else {
                if (prev == null)
                    prev = curr;
                else if (prev != curr)
                    prev = prev.union(curr);
            }
            ch = peek();
        }
    }

    private CharPredicate bitsOrSingle(BitClass bits, int ch) {
        /* Bits can only handle codepoints in [u+0000-u+00ff] range.
           Use "single" node instead of bits when dealing with unicode
           case folding for codepoints listed below.
           (1)Uppercase out of range: u+00ff, u+00b5
              toUpperCase(u+00ff) -> u+0178
              toUpperCase(u+00b5) -> u+039c
           (2)LatinSmallLetterLongS u+17f
              toUpperCase(u+017f) -> u+0053
           (3)LatinSmallLetterDotlessI u+131
              toUpperCase(u+0131) -> u+0049
           (4)LatinCapitalLetterIWithDotAbove u+0130
              toLowerCase(u+0130) -> u+0069
           (5)KelvinSign u+212a
              toLowerCase(u+212a) ==> u+006B
           (6)AngstromSign u+212b
              toLowerCase(u+212b) ==> u+00e5
        */
        if (ch < 256 &&
            !(has(CASE_INSENSITIVE) && has(UNICODE_CASE) &&
              (ch == 0xff || ch == 0xb5 ||
               ch == 0x49 || ch == 0x69 ||    //I and i
               ch == 0x53 || ch == 0x73 ||    //S and s
               ch == 0x4b || ch == 0x6b ||    //K and k
               ch == 0xc5 || ch == 0xe5))) {  //A+ring
            bits.add(ch, flags0);
            return null;
        }
        return single(ch);
    }

    /**
     *  Returns a suitably optimized, single character predicate
     */
    private CharPredicate single(final int ch) {
        if (has(CASE_INSENSITIVE)) {
            int lower, upper;
            if (has(UNICODE_CASE)) {
                upper = Character.toUpperCase(ch);
                lower = Character.toLowerCase(upper);
                // Unicode case insensitive matches
                if (upper != lower)
                    return SingleU(lower);
            } else if (ASCII.isAscii(ch)) {
                lower = ASCII.toLower(ch);
                upper = ASCII.toUpper(ch);
                // Case insensitive matches a given BMP character
                if (lower != upper)
                    return SingleI(lower, upper);
            }
        }
        if (isSupplementary(ch))
            return SingleS(ch);
        return Single(ch);  // Match a given BMP character
    }

    /**
     * Parse a single character or a character range in a character class
     * and return its representative node.
     */
    private CharPredicate range(BitClass bits) {
        int ch = peek();
        if (ch == '\\') {
            ch = nextEscaped();
            if (ch == 'p' || ch == 'P') { // A property
                boolean comp = (ch == 'P');
                boolean oneLetter = true;
                // Consume { if present
                ch = next();
                if (ch != '{')
                    unread();
                else
                    oneLetter = false;
                return family(oneLetter, comp);
            } else { // ordinary escape
                boolean isrange = temp[cursor+1] == '-';
                unread();
                ch = escape(true, true, isrange);
                if (ch == -1)
                    return predicate;
            }
        } else {
            next();
        }
        if (ch >= 0) {
            if (peek() == '-') {
                int endRange = temp[cursor+1];
                if (endRange == '[') {
                    return bitsOrSingle(bits, ch);
                }
                if (endRange != ']') {
                    next();
                    int m = peek();
                    if (m == '\\') {
                        m = escape(true, false, true);
                    } else {
                        next();
                    }
                    if (m < ch) {
                        throw error("Illegal character range");
                    }
                    if (has(CASE_INSENSITIVE)) {
                        if (has(UNICODE_CASE))
                            return CIRangeU(ch, m);
                        return CIRange(ch, m);
                    } else {
                        return Range(ch, m);
                    }
                }
            }
            return bitsOrSingle(bits, ch);
        }
        throw error("Unexpected character '"+((char)ch)+"'");
    }

    /**
     * Parses a Unicode character family and returns its representative node.
     */
    private CharPredicate family(boolean singleLetter, boolean isComplement) {
        next();
        String name;
        CharPredicate p = null;

        if (singleLetter) {
            int c = temp[cursor];
            if (!Character.isSupplementaryCodePoint(c)) {
                name = String.valueOf((char)c);
            } else {
                name = new String(temp, cursor, 1);
            }
            read();
        } else {
            int i = cursor;
            mark('}');
            while(read() != '}') {
            }
            mark('\000');
            int j = cursor;
            if (j > patternLength)
                throw error("Unclosed character family");
            if (i + 1 >= j)
                throw error("Empty character family");
            name = new String(temp, i, j-i-1);
        }

        int i = name.indexOf('=');
        if (i != -1) {
            // property construct \p{name=value}
            String value = name.substring(i + 1);
            name = name.substring(0, i).toLowerCase(Locale.ENGLISH);
            switch (name) {
                case "sc":
                case "script":
                    p = CharPredicates.forUnicodeScript(value);
                    break;
                case "blk":
                case "block":
                    p = CharPredicates.forUnicodeBlock(value);
                    break;
                case "gc":
                case "general_category":
                    p = CharPredicates.forProperty(value, has(CASE_INSENSITIVE));
                    break;
                default:
                    break;
            }
            if (p == null)
                throw error("Unknown Unicode property {name=<" + name + ">, "
                             + "value=<" + value + ">}");

        } else {
            if (name.startsWith("In")) {
                // \p{InBlockName}
                p = CharPredicates.forUnicodeBlock(name.substring(2));
            } else if (name.startsWith("Is")) {
                // \p{IsGeneralCategory} and \p{IsScriptName}
                String shortName = name.substring(2);
                p = CharPredicates.forUnicodeProperty(shortName, has(CASE_INSENSITIVE));
                if (p == null)
                    p = CharPredicates.forProperty(shortName, has(CASE_INSENSITIVE));
                if (p == null)
                    p = CharPredicates.forUnicodeScript(shortName);
            } else {
                if (has(UNICODE_CHARACTER_CLASS))
                    p = CharPredicates.forPOSIXName(name, has(CASE_INSENSITIVE));
                if (p == null)
                    p = CharPredicates.forProperty(name, has(CASE_INSENSITIVE));
            }
            if (p == null)
                throw error("Unknown character property name {" + name + "}");
        }
        if (isComplement) {
            // it might be too expensive to detect if a complement of
            // CharProperty can match "certain" supplementary. So just
            // go with StartS.
            hasSupplementary = true;
            p = p.negate();
        }
        return p;
    }

    private CharProperty newCharProperty(CharPredicate p) {
        if (p == null)
            return null;
        if (p instanceof BmpCharPredicate)
            return new BmpCharProperty((BmpCharPredicate)p);
        else {
            hasSupplementary = true;
            return new CharProperty(p);
        }
    }

    /**
     * Parses and returns the name of a "named capturing group", the trailing
     * ">" is consumed after parsing.
     */
    private String groupname(int ch) {
        StringBuilder sb = new StringBuilder();
        if (!ASCII.isAlpha(ch))
            throw error("capturing group name does not start with a Latin letter");
        do {
            sb.append((char) ch);
        } while (ASCII.isAlnum(ch=read()));
        if (ch != '>')
            throw error("named capturing group is missing trailing '>'");
        return sb.toString();
    }

    /**
     * Parses a group and returns the head node of a set of nodes that process
     * the group. Sometimes a double return system is used where the tail is
     * returned in root.
     */
    private Node group0() {
        boolean capturingGroup = false;
        Node head;
        Node tail;
        int save = flags0;
        int saveTCNCount = topClosureNodes.size();
        root = null;
        int ch = next();
        if (ch == '?') {
            ch = skip();
            switch (ch) {
                case ':' -> {   //  (?:xxx) pure group
                    head = createGroup(true);
                    tail = root;
                    head.next = expr(tail);
                }
                case '=', '!' -> {   // (?=xxx) and (?!xxx) lookahead
                    head = createGroup(true);
                    tail = root;
                    head.next = expr(tail);
                    if (ch == '=') {
                        head = tail = new Pos(head);
                    } else {
                        head = tail = new Neg(head);
                    }
                }
                case '>' -> {   // (?>xxx)  independent group
                    head = createGroup(true);
                    tail = root;
                    head.next = expr(tail);
                    head = tail = new Ques(head, Qtype.INDEPENDENT);
                }
                case '<' -> {   // (?<xxx)  look behind
                    ch = read();
                    if (ch != '=' && ch != '!') {
                        // named captured group
                        String name = groupname(ch);
                        if (namedGroups().containsKey(name))
                            throw error("Named capturing group <" + name
                                        + "> is already defined");
                        capturingGroup = true;
                        head = createGroup(false);
                        tail = root;
                        namedGroups().put(name, capturingGroupCount - 1);
                        head.next = expr(tail);
                        break;
                    }
                    int start = cursor;
                    head = createGroup(true);
                    tail = root;
                    head.next = expr(tail);
                    tail.next = LookBehindEndNode.INSTANCE;
                    TreeInfo info = new TreeInfo();
                    head.study(info);
                    if (info.maxValid == false) {
                        throw error("Look-behind group does not have "
                                    + "an obvious maximum length");
                    }
                    boolean hasSupplementary = findSupplementary(start, patternLength);
                    if (ch == '=') {
                        head = tail = (hasSupplementary ?
                            new BehindS(head, info.maxLength,
                                info.minLength) :
                            new Behind(head, info.maxLength,
                                info.minLength));
                    } else { // if (ch == '!')
                        head = tail = (hasSupplementary ?
                            new NotBehindS(head, info.maxLength,
                                info.minLength) :
                            new NotBehind(head, info.maxLength,
                                info.minLength));
                    }
                    // clear all top-closure-nodes inside lookbehind
                    if (saveTCNCount < topClosureNodes.size())
                        topClosureNodes.subList(saveTCNCount, topClosureNodes.size()).clear();
                }
                case '$', '@' -> throw error("Unknown group type");
                default -> {    // (?xxx:) inlined match flags
                    unread();
                    addFlag();
                    ch = read();
                    if (ch == ')') {
                        return null;    // Inline modifier only
                    }
                    if (ch != ':') {
                        throw error("Unknown inline modifier");
                    }
                    head = createGroup(true);
                    tail = root;
                    head.next = expr(tail);
                }
            }
        } else { // (xxx) a regular group
            capturingGroup = true;
            head = createGroup(false);
            tail = root;
            head.next = expr(tail);
        }

        accept(')', "Unclosed group");
        flags0 = save;

        // Check for quantifiers
        Node node = closure(head);
        if (node == head) { // No closure
            root = tail;
            return node;    // Dual return
        }
        if (head == tail) { // Zero length assertion
            root = node;
            return node;    // Dual return
        }

        // have group closure, clear all inner closure nodes from the
        // top list (no backtracking stopper optimization for inner
        if (saveTCNCount < topClosureNodes.size())
            topClosureNodes.subList(saveTCNCount, topClosureNodes.size()).clear();

        if (node instanceof Ques ques) {
            if (ques.type == Qtype.POSSESSIVE) {
                root = node;
                return node;
            }
            tail.next = new BranchConn();
            tail = tail.next;
            if (ques.type == Qtype.GREEDY) {
                head = new Branch(head, null, tail);
            } else { // Reluctant quantifier
                head = new Branch(null, head, tail);
            }
            root = tail;
            return head;
        } else if (node instanceof Curly curly) {
            if (curly.type == Qtype.POSSESSIVE) {
                root = node;
                return node;
            }
            // Discover if the group is deterministic
            TreeInfo info = new TreeInfo();
            if (head.study(info)) { // Deterministic
                GroupTail temp = (GroupTail) tail;
                head = root = new GroupCurly(head.next, curly.cmin,
                                   curly.cmax, curly.type,
                                   ((GroupTail)tail).localIndex,
                                   ((GroupTail)tail).groupIndex,
                                             capturingGroup);
                return head;
            } else { // Non-deterministic
                int temp = ((GroupHead) head).localIndex;
                Loop loop;
                if (curly.type == Qtype.GREEDY) {
                    loop = new Loop(this.localCount, temp);
                    // add the max_reps greedy to the top-closure-node list
                    if (curly.cmax == MAX_REPS)
                        topClosureNodes.add(loop);
                } else {  // Reluctant Curly
                    loop = new LazyLoop(this.localCount, temp);
                }
                Prolog prolog = new Prolog(loop);
                this.localCount += 1;
                loop.cmin = curly.cmin;
                loop.cmax = curly.cmax;
                loop.body = head;
                tail.next = loop;
                root = loop;
                return prolog; // Dual return
            }
        }
        throw error("Internal logic error");
    }

    /**
     * Create group head and tail nodes using double return. If the group is
     * created with anonymous true then it is a pure group and should not
     * affect group counting.
     */
    private Node createGroup(boolean anonymous) {
        int localIndex = localCount++;
        int groupIndex = 0;
        if (!anonymous)
            groupIndex = capturingGroupCount++;
        GroupHead head = new GroupHead(localIndex);
        root = new GroupTail(localIndex, groupIndex);

        // for debug/print only, head.match does NOT need the "tail" info
        head.tail = (GroupTail)root;

        if (!anonymous && groupIndex < 10)
            groupNodes[groupIndex] = head;
        return head;
    }

    @SuppressWarnings("fallthrough")
    /**
     * Parses inlined match flags and set them appropriately.
     */
    private void addFlag() {
        int ch = peek();
        for (;;) {
            switch (ch) {
            case 'i':
                flags0 |= CASE_INSENSITIVE;
                break;
            case 'm':
                flags0 |= MULTILINE;
                break;
            case 's':
                flags0 |= DOTALL;
                break;
            case 'd':
                flags0 |= UNIX_LINES;
                break;
            case 'u':
                flags0 |= UNICODE_CASE;
                break;
            case 'c':
                flags0 |= CANON_EQ;
                break;
            case 'x':
                flags0 |= COMMENTS;
                break;
            case 'U':
                flags0 |= (UNICODE_CHARACTER_CLASS | UNICODE_CASE);
                break;
            case '-': // subFlag then fall through
                ch = next();
                subFlag();
            default:
                return;
            }
            ch = next();
        }
    }

    @SuppressWarnings("fallthrough")
    /**
     * Parses the second part of inlined match flags and turns off
     * flags appropriately.
     */
    private void subFlag() {
        int ch = peek();
        for (;;) {
            switch (ch) {
            case 'i':
                flags0 &= ~CASE_INSENSITIVE;
                break;
            case 'm':
                flags0 &= ~MULTILINE;
                break;
            case 's':
                flags0 &= ~DOTALL;
                break;
            case 'd':
                flags0 &= ~UNIX_LINES;
                break;
            case 'u':
                flags0 &= ~UNICODE_CASE;
                break;
            case 'c':
                flags0 &= ~CANON_EQ;
                break;
            case 'x':
                flags0 &= ~COMMENTS;
                break;
            case 'U':
                flags0 &= ~(UNICODE_CHARACTER_CLASS | UNICODE_CASE);
                break;
            default:
                return;
            }
            ch = next();
        }
    }

    static final int MAX_REPS   = 0x7FFFFFFF;

    static enum Qtype {
        GREEDY, LAZY, POSSESSIVE, INDEPENDENT
    }

    private Qtype qtype() {
        int ch = next();
        if (ch == '?') {
            next();
            return Qtype.LAZY;
        } else if (ch == '+') {
            next();
            return Qtype.POSSESSIVE;
        }
        return Qtype.GREEDY;
    }

    private Node curly(Node prev, int cmin) {
        Qtype qtype = qtype();
        if (qtype == Qtype.GREEDY) {
            if (prev instanceof BmpCharProperty) {
                return new BmpCharPropertyGreedy((BmpCharProperty)prev, cmin);
            } else if (prev instanceof CharProperty) {
                return new CharPropertyGreedy((CharProperty)prev, cmin);
            }
        }
        return new Curly(prev, cmin, MAX_REPS, qtype);
    }

    /**
     * Processes repetition. If the next character peeked is a quantifier
     * then new nodes must be appended to handle the repetition.
     * Prev could be a single or a group, so it could be a chain of nodes.
     */
    private Node closure(Node prev) {
        int ch = peek();
        switch (ch) {
        case '?':
            return new Ques(prev, qtype());
        case '*':
            return curly(prev, 0);
        case '+':
            return curly(prev, 1);
        case '{':
            ch = skip();
            if (ASCII.isDigit(ch)) {
                int cmin = 0, cmax;
                try {
                    do {
                        cmin = Math.addExact(Math.multiplyExact(cmin, 10),
                                             ch - '0');
                    } while (ASCII.isDigit(ch = read()));
                    if (ch == ',') {
                        ch = read();
                        if (ch == '}') {
                            unread();
                            return curly(prev, cmin);
                        } else {
                            cmax = 0;
                            while (ASCII.isDigit(ch)) {
                                cmax = Math.addExact(Math.multiplyExact(cmax, 10),
                                                     ch - '0');
                                ch = read();
                            }
                        }
                    } else {
                        cmax = cmin;
                    }
                } catch (ArithmeticException ae) {
                    throw error("Illegal repetition range");
                }
                if (ch != '}')
                    throw error("Unclosed counted closure");
                if (cmax < cmin)
                    throw error("Illegal repetition range");
                unread();
                return (cmin == 0 && cmax == 1)
                        ? new Ques(prev, qtype())
                        : new Curly(prev, cmin, cmax, qtype());
            } else {
                throw error("Illegal repetition");
            }
        default:
            return prev;
        }
    }

    /**
     *  Utility method for parsing control escape sequences.
     */
    private int c() {
        if (cursor < patternLength) {
            return read() ^ 64;
        }
        throw error("Illegal control escape sequence");
    }

    /**
     *  Utility method for parsing octal escape sequences.
     */
    private int o() {
        int n = read();
        if (((n-'0')|('7'-n)) >= 0) {
            int m = read();
            if (((m-'0')|('7'-m)) >= 0) {
                int o = read();
                if ((((o-'0')|('7'-o)) >= 0) && (((n-'0')|('3'-n)) >= 0)) {
                    return (n - '0') * 64 + (m - '0') * 8 + (o - '0');
                }
                unread();
                return (n - '0') * 8 + (m - '0');
            }
            unread();
            return (n - '0');
        }
        throw error("Illegal octal escape sequence");
    }

    /**
     *  Utility method for parsing hexadecimal escape sequences.
     */
    private int x() {
        int n = read();
        if (ASCII.isHexDigit(n)) {
            int m = read();
            if (ASCII.isHexDigit(m)) {
                return ASCII.toDigit(n) * 16 + ASCII.toDigit(m);
            }
        } else if (n == '{' && ASCII.isHexDigit(peek())) {
            int ch = 0;
            while (ASCII.isHexDigit(n = read())) {
                ch = (ch << 4) + ASCII.toDigit(n);
                if (ch > Character.MAX_CODE_POINT)
                    throw error("Hexadecimal codepoint is too big");
            }
            if (n != '}')
                throw error("Unclosed hexadecimal escape sequence");
            return ch;
        }
        throw error("Illegal hexadecimal escape sequence");
    }

    /**
     *  Utility method for parsing unicode escape sequences.
     */
    private int cursor() {
        return cursor;
    }

    private void setcursor(int pos) {
        cursor = pos;
    }

    private int uxxxx() {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            int ch = read();
            if (!ASCII.isHexDigit(ch)) {
                throw error("Illegal Unicode escape sequence");
            }
            n = n * 16 + ASCII.toDigit(ch);
        }
        return n;
    }

    private int u() {
        int n = uxxxx();
        if (Character.isHighSurrogate((char)n)) {
            int cur = cursor();
            if (read() == '\\' && read() == 'u') {
                int n2 = uxxxx();
                if (Character.isLowSurrogate((char)n2))
                    return Character.toCodePoint((char)n, (char)n2);
            }
            setcursor(cur);
        }
        return n;
    }

    private int N() {
        if (read() == '{') {
            int i = cursor;
            while (read() != '}') {
                if (cursor >= patternLength)
                    throw error("Unclosed character name escape sequence");
            }
            String name = new String(temp, i, cursor - i - 1);
            try {
                return Character.codePointOf(name);
            } catch (IllegalArgumentException x) {
                throw error("Unknown character name [" + name + "]");
            }
        }
        throw error("Illegal character name escape sequence");
    }

    //
    // Utility methods for code point support
    //
    private static final int countChars(CharSequence seq, int index,
                                        int lengthInCodePoints) {
        // optimization
        if (lengthInCodePoints == 1 && !Character.isHighSurrogate(seq.charAt(index))) {
            assert (index >= 0 && index < seq.length());
            return 1;
        }
        int length = seq.length();
        int x = index;
        if (lengthInCodePoints >= 0) {
            assert (index >= 0 && index < length);
            for (int i = 0; x < length && i < lengthInCodePoints; i++) {
                if (Character.isHighSurrogate(seq.charAt(x++))) {
                    if (x < length && Character.isLowSurrogate(seq.charAt(x))) {
                        x++;
                    }
                }
            }
            return x - index;
        }

        assert (index >= 0 && index <= length);
        if (index == 0) {
            return 0;
        }
        int len = -lengthInCodePoints;
        for (int i = 0; x > 0 && i < len; i++) {
            if (Character.isLowSurrogate(seq.charAt(--x))) {
                if (x > 0 && Character.isHighSurrogate(seq.charAt(x-1))) {
                    x--;
                }
            }
        }
        return index - x;
    }

    private static final int countCodePoints(CharSequence seq) {
        int length = seq.length();
        int n = 0;
        for (int i = 0; i < length; ) {
            n++;
            if (Character.isHighSurrogate(seq.charAt(i++))) {
                if (i < length && Character.isLowSurrogate(seq.charAt(i))) {
                    i++;
                }
            }
        }
        return n;
    }

    /**
     *  Creates a bit vector for matching Latin-1 values. A normal BitClass
     *  never matches values above Latin-1, and a complemented BitClass always
     *  matches values above Latin-1.
     */
    static final class BitClass implements BmpCharPredicate {
        final boolean[] bits;
        BitClass() {
            bits = new boolean[256];
        }
        BitClass add(int c, int flags) {
            assert c >= 0 && c <= 255;
            if ((flags & CASE_INSENSITIVE) != 0) {
                if (ASCII.isAscii(c)) {
                    bits[ASCII.toUpper(c)] = true;
                    bits[ASCII.toLower(c)] = true;
                } else if ((flags & UNICODE_CASE) != 0) {
                    bits[Character.toLowerCase(c)] = true;
                    bits[Character.toUpperCase(c)] = true;
                }
            }
            bits[c] = true;
            return this;
        }
        public boolean is(int ch) {
            return ch < 256 && bits[ch];
        }
    }


    /**
     *  Utility method for creating a string slice matcher.
     */
    private Node newSlice(int[] buf, int count, boolean hasSupplementary) {
        int[] tmp = new int[count];
        if (has(CASE_INSENSITIVE)) {
            if (has(UNICODE_CASE)) {
                for (int i = 0; i < count; i++) {
                    tmp[i] = Character.toLowerCase(
                                 Character.toUpperCase(buf[i]));
                }
                return hasSupplementary? new SliceUS(tmp) : new SliceU(tmp);
            }
            for (int i = 0; i < count; i++) {
                tmp[i] = ASCII.toLower(buf[i]);
            }
            return hasSupplementary? new SliceIS(tmp) : new SliceI(tmp);
        }
        for (int i = 0; i < count; i++) {
            tmp[i] = buf[i];
        }
        return hasSupplementary ? new SliceS(tmp) : new Slice(tmp);
    }

    /**
     * The following classes are the building components of the object
     * tree that represents a compiled regular expression. The object tree
     * is made of individual elements that handle constructs in the Pattern.
     * Each type of object knows how to match its equivalent construct with
     * the match() method.
     */

    /**
     * Base class for all node classes. Subclasses should override the match()
     * method as appropriate. This class is an accepting node, so its match()
     * always returns true.
     */
    static class Node extends Object {
        Node next;
        Node() {
            next = Pattern.accept;
        }
        /**
         * This method implements the classic accept node.
         */
        boolean match(Matcher matcher, int i, CharSequence seq) {
            matcher.last = i;
            matcher.groups[0] = matcher.first;
            matcher.groups[1] = matcher.last;
            return true;
        }
        /**
         * This method is good for all zero length assertions.
         */
        boolean study(TreeInfo info) {
            if (next != null) {
                return next.study(info);
            } else {
                return info.deterministic;
            }
        }
    }

    static class LastNode extends Node {
        /**
         * This method implements the classic accept node with
         * the addition of a check to see if the match occurred
         * using all of the input.
         */
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (matcher.acceptMode == Matcher.ENDANCHOR && i != matcher.to)
                return false;
            matcher.last = i;
            matcher.groups[0] = matcher.first;
            matcher.groups[1] = matcher.last;
            return true;
        }
    }

    /**
     * Used for REs that can start anywhere within the input string.
     * This basically tries to match repeatedly at each spot in the
     * input string, moving forward after each try. An anchored search
     * or a BnM will bypass this node completely.
     */
    static class Start extends Node {
        int minLength;
        Start(Node node) {
            this.next = node;
            TreeInfo info = new TreeInfo();
            next.study(info);
            minLength = info.minLength;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i > matcher.to - minLength) {
                matcher.hitEnd = true;
                return false;
            }
            int guard = matcher.to - minLength;
            for (; i <= guard; i++) {
                if (next.match(matcher, i, seq)) {
                    matcher.first = i;
                    matcher.groups[0] = matcher.first;
                    matcher.groups[1] = matcher.last;
                    return true;
                }
            }
            matcher.hitEnd = true;
            return false;
        }
        boolean study(TreeInfo info) {
            next.study(info);
            info.maxValid = false;
            info.deterministic = false;
            return false;
        }
    }

    /*
     * StartS supports supplementary characters, including unpaired surrogates.
     */
    static final class StartS extends Start {
        StartS(Node node) {
            super(node);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i > matcher.to - minLength) {
                matcher.hitEnd = true;
                return false;
            }
            int guard = matcher.to - minLength;
            while (i <= guard) {
                //if ((ret = next.match(matcher, i, seq)) || i == guard)
                if (next.match(matcher, i, seq)) {
                    matcher.first = i;
                    matcher.groups[0] = matcher.first;
                    matcher.groups[1] = matcher.last;
                    return true;
                }
                if (i == guard)
                    break;
                // Optimization to move to the next character. This is
                // faster than countChars(seq, i, 1).
                if (Character.isHighSurrogate(seq.charAt(i++))) {
                    if (i < seq.length() &&
                        Character.isLowSurrogate(seq.charAt(i))) {
                        i++;
                    }
                }
            }
            matcher.hitEnd = true;
            return false;
        }
    }

    /**
     * Node to anchor at the beginning of input. This object implements the
     * match for a \A sequence, and the caret anchor will use this if not in
     * multiline mode.
     */
    static final class Begin extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int fromIndex = (matcher.anchoringBounds) ?
                matcher.from : 0;
            if (i == fromIndex && next.match(matcher, i, seq)) {
                matcher.first = i;
                matcher.groups[0] = i;
                matcher.groups[1] = matcher.last;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Node to anchor at the end of input. This is the absolute end, so this
     * should not match at the last newline before the end as $ will.
     */
    static final class End extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int endIndex = (matcher.anchoringBounds) ?
                matcher.to : matcher.getTextLength();
            if (i == endIndex) {
                matcher.hitEnd = true;
                return next.match(matcher, i, seq);
            }
            return false;
        }
    }

    /**
     * Node to anchor at the beginning of a line. This is essentially the
     * object to match for the multiline ^.
     */
    static final class Caret extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int startIndex = matcher.from;
            int endIndex = matcher.to;
            if (!matcher.anchoringBounds) {
                startIndex = 0;
                endIndex = matcher.getTextLength();
            }
            // Perl does not match ^ at end of input even after newline
            if (i == endIndex) {
                matcher.hitEnd = true;
                return false;
            }
            if (i > startIndex) {
                char ch = seq.charAt(i-1);
                if (ch != '\n' && ch != '\r'
                    && (ch|1) != '\u2029'
                    && ch != '\u0085' ) {
                    return false;
                }
                // Should treat /r/n as one newline
                if (ch == '\r' && seq.charAt(i) == '\n')
                    return false;
            }
            return next.match(matcher, i, seq);
        }
    }

    /**
     * Node to anchor at the beginning of a line when in unixdot mode.
     */
    static final class UnixCaret extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int startIndex = matcher.from;
            int endIndex = matcher.to;
            if (!matcher.anchoringBounds) {
                startIndex = 0;
                endIndex = matcher.getTextLength();
            }
            // Perl does not match ^ at end of input even after newline
            if (i == endIndex) {
                matcher.hitEnd = true;
                return false;
            }
            if (i > startIndex) {
                char ch = seq.charAt(i-1);
                if (ch != '\n') {
                    return false;
                }
            }
            return next.match(matcher, i, seq);
        }
    }

    /**
     * Node to match the location where the last match ended.
     * This is used for the \G construct.
     */
    static final class LastMatch extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i != matcher.oldLast)
                return false;
            return next.match(matcher, i, seq);
        }
    }

    /**
     * Node to anchor at the end of a line or the end of input based on the
     * multiline mode.
     *
     * When not in multiline mode, the $ can only match at the very end
     * of the input, unless the input ends in a line terminator in which
     * it matches right before the last line terminator.
     *
     * Note that \r\n is considered an atomic line terminator.
     *
     * Like ^ the $ operator matches at a position, it does not match the
     * line terminators themselves.
     */
    static final class Dollar extends Node {
        boolean multiline;
        Dollar(boolean mul) {
            multiline = mul;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int endIndex = (matcher.anchoringBounds) ?
                matcher.to : matcher.getTextLength();
            if (!multiline) {
                if (i < endIndex - 2)
                    return false;
                if (i == endIndex - 2) {
                    char ch = seq.charAt(i);
                    if (ch != '\r')
                        return false;
                    ch = seq.charAt(i + 1);
                    if (ch != '\n')
                        return false;
                }
            }
            // Matches before any line terminator; also matches at the
            // end of input
            // Before line terminator:
            // If multiline, we match here no matter what
            // If not multiline, fall through so that the end
            // is marked as hit; this must be a /r/n or a /n
            // at the very end so the end was hit; more input
            // could make this not match here
            if (i < endIndex) {
                char ch = seq.charAt(i);
                 if (ch == '\n') {
                     // No match between \r\n
                     if (i > 0 && seq.charAt(i-1) == '\r')
                         return false;
                     if (multiline)
                         return next.match(matcher, i, seq);
                 } else if (ch == '\r' || ch == '\u0085' ||
                            (ch|1) == '\u2029') {
                     if (multiline)
                         return next.match(matcher, i, seq);
                 } else { // No line terminator, no match
                     return false;
                 }
            }
            // Matched at current end so hit end
            matcher.hitEnd = true;
            // If a $ matches because of end of input, then more input
            // could cause it to fail!
            matcher.requireEnd = true;
            return next.match(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            next.study(info);
            return info.deterministic;
        }
    }

    /**
     * Node to anchor at the end of a line or the end of input based on the
     * multiline mode when in unix lines mode.
     */
    static final class UnixDollar extends Node {
        boolean multiline;
        UnixDollar(boolean mul) {
            multiline = mul;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int endIndex = (matcher.anchoringBounds) ?
                matcher.to : matcher.getTextLength();
            if (i < endIndex) {
                char ch = seq.charAt(i);
                if (ch == '\n') {
                    // If not multiline, then only possible to
                    // match at very end or one before end
                    if (multiline == false && i != endIndex - 1)
                        return false;
                    // If multiline return next.match without setting
                    // matcher.hitEnd
                    if (multiline)
                        return next.match(matcher, i, seq);
                } else {
                    return false;
                }
            }
            // Matching because at the end or 1 before the end;
            // more input could change this so set hitEnd
            matcher.hitEnd = true;
            // If a $ matches because of end of input, then more input
            // could cause it to fail!
            matcher.requireEnd = true;
            return next.match(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            next.study(info);
            return info.deterministic;
        }
    }

    /**
     * Node class that matches a Unicode line ending '\R'
     */
    static final class LineEnding extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            // (u+000Du+000A|[u+000Au+000Bu+000Cu+000Du+0085u+2028u+2029])
            if (i < matcher.to) {
                int ch = seq.charAt(i);
                if (ch == 0x0A || ch == 0x0B || ch == 0x0C ||
                    ch == 0x85 || ch == 0x2028 || ch == 0x2029)
                    return next.match(matcher, i + 1, seq);
                if (ch == 0x0D) {
                    i++;
                    if (i < matcher.to) {
                        if (seq.charAt(i) == 0x0A &&
                            next.match(matcher, i + 1, seq)) {
                            return true;
                        }
                    } else {
                        matcher.hitEnd = true;
                    }
                    return next.match(matcher, i, seq);
                }
            } else {
                matcher.hitEnd = true;
            }
            return false;
        }
        boolean study(TreeInfo info) {
            info.minLength++;
            info.maxLength += 2;
            return next.study(info);
        }
    }

    /**
     * Abstract node class to match one character satisfying some
     * boolean property.
     */
    static class CharProperty extends Node {
        final CharPredicate predicate;

        CharProperty (CharPredicate predicate) {
            this.predicate = predicate;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i < matcher.to) {
                int ch = Character.codePointAt(seq, i);
                i += Character.charCount(ch);
                if (i <= matcher.to) {
                    return predicate.is(ch) &&
                           next.match(matcher, i, seq);
                }
            }
            matcher.hitEnd = true;
            return false;
        }
        boolean study(TreeInfo info) {
            info.minLength++;
            info.maxLength++;
            return next.study(info);
        }
    }

    /**
     * Optimized version of CharProperty that works only for
     * properties never satisfied by Supplementary characters.
     */
    private static class BmpCharProperty extends CharProperty {
        BmpCharProperty (BmpCharPredicate predicate) {
            super(predicate);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i < matcher.to) {
                return predicate.is(seq.charAt(i)) &&
                       next.match(matcher, i + 1, seq);
            } else {
                matcher.hitEnd = true;
                return false;
            }
        }
    }

    private static class NFCCharProperty extends Node {
        CharPredicate predicate;
        NFCCharProperty (CharPredicate predicate) {
            this.predicate = predicate;
        }

        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i < matcher.to) {
                int ch0 = Character.codePointAt(seq, i);
                int n = Character.charCount(ch0);
                int j = Grapheme.nextBoundary(seq, i, matcher.to);
                if (i + n == j) { // single cp grapheme, assume nfc
                    if (predicate.is(ch0))
                        return next.match(matcher, j, seq);
                } else {
                    while (i + n < j) {
                        String nfc = Normalizer.normalize(
                            seq.toString().substring(i, j), Normalizer.Form.NFC);
                        if (nfc.codePointCount(0, nfc.length()) == 1) {
                            if (predicate.is(nfc.codePointAt(0)) &&
                                next.match(matcher, j, seq)) {
                                return true;
                            }
                        }

                        ch0 = Character.codePointBefore(seq, j);
                        j -= Character.charCount(ch0);
                    }
                }
                if (j < matcher.to)
                    return false;
            }
            matcher.hitEnd = true;
            return false;
        }

        boolean study(TreeInfo info) {
            info.minLength++;
            info.deterministic = false;
            return next.study(info);
        }
    }

    /**
     * Node class that matches an unicode extended grapheme cluster
     */
    static class XGrapheme extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (i < matcher.to) {
                i = Grapheme.nextBoundary(seq, i, matcher.to);
                return next.match(matcher, i, seq);
            }
            matcher.hitEnd = true;
            return false;
        }

        boolean study(TreeInfo info) {
            info.minLength++;
            info.deterministic = false;
            return next.study(info);
        }
    }

    /**
     * Node class that handles grapheme boundaries
     */
    static class GraphemeBound extends Node {
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int startIndex = matcher.from;
            int endIndex = matcher.to;
            if (matcher.transparentBounds) {
                startIndex = 0;
                endIndex = matcher.getTextLength();
            }
            if (i == startIndex) {
                // continue with return below
            } else if (i < endIndex) {
                if (Character.isSurrogatePair(seq.charAt(i - 1), seq.charAt(i))) {
                    return false;
                }
                if (Grapheme.nextBoundary(seq, matcher.last, endIndex) > i) {
                    return false;
                }
            } else {
                matcher.hitEnd = true;
                matcher.requireEnd = true;
            }
            return next.match(matcher, i, seq);
        }
    }

    /**
     * Base class for all Slice nodes
     */
    static class SliceNode extends Node {
        int[] buffer;
        SliceNode(int[] buf) {
            buffer = buf;
        }
        boolean study(TreeInfo info) {
            info.minLength += buffer.length;
            info.maxLength += buffer.length;
            return next.study(info);
        }
    }

    /**
     * Node class for a case sensitive/BMP-only sequence of literal
     * characters.
     */
    static class Slice extends SliceNode {
        Slice(int[] buf) {
            super(buf);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] buf = buffer;
            int len = buf.length;
            for (int j=0; j<len; j++) {
                if ((i+j) >= matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                if (buf[j] != seq.charAt(i+j))
                    return false;
            }
            return next.match(matcher, i+len, seq);
        }
    }

    /**
     * Node class for a case_insensitive/BMP-only sequence of literal
     * characters.
     */
    static class SliceI extends SliceNode {
        SliceI(int[] buf) {
            super(buf);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] buf = buffer;
            int len = buf.length;
            for (int j=0; j<len; j++) {
                if ((i+j) >= matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                int c = seq.charAt(i+j);
                if (buf[j] != c &&
                    buf[j] != ASCII.toLower(c))
                    return false;
            }
            return next.match(matcher, i+len, seq);
        }
    }

    /**
     * Node class for a unicode_case_insensitive/BMP-only sequence of
     * literal characters. Uses unicode case folding.
     */
    static final class SliceU extends SliceNode {
        SliceU(int[] buf) {
            super(buf);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] buf = buffer;
            int len = buf.length;
            for (int j=0; j<len; j++) {
                if ((i+j) >= matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                int c = seq.charAt(i+j);
                if (buf[j] != c &&
                    buf[j] != Character.toLowerCase(Character.toUpperCase(c)))
                    return false;
            }
            return next.match(matcher, i+len, seq);
        }
    }

    /**
     * Node class for a case sensitive sequence of literal characters
     * including supplementary characters.
     */
    static final class SliceS extends Slice {
        SliceS(int[] buf) {
            super(buf);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] buf = buffer;
            int x = i;
            for (int j = 0; j < buf.length; j++) {
                if (x >= matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                int c = Character.codePointAt(seq, x);
                if (buf[j] != c)
                    return false;
                x += Character.charCount(c);
                if (x > matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
            }
            return next.match(matcher, x, seq);
        }
    }

    /**
     * Node class for a case insensitive sequence of literal characters
     * including supplementary characters.
     */
    static class SliceIS extends SliceNode {
        SliceIS(int[] buf) {
            super(buf);
        }
        int toLower(int c) {
            return ASCII.toLower(c);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] buf = buffer;
            int x = i;
            for (int j = 0; j < buf.length; j++) {
                if (x >= matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                int c = Character.codePointAt(seq, x);
                if (buf[j] != c && buf[j] != toLower(c))
                    return false;
                x += Character.charCount(c);
                if (x > matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
            }
            return next.match(matcher, x, seq);
        }
    }

    /**
     * Node class for a case insensitive sequence of literal characters.
     * Uses unicode case folding.
     */
    static final class SliceUS extends SliceIS {
        SliceUS(int[] buf) {
            super(buf);
        }
        int toLower(int c) {
            return Character.toLowerCase(Character.toUpperCase(c));
        }
    }

    /**
     * The 0 or 1 quantifier. This one class implements all three types.
     */
    static final class Ques extends Node {
        Node atom;
        Qtype type;
        Ques(Node node, Qtype type) {
            this.atom = node;
            this.type = type;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            switch (type) {
            case GREEDY:
                return (atom.match(matcher, i, seq) && next.match(matcher, matcher.last, seq))
                    || next.match(matcher, i, seq);
            case LAZY:
                return next.match(matcher, i, seq)
                    || (atom.match(matcher, i, seq) && next.match(matcher, matcher.last, seq));
            case POSSESSIVE:
                if (atom.match(matcher, i, seq)) i = matcher.last;
                return next.match(matcher, i, seq);
            default:
                return atom.match(matcher, i, seq) && next.match(matcher, matcher.last, seq);
            }
        }
        boolean study(TreeInfo info) {
            if (type != Qtype.INDEPENDENT) {
                int minL = info.minLength;
                atom.study(info);
                info.minLength = minL;
                info.deterministic = false;
                return next.study(info);
            } else {
                atom.study(info);
                return next.study(info);
            }
        }
    }

    /**
     * Handles the greedy style repetition with the specified minimum
     * and the maximum equal to MAX_REPS, for *, + and {N,} quantifiers.
     */
    static class CharPropertyGreedy extends Node {
        final CharPredicate predicate;
        final int cmin;

        CharPropertyGreedy(CharProperty cp, int cmin) {
            this.predicate = cp.predicate;
            this.cmin = cmin;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int starti = i;
            int n = 0;
            int to = matcher.to;
            // greedy, all the way down
            while (i < to) {
                int ch = Character.codePointAt(seq, i);
                int len = Character.charCount(ch);
                if (i + len > to) {
                    // the region cut off the high half of a surrogate pair
                    matcher.hitEnd = true;
                    ch = seq.charAt(i);
                    len = 1;
                }
                if (!predicate.is(ch))
                    break;
                i += len;
                n++;
            }
            if (i >= to) {
                matcher.hitEnd = true;
            }
            while (n >= cmin) {
                if (next.match(matcher, i, seq))
                    return true;
                if (n == cmin)
                    return false;
                // backing off if match fails
                int ch = Character.codePointBefore(seq, i);
                // check if the region cut off the low half of a surrogate pair
                i = Math.max(starti, i - Character.charCount(ch));
                n--;
            }
            return false;
        }

        boolean study(TreeInfo info) {
            info.minLength += cmin;
            if (info.maxValid) {
                info.maxLength += MAX_REPS;
            }
            info.deterministic = false;
            return next.study(info);
        }
    }

    static final class BmpCharPropertyGreedy extends CharPropertyGreedy {

        BmpCharPropertyGreedy(BmpCharProperty bcp, int cmin) {
            super(bcp, cmin);
        }

        boolean match(Matcher matcher, int i, CharSequence seq) {
            int n = 0;
            int to = matcher.to;
            while (i < to && predicate.is(seq.charAt(i))) {
                i++; n++;
            }
            if (i >= to) {
                matcher.hitEnd = true;
            }
            while (n >= cmin) {
                if (next.match(matcher, i, seq))
                    return true;
                i--; n--;  // backing off if match fails
            }
            return false;
        }
    }

    /**
     * Handles the curly-brace style repetition with a specified minimum and
     * maximum occurrences. The * quantifier is handled as a special case.
     * This class handles the three types.
     */
    static final class Curly extends Node {
        Node atom;
        Qtype type;
        int cmin;
        int cmax;

        Curly(Node node, int cmin, int cmax, Qtype type) {
            this.atom = node;
            this.type = type;
            this.cmin = cmin;
            this.cmax = cmax;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int j;
            for (j = 0; j < cmin; j++) {
                if (atom.match(matcher, i, seq)) {
                    i = matcher.last;
                    continue;
                }
                return false;
            }
            if (type == Qtype.GREEDY)
                return match0(matcher, i, j, seq);
            else if (type == Qtype.LAZY)
                return match1(matcher, i, j, seq);
            else
                return match2(matcher, i, j, seq);
        }
        // Greedy match.
        // i is the index to start matching at
        // j is the number of atoms that have matched
        boolean match0(Matcher matcher, int i, int j, CharSequence seq) {
            if (j >= cmax) {
                // We have matched the maximum... continue with the rest of
                // the regular expression
                return next.match(matcher, i, seq);
            }
            int backLimit = j;
            while (atom.match(matcher, i, seq)) {
                // k is the length of this match
                int k = matcher.last - i;
                if (k == 0) // Zero length match
                    break;
                // Move up index and number matched
                i = matcher.last;
                j++;
                // We are greedy so match as many as we can
                while (j < cmax) {
                    if (!atom.match(matcher, i, seq))
                        break;
                    if (i + k != matcher.last) {
                        if (match0(matcher, matcher.last, j+1, seq))
                            return true;
                        break;
                    }
                    i += k;
                    j++;
                }
                // Handle backing off if match fails
                while (j >= backLimit) {
                   if (next.match(matcher, i, seq))
                        return true;
                    i -= k;
                    j--;
                }
                return false;
            }
            return next.match(matcher, i, seq);
        }
        // Reluctant match. At this point, the minimum has been satisfied.
        // i is the index to start matching at
        // j is the number of atoms that have matched
        boolean match1(Matcher matcher, int i, int j, CharSequence seq) {
            for (;;) {
                // Try finishing match without consuming any more
                if (next.match(matcher, i, seq))
                    return true;
                // At the maximum, no match found
                if (j >= cmax)
                    return false;
                // Okay, must try one more atom
                if (!atom.match(matcher, i, seq))
                    return false;
                // If we haven't moved forward then must break out
                if (i == matcher.last)
                    return false;
                // Move up index and number matched
                i = matcher.last;
                j++;
            }
        }
        boolean match2(Matcher matcher, int i, int j, CharSequence seq) {
            for (; j < cmax; j++) {
                if (!atom.match(matcher, i, seq))
                    break;
                if (i == matcher.last)
                    break;
                i = matcher.last;
            }
            return next.match(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            // Save original info
            int minL = info.minLength;
            int maxL = info.maxLength;
            boolean maxV = info.maxValid;
            boolean detm = info.deterministic;
            info.reset();

            atom.study(info);

            int temp = info.minLength * cmin + minL;
            if (temp < minL) {
                temp = 0xFFFFFFF; // arbitrary large number
            }
            info.minLength = temp;

            if (maxV & info.maxValid) {
                temp = info.maxLength * cmax + maxL;
                info.maxLength = temp;
                if (temp < maxL) {
                    info.maxValid = false;
                }
            } else {
                info.maxValid = false;
            }

            if (info.deterministic && cmin == cmax)
                info.deterministic = detm;
            else
                info.deterministic = false;
            return next.study(info);
        }
    }

    /**
     * Handles the curly-brace style repetition with a specified minimum and
     * maximum occurrences in deterministic cases. This is an iterative
     * optimization over the Prolog and Loop system which would handle this
     * in a recursive way. The * quantifier is handled as a special case.
     * If capture is true then this class saves group settings and ensures
     * that groups are unset when backing off of a group match.
     */
    static final class GroupCurly extends Node {
        Node atom;
        Qtype type;
        int cmin;
        int cmax;
        int localIndex;
        int groupIndex;
        boolean capture;

        GroupCurly(Node node, int cmin, int cmax, Qtype type, int local,
                   int group, boolean capture) {
            this.atom = node;
            this.type = type;
            this.cmin = cmin;
            this.cmax = cmax;
            this.localIndex = local;
            this.groupIndex = group;
            this.capture = capture;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] groups = matcher.groups;
            int[] locals = matcher.locals;
            int save0 = locals[localIndex];
            int save1 = 0;
            int save2 = 0;

            if (capture) {
                save1 = groups[groupIndex];
                save2 = groups[groupIndex+1];
            }

            // Notify GroupTail there is no need to setup group info
            // because it will be set here
            locals[localIndex] = -1;

            boolean ret = true;
            for (int j = 0; j < cmin; j++) {
                if (atom.match(matcher, i, seq)) {
                    if (capture) {
                        groups[groupIndex] = i;
                        groups[groupIndex+1] = matcher.last;
                    }
                    i = matcher.last;
                } else {
                    ret = false;
                    break;
                }
            }
            if (ret) {
                if (type == Qtype.GREEDY) {
                    ret = match0(matcher, i, cmin, seq);
                } else if (type == Qtype.LAZY) {
                    ret = match1(matcher, i, cmin, seq);
                } else {
                    ret = match2(matcher, i, cmin, seq);
                }
            }
            if (!ret) {
                locals[localIndex] = save0;
                if (capture) {
                    groups[groupIndex] = save1;
                    groups[groupIndex+1] = save2;
                }
            }
            return ret;
        }
        // Aggressive group match
        boolean match0(Matcher matcher, int i, int j, CharSequence seq) {
            // don't back off passing the starting "j"
            int min = j;
            int[] groups = matcher.groups;
            int save0 = 0;
            int save1 = 0;
            if (capture) {
                save0 = groups[groupIndex];
                save1 = groups[groupIndex+1];
            }
            for (;;) {
                if (j >= cmax)
                    break;
                if (!atom.match(matcher, i, seq))
                    break;
                int k = matcher.last - i;
                if (k <= 0) {
                    if (capture) {
                        groups[groupIndex] = i;
                        groups[groupIndex+1] = i + k;
                    }
                    i = i + k;
                    break;
                }
                for (;;) {
                    if (capture) {
                        groups[groupIndex] = i;
                        groups[groupIndex+1] = i + k;
                    }
                    i = i + k;
                    if (++j >= cmax)
                        break;
                    if (!atom.match(matcher, i, seq))
                        break;
                    if (i + k != matcher.last) {
                        if (match0(matcher, i, j, seq))
                            return true;
                        break;
                    }
                }
                while (j > min) {
                    if (next.match(matcher, i, seq)) {
                        if (capture) {
                            groups[groupIndex+1] = i;
                            groups[groupIndex] = i - k;
                        }
                        return true;
                    }
                    // backing off
                    i = i - k;
                    if (capture) {
                        groups[groupIndex+1] = i;
                        groups[groupIndex] = i - k;
                    }
                    j--;

                }
                break;
            }
            if (capture) {
                groups[groupIndex] = save0;
                groups[groupIndex+1] = save1;
            }
            return next.match(matcher, i, seq);
        }
        // Reluctant matching
        boolean match1(Matcher matcher, int i, int j, CharSequence seq) {
            for (;;) {
                if (next.match(matcher, i, seq))
                    return true;
                if (j >= cmax)
                    return false;
                if (!atom.match(matcher, i, seq))
                    return false;
                if (i == matcher.last)
                    return false;
                if (capture) {
                    matcher.groups[groupIndex] = i;
                    matcher.groups[groupIndex+1] = matcher.last;
                }
                i = matcher.last;
                j++;
            }
        }
        // Possessive matching
        boolean match2(Matcher matcher, int i, int j, CharSequence seq) {
            for (; j < cmax; j++) {
                if (!atom.match(matcher, i, seq)) {
                    break;
                }
                if (capture) {
                    matcher.groups[groupIndex] = i;
                    matcher.groups[groupIndex+1] = matcher.last;
                }
                if (i == matcher.last) {
                    break;
                }
                i = matcher.last;
            }
            return next.match(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            // Save original info
            int minL = info.minLength;
            int maxL = info.maxLength;
            boolean maxV = info.maxValid;
            boolean detm = info.deterministic;
            info.reset();

            atom.study(info);

            int temp = info.minLength * cmin + minL;
            if (temp < minL) {
                temp = 0xFFFFFFF; // Arbitrary large number
            }
            info.minLength = temp;

            if (maxV & info.maxValid) {
                temp = info.maxLength * cmax + maxL;
                info.maxLength = temp;
                if (temp < maxL) {
                    info.maxValid = false;
                }
            } else {
                info.maxValid = false;
            }

            if (info.deterministic && cmin == cmax) {
                info.deterministic = detm;
            } else {
                info.deterministic = false;
            }
            return next.study(info);
        }
    }

    /**
     * A Guard node at the end of each atom node in a Branch. It
     * serves the purpose of chaining the "match" operation to
     * "next" but not the "study", so we can collect the TreeInfo
     * of each atom node without including the TreeInfo of the
     * "next".
     */
    static final class BranchConn extends Node {
        BranchConn() {}
        boolean match(Matcher matcher, int i, CharSequence seq) {
            return next.match(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            return info.deterministic;
        }
    }

    /**
     * Handles the branching of alternations. Note this is also used for
     * the ? quantifier to branch between the case where it matches once
     * and where it does not occur.
     */
    static final class Branch extends Node {
        Node[] atoms = new Node[2];
        int size = 2;
        Node conn;
        Branch(Node first, Node second, Node branchConn) {
            conn = branchConn;
            atoms[0] = first;
            atoms[1] = second;
        }

        void add(Node node) {
            if (size >= atoms.length) {
                Node[] tmp = new Node[atoms.length*2];
                System.arraycopy(atoms, 0, tmp, 0, atoms.length);
                atoms = tmp;
            }
            atoms[size++] = node;
        }

        boolean match(Matcher matcher, int i, CharSequence seq) {
            for (int n = 0; n < size; n++) {
                if (atoms[n] == null) {
                    if (conn.next.match(matcher, i, seq))
                        return true;
                } else if (atoms[n].match(matcher, i, seq)) {
                    return true;
                }
            }
            return false;
        }

        boolean study(TreeInfo info) {
            int minL = info.minLength;
            int maxL = info.maxLength;
            boolean maxV = info.maxValid;

            int minL2 = Integer.MAX_VALUE; //arbitrary large enough num
            int maxL2 = -1;
            for (int n = 0; n < size; n++) {
                info.reset();
                if (atoms[n] != null)
                    atoms[n].study(info);
                minL2 = Math.min(minL2, info.minLength);
                maxL2 = Math.max(maxL2, info.maxLength);
                maxV = (maxV & info.maxValid);
            }

            minL += minL2;
            maxL += maxL2;

            info.reset();
            conn.next.study(info);

            info.minLength += minL;
            info.maxLength += maxL;
            info.maxValid &= maxV;
            info.deterministic = false;
            return false;
        }
    }

    /**
     * The GroupHead saves the location where the group begins in the locals
     * and restores them when the match is done.
     *
     * The matchRef is used when a reference to this group is accessed later
     * in the expression. The locals will have a negative value in them to
     * indicate that we do not want to unset the group if the reference
     * doesn't match.
     */
    static final class GroupHead extends Node {
        int localIndex;
        GroupTail tail;    // for debug/print only, match does not need to know
        GroupHead(int localCount) {
            localIndex = localCount;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int save = matcher.locals[localIndex];
            matcher.locals[localIndex] = i;
            boolean ret = next.match(matcher, i, seq);
            matcher.locals[localIndex] = save;
            return ret;
        }
    }

    /**
     * The GroupTail handles the setting of group beginning and ending
     * locations when groups are successfully matched. It must also be able to
     * unset groups that have to be backed off of.
     *
     * The GroupTail node is also used when a previous group is referenced,
     * and in that case no group information needs to be set.
     */
    static final class GroupTail extends Node {
        int localIndex;
        int groupIndex;
        GroupTail(int localCount, int groupCount) {
            localIndex = localCount;
            groupIndex = groupCount + groupCount;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int tmp = matcher.locals[localIndex];
            if (tmp >= 0) { // This is the normal group case.
                // Save the group so we can unset it if it
                // backs off of a match.
                int groupStart = matcher.groups[groupIndex];
                int groupEnd = matcher.groups[groupIndex+1];

                matcher.groups[groupIndex] = tmp;
                matcher.groups[groupIndex+1] = i;
                if (next.match(matcher, i, seq)) {
                    return true;
                }
                matcher.groups[groupIndex] = groupStart;
                matcher.groups[groupIndex+1] = groupEnd;
                return false;
            } else {
                // This is a group reference case. We don't need to save any
                // group info because it isn't really a group.
                matcher.last = i;
                return true;
            }
        }
    }

    /**
     * This sets up a loop to handle a recursive quantifier structure.
     */
    static final class Prolog extends Node {
        Loop loop;
        Prolog(Loop loop) {
            this.loop = loop;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            return loop.matchInit(matcher, i, seq);
        }
        boolean study(TreeInfo info) {
            return loop.study(info);
        }
    }

    /**
     * Handles the repetition count for a greedy Curly. The matchInit
     * is called from the Prolog to save the index of where the group
     * beginning is stored. A zero length group check occurs in the
     * normal match but is skipped in the matchInit.
     */
    static class Loop extends Node {
        Node body;
        int countIndex; // local count index in matcher locals
        int beginIndex; // group beginning index
        int cmin, cmax;
        int posIndex;
        Loop(int countIndex, int beginIndex) {
            this.countIndex = countIndex;
            this.beginIndex = beginIndex;
            this.posIndex = -1;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            // Avoid infinite loop in zero-length case.
            if (i > matcher.locals[beginIndex]) {
                int count = matcher.locals[countIndex];

                // This block is for before we reach the minimum
                // iterations required for the loop to match
                if (count < cmin) {
                    matcher.locals[countIndex] = count + 1;
                    boolean b = body.match(matcher, i, seq);
                    // If match failed we must backtrack, so
                    // the loop count should NOT be incremented
                    if (!b)
                        matcher.locals[countIndex] = count;
                    // Return success or failure since we are under
                    // minimum
                    return b;
                }
                // This block is for after we have the minimum
                // iterations required for the loop to match
                if (count < cmax) {
                    // Let's check if we have already tried and failed
                    // at this starting position "i" in the past.
                    // If yes, then just return false wihtout trying
                    // again, to stop the exponential backtracking.
                    if (posIndex != -1 &&
                        matcher.localsPos[posIndex].contains(i)) {
                        return next.match(matcher, i, seq);
                    }
                    matcher.locals[countIndex] = count + 1;
                    boolean b = body.match(matcher, i, seq);
                    // If match failed we must backtrack, so
                    // the loop count should NOT be incremented
                    if (b)
                        return true;
                    matcher.locals[countIndex] = count;
                    // save the failed position
                    if (posIndex != -1) {
                        matcher.localsPos[posIndex].add(i);
                    }
                }
            }
            return next.match(matcher, i, seq);
        }
        boolean matchInit(Matcher matcher, int i, CharSequence seq) {
            int save = matcher.locals[countIndex];
            boolean ret;
            if (posIndex != -1 && matcher.localsPos[posIndex] == null) {
                matcher.localsPos[posIndex] = new IntHashSet();
            }
            if (0 < cmin) {
                matcher.locals[countIndex] = 1;
                ret = body.match(matcher, i, seq);
            } else if (0 < cmax) {
                matcher.locals[countIndex] = 1;
                ret = body.match(matcher, i, seq);
                if (ret == false)
                    ret = next.match(matcher, i, seq);
            } else {
                ret = next.match(matcher, i, seq);
            }
            matcher.locals[countIndex] = save;
            return ret;
        }
        boolean study(TreeInfo info) {
            info.maxValid = false;
            info.deterministic = false;
            return false;
        }
    }

    /**
     * Handles the repetition count for a reluctant Curly. The matchInit
     * is called from the Prolog to save the index of where the group
     * beginning is stored. A zero length group check occurs in the
     * normal match but is skipped in the matchInit.
     */
    static final class LazyLoop extends Loop {
        LazyLoop(int countIndex, int beginIndex) {
            super(countIndex, beginIndex);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            // Check for zero length group
            if (i > matcher.locals[beginIndex]) {
                int count = matcher.locals[countIndex];
                if (count < cmin) {
                    matcher.locals[countIndex] = count + 1;
                    boolean result = body.match(matcher, i, seq);
                    // If match failed we must backtrack, so
                    // the loop count should NOT be incremented
                    if (!result)
                        matcher.locals[countIndex] = count;
                    return result;
                }
                if (next.match(matcher, i, seq))
                    return true;
                if (count < cmax) {
                    matcher.locals[countIndex] = count + 1;
                    boolean result = body.match(matcher, i, seq);
                    // If match failed we must backtrack, so
                    // the loop count should NOT be incremented
                    if (!result)
                        matcher.locals[countIndex] = count;
                    return result;
                }
                return false;
            }
            return next.match(matcher, i, seq);
        }
        boolean matchInit(Matcher matcher, int i, CharSequence seq) {
            int save = matcher.locals[countIndex];
            boolean ret = false;
            if (0 < cmin) {
                matcher.locals[countIndex] = 1;
                ret = body.match(matcher, i, seq);
            } else if (next.match(matcher, i, seq)) {
                ret = true;
            } else if (0 < cmax) {
                matcher.locals[countIndex] = 1;
                ret = body.match(matcher, i, seq);
            }
            matcher.locals[countIndex] = save;
            return ret;
        }
        boolean study(TreeInfo info) {
            info.maxValid = false;
            info.deterministic = false;
            return false;
        }
    }

    /**
     * Refers to a group in the regular expression. Attempts to match
     * whatever the group referred to last matched.
     */
    static class BackRef extends Node {
        int groupIndex;
        BackRef(int groupCount) {
            super();
            groupIndex = groupCount + groupCount;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int j = matcher.groups[groupIndex];
            int k = matcher.groups[groupIndex+1];

            int groupSize = k - j;
            // If the referenced group didn't match, neither can this
            if (j < 0)
                return false;

            // If there isn't enough input left no match
            if (i + groupSize > matcher.to) {
                matcher.hitEnd = true;
                return false;
            }
            // Check each new char to make sure it matches what the group
            // referenced matched last time around
            for (int index=0; index<groupSize; index++)
                if (seq.charAt(i+index) != seq.charAt(j+index))
                    return false;

            return next.match(matcher, i+groupSize, seq);
        }
        boolean study(TreeInfo info) {
            info.maxValid = false;
            return next.study(info);
        }
    }

    static class CIBackRef extends Node {
        int groupIndex;
        boolean doUnicodeCase;
        CIBackRef(int groupCount, boolean doUnicodeCase) {
            super();
            groupIndex = groupCount + groupCount;
            this.doUnicodeCase = doUnicodeCase;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int j = matcher.groups[groupIndex];
            int k = matcher.groups[groupIndex+1];

            int groupSize = k - j;

            // If the referenced group didn't match, neither can this
            if (j < 0)
                return false;

            // If there isn't enough input left no match
            if (i + groupSize > matcher.to) {
                matcher.hitEnd = true;
                return false;
            }

            // Check each new char to make sure it matches what the group
            // referenced matched last time around
            int x = i;
            for (int index=0; index<groupSize; index++) {
                int c1 = Character.codePointAt(seq, x);
                int c2 = Character.codePointAt(seq, j);
                if (c1 != c2) {
                    if (doUnicodeCase) {
                        int cc1 = Character.toUpperCase(c1);
                        int cc2 = Character.toUpperCase(c2);
                        if (cc1 != cc2 &&
                            Character.toLowerCase(cc1) !=
                            Character.toLowerCase(cc2))
                            return false;
                    } else {
                        if (ASCII.toLower(c1) != ASCII.toLower(c2))
                            return false;
                    }
                }
                x += Character.charCount(c1);
                j += Character.charCount(c2);
            }

            return next.match(matcher, i+groupSize, seq);
        }
        boolean study(TreeInfo info) {
            info.maxValid = false;
            return next.study(info);
        }
    }

    /**
     * Searches until the next instance of its atom. This is useful for
     * finding the atom efficiently without passing an instance of it
     * (greedy problem) and without a lot of wasted search time (reluctant
     * problem).
     */
    static final class First extends Node {
        Node atom;
        First(Node node) {
            this.atom = BnM.optimize(node);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            if (atom instanceof BnM) {
                return atom.match(matcher, i, seq)
                    && next.match(matcher, matcher.last, seq);
            }
            for (;;) {
                if (i > matcher.to) {
                    matcher.hitEnd = true;
                    return false;
                }
                if (atom.match(matcher, i, seq)) {
                    return next.match(matcher, matcher.last, seq);
                }
                i += countChars(seq, i, 1);
                matcher.first++;
            }
        }
        boolean study(TreeInfo info) {
            atom.study(info);
            info.maxValid = false;
            info.deterministic = false;
            return next.study(info);
        }
    }

    /**
     * Zero width positive lookahead.
     */
    static final class Pos extends Node {
        Node cond;
        Pos(Node cond) {
            this.cond = cond;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int savedTo = matcher.to;
            boolean conditionMatched;

            // Relax transparent region boundaries for lookahead
            if (matcher.transparentBounds)
                matcher.to = matcher.getTextLength();
            try {
                conditionMatched = cond.match(matcher, i, seq);
            } finally {
                // Reinstate region boundaries
                matcher.to = savedTo;
            }
            return conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * Zero width negative lookahead.
     */
    static final class Neg extends Node {
        Node cond;
        Neg(Node cond) {
            this.cond = cond;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int savedTo = matcher.to;
            boolean conditionMatched;

            // Relax transparent region boundaries for lookahead
            if (matcher.transparentBounds)
                matcher.to = matcher.getTextLength();
            try {
                if (i < matcher.to) {
                    conditionMatched = !cond.match(matcher, i, seq);
                } else {
                    // If a negative lookahead succeeds then more input
                    // could cause it to fail!
                    matcher.requireEnd = true;
                    conditionMatched = !cond.match(matcher, i, seq);
                }
            } finally {
                // Reinstate region boundaries
                matcher.to = savedTo;
            }
            return conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * For use with lookbehinds; matches the position where the lookbehind
     * was encountered.
     */
    static class LookBehindEndNode extends Node {
        private LookBehindEndNode() {} // Singleton

        static LookBehindEndNode INSTANCE = new LookBehindEndNode();

        boolean match(Matcher matcher, int i, CharSequence seq) {
            return i == matcher.lookbehindTo;
        }
    }

    /**
     * Zero width positive lookbehind.
     */
    static class Behind extends Node {
        Node cond;
        int rmax, rmin;
        Behind(Node cond, int rmax, int rmin) {
            this.cond = cond;
            this.rmax = rmax;
            this.rmin = rmin;
        }

        boolean match(Matcher matcher, int i, CharSequence seq) {
            int savedFrom = matcher.from;
            boolean conditionMatched = false;
            int startIndex = (!matcher.transparentBounds) ?
                             matcher.from : 0;
            int from = Math.max(i - rmax, startIndex);
            // Set end boundary
            int savedLBT = matcher.lookbehindTo;
            matcher.lookbehindTo = i;
            // Relax transparent region boundaries for lookbehind
            if (matcher.transparentBounds)
                matcher.from = 0;
            for (int j = i - rmin; !conditionMatched && j >= from; j--) {
                conditionMatched = cond.match(matcher, j, seq);
            }
            matcher.from = savedFrom;
            matcher.lookbehindTo = savedLBT;
            return conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * Zero width positive lookbehind, including supplementary
     * characters or unpaired surrogates.
     */
    static final class BehindS extends Behind {
        BehindS(Node cond, int rmax, int rmin) {
            super(cond, rmax, rmin);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int rmaxChars = countChars(seq, i, -rmax);
            int rminChars = countChars(seq, i, -rmin);
            int savedFrom = matcher.from;
            int startIndex = (!matcher.transparentBounds) ?
                             matcher.from : 0;
            boolean conditionMatched = false;
            int from = Math.max(i - rmaxChars, startIndex);
            // Set end boundary
            int savedLBT = matcher.lookbehindTo;
            matcher.lookbehindTo = i;
            // Relax transparent region boundaries for lookbehind
            if (matcher.transparentBounds)
                matcher.from = 0;

            for (int j = i - rminChars;
                 !conditionMatched && j >= from;
                 j -= j>from ? countChars(seq, j, -1) : 1) {
                conditionMatched = cond.match(matcher, j, seq);
            }
            matcher.from = savedFrom;
            matcher.lookbehindTo = savedLBT;
            return conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * Zero width negative lookbehind.
     */
    static class NotBehind extends Node {
        Node cond;
        int rmax, rmin;
        NotBehind(Node cond, int rmax, int rmin) {
            this.cond = cond;
            this.rmax = rmax;
            this.rmin = rmin;
        }

        boolean match(Matcher matcher, int i, CharSequence seq) {
            int savedLBT = matcher.lookbehindTo;
            int savedFrom = matcher.from;
            boolean conditionMatched = false;
            int startIndex = (!matcher.transparentBounds) ?
                             matcher.from : 0;
            int from = Math.max(i - rmax, startIndex);
            matcher.lookbehindTo = i;
            // Relax transparent region boundaries for lookbehind
            if (matcher.transparentBounds)
                matcher.from = 0;
            for (int j = i - rmin; !conditionMatched && j >= from; j--) {
                conditionMatched = cond.match(matcher, j, seq);
            }
            // Reinstate region boundaries
            matcher.from = savedFrom;
            matcher.lookbehindTo = savedLBT;
            return !conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * Zero width negative lookbehind, including supplementary
     * characters or unpaired surrogates.
     */
    static final class NotBehindS extends NotBehind {
        NotBehindS(Node cond, int rmax, int rmin) {
            super(cond, rmax, rmin);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int rmaxChars = countChars(seq, i, -rmax);
            int rminChars = countChars(seq, i, -rmin);
            int savedFrom = matcher.from;
            int savedLBT = matcher.lookbehindTo;
            boolean conditionMatched = false;
            int startIndex = (!matcher.transparentBounds) ?
                             matcher.from : 0;
            int from = Math.max(i - rmaxChars, startIndex);
            matcher.lookbehindTo = i;
            // Relax transparent region boundaries for lookbehind
            if (matcher.transparentBounds)
                matcher.from = 0;
            for (int j = i - rminChars;
                 !conditionMatched && j >= from;
                 j -= j>from ? countChars(seq, j, -1) : 1) {
                conditionMatched = cond.match(matcher, j, seq);
            }
            //Reinstate region boundaries
            matcher.from = savedFrom;
            matcher.lookbehindTo = savedLBT;
            return !conditionMatched && next.match(matcher, i, seq);
        }
    }

    /**
     * Handles word boundaries. Includes a field to allow this one class to
     * deal with the different types of word boundaries we can match. The word
     * characters include underscores, letters, and digits. Non spacing marks
     * can are also part of a word if they have a base character, otherwise
     * they are ignored for purposes of finding word boundaries.
     */
    static final class Bound extends Node {
        static int LEFT = 0x1;
        static int RIGHT= 0x2;
        static int BOTH = 0x3;
        static int NONE = 0x4;
        int type;
        boolean useUWORD;
        Bound(int n, boolean useUWORD) {
            type = n;
            this.useUWORD = useUWORD;
        }

        boolean isWord(int ch) {
            return useUWORD ? CharPredicates.WORD().is(ch)
                            : (ch == '_' || Character.isLetterOrDigit(ch));
        }

        int check(Matcher matcher, int i, CharSequence seq) {
            int ch;
            boolean left = false;
            int startIndex = matcher.from;
            int endIndex = matcher.to;
            if (matcher.transparentBounds) {
                startIndex = 0;
                endIndex = matcher.getTextLength();
            }
            if (i > startIndex) {
                ch = Character.codePointBefore(seq, i);
                left = (isWord(ch) ||
                    ((Character.getType(ch) == Character.NON_SPACING_MARK)
                     && hasBaseCharacter(matcher, i-1, seq)));
            }
            boolean right = false;
            if (i < endIndex) {
                ch = Character.codePointAt(seq, i);
                right = (isWord(ch) ||
                    ((Character.getType(ch) == Character.NON_SPACING_MARK)
                     && hasBaseCharacter(matcher, i, seq)));
            } else {
                // Tried to access char past the end
                matcher.hitEnd = true;
                // The addition of another char could wreck a boundary
                matcher.requireEnd = true;
            }
            return ((left ^ right) ? (right ? LEFT : RIGHT) : NONE);
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            return (check(matcher, i, seq) & type) > 0
                && next.match(matcher, i, seq);
        }
    }

    /**
     * Non spacing marks only count as word characters in bounds calculations
     * if they have a base character.
     */
    private static boolean hasBaseCharacter(Matcher matcher, int i,
                                            CharSequence seq)
    {
        int start = (!matcher.transparentBounds) ?
            matcher.from : 0;
        for (int x=i; x >= start; x--) {
            int ch = Character.codePointAt(seq, x);
            if (Character.isLetterOrDigit(ch))
                return true;
            if (Character.getType(ch) == Character.NON_SPACING_MARK)
                continue;
            return false;
        }
        return false;
    }

    /**
     * Attempts to match a slice in the input using the Boyer-Moore string
     * matching algorithm. The algorithm is based on the idea that the
     * pattern can be shifted farther ahead in the search text if it is
     * matched right to left.
     * <p>
     * The pattern is compared to the input one character at a time, from
     * the rightmost character in the pattern to the left. If the characters
     * all match the pattern has been found. If a character does not match,
     * the pattern is shifted right a distance that is the maximum of two
     * functions, the bad character shift and the good suffix shift. This
     * shift moves the attempted match position through the input more
     * quickly than a naive one position at a time check.
     * <p>
     * The bad character shift is based on the character from the text that
     * did not match. If the character does not appear in the pattern, the
     * pattern can be shifted completely beyond the bad character. If the
     * character does occur in the pattern, the pattern can be shifted to
     * line the pattern up with the next occurrence of that character.
     * <p>
     * The good suffix shift is based on the idea that some subset on the right
     * side of the pattern has matched. When a bad character is found, the
     * pattern can be shifted right by the pattern length if the subset does
     * not occur again in pattern, or by the amount of distance to the
     * next occurrence of the subset in the pattern.
     *
     * Boyer-Moore search methods adapted from code by Amy Yu.
     */
    static class BnM extends Node {
        int[] buffer;
        int[] lastOcc;
        int[] optoSft;

        /**
         * Pre calculates arrays needed to generate the bad character
         * shift and the good suffix shift. Only the last seven bits
         * are used to see if chars match; This keeps the tables small
         * and covers the heavily used ASCII range, but occasionally
         * results in an aliased match for the bad character shift.
         */
        static Node optimize(Node node) {
            if (!(node instanceof Slice)) {
                return node;
            }

            int[] src = ((Slice) node).buffer;
            int patternLength = src.length;
            // The BM algorithm requires a bit of overhead;
            // If the pattern is short don't use it, since
            // a shift larger than the pattern length cannot
            // be used anyway.
            if (patternLength < 4) {
                return node;
            }
            int i, j;
            int[] lastOcc = new int[128];
            int[] optoSft = new int[patternLength];
            // Precalculate part of the bad character shift
            // It is a table for where in the pattern each
            // lower 7-bit value occurs
            for (i = 0; i < patternLength; i++) {
                lastOcc[src[i]&0x7F] = i + 1;
            }
            // Precalculate the good suffix shift
            // i is the shift amount being considered
NEXT:       for (i = patternLength; i > 0; i--) {
                // j is the beginning index of suffix being considered
                for (j = patternLength - 1; j >= i; j--) {
                    // Testing for good suffix
                    if (src[j] == src[j-i]) {
                        // src[j..len] is a good suffix
                        optoSft[j-1] = i;
                    } else {
                        // No match. The array has already been
                        // filled up with correct values before.
                        continue NEXT;
                    }
                }
                // This fills up the remaining of optoSft
                // any suffix can not have larger shift amount
                // then its sub-suffix. Why???
                while (j > 0) {
                    optoSft[--j] = i;
                }
            }
            // Set the guard value because of unicode compression
            optoSft[patternLength-1] = 1;
            if (node instanceof SliceS)
                return new BnMS(src, lastOcc, optoSft, node.next);
            return new BnM(src, lastOcc, optoSft, node.next);
        }
        BnM(int[] src, int[] lastOcc, int[] optoSft, Node next) {
            this.buffer = src;
            this.lastOcc = lastOcc;
            this.optoSft = optoSft;
            this.next = next;
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] src = buffer;
            int patternLength = src.length;
            int last = matcher.to - patternLength;

            // Loop over all possible match positions in text
NEXT:       while (i <= last) {
                // Loop over pattern from right to left
                for (int j = patternLength - 1; j >= 0; j--) {
                    int ch = seq.charAt(i+j);
                    if (ch != src[j]) {
                        // Shift search to the right by the maximum of the
                        // bad character shift and the good suffix shift
                        i += Math.max(j + 1 - lastOcc[ch&0x7F], optoSft[j]);
                        continue NEXT;
                    }
                }
                // Entire pattern matched starting at i
                matcher.first = i;
                boolean ret = next.match(matcher, i + patternLength, seq);
                if (ret) {
                    matcher.first = i;
                    matcher.groups[0] = matcher.first;
                    matcher.groups[1] = matcher.last;
                    return true;
                }
                i++;
            }
            // BnM is only used as the leading node in the unanchored case,
            // and it replaced its Start() which always searches to the end
            // if it doesn't find what it's looking for, so hitEnd is true.
            matcher.hitEnd = true;
            return false;
        }
        boolean study(TreeInfo info) {
            info.minLength += buffer.length;
            info.maxValid = false;
            return next.study(info);
        }
    }

    /**
     * Supplementary support version of BnM(). Unpaired surrogates are
     * also handled by this class.
     */
    static final class BnMS extends BnM {
        int lengthInChars;

        BnMS(int[] src, int[] lastOcc, int[] optoSft, Node next) {
            super(src, lastOcc, optoSft, next);
            for (int cp : buffer) {
                lengthInChars += Character.charCount(cp);
            }
        }
        boolean match(Matcher matcher, int i, CharSequence seq) {
            int[] src = buffer;
            int patternLength = src.length;
            int last = matcher.to - lengthInChars;

            // Loop over all possible match positions in text
NEXT:       while (i <= last) {
                // Loop over pattern from right to left
                int ch;
                for (int j = countChars(seq, i, patternLength), x = patternLength - 1;
                     j > 0; j -= Character.charCount(ch), x--) {
                    ch = Character.codePointBefore(seq, i+j);
                    if (ch != src[x]) {
                        // Shift search to the right by the maximum of the
                        // bad character shift and the good suffix shift
                        int n = Math.max(x + 1 - lastOcc[ch&0x7F], optoSft[x]);
                        i += countChars(seq, i, n);
                        continue NEXT;
                    }
                }
                // Entire pattern matched starting at i
                matcher.first = i;
                boolean ret = next.match(matcher, i + lengthInChars, seq);
                if (ret) {
                    matcher.first = i;
                    matcher.groups[0] = matcher.first;
                    matcher.groups[1] = matcher.last;
                    return true;
                }
                i += countChars(seq, i, 1);
            }
            matcher.hitEnd = true;
            return false;
        }
    }

    @FunctionalInterface
    static interface CharPredicate {
        boolean is(int ch);

        default CharPredicate and(CharPredicate p) {
            return ch -> is(ch) && p.is(ch);
        }
        default CharPredicate union(CharPredicate p) {
            return ch -> is(ch) || p.is(ch);
        }
        default CharPredicate union(CharPredicate p1,
                                    CharPredicate p2) {
            return ch -> is(ch) || p1.is(ch) || p2.is(ch);
        }
        default CharPredicate negate() {
            return ch -> !is(ch);
        }
    }

    static interface BmpCharPredicate extends CharPredicate {

        default CharPredicate and(CharPredicate p) {
            if (p instanceof BmpCharPredicate)
                return (BmpCharPredicate)(ch -> is(ch) && p.is(ch));
            return ch -> is(ch) && p.is(ch);
        }
        default CharPredicate union(CharPredicate p) {
            if (p instanceof BmpCharPredicate)
                return (BmpCharPredicate)(ch -> is(ch) || p.is(ch));
            return ch -> is(ch) || p.is(ch);
        }
        static CharPredicate union(CharPredicate... predicates) {
            CharPredicate cp = ch -> {
                for (CharPredicate p : predicates) {
                    if (!p.is(ch))
                        return false;
                }
                return true;
            };
            for (CharPredicate p : predicates) {
                if (! (p instanceof BmpCharPredicate))
                    return cp;
            }
            return (BmpCharPredicate)cp;
        }
    }

    /**
     * matches a Perl vertical whitespace
     */
    static BmpCharPredicate VertWS() {
        return cp -> (cp >= 0x0A && cp <= 0x0D) ||
            cp == 0x85 || cp == 0x2028 || cp == 0x2029;
    }

    /**
     * matches a Perl horizontal whitespace
     */
    static BmpCharPredicate HorizWS() {
        return cp ->
            cp == 0x09 || cp == 0x20 || cp == 0xa0 || cp == 0x1680 ||
            cp == 0x180e || cp >= 0x2000 && cp <= 0x200a ||  cp == 0x202f ||
            cp == 0x205f || cp == 0x3000;
    }

    /**
     *  for the Unicode category ALL and the dot metacharacter when
     *  in dotall mode.
     */
    static CharPredicate ALL() {
        return ch -> true;
    }

    /**
     * for the dot metacharacter when dotall is not enabled.
     */
    static CharPredicate DOT() {
        return ch ->
            (ch != '\n' && ch != '\r'
            && (ch|1) != '\u2029'
            && ch != '\u0085');
    }

    /**
     *  the dot metacharacter when dotall is not enabled but UNIX_LINES is enabled.
     */
    static CharPredicate UNIXDOT() {
        return ch ->  ch != '\n';
    }

    /**
     * Indicate that matches a Supplementary Unicode character
     */
    static CharPredicate SingleS(int c) {
        return ch -> ch == c;
    }

    /**
     * A bmp/optimized predicate of single
     */
    static BmpCharPredicate Single(int c) {
        return ch -> ch == c;
    }

    /**
     * Case insensitive matches a given BMP character
     */
    static BmpCharPredicate SingleI(int lower, int upper) {
        return ch -> ch == lower || ch == upper;
    }

    /**
     * Unicode case insensitive matches a given Unicode character
     */
    static CharPredicate SingleU(int lower) {
        return ch -> lower == ch ||
                     lower == Character.toLowerCase(Character.toUpperCase(ch));
    }

    private static boolean inRange(int lower, int ch, int upper) {
        return lower <= ch && ch <= upper;
    }

    /**
     * Characters within a explicit value range
     */
    static CharPredicate Range(int lower, int upper) {
        if (upper < Character.MIN_HIGH_SURROGATE ||
            lower > Character.MAX_LOW_SURROGATE &&
            upper < Character.MIN_SUPPLEMENTARY_CODE_POINT)
            return (BmpCharPredicate)(ch -> inRange(lower, ch, upper));
        return ch -> inRange(lower, ch, upper);
    }

   /**
    * Characters within a explicit value range in a case insensitive manner.
    */
    static CharPredicate CIRange(int lower, int upper) {
        return ch -> inRange(lower, ch, upper) ||
                     ASCII.isAscii(ch) &&
                     (inRange(lower, ASCII.toUpper(ch), upper) ||
                      inRange(lower, ASCII.toLower(ch), upper));
    }

    static CharPredicate CIRangeU(int lower, int upper) {
        return ch -> {
            if (inRange(lower, ch, upper))
                return true;
            int up = Character.toUpperCase(ch);
            return inRange(lower, up, upper) ||
                   inRange(lower, Character.toLowerCase(up), upper);
        };
    }

    /**
     *  This must be the very first initializer.
     */
    static final Node accept = new Node();

    static final Node lastAccept = new LastNode();

    /**
     * Creates a predicate that tests if this pattern is found in a given input
     * string.
     *
     * @apiNote
     * This method creates a predicate that behaves as if it creates a matcher
     * from the input sequence and then calls {@code find}, for example a
     * predicate of the form:
     * <pre>{@code
     *   s -> matcher(s).find();
     * }</pre>
     *
     * @return  The predicate which can be used for finding a match on a
     *          subsequence of a string
     * @since   1.8
     * @see     Matcher#find
     */
    public Predicate<String> asPredicate() {
        return s -> matcher(s).find();
    }

    /**
     * Creates a predicate that tests if this pattern matches a given input string.
     *
     * @apiNote
     * This method creates a predicate that behaves as if it creates a matcher
     * from the input sequence and then calls {@code matches}, for example a
     * predicate of the form:
     * <pre>{@code
     *   s -> matcher(s).matches();
     * }</pre>
     *
     * @return  The predicate which can be used for matching an input string
     *          against this pattern.
     * @since   11
     * @see     Matcher#matches
     */
    public Predicate<String> asMatchPredicate() {
        return s -> matcher(s).matches();
    }

    /**
     * Creates a stream from the given input sequence around matches of this
     * pattern.
     *
     * <p> The stream returned by this method contains each substring of the
     * input sequence that is terminated by another subsequence that matches
     * this pattern or is terminated by the end of the input sequence.  The
     * substrings in the stream are in the order in which they occur in the
     * input. Trailing empty strings will be discarded and not encountered in
     * the stream.
     *
     * <p> If this pattern does not match any subsequence of the input then
     * the resulting stream has just one element, namely the input sequence in
     * string form.
     *
     * <p> When there is a positive-width match at the beginning of the input
     * sequence then an empty leading substring is included at the beginning
     * of the stream. A zero-width match at the beginning however never produces
     * such empty leading substring.
     *
     * <p> If the input sequence is mutable, it must remain constant during the
     * execution of the terminal stream operation.  Otherwise, the result of the
     * terminal stream operation is undefined.
     *
     * @param   input
     *          The character sequence to be split
     *
     * @return  The stream of strings computed by splitting the input
     *          around matches of this pattern
     * @see     #split(CharSequence)
     * @since   1.8
     */
    public Stream<String> splitAsStream(final CharSequence input) {
        class MatcherIterator implements Iterator<String> {
            private Matcher matcher;
            // The start position of the next sub-sequence of input
            // when current == input.length there are no more elements
            private int current;
            // null if the next element, if any, needs to obtained
            private String nextElement;
            // > 0 if there are N next empty elements
            private int emptyElementCount;

            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                if (emptyElementCount == 0) {
                    String n = nextElement;
                    nextElement = null;
                    return n;
                } else {
                    emptyElementCount--;
                    return "";
                }
            }

            public boolean hasNext() {
                if (matcher == null) {
                    matcher = matcher(input);
                    // If the input is an empty string then the result can only be a
                    // stream of the input.  Induce that by setting the empty
                    // element count to 1
                    emptyElementCount = input.length() == 0 ? 1 : 0;
                }
                if (nextElement != null || emptyElementCount > 0)
                    return true;

                if (current == input.length())
                    return false;

                // Consume the next matching element
                // Count sequence of matching empty elements
                while (matcher.find()) {
                    nextElement = input.subSequence(current, matcher.start()).toString();
                    current = matcher.end();
                    if (!nextElement.isEmpty()) {
                        return true;
                    } else if (current > 0) { // no empty leading substring for zero-width
                                              // match at the beginning of the input
                        emptyElementCount++;
                    }
                }

                // Consume last matching element
                nextElement = input.subSequence(current, input.length()).toString();
                current = input.length();
                if (!nextElement.isEmpty()) {
                    return true;
                } else {
                    // Ignore a terminal sequence of matching empty elements
                    emptyElementCount = 0;
                    nextElement = null;
                    return false;
                }
            }
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new MatcherIterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
