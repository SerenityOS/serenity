/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;

import java.util.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.main.Option.TARGET;

/** The classfile version target.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public enum Target {
    JDK1_1("1.1", 45, 3),
    JDK1_2("1.2", 46, 0),
    JDK1_3("1.3", 47, 0),

    /** J2SE1.4 = Merlin. */
    JDK1_4("1.4", 48, 0),

    /** JDK 5, codename Tiger. */
    JDK1_5("5", 49, 0),

    /** JDK 6. */
    JDK1_6("6", 50, 0),

    /** JDK 7. */
    JDK1_7("7", 51, 0),

    /** JDK 8. */
    JDK1_8("8", 52, 0),

    /** JDK 9. */
    JDK1_9("9", 53, 0),

    /** JDK 10. */
    JDK1_10("10", 54, 0),

    /** JDK 11. */
    JDK1_11("11", 55, 0),

    /** JDK 12. */
    JDK1_12("12", 56, 0),

    /** JDK 13. */
    JDK1_13("13", 57, 0),

    /** JDK 14. */
    JDK1_14("14", 58, 0),

    /** JDK 15. */
    JDK1_15("15", 59, 0),

    /** JDK 16. */
    JDK1_16("16", 60, 0),

    /** JDK 17. */
    JDK1_17("17", 61, 0),

    /** JDK 18. */
    JDK1_18("18", 62, 0);

    private static final Context.Key<Target> targetKey = new Context.Key<>();

    public static Target instance(Context context) {
        Target instance = context.get(targetKey);
        if (instance == null) {
            Options options = Options.instance(context);
            String targetString = options.get(TARGET);
            if (targetString != null) instance = lookup(targetString);
            if (instance == null) instance = DEFAULT;
            context.put(targetKey, instance);
        }
        return instance;
    }

    public static final Target MIN = Target.JDK1_7;

    private static final Target MAX = values()[values().length - 1];

    private static final Map<String,Target> tab = new HashMap<>();
    static {
        for (Target t : values()) {
            tab.put(t.name, t);
        }
        tab.put("1.5", JDK1_5);
        tab.put("1.6", JDK1_6);
        tab.put("1.7", JDK1_7);
        tab.put("1.8", JDK1_8);
        tab.put("1.9", JDK1_9);
        tab.put("1.10", JDK1_10);
    }

    public final String name;
    public final int majorVersion;
    public final int minorVersion;
    private Target(String name, int majorVersion, int minorVersion) {
        this.name = name;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public static final Target DEFAULT = values()[values().length - 1];

    public static Target lookup(String name) {
        return tab.get(name);
    }

    public boolean isSupported() {
        return this.compareTo(MIN) >= 0;
    }

    /** Return the character to be used in constructing synthetic
     *  identifiers, where not specified by the JLS.
     */
    public char syntheticNameChar() {
        return '$';
    }

    /** Does the target VM expect MethodParameters attributes?
     */
    public boolean hasMethodParameters() {
        return compareTo(JDK1_8) >= 0;
    }

    /** Does the target JDK contain StringConcatFactory class?
     */
    public boolean hasStringConcatFactory() {
        return compareTo(JDK1_9) >= 0;
    }

    /** Value of platform release used to access multi-release jar files
     */
    public String multiReleaseValue() {
        return Integer.toString(this.ordinal() - Target.JDK1_1.ordinal() + 1);
    }

    /** All modules that export an API are roots when compiling code in the unnamed
     *  module and targeting 11 or newer.
     */
    public boolean allApiModulesAreRoots() {
        return compareTo(JDK1_11) >= 0;
    }

    /** Does the target VM support nestmate access?
     */
    public boolean hasNestmateAccess() {
        return compareTo(JDK1_11) >= 0;
    }

    /** language runtime uses nest-based access.
     *  e.g. lambda and string concat spin dynamic proxy class as a nestmate
     *  of the target class
     */
    public boolean runtimeUseNestAccess() {
        return compareTo(JDK1_15) >= 0;
    }

    /** Does the target VM support virtual private invocations?
     */
    public boolean hasVirtualPrivateInvoke() {
        return compareTo(JDK1_11) >= 0;
    }

    /** Does the target VM support sealed types
     */
    public boolean hasSealedClasses() {
        return compareTo(JDK1_15) >= 0;
    }

    /** Is the ACC_STRICT bit redundant and obsolete
     */
    public boolean obsoleteAccStrict() {
        return compareTo(JDK1_17) >= 0;
    }
}
