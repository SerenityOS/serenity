/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.consumer;

import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordedThreadGroup;
import jdk.jfr.internal.Type;

/**
 * Abstract factory for creating specialized types
 */
public abstract class ObjectFactory<T> {
    private static final JdkJfrConsumer PRIVATE_ACCESS = JdkJfrConsumer.instance();

    private static final String TYPE_PREFIX_VERSION_1 = "com.oracle.jfr.types.";
    private static final String TYPE_PREFIX_VERSION_2 = Type.TYPES_PREFIX;
    public static final String STACK_FRAME_VERSION_1 = TYPE_PREFIX_VERSION_1 + "StackFrame";
    public static final String STACK_FRAME_VERSION_2 = TYPE_PREFIX_VERSION_2 + "StackFrame";

    static ObjectFactory<?> create(Type type, TimeConverter timeConverter) {
        switch (type.getName()) {
        case "java.lang.Thread":
            return createThreadFactory(type, timeConverter);
        case TYPE_PREFIX_VERSION_1 + "StackFrame":
        case TYPE_PREFIX_VERSION_2 + "StackFrame":
            return createFrameFactory(type, timeConverter);
        case TYPE_PREFIX_VERSION_1 + "Method":
        case TYPE_PREFIX_VERSION_2 + "Method":
            return createMethodFactory(type, timeConverter);
        case TYPE_PREFIX_VERSION_1 + "ThreadGroup":
        case TYPE_PREFIX_VERSION_2 + "ThreadGroup":
            return createdThreadGroupFactory(type, timeConverter);
        case TYPE_PREFIX_VERSION_1 + "StackTrace":
        case TYPE_PREFIX_VERSION_2 + "StackTrace":
            return createStackTraceFactory(type, timeConverter);
        case TYPE_PREFIX_VERSION_1 + "ClassLoader":
        case TYPE_PREFIX_VERSION_2 + "ClassLoader":
            return createClassLoaderFactory(type, timeConverter);
        case "java.lang.Class":
            return createClassFactory(type, timeConverter);
        }
        return null;
    }

    private static ObjectFactory<RecordedClass> createClassFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedClass>(type, timeConverter) {
            @Override
            RecordedClass createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedClass(objectContext, id, values);
            }
        };
    }

    private static ObjectFactory<?> createClassLoaderFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedClassLoader>(type, timeConverter) {
            @Override
            RecordedClassLoader createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedClassLoader(objectContext, id, values);
            }
        };
    }

    private static ObjectFactory<RecordedStackTrace> createStackTraceFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedStackTrace>(type, timeConverter) {
            @Override
            RecordedStackTrace createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedStackTrace(objectContext, values);
            }
        };
    }

    private static ObjectFactory<RecordedThreadGroup> createdThreadGroupFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedThreadGroup>(type, timeConverter) {
            @Override
            RecordedThreadGroup createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedThreadGroup(objectContext, values);
            }
        };
    }

    private static ObjectFactory<RecordedMethod> createMethodFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedMethod>(type, timeConverter) {
            @Override
            RecordedMethod createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedMethod(objectContext, values);
            }
        };
    }

    private static ObjectFactory<RecordedFrame> createFrameFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedFrame>(type, timeConverter) {
            @Override
            RecordedFrame createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedFrame(objectContext, values);
            }
        };
    }

    private static ObjectFactory<RecordedThread> createThreadFactory(Type type, TimeConverter timeConverter) {
        return new ObjectFactory<RecordedThread>(type, timeConverter) {
            @Override
            RecordedThread createTyped(ObjectContext objectContext, long id, Object[] values) {
                return PRIVATE_ACCESS.newRecordedThread(objectContext, id, values);
            }
        };
    }

    private final ObjectContext objectContext;

    private ObjectFactory(Type type, TimeConverter timeConverter) {
        this.objectContext = new ObjectContext(null, type.getFields(), timeConverter);
    }

    T createObject(long id, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Object[] array) {
            return createTyped(objectContext, id, array);
        }
        throw new InternalError("Object factory must have struct type. Type was " + value.getClass().getName());
    }

    abstract T createTyped(ObjectContext objectContextm, long id, Object[] values);
}
