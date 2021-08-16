/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jshell;

import static com.sun.tools.javac.code.Flags.COMPOUND;
import static com.sun.tools.javac.code.Kinds.Kind.PCK;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.IntersectionClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.JavacMessages;
import java.util.Locale;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Print types in source form.
 */
class TypePrinter extends Printer {

    private static final String OBJECT = "Object";

    private final JavacMessages messages;
    private final Types types;
    private final BinaryOperator<String> fullClassNameAndPackageToClass;
    private final Function<TypeSymbol, String> anonymousToName;
    private final boolean printIntersectionTypes;
    private final AnonymousTypeKind anonymousTypesKind;

    /**Create a TypePrinter.
     *
     * @param messages javac's messages
     * @param fullClassNameAndPackageToClass convertor to convert full class names to
     *                                       simple class names.
     * @param printIntersectionTypes whether intersection types should be printed
     * @param anonymousTypesKind how the anonymous types should be printed
     */
    TypePrinter(JavacMessages messages, Types types,
                BinaryOperator<String> fullClassNameAndPackageToClass,
                boolean printIntersectionTypes, AnonymousTypeKind anonymousTypesKind) {
        this(messages, types, fullClassNameAndPackageToClass, cs -> cs.flatName().toString(),
             printIntersectionTypes, anonymousTypesKind);
    }

    /**Create a TypePrinter.
     *
     * @param messages javac's messages
     * @param fullClassNameAndPackageToClass convertor to convert full class names to
     *                                       simple class names.
     * @param anonymousToName convertor from anonymous classes to name that should be printed
     *                        if anonymousTypesKind == AnonymousTypeKind.DECLARE
     * @param printIntersectionTypes whether intersection types should be printed
     * @param anonymousTypesKind how the anonymous types should be printed
     */
    TypePrinter(JavacMessages messages, Types types,
                BinaryOperator<String> fullClassNameAndPackageToClass,
                Function<TypeSymbol, String> anonymousToName,
                boolean printIntersectionTypes, AnonymousTypeKind anonymousTypesKind) {
        this.messages = messages;
        this.types = types;
        this.fullClassNameAndPackageToClass = fullClassNameAndPackageToClass;
        this.anonymousToName = anonymousToName;
        this.printIntersectionTypes = printIntersectionTypes;
        this.anonymousTypesKind = anonymousTypesKind;
    }

    String toString(Type t) {
        return visit(t, Locale.getDefault());
    }

    @Override
    protected String localize(Locale locale, String key, Object... args) {
        return messages.getLocalizedString(locale, key, args);
    }

    @Override
    protected String capturedVarId(Type.CapturedType t, Locale locale) {
        throw new InternalError("should never call this");
    }

    @Override
    public String visitCapturedType(Type.CapturedType t, Locale locale) {
        return visit(t.wildcard, locale);
    }

    @Override
    public String visitType(Type t, Locale locale) {
        String s = (t.tsym == null || t.tsym.name == null)
                ? OBJECT // none
                : t.tsym.name.toString();
        return s;
    }

    /**
     * Converts a class name into a (possibly localized) string. Anonymous inner
     * classes get converted into a localized string.
     *
     * @param t the type of the class whose name is to be rendered
     * @param longform if set, the class' fullname is displayed - if unset the
     * short name is chosen (w/o package)
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    @Override
    protected String className(ClassType t, boolean longform, Locale locale) {
        TypeSymbol sym = t.tsym;
        if (sym.name.length() == 0 && (sym.flags() & COMPOUND) != 0) {
            if (printIntersectionTypes) {
                return ((IntersectionClassType) t).getExplicitComponents()
                                                  .stream()
                                                  .map(i -> visit(i, locale))
                                                  .collect(Collectors.joining("&"));
            } else {
                return visit(types.erasure(t), locale);
            }
        } else if (sym.name.length() == 0) {
            if (anonymousTypesKind == AnonymousTypeKind.DECLARE) {
                return anonymousToName.apply(sym);
            }
            // Anonymous
            String s;
            boolean isClass;
            ClassType norm = (ClassType) t.tsym.type;
            if (norm == null) {
                s = OBJECT;
                isClass = true;
            } else if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                s = visit(norm.interfaces_field.head, locale);
                isClass = false;
            } else {
                s = visit(norm.supertype_field, locale);
                isClass = true;
            }
            if (anonymousTypesKind == AnonymousTypeKind.DISPLAY) {
                s = isClass ? "<anonymous class extending " + s + ">"
                            : "<anonymous class implementing " + s + ">";
            }
            return s;
        } else if (longform) {
            String pkg = "";
            for (Symbol psym = sym; psym != null; psym = psym.owner) {
                if (psym.kind == PCK) {
                    pkg = psym.getQualifiedName().toString();
                    break;
                }
            }
            return fullClassNameAndPackageToClass.apply(
                    sym.getQualifiedName().toString(),
                    pkg
            );
        } else {
            return sym.name.toString();
        }
    }

    @Override
    public String visitClassSymbol(ClassSymbol sym, Locale locale) {
        return sym.name.isEmpty()
                ? sym.flatname.toString() // Anonymous
                : sym.fullname.toString();
    }

    @Override
    public String visitPackageSymbol(PackageSymbol s, Locale locale) {
        return s.isUnnamed()
                ? "" // Unnamed package
                : s.fullname.toString();
    }

    /** Specifies how the anonymous classes should be handled. */
    public enum AnonymousTypeKind {
        /* The anonymous class is printed as the name of its supertype. */
        SUPER,
        /* The anonymous class is printed as converted by the anonymousToName
         * convertor. */
        DECLARE,
        /* The anonymous class is printed in a human readable form. */
        DISPLAY;
    }
}
