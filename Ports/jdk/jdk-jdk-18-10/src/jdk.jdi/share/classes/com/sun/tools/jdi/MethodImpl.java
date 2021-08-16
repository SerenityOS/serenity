/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public abstract class MethodImpl extends TypeComponentImpl
                                 implements Method
{
    private JNITypeParser signatureParser;

    abstract int argSlotCount() throws AbsentInformationException;

    abstract List<Location> allLineLocations(SDE.Stratum stratum,
                                             String sourceName)
                            throws AbsentInformationException;

    abstract List<Location> locationsOfLine(SDE.Stratum stratum,
                                            String sourceName,
                                            int lineNumber)
                            throws AbsentInformationException;

    MethodImpl(VirtualMachine vm, ReferenceTypeImpl declaringType,
               long ref, String name, String signature,
               String genericSignature, int modifiers) {
        super(vm, declaringType, ref, name, signature,
              genericSignature, modifiers);
        signatureParser = new JNITypeParser(signature);
    }

    static MethodImpl createMethodImpl(VirtualMachine vm,
                                       ReferenceTypeImpl declaringType,
                                       long ref,
                                       String name,
                                       String signature,
                                       String genericSignature,
                                       int modifiers) {
        if ((modifiers & (VMModifiers.NATIVE | VMModifiers.ABSTRACT)) != 0) {
            return new NonConcreteMethodImpl(vm, declaringType, ref,
                                             name, signature,
                                             genericSignature,
                                             modifiers);
        } else {
            return new ConcreteMethodImpl(vm, declaringType, ref,
                                          name, signature,
                                          genericSignature,
                                          modifiers);
        }
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof MethodImpl)) {
            MethodImpl other = (MethodImpl)obj;
            return (declaringType().equals(other.declaringType())) &&
                   (ref() == other.ref()) &&
                   super.equals(obj);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (int)ref();
    }

    public final List<Location> allLineLocations()
                                throws AbsentInformationException {
        return allLineLocations(vm.getDefaultStratum(), null);
    }

    public List<Location> allLineLocations(String stratumID,
                                           String sourceName)
                          throws AbsentInformationException {
        return allLineLocations(declaringType.stratum(stratumID), sourceName);
    }

    public final List<Location> locationsOfLine(int lineNumber)
                                throws AbsentInformationException {
        return locationsOfLine(vm.getDefaultStratum(),
                               null, lineNumber);
    }

    public List<Location> locationsOfLine(String stratumID,
                                          String sourceName,
                                          int lineNumber)
                          throws AbsentInformationException {
        return locationsOfLine(declaringType.stratum(stratumID),
                               sourceName, lineNumber);
    }

    LineInfo codeIndexToLineInfo(SDE.Stratum stratum,
                                 long codeIndex) {
        if (stratum.isJava()) {
            return new BaseLineInfo(-1, declaringType);
        } else {
            return new StratumLineInfo(stratum.id(), -1, null, null);
        }
    }

    /**
     * @return a text representation of the declared return type
     * of this method.
     */
    public String returnTypeName() {
        return signatureParser.typeName();
    }

    private String returnSignature() {
        return signatureParser.signature();
    }

    public Type returnType() throws ClassNotLoadedException {
        return findType(returnSignature());
    }

    public Type findType(String signature) throws ClassNotLoadedException {
        ReferenceTypeImpl enclosing = (ReferenceTypeImpl)declaringType();
        return enclosing.findType(signature);
    }

    public List<String> argumentTypeNames() {
        return signatureParser.argumentTypeNames();
    }

    public List<String> argumentSignatures() {
        return signatureParser.argumentSignatures();
    }

    Type argumentType(int index) throws ClassNotLoadedException {
        ReferenceTypeImpl enclosing = (ReferenceTypeImpl)declaringType();
        String signature = argumentSignatures().get(index);
        return enclosing.findType(signature);
    }

    public List<Type> argumentTypes() throws ClassNotLoadedException {
        int size = argumentSignatures().size();
        List<Type> types = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Type type = argumentType(i);
            types.add(type);
        }

        return types;
    }

    public int compareTo(Method method) {
        ReferenceTypeImpl declaringType = (ReferenceTypeImpl)declaringType();
        int rc = declaringType.compareTo(method.declaringType());
        if (rc == 0) {
            rc = declaringType.indexOf(this) - declaringType.indexOf(method);
        }
        return rc;
    }

    public boolean isAbstract() {
        return isModifierSet(VMModifiers.ABSTRACT);
    }

    public boolean isDefault() {
        return !isModifierSet(VMModifiers.ABSTRACT) &&
               !isModifierSet(VMModifiers.STATIC) &&
               !isModifierSet(VMModifiers.PRIVATE) &&
               declaringType() instanceof InterfaceType;
    }

    public boolean isSynchronized() {
        return isModifierSet(VMModifiers.SYNCHRONIZED);
    }

    public boolean isNative() {
        return isModifierSet(VMModifiers.NATIVE);
    }

    public boolean isVarArgs() {
        return isModifierSet(VMModifiers.VARARGS);
    }

    public boolean isBridge() {
        return isModifierSet(VMModifiers.BRIDGE);
    }

    public boolean isConstructor() {
        return name().equals("<init>");
    }

    public boolean isStaticInitializer() {
        return name().equals("<clinit>");
    }

    public boolean isObsolete() {
        try {
            return JDWP.Method.IsObsolete.process(vm,
                                    declaringType, ref).isObsolete;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    /*
     * A container class for the return value to allow
     * proper type-checking.
     */
    class ReturnContainer implements ValueContainer {
        ReturnContainer() {
        }
        public Type type() throws ClassNotLoadedException {
            return returnType();
        }
        public String typeName(){
            return returnTypeName();
        }
        public String signature() {
            return returnSignature(); //type().signature();
        }
        public Type findType(String signature) throws ClassNotLoadedException {
            return MethodImpl.this.findType(signature);
        }
    }
    ReturnContainer retValContainer = null;
    ReturnContainer getReturnValueContainer() {
        if (retValContainer == null) {
            retValContainer = new ReturnContainer();
        }
        return retValContainer;
    }

    /*
     * A container class for the argument to allow
     * proper type-checking.
     */
    class ArgumentContainer implements ValueContainer {
        int index;

        ArgumentContainer(int index) {
            this.index = index;
        }
        public Type type() throws ClassNotLoadedException {
            return argumentType(index);
        }
        public String typeName(){
            return argumentTypeNames().get(index);
        }
        public String signature() {
            return argumentSignatures().get(index);
        }
        public Type findType(String signature) throws ClassNotLoadedException {
            return MethodImpl.this.findType(signature);
        }
    }

    /*
     * This is a var args method.  Thus, its last param is an
     * array. If the method has n params, then:
     * 1.  If there are n args and the last is the same type as the type of
     *     the last param, do nothing.  IE, a String[]
     *     can be passed to a String...
     * 2.  If there are >= n arguments and for each arg whose number is >= n,
     *     the arg type is 'compatible' with the component type of
     *     the last param, then do
     *     - create an array of the type of the last param
     *     - put the n, ... args into this array.
     *       We might have to do conversions here.
     *     - put this array into arguments(n)
     *     - delete arguments(n+1), ...
     * NOTE that this might modify the input list.
     */
    void handleVarArgs(List<Value> arguments)
        throws ClassNotLoadedException, InvalidTypeException {
        List<Type> paramTypes = this.argumentTypes();
        ArrayType lastParamType = (ArrayType)paramTypes.get(paramTypes.size() - 1);
        int argCount = arguments.size();
        int paramCount = paramTypes.size();
        if (argCount < paramCount - 1) {
            // Error; will be caught later.
            return;
        }
        if (argCount == paramCount - 1) {
            // It is ok to pass 0 args to the var arg.
            // We have to gen a 0 length array.
            ArrayReference argArray = lastParamType.newInstance(0);
            arguments.add(argArray);
            return;
        }
        Value nthArgValue = arguments.get(paramCount - 1);
        if (nthArgValue == null && argCount == paramCount) {
            // We have one varargs parameter and it is null
            // so we don't have to do anything.
            return;
        }
        // If the first varargs parameter is null, then don't
        // access its type since it can't be an array.
        Type nthArgType = (nthArgValue == null) ? null : nthArgValue.type();
        if (nthArgType instanceof ArrayTypeImpl) {
            if (argCount == paramCount &&
                ((ArrayTypeImpl)nthArgType).isAssignableTo(lastParamType)) {
                /*
                 * This is case 1.  A compatible array is being passed to the
                 * var args array param.  We don't have to do anything.
                 */
                return;
            }
        }

        /*
         * Case 2.  We have to verify that the n, n+1, ... args are compatible
         * with componentType, and do conversions if necessary and create
         * an array of componentType to hold these possibly converted values.
         */
        int count = argCount - paramCount + 1;
        ArrayReference argArray = lastParamType.newInstance(count);

        /*
         * This will copy arguments(paramCount - 1) ... to argArray(0) ...
         * doing whatever conversions are needed!  It will throw an
         * exception if an incompatible arg is encountered
         */
        argArray.setValues(0, arguments, paramCount - 1, count);
        arguments.set(paramCount - 1, argArray);

        /*
         * Remove the excess args
         */
        for (int ii = paramCount; ii < argCount; ii++) {
            arguments.remove(paramCount);
        }
        return;
    }

    /*
     * The output list will be different than the input list.
     */
    List<Value> validateAndPrepareArgumentsForInvoke(List<? extends Value> origArguments)
                         throws ClassNotLoadedException, InvalidTypeException {

        List<Value> arguments = new ArrayList<>(origArguments);
        if (isVarArgs()) {
            handleVarArgs(arguments);
        }

        int argSize = arguments.size();

        JNITypeParser parser = new JNITypeParser(signature());
        List<String> signatures = parser.argumentSignatures();

        if (signatures.size() != argSize) {
            throw new IllegalArgumentException("Invalid argument count: expected " +
                                               signatures.size() + ", received " +
                                               arguments.size());
        }

        for (int i = 0; i < argSize; i++) {
            Value value = arguments.get(i);
            value = ValueImpl.prepareForAssignment(value,
                                                   new ArgumentContainer(i));
            arguments.set(i, value);
        }
        return arguments;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(declaringType().name());
        sb.append(".");
        sb.append(name());
        sb.append("(");
        boolean first = true;
        for (String name : argumentTypeNames()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(name);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }
}
