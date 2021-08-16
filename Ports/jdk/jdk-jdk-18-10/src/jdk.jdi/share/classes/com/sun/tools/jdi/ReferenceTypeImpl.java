/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InternalException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ModuleReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public abstract class ReferenceTypeImpl extends TypeImpl implements ReferenceType {
    protected long ref;
    private String signature = null;
    private String genericSignature = null;
    private boolean genericSignatureGotten = false;
    private String baseSourceName = null;
    private String baseSourceDir = null;
    private String baseSourcePath = null;
    protected int modifiers = -1;
    private SoftReference<List<Field>> fieldsRef = null;
    private SoftReference<List<Method>> methodsRef = null;
    private SoftReference<SDE> sdeRef = null;

    private boolean isClassLoaderCached = false;
    private ClassLoaderReference classLoader = null;
    private ClassObjectReference classObject = null;
    private ModuleReference module = null;

    private int status = 0;
    private boolean isPrepared = false;

    private boolean versionNumberGotten = false;
    private int majorVersion;
    private int minorVersion;

    private boolean constantPoolInfoGotten = false;
    private int constanPoolCount;
    private SoftReference<byte[]> constantPoolBytesRef = null;

    /* to mark a SourceFile request that returned a genuine JDWP.Error.ABSENT_INFORMATION */
    private static final String ABSENT_BASE_SOURCE_NAME = "**ABSENT_BASE_SOURCE_NAME**";

    /* to mark when no info available */
    static final SDE NO_SDE_INFO_MARK = new SDE();

    // bits set when initialization was attempted (succeeded or failed)
    private static final int INITIALIZED_OR_FAILED =
        JDWP.ClassStatus.INITIALIZED | JDWP.ClassStatus.ERROR;

    protected ReferenceTypeImpl(VirtualMachine aVm, long aRef) {
        super(aVm);
        ref = aRef;
        genericSignatureGotten = false;
    }

    void noticeRedefineClass() {
        //Invalidate information previously fetched and cached.
        //These will be refreshed later on demand.
        baseSourceName = null;
        baseSourcePath = null;
        modifiers = -1;
        fieldsRef = null;
        methodsRef = null;
        sdeRef = null;
        versionNumberGotten = false;
        constantPoolInfoGotten = false;
    }

    Method getMethodMirror(long ref) {
        if (ref == 0) {
            // obsolete method
            return new ObsoleteMethodImpl(vm, this);
        }
        // Fetch all methods for the class, check performance impact
        // Needs no synchronization now, since methods() returns
        // unmodifiable local data
        Iterator<Method> it = methods().iterator();
        while (it.hasNext()) {
            MethodImpl method = (MethodImpl)it.next();
            if (method.ref() == ref) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid method id: " + ref);
    }

    Field getFieldMirror(long ref) {
        // Fetch all fields for the class, check performance impact
        // Needs no synchronization now, since fields() returns
        // unmodifiable local data
        Iterator<Field>it = fields().iterator();
        while (it.hasNext()) {
            FieldImpl field = (FieldImpl)it.next();
            if (field.ref() == ref) {
                return field;
            }
        }
        throw new IllegalArgumentException("Invalid field id: " + ref);
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof ReferenceTypeImpl)) {
            ReferenceTypeImpl other = (ReferenceTypeImpl)obj;
            return (ref() == other.ref()) &&
                (vm.equals(other.virtualMachine()));
        } else {
            return false;
        }
    }

    public int hashCode() {
        return(int)ref();
    }

    public int compareTo(ReferenceType object) {
        /*
         * Note that it is critical that compareTo() == 0
         * implies that equals() == true. Otherwise, TreeSet
         * will collapse classes.
         *
         * (Classes of the same name loaded by different class loaders
         * or in different VMs must not return 0).
         */
        ReferenceTypeImpl other = (ReferenceTypeImpl)object;
        int comp = name().compareTo(other.name());
        if (comp == 0) {
            long rf1 = ref();
            long rf2 = other.ref();
            // optimize for typical case: refs equal and VMs equal
            if (rf1 == rf2) {
                // sequenceNumbers are always positive
                comp = vm.sequenceNumber -
                 ((VirtualMachineImpl)(other.virtualMachine())).sequenceNumber;
            } else {
                comp = (rf1 < rf2)? -1 : 1;
            }
        }
        return comp;
    }

    public String signature() {
        if (signature == null) {
            // Does not need synchronization, since worst-case
            // static info is fetched twice
            if (vm.canGet1_5LanguageFeatures()) {
                /*
                 * we might as well get both the signature and the
                 * generic signature.
                 */
                genericSignature();
            } else {
                try {
                    signature = JDWP.ReferenceType.Signature.
                        process(vm, this).signature;
                } catch (JDWPException exc) {
                    throw exc.toJDIException();
                }
            }
        }
        return signature;
    }

    public String genericSignature() {
        // This gets both the signature and the generic signature
        if (vm.canGet1_5LanguageFeatures() && !genericSignatureGotten) {
            // Does not need synchronization, since worst-case
            // static info is fetched twice
            JDWP.ReferenceType.SignatureWithGeneric result;
            try {
                result = JDWP.ReferenceType.SignatureWithGeneric.
                    process(vm, this);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
            signature = result.signature;
            setGenericSignature(result.genericSignature);
        }
        return genericSignature;
    }

    public ClassLoaderReference classLoader() {
        if (!isClassLoaderCached) {
            // Does not need synchronization, since worst-case
            // static info is fetched twice
            try {
                classLoader = JDWP.ReferenceType.ClassLoader.
                    process(vm, this).classLoader;
                isClassLoaderCached = true;
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
        return classLoader;
    }

    public ModuleReference module() {
        if (module != null) {
            return module;
        }
        // Does not need synchronization, since worst-case
        // static info is fetched twice
        try {
            ModuleReferenceImpl m = JDWP.ReferenceType.Module.
                process(vm, this).module;
            module = vm.getModule(m.ref());
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        return module;
    }

    public boolean isPublic() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.PUBLIC) > 0);
    }

    public boolean isProtected() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.PROTECTED) > 0);
    }

    public boolean isPrivate() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.PRIVATE) > 0);
    }

    public boolean isPackagePrivate() {
        return !isPublic() && !isPrivate() && !isProtected();
    }

    public boolean isAbstract() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.ABSTRACT) > 0);
    }

    public boolean isFinal() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.FINAL) > 0);
    }

    public boolean isStatic() {
        if (modifiers == -1)
            getModifiers();

        return((modifiers & VMModifiers.STATIC) > 0);
    }

    public boolean isPrepared() {
        // This ref type may have been prepared before we were getting
        // events, so get it once.  After that,
        // this status flag is updated through the ClassPrepareEvent,
        // there is no need for the expense of a JDWP query.
        if (status == 0) {
            updateStatus();
        }
        return isPrepared;
    }

    public boolean isVerified() {
        // Once true, it never resets, so we don't need to update
        if ((status & JDWP.ClassStatus.VERIFIED) == 0) {
            updateStatus();
        }
        return (status & JDWP.ClassStatus.VERIFIED) != 0;
    }

    public boolean isInitialized() {
        // Once initialization succeeds or fails, it never resets,
        // so we don't need to update
        if ((status & INITIALIZED_OR_FAILED) == 0) {
            updateStatus();
        }
        return (status & JDWP.ClassStatus.INITIALIZED) != 0;
    }

    public boolean failedToInitialize() {
        // Once initialization succeeds or fails, it never resets,
        // so we don't need to update
        if ((status & INITIALIZED_OR_FAILED) == 0) {
            updateStatus();
        }
        return (status & JDWP.ClassStatus.ERROR) != 0;
    }

    public List<Field> fields() {
        List<Field> fields = (fieldsRef == null) ? null : fieldsRef.get();
        if (fields == null) {
            if (vm.canGet1_5LanguageFeatures()) {
                JDWP.ReferenceType.FieldsWithGeneric.FieldInfo[] jdwpFields;
                try {
                    jdwpFields = JDWP.ReferenceType.FieldsWithGeneric.
                        process(vm, this).declared;
                } catch (JDWPException exc) {
                    throw exc.toJDIException();
                }
                fields = new ArrayList<>(jdwpFields.length);
                for (int i=0; i<jdwpFields.length; i++) {
                    JDWP.ReferenceType.FieldsWithGeneric.FieldInfo fi
                        = jdwpFields[i];

                    Field field = new FieldImpl(vm, this, fi.fieldID,
                                                fi.name, fi.signature,
                                                fi.genericSignature,
                                                fi.modBits);
                    fields.add(field);
                }
            } else {
                JDWP.ReferenceType.Fields.FieldInfo[] jdwpFields;
                try {
                    jdwpFields = JDWP.ReferenceType.Fields.
                        process(vm, this).declared;
                } catch (JDWPException exc) {
                    throw exc.toJDIException();
                }
                fields = new ArrayList<>(jdwpFields.length);
                for (int i=0; i<jdwpFields.length; i++) {
                    JDWP.ReferenceType.Fields.FieldInfo fi = jdwpFields[i];

                    Field field = new FieldImpl(vm, this, fi.fieldID,
                                            fi.name, fi.signature,
                                            null,
                                            fi.modBits);
                    fields.add(field);
                }
            }

            fields = Collections.unmodifiableList(fields);
            fieldsRef = new SoftReference<List<Field>>(fields);
        }
        return fields;
    }

    abstract List<? extends ReferenceType> inheritedTypes();

    void addVisibleFields(List<Field> visibleList, Map<String, Field> visibleTable, List<String> ambiguousNames) {
        for (Field field : visibleFields()) {
            String name = field.name();
            if (!ambiguousNames.contains(name)) {
                Field duplicate = visibleTable.get(name);
                if (duplicate == null) {
                    visibleList.add(field);
                    visibleTable.put(name, field);
                } else if (!field.equals(duplicate)) {
                    ambiguousNames.add(name);
                    visibleTable.remove(name);
                    visibleList.remove(duplicate);
                } else {
                    // identical field from two branches; do nothing
                }
            }
        }
    }

    public List<Field> visibleFields() {
        /*
         * Maintain two different collections of visible fields. The
         * list maintains a reasonable order for return. The
         * hash map provides an efficient way to lookup visible fields
         * by name, important for finding hidden or ambiguous fields.
         */
        List<Field> visibleList = new ArrayList<>();
        Map<String, Field>  visibleTable = new HashMap<String, Field>();

        /* Track fields removed from above collection due to ambiguity */
        List<String> ambiguousNames = new ArrayList<String>();

        /* Add inherited, visible fields */
        List<? extends ReferenceType> types = inheritedTypes();
        Iterator<? extends ReferenceType> iter = types.iterator();
        while (iter.hasNext()) {
            /*
             * TO DO: Be defensive and check for cyclic interface inheritance
             */
            ReferenceTypeImpl type = (ReferenceTypeImpl)iter.next();
            type.addVisibleFields(visibleList, visibleTable, ambiguousNames);
        }

        /*
         * Insert fields from this type, removing any inherited fields they
         * hide.
         */
        List<Field> retList = new ArrayList<>(fields());
        for (Field field : retList) {
            Field hidden = visibleTable.get(field.name());
            if (hidden != null) {
                visibleList.remove(hidden);
            }
        }
        retList.addAll(visibleList);
        return retList;
    }

    void addAllFields(List<Field> fieldList, Set<ReferenceType> typeSet) {
        /* Continue the recursion only if this type is new */
        if (!typeSet.contains(this)) {
            typeSet.add(this);

            /* Add local fields */
            fieldList.addAll(fields());

            /* Add inherited fields */
            List<? extends ReferenceType> types = inheritedTypes();
            Iterator<? extends ReferenceType> iter = types.iterator();
            while (iter.hasNext()) {
                ReferenceTypeImpl type = (ReferenceTypeImpl)iter.next();
                type.addAllFields(fieldList, typeSet);
            }
        }
    }
    public List<Field> allFields() {
        List<Field> fieldList = new ArrayList<>();
        Set<ReferenceType> typeSet = new HashSet<ReferenceType>();
        addAllFields(fieldList, typeSet);
        return fieldList;
    }

    public Field fieldByName(String fieldName) {
        List<Field> searchList = visibleFields();

        for (int i = 0; i < searchList.size(); i++) {
            Field f = searchList.get(i);

            if (f.name().equals(fieldName)) {
                return f;
            }
        }
        //throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + name());
        return null;
    }

    public List<Method> methods() {
        List<Method> methods = (methodsRef == null) ? null : methodsRef.get();
        if (methods == null) {
            if (!vm.canGet1_5LanguageFeatures()) {
                methods = methods1_4();
            } else {
                JDWP.ReferenceType.MethodsWithGeneric.MethodInfo[] declared;
                try {
                    declared = JDWP.ReferenceType.MethodsWithGeneric.
                        process(vm, this).declared;
                } catch (JDWPException exc) {
                    throw exc.toJDIException();
                }
                methods = new ArrayList<>(declared.length);
                for (int i = 0; i < declared.length; i++) {
                    JDWP.ReferenceType.MethodsWithGeneric.MethodInfo
                        mi = declared[i];

                    Method method = MethodImpl.createMethodImpl(vm, this,
                                                         mi.methodID,
                                                         mi.name, mi.signature,
                                                         mi.genericSignature,
                                                         mi.modBits);
                    methods.add(method);
                }
            }
            methods = Collections.unmodifiableList(methods);
            methodsRef = new SoftReference<List<Method>>(methods);
        }
        return methods;
    }

    private List<Method> methods1_4() {
        List<Method> methods;
        JDWP.ReferenceType.Methods.MethodInfo[] declared;
        try {
            declared = JDWP.ReferenceType.Methods.
                process(vm, this).declared;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        methods = new ArrayList<Method>(declared.length);
        for (int i=0; i<declared.length; i++) {
            JDWP.ReferenceType.Methods.MethodInfo mi = declared[i];

            Method method = MethodImpl.createMethodImpl(vm, this,
                                                        mi.methodID,
                                                        mi.name, mi.signature,
                                                        null,
                                                        mi.modBits);
            methods.add(method);
        }
        return methods;
    }

    /*
     * Utility method used by subclasses to build lists of visible
     * methods.
     */
    void addToMethodMap(Map<String, Method> methodMap, List<Method> methodList) {
        for (Method method : methodList)
            methodMap.put(method.name().concat(method.signature()), method);
        }

    abstract void addVisibleMethods(Map<String, Method> methodMap, Set<InterfaceType> seenInterfaces);

    public List<Method> visibleMethods() {
        /*
         * Build a collection of all visible methods. The hash
         * map allows us to do this efficiently by keying on the
         * concatenation of name and signature.
         */
        Map<String, Method> map = new HashMap<String, Method>();
        addVisibleMethods(map, new HashSet<InterfaceType>());

        /*
         * ... but the hash map destroys order. Methods should be
         * returned in a sensible order, as they are in allMethods().
         * So, start over with allMethods() and use the hash map
         * to filter that ordered collection.
         */
        List<Method> list = allMethods();
        list.retainAll(new HashSet<Method>(map.values()));
        return list;
    }

    abstract public List<Method> allMethods();

    public List<Method> methodsByName(String name) {
        List<Method> methods = visibleMethods();
        ArrayList<Method> retList = new ArrayList<Method>(methods.size());
        for (Method candidate : methods) {
            if (candidate.name().equals(name)) {
                retList.add(candidate);
            }
        }
        retList.trimToSize();
        return retList;
    }

    public List<Method> methodsByName(String name, String signature) {
        List<Method> methods = visibleMethods();
        ArrayList<Method> retList = new ArrayList<Method>(methods.size());
        for (Method candidate : methods) {
            if (candidate.name().equals(name) &&
                candidate.signature().equals(signature)) {
                retList.add(candidate);
            }
        }
        retList.trimToSize();
        return retList;
    }

    List<InterfaceType> getInterfaces() {
        InterfaceTypeImpl[] intfs;
        try {
            intfs = JDWP.ReferenceType.Interfaces.
                                         process(vm, this).interfaces;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        return Arrays.asList((InterfaceType[])intfs);
    }

    public List<ReferenceType> nestedTypes() {
        List<ReferenceType> nested = new ArrayList<ReferenceType>();
        String outername = name();
        int outerlen = outername.length();
        vm.forEachClass(refType -> {
            String name = refType.name();
            int len = name.length();
            /* The separator is historically '$' but could also be '#' */
            if ( len > outerlen && name.startsWith(outername) ) {
                char c = name.charAt(outerlen);
                if ( c == '$' || c == '#' ) {
                    nested.add(refType);
                }
            }
        });
        return nested;
    }

    public Value getValue(Field sig) {
        List<Field> list = new ArrayList<Field>(1);
        list.add(sig);
        Map<Field, Value> map = getValues(list);
        return map.get(sig);
    }


    void validateFieldAccess(Field field) {
        /*
         * Field must be in this object's class, a superclass, or
         * implemented interface
         */
        ReferenceTypeImpl declType = (ReferenceTypeImpl)field.declaringType();
        if (!declType.isAssignableFrom(this)) {
            throw new IllegalArgumentException("Invalid field");
        }
    }

    void validateFieldSet(Field field) {
        validateFieldAccess(field);
        if (field.isFinal()) {
            throw new IllegalArgumentException("Cannot set value of final field");
        }
    }

    /**
     * Returns a map of field values
     */
    public Map<Field,Value> getValues(List<? extends Field> theFields) {
        validateMirrors(theFields);

        int size = theFields.size();
        JDWP.ReferenceType.GetValues.Field[] queryFields =
                         new JDWP.ReferenceType.GetValues.Field[size];

        for (int i=0; i<size; i++) {
            FieldImpl field = (FieldImpl)theFields.get(i);

            validateFieldAccess(field);

            // Do more validation specific to ReferenceType field getting
            if (!field.isStatic()) {
                throw new IllegalArgumentException(
                     "Attempt to use non-static field with ReferenceType");
            }
            queryFields[i] = new JDWP.ReferenceType.GetValues.Field(
                                         field.ref());
        }

        Map<Field, Value> map = new HashMap<Field, Value>(size);

        ValueImpl[] values;
        try {
            values = JDWP.ReferenceType.GetValues.
                                     process(vm, this, queryFields).values;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }

        if (size != values.length) {
            throw new InternalException(
                         "Wrong number of values returned from target VM");
        }
        for (int i=0; i<size; i++) {
            FieldImpl field = (FieldImpl)theFields.get(i);
            map.put(field, values[i]);
        }

        return map;
    }

    public ClassObjectReference classObject() {
        if (classObject == null) {
            // Are classObjects unique for an Object, or
            // created each time? Is this spec'ed?
            synchronized(this) {
                if (classObject == null) {
                    try {
                        classObject = JDWP.ReferenceType.ClassObject.
                            process(vm, this).classObject;
                    } catch (JDWPException exc) {
                        throw exc.toJDIException();
                    }
                }
            }
        }
        return classObject;
    }

    SDE.Stratum stratum(String stratumID) {
        SDE sde = sourceDebugExtensionInfo();
        if (!sde.isValid()) {
            sde = NO_SDE_INFO_MARK;
        }
        return sde.stratum(stratumID);
    }

    public String sourceName() throws AbsentInformationException {
        return sourceNames(vm.getDefaultStratum()).get(0);
    }

    public List<String> sourceNames(String stratumID)
                                throws AbsentInformationException {
        SDE.Stratum stratum = stratum(stratumID);
        if (stratum.isJava()) {
            List<String> result = new ArrayList<String>(1);
            result.add(baseSourceName());
            return result;
        }
        return stratum.sourceNames(this);
    }

    public List<String> sourcePaths(String stratumID)
                                throws AbsentInformationException {
        SDE.Stratum stratum = stratum(stratumID);
        if (stratum.isJava()) {
            List<String> result = new ArrayList<String>(1);
            result.add(baseSourceDir() + baseSourceName());
            return result;
        }
        return stratum.sourcePaths(this);
    }

    String baseSourceName() throws AbsentInformationException {
        String bsn = baseSourceName;
        if (bsn == null) {
            // Does not need synchronization, since worst-case
            // static info is fetched twice
            try {
                bsn = JDWP.ReferenceType.SourceFile.
                    process(vm, this).sourceFile;
            } catch (JDWPException exc) {
                if (exc.errorCode() == JDWP.Error.ABSENT_INFORMATION) {
                    bsn = ABSENT_BASE_SOURCE_NAME;
                } else {
                    throw exc.toJDIException();
                }
            }
            baseSourceName = bsn;
        }
        if (bsn == ABSENT_BASE_SOURCE_NAME) {
            throw new AbsentInformationException();
        }
        return bsn;
    }

    String baseSourcePath() throws AbsentInformationException {
        String bsp = baseSourcePath;
        if (bsp == null) {
            bsp = baseSourceDir() + baseSourceName();
            baseSourcePath = bsp;
        }
        return bsp;
    }

    String baseSourceDir() {
        if (baseSourceDir == null) {
            String typeName = name();
            StringBuilder sb = new StringBuilder(typeName.length() + 10);
            int index = 0;
            int nextIndex;

            while ((nextIndex = typeName.indexOf('.', index)) > 0) {
                sb.append(typeName.substring(index, nextIndex));
                sb.append(java.io.File.separatorChar);
                index = nextIndex + 1;
            }
            baseSourceDir = sb.toString();
        }
        return baseSourceDir;
    }

    public String sourceDebugExtension()
                           throws AbsentInformationException {
        if (!vm.canGetSourceDebugExtension()) {
            throw new UnsupportedOperationException();
        }
        SDE sde = sourceDebugExtensionInfo();
        if (sde == NO_SDE_INFO_MARK) {
            throw new AbsentInformationException();
        }
        return sde.sourceDebugExtension;
    }

    private SDE sourceDebugExtensionInfo() {
        if (!vm.canGetSourceDebugExtension()) {
            return NO_SDE_INFO_MARK;
        }
        SDE sde = (sdeRef == null) ?  null : sdeRef.get();
        if (sde == null) {
            String extension = null;
            try {
                extension = JDWP.ReferenceType.SourceDebugExtension.
                    process(vm, this).extension;
            } catch (JDWPException exc) {
                if (exc.errorCode() != JDWP.Error.ABSENT_INFORMATION) {
                    sdeRef = new SoftReference<SDE>(NO_SDE_INFO_MARK);
                    throw exc.toJDIException();
                }
            }
            if (extension == null) {
                sde = NO_SDE_INFO_MARK;
            } else {
                sde = new SDE(extension);
            }
            sdeRef = new SoftReference<SDE>(sde);
        }
        return sde;
    }

    public List<String> availableStrata() {
        SDE sde = sourceDebugExtensionInfo();
        if (sde.isValid()) {
            return sde.availableStrata();
        } else {
            List<String> strata = new ArrayList<String>();
            strata.add(SDE.BASE_STRATUM_NAME);
            return strata;
        }
    }

    /**
     * Always returns non-null stratumID
     */
    public String defaultStratum() {
        SDE sdei = sourceDebugExtensionInfo();
        if (sdei.isValid()) {
            return sdei.defaultStratumId;
        } else {
            return SDE.BASE_STRATUM_NAME;
        }
    }

    public int modifiers() {
        if (modifiers == -1)
            getModifiers();

        return modifiers;
    }

    public List<Location> allLineLocations()
                            throws AbsentInformationException {
        return allLineLocations(vm.getDefaultStratum(), null);
    }

    public List<Location> allLineLocations(String stratumID, String sourceName)
                            throws AbsentInformationException {
        boolean someAbsent = false; // A method that should have info, didn't
        SDE.Stratum stratum = stratum(stratumID);
        List<Location> list = new ArrayList<Location>();  // location list

        for (Iterator<Method> iter = methods().iterator(); iter.hasNext(); ) {
            MethodImpl method = (MethodImpl)iter.next();
            try {
                list.addAll(
                   method.allLineLocations(stratum, sourceName));
            } catch(AbsentInformationException exc) {
                someAbsent = true;
            }
        }

        // If we retrieved no line info, and at least one of the methods
        // should have had some (as determined by an
        // AbsentInformationException being thrown) then we rethrow
        // the AbsentInformationException.
        if (someAbsent && list.size() == 0) {
            throw new AbsentInformationException();
        }
        return list;
    }

    public List<Location> locationsOfLine(int lineNumber)
                           throws AbsentInformationException {
        return locationsOfLine(vm.getDefaultStratum(),
                               null,
                               lineNumber);
    }

    public List<Location> locationsOfLine(String stratumID,
                                String sourceName,
                                int lineNumber)
                           throws AbsentInformationException {
        // A method that should have info, didn't
        boolean someAbsent = false;
        // A method that should have info, did
        boolean somePresent = false;
        List<Method> methods = methods();
        SDE.Stratum stratum = stratum(stratumID);

        List<Location> list = new ArrayList<Location>();

        Iterator<Method> iter = methods.iterator();
        while(iter.hasNext()) {
            MethodImpl method = (MethodImpl)iter.next();
            // eliminate native and abstract to eliminate
            // false positives
            if (!method.isAbstract() &&
                !method.isNative()) {
                try {
                    list.addAll(
                       method.locationsOfLine(stratum,
                                              sourceName,
                                              lineNumber));
                    somePresent = true;
                } catch(AbsentInformationException exc) {
                    someAbsent = true;
                }
            }
        }
        if (someAbsent && !somePresent) {
            throw new AbsentInformationException();
        }
        return list;
    }

    public List<ObjectReference> instances(long maxInstances) {
        if (!vm.canGetInstanceInfo()) {
            throw new UnsupportedOperationException(
                "target does not support getting instances");
        }

        if (maxInstances < 0) {
            throw new IllegalArgumentException("maxInstances is less than zero: "
                                              + maxInstances);
        }
        int intMax = (maxInstances > Integer.MAX_VALUE)?
            Integer.MAX_VALUE: (int)maxInstances;
        // JDWP can't currently handle more than this (in mustang)

        try {
            return Arrays.asList(
                (ObjectReference[])JDWP.ReferenceType.Instances.
                        process(vm, this, intMax).instances);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    private void getClassFileVersion() {
        if (!vm.canGetClassFileVersion()) {
            throw new UnsupportedOperationException();
        }
        JDWP.ReferenceType.ClassFileVersion classFileVersion;
        if (versionNumberGotten) {
            return;
        } else {
            try {
                classFileVersion = JDWP.ReferenceType.ClassFileVersion.process(vm, this);
            } catch (JDWPException exc) {
                if (exc.errorCode() == JDWP.Error.ABSENT_INFORMATION) {
                    majorVersion = 0;
                    minorVersion = 0;
                    versionNumberGotten = true;
                    return;
                } else {
                    throw exc.toJDIException();
                }
            }
            majorVersion = classFileVersion.majorVersion;
            minorVersion = classFileVersion.minorVersion;
            versionNumberGotten = true;
        }
    }

    public int majorVersion() {
        try {
            getClassFileVersion();
        } catch (RuntimeException exc) {
            throw exc;
        }
        return majorVersion;
    }

    public int minorVersion() {
        try {
            getClassFileVersion();
        } catch (RuntimeException exc) {
            throw exc;
        }
        return minorVersion;
    }

    private byte[] getConstantPoolInfo() {
        JDWP.ReferenceType.ConstantPool jdwpCPool;
        if (!vm.canGetConstantPool()) {
            throw new UnsupportedOperationException();
        }
        if (constantPoolInfoGotten) {
            if (constantPoolBytesRef == null) {
                return null;
            }
            byte[] cpbytes = constantPoolBytesRef.get();
            if (cpbytes != null) {
                return cpbytes;
            }
        }

        try {
            jdwpCPool = JDWP.ReferenceType.ConstantPool.process(vm, this);
        } catch (JDWPException exc) {
            if (exc.errorCode() == JDWP.Error.ABSENT_INFORMATION) {
                constanPoolCount = 0;
                constantPoolBytesRef = null;
                constantPoolInfoGotten = true;
                return null;
            } else {
                throw exc.toJDIException();
            }
        }
        byte[] cpbytes;
        constanPoolCount = jdwpCPool.count;
        cpbytes = jdwpCPool.bytes;
        constantPoolBytesRef = new SoftReference<byte[]>(cpbytes);
        constantPoolInfoGotten = true;
        return cpbytes;
    }

    public int constantPoolCount() {
        try {
            getConstantPoolInfo();
        } catch (RuntimeException exc) {
            throw exc;
        }
        return constanPoolCount;
    }

    public byte[] constantPool() {
        byte[] cpbytes;
        try {
            cpbytes = getConstantPoolInfo();
        } catch (RuntimeException exc) {
            throw exc;
        }
        if (cpbytes != null) {
            /*
             * Arrays are always modifiable, so it is a little unsafe
             * to return the cached bytecodes directly; instead, we
             * make a clone at the cost of using more memory.
             */
            return cpbytes.clone();
        } else {
            return null;
        }
    }

    // Does not need synchronization, since worst-case
    // static info is fetched twice
    void getModifiers() {
        if (modifiers != -1) {
            return;
        }
        try {
            modifiers = JDWP.ReferenceType.Modifiers.
                                  process(vm, this).modBits;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    void decodeStatus(int status) {
        this.status = status;
        if ((status & JDWP.ClassStatus.PREPARED) != 0) {
            isPrepared = true;
        }
    }

    void updateStatus() {
        try {
            decodeStatus(JDWP.ReferenceType.Status.process(vm, this).status);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    void markPrepared() {
        isPrepared = true;
    }

    long ref() {
        return ref;
    }

    int indexOf(Method method) {
        // Make sure they're all here - the obsolete method
        // won't be found and so will have index -1
        return methods().indexOf(method);
    }

    int indexOf(Field field) {
        // Make sure they're all here
        return fields().indexOf(field);
    }

    /*
     * Return true if an instance of this type
     * can be assigned to a variable of the given type
     */
    abstract boolean isAssignableTo(ReferenceType type);

    boolean isAssignableFrom(ReferenceType type) {
        return ((ReferenceTypeImpl)type).isAssignableTo(this);
    }

    boolean isAssignableFrom(ObjectReference object) {
        return object == null ||
               isAssignableFrom(object.referenceType());
    }

    void setStatus(int status) {
        decodeStatus(status);
    }

    void setSignature(String signature) {
        this.signature = signature;
    }

    void setGenericSignature(String signature) {
        if (signature != null && signature.length() == 0) {
            this.genericSignature = null;
        } else{
            this.genericSignature = signature;
        }
        this.genericSignatureGotten = true;
    }

    private static boolean isOneDimensionalPrimitiveArray(String signature) {
        JNITypeParser sig = new JNITypeParser(signature);
        if (sig.isArray()) {
            JNITypeParser componentSig = new JNITypeParser(sig.componentSignature());
            return componentSig.isPrimitive();
        }
        return false;
    }

    Type findType(String signature) throws ClassNotLoadedException {
        Type type;
        JNITypeParser sig = new JNITypeParser(signature);
        if (sig.isVoid()) {
            type = vm.theVoidType();
        } else if (sig.isPrimitive()) {
            type = vm.primitiveTypeMirror(sig.jdwpTag());
        } else {
            // Must be a reference type.
            ClassLoaderReferenceImpl loader =
                    (ClassLoaderReferenceImpl) classLoader();
            if ((loader == null) ||
                    (isOneDimensionalPrimitiveArray(signature)) //Work around 4450091
            ) {
                // Caller wants type of boot class field
                type = vm.findBootType(signature);
            } else {
                // Caller wants type of non-boot class field
                type = loader.findType(signature);
            }
        }
        return type;
    }

    String loaderString() {
        if (classLoader() != null) {
            return "loaded by " + classLoader().toString();
        } else {
            return "no class loader";
        }
    }

}
