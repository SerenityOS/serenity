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

package com.sun.tools.javac.code;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.comp.Annotate.AnnotationTypeMetadata;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.jvm.PoolConstant;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import com.sun.tools.javac.code.Scope.WriteableScope;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.code.TypeTag.FORALL;
import static com.sun.tools.javac.code.TypeTag.TYPEVAR;
import static com.sun.tools.javac.jvm.ByteCodes.iadd;
import static com.sun.tools.javac.jvm.ByteCodes.ishll;
import static com.sun.tools.javac.jvm.ByteCodes.lushrl;
import static com.sun.tools.javac.jvm.ByteCodes.lxor;
import static com.sun.tools.javac.jvm.ByteCodes.string_add;

/** Root class for Java symbols. It contains subclasses
 *  for specific sorts of symbols, such as variables, methods and operators,
 *  types, packages. Each subclass is represented as a static inner class
 *  inside Symbol.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Symbol extends AnnoConstruct implements PoolConstant, Element {

    /** The kind of this symbol.
     *  @see Kinds
     */
    public Kind kind;

    /** The flags of this symbol.
     */
    public long flags_field;

    /** An accessor method for the flags of this symbol.
     *  Flags of class symbols should be accessed through the accessor
     *  method to make sure that the class symbol is loaded.
     */
    public long flags() { return flags_field; }

    /** The name of this symbol in Utf8 representation.
     */
    public Name name;

    /** The type of this symbol.
     */
    public Type type;

    /** The owner of this symbol.
     */
    public Symbol owner;

    /** The completer of this symbol.
     * This should never equal null (NULL_COMPLETER should be used instead).
     */
    public Completer completer;

    /** A cache for the type erasure of this symbol.
     */
    public Type erasure_field;

    // <editor-fold defaultstate="collapsed" desc="annotations">

    /** The attributes of this symbol are contained in this
     * SymbolMetadata. The SymbolMetadata instance is NOT immutable.
     */
    protected SymbolMetadata metadata;


    /** An accessor method for the attributes of this symbol.
     *  Attributes of class symbols should be accessed through the accessor
     *  method to make sure that the class symbol is loaded.
     */
    public List<Attribute.Compound> getRawAttributes() {
        return (metadata == null)
                ? List.nil()
                : metadata.getDeclarationAttributes();
    }

    /** An accessor method for the type attributes of this symbol.
     *  Attributes of class symbols should be accessed through the accessor
     *  method to make sure that the class symbol is loaded.
     */
    public List<Attribute.TypeCompound> getRawTypeAttributes() {
        return (metadata == null)
                ? List.nil()
                : metadata.getTypeAttributes();
    }

    /** Fetch a particular annotation from a symbol. */
    public Attribute.Compound attribute(Symbol anno) {
        for (Attribute.Compound a : getRawAttributes()) {
            if (a.type.tsym == anno) return a;
        }
        return null;
    }

    public boolean annotationsPendingCompletion() {
        return metadata == null ? false : metadata.pendingCompletion();
    }

    public void appendAttributes(List<Attribute.Compound> l) {
        if (l.nonEmpty()) {
            initedMetadata().append(l);
        }
    }

    public void appendClassInitTypeAttributes(List<Attribute.TypeCompound> l) {
        if (l.nonEmpty()) {
            initedMetadata().appendClassInitTypeAttributes(l);
        }
    }

    public void appendInitTypeAttributes(List<Attribute.TypeCompound> l) {
        if (l.nonEmpty()) {
            initedMetadata().appendInitTypeAttributes(l);
        }
    }

    public void appendUniqueTypeAttributes(List<Attribute.TypeCompound> l) {
        if (l.nonEmpty()) {
            initedMetadata().appendUniqueTypes(l);
        }
    }

    public List<Attribute.TypeCompound> getClassInitTypeAttributes() {
        return (metadata == null)
                ? List.nil()
                : metadata.getClassInitTypeAttributes();
    }

    public List<Attribute.TypeCompound> getInitTypeAttributes() {
        return (metadata == null)
                ? List.nil()
                : metadata.getInitTypeAttributes();
    }

    public void setInitTypeAttributes(List<Attribute.TypeCompound> l) {
        initedMetadata().setInitTypeAttributes(l);
    }

    public void setClassInitTypeAttributes(List<Attribute.TypeCompound> l) {
        initedMetadata().setClassInitTypeAttributes(l);
    }

    public List<Attribute.Compound> getDeclarationAttributes() {
        return (metadata == null)
                ? List.nil()
                : metadata.getDeclarationAttributes();
    }

    public boolean hasAnnotations() {
        return (metadata != null && !metadata.isEmpty());
    }

    public boolean hasTypeAnnotations() {
        return (metadata != null && !metadata.isTypesEmpty());
    }

    public boolean isCompleted() {
        return completer.isTerminal();
    }

    public void prependAttributes(List<Attribute.Compound> l) {
        if (l.nonEmpty()) {
            initedMetadata().prepend(l);
        }
    }

    public void resetAnnotations() {
        initedMetadata().reset();
    }

    public void setAttributes(Symbol other) {
        if (metadata != null || other.metadata != null) {
            initedMetadata().setAttributes(other.metadata);
        }
    }

    public void setDeclarationAttributes(List<Attribute.Compound> a) {
        if (metadata != null || a.nonEmpty()) {
            initedMetadata().setDeclarationAttributes(a);
        }
    }

    public void setTypeAttributes(List<Attribute.TypeCompound> a) {
        if (metadata != null || a.nonEmpty()) {
            if (metadata == null)
                metadata = new SymbolMetadata(this);
            metadata.setTypeAttributes(a);
        }
    }

    private SymbolMetadata initedMetadata() {
        if (metadata == null)
            metadata = new SymbolMetadata(this);
        return metadata;
    }

    /** This method is intended for debugging only. */
    public SymbolMetadata getMetadata() {
        return metadata;
    }

    // </editor-fold>

    /** Construct a symbol with given kind, flags, name, type and owner.
     */
    public Symbol(Kind kind, long flags, Name name, Type type, Symbol owner) {
        this.kind = kind;
        this.flags_field = flags;
        this.type = type;
        this.owner = owner;
        this.completer = Completer.NULL_COMPLETER;
        this.erasure_field = null;
        this.name = name;
    }

    @Override
    public int poolTag() {
        throw new AssertionError("Invalid pool entry");
    }

    /** Clone this symbol with new owner.
     *  Legal only for fields and methods.
     */
    public Symbol clone(Symbol newOwner) {
        throw new AssertionError();
    }

    public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
        return v.visitSymbol(this, p);
    }

    /** The Java source which this symbol represents.
     *  A description of this symbol; overrides Object.
     */
    public String toString() {
        return name.toString();
    }

    /** A Java source description of the location of this symbol; used for
     *  error reporting.
     *
     * @return null if the symbol is a package or a toplevel class defined in
     * the default package; otherwise, the owner symbol is returned
     */
    public Symbol location() {
        if (owner.name == null || (owner.name.isEmpty() &&
                                   (owner.flags() & BLOCK) == 0 &&
                                   owner.kind != PCK &&
                                   owner.kind != TYP)) {
            return null;
        }
        return owner;
    }

    public Symbol location(Type site, Types types) {
        if (owner.name == null || owner.name.isEmpty()) {
            return location();
        }
        if (owner.type.hasTag(CLASS)) {
            Type ownertype = types.asOuterSuper(site, owner);
            if (ownertype != null) return ownertype.tsym;
        }
        return owner;
    }

    public Symbol baseSymbol() {
        return this;
    }

    /** The symbol's erased type.
     */
    public Type erasure(Types types) {
        if (erasure_field == null)
            erasure_field = types.erasure(type);
        return erasure_field;
    }

    /** The external type of a symbol. This is the symbol's erased type
     *  except for constructors of inner classes which get the enclosing
     *  instance class added as first argument.
     */
    public Type externalType(Types types) {
        Type t = erasure(types);
        if (name == name.table.names.init && owner.hasOuterInstance()) {
            Type outerThisType = types.erasure(owner.type.getEnclosingType());
            return new MethodType(t.getParameterTypes().prepend(outerThisType),
                                  t.getReturnType(),
                                  t.getThrownTypes(),
                                  t.tsym);
        } else {
            return t;
        }
    }

    public boolean isDeprecated() {
        return (flags_field & DEPRECATED) != 0;
    }

    public boolean hasDeprecatedAnnotation() {
        return (flags_field & DEPRECATED_ANNOTATION) != 0;
    }

    public boolean isDeprecatedForRemoval() {
        return (flags_field & DEPRECATED_REMOVAL) != 0;
    }

    public boolean isPreviewApi() {
        return (flags_field & PREVIEW_API) != 0;
    }

    public boolean isDeprecatableViaAnnotation() {
        switch (getKind()) {
            case LOCAL_VARIABLE:
            case PACKAGE:
            case PARAMETER:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                return false;
            default:
                return true;
        }
    }

    public boolean isStatic() {
        return
            (flags() & STATIC) != 0 ||
            (owner.flags() & INTERFACE) != 0 && kind != MTH &&
             name != name.table.names._this;
    }

    public boolean isInterface() {
        return (flags() & INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (flags_field & ABSTRACT) != 0;
    }

    public boolean isPrivate() {
        return (flags_field & Flags.AccessFlags) == PRIVATE;
    }

    public boolean isPublic() {
        return (flags_field & Flags.AccessFlags) == PUBLIC;
    }

    public boolean isEnum() {
        return (flags() & ENUM) != 0;
    }

    public boolean isSealed() {
        return (flags_field & SEALED) != 0;
    }

    public boolean isNonSealed() {
        return (flags_field & NON_SEALED) != 0;
    }

    public boolean isFinal() {
        return (flags_field & FINAL) != 0;
    }

   /** Is this symbol declared (directly or indirectly) local
     *  to a method or variable initializer?
     *  Also includes fields of inner classes which are in
     *  turn local to a method or variable initializer.
     */
    public boolean isDirectlyOrIndirectlyLocal() {
        return
            (owner.kind.matches(KindSelector.VAL_MTH) ||
             (owner.kind == TYP && owner.isDirectlyOrIndirectlyLocal()));
    }

    /** Has this symbol an empty name? This includes anonymous
     *  inner classes.
     */
    public boolean isAnonymous() {
        return name.isEmpty();
    }

    /** Is this symbol a constructor?
     */
    public boolean isConstructor() {
        return name == name.table.names.init;
    }

    public boolean isDynamic() {
        return false;
    }

    /** The fully qualified name of this symbol.
     *  This is the same as the symbol's name except for class symbols,
     *  which are handled separately.
     */
    public Name getQualifiedName() {
        return name;
    }

    /** The fully qualified name of this symbol after converting to flat
     *  representation. This is the same as the symbol's name except for
     *  class symbols, which are handled separately.
     */
    public Name flatName() {
        return getQualifiedName();
    }

    /** If this is a class or package, its members, otherwise null.
     */
    public WriteableScope members() {
        return null;
    }

    /** A class is an inner class if it it has an enclosing instance class.
     */
    public boolean isInner() {
        return kind == TYP && type.getEnclosingType().hasTag(CLASS);
    }

    /** An inner class has an outer instance if it is not an interface
     *  it has an enclosing instance class which might be referenced from the class.
     *  Nested classes can see instance members of their enclosing class.
     *  Their constructors carry an additional this$n parameter, inserted
     *  implicitly by the compiler.
     *
     *  @see #isInner
     */
    public boolean hasOuterInstance() {
        return
            type.getEnclosingType().hasTag(CLASS) && (flags() & (INTERFACE | NOOUTERTHIS)) == 0;
    }

    /** The closest enclosing class of this symbol's declaration.
     *  Warning: this (misnamed) method returns the receiver itself
     *  when the receiver is a class (as opposed to its enclosing
     *  class as one may be misled to believe.)
     */
    public ClassSymbol enclClass() {
        Symbol c = this;
        while (c != null &&
               (!c.kind.matches(KindSelector.TYP) || !c.type.hasTag(CLASS))) {
            c = c.owner;
        }
        return (ClassSymbol)c;
    }

    /** The outermost class which indirectly owns this symbol.
     */
    public ClassSymbol outermostClass() {
        Symbol sym = this;
        Symbol prev = null;
        while (sym.kind != PCK) {
            prev = sym;
            sym = sym.owner;
        }
        return (ClassSymbol) prev;
    }

    /** The package which indirectly owns this symbol.
     */
    public PackageSymbol packge() {
        Symbol sym = this;
        while (sym.kind != PCK) {
            sym = sym.owner;
        }
        return (PackageSymbol) sym;
    }

    /** Is this symbol a subclass of `base'? Only defined for ClassSymbols.
     */
    public boolean isSubClass(Symbol base, Types types) {
        throw new AssertionError("isSubClass " + this);
    }

    /** Fully check membership: hierarchy, protection, and hiding.
     *  Does not exclude methods not inherited due to overriding.
     */
    public boolean isMemberOf(TypeSymbol clazz, Types types) {
        return
            owner == clazz ||
            clazz.isSubClass(owner, types) &&
            isInheritedIn(clazz, types) &&
            !hiddenIn((ClassSymbol)clazz, types);
    }

    /** Is this symbol the same as or enclosed by the given class? */
    public boolean isEnclosedBy(ClassSymbol clazz) {
        for (Symbol sym = this; sym.kind != PCK; sym = sym.owner)
            if (sym == clazz) return true;
        return false;
    }

    private boolean hiddenIn(ClassSymbol clazz, Types types) {
        Symbol sym = hiddenInInternal(clazz, types);
        Assert.check(sym != null, "the result of hiddenInInternal() can't be null");
        /* If we find the current symbol then there is no symbol hiding it
         */
        return sym != this;
    }

    /** This method looks in the supertypes graph that has the current class as the
     * initial node, till it finds the current symbol or another symbol that hides it.
     * If the current class has more than one supertype (extends one class and
     * implements one or more interfaces) then null can be returned, meaning that
     * a wrong path in the supertypes graph was selected. Null can only be returned
     * as a temporary value, as a result of the recursive call.
     */
    private Symbol hiddenInInternal(ClassSymbol currentClass, Types types) {
        if (currentClass == owner) {
            return this;
        }
        for (Symbol sym : currentClass.members().getSymbolsByName(name)) {
            if (sym.kind == kind &&
                    (kind != MTH ||
                    (sym.flags() & STATIC) != 0 &&
                    types.isSubSignature(sym.type, type))) {
                return sym;
            }
        }
        Symbol hiddenSym = null;
        for (Type st : types.interfaces(currentClass.type)
                .prepend(types.supertype(currentClass.type))) {
            if (st != null && (st.hasTag(CLASS))) {
                Symbol sym = hiddenInInternal((ClassSymbol)st.tsym, types);
                if (sym == this) {
                    return this;
                } else if (sym != null) {
                    hiddenSym = sym;
                }
            }
        }
        return hiddenSym;
    }

    /** Is this symbol accessible in a given class?
     *  PRE: If symbol's owner is a interface,
     *       it is already assumed that the interface is a superinterface
     *       the given class.
     *  @param clazz  The class for which we want to establish membership.
     *                This must be a subclass of the member's owner.
     */
    public final boolean isAccessibleIn(Symbol clazz, Types types) {
        switch ((int)(flags_field & Flags.AccessFlags)) {
        default: // error recovery
        case PUBLIC:
            return true;
        case PRIVATE:
            return this.owner == clazz;
        case PROTECTED:
            // we model interfaces as extending Object
            return (clazz.flags() & INTERFACE) == 0;
        case 0:
            PackageSymbol thisPackage = this.packge();
            for (Symbol sup = clazz;
                 sup != null && sup != this.owner;
                 sup = types.supertype(sup.type).tsym) {
                while (sup.type.hasTag(TYPEVAR))
                    sup = sup.type.getUpperBound().tsym;
                if (sup.type.isErroneous())
                    return true; // error recovery
                if ((sup.flags() & COMPOUND) != 0)
                    continue;
                if (sup.packge() != thisPackage)
                    return false;
            }
            return (clazz.flags() & INTERFACE) == 0;
        }
    }

    /** Is this symbol inherited into a given class?
     *  PRE: If symbol's owner is a interface,
     *       it is already assumed that the interface is a superinterface
     *       of the given class.
     *  @param clazz  The class for which we want to establish membership.
     *                This must be a subclass of the member's owner.
     */
    public boolean isInheritedIn(Symbol clazz, Types types) {
        return isAccessibleIn(clazz, types);
    }

    /** The (variable or method) symbol seen as a member of given
     *  class type`site' (this might change the symbol's type).
     *  This is used exclusively for producing diagnostics.
     */
    public Symbol asMemberOf(Type site, Types types) {
        throw new AssertionError();
    }

    /** Does this method symbol override `other' symbol, when both are seen as
     *  members of class `origin'?  It is assumed that _other is a member
     *  of origin.
     *
     *  It is assumed that both symbols have the same name.  The static
     *  modifier is ignored for this test.
     *
     *  See JLS 8.4.6.1 (without transitivity) and 8.4.6.4
     */
    public boolean overrides(Symbol _other, TypeSymbol origin, Types types, boolean checkResult) {
        return false;
    }

    /** Complete the elaboration of this symbol's definition.
     */
    public void complete() throws CompletionFailure {
        if (completer != Completer.NULL_COMPLETER) {
            Completer c = completer;
            completer = Completer.NULL_COMPLETER;
            c.complete(this);
        }
    }

    public void apiComplete() throws CompletionFailure {
        try {
            complete();
        } catch (CompletionFailure cf) {
            cf.dcfh.handleAPICompletionFailure(cf);
        }
    }

    /** True if the symbol represents an entity that exists.
     */
    public boolean exists() {
        return true;
    }

    @DefinedBy(Api.LANGUAGE_MODEL)
    public Type asType() {
        return type;
    }

    @DefinedBy(Api.LANGUAGE_MODEL)
    public Symbol getEnclosingElement() {
        return owner;
    }

    @DefinedBy(Api.LANGUAGE_MODEL)
    public ElementKind getKind() {
        return ElementKind.OTHER;       // most unkind
    }

    @DefinedBy(Api.LANGUAGE_MODEL)
    public Set<Modifier> getModifiers() {
        apiComplete();
        return Flags.asModifierSet(flags());
    }

    @DefinedBy(Api.LANGUAGE_MODEL)
    public Name getSimpleName() {
        return name;
    }

    /**
     * This is the implementation for {@code
     * javax.lang.model.element.Element.getAnnotationMirrors()}.
     */
    @Override @DefinedBy(Api.LANGUAGE_MODEL)
    public List<Attribute.Compound> getAnnotationMirrors() {
        apiComplete();
        return getRawAttributes();
    }


    // TODO: getEnclosedElements should return a javac List, fix in FilteredMemberList
    @DefinedBy(Api.LANGUAGE_MODEL)
    public java.util.List<Symbol> getEnclosedElements() {
        return List.nil();
    }

    public List<TypeVariableSymbol> getTypeParameters() {
        ListBuffer<TypeVariableSymbol> l = new ListBuffer<>();
        for (Type t : type.getTypeArguments()) {
            Assert.check(t.tsym.getKind() == ElementKind.TYPE_PARAMETER);
            l.append((TypeVariableSymbol)t.tsym);
        }
        return l.toList();
    }

    public static class DelegatedSymbol<T extends Symbol> extends Symbol {
        protected T other;
        public DelegatedSymbol(T other) {
            super(other.kind, other.flags_field, other.name, other.type, other.owner);
            this.other = other;
        }
        public String toString() { return other.toString(); }
        public Symbol location() { return other.location(); }
        public Symbol location(Type site, Types types) { return other.location(site, types); }
        public Symbol baseSymbol() { return other; }
        public Type erasure(Types types) { return other.erasure(types); }
        public Type externalType(Types types) { return other.externalType(types); }
        public boolean isDirectlyOrIndirectlyLocal() { return other.isDirectlyOrIndirectlyLocal(); }
        public boolean isConstructor() { return other.isConstructor(); }
        public Name getQualifiedName() { return other.getQualifiedName(); }
        public Name flatName() { return other.flatName(); }
        public WriteableScope members() { return other.members(); }
        public boolean isInner() { return other.isInner(); }
        public boolean hasOuterInstance() { return other.hasOuterInstance(); }
        public ClassSymbol enclClass() { return other.enclClass(); }
        public ClassSymbol outermostClass() { return other.outermostClass(); }
        public PackageSymbol packge() { return other.packge(); }
        public boolean isSubClass(Symbol base, Types types) { return other.isSubClass(base, types); }
        public boolean isMemberOf(TypeSymbol clazz, Types types) { return other.isMemberOf(clazz, types); }
        public boolean isEnclosedBy(ClassSymbol clazz) { return other.isEnclosedBy(clazz); }
        public boolean isInheritedIn(Symbol clazz, Types types) { return other.isInheritedIn(clazz, types); }
        public Symbol asMemberOf(Type site, Types types) { return other.asMemberOf(site, types); }
        public void complete() throws CompletionFailure { other.complete(); }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return other.accept(v, p);
        }

        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitSymbol(other, p);
        }

        public T getUnderlyingSymbol() {
            return other;
        }
    }

    /** A base class for Symbols representing types.
     */
    public static abstract class TypeSymbol extends Symbol {
        public TypeSymbol(Kind kind, long flags, Name name, Type type, Symbol owner) {
            super(kind, flags, name, type, owner);
        }
        /** form a fully qualified name from a name and an owner
         */
        public static Name formFullName(Name name, Symbol owner) {
            if (owner == null) return name;
            if ((owner.kind != ERR) &&
                (owner.kind.matches(KindSelector.VAL_MTH) ||
                 (owner.kind == TYP && owner.type.hasTag(TYPEVAR))
                 )) return name;
            Name prefix = owner.getQualifiedName();
            if (prefix == null || prefix == prefix.table.names.empty)
                return name;
            else return prefix.append('.', name);
        }

        /** form a fully qualified name from a name and an owner, after
         *  converting to flat representation
         */
        public static Name formFlatName(Name name, Symbol owner) {
            if (owner == null || owner.kind.matches(KindSelector.VAL_MTH) ||
                (owner.kind == TYP && owner.type.hasTag(TYPEVAR))
                ) return name;
            char sep = owner.kind == TYP ? '$' : '.';
            Name prefix = owner.flatName();
            if (prefix == null || prefix == prefix.table.names.empty)
                return name;
            else return prefix.append(sep, name);
        }

        /**
         * A partial ordering between type symbols that refines the
         * class inheritance graph.
         *
         * Type variables always precede other kinds of symbols.
         */
        public final boolean precedes(TypeSymbol that, Types types) {
            if (this == that)
                return false;
            if (type.hasTag(that.type.getTag())) {
                if (type.hasTag(CLASS)) {
                    return
                        types.rank(that.type) < types.rank(this.type) ||
                        types.rank(that.type) == types.rank(this.type) &&
                        that.getQualifiedName().compareTo(this.getQualifiedName()) < 0;
                } else if (type.hasTag(TYPEVAR)) {
                    return types.isSubtype(this.type, that.type);
                }
            }
            return type.hasTag(TYPEVAR);
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Symbol> getEnclosedElements() {
            List<Symbol> list = List.nil();
            if (kind == TYP && type.hasTag(TYPEVAR)) {
                return list;
            }
            apiComplete();
            for (Symbol sym : members().getSymbols(NON_RECURSIVE)) {
                sym.apiComplete();
                if ((sym.flags() & SYNTHETIC) == 0 && sym.owner == this && sym.kind != ERR) {
                    list = list.prepend(sym);
                }
            }
            return list;
        }

        public AnnotationTypeMetadata getAnnotationTypeMetadata() {
            Assert.error("Only on ClassSymbol");
            return null; //unreachable
        }

        public boolean isAnnotationType() { return false; }

        @Override
        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitTypeSymbol(this, p);
        }
    }

    /**
     * Type variables are represented by instances of this class.
     */
    public static class TypeVariableSymbol
            extends TypeSymbol implements TypeParameterElement {

        public TypeVariableSymbol(long flags, Name name, Type type, Symbol owner) {
            super(TYP, flags, name, type, owner);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            return ElementKind.TYPE_PARAMETER;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Symbol getGenericElement() {
            return owner;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Type> getBounds() {
            TypeVar t = (TypeVar)type;
            Type bound = t.getUpperBound();
            if (!bound.isCompound())
                return List.of(bound);
            ClassType ct = (ClassType)bound;
            if (!ct.tsym.erasure_field.isInterface()) {
                return ct.interfaces_field.prepend(ct.supertype_field);
            } else {
                // No superclass was given in bounds.
                // In this case, supertype is Object, erasure is first interface.
                return ct.interfaces_field;
            }
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Attribute.Compound> getAnnotationMirrors() {
            // Declaration annotations on type variables are stored in type attributes
            // on the owner of the TypeVariableSymbol
            List<Attribute.TypeCompound> candidates = owner.getRawTypeAttributes();
            int index = owner.getTypeParameters().indexOf(this);
            List<Attribute.Compound> res = List.nil();
            for (Attribute.TypeCompound a : candidates) {
                if (isCurrentSymbolsAnnotation(a, index))
                    res = res.prepend(a);
            }

            return res.reverse();
        }

        // Helper to getAnnotation[s]
        @Override
        public <A extends Annotation> Attribute.Compound getAttribute(Class<A> annoType) {
            String name = annoType.getName();

            // Declaration annotations on type variables are stored in type attributes
            // on the owner of the TypeVariableSymbol
            List<Attribute.TypeCompound> candidates = owner.getRawTypeAttributes();
            int index = owner.getTypeParameters().indexOf(this);
            for (Attribute.TypeCompound anno : candidates)
                if (isCurrentSymbolsAnnotation(anno, index) &&
                    name.contentEquals(anno.type.tsym.flatName()))
                    return anno;

            return null;
        }
            //where:
            boolean isCurrentSymbolsAnnotation(Attribute.TypeCompound anno, int index) {
                return (anno.position.type == TargetType.CLASS_TYPE_PARAMETER ||
                        anno.position.type == TargetType.METHOD_TYPE_PARAMETER) &&
                        anno.position.parameter_index == index;
            }


        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitTypeParameter(this, p);
        }
    }
    /** A class for module symbols.
     */
    public static class ModuleSymbol extends TypeSymbol
            implements ModuleElement {

        public Name version;
        public JavaFileManager.Location sourceLocation;
        public JavaFileManager.Location classLocation;
        public JavaFileManager.Location patchLocation;
        public JavaFileManager.Location patchOutputLocation;

        /** All directives, in natural order. */
        public List<com.sun.tools.javac.code.Directive> directives;
        public List<com.sun.tools.javac.code.Directive.RequiresDirective> requires;
        public List<com.sun.tools.javac.code.Directive.ExportsDirective> exports;
        public List<com.sun.tools.javac.code.Directive.OpensDirective> opens;
        public List<com.sun.tools.javac.code.Directive.ProvidesDirective> provides;
        public List<com.sun.tools.javac.code.Directive.UsesDirective> uses;

        public ClassSymbol module_info;

        public PackageSymbol unnamedPackage;
        public Map<Name, PackageSymbol> visiblePackages;
        public Set<ModuleSymbol> readModules;
        public List<Symbol> enclosedPackages = List.nil();

        public Completer usesProvidesCompleter = Completer.NULL_COMPLETER;
        public final Set<ModuleFlags> flags = EnumSet.noneOf(ModuleFlags.class);
        public final Set<ModuleResolutionFlags> resolutionFlags = EnumSet.noneOf(ModuleResolutionFlags.class);

        /**
         * Create a ModuleSymbol with an associated module-info ClassSymbol.
         */
        public static ModuleSymbol create(Name name, Name module_info) {
            ModuleSymbol msym = new ModuleSymbol(name, null);
            ClassSymbol info = new ClassSymbol(Flags.MODULE, module_info, msym);
            info.fullname = formFullName(module_info, msym);
            info.flatname = info.fullname;
            info.members_field = WriteableScope.create(info);
            msym.module_info = info;
            return msym;
        }

        public ModuleSymbol(Name name, Symbol owner) {
            super(MDL, 0, name, null, owner);
            Assert.checkNonNull(name);
            this.type = new ModuleType(this);
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_Module;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Name getSimpleName() {
            return Convert.shortName(name);
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public boolean isOpen() {
            return flags.contains(ModuleFlags.OPEN);
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public boolean isUnnamed() {
            return name.isEmpty() && owner == null;
        }

        @Override
        public boolean isDeprecated() {
            return hasDeprecatedAnnotation();
        }

        public boolean isNoModule() {
            return false;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            return ElementKind.MODULE;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public java.util.List<Directive> getDirectives() {
            apiComplete();
            completeUsesProvides();
            return Collections.unmodifiableList(directives);
        }

        public void completeUsesProvides() {
            if (usesProvidesCompleter != Completer.NULL_COMPLETER) {
                Completer c = usesProvidesCompleter;
                usesProvidesCompleter = Completer.NULL_COMPLETER;
                c.complete(this);
            }
        }

        @Override
        public ClassSymbol outermostClass() {
            return null;
        }

        @Override
        public String toString() {
            // TODO: the following strings should be localized
            // Do this with custom anon subtypes in Symtab
            String n = (name == null) ? "<unknown>"
                    : (name.isEmpty()) ? "<unnamed>"
                    : String.valueOf(name);
            return n;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitModule(this, p);
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Symbol> getEnclosedElements() {
            List<Symbol> list = List.nil();
            for (Symbol sym : enclosedPackages) {
                if (sym.members().anyMatch(m -> m.kind == TYP))
                    list = list.prepend(sym);
            }
            return list;
        }

        public void reset() {
            this.directives = null;
            this.requires = null;
            this.exports = null;
            this.provides = null;
            this.uses = null;
            this.visiblePackages = null;
        }

    }

    public enum ModuleFlags {
        OPEN(0x0020),
        SYNTHETIC(0x1000),
        MANDATED(0x8000);

        public static int value(Set<ModuleFlags> s) {
            int v = 0;
            for (ModuleFlags f: s)
                v |= f.value;
            return v;
        }

        private ModuleFlags(int value) {
            this.value = value;
        }

        public final int value;
    }

    public enum ModuleResolutionFlags {
        DO_NOT_RESOLVE_BY_DEFAULT(0x0001),
        WARN_DEPRECATED(0x0002),
        WARN_DEPRECATED_REMOVAL(0x0004),
        WARN_INCUBATING(0x0008);

        public static int value(Set<ModuleResolutionFlags> s) {
            int v = 0;
            for (ModuleResolutionFlags f: s)
                v |= f.value;
            return v;
        }

        private ModuleResolutionFlags(int value) {
            this.value = value;
        }

        public final int value;
    }

    /** A class for package symbols
     */
    public static class PackageSymbol extends TypeSymbol
        implements PackageElement {

        public WriteableScope members_field;
        public Name fullname;
        public ClassSymbol package_info; // see bug 6443073
        public ModuleSymbol modle;
        // the file containing the documentation comments for the package
        public JavaFileObject sourcefile;

        public PackageSymbol(Name name, Type type, Symbol owner) {
            super(PCK, 0, name, type, owner);
            this.members_field = null;
            this.fullname = formFullName(name, owner);
        }

        public PackageSymbol(Name name, Symbol owner) {
            this(name, null, owner);
            this.type = new PackageType(this);
        }

        public String toString() {
            return fullname.toString();
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Name getQualifiedName() {
            return fullname;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public boolean isUnnamed() {
            return name.isEmpty() && owner != null;
        }

        public WriteableScope members() {
            complete();
            return members_field;
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_Package;
        }

        public long flags() {
            complete();
            return flags_field;
        }

        @Override
        public List<Attribute.Compound> getRawAttributes() {
            complete();
            if (package_info != null) {
                package_info.complete();
                mergeAttributes();
            }
            return super.getRawAttributes();
        }

        private void mergeAttributes() {
            if (metadata == null &&
                package_info.metadata != null) {
                metadata = new SymbolMetadata(this);
                metadata.setAttributes(package_info.metadata);
            }
        }

        /** A package "exists" if a type or package that exists has
         *  been seen within it.
         */
        public boolean exists() {
            return (flags_field & EXISTS) != 0;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            return ElementKind.PACKAGE;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Symbol getEnclosingElement() {
            return modle != null && !modle.isNoModule() ? modle : null;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitPackage(this, p);
        }

        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitPackageSymbol(this, p);
        }

        /**Resets the Symbol into the state good for next round of annotation processing.*/
        public void reset() {
            metadata = null;
        }

    }

    public static class RootPackageSymbol extends PackageSymbol {
        public final MissingInfoHandler missingInfoHandler;
        public final boolean allowPrivateInvokeVirtual;

        public RootPackageSymbol(Name name, Symbol owner,
                                 MissingInfoHandler missingInfoHandler,
                                 boolean allowPrivateInvokeVirtual) {
            super(name, owner);
            this.missingInfoHandler = missingInfoHandler;
            this.allowPrivateInvokeVirtual = allowPrivateInvokeVirtual;
        }

    }

    /** A class for class symbols
     */
    public static class ClassSymbol extends TypeSymbol implements TypeElement {

        /** a scope for all class members; variables, methods and inner classes
         *  type parameters are not part of this scope
         */
        public WriteableScope members_field;

        /** the fully qualified name of the class, i.e. pck.outer.inner.
         *  null for anonymous classes
         */
        public Name fullname;

        /** the fully qualified name of the class after converting to flat
         *  representation, i.e. pck.outer$inner,
         *  set externally for local and anonymous classes
         */
        public Name flatname;

        /** the sourcefile where the class came from
         */
        public JavaFileObject sourcefile;

        /** the classfile from where to load this class
         *  this will have extension .class or .java
         */
        public JavaFileObject classfile;

        /** the list of translated local classes (used for generating
         * InnerClasses attribute)
         */
        public List<ClassSymbol> trans_local;

        /** the annotation metadata attached to this class */
        private AnnotationTypeMetadata annotationTypeMetadata;

        /* the list of any of record components, only non empty if the class is a record
         * and it has at least one record component
         */
        private List<RecordComponent> recordComponents = List.nil();

        // sealed classes related fields
        /** The classes, or interfaces, permitted to extend this class, or interface
         */
        public List<Symbol> permitted;

        public boolean isPermittedExplicit = false;

        public ClassSymbol(long flags, Name name, Type type, Symbol owner) {
            super(TYP, flags, name, type, owner);
            this.members_field = null;
            this.fullname = formFullName(name, owner);
            this.flatname = formFlatName(name, owner);
            this.sourcefile = null;
            this.classfile = null;
            this.annotationTypeMetadata = AnnotationTypeMetadata.notAnAnnotationType();
            this.permitted = List.nil();
        }

        public ClassSymbol(long flags, Name name, Symbol owner) {
            this(
                flags,
                name,
                new ClassType(Type.noType, null, null),
                owner);
            this.type.tsym = this;
        }

        /** The Java source which this symbol represents.
         */
        public String toString() {
            return className();
        }

        public long flags() {
            complete();
            return flags_field;
        }

        public WriteableScope members() {
            complete();
            return members_field;
        }

        @Override
        public List<Attribute.Compound> getRawAttributes() {
            complete();
            return super.getRawAttributes();
        }

        @Override
        public List<Attribute.TypeCompound> getRawTypeAttributes() {
            complete();
            return super.getRawTypeAttributes();
        }

        public Type erasure(Types types) {
            if (erasure_field == null)
                erasure_field = new ClassType(types.erasure(type.getEnclosingType()),
                                              List.nil(), this,
                                              type.getMetadata());
            return erasure_field;
        }

        public String className() {
            if (name.isEmpty())
                return
                    Log.getLocalizedString("anonymous.class", flatname);
            else
                return fullname.toString();
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Name getQualifiedName() {
            return fullname;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Symbol> getEnclosedElements() {
            List<Symbol> result = super.getEnclosedElements();
            if (!recordComponents.isEmpty()) {
                List<RecordComponent> reversed = recordComponents.reverse();
                for (RecordComponent rc : reversed) {
                    result = result.prepend(rc);
                }
            }
            return result;
        }

        public Name flatName() {
            return flatname;
        }

        public boolean isSubClass(Symbol base, Types types) {
            if (this == base) {
                return true;
            } else if ((base.flags() & INTERFACE) != 0) {
                for (Type t = type; t.hasTag(CLASS); t = types.supertype(t))
                    for (List<Type> is = types.interfaces(t);
                         is.nonEmpty();
                         is = is.tail)
                        if (is.head.tsym.isSubClass(base, types)) return true;
            } else {
                for (Type t = type; t.hasTag(CLASS); t = types.supertype(t))
                    if (t.tsym == base) return true;
            }
            return false;
        }

        /** Complete the elaboration of this symbol's definition.
         */
        public void complete() throws CompletionFailure {
            Completer origCompleter = completer;
            try {
                super.complete();
            } catch (CompletionFailure ex) {
                ex.dcfh.classSymbolCompleteFailed(this, origCompleter);
                // quiet error recovery
                flags_field |= (PUBLIC|STATIC);
                this.type = new ErrorType(this, Type.noType);
                throw ex;
            }
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Type> getInterfaces() {
            apiComplete();
            if (type instanceof ClassType classType) {
                if (classType.interfaces_field == null) // FIXME: shouldn't be null
                    classType.interfaces_field = List.nil();
                if (classType.all_interfaces_field != null)
                    return Type.getModelTypes(classType.all_interfaces_field);
                return classType.interfaces_field;
            } else {
                return List.nil();
            }
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Type getSuperclass() {
            apiComplete();
            if (type instanceof ClassType classType) {
                if (classType.supertype_field == null) // FIXME: shouldn't be null
                    classType.supertype_field = Type.noType;
                // An interface has no superclass; its supertype is Object.
                return classType.isInterface()
                    ? Type.noType
                    : classType.supertype_field.getModelType();
            } else {
                return Type.noType;
            }
        }

        /**
         * Returns the next class to search for inherited annotations or {@code null}
         * if the next class can't be found.
         */
        private ClassSymbol getSuperClassToSearchForAnnotations() {

            Type sup = getSuperclass();

            if (!sup.hasTag(CLASS) || sup.isErroneous())
                return null;

            return (ClassSymbol) sup.tsym;
        }


        @Override
        protected <A extends Annotation> A[] getInheritedAnnotations(Class<A> annoType) {

            ClassSymbol sup = getSuperClassToSearchForAnnotations();

            return sup == null ? super.getInheritedAnnotations(annoType)
                               : sup.getAnnotationsByType(annoType);
        }


        @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            apiComplete();
            long flags = flags();
            if ((flags & ANNOTATION) != 0)
                return ElementKind.ANNOTATION_TYPE;
            else if ((flags & INTERFACE) != 0)
                return ElementKind.INTERFACE;
            else if ((flags & ENUM) != 0)
                return ElementKind.ENUM;
            else if ((flags & RECORD) != 0)
                return ElementKind.RECORD;
            else
                return ElementKind.CLASS;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<Modifier> getModifiers() {
            apiComplete();
            long flags = flags();
            return Flags.asModifierSet(flags & ~DEFAULT);
        }

        public RecordComponent getRecordComponent(VarSymbol field) {
            for (RecordComponent rc : recordComponents) {
                if (rc.name == field.name) {
                    return rc;
                }
            }
            return null;
        }

        public RecordComponent getRecordComponent(JCVariableDecl var, boolean addIfMissing, List<JCAnnotation> annotations) {
            for (RecordComponent rc : recordComponents) {
                /* it could be that a record erroneously declares two record components with the same name, in that
                 * case we need to use the position to disambiguate
                 */
                if (rc.name == var.name && var.pos == rc.pos) {
                    return rc;
                }
            }
            RecordComponent rc = null;
            if (addIfMissing) {
                recordComponents = recordComponents.append(rc = new RecordComponent(var.sym, annotations));
            }
            return rc;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public List<? extends RecordComponent> getRecordComponents() {
            return recordComponents;
        }

        public void setRecordComponents(List<RecordComponent> recordComponents) {
            this.recordComponents = recordComponents;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public NestingKind getNestingKind() {
            apiComplete();
            if (owner.kind == PCK)
                return NestingKind.TOP_LEVEL;
            else if (name.isEmpty())
                return NestingKind.ANONYMOUS;
            else if (owner.kind == MTH)
                return NestingKind.LOCAL;
            else
                return NestingKind.MEMBER;
        }

        @Override
        protected <A extends Annotation> Attribute.Compound getAttribute(final Class<A> annoType) {

            Attribute.Compound attrib = super.getAttribute(annoType);

            boolean inherited = annoType.isAnnotationPresent(Inherited.class);
            if (attrib != null || !inherited)
                return attrib;

            // Search supertypes
            ClassSymbol superType = getSuperClassToSearchForAnnotations();
            return superType == null ? null
                                     : superType.getAttribute(annoType);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitType(this, p);
        }

        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitClassSymbol(this, p);
        }

        public void markAbstractIfNeeded(Types types) {
            if (types.enter.getEnv(this) != null &&
                (flags() & ENUM) != 0 && types.supertype(type).tsym == types.syms.enumSym &&
                (flags() & (FINAL | ABSTRACT)) == 0) {
                if (types.firstUnimplementedAbstract(this) != null)
                    // add the ABSTRACT flag to an enum
                    flags_field |= ABSTRACT;
            }
        }

        /**Resets the Symbol into the state good for next round of annotation processing.*/
        public void reset() {
            kind = TYP;
            erasure_field = null;
            members_field = null;
            flags_field = 0;
            if (type instanceof ClassType classType) {
                classType.setEnclosingType(Type.noType);
                classType.rank_field = -1;
                classType.typarams_field = null;
                classType.allparams_field = null;
                classType.supertype_field = null;
                classType.interfaces_field = null;
                classType.all_interfaces_field = null;
            }
            clearAnnotationMetadata();
        }

        public void clearAnnotationMetadata() {
            metadata = null;
            annotationTypeMetadata = AnnotationTypeMetadata.notAnAnnotationType();
        }

        @Override
        public AnnotationTypeMetadata getAnnotationTypeMetadata() {
            return annotationTypeMetadata;
        }

        @Override
        public boolean isAnnotationType() {
            return (flags_field & Flags.ANNOTATION) != 0;
        }

        public void setAnnotationTypeMetadata(AnnotationTypeMetadata a) {
            Assert.checkNonNull(a);
            Assert.check(!annotationTypeMetadata.isMetadataForAnnotationType());
            this.annotationTypeMetadata = a;
        }

        public boolean isRecord() {
            return (flags_field & RECORD) != 0;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Type> getPermittedSubclasses() {
            return permitted.map(s -> s.type);
        }
    }


    /** A class for variable symbols
     */
    public static class VarSymbol extends Symbol implements VariableElement {

        /** The variable's declaration position.
         */
        public int pos = Position.NOPOS;

        /** The variable's address. Used for different purposes during
         *  flow analysis, translation and code generation.
         *  Flow analysis:
         *    If this is a blank final or local variable, its sequence number.
         *  Translation:
         *    If this is a private field, its access number.
         *  Code generation:
         *    If this is a local variable, its logical slot number.
         */
        public int adr = -1;

        /** Construct a variable symbol, given its flags, name, type and owner.
         */
        public VarSymbol(long flags, Name name, Type type, Symbol owner) {
            super(VAR, flags, name, type, owner);
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_Fieldref;
        }

        public MethodHandleSymbol asMethodHandle(boolean getter) {
            return new MethodHandleSymbol(this, getter);
        }

        /** Clone this symbol with new owner.
         */
        public VarSymbol clone(Symbol newOwner) {
            VarSymbol v = new VarSymbol(flags_field, name, type, newOwner) {
                @Override
                public Symbol baseSymbol() {
                    return VarSymbol.this;
                }

                @Override
                public Object poolKey(Types types) {
                    return new Pair<>(newOwner, baseSymbol());
                }
            };
            v.pos = pos;
            v.adr = adr;
            v.data = data;
//          System.out.println("clone " + v + " in " + newOwner);//DEBUG
            return v;
        }

        public String toString() {
            return name.toString();
        }

        public Symbol asMemberOf(Type site, Types types) {
            return new VarSymbol(flags_field, name, types.memberType(site, this), owner);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            long flags = flags();
            if ((flags & PARAMETER) != 0) {
                if (isExceptionParameter())
                    return ElementKind.EXCEPTION_PARAMETER;
                else
                    return ElementKind.PARAMETER;
            } else if ((flags & ENUM) != 0) {
                return ElementKind.ENUM_CONSTANT;
            } else if (owner.kind == TYP || owner.kind == ERR) {
                return ElementKind.FIELD;
            } else if (isResourceVariable()) {
                return ElementKind.RESOURCE_VARIABLE;
            } else if ((flags & MATCH_BINDING) != 0) {
                ElementKind kind = ElementKind.BINDING_VARIABLE;
                return kind;
            } else {
                return ElementKind.LOCAL_VARIABLE;
            }
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitVariable(this, p);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Object getConstantValue() { // Mirror API
            return Constants.decode(getConstValue(), type);
        }

        public void setLazyConstValue(final Env<AttrContext> env,
                                      final Attr attr,
                                      final JCVariableDecl variable)
        {
            setData((Callable<Object>)() -> attr.attribLazyConstantValue(env, variable, type));
        }

        /**
         * The variable's constant value, if this is a constant.
         * Before the constant value is evaluated, it points to an
         * initializer environment.  If this is not a constant, it can
         * be used for other stuff.
         */
        private Object data;

        public boolean isExceptionParameter() {
            return data == ElementKind.EXCEPTION_PARAMETER;
        }

        public boolean isResourceVariable() {
            return data == ElementKind.RESOURCE_VARIABLE;
        }

        public Object getConstValue() {
            // TODO: Consider if getConstValue and getConstantValue can be collapsed
            if (data == ElementKind.EXCEPTION_PARAMETER ||
                data == ElementKind.RESOURCE_VARIABLE) {
                return null;
            } else if (data instanceof Callable<?> callableData) {
                // In this case, this is a final variable, with an as
                // yet unevaluated initializer.
                data = null; // to make sure we don't evaluate this twice.
                try {
                    data = callableData.call();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }
            return data;
        }

        public void setData(Object data) {
            Assert.check(!(data instanceof Env<?>), this);
            this.data = data;
        }

        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitVarSymbol(this, p);
        }
    }

    public static class RecordComponent extends VarSymbol implements RecordComponentElement {
        public MethodSymbol accessor;
        public JCTree.JCMethodDecl accessorMeth;
        /* the original annotations applied to the record component
         */
        private final List<JCAnnotation> originalAnnos;
        /* if the user happens to erroneously declare two components with the same name, we need a way to differentiate
         * them, the code will fail anyway but we need to keep the information for better error recovery
         */
        private final int pos;

        private final boolean isVarargs;

        /**
         * Construct a record component, given its flags, name, type and owner.
         */
        public RecordComponent(Name name, Type type, Symbol owner) {
            super(PUBLIC, name, type, owner);
            pos = -1;
            originalAnnos = List.nil();
            isVarargs = false;
        }

        public RecordComponent(VarSymbol field, List<JCAnnotation> annotations) {
            super(PUBLIC, field.name, field.type, field.owner);
            this.originalAnnos = annotations;
            this.pos = field.pos;
            /* it is better to store the original information for this one, instead of relying
             * on the info in the type of the symbol. This is because on the presence of APs
             * the symbol will be blown out and we won't be able to know if the original
             * record component was declared varargs or not.
             */
            this.isVarargs = type.hasTag(TypeTag.ARRAY) && ((ArrayType)type).isVarargs();
        }

        public List<JCAnnotation> getOriginalAnnos() { return originalAnnos; }

        public boolean isVarargs() {
            return isVarargs;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            return ElementKind.RECORD_COMPONENT;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public ExecutableElement getAccessor() {
            return accessor;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitRecordComponent(this, p);
        }
    }

    public static class ParamSymbol extends VarSymbol {
        public ParamSymbol(long flags, Name name, Type type, Symbol owner) {
            super(flags, name, type, owner);
        }

        @Override
        public Name getSimpleName() {
            if ((flags_field & NAME_FILLED) == 0) {
                flags_field |= NAME_FILLED;
                Symbol rootPack = this;
                while (rootPack != null && !(rootPack instanceof RootPackageSymbol)) {
                    rootPack = rootPack.owner;
                }
                if (rootPack != null) {
                    Name inferredName =
                            ((RootPackageSymbol) rootPack).missingInfoHandler.getParameterName(this);
                    if (inferredName != null) {
                        this.name = inferredName;
                    }
                }
            }
            return super.getSimpleName();
        }

    }

    public static class BindingSymbol extends VarSymbol {

        public BindingSymbol(long flags, Name name, Type type, Symbol owner) {
            super(flags | Flags.HASINIT | Flags.MATCH_BINDING, name, type, owner);
        }

        public boolean isAliasFor(BindingSymbol b) {
            return aliases().containsAll(b.aliases());
        }

        List<BindingSymbol> aliases() {
            return List.of(this);
        }

        public void preserveBinding() {
            flags_field |= Flags.MATCH_BINDING_TO_OUTER;
        }

        public boolean isPreserved() {
            return (flags_field & Flags.MATCH_BINDING_TO_OUTER) != 0;
        }
    }

    /** A class for method symbols.
     */
    public static class MethodSymbol extends Symbol implements ExecutableElement {

        /** The code of the method. */
        public Code code = null;

        /** The extra (synthetic/mandated) parameters of the method. */
        public List<VarSymbol> extraParams = List.nil();

        /** The captured local variables in an anonymous class */
        public List<VarSymbol> capturedLocals = List.nil();

        /** The parameters of the method. */
        public List<VarSymbol> params = null;

        /** For an annotation type element, its default value if any.
         *  The value is null if none appeared in the method
         *  declaration.
         */
        public Attribute defaultValue = null;

        /** Construct a method symbol, given its flags, name, type and owner.
         */
        public MethodSymbol(long flags, Name name, Type type, Symbol owner) {
            super(MTH, flags, name, type, owner);
            if (owner.type.hasTag(TYPEVAR)) Assert.error(owner + "." + name);
        }

        /** Clone this symbol with new owner.
         */
        public MethodSymbol clone(Symbol newOwner) {
            MethodSymbol m = new MethodSymbol(flags_field, name, type, newOwner) {
                @Override
                public Symbol baseSymbol() {
                    return MethodSymbol.this;
                }

                @Override
                public Object poolKey(Types types) {
                    return new Pair<>(newOwner, baseSymbol());
                }
            };
            m.code = code;
            return m;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<Modifier> getModifiers() {
            long flags = flags();
            return Flags.asModifierSet((flags & DEFAULT) != 0 ? flags & ~ABSTRACT : flags);
        }

        /** The Java source which this symbol represents.
         */
        public String toString() {
            if ((flags() & BLOCK) != 0) {
                return owner.name.toString();
            } else {
                String s = (name == name.table.names.init)
                    ? owner.name.toString()
                    : name.toString();
                if (type != null) {
                    if (type.hasTag(FORALL))
                        s = "<" + ((ForAll)type).getTypeArguments() + ">" + s;
                    s += "(" + type.argtypes((flags() & VARARGS) != 0) + ")";
                }
                return s;
            }
        }

        @Override
        public int poolTag() {
            return owner.isInterface() ?
                    ClassFile.CONSTANT_InterfaceMethodref : ClassFile.CONSTANT_Methodref;
        }

        public boolean isHandle() {
            return false;
        }


        public MethodHandleSymbol asHandle() {
            return new MethodHandleSymbol(this);
        }

        /** find a symbol that this (proxy method) symbol implements.
         *  @param    c       The class whose members are searched for
         *                    implementations
         */
        public Symbol implemented(TypeSymbol c, Types types) {
            Symbol impl = null;
            for (List<Type> is = types.interfaces(c.type);
                 impl == null && is.nonEmpty();
                 is = is.tail) {
                TypeSymbol i = is.head.tsym;
                impl = implementedIn(i, types);
                if (impl == null)
                    impl = implemented(i, types);
            }
            return impl;
        }

        public Symbol implementedIn(TypeSymbol c, Types types) {
            Symbol impl = null;
            for (Symbol sym : c.members().getSymbolsByName(name)) {
                if (this.overrides(sym, (TypeSymbol)owner, types, true) &&
                    // FIXME: I suspect the following requires a
                    // subst() for a parametric return type.
                    types.isSameType(type.getReturnType(),
                                     types.memberType(owner.type, sym).getReturnType())) {
                    impl = sym;
                }
            }
            return impl;
        }

        /** Will the erasure of this method be considered by the VM to
         *  override the erasure of the other when seen from class `origin'?
         */
        public boolean binaryOverrides(Symbol _other, TypeSymbol origin, Types types) {
            if (isConstructor() || _other.kind != MTH) return false;

            if (this == _other) return true;
            MethodSymbol other = (MethodSymbol)_other;

            // check for a direct implementation
            if (other.isOverridableIn((TypeSymbol)owner) &&
                types.asSuper(owner.type, other.owner) != null &&
                types.isSameType(erasure(types), other.erasure(types)))
                return true;

            // check for an inherited implementation
            return
                (flags() & ABSTRACT) == 0 &&
                other.isOverridableIn(origin) &&
                this.isMemberOf(origin, types) &&
                types.isSameType(erasure(types), other.erasure(types));
        }

        /** The implementation of this (abstract) symbol in class origin,
         *  from the VM's point of view, null if method does not have an
         *  implementation in class.
         *  @param origin   The class of which the implementation is a member.
         */
        public MethodSymbol binaryImplementation(ClassSymbol origin, Types types) {
            for (TypeSymbol c = origin; c != null; c = types.supertype(c.type).tsym) {
                for (Symbol sym : c.members().getSymbolsByName(name)) {
                    if (sym.kind == MTH &&
                        ((MethodSymbol)sym).binaryOverrides(this, origin, types))
                        return (MethodSymbol)sym;
                }
            }
            return null;
        }

        /** Does this symbol override `other' symbol, when both are seen as
         *  members of class `origin'?  It is assumed that _other is a member
         *  of origin.
         *
         *  It is assumed that both symbols have the same name.  The static
         *  modifier is ignored for this test.
         *
         *  A quirk in the works is that if the receiver is a method symbol for
         *  an inherited abstract method we answer false summarily all else being
         *  immaterial. Abstract "own" methods (i.e `this' is a direct member of
         *  origin) don't get rejected as summarily and are put to test against the
         *  suitable criteria.
         *
         *  See JLS 8.4.6.1 (without transitivity) and 8.4.6.4
         */
        public boolean overrides(Symbol _other, TypeSymbol origin, Types types, boolean checkResult) {
            return overrides(_other, origin, types, checkResult, true);
        }

        /** Does this symbol override `other' symbol, when both are seen as
         *  members of class `origin'?  It is assumed that _other is a member
         *  of origin.
         *
         *  Caveat: If `this' is an abstract inherited member of origin, it is
         *  deemed to override `other' only when `requireConcreteIfInherited'
         *  is false.
         *
         *  It is assumed that both symbols have the same name.  The static
         *  modifier is ignored for this test.
         *
         *  See JLS 8.4.6.1 (without transitivity) and 8.4.6.4
         */
        public boolean overrides(Symbol _other, TypeSymbol origin, Types types, boolean checkResult,
                                            boolean requireConcreteIfInherited) {
            if (isConstructor() || _other.kind != MTH) return false;

            if (this == _other) return true;
            MethodSymbol other = (MethodSymbol)_other;

            // check for a direct implementation
            if (other.isOverridableIn((TypeSymbol)owner) &&
                types.asSuper(owner.type, other.owner) != null) {
                Type mt = types.memberType(owner.type, this);
                Type ot = types.memberType(owner.type, other);
                if (types.isSubSignature(mt, ot)) {
                    if (!checkResult)
                        return true;
                    if (types.returnTypeSubstitutable(mt, ot))
                        return true;
                }
            }

            // check for an inherited implementation
            if (((flags() & ABSTRACT) != 0 && requireConcreteIfInherited) ||
                    ((other.flags() & ABSTRACT) == 0 && (other.flags() & DEFAULT) == 0) ||
                    !other.isOverridableIn(origin) ||
                    !this.isMemberOf(origin, types))
                return false;

            // assert types.asSuper(origin.type, other.owner) != null;
            Type mt = types.memberType(origin.type, this);
            Type ot = types.memberType(origin.type, other);
            return
                types.isSubSignature(mt, ot) &&
                (!checkResult || types.resultSubtype(mt, ot, types.noWarnings));
        }

        private boolean isOverridableIn(TypeSymbol origin) {
            // JLS 8.4.6.1
            switch ((int)(flags_field & Flags.AccessFlags)) {
            case Flags.PRIVATE:
                return false;
            case Flags.PUBLIC:
                return !this.owner.isInterface() ||
                        (flags_field & STATIC) == 0;
            case Flags.PROTECTED:
                return (origin.flags() & INTERFACE) == 0;
            case 0:
                // for package private: can only override in the same
                // package
                return
                    this.packge() == origin.packge() &&
                    (origin.flags() & INTERFACE) == 0;
            default:
                return false;
            }
        }

        @Override
        public boolean isInheritedIn(Symbol clazz, Types types) {
            switch ((int)(flags_field & Flags.AccessFlags)) {
                case PUBLIC:
                    return !this.owner.isInterface() ||
                            clazz == owner ||
                            (flags_field & STATIC) == 0;
                default:
                    return super.isInheritedIn(clazz, types);
            }
        }

        public boolean isLambdaMethod() {
            return (flags() & LAMBDA_METHOD) == LAMBDA_METHOD;
        }

        /** override this method to point to the original enclosing method if this method symbol represents a synthetic
         *  lambda method
         */
        public MethodSymbol originalEnclosingMethod() {
            return this;
        }

        /** The implementation of this (abstract) symbol in class origin;
         *  null if none exists. Synthetic methods are not considered
         *  as possible implementations.
         */
        public MethodSymbol implementation(TypeSymbol origin, Types types, boolean checkResult) {
            return implementation(origin, types, checkResult, implementation_filter);
        }
        // where
            public static final Predicate<Symbol> implementation_filter = s ->
                    s.kind == MTH && (s.flags() & SYNTHETIC) == 0;

        public MethodSymbol implementation(TypeSymbol origin, Types types, boolean checkResult, Predicate<Symbol> implFilter) {
            MethodSymbol res = types.implementation(this, origin, checkResult, implFilter);
            if (res != null)
                return res;
            // if origin is derived from a raw type, we might have missed
            // an implementation because we do not know enough about instantiations.
            // in this case continue with the supertype as origin.
            if (types.isDerivedRaw(origin.type) && !origin.isInterface())
                return implementation(types.supertype(origin.type).tsym, types, checkResult);
            else
                return null;
        }

        public List<VarSymbol> params() {
            owner.complete();
            if (params == null) {
                ListBuffer<VarSymbol> newParams = new ListBuffer<>();
                int i = 0;
                for (Type t : type.getParameterTypes()) {
                    Name paramName = name.table.fromString("arg" + i);
                    VarSymbol param = new VarSymbol(PARAMETER, paramName, t, this);
                    newParams.append(param);
                    i++;
                }
                params = newParams.toList();
            }
            Assert.checkNonNull(params);
            return params;
        }

        public Symbol asMemberOf(Type site, Types types) {
            return new MethodSymbol(flags_field, name, types.memberType(site, this), owner);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public ElementKind getKind() {
            if (name == name.table.names.init)
                return ElementKind.CONSTRUCTOR;
            else if (name == name.table.names.clinit)
                return ElementKind.STATIC_INIT;
            else if ((flags() & BLOCK) != 0)
                return isStatic() ? ElementKind.STATIC_INIT : ElementKind.INSTANCE_INIT;
            else
                return ElementKind.METHOD;
        }

        public boolean isStaticOrInstanceInit() {
            return getKind() == ElementKind.STATIC_INIT ||
                    getKind() == ElementKind.INSTANCE_INIT;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Attribute getDefaultValue() {
            return defaultValue;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public List<VarSymbol> getParameters() {
            return params();
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public boolean isVarArgs() {
            return (flags() & VARARGS) != 0;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public boolean isDefault() {
            return (flags() & DEFAULT) != 0;
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }

        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitMethodSymbol(this, p);
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Type getReceiverType() {
            return asType().getReceiverType();
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public Type getReturnType() {
            return asType().getReturnType();
        }

        @DefinedBy(Api.LANGUAGE_MODEL)
        public List<Type> getThrownTypes() {
            return asType().getThrownTypes();
        }
    }

    /** A class for invokedynamic method calls.
     */
    public static class DynamicMethodSymbol extends MethodSymbol implements Dynamic {

        public LoadableConstant[] staticArgs;
        public MethodHandleSymbol bsm;

        public DynamicMethodSymbol(Name name, Symbol owner, MethodHandleSymbol bsm, Type type, LoadableConstant[] staticArgs) {
            super(0, name, type, owner);
            this.bsm = bsm;
            this.staticArgs = staticArgs;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Override
        public LoadableConstant[] staticArgs() {
            return staticArgs;
        }

        @Override
        public MethodHandleSymbol bootstrapMethod() {
            return bsm;
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_InvokeDynamic;
        }

        @Override
        public Type dynamicType() {
            return type;
        }
    }

    /** A class for condy.
     */
    public static class DynamicVarSymbol extends VarSymbol implements Dynamic, LoadableConstant {
        public LoadableConstant[] staticArgs;
        public MethodHandleSymbol bsm;

        public DynamicVarSymbol(Name name, Symbol owner, MethodHandleSymbol bsm, Type type, LoadableConstant[] staticArgs) {
            super(0, name, type, owner);
            this.bsm = bsm;
            this.staticArgs = staticArgs;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Override
        public PoolConstant dynamicType() {
            return type;
        }

        @Override
        public LoadableConstant[] staticArgs() {
            return staticArgs;
        }

        @Override
        public LoadableConstant bootstrapMethod() {
            return bsm;
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_Dynamic;
        }
    }

    /** A class for method handles.
     */
    public static class MethodHandleSymbol extends MethodSymbol implements LoadableConstant {

        private Symbol refSym;
        private boolean getter;

        public MethodHandleSymbol(Symbol msym) {
            this(msym, false);
        }

        public MethodHandleSymbol(Symbol msym, boolean getter) {
            super(msym.flags_field, msym.name, msym.type, msym.owner);
            this.refSym = msym;
            this.getter = getter;
        }

        /**
         * Returns the kind associated with this method handle.
         */
        public int referenceKind() {
            if (refSym.kind == VAR) {
                return getter ?
                        refSym.isStatic() ? ClassFile.REF_getStatic : ClassFile.REF_getField :
                        refSym.isStatic() ? ClassFile.REF_putStatic : ClassFile.REF_putField;
            } else {
                if (refSym.isConstructor()) {
                    return ClassFile.REF_newInvokeSpecial;
                } else {
                    if (refSym.isStatic()) {
                        return ClassFile.REF_invokeStatic;
                    } else if ((refSym.flags() & PRIVATE) != 0 && !allowPrivateInvokeVirtual()) {
                        return ClassFile.REF_invokeSpecial;
                    } else if (refSym.enclClass().isInterface()) {
                        return ClassFile.REF_invokeInterface;
                    } else {
                        return ClassFile.REF_invokeVirtual;
                    }
                }
            }
        }

        private boolean allowPrivateInvokeVirtual() {
            Symbol rootPack = this;
            while (rootPack != null && !(rootPack instanceof RootPackageSymbol)) {
                rootPack = rootPack.owner;
            }
            return rootPack != null && ((RootPackageSymbol) rootPack).allowPrivateInvokeVirtual;
        }
        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_MethodHandle;
        }

        @Override
        public Object poolKey(Types types) {
            return new Pair<>(baseSymbol(), referenceKind());
        }

        @Override
        public MethodHandleSymbol asHandle() {
            return this;
        }

        @Override
        public Symbol baseSymbol() {
            return refSym;
        }


        @Override
        public boolean isHandle() {
            return true;
        }
    }

    /** A class for predefined operators.
     */
    public static class OperatorSymbol extends MethodSymbol {

        public int opcode;
        private int accessCode = Integer.MIN_VALUE;

        public OperatorSymbol(Name name, Type type, int opcode, Symbol owner) {
            super(PUBLIC | STATIC, name, type, owner);
            this.opcode = opcode;
        }

        @Override
        public <R, P> R accept(Symbol.Visitor<R, P> v, P p) {
            return v.visitOperatorSymbol(this, p);
        }

        public int getAccessCode(Tag tag) {
            if (accessCode != Integer.MIN_VALUE && !tag.isIncOrDecUnaryOp()) {
                return accessCode;
            }
            accessCode = AccessCode.from(tag, opcode);
            return accessCode;
        }

        /** Access codes for dereferencing, assignment,
         *  and pre/post increment/decrement.

         *  All access codes for accesses to the current class are even.
         *  If a member of the superclass should be accessed instead (because
         *  access was via a qualified super), add one to the corresponding code
         *  for the current class, making the number odd.
         *  This numbering scheme is used by the backend to decide whether
         *  to issue an invokevirtual or invokespecial call.
         *
         *  @see Gen#visitSelect(JCFieldAccess tree)
         */
        public enum AccessCode {
            UNKNOWN(-1, Tag.NO_TAG),
            DEREF(0, Tag.NO_TAG),
            ASSIGN(2, Tag.ASSIGN),
            PREINC(4, Tag.PREINC),
            PREDEC(6, Tag.PREDEC),
            POSTINC(8, Tag.POSTINC),
            POSTDEC(10, Tag.POSTDEC),
            FIRSTASGOP(12, Tag.NO_TAG);

            public final int code;
            public final Tag tag;
            public static final int numberOfAccessCodes = (lushrl - ishll + lxor + 2 - iadd) * 2 + FIRSTASGOP.code + 2;

            AccessCode(int code, Tag tag) {
                this.code = code;
                this.tag = tag;
            }

            public static AccessCode getFromCode(int code) {
                for (AccessCode aCodes : AccessCode.values()) {
                    if (aCodes.code == code) {
                        return aCodes;
                    }
                }
                return UNKNOWN;
            }

            static int from(Tag tag, int opcode) {
                /** Map bytecode of binary operation to access code of corresponding
                *  assignment operation. This is always an even number.
                */
                switch (tag) {
                    case PREINC:
                        return AccessCode.PREINC.code;
                    case PREDEC:
                        return AccessCode.PREDEC.code;
                    case POSTINC:
                        return AccessCode.POSTINC.code;
                    case POSTDEC:
                        return AccessCode.POSTDEC.code;
                }
                if (iadd <= opcode && opcode <= lxor) {
                    return (opcode - iadd) * 2 + FIRSTASGOP.code;
                } else if (opcode == string_add) {
                    return (lxor + 1 - iadd) * 2 + FIRSTASGOP.code;
                } else if (ishll <= opcode && opcode <= lushrl) {
                    return (opcode - ishll + lxor + 2 - iadd) * 2 + FIRSTASGOP.code;
                }
                return -1;
            }
        }
    }

    /** Symbol completer interface.
     */
    public static interface Completer {

        /** Dummy completer to be used when the symbol has been completed or
         * does not need completion.
         */
        public static final Completer NULL_COMPLETER = new Completer() {
            public void complete(Symbol sym) { }
            public boolean isTerminal() { return true; }
        };

        void complete(Symbol sym) throws CompletionFailure;

        /** Returns true if this completer is <em>terminal</em>. A terminal
         * completer is used as a place holder when the symbol is completed.
         * Calling complete on a terminal completer will not affect the symbol.
         *
         * The dummy NULL_COMPLETER and the GraphDependencies completer are
         * examples of terminal completers.
         *
         * @return true iff this completer is terminal
         */
        default boolean isTerminal() {
            return false;
        }
    }

    public static class CompletionFailure extends RuntimeException {
        private static final long serialVersionUID = 0;
        public final transient DeferredCompletionFailureHandler dcfh;
        public transient Symbol sym;

        /** A diagnostic object describing the failure
         */
        private transient JCDiagnostic diag;

        private transient Supplier<JCDiagnostic> diagSupplier;

        public CompletionFailure(Symbol sym, Supplier<JCDiagnostic> diagSupplier, DeferredCompletionFailureHandler dcfh) {
            this.dcfh = dcfh;
            this.sym = sym;
            this.diagSupplier = diagSupplier;
//          this.printStackTrace();//DEBUG
        }

        public JCDiagnostic getDiagnostic() {
            if (diag == null && diagSupplier != null) {
                diag = diagSupplier.get();
            }
            return diag;
        }

        @Override
        public String getMessage() {
            return getDiagnostic().getMessage(null);
        }

        public JCDiagnostic getDetailValue() {
            return getDiagnostic();
        }

        @Override
        public CompletionFailure initCause(Throwable cause) {
            super.initCause(cause);
            return this;
        }

        public void resetDiagnostic(Supplier<JCDiagnostic> diagSupplier) {
            this.diagSupplier = diagSupplier;
            this.diag = null;
        }

    }

    /**
     * A visitor for symbols.  A visitor is used to implement operations
     * (or relations) on symbols.  Most common operations on types are
     * binary relations and this interface is designed for binary
     * relations, that is, operations on the form
     * Symbol&nbsp;&times;&nbsp;P&nbsp;&rarr;&nbsp;R.
     * <!-- In plain text: Type x P -> R -->
     *
     * @param <R> the return type of the operation implemented by this
     * visitor; use Void if no return type is needed.
     * @param <P> the type of the second argument (the first being the
     * symbol itself) of the operation implemented by this visitor; use
     * Void if a second argument is not needed.
     */
    public interface Visitor<R,P> {
        R visitClassSymbol(ClassSymbol s, P arg);
        R visitMethodSymbol(MethodSymbol s, P arg);
        R visitPackageSymbol(PackageSymbol s, P arg);
        R visitOperatorSymbol(OperatorSymbol s, P arg);
        R visitVarSymbol(VarSymbol s, P arg);
        R visitTypeSymbol(TypeSymbol s, P arg);
        R visitSymbol(Symbol s, P arg);
    }
}
