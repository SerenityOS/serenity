/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.hotspot;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;
import jdk.vm.ci.meta.UnresolvedJavaType;

/**
 * Represents a method signature.
 */
public class HotSpotSignature implements Signature {

    private final List<String> parameters = new ArrayList<>();
    private final String returnType;
    private final String originalString;
    private ResolvedJavaType[] parameterTypes;
    private ResolvedJavaType returnTypeCache;
    private final HotSpotJVMCIRuntime runtime;

    public HotSpotSignature(HotSpotJVMCIRuntime runtime, String signature) {
        this.runtime = runtime;
        if (signature.length() == 0) {
            throw new IllegalArgumentException("Signature cannot be empty");
        }
        this.originalString = signature;

        if (signature.charAt(0) == '(') {
            int cur = 1;
            while (cur < signature.length() && signature.charAt(cur) != ')') {
                int nextCur = parseSignature(signature, cur);
                parameters.add(signature.substring(cur, nextCur));
                cur = nextCur;
            }

            cur++;
            int nextCur = parseSignature(signature, cur);
            returnType = signature.substring(cur, nextCur);
            if (nextCur != signature.length()) {
                throw new IllegalArgumentException("Extra characters at end of signature: " + signature);
            }
        } else {
            throw new IllegalArgumentException("Signature must start with a '(': " + signature);
        }
    }

    public HotSpotSignature(HotSpotJVMCIRuntime runtime, ResolvedJavaType returnType, ResolvedJavaType... parameterTypes) {
        this.runtime = runtime;
        this.parameterTypes = parameterTypes.clone();
        this.returnTypeCache = returnType;
        this.returnType = returnType.getName();
        StringBuilder sb = new StringBuilder("(");
        for (JavaType type : parameterTypes) {
            parameters.add(type.getName());
            sb.append(type.getName());
        }
        sb.append(")").append(returnType.getName());
        this.originalString = sb.toString();
        assert new HotSpotSignature(runtime, originalString).equals(this);
    }

    private static int parseSignature(String signature, int start) {
        try {
            int cur = start;
            char first;
            do {
                first = signature.charAt(cur);
                cur++;
            } while (first == '[');

            switch (first) {
                case 'L':
                    while (signature.charAt(cur) != ';') {
                        if (signature.charAt(cur) == '.') {
                            throw new IllegalArgumentException("Class name in signature contains '.' at index " + cur + ": " + signature);
                        }
                        cur++;
                    }
                    cur++;
                    break;
                case 'V':
                case 'I':
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'J':
                case 'S':
                case 'Z':
                    break;
                default:
                    throw new IllegalArgumentException("Invalid character '" + signature.charAt(cur - 1) + "' at index " + (cur - 1) + " in signature: " + signature);
            }
            return cur;
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Truncated signature: " + signature);
        }
    }

    @Override
    public int getParameterCount(boolean withReceiver) {
        return parameters.size() + (withReceiver ? 1 : 0);
    }

    @Override
    public JavaKind getParameterKind(int index) {
        return JavaKind.fromTypeString(parameters.get(index));
    }

    private static boolean checkValidCache(ResolvedJavaType type, ResolvedJavaType accessingClass) {
        assert accessingClass != null;
        if (type == null) {
            return false;
        } else if (type instanceof HotSpotResolvedObjectTypeImpl) {
            return ((HotSpotResolvedObjectTypeImpl) type).isDefinitelyResolvedWithRespectTo(accessingClass);
        }
        return true;
    }

    private static JavaType getUnresolvedOrPrimitiveType(HotSpotJVMCIRuntime runtime, String name) {
        if (name.length() == 1) {
            JavaKind kind = JavaKind.fromPrimitiveOrVoidTypeChar(name.charAt(0));
            return runtime.getHostJVMCIBackend().getMetaAccess().lookupJavaType(kind.toJavaClass());
        }
        return UnresolvedJavaType.create(name);
    }

    @Override
    public JavaType getParameterType(int index, ResolvedJavaType accessingClass) {
        if (accessingClass == null) {
            // Caller doesn't care about resolution context so return an unresolved
            // or primitive type (primitive type resolution is context free)
            return getUnresolvedOrPrimitiveType(runtime, parameters.get(index));
        }
        if (parameterTypes == null) {
            parameterTypes = new ResolvedJavaType[parameters.size()];
        }

        ResolvedJavaType type = parameterTypes[index];
        if (!checkValidCache(type, accessingClass)) {
            JavaType result = runtime.lookupType(parameters.get(index), (HotSpotResolvedObjectType) accessingClass, false);
            if (result instanceof ResolvedJavaType) {
                type = (ResolvedJavaType) result;
                parameterTypes[index] = type;
            } else {
                assert result != null;
                return result;
            }
        }
        assert type != null;
        return type;
    }

    @Override
    public String toMethodDescriptor() {
        assert originalString.equals(Signature.super.toMethodDescriptor()) : originalString + " != " + Signature.super.toMethodDescriptor();
        return originalString;
    }

    @Override
    public JavaKind getReturnKind() {
        return JavaKind.fromTypeString(returnType);
    }

    @Override
    public JavaType getReturnType(ResolvedJavaType accessingClass) {
        if (accessingClass == null) {
            // Caller doesn't care about resolution context so return an unresolved
            // or primitive type (primitive type resolution is context free)
            return getUnresolvedOrPrimitiveType(runtime, returnType);
        }
        if (!checkValidCache(returnTypeCache, accessingClass)) {
            JavaType result = runtime.lookupType(returnType, (HotSpotResolvedObjectType) accessingClass, false);
            if (result instanceof ResolvedJavaType) {
                returnTypeCache = (ResolvedJavaType) result;
            } else {
                return result;
            }
        }
        return returnTypeCache;
    }

    @Override
    public String toString() {
        return "HotSpotSignature<" + originalString + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HotSpotSignature) {
            HotSpotSignature other = (HotSpotSignature) obj;
            if (other.originalString.equals(originalString)) {
                assert other.parameters.equals(parameters);
                assert other.returnType.equals(returnType);
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return originalString.hashCode();
    }
}
