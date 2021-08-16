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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.Option;
import jdk.vm.ci.meta.JavaConstant;

/**
 * Encapsulates a JNI reference to an object in the HotSpot heap.
 *
 * {@link IndirectHotSpotObjectConstantImpl} objects are only allocated in the shared library heap.
 *
 * @see HotSpotObjectConstantScope
 */
final class IndirectHotSpotObjectConstantImpl extends HotSpotObjectConstantImpl {
    /**
     * An object handle in {@code JVMCI::_object_handles}.
     */
    private long objectHandle;

    /**
     * Lazily computed hash code.
     */
    private int hashCode;

    final IndirectHotSpotObjectConstantImpl base;

    private static class Audit {
        final Object scope;
        final long handle;
        final Throwable origin;

        Audit(Object scope, long handle, Throwable origin) {
            this.scope = scope;
            this.handle = handle;
            this.origin = origin;
        }
    }

    /**
     * Details useful to audit a scoped handle used after its creating scope closes. Set to an
     * {@link Audit} object if {@link HotSpotJVMCIRuntime.Option#AuditHandles} is true otherwise to
     * {@link HotSpotObjectConstantScope#localScopeDescription}.
     */
    private Object rawAudit;

    @SuppressWarnings("serial")
    @VMEntryPoint
    private IndirectHotSpotObjectConstantImpl(long objectHandle, boolean compressed, boolean skipRegister) {
        super(compressed);
        assert objectHandle != 0 && UnsafeAccess.UNSAFE.getLong(objectHandle) != 0;
        this.objectHandle = objectHandle;
        this.base = null;
        if (!skipRegister) {
            HotSpotObjectConstantScope scope = HotSpotObjectConstantScope.CURRENT.get();
            if (scope != null && !scope.isGlobal()) {
                scope.add(this);
                if (HotSpotJVMCIRuntime.Option.AuditHandles.getBoolean()) {
                    rawAudit = new Audit(scope.localScopeDescription, objectHandle, new Throwable() {
                        @Override
                        public String toString() {
                            return "Created " + objectHandle;
                        }
                    });
                }
            } else {
                HandleCleaner.create(this, objectHandle);
            }
        }
    }

    private IndirectHotSpotObjectConstantImpl(IndirectHotSpotObjectConstantImpl base, boolean compressed) {
        super(compressed);
        // This is a variant of an original object that only varies in compress vs uncompressed.
        // Instead of creating a new handle, reference that object and objectHandle.
        this.objectHandle = base.getHandle();
        // There should only be one level of indirection to the base object.
        assert base.base == null || base.base.base == null;
        this.base = base.base != null ? base.base : base;
    }

    long getHandle() {
        checkHandle();
        return objectHandle;
    }

    private void checkHandle() {
        if (objectHandle == 0L) {
            String message;
            if (rawAudit instanceof Audit) {
                Audit audit = (Audit) rawAudit;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                ps.println("Foreign object reference " + audit.handle + " created in scope '" + audit.scope + "' is no longer valid. Origin: {");
                audit.origin.printStackTrace(ps);
                ps.print('}');
                ps.flush();
                message = baos.toString();
            } else {
                message = "Foreign object reference created in scope '" + rawAudit + "' is no longer valid. " +
                                "Set property " + Option.AuditHandles.getPropertyName() + "=true to show origin of invalid foreign references.";
            }
            throw new NullPointerException(message);
        }
    }

    boolean isValid() {
        return objectHandle != 0L;
    }

    @Override
    public HotSpotResolvedObjectType getType() {
        checkHandle();
        return super.getType();
    }

    /**
     * Clears the foreign object reference.
     */
    void clear(Object scopeDescription) {
        checkHandle();
        CompilerToVM.compilerToVM().deleteGlobalHandle(objectHandle);
        if (rawAudit == null) {
            rawAudit = scopeDescription;
        }
        objectHandle = 0L;
    }

    @Override
    public JavaConstant compress() {
        assert !compressed;
        return new IndirectHotSpotObjectConstantImpl(this, true);
    }

    @Override
    public JavaConstant uncompress() {
        assert compressed;
        return new IndirectHotSpotObjectConstantImpl(this, false);
    }

    @Override
    public int getIdentityHashCode() {
        checkHandle();
        int hash = hashCode;
        if (hash == 0) {
            hash = runtime().compilerToVm.getIdentityHashCode(this);
            if (hash == 0) {
                hash = 31;
            }
            hashCode = hash;
        }
        return hash;
    }
}
