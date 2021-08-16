/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.runtime.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.meta.SpeculationLog.SpeculationReasonEncoding;
import jdk.vm.ci.runtime.JVMCI;

public class TestSpeculationLog extends MethodUniverse {

    static final class Dummy implements SpeculationLog.SpeculationReason {

        final int[] ints = {Integer.MIN_VALUE, -42, -1, 0, 1, 42, Integer.MAX_VALUE};
        final long[] longs = {Long.MIN_VALUE, -42, -1, 0, 1, 42, Long.MAX_VALUE};
        final String[] strings = {null, "non-empty string", ""};
        final Collection<ResolvedJavaMethod> methods = new ArrayList<>(MethodUniverse.methods.values()).subList(0, 10);
        final Collection<ResolvedJavaMethod> constructors = new ArrayList<>(MethodUniverse.constructors.values()).subList(0, 10);
        final Collection<ResolvedJavaType> types = new ArrayList<>(TypeUniverse.javaTypes).subList(0, 10);

        private final boolean useCache;
        private SpeculationReasonEncoding cachedEncoding;

        Dummy(boolean useCache) {
            this.useCache = useCache;
        }

        @Override
        public SpeculationReasonEncoding encode(Supplier<SpeculationReasonEncoding> encodingSupplier) {
            SpeculationReasonEncoding encoding = cachedEncoding;
            if (encoding == null) {
                encoding = encodingSupplier.get();
                for (int i : ints) {
                    encoding.addInt(i);
                }
                for (long l : longs) {
                    encoding.addLong(l);
                }
                for (String s : strings) {
                    encoding.addString(s);
                }
                for (ResolvedJavaMethod m : methods) {
                    encoding.addMethod(m);
                }
                for (ResolvedJavaMethod c : constructors) {
                    encoding.addMethod(c);
                }
                for (ResolvedJavaType t : types) {
                    encoding.addType(t);
                }
                encoding.addMethod(null);
                encoding.addType(null);
            }
            if (useCache) {
                cachedEncoding = encoding;
            }
            return encoding;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Dummy) {
                Dummy that = (Dummy) obj;
                return Arrays.equals(this.ints, that.ints) &&
                                Arrays.equals(this.longs, that.longs) &&
                                Arrays.equals(this.strings, that.strings) &&
                                this.methods.equals(that.methods) &&
                                this.constructors.equals(that.constructors) &&
                                this.types.equals(that.types);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(ints) ^
                            Arrays.hashCode(longs) ^
                            Arrays.hashCode(strings) ^
                            methods.hashCode() ^
                            constructors.hashCode() ^
                            types.hashCode();
        }
    }

    @Test
    public synchronized void testSpeculationIdentity() {
        CodeCacheProvider codeCache = JVMCI.getRuntime().getHostJVMCIBackend().getCodeCache();
        SpeculationLog log = codeCache.createSpeculationLog();
        Dummy spec1 = new Dummy(true);
        Dummy spec2 = new Dummy(false);
        Assert.assertTrue(log.maySpeculate(spec1));
        Assert.assertTrue(log.maySpeculate(spec2));
        SpeculationLog.Speculation s1 = log.speculate(spec1);
        SpeculationLog.Speculation s2 = log.speculate(spec2);
        Assert.assertTrue("Speculation should maintain identity", s1.equals(s2));
        JavaConstant e1 = metaAccess.encodeSpeculation(s1);
        JavaConstant e2 = metaAccess.encodeSpeculation(s2);
        Assert.assertTrue("speculation encoding should maintain identity", e1.equals(e2));
    }
}
