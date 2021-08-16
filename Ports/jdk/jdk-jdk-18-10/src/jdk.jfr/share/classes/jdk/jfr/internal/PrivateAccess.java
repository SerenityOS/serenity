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

package jdk.jfr.internal;

import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Configuration;
import jdk.jfr.EventSettings;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorderPermission;
import jdk.jfr.Recording;
import jdk.jfr.SettingControl;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.management.EventSettingsModifier;

/**
 * Provides access to package private function in jdk.jfr.
 * <p>
 * The static initializer in this class loads the Settings class, which will
 * call {@link #setPrivateAccess(PrivateAccess)} on this class, which can be
 * used call to package protected methods.
 *
 * This is similar to how java.lang accesses package private methods in
 * java.lang.reflect.
 */
public abstract class PrivateAccess {
    private volatile static PrivateAccess instance;

    public static PrivateAccess getInstance() {
        // Can't be initialized in <clinit> because it may
        // deadlock with FlightRecorderPermission.<clinit>
        if (instance == null) {
            // Will trigger
            // FlightRecorderPermission.<clinit>
            // which will call PrivateAccess.setPrivateAccess
            new FlightRecorderPermission(Utils.REGISTER_EVENT);
        }
        return instance;
    }

    public static void setPrivateAccess(PrivateAccess pa) {
        instance = pa;
    }

    public abstract Type getType(Object o);

    public abstract Configuration newConfiguration(String name, String label, String description, String provider, Map<String,String> settings, String contents);

    public abstract EventType newEventType(PlatformEventType eventTypes);

    public abstract AnnotationElement newAnnotation(Type annotationType, List<Object> values, boolean boot);

    public abstract ValueDescriptor newValueDescriptor(String name, Type fieldType, List<AnnotationElement> annotations, int dimension, boolean constantPool, String fieldName);

    public abstract PlatformRecording getPlatformRecording(Recording r);

    public abstract PlatformEventType getPlatformEventType(EventType eventType);

    public abstract boolean isConstantPool(ValueDescriptor v);

    public abstract String getFieldName(ValueDescriptor v);

    public abstract ValueDescriptor newValueDescriptor(Class<?> type, String name);

    public abstract SettingDescriptor newSettingDescriptor(Type type, String name, String def, List<AnnotationElement> aes);

    public abstract void setAnnotations(ValueDescriptor v, List<AnnotationElement> a);

    public abstract void setAnnotations(SettingDescriptor s, List<AnnotationElement> a);

    public abstract boolean isUnsigned(ValueDescriptor v);

    public abstract PlatformRecorder getPlatformRecorder();

    @SuppressWarnings("removal")
    public abstract AccessControlContext getContext(SettingControl sc);

    public abstract EventSettings newEventSettings(EventSettingsModifier esm);
}
