/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.org.objectweb.asm;

import java.lang.reflect.InaccessibleObjectException;

public class ClassWriterExt extends ClassWriter {
    private boolean cacheInvokeDynamic = true;
    private boolean cacheMTypes = true;
    private boolean cacheMHandles = true;

    public ClassWriterExt(ClassReader cr, int flags) {
        super(cr, flags);
    }

    public ClassWriterExt(int flags) {
        super(flags);
    }
/*
    @Override
    Item newInvokeDynamicItem(final String name, final String desc,
                    final Handle bsm, final Object... bsmArgs) {
        if (cacheInvokeDynamic) {
            return super.newInvokeDynamicItem(name, desc, bsm, bsmArgs);
        }
        int type = ClassWriter.INDY;
        disableItemHashTableFor(type);
        Item result;
        try {
            return super.newInvokeDynamicItem(name, desc, bsm, bsmArgs);
        } finally {
            restoreItemHashTableFor(type);
        }
    }

    @Override
    Item newStringishItem(final int type, final String value) {
        if (type != ClassWriter.MTYPE) {
            return super.newStringishItem(type, value);
        }
        if (cacheMTypes) {
            return super.newStringishItem(type, value);
        }
        disableItemHashTableFor(type);
        try {
            return super.newStringishItem(type, value);
        } finally {
            restoreItemHashTableFor(type);
        }
    }

    @Override
    Item newHandleItem(final int tag, final String owner, final String name,
            final String desc, final boolean itf) {
        if (cacheMHandles) {
            return super.newHandleItem(tag, owner, name, desc, itf);
        }
        int type = ClassWriter.HANDLE_BASE + tag;
        disableItemHashTableFor(type);
        try {
            return super.newHandleItem(tag, owner, name, desc, itf);
        } finally {
            restoreItemHashTableFor(type);
        }
    }

    private void disableItemHashTableFor(int type) {
        for (Item i : items) {
            while (i != null) {
                if (i.type == type) {
                    i.type = -type;
                }
                i = i.next;
            }
        }
    }

    private void restoreItemHashTableFor(int type) {
        for (Item i : items) {
            while (i != null) {
                if (i.type == -type) {
                    i.type = type;
                }
                i = i.next;
            }
        }
    }
*/
    public void setCacheInvokeDynamic(boolean value) {
        if (!value) throw new Error("method isn't implemented yet");
        cacheInvokeDynamic = value;
    }
    public void setCacheMTypes(boolean value) {
        if (!value) throw new Error("method isn't implemented yet");
        cacheMTypes = value;
    }
    public void setCacheMHandles(boolean value) {
        if (!value) throw new Error("method isn't implemented yet");
        cacheMHandles = value;
    }

    public int getBytecodeLength(MethodVisitor mv) {
        ByteVector code;
        try {
            java.lang.reflect.Field field = mv.getClass().getDeclaredField("code");
            field.setAccessible(true);
            code = (ByteVector) field.get(mv);
        } catch (InaccessibleObjectException | SecurityException | ReflectiveOperationException e) {
            throw new Error("can not read field 'code' from class " + mv.getClass(), e);
        }
        try {
            java.lang.reflect.Field field = code.getClass().getDeclaredField("length");
            field.setAccessible(true);
            return field.getInt(code);
        } catch (InaccessibleObjectException | SecurityException | ReflectiveOperationException e) {
            throw new Error("can not read field 'length' from class " + code.getClass(), e);
        }
    }
}

