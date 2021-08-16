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

package com.sun.tools.javac.util;

/**
 * Access to the compiler's name table.  Standard names are defined,
 * as well as methods to create new names.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Names {

    public static final Context.Key<Names> namesKey = new Context.Key<>();

    public static Names instance(Context context) {
        Names instance = context.get(namesKey);
        if (instance == null) {
            instance = new Names(context);
            context.put(namesKey, instance);
        }
        return instance;
    }

    // operators and punctuation
    public final Name asterisk;
    public final Name comma;
    public final Name empty;
    public final Name hyphen;
    public final Name one;
    public final Name slash;

    // keywords
    public final Name _class;
    public final Name _super;
    public final Name _this;
    public final Name var;
    public final Name exports;
    public final Name opens;
    public final Name module;
    public final Name provides;
    public final Name requires;
    public final Name to;
    public final Name transitive;
    public final Name uses;
    public final Name open;
    public final Name with;
    public final Name yield;

    // field and method names
    public final Name _name;
    public final Name addSuppressed;
    public final Name any;
    public final Name append;
    public final Name clinit;
    public final Name clone;
    public final Name close;
    public final Name deserializeLambda;
    public final Name desiredAssertionStatus;
    public final Name equals;
    public final Name error;
    public final Name finalize;
    public final Name forRemoval;
    public final Name reflective;
    public final Name getClass;
    public final Name hasNext;
    public final Name hashCode;
    public final Name init;
    public final Name iterator;
    public final Name length;
    public final Name next;
    public final Name ordinal;
    public final Name provider;
    public final Name serialVersionUID;
    public final Name toString;
    public final Name value;
    public final Name valueOf;
    public final Name values;
    public final Name readResolve;
    public final Name readObject;

    // class names
    public final Name java_io_Serializable;
    public final Name java_lang_Class;
    public final Name java_lang_Cloneable;
    public final Name java_lang_Enum;
    public final Name java_lang_Object;

    // names of builtin classes
    public final Name Array;
    public final Name Bound;
    public final Name Method;

    // package names
    public final Name java;
    public final Name java_lang;

    // module names
    public final Name java_base;

    // attribute names
    public final Name Annotation;
    public final Name AnnotationDefault;
    public final Name BootstrapMethods;
    public final Name Bridge;
    public final Name CharacterRangeTable;
    public final Name Code;
    public final Name CompilationID;
    public final Name ConstantValue;
    public final Name Deprecated;
    public final Name EnclosingMethod;
    public final Name Enum;
    public final Name Exceptions;
    public final Name InnerClasses;
    public final Name LineNumberTable;
    public final Name LocalVariableTable;
    public final Name LocalVariableTypeTable;
    public final Name MethodParameters;
    public final Name Module;
    public final Name ModuleResolution;
    public final Name NestHost;
    public final Name NestMembers;
    public final Name Record;
    public final Name RuntimeInvisibleAnnotations;
    public final Name RuntimeInvisibleParameterAnnotations;
    public final Name RuntimeInvisibleTypeAnnotations;
    public final Name RuntimeVisibleAnnotations;
    public final Name RuntimeVisibleParameterAnnotations;
    public final Name RuntimeVisibleTypeAnnotations;
    public final Name Signature;
    public final Name SourceFile;
    public final Name SourceID;
    public final Name StackMap;
    public final Name StackMapTable;
    public final Name Synthetic;
    public final Name Value;
    public final Name Varargs;
    public final Name PermittedSubclasses;

    // members of java.lang.annotation.ElementType
    public final Name ANNOTATION_TYPE;
    public final Name CONSTRUCTOR;
    public final Name FIELD;
    public final Name LOCAL_VARIABLE;
    public final Name METHOD;
    public final Name MODULE;
    public final Name PACKAGE;
    public final Name PARAMETER;
    public final Name TYPE;
    public final Name TYPE_PARAMETER;
    public final Name TYPE_USE;
    public final Name RECORD_COMPONENT;

    // members of java.lang.annotation.RetentionPolicy
    public final Name CLASS;
    public final Name RUNTIME;
    public final Name SOURCE;

    // other identifiers
    public final Name T;
    public final Name ex;
    public final Name module_info;
    public final Name package_info;
    public final Name requireNonNull;

    // lambda-related
    public final Name lambda;
    public final Name metafactory;
    public final Name altMetafactory;
    public final Name dollarThis;

    // string concat
    public final Name makeConcat;
    public final Name makeConcatWithConstants;

    // record related
    // members of java.lang.runtime.ObjectMethods
    public final Name bootstrap;

    public final Name record;
    public final Name non;

    // serialization members, used by records too
    public final Name serialPersistentFields;
    public final Name writeObject;
    public final Name writeReplace;
    public final Name readObjectNoData;

    // sealed types
    public final Name permits;
    public final Name sealed;

    // pattern switches
    public final Name typeSwitch;
    public final Name enumSwitch;

    public final Name.Table table;

    public Names(Context context) {
        Options options = Options.instance(context);
        table = createTable(options);

        // operators and punctuation
        asterisk = fromString("*");
        comma = fromString(",");
        empty = fromString("");
        hyphen = fromString("-");
        one = fromString("1");
        slash = fromString("/");

        // keywords
        _class = fromString("class");
        _super = fromString("super");
        _this = fromString("this");
        var = fromString("var");
        exports = fromString("exports");
        opens = fromString("opens");
        module = fromString("module");
        provides = fromString("provides");
        requires = fromString("requires");
        to = fromString("to");
        transitive = fromString("transitive");
        uses = fromString("uses");
        open = fromString("open");
        with = fromString("with");
        yield = fromString("yield");

        // field and method names
        _name = fromString("name");
        addSuppressed = fromString("addSuppressed");
        any = fromString("<any>");
        append = fromString("append");
        clinit = fromString("<clinit>");
        clone = fromString("clone");
        close = fromString("close");
        deserializeLambda = fromString("$deserializeLambda$");
        desiredAssertionStatus = fromString("desiredAssertionStatus");
        equals = fromString("equals");
        error = fromString("<error>");
        finalize = fromString("finalize");
        forRemoval = fromString("forRemoval");
        reflective = fromString("reflective");
        getClass = fromString("getClass");
        hasNext = fromString("hasNext");
        hashCode = fromString("hashCode");
        init = fromString("<init>");
        iterator = fromString("iterator");
        length = fromString("length");
        next = fromString("next");
        ordinal = fromString("ordinal");
        provider = fromString("provider");
        serialVersionUID = fromString("serialVersionUID");
        toString = fromString("toString");
        value = fromString("value");
        valueOf = fromString("valueOf");
        values = fromString("values");
        readResolve = fromString("readResolve");
        readObject = fromString("readObject");
        dollarThis = fromString("$this");

        // class names
        java_io_Serializable = fromString("java.io.Serializable");
        java_lang_Class = fromString("java.lang.Class");
        java_lang_Cloneable = fromString("java.lang.Cloneable");
        java_lang_Enum = fromString("java.lang.Enum");
        java_lang_Object = fromString("java.lang.Object");

        // names of builtin classes
        Array = fromString("Array");
        Bound = fromString("Bound");
        Method = fromString("Method");

        // package names
        java = fromString("java");
        java_lang = fromString("java.lang");

        // module names
        java_base = fromString("java.base");

        // attribute names
        Annotation = fromString("Annotation");
        AnnotationDefault = fromString("AnnotationDefault");
        BootstrapMethods = fromString("BootstrapMethods");
        Bridge = fromString("Bridge");
        CharacterRangeTable = fromString("CharacterRangeTable");
        Code = fromString("Code");
        CompilationID = fromString("CompilationID");
        ConstantValue = fromString("ConstantValue");
        Deprecated = fromString("Deprecated");
        EnclosingMethod = fromString("EnclosingMethod");
        Enum = fromString("Enum");
        Exceptions = fromString("Exceptions");
        InnerClasses = fromString("InnerClasses");
        LineNumberTable = fromString("LineNumberTable");
        LocalVariableTable = fromString("LocalVariableTable");
        LocalVariableTypeTable = fromString("LocalVariableTypeTable");
        MethodParameters = fromString("MethodParameters");
        Module = fromString("Module");
        ModuleResolution = fromString("ModuleResolution");
        NestHost = fromString("NestHost");
        NestMembers = fromString("NestMembers");
        Record = fromString("Record");
        RuntimeInvisibleAnnotations = fromString("RuntimeInvisibleAnnotations");
        RuntimeInvisibleParameterAnnotations = fromString("RuntimeInvisibleParameterAnnotations");
        RuntimeInvisibleTypeAnnotations = fromString("RuntimeInvisibleTypeAnnotations");
        RuntimeVisibleAnnotations = fromString("RuntimeVisibleAnnotations");
        RuntimeVisibleParameterAnnotations = fromString("RuntimeVisibleParameterAnnotations");
        RuntimeVisibleTypeAnnotations = fromString("RuntimeVisibleTypeAnnotations");
        Signature = fromString("Signature");
        SourceFile = fromString("SourceFile");
        SourceID = fromString("SourceID");
        StackMap = fromString("StackMap");
        StackMapTable = fromString("StackMapTable");
        Synthetic = fromString("Synthetic");
        Value = fromString("Value");
        Varargs = fromString("Varargs");
        PermittedSubclasses = fromString("PermittedSubclasses");

        // members of java.lang.annotation.ElementType
        ANNOTATION_TYPE = fromString("ANNOTATION_TYPE");
        CONSTRUCTOR = fromString("CONSTRUCTOR");
        FIELD = fromString("FIELD");
        LOCAL_VARIABLE = fromString("LOCAL_VARIABLE");
        METHOD = fromString("METHOD");
        MODULE = fromString("MODULE");
        PACKAGE = fromString("PACKAGE");
        PARAMETER = fromString("PARAMETER");
        TYPE = fromString("TYPE");
        TYPE_PARAMETER = fromString("TYPE_PARAMETER");
        TYPE_USE = fromString("TYPE_USE");
        RECORD_COMPONENT = fromString("RECORD_COMPONENT");

        // members of java.lang.annotation.RetentionPolicy
        CLASS = fromString("CLASS");
        RUNTIME = fromString("RUNTIME");
        SOURCE = fromString("SOURCE");

        // other identifiers
        T = fromString("T");
        ex = fromString("ex");
        module_info = fromString("module-info");
        package_info = fromString("package-info");
        requireNonNull = fromString("requireNonNull");

        //lambda-related
        lambda = fromString("lambda$");
        metafactory = fromString("metafactory");
        altMetafactory = fromString("altMetafactory");

        // string concat
        makeConcat = fromString("makeConcat");
        makeConcatWithConstants = fromString("makeConcatWithConstants");

        bootstrap = fromString("bootstrap");
        record = fromString("record");
        non = fromString("non");

        serialPersistentFields = fromString("serialPersistentFields");
        writeObject = fromString("writeObject");
        writeReplace = fromString("writeReplace");
        readObjectNoData = fromString("readObjectNoData");

        // sealed types
        permits = fromString("permits");
        sealed = fromString("sealed");

        // pattern switches
        typeSwitch = fromString("typeSwitch");
        enumSwitch = fromString("enumSwitch");
    }

    protected Name.Table createTable(Options options) {
        boolean useUnsharedTable = options.isSet("useUnsharedTable");
        if (useUnsharedTable)
            return UnsharedNameTable.create(this);
        else
            return SharedNameTable.create(this);
    }

    public void dispose() {
        table.dispose();
    }

    public Name fromChars(char[] cs, int start, int len) {
        return table.fromChars(cs, start, len);
    }

    public Name fromString(String s) {
        return table.fromString(s);
    }

    public Name fromUtf(byte[] cs) {
        return table.fromUtf(cs);
    }

    public Name fromUtf(byte[] cs, int start, int len) {
        return table.fromUtf(cs, start, len);
    }
}
