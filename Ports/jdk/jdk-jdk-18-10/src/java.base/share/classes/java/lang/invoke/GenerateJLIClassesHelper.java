/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import sun.invoke.util.Wrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.MethodHandleStatics.CLASSFILE_VERSION;
import static java.lang.invoke.MethodTypeForm.*;
import static java.lang.invoke.LambdaForm.Kind.*;

/**
 * Helper class to assist the GenerateJLIClassesPlugin to get access to
 * generate classes ahead of time.
 */
class GenerateJLIClassesHelper {
    // Map from DirectMethodHandle method type name to index to LambdForms
    static final Map<String, Integer> DMH_METHOD_TYPE_MAP =
            Map.of(
                    DIRECT_INVOKE_VIRTUAL.methodName,     LF_INVVIRTUAL,
                    DIRECT_INVOKE_STATIC.methodName,      LF_INVSTATIC,
                    DIRECT_INVOKE_SPECIAL.methodName,     LF_INVSPECIAL,
                    DIRECT_NEW_INVOKE_SPECIAL.methodName, LF_NEWINVSPECIAL,
                    DIRECT_INVOKE_INTERFACE.methodName,   LF_INVINTERFACE,
                    DIRECT_INVOKE_STATIC_INIT.methodName, LF_INVSTATIC_INIT,
                    DIRECT_INVOKE_SPECIAL_IFC.methodName, LF_INVSPECIAL_IFC
            );

    static final String DIRECT_HOLDER = "java/lang/invoke/DirectMethodHandle$Holder";
    static final String DELEGATING_HOLDER = "java/lang/invoke/DelegatingMethodHandle$Holder";
    static final String BASIC_FORMS_HOLDER = "java/lang/invoke/LambdaForm$Holder";
    static final String INVOKERS_HOLDER = "java/lang/invoke/Invokers$Holder";
    static final String INVOKERS_HOLDER_CLASS_NAME = INVOKERS_HOLDER.replace('/', '.');
    static final String BMH_SPECIES_PREFIX = "java.lang.invoke.BoundMethodHandle$Species_";

    static class HolderClassBuilder {


        private final TreeSet<String> speciesTypes = new TreeSet<>();
        private final TreeSet<String> invokerTypes = new TreeSet<>();
        private final TreeSet<String> callSiteTypes = new TreeSet<>();
        private final Map<String, Set<String>> dmhMethods = new TreeMap<>();

        HolderClassBuilder addSpeciesType(String type) {
            speciesTypes.add(expandSignature(type));
            return this;
        }

        HolderClassBuilder addInvokerType(String methodType) {
            validateMethodType(methodType);
            invokerTypes.add(methodType);
            return this;
        }

        HolderClassBuilder addCallSiteType(String csType) {
            validateMethodType(csType);
            callSiteTypes.add(csType);
            return this;
        }

        Map<String, byte[]> build() {
            int count = 0;
            for (Set<String> entry : dmhMethods.values()) {
                count += entry.size();
            }
            MethodType[] directMethodTypes = new MethodType[count];
            int[] dmhTypes = new int[count];
            int index = 0;
            for (Map.Entry<String, Set<String>> entry : dmhMethods.entrySet()) {
                String dmhType = entry.getKey();
                for (String type : entry.getValue()) {
                    // The DMH type to actually ask for is retrieved by removing
                    // the first argument, which needs to be of Object.class
                    MethodType mt = asMethodType(type);
                    if (mt.parameterCount() < 1 ||
                            mt.parameterType(0) != Object.class) {
                        throw new RuntimeException(
                                "DMH type parameter must start with L: " + dmhType + " " + type);
                    }

                    // Adapt the method type of the LF to retrieve
                    directMethodTypes[index] = mt.dropParameterTypes(0, 1);

                    // invokeVirtual and invokeInterface must have a leading Object
                    // parameter, i.e., the receiver
                    dmhTypes[index] = DMH_METHOD_TYPE_MAP.get(dmhType);
                    if (dmhTypes[index] == LF_INVINTERFACE || dmhTypes[index] == LF_INVVIRTUAL) {
                        if (mt.parameterCount() < 2 ||
                                mt.parameterType(1) != Object.class) {
                            throw new RuntimeException(
                                    "DMH type parameter must start with LL: " + dmhType + " " + type);
                        }
                    }
                    index++;
                }
            }

            // The invoker type to ask for is retrieved by removing the first
            // and the last argument, which needs to be of Object.class
            MethodType[] invokerMethodTypes = new MethodType[invokerTypes.size()];
            index = 0;
            for (String invokerType : invokerTypes) {
                MethodType mt = asMethodType(invokerType);
                final int lastParam = mt.parameterCount() - 1;
                if (mt.parameterCount() < 2 ||
                        mt.parameterType(0) != Object.class ||
                        mt.parameterType(lastParam) != Object.class) {
                    throw new RuntimeException(
                            "Invoker type parameter must start and end with Object: " + invokerType);
                }
                mt = mt.dropParameterTypes(lastParam, lastParam + 1);
                invokerMethodTypes[index] = mt.dropParameterTypes(0, 1);
                index++;
            }

            // The callSite type to ask for is retrieved by removing the last
            // argument, which needs to be of Object.class
            MethodType[] callSiteMethodTypes = new MethodType[callSiteTypes.size()];
            index = 0;
            for (String callSiteType : callSiteTypes) {
                MethodType mt = asMethodType(callSiteType);
                final int lastParam = mt.parameterCount() - 1;
                if (mt.parameterCount() < 1 ||
                        mt.parameterType(lastParam) != Object.class) {
                    throw new RuntimeException(
                            "CallSite type parameter must end with Object: " + callSiteType);
                }
                callSiteMethodTypes[index] = mt.dropParameterTypes(lastParam, lastParam + 1);
                index++;
            }

            Map<String, byte[]> result = new TreeMap<>();
            result.put(DIRECT_HOLDER,
                       generateDirectMethodHandleHolderClassBytes(
                            DIRECT_HOLDER, directMethodTypes, dmhTypes));
            result.put(DELEGATING_HOLDER,
                       generateDelegatingMethodHandleHolderClassBytes(
                            DELEGATING_HOLDER, directMethodTypes));
            result.put(INVOKERS_HOLDER,
                       generateInvokersHolderClassBytes(INVOKERS_HOLDER,
                            invokerMethodTypes, callSiteMethodTypes));
            result.put(BASIC_FORMS_HOLDER,
                       generateBasicFormsClassBytes(BASIC_FORMS_HOLDER));

            speciesTypes.forEach(types -> {
                Map.Entry<String, byte[]> entry = generateConcreteBMHClassBytes(types);
                result.put(entry.getKey(), entry.getValue());
            });

            // clear builder
            speciesTypes.clear();
            invokerTypes.clear();
            callSiteTypes.clear();
            dmhMethods.clear();

            return result;
        }

        private static MethodType asMethodType(String basicSignatureString) {
            String[] parts = basicSignatureString.split("_");
            assert (parts.length == 2);
            assert (parts[1].length() == 1);
            String parameters = expandSignature(parts[0]);
            Class<?> rtype = simpleType(parts[1].charAt(0));
            if (parameters.isEmpty()) {
                return MethodType.methodType(rtype);
            } else {
                Class<?>[] ptypes = new Class<?>[parameters.length()];
                for (int i = 0; i < ptypes.length; i++) {
                    ptypes[i] = simpleType(parameters.charAt(i));
                }
                return MethodType.methodType(rtype, ptypes);
            }
        }

        private void addDMHMethodType(String dmh, String methodType) {
            validateMethodType(methodType);
            Set<String> methodTypes = dmhMethods.get(dmh);
            if (methodTypes == null) {
                methodTypes = new TreeSet<>();
                dmhMethods.put(dmh, methodTypes);
            }
            methodTypes.add(methodType);
        }

        private static void validateMethodType(String type) {
            String[] typeParts = type.split("_");
            // check return type (second part)
            if (typeParts.length != 2 || typeParts[1].length() != 1
                    || !isBasicTypeChar(typeParts[1].charAt(0))) {
                throw new RuntimeException(
                        "Method type signature must be of form [LJIFD]*_[LJIFDV]");
            }
            // expand and check arguments (first part)
            expandSignature(typeParts[0]);
        }

        // Convert LL -> LL, L3 -> LLL
        private static String expandSignature(String signature) {
            StringBuilder sb = new StringBuilder();
            char last = 'X';
            int count = 0;
            for (int i = 0; i < signature.length(); i++) {
                char c = signature.charAt(i);
                if (c >= '0' && c <= '9') {
                    count *= 10;
                    count += (c - '0');
                } else {
                    requireBasicType(c);
                    for (int j = 1; j < count; j++) {
                        sb.append(last);
                    }
                    sb.append(c);
                    last = c;
                    count = 0;
                }
            }

            // ended with a number, e.g., "L2": append last char count - 1 times
            if (count > 1) {
                requireBasicType(last);
                for (int j = 1; j < count; j++) {
                    sb.append(last);
                }
            }
            return sb.toString();
        }

        private static void requireBasicType(char c) {
            if (!isArgBasicTypeChar(c)) {
                throw new RuntimeException(
                        "Character " + c + " must correspond to a basic field type: LIJFD");
            }
        }

        private static Class<?> simpleType(char c) {
            if (isBasicTypeChar(c)) {
                return LambdaForm.BasicType.basicType(c).basicTypeClass();
            }
            switch (c) {
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                    throw new IllegalArgumentException("Not a valid primitive: " + c +
                            " (use I instead)");
                default:
                    throw new IllegalArgumentException("Not a primitive: " + c);
            }
        }
    }

    /*
     * Returns a map of class name in internal form to the corresponding class bytes
     * per the given stream of SPECIES_RESOLVE and LF_RESOLVE trace logs.
     *
     * Used by GenerateJLIClassesPlugin to pre-generate holder classes during
     * jlink phase.
     */
    static Map<String, byte[]> generateHolderClasses(Stream<String> traces)  {
        Objects.requireNonNull(traces);
        HolderClassBuilder builder = new HolderClassBuilder();
        traces.map(line -> line.split(" "))
                .forEach(parts -> {
                    switch (parts[0]) {
                        case "[SPECIES_RESOLVE]":
                            // Allow for new types of species data classes being resolved here
                            assert parts.length >= 2;
                            if (parts[1].startsWith(BMH_SPECIES_PREFIX)) {
                                String species = parts[1].substring(BMH_SPECIES_PREFIX.length());
                                if (!"L".equals(species)) {
                                    builder.addSpeciesType(species);
                                }
                            }
                            break;
                        case "[LF_RESOLVE]":
                            assert parts.length > 3;
                            String methodType = parts[3];
                            if (parts[1].equals(INVOKERS_HOLDER_CLASS_NAME)) {
                                if ("linkToTargetMethod".equals(parts[2]) ||
                                        "linkToCallSite".equals(parts[2])) {
                                    builder.addCallSiteType(methodType);
                                } else {
                                    builder.addInvokerType(methodType);
                                }
                            } else if (parts[1].contains("DirectMethodHandle")) {
                                String dmh = parts[2];
                                // ignore getObject etc for now (generated by default)
                                if (DMH_METHOD_TYPE_MAP.containsKey(dmh)) {
                                    builder.addDMHMethodType(dmh, methodType);
                                }
                            }
                            break;
                        default:
                            break; // ignore
                    }
                });

        return builder.build();
    }

    /**
     * Returns a {@code byte[]} representation of a class implementing
     * the zero and identity forms of all {@code LambdaForm.BasicType}s.
     */
    static byte[] generateBasicFormsClassBytes(String className) {
        ArrayList<LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        HashSet<String> dedupSet = new HashSet<>();
        for (LambdaForm.BasicType type : LambdaForm.BasicType.values()) {
            LambdaForm zero = LambdaForm.zeroForm(type);
            String name = zero.kind.defaultLambdaName
                   + "_" + zero.returnType().basicTypeChar();
            if (dedupSet.add(name)) {
                names.add(name);
                forms.add(zero);
            }

            LambdaForm identity = LambdaForm.identityForm(type);
            name = identity.kind.defaultLambdaName
                   + "_" + identity.returnType().basicTypeChar();
            if (dedupSet.add(name)) {
                names.add(name);
                forms.add(identity);
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new LambdaForm[0]));
    }

    /**
     * Returns a {@code byte[]} representation of a class implementing
     * DirectMethodHandle of each pairwise combination of {@code MethodType} and
     * an {@code int} representing method type.
     */
    static byte[] generateDirectMethodHandleHolderClassBytes(String className,
            MethodType[] methodTypes, int[] types) {
        ArrayList<LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < methodTypes.length; i++) {
            // invokeVirtual and invokeInterface must have a leading Object
            // parameter, i.e., the receiver
            if (types[i] == LF_INVVIRTUAL || types[i] == LF_INVINTERFACE) {
                if (methodTypes[i].parameterCount() < 1 ||
                        methodTypes[i].parameterType(0) != Object.class) {
                    throw new InternalError("Invalid method type for " +
                            (types[i] == LF_INVVIRTUAL ? "invokeVirtual" : "invokeInterface") +
                            " DMH, needs at least two leading reference arguments: " +
                            methodTypes[i]);
                }
            }

            LambdaForm form = DirectMethodHandle.makePreparedLambdaForm(methodTypes[i], types[i]);
            forms.add(form);
            names.add(form.kind.defaultLambdaName);
        }
        for (Wrapper wrapper : Wrapper.values()) {
            if (wrapper == Wrapper.VOID) {
                continue;
            }
            for (byte b = DirectMethodHandle.AF_GETFIELD; b < DirectMethodHandle.AF_LIMIT; b++) {
                int ftype = DirectMethodHandle.ftypeKind(wrapper.primitiveType());
                LambdaForm form = DirectMethodHandle
                        .makePreparedFieldLambdaForm(b, /*isVolatile*/false, ftype);
                if (form.kind != LambdaForm.Kind.GENERIC) {
                    forms.add(form);
                    names.add(form.kind.defaultLambdaName);
                }
                // volatile
                form = DirectMethodHandle
                        .makePreparedFieldLambdaForm(b, /*isVolatile*/true, ftype);
                if (form.kind != LambdaForm.Kind.GENERIC) {
                    forms.add(form);
                    names.add(form.kind.defaultLambdaName);
                }
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new LambdaForm[0]));
    }

    /**
     * Returns a {@code byte[]} representation of a class implementing
     * DelegatingMethodHandles of each {@code MethodType} kind in the
     * {@code methodTypes} argument.
     */
    static byte[] generateDelegatingMethodHandleHolderClassBytes(String className,
            MethodType[] methodTypes) {

        HashSet<MethodType> dedupSet = new HashSet<>();
        ArrayList<LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < methodTypes.length; i++) {
            // generate methods representing the DelegatingMethodHandle
            if (dedupSet.add(methodTypes[i])) {
                // reinvokers are variant with the associated SpeciesData
                // and shape of the target LF, but we can easily pregenerate
                // the basic reinvokers associated with Species_L. Ultimately we
                // may want to consider pregenerating more of these, which will
                // require an even more complex naming scheme
                LambdaForm reinvoker = makeReinvokerFor(methodTypes[i]);
                forms.add(reinvoker);
                String speciesSig = BoundMethodHandle.speciesDataFor(reinvoker).key();
                assert(speciesSig.equals("L"));
                names.add(reinvoker.kind.defaultLambdaName + "_" + speciesSig);

                LambdaForm delegate = makeDelegateFor(methodTypes[i]);
                forms.add(delegate);
                names.add(delegate.kind.defaultLambdaName);
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new LambdaForm[0]));
    }

    /**
     * Returns a {@code byte[]} representation of a class implementing
     * the invoker forms for the set of supplied {@code invokerMethodTypes}
     * and {@code callSiteMethodTypes}.
     */
    static byte[] generateInvokersHolderClassBytes(String className,
            MethodType[] invokerMethodTypes, MethodType[] callSiteMethodTypes) {

        HashSet<MethodType> dedupSet = new HashSet<>();
        ArrayList<LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        int[] types = {
            MethodTypeForm.LF_EX_LINKER,
            MethodTypeForm.LF_EX_INVOKER,
            MethodTypeForm.LF_GEN_LINKER,
            MethodTypeForm.LF_GEN_INVOKER
        };

        for (int i = 0; i < invokerMethodTypes.length; i++) {
            // generate methods representing invokers of the specified type
            if (dedupSet.add(invokerMethodTypes[i])) {
                for (int type : types) {
                    LambdaForm invokerForm = Invokers.invokeHandleForm(invokerMethodTypes[i],
                            /*customized*/false, type);
                    forms.add(invokerForm);
                    names.add(invokerForm.kind.defaultLambdaName);
                }
            }
        }

        dedupSet = new HashSet<>();
        for (int i = 0; i < callSiteMethodTypes.length; i++) {
            // generate methods representing invokers of the specified type
            if (dedupSet.add(callSiteMethodTypes[i])) {
                LambdaForm callSiteForm = Invokers.callSiteForm(callSiteMethodTypes[i], true);
                forms.add(callSiteForm);
                names.add(callSiteForm.kind.defaultLambdaName);

                LambdaForm methodHandleForm = Invokers.callSiteForm(callSiteMethodTypes[i], false);
                forms.add(methodHandleForm);
                names.add(methodHandleForm.kind.defaultLambdaName);
            }
        }

        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new LambdaForm[0]));
    }

    /*
     * Generate customized code for a set of LambdaForms of specified types into
     * a class with a specified name.
     */
    private static byte[] generateCodeBytesForLFs(String className, String[] names, LambdaForm[] forms) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(CLASSFILE_VERSION, Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                className, null, InvokerBytecodeGenerator.INVOKER_SUPER_NAME, null);
        cw.visitSource(className.substring(className.lastIndexOf('/') + 1), null);

        for (int i = 0; i < forms.length; i++) {
            InvokerBytecodeGenerator g
                = new InvokerBytecodeGenerator(className, names[i], forms[i], forms[i].methodType());
            g.setClassWriter(cw);
            g.addMethod();
        }

        return cw.toByteArray();
    }

    private static LambdaForm makeReinvokerFor(MethodType type) {
        MethodHandle emptyHandle = MethodHandles.empty(type);
        return DelegatingMethodHandle.makeReinvokerForm(emptyHandle,
                MethodTypeForm.LF_REBIND,
                BoundMethodHandle.speciesData_L(),
                BoundMethodHandle.speciesData_L().getterFunction(0));
    }

    private static LambdaForm makeDelegateFor(MethodType type) {
        MethodHandle handle = MethodHandles.empty(type);
        return DelegatingMethodHandle.makeReinvokerForm(
                handle,
                MethodTypeForm.LF_DELEGATE,
                DelegatingMethodHandle.class,
                DelegatingMethodHandle.NF_getTarget);
    }

    /**
     * Returns a {@code byte[]} representation of {@code BoundMethodHandle}
     * species class implementing the signature defined by {@code types}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Map.Entry<String, byte[]> generateConcreteBMHClassBytes(final String types) {
        for (char c : types.toCharArray()) {
            if (!isArgBasicTypeChar(c)) {
                throw new IllegalArgumentException("All characters must "
                        + "correspond to a basic field type: LIJFD");
            }
        }
        final BoundMethodHandle.SpeciesData species = BoundMethodHandle.SPECIALIZER.findSpecies(types);
        final String className = species.speciesCode().getName();
        final ClassSpecializer.Factory factory = BoundMethodHandle.SPECIALIZER.factory();
        final byte[] code = factory.generateConcreteSpeciesCodeFile(className, species);
        return Map.entry(className.replace('.', '/'), code);
    }

}
