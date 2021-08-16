/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

/**
 * Each nested class tests a member of a specific access.
 *
 * Caller class is cached when setAccessible is not used to suppress the access
 * check and only access allowed.  If not accessible, caller class is not cached.
 */
public class AccessTest {
    public static class PublicConstructor implements Callable<Object> {
        public Object call() throws Exception {
            Constructor c = Members.class.getConstructor();
            return c.newInstance();
        }
    }

    public static class PublicMethod extends Members implements Callable<Void> {
        public Void call() throws Exception {
            Method m = Members.class.getDeclaredMethod("publicMethod");
            m.invoke(new PublicMethod());
            return null;
        }
    }

    public static class ProtectedMethod extends Members implements Callable<Void> {
        public Void call() throws Exception {
            Method m = Members.class.getDeclaredMethod("protectedMethod");
            m.invoke(new ProtectedMethod());
            return null;
        }
    }

    /*
     * private field is not accessible.  So caller class is not cached.
     */
    public static class PrivateMethod extends Members implements Callable<Void> {
        public Void call() throws Exception {
            Method m = Members.class.getDeclaredMethod("privateMethod");
            try {
                m.invoke(new ProtectedMethod());
            } catch (IllegalAccessException e) {
            }
            return null;
        }
    }

    public static class PublicField extends Members implements Callable<Void> {
        public Void call() throws Exception {
            Field f = Members.class.getDeclaredField("publicField");
            f.get(new PublicField());
            return null;
        }
    }

    public static class ProtectedField extends Members implements Callable<Void> {
        public Void call() throws Exception {
            Field f = Members.class.getDeclaredField("protectedField");
            f.get(new ProtectedField());
            return null;
        }
    }

    /*
     * private field is not accessible.  So caller class is not cached.
     */
    public static class PrivateField implements Callable<Void> {
        public Void call() throws Exception {
            Field f = Members.class.getDeclaredField("privateField");
            try {
                f.get(new Members());
            } catch (IllegalAccessException e) {
            }
            return null;
        }
    }

    /*
     * Validate final field
     */
    public static class FinalField implements Callable<Void> {
        final Field f;
        final boolean isStatic;
        public FinalField(String name) throws Exception {
            this.f = Members.class.getDeclaredField(name);
            this.isStatic = Modifier.isStatic(f.getModifiers());
            if (!Modifier.isFinal(f.getModifiers())) {
                throw new RuntimeException("not a final field");
            }
        }
        public Void call() throws Exception {
            Members obj = isStatic ? null : new Members();
            try {
                f.set(obj, 20);
                throw new RuntimeException("IllegalAccessException expected");
            } catch (IllegalAccessException e) {
                // expected
            }
            return null;
        }
    }

    public static class PublicFinalField extends FinalField {
        public PublicFinalField() throws Exception {
            super("publicFinalField");
        }
    }

    public static class PrivateFinalField extends FinalField {
        public PrivateFinalField() throws Exception {
            super("privateFinalField");
        }
    }

    public static class PublicStaticFinalField extends FinalField {
        public PublicStaticFinalField() throws Exception {
            super("publicStaticFinalField");
        }
    }

    public static class PrivateStaticFinalField extends FinalField {
        public PrivateStaticFinalField() throws Exception {
            super("privateStaticFinalField");
        }
    }

    public static class NewInstance implements Callable<Object> {
        public Object call() throws Exception {
            return Members.class.newInstance();
        }
    }

}
