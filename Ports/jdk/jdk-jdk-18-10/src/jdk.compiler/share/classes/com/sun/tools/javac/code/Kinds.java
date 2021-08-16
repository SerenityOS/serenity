/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import java.util.EnumSet;
import java.util.Set;
import java.util.Locale;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.api.Formattable;
import com.sun.tools.javac.api.Messages;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.code.TypeTag.PACKAGE;
import static com.sun.tools.javac.code.TypeTag.TYPEVAR;

/** Internal symbol kinds, which distinguish between elements of
 *  different subclasses of Symbol. Symbol kinds are organized so they can be
 *  or'ed to sets.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Kinds {

    private Kinds() {} // uninstantiable

    /**
     * Kind of symbols.
     *
     * IMPORTANT: This is an ordered type.  The ordering of
     * declarations in this enum matters.  Be careful when changing
     * it.
     */
    public enum Kind {
        NIL(Category.BASIC, KindSelector.NIL),
        PCK(Category.BASIC, KindName.PACKAGE, KindSelector.PCK),
        TYP(Category.BASIC, KindName.CLASS, KindSelector.TYP),
        VAR(Category.BASIC, KindName.VAR, KindSelector.VAR),
        MTH(Category.BASIC, KindName.METHOD, KindSelector.MTH),
        POLY(Category.BASIC, KindSelector.POLY),
        MDL(Category.BASIC, KindSelector.MDL),
        ERR(Category.ERROR, KindSelector.ERR),
        AMBIGUOUS(Category.RESOLUTION_TARGET),                         // overloaded       target
        HIDDEN(Category.RESOLUTION_TARGET),                            // not overloaded   non-target
        STATICERR(Category.RESOLUTION_TARGET),                         // overloaded?      target
        MISSING_ENCL(Category.RESOLUTION),                             // not overloaded   non-target
        BAD_RESTRICTED_TYPE(Category.RESOLUTION),                      // not overloaded   non-target
        ABSENT_VAR(Category.RESOLUTION_TARGET, KindName.VAR),          // not overloaded   non-target
        WRONG_MTHS(Category.RESOLUTION_TARGET, KindName.METHOD),       // overloaded       target
        WRONG_MTH(Category.RESOLUTION_TARGET, KindName.METHOD),        // not overloaded   target
        ABSENT_MTH(Category.RESOLUTION_TARGET, KindName.METHOD),       // not overloaded   non-target
        ABSENT_TYP(Category.RESOLUTION_TARGET, KindName.CLASS);        // not overloaded   non-target

        // There are essentially two "levels" to the Kind datatype.
        // The first is a totally-ordered set of categories of
        // solutions.  Within each category, we have more
        // possibilities.
        private enum Category {
            BASIC, ERROR, RESOLUTION, RESOLUTION_TARGET;
        }

        private final KindName kindName;
        private final KindName absentKind;
        private final KindSelector selector;
        private final Category category;

        private Kind(Category category) {
            this(category, null, null, null);
        }

        private Kind(Category category,
                     KindSelector selector) {
            this(category, null, null, selector);
        }

        private Kind(Category category,
                     KindName absentKind) {
            this(category, null, absentKind, null);
        }

        private Kind(Category category,
                     KindName kindName,
                     KindSelector selector) {
            this(category, kindName, null, selector);
        }

        private Kind(Category category,
                     KindName kindName,
                     KindName absentKind,
                     KindSelector selector) {
            this.category = category;
            this.kindName = kindName;
            this.absentKind = absentKind;
            this.selector = selector;
        }

        public KindSelector toSelector() {
            return selector;
        }

        public boolean matches(KindSelector kindSelectors) {
            return selector.contains(kindSelectors);
        }

        public boolean isResolutionError() {
            return category == Category.RESOLUTION || category == Category.RESOLUTION_TARGET;
        }

        public boolean isResolutionTargetError() {
            return category == Category.RESOLUTION_TARGET;
        }

        public boolean isValid() {
            return category == Category.BASIC;
        }

        public boolean betterThan(Kind other) {
            return ordinal() < other.ordinal();
        }

        public KindName kindName() {
            if (kindName == null) {
                throw new AssertionError("Unexpected kind: " + this);
            } else {
                return kindName;
            }
        }

        public KindName absentKind() {
            if (absentKind == null) {
                throw new AssertionError("Unexpected kind: " + this);
            } else {
                return absentKind;
            }
        }
    }

    public static class KindSelector {

        //basic selectors
        public static final KindSelector NIL = new KindSelector(0);
        public static final KindSelector PCK = new KindSelector(0x01);
        public static final KindSelector TYP = new KindSelector(0x02);
        public static final KindSelector VAR = new KindSelector(0x04);
        public static final KindSelector VAL = new KindSelector(0x0c);
        public static final KindSelector MTH = new KindSelector(0x10);
        public static final KindSelector POLY = new KindSelector(0x20);
        public static final KindSelector MDL = new KindSelector(0x40);
        public static final KindSelector ERR = new KindSelector(0x7f);
        public static final KindSelector ASG = new KindSelector(0x84);

        //common derived selectors
        public static final KindSelector TYP_PCK = of(TYP, PCK);
        public static final KindSelector VAL_MTH = of(VAL, MTH);
        public static final KindSelector VAL_POLY = of(VAL, POLY);
        public static final KindSelector VAL_TYP = of(VAL, TYP);
        public static final KindSelector VAL_TYP_PCK = of(VAL, TYP, PCK);

        private final byte data;

        private KindSelector(int data) {
            this.data = (byte) data;
        }

        public static KindSelector of(KindSelector... kindSelectors) {
            byte newData = 0;
            for (KindSelector kindSel : kindSelectors) {
                newData |= kindSel.data;
            }
            return new KindSelector(newData);
        }

        public boolean subset(KindSelector other) {
            return (data & ~other.data) == 0;
        }

        public boolean contains(KindSelector other) {
            return (data & other.data) != 0;
        }

        /** A set of KindName(s) representing a set of symbol's kinds. */
        public Set<KindName> kindNames() {
            EnumSet<KindName> kinds = EnumSet.noneOf(KindName.class);
            if ((data & VAL.data) != 0) {
                if ((data & VAL.data) == VAR.data) kinds.add(KindName.VAR);
                else kinds.add(KindName.VAL);
            }
            if ((data & MTH.data) != 0) kinds.add(KindName.METHOD);
            if ((data & TYP.data) != 0) kinds.add(KindName.CLASS);
            if ((data & PCK.data) != 0) kinds.add(KindName.PACKAGE);
            if ((data & MDL.data) != 0) kinds.add(KindName.MODULE);
            return kinds;
        }
    }

    public enum KindName implements Formattable {
        ANNOTATION("kindname.annotation"),
        CONSTRUCTOR("kindname.constructor"),
        INTERFACE("kindname.interface"),
        ENUM("kindname.enum"),
        STATIC("kindname.static"),
        TYPEVAR("kindname.type.variable"),
        BOUND("kindname.type.variable.bound"),
        VAR("kindname.variable"),
        VAL("kindname.value"),
        METHOD("kindname.method"),
        CLASS("kindname.class"),
        STATIC_INIT("kindname.static.init"),
        INSTANCE_INIT("kindname.instance.init"),
        PACKAGE("kindname.package"),
        MODULE("kindname.module"),
        RECORD_COMPONENT("kindname.record.component"),
        RECORD("kindname.record");

        private final String name;

        KindName(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public String getKind() {
            return "Kindname";
        }

        public String toString(Locale locale, Messages messages) {
            String s = toString();
            return messages.getLocalizedString(locale, "compiler.misc." + s);
        }
    }

    public static KindName kindName(MemberReferenceTree.ReferenceMode mode) {
        switch (mode) {
            case INVOKE: return KindName.METHOD;
            case NEW: return KindName.CONSTRUCTOR;
            default : throw new AssertionError("Unexpected mode: "+ mode);
        }
    }

    /** A KindName representing a given symbol
     */
    public static KindName kindName(Symbol sym) {
        switch (sym.getKind()) {
        case PACKAGE:
            return KindName.PACKAGE;

        case ENUM:
            return KindName.ENUM;

        case ANNOTATION_TYPE:
        case CLASS:
            return KindName.CLASS;

        case RECORD:
            return KindName.RECORD;

        case INTERFACE:
            return KindName.INTERFACE;

        case TYPE_PARAMETER:
            return KindName.TYPEVAR;

        case BINDING_VARIABLE:
        case ENUM_CONSTANT:
        case PARAMETER:
        case LOCAL_VARIABLE:
        case EXCEPTION_PARAMETER:
        case RESOURCE_VARIABLE:
            return KindName.VAR;

        case FIELD:
            return ((sym.flags_field & RECORD) != 0) ? KindName.RECORD_COMPONENT : KindName.VAR;

        case CONSTRUCTOR:
            return KindName.CONSTRUCTOR;

        case METHOD:
            return KindName.METHOD;
        case STATIC_INIT:
            return KindName.STATIC_INIT;
        case INSTANCE_INIT:
            return KindName.INSTANCE_INIT;

        default:
                throw new AssertionError("Unexpected kind: "+sym.getKind());
        }
    }

    /** A KindName representing the kind of a given class/interface type.
     */
    public static KindName typeKindName(Type t) {
        if (t.hasTag(TYPEVAR) ||
            t.hasTag(CLASS) && (t.tsym.flags() & COMPOUND) != 0)
            return KindName.BOUND;
        else if (t.hasTag(PACKAGE))
            return KindName.PACKAGE;
        else if ((t.tsym.flags_field & ANNOTATION) != 0)
            return KindName.ANNOTATION;
        else if ((t.tsym.flags_field & INTERFACE) != 0)
            return KindName.INTERFACE;
        else
            return KindName.CLASS;
    }

}
