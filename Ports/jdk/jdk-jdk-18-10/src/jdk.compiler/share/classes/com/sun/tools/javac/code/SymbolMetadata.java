/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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


import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Pair;

/**
 * Container for all annotations (attributes in javac) on a Symbol.
 *
 * This class is explicitly mutable. Its contents will change when attributes
 * are annotated onto the Symbol. However this class depends on the facts that
 * List (in javac) is immutable.
 *
 * An instance of this class can be in one of three states:
 *
 * NOT_STARTED indicates that the Symbol this instance belongs to has not been
 * annotated (yet). Specifically if the declaration is not annotated this
 * instance will never move past NOT_STARTED. You can never go back to
 * NOT_STARTED.
 *
 * IN_PROGRESS annotations have been found on the declaration. Will be processed
 * later. You can reset to IN_PROGRESS. While IN_PROGRESS you can set the list
 * of attributes (and this moves out of the IN_PROGRESS state).
 *
 * "unnamed" this SymbolMetadata contains some attributes, possibly the final set.
 * While in this state you can only prepend or append to the attributes not set
 * it directly. You can also move back to the IN_PROGRESS state using reset().
 *
 * <p><b>This is NOT part of any supported API. If you write code that depends
 * on this, you do so at your own risk. This code and its internal interfaces
 * are subject to change or deletion without notice.</b>
 */
public class SymbolMetadata {

    private static final List<Attribute.Compound> DECL_NOT_STARTED = List.of(null);
    private static final List<Attribute.Compound> DECL_IN_PROGRESS = List.of(null);

    /*
     * This field should never be null
     */
    private List<Attribute.Compound> attributes = DECL_NOT_STARTED;

    /*
     * Type attributes for this symbol.
     * This field should never be null.
     */
    private List<Attribute.TypeCompound> type_attributes = List.nil();

    /*
     * Type attributes of initializers in this class.
     * Unused if the current symbol is not a ClassSymbol.
     */
    private List<Attribute.TypeCompound> init_type_attributes = List.nil();

    /*
     * Type attributes of class initializers in this class.
     * Unused if the current symbol is not a ClassSymbol.
     */
    private List<Attribute.TypeCompound> clinit_type_attributes = List.nil();

    /*
     * The Symbol this SymbolMetadata instance belongs to
     */
    private final Symbol sym;

    public SymbolMetadata(Symbol sym) {
        this.sym = sym;
    }

    public List<Attribute.Compound> getDeclarationAttributes() {
        return filterDeclSentinels(attributes);
    }

    public List<Attribute.TypeCompound> getTypeAttributes() {
        return type_attributes;
    }

    public List<Attribute.TypeCompound> getInitTypeAttributes() {
        return init_type_attributes;
    }

    public List<Attribute.TypeCompound> getClassInitTypeAttributes() {
        return clinit_type_attributes;
    }

    public void setDeclarationAttributes(List<Attribute.Compound> a) {
        Assert.check(pendingCompletion() || !isStarted());
        if (a == null) {
            throw new NullPointerException();
        }
        attributes = a;
    }

    public void setTypeAttributes(List<Attribute.TypeCompound> a) {
        if (a == null) {
            throw new NullPointerException();
        }
        type_attributes = a;
    }

    public void setInitTypeAttributes(List<Attribute.TypeCompound> a) {
        if (a == null) {
            throw new NullPointerException();
        }
        init_type_attributes = a;
    }

    public void setClassInitTypeAttributes(List<Attribute.TypeCompound> a) {
        if (a == null) {
            throw new NullPointerException();
        }
        clinit_type_attributes = a;
    }

    public void setAttributes(SymbolMetadata other) {
        if (other == null) {
            throw new NullPointerException();
        }
        setDeclarationAttributes(other.getDeclarationAttributes());
        if ((sym.flags() & Flags.BRIDGE) != 0) {
            Assert.check(other.sym.kind == Kind.MTH);
            ListBuffer<TypeCompound> typeAttributes = new ListBuffer<>();
            for (TypeCompound tc : other.getTypeAttributes()) {
                // Carry over only contractual type annotations: i.e nothing interior to method body.
                if (!tc.position.type.isLocal())
                    typeAttributes.append(tc);
            }
            setTypeAttributes(typeAttributes.toList());
        } else {
            setTypeAttributes(other.getTypeAttributes());
        }
        if (sym.kind == Kind.TYP) {
            setInitTypeAttributes(other.getInitTypeAttributes());
            setClassInitTypeAttributes(other.getClassInitTypeAttributes());
        }
    }

    public SymbolMetadata reset() {
        attributes = DECL_IN_PROGRESS;
        return this;
    }

    public boolean isEmpty() {
        return !isStarted()
                || pendingCompletion()
                || attributes.isEmpty();
    }

    public boolean isTypesEmpty() {
        return type_attributes.isEmpty();
    }

    public boolean pendingCompletion() {
        return attributes == DECL_IN_PROGRESS;
    }

    public SymbolMetadata append(List<Attribute.Compound> l) {
        attributes = filterDeclSentinels(attributes);

        if (l.isEmpty()) {
            // no-op
        } else if (attributes.isEmpty()) {
            attributes = l;
        } else {
            attributes = attributes.appendList(l);
        }
        return this;
    }

    public SymbolMetadata appendUniqueTypes(List<Attribute.TypeCompound> l) {
        if (l.isEmpty()) {
            // no-op
        } else if (type_attributes.isEmpty()) {
            type_attributes = l;
        } else {
            // TODO: in case we expect a large number of annotations, this
            // might be inefficient.
            for (Attribute.TypeCompound tc : l) {
                if (!type_attributes.contains(tc))
                    type_attributes = type_attributes.append(tc);
            }
        }
        return this;
    }

    public SymbolMetadata appendInitTypeAttributes(List<Attribute.TypeCompound> l) {
        if (l.isEmpty()) {
            // no-op
        } else if (init_type_attributes.isEmpty()) {
            init_type_attributes = l;
        } else {
            init_type_attributes = init_type_attributes.appendList(l);
        }
        return this;
    }

    public SymbolMetadata appendClassInitTypeAttributes(List<Attribute.TypeCompound> l) {
        if (l.isEmpty()) {
            // no-op
        } else if (clinit_type_attributes.isEmpty()) {
            clinit_type_attributes = l;
        } else {
            clinit_type_attributes = clinit_type_attributes.appendList(l);
        }
        return this;
    }

    public SymbolMetadata prepend(List<Attribute.Compound> l) {
        attributes = filterDeclSentinels(attributes);

        if (l.isEmpty()) {
            // no-op
        } else if (attributes.isEmpty()) {
            attributes = l;
        } else {
            attributes = attributes.prependList(l);
        }
        return this;
    }

    private List<Attribute.Compound> filterDeclSentinels(List<Attribute.Compound> a) {
        return (a == DECL_IN_PROGRESS || a == DECL_NOT_STARTED)
                ? List.nil()
                : a;
    }

    private boolean isStarted() {
        return attributes != DECL_NOT_STARTED;
    }

    private List<Attribute.Compound> removeFromCompoundList(List<Attribute.Compound> l, Attribute.Compound compound) {
        ListBuffer<Attribute.Compound> lb = new ListBuffer<>();
        for (Attribute.Compound c : l) {
            if (c != compound) {
                lb.add(c);
            }
        }
        return lb.toList();
    }

    public void removeDeclarationMetadata(Attribute.Compound compound) {
        if (attributes.contains(compound)) {
            attributes = removeFromCompoundList(attributes, compound);
        } else {
            // slow path, it could be that attributes list contains annotation containers, so we have to dig deeper
            for (Attribute.Compound attrCompound : attributes) {
                if (attrCompound.isSynthesized() && !attrCompound.values.isEmpty()) {
                    Pair<Symbol.MethodSymbol, Attribute> val = attrCompound.values.get(0);
                    if (val.fst.getSimpleName().contentEquals("value") &&
                            val.snd instanceof Attribute.Array arr) {
                        if (arr.values.length != 0
                                && arr.values[0] instanceof Attribute.Compound
                                && arr.values[0].type == compound.type) {
                            attributes = removeFromCompoundList(attributes, attrCompound);
                        }
                    }
                }
            }
        }
    }
}
