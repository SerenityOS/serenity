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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.ElementVisitor;

import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.RootPackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.BottomType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.ErrorType;
import com.sun.tools.javac.code.Type.JCPrimitiveType;
import com.sun.tools.javac.code.Type.JCVoidType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Type.UnknownType;
import com.sun.tools.javac.code.Types.UniqueType;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.Iterators;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.*;

/** A class that defines all predefined constants and operators
 *  as well as special classes such as java.lang.Object, which need
 *  to be known to the compiler. All symbols are held in instance
 *  fields. This makes it possible to work in multiple concurrent
 *  projects, which might use different class files for library classes.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Symtab {
    /** The context key for the symbol table. */
    protected static final Context.Key<Symtab> symtabKey = new Context.Key<>();

    /** Get the symbol table instance. */
    public static Symtab instance(Context context) {
        Symtab instance = context.get(symtabKey);
        if (instance == null)
            instance = new Symtab(context);
        return instance;
    }

    /** Builtin types.
     */
    public final JCPrimitiveType byteType = new JCPrimitiveType(BYTE, null);
    public final JCPrimitiveType charType = new JCPrimitiveType(CHAR, null);
    public final JCPrimitiveType shortType = new JCPrimitiveType(SHORT, null);
    public final JCPrimitiveType intType = new JCPrimitiveType(INT, null);
    public final JCPrimitiveType longType = new JCPrimitiveType(LONG, null);
    public final JCPrimitiveType floatType = new JCPrimitiveType(FLOAT, null);
    public final JCPrimitiveType doubleType = new JCPrimitiveType(DOUBLE, null);
    public final JCPrimitiveType booleanType = new JCPrimitiveType(BOOLEAN, null);
    public final Type botType = new BottomType();
    public final JCVoidType voidType = new JCVoidType();

    private final Names names;
    private final JavacMessages messages;
    private final Completer initialCompleter;
    private final Completer moduleCompleter;

    /** A symbol for the unnamed module.
     */
    public final ModuleSymbol unnamedModule;

    /** The error module.
     */
    public final ModuleSymbol errModule;

    /** A symbol for no module, for use with -source 8 or less
     */
    public final ModuleSymbol noModule;

    /** A symbol for the root package.
     */
    public final PackageSymbol rootPackage;

    /** A symbol that stands for a missing symbol.
     */
    public final TypeSymbol noSymbol;

    /** The error symbol.
     */
    public final ClassSymbol errSymbol;

    /** The unknown symbol.
     */
    public final ClassSymbol unknownSymbol;

    /** A value for the errType, with a originalType of noType */
    public final Type errType;

    /** A value for the unknown type. */
    public final Type unknownType;

    /** The builtin type of all arrays. */
    public final ClassSymbol arrayClass;
    public final MethodSymbol arrayCloneMethod;

    /** VGJ: The (singleton) type of all bound types. */
    public final ClassSymbol boundClass;

    /** The builtin type of all methods. */
    public final ClassSymbol methodClass;

    /** A symbol for the java.base module.
     */
    public final ModuleSymbol java_base;

    /** Predefined types.
     */
    public final Type objectType;
    public final Type objectMethodsType;
    public final Type objectsType;
    public final Type classType;
    public final Type classLoaderType;
    public final Type stringType;
    public final Type stringBufferType;
    public final Type stringBuilderType;
    public final Type cloneableType;
    public final Type serializableType;
    public final Type serializedLambdaType;
    public final Type varHandleType;
    public final Type methodHandleType;
    public final Type methodHandleLookupType;
    public final Type methodTypeType;
    public final Type nativeHeaderType;
    public final Type throwableType;
    public final Type errorType;
    public final Type interruptedExceptionType;
    public final Type illegalArgumentExceptionType;
    public final Type exceptionType;
    public final Type runtimeExceptionType;
    public final Type classNotFoundExceptionType;
    public final Type noClassDefFoundErrorType;
    public final Type noSuchFieldErrorType;
    public final Type assertionErrorType;
    public final Type incompatibleClassChangeErrorType;
    public final Type cloneNotSupportedExceptionType;
    public final Type annotationType;
    public final TypeSymbol enumSym;
    public final Type listType;
    public final Type collectionsType;
    public final Type comparableType;
    public final Type comparatorType;
    public final Type arraysType;
    public final Type iterableType;
    public final Type iteratorType;
    public final Type annotationTargetType;
    public final Type overrideType;
    public final Type retentionType;
    public final Type deprecatedType;
    public final Type suppressWarningsType;
    public final Type supplierType;
    public final Type inheritedType;
    public final Type profileType;
    public final Type proprietaryType;
    public final Type systemType;
    public final Type autoCloseableType;
    public final Type trustMeType;
    public final Type lambdaMetafactory;
    public final Type stringConcatFactory;
    public final Type repeatableType;
    public final Type documentedType;
    public final Type elementTypeType;
    public final Type functionalInterfaceType;
    public final Type previewFeatureType;
    public final Type previewFeatureInternalType;
    public final Type typeDescriptorType;
    public final Type recordType;
    public final Type switchBootstrapsType;
    public final Type valueBasedType;
    public final Type valueBasedInternalType;

    /** The symbol representing the length field of an array.
     */
    public final VarSymbol lengthVar;

    /** The symbol representing the final finalize method on enums */
    public final MethodSymbol enumFinalFinalize;

    /** The symbol representing the close method on TWR AutoCloseable type */
    public final MethodSymbol autoCloseableClose;

    /** The predefined type that belongs to a tag.
     */
    public final Type[] typeOfTag = new Type[TypeTag.getTypeTagCount()];

    /** The name of the class that belongs to a basic type tag.
     */
    public final Name[] boxedName = new Name[TypeTag.getTypeTagCount()];

    /** A hashtable containing the encountered top-level and member classes,
     *  indexed by flat names. The table does not contain local classes.
     *  It should be updated from the outside to reflect classes defined
     *  by compiled source files.
     */
    private final Map<Name, Map<ModuleSymbol,ClassSymbol>> classes = new HashMap<>();

    /** A hashtable containing the encountered packages.
     *  the table should be updated from outside to reflect packages defined
     *  by compiled source files.
     */
    private final Map<Name, Map<ModuleSymbol,PackageSymbol>> packages = new HashMap<>();

    /** A hashtable giving the encountered modules.
     */
    private final Map<Name, ModuleSymbol> modules = new LinkedHashMap<>();

    private final Map<Types.UniqueType, VarSymbol> classFields = new HashMap<>();

    public VarSymbol getClassField(Type type, Types types) {
        return classFields.computeIfAbsent(
            new UniqueType(type, types), k -> {
                Type arg = null;
                if (type.getTag() == ARRAY || type.getTag() == CLASS)
                    arg = types.erasure(type);
                else if (type.isPrimitiveOrVoid())
                    arg = types.boxedClass(type).type;
                else
                    throw new AssertionError(type);

                Type t = new ClassType(
                    classType.getEnclosingType(), List.of(arg), classType.tsym);
                return new VarSymbol(
                    STATIC | PUBLIC | FINAL, names._class, t, type.tsym);
            });
    }

    public void initType(Type type, ClassSymbol c) {
        type.tsym = c;
        typeOfTag[type.getTag().ordinal()] = type;
    }

    public void initType(Type type, String name) {
        initType(
            type,
            new ClassSymbol(
                PUBLIC, names.fromString(name), type, rootPackage));
    }

    public void initType(Type type, String name, String bname) {
        initType(type, name);
        boxedName[type.getTag().ordinal()] = names.fromString("java.lang." + bname);
    }

    /** The class symbol that owns all predefined symbols.
     */
    public final ClassSymbol predefClass;

    /** Enter a class into symbol table.
     *  @param s The name of the class.
     */
    private Type enterClass(String s) {
        return enterClass(java_base, names.fromString(s)).type;
    }

    public void synthesizeEmptyInterfaceIfMissing(final Type type) {
        final Completer completer = type.tsym.completer;
        type.tsym.completer = new Completer() {
            @Override
            public void complete(Symbol sym) throws CompletionFailure {
                try {
                    completer.complete(sym);
                } catch (CompletionFailure e) {
                    sym.flags_field |= (PUBLIC | INTERFACE);
                    ((ClassType) sym.type).supertype_field = objectType;
                }
            }

            @Override
            public boolean isTerminal() {
                return completer.isTerminal();
            }
        };
    }

    public void synthesizeBoxTypeIfMissing(final Type type) {
        ClassSymbol sym = enterClass(java_base, boxedName[type.getTag().ordinal()]);
        final Completer completer = sym.completer;
        sym.completer = new Completer() {
            @Override
            public void complete(Symbol sym) throws CompletionFailure {
                try {
                    completer.complete(sym);
                } catch (CompletionFailure e) {
                    sym.flags_field |= PUBLIC;
                    ((ClassType) sym.type).supertype_field = objectType;
                    MethodSymbol boxMethod =
                        new MethodSymbol(PUBLIC | STATIC, names.valueOf,
                                         new MethodType(List.of(type), sym.type,
                                List.nil(), methodClass),
                            sym);
                    sym.members().enter(boxMethod);
                    MethodSymbol unboxMethod =
                        new MethodSymbol(PUBLIC,
                            type.tsym.name.append(names.Value), // x.intValue()
                            new MethodType(List.nil(), type,
                                List.nil(), methodClass),
                            sym);
                    sym.members().enter(unboxMethod);
                }
            }

            @Override
            public boolean isTerminal() {
                return completer.isTerminal();
            }
        };
    }

    // Enter a synthetic class that is used to mark classes in ct.sym.
    // This class does not have a class file.
    private Type enterSyntheticAnnotation(String name) {
        // for now, leave the module null, to prevent problems from synthesizing the
        // existence of a class in any specific module, including noModule
        ClassType type = (ClassType)enterClass(java_base, names.fromString(name)).type;
        ClassSymbol sym = (ClassSymbol)type.tsym;
        sym.completer = Completer.NULL_COMPLETER;
        sym.flags_field = PUBLIC|ACYCLIC|ANNOTATION|INTERFACE;
        sym.erasure_field = type;
        sym.members_field = WriteableScope.create(sym);
        type.typarams_field = List.nil();
        type.allparams_field = List.nil();
        type.supertype_field = annotationType;
        type.interfaces_field = List.nil();
        return type;
    }

    /** Constructor; enters all predefined identifiers and operators
     *  into symbol table.
     */
    protected Symtab(Context context) throws CompletionFailure {
        context.put(symtabKey, this);

        names = Names.instance(context);

        // Create the unknown type
        unknownType = new UnknownType();

        messages = JavacMessages.instance(context);

        MissingInfoHandler missingInfoHandler = MissingInfoHandler.instance(context);

        Target target = Target.instance(context);
        rootPackage = new RootPackageSymbol(names.empty, null,
                                            missingInfoHandler,
                                            target.runtimeUseNestAccess());

        // create the basic builtin symbols
        unnamedModule = new ModuleSymbol(names.empty, null) {
                {
                    directives = List.nil();
                    exports = List.nil();
                    provides = List.nil();
                    uses = List.nil();
                    ModuleSymbol java_base = enterModule(names.java_base);
                    com.sun.tools.javac.code.Directive.RequiresDirective d =
                            new com.sun.tools.javac.code.Directive.RequiresDirective(java_base,
                                    EnumSet.of(com.sun.tools.javac.code.Directive.RequiresFlag.MANDATED));
                    requires = List.of(d);
                }
                @Override
                public String toString() {
                    return messages.getLocalizedString("compiler.misc.unnamed.module");
                }
            };
        addRootPackageFor(unnamedModule);
        unnamedModule.enclosedPackages = unnamedModule.enclosedPackages.prepend(unnamedModule.unnamedPackage);

        errModule = new ModuleSymbol(names.empty, null) {
                {
                    directives = List.nil();
                    exports = List.nil();
                    provides = List.nil();
                    uses = List.nil();
                    ModuleSymbol java_base = enterModule(names.java_base);
                    com.sun.tools.javac.code.Directive.RequiresDirective d =
                            new com.sun.tools.javac.code.Directive.RequiresDirective(java_base,
                                    EnumSet.of(com.sun.tools.javac.code.Directive.RequiresFlag.MANDATED));
                    requires = List.of(d);
                }
            };
        addRootPackageFor(errModule);

        noModule = new ModuleSymbol(names.empty, null) {
            @Override public boolean isNoModule() {
                return true;
            }
        };
        addRootPackageFor(noModule);

        noSymbol = new TypeSymbol(NIL, 0, names.empty, Type.noType, rootPackage) {
            @Override @DefinedBy(Api.LANGUAGE_MODEL)
            public <R, P> R accept(ElementVisitor<R, P> v, P p) {
                return v.visitUnknown(this, p);
            }
        };

        // create the error symbols
        errSymbol = new ClassSymbol(PUBLIC|STATIC|ACYCLIC, names.any, null, rootPackage);
        errType = new ErrorType(errSymbol, Type.noType);

        unknownSymbol = new ClassSymbol(PUBLIC|STATIC|ACYCLIC, names.fromString("<any?>"), null, rootPackage);
        unknownSymbol.members_field = new Scope.ErrorScope(unknownSymbol);
        unknownSymbol.type = unknownType;

        // initialize builtin types
        initType(byteType, "byte", "Byte");
        initType(shortType, "short", "Short");
        initType(charType, "char", "Character");
        initType(intType, "int", "Integer");
        initType(longType, "long", "Long");
        initType(floatType, "float", "Float");
        initType(doubleType, "double", "Double");
        initType(booleanType, "boolean", "Boolean");
        initType(voidType, "void", "Void");
        initType(botType, "<nulltype>");
        initType(errType, errSymbol);
        initType(unknownType, unknownSymbol);

        // the builtin class of all arrays
        arrayClass = new ClassSymbol(PUBLIC|ACYCLIC, names.Array, noSymbol);

        // VGJ
        boundClass = new ClassSymbol(PUBLIC|ACYCLIC, names.Bound, noSymbol);
        boundClass.members_field = new Scope.ErrorScope(boundClass);

        // the builtin class of all methods
        methodClass = new ClassSymbol(PUBLIC|ACYCLIC, names.Method, noSymbol);
        methodClass.members_field = new Scope.ErrorScope(boundClass);

        // Create class to hold all predefined constants and operations.
        predefClass = new ClassSymbol(PUBLIC|ACYCLIC, names.empty, rootPackage);
        WriteableScope scope = WriteableScope.create(predefClass);
        predefClass.members_field = scope;

        // Get the initial completer for Symbols from the ClassFinder
        initialCompleter = ClassFinder.instance(context).getCompleter();
        rootPackage.members_field = WriteableScope.create(rootPackage);

        // Enter symbols for basic types.
        scope.enter(byteType.tsym);
        scope.enter(shortType.tsym);
        scope.enter(charType.tsym);
        scope.enter(intType.tsym);
        scope.enter(longType.tsym);
        scope.enter(floatType.tsym);
        scope.enter(doubleType.tsym);
        scope.enter(booleanType.tsym);
        scope.enter(errType.tsym);

        // Enter symbol for the errSymbol
        scope.enter(errSymbol);

        Source source = Source.instance(context);
        if (Feature.MODULES.allowedInSource(source)) {
            java_base = enterModule(names.java_base);
            //avoid completing java.base during the Symtab initialization
            java_base.completer = Completer.NULL_COMPLETER;
            java_base.visiblePackages = Collections.emptyMap();
        } else {
            java_base = noModule;
        }

        // Get the initial completer for ModuleSymbols from Modules
        moduleCompleter = Modules.instance(context).getCompleter();

        // Enter predefined classes. All are assumed to be in the java.base module.
        objectType = enterClass("java.lang.Object");
        objectMethodsType = enterClass("java.lang.runtime.ObjectMethods");
        objectsType = enterClass("java.util.Objects");
        classType = enterClass("java.lang.Class");
        stringType = enterClass("java.lang.String");
        stringBufferType = enterClass("java.lang.StringBuffer");
        stringBuilderType = enterClass("java.lang.StringBuilder");
        cloneableType = enterClass("java.lang.Cloneable");
        throwableType = enterClass("java.lang.Throwable");
        serializableType = enterClass("java.io.Serializable");
        serializedLambdaType = enterClass("java.lang.invoke.SerializedLambda");
        varHandleType = enterClass("java.lang.invoke.VarHandle");
        methodHandleType = enterClass("java.lang.invoke.MethodHandle");
        methodHandleLookupType = enterClass("java.lang.invoke.MethodHandles$Lookup");
        methodTypeType = enterClass("java.lang.invoke.MethodType");
        errorType = enterClass("java.lang.Error");
        illegalArgumentExceptionType = enterClass("java.lang.IllegalArgumentException");
        interruptedExceptionType = enterClass("java.lang.InterruptedException");
        exceptionType = enterClass("java.lang.Exception");
        runtimeExceptionType = enterClass("java.lang.RuntimeException");
        classNotFoundExceptionType = enterClass("java.lang.ClassNotFoundException");
        noClassDefFoundErrorType = enterClass("java.lang.NoClassDefFoundError");
        noSuchFieldErrorType = enterClass("java.lang.NoSuchFieldError");
        assertionErrorType = enterClass("java.lang.AssertionError");
        incompatibleClassChangeErrorType = enterClass("java.lang.IncompatibleClassChangeError");
        cloneNotSupportedExceptionType = enterClass("java.lang.CloneNotSupportedException");
        annotationType = enterClass("java.lang.annotation.Annotation");
        classLoaderType = enterClass("java.lang.ClassLoader");
        enumSym = enterClass(java_base, names.java_lang_Enum);
        enumFinalFinalize =
            new MethodSymbol(PROTECTED|FINAL|HYPOTHETICAL,
                             names.finalize,
                             new MethodType(List.nil(), voidType,
                                            List.nil(), methodClass),
                             enumSym);
        listType = enterClass("java.util.List");
        collectionsType = enterClass("java.util.Collections");
        comparableType = enterClass("java.lang.Comparable");
        comparatorType = enterClass("java.util.Comparator");
        arraysType = enterClass("java.util.Arrays");
        iterableType = enterClass("java.lang.Iterable");
        iteratorType = enterClass("java.util.Iterator");
        annotationTargetType = enterClass("java.lang.annotation.Target");
        overrideType = enterClass("java.lang.Override");
        retentionType = enterClass("java.lang.annotation.Retention");
        deprecatedType = enterClass("java.lang.Deprecated");
        suppressWarningsType = enterClass("java.lang.SuppressWarnings");
        supplierType = enterClass("java.util.function.Supplier");
        inheritedType = enterClass("java.lang.annotation.Inherited");
        repeatableType = enterClass("java.lang.annotation.Repeatable");
        documentedType = enterClass("java.lang.annotation.Documented");
        elementTypeType = enterClass("java.lang.annotation.ElementType");
        systemType = enterClass("java.lang.System");
        autoCloseableType = enterClass("java.lang.AutoCloseable");
        autoCloseableClose = new MethodSymbol(PUBLIC,
                             names.close,
                             new MethodType(List.nil(), voidType,
                                            List.of(exceptionType), methodClass),
                             autoCloseableType.tsym);
        trustMeType = enterClass("java.lang.SafeVarargs");
        nativeHeaderType = enterClass("java.lang.annotation.Native");
        lambdaMetafactory = enterClass("java.lang.invoke.LambdaMetafactory");
        stringConcatFactory = enterClass("java.lang.invoke.StringConcatFactory");
        functionalInterfaceType = enterClass("java.lang.FunctionalInterface");
        previewFeatureType = enterClass("jdk.internal.javac.PreviewFeature");
        previewFeatureInternalType = enterSyntheticAnnotation("jdk.internal.PreviewFeature+Annotation");
        typeDescriptorType = enterClass("java.lang.invoke.TypeDescriptor");
        recordType = enterClass("java.lang.Record");
        switchBootstrapsType = enterClass("java.lang.runtime.SwitchBootstraps");
        valueBasedType = enterClass("jdk.internal.ValueBased");
        valueBasedInternalType = enterSyntheticAnnotation("jdk.internal.ValueBased+Annotation");

        synthesizeEmptyInterfaceIfMissing(autoCloseableType);
        synthesizeEmptyInterfaceIfMissing(cloneableType);
        synthesizeEmptyInterfaceIfMissing(serializableType);
        synthesizeEmptyInterfaceIfMissing(lambdaMetafactory);
        synthesizeEmptyInterfaceIfMissing(serializedLambdaType);
        synthesizeEmptyInterfaceIfMissing(stringConcatFactory);
        synthesizeBoxTypeIfMissing(doubleType);
        synthesizeBoxTypeIfMissing(floatType);
        synthesizeBoxTypeIfMissing(voidType);

        // Enter a synthetic class that is used to mark internal
        // proprietary classes in ct.sym.  This class does not have a
        // class file.
        proprietaryType = enterSyntheticAnnotation("sun.Proprietary+Annotation");

        // Enter a synthetic class that is used to provide profile info for
        // classes in ct.sym.  This class does not have a class file.
        profileType = enterSyntheticAnnotation("jdk.Profile+Annotation");
        MethodSymbol m = new MethodSymbol(PUBLIC | ABSTRACT, names.value, intType, profileType.tsym);
        profileType.tsym.members().enter(m);

        // Enter a class for arrays.
        // The class implements java.lang.Cloneable and java.io.Serializable.
        // It has a final length field and a clone method.
        ClassType arrayClassType = (ClassType)arrayClass.type;
        arrayClassType.supertype_field = objectType;
        arrayClassType.interfaces_field = List.of(cloneableType, serializableType);
        arrayClass.members_field = WriteableScope.create(arrayClass);
        lengthVar = new VarSymbol(
            PUBLIC | FINAL,
            names.length,
            intType,
            arrayClass);
        arrayClass.members().enter(lengthVar);
        arrayCloneMethod = new MethodSymbol(
            PUBLIC,
            names.clone,
            new MethodType(List.nil(), objectType,
                           List.nil(), methodClass),
            arrayClass);
        arrayClass.members().enter(arrayCloneMethod);

        if (java_base != noModule)
            java_base.completer = moduleCompleter::complete; //bootstrap issues

    }

    /** Define a new class given its name and owner.
     */
    public ClassSymbol defineClass(Name name, Symbol owner) {
        ClassSymbol c = new ClassSymbol(0, name, owner);
        c.completer = initialCompleter;
        return c;
    }

    /** Create a new toplevel or member class symbol with given name
     *  and owner and enter in `classes' unless already there.
     */
    public ClassSymbol enterClass(ModuleSymbol msym, Name name, TypeSymbol owner) {
        Assert.checkNonNull(msym);
        Name flatname = TypeSymbol.formFlatName(name, owner);
        ClassSymbol c = getClass(msym, flatname);
        if (c == null) {
            c = defineClass(name, owner);
            doEnterClass(msym, c);
        } else if ((c.name != name || c.owner != owner) && owner.kind == TYP && c.owner.kind == PCK) {
            // reassign fields of classes that might have been loaded with
            // their flat names.
            c.owner.members().remove(c);
            c.name = name;
            c.owner = owner;
            c.fullname = ClassSymbol.formFullName(name, owner);
        }
        return c;
    }

    public ClassSymbol getClass(ModuleSymbol msym, Name flatName) {
        Assert.checkNonNull(msym, flatName::toString);
        return classes.getOrDefault(flatName, Collections.emptyMap()).get(msym);
    }

    public PackageSymbol lookupPackage(ModuleSymbol msym, Name flatName) {
        return lookupPackage(msym, flatName, false);
    }

    private PackageSymbol lookupPackage(ModuleSymbol msym, Name flatName, boolean onlyExisting) {
        Assert.checkNonNull(msym);

        if (flatName.isEmpty()) {
            //unnamed packages only from the current module - visiblePackages contains *root* package, not unnamed package!
            return msym.unnamedPackage;
        }

        if (msym == noModule) {
            return enterPackage(msym, flatName);
        }

        msym.complete();

        PackageSymbol pack;

        pack = msym.visiblePackages.get(flatName);

        if (pack != null)
            return pack;

        pack = getPackage(msym, flatName);

        if ((pack != null && pack.exists()) || onlyExisting)
            return pack;

        boolean dependsOnUnnamed = msym.requires != null &&
                                   msym.requires.stream()
                                                .map(rd -> rd.module)
                                                .anyMatch(mod -> mod == unnamedModule);

        if (dependsOnUnnamed) {
            //msyms depends on the unnamed module, for which we generally don't know
            //the list of packages it "exports" ahead of time. So try to lookup the package in the
            //current module, and in the unnamed module and see if it exists in one of them
            PackageSymbol unnamedPack = getPackage(unnamedModule, flatName);

            if (unnamedPack != null && unnamedPack.exists()) {
                msym.visiblePackages.put(unnamedPack.fullname, unnamedPack);
                return unnamedPack;
            }

            pack = enterPackage(msym, flatName);
            pack.complete();
            if (pack.exists())
                return pack;

            unnamedPack = enterPackage(unnamedModule, flatName);
            unnamedPack.complete();
            if (unnamedPack.exists()) {
                msym.visiblePackages.put(unnamedPack.fullname, unnamedPack);
                return unnamedPack;
            }

            return pack;
        }

        return enterPackage(msym, flatName);
    }

    private static final Map<ModuleSymbol, ClassSymbol> EMPTY = new HashMap<>();

    public void removeClass(ModuleSymbol msym, Name flatName) {
        classes.getOrDefault(flatName, EMPTY).remove(msym);
    }

    public Iterable<ClassSymbol> getAllClasses() {
        return () -> Iterators.createCompoundIterator(classes.values(), v -> v.values().iterator());
    }

    private void doEnterClass(ModuleSymbol msym, ClassSymbol cs) {
        classes.computeIfAbsent(cs.flatname, n -> new HashMap<>()).put(msym, cs);
    }

    /** Create a new member or toplevel class symbol with given flat name
     *  and enter in `classes' unless already there.
     */
    public ClassSymbol enterClass(ModuleSymbol msym, Name flatname) {
        Assert.checkNonNull(msym);
        PackageSymbol ps = lookupPackage(msym, Convert.packagePart(flatname));
        Assert.checkNonNull(ps);
        Assert.checkNonNull(ps.modle);
        ClassSymbol c = getClass(ps.modle, flatname);
        if (c == null) {
            c = defineClass(Convert.shortName(flatname), ps);
            doEnterClass(ps.modle, c);
            return c;
        } else
            return c;
    }

    /** Check to see if a package exists, given its fully qualified name.
     */
    public boolean packageExists(ModuleSymbol msym, Name fullname) {
        Assert.checkNonNull(msym);
        PackageSymbol pack = lookupPackage(msym, fullname, true);
        return pack != null && pack.exists();
    }

    /** Make a package, given its fully qualified name.
     */
    public PackageSymbol enterPackage(ModuleSymbol currModule, Name fullname) {
        Assert.checkNonNull(currModule);
        PackageSymbol p = getPackage(currModule, fullname);
        if (p == null) {
            Assert.check(!fullname.isEmpty(), () -> "rootPackage missing!; currModule: " + currModule);
            p = new PackageSymbol(
                    Convert.shortName(fullname),
                    enterPackage(currModule, Convert.packagePart(fullname)));
            p.completer = initialCompleter;
            p.modle = currModule;
            doEnterPackage(currModule, p);
        }
        return p;
    }

    private void doEnterPackage(ModuleSymbol msym, PackageSymbol pack) {
        packages.computeIfAbsent(pack.fullname, n -> new HashMap<>()).put(msym, pack);
        msym.enclosedPackages = msym.enclosedPackages.prepend(pack);
    }

    private void addRootPackageFor(ModuleSymbol module) {
        doEnterPackage(module, rootPackage);
        PackageSymbol unnamedPackage = new PackageSymbol(names.empty, rootPackage) {
                @Override
                public String toString() {
                    return messages.getLocalizedString("compiler.misc.unnamed.package");
                }
            };
        unnamedPackage.modle = module;
        //we cannot use a method reference below, as initialCompleter might be null now
        unnamedPackage.completer = s -> initialCompleter.complete(s);
        unnamedPackage.flags_field |= EXISTS;
        module.unnamedPackage = unnamedPackage;
    }

    public PackageSymbol getPackage(ModuleSymbol module, Name fullname) {
        return packages.getOrDefault(fullname, Collections.emptyMap()).get(module);
    }

    public ModuleSymbol enterModule(Name name) {
        ModuleSymbol msym = modules.get(name);
        if (msym == null) {
            msym = ModuleSymbol.create(name, names.module_info);
            addRootPackageFor(msym);
            msym.completer = s -> moduleCompleter.complete(s); //bootstrap issues
            modules.put(name, msym);
        }
        return msym;
    }

    public ModuleSymbol getModule(Name name) {
        return modules.get(name);
    }

    //temporary:
    public ModuleSymbol inferModule(Name packageName) {
        if (packageName.isEmpty())
            return java_base == noModule ? noModule : unnamedModule;//!

        ModuleSymbol msym = null;
        Map<ModuleSymbol,PackageSymbol> map = packages.get(packageName);
        if (map == null)
            return null;
        for (Map.Entry<ModuleSymbol,PackageSymbol> e: map.entrySet()) {
            if (!e.getValue().members().isEmpty()) {
                if (msym == null) {
                    msym = e.getKey();
                } else {
                    return null;
                }
            }
        }
        return msym;
    }

    public List<ModuleSymbol> listPackageModules(Name packageName) {
        if (packageName.isEmpty())
            return List.nil();

        List<ModuleSymbol> result = List.nil();
        Map<ModuleSymbol,PackageSymbol> map = packages.get(packageName);
        if (map != null) {
            for (Map.Entry<ModuleSymbol, PackageSymbol> e: map.entrySet()) {
                if (!e.getValue().members().isEmpty()) {
                    result = result.prepend(e.getKey());
                }
            }
        }
        return result;
    }

    public Collection<ModuleSymbol> getAllModules() {
        return modules.values();
    }

    public Iterable<ClassSymbol> getClassesForName(Name candidate) {
        return classes.getOrDefault(candidate, Collections.emptyMap()).values();
    }

    public Iterable<PackageSymbol> getPackagesForName(Name candidate) {
        return packages.getOrDefault(candidate, Collections.emptyMap()).values();
    }
}
