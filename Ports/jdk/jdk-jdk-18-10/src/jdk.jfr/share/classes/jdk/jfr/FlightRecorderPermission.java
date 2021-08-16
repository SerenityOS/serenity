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

package jdk.jfr;

import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jdk.jfr.internal.PlatformEventType;
import jdk.jfr.internal.PlatformRecorder;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.management.EventSettingsModifier;

/**
 * Permission for controlling access to Flight Recorder.
 * <p>
 * The following table provides a summary of what the permission
 * allows, and the risks of granting code the permission.
 *
 * <table class="striped">
 * <caption style="display:none">Table shows permission target name,
 *      what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 *
 * <tbody>
 * <tr>
 * <th scope="row">{@code accessFlightRecorder}</th>
 * <td>Ability to create a Flight Recorder instance, register callbacks to
 * monitor the Flight Recorder life cycle, and control an existing instance
 * of Flight Recorder, which can record and dump runtime information, such as
 * stack traces, class names, and data in user defined events.</td>
 * <td>A malicious user may be able to extract sensitive information that is stored in
 * events and interrupt Flight Recorder by installing listeners or hooks that
 * never finish.</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code registerEvent}</th>
 * <td>Ability to register events, write data to the Flight Recorder buffers,
 * and execute code in a callback function for periodic events.
 *
 * <td>A malicious user may be able to write sensitive information to Flight
 * Recorder buffers.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <p>
 * Typically, programmers do not create {@code FlightRecorderPermission} objects
 * directly. Instead the objects are created by the security policy code that is based on
 * reading the security policy file.
 *
 * @since 9
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 */
@SuppressWarnings("serial")
public final class FlightRecorderPermission extends java.security.BasicPermission {

    // Purpose of InternalAccess is to give classes in jdk.jfr.internal
    // access to package private methods in this package (jdk.jfr).
    //
    // The initialization could be done in any class in this package,
    // but this one was chosen because it is light weight and
    // lacks dependencies on other public classes.
    static {
        PrivateAccess.setPrivateAccess(new InternalAccess());
    }

    private static final class InternalAccess extends PrivateAccess {

        @Override
        public Type getType(Object o) {
            if (o instanceof AnnotationElement ae) {
                return ae.getType();
            }
            if (o instanceof EventType et) {
                return et.getType();
            }
            if (o instanceof ValueDescriptor vd) {
                return vd.getType();
            }
            if (o instanceof SettingDescriptor sd) {
                return sd.getType();
            }
            throw new Error("Unknown type " + o.getClass());
        }

        @Override
        public Configuration newConfiguration(String name, String label, String description, String provider, Map<String, String> settings, String contents) {
            return new Configuration(name, label, description, provider, settings, contents);
        }

        @Override
        public EventType newEventType(PlatformEventType platformEventType) {
            return new EventType(platformEventType);
        }

        @Override
        public AnnotationElement newAnnotation(Type annotationType, List<Object> values, boolean boot) {
            return new AnnotationElement(annotationType, values, boot);
        }

        @Override
        public ValueDescriptor newValueDescriptor(String name, Type fieldType, List<AnnotationElement> annos, int dimension, boolean constantPool, String fieldName) {
            return new ValueDescriptor(fieldType, name, annos, dimension, constantPool, fieldName);
        }

        @Override
        public PlatformRecording getPlatformRecording(Recording r) {
            return r.getInternal();
        }

        @Override
        public PlatformEventType getPlatformEventType(EventType eventType) {
            return eventType.getPlatformEventType();
        }

        @Override
        public boolean isConstantPool(ValueDescriptor v) {
            return v.isConstantPool();
        }

        @Override
        public void setAnnotations(ValueDescriptor v, List<AnnotationElement> a) {
            v.setAnnotations(a);
        }

        @Override
        public void setAnnotations(SettingDescriptor s, List<AnnotationElement> a) {
           s.setAnnotations(a);
        }

        @Override
        public String getFieldName(ValueDescriptor v) {
            return v.getJavaFieldName();
        }

        @Override
        public ValueDescriptor newValueDescriptor(Class<?> type, String name) {
            return new ValueDescriptor(type, name, List.of(), true);
        }

        @Override
        public SettingDescriptor newSettingDescriptor(Type type, String name, String defaultValue, List<AnnotationElement> annotations) {
            return new SettingDescriptor(type, name, defaultValue, annotations);
        }

        @Override
        public boolean isUnsigned(ValueDescriptor v) {
            return v.isUnsigned();
        }

        @Override
        public PlatformRecorder getPlatformRecorder() {
            return FlightRecorder.getFlightRecorder().getInternal();
        }

        @SuppressWarnings("removal")
        @Override
        public AccessControlContext getContext(SettingControl settingControl) {
            return settingControl.getContext();
        }

        @Override
        public EventSettings newEventSettings(EventSettingsModifier esm) {
            return new EventSettings.DelegatedEventSettings(esm);
        }
    }

    /**
     * Constructs a {@code FlightRecorderPermission} with the specified name.
     *
     * @param name the permission name, must be either
     *        {@code "accessFlightRecorder"} or {@code "registerEvent"}, not
     *        {@code null}
     *
     * @throws IllegalArgumentException if {@code name} is empty or not valid
     */
    public FlightRecorderPermission(String name) {
        super(Objects.requireNonNull(name));
        if (!name.equals(Utils.ACCESS_FLIGHT_RECORDER) && !name.equals(Utils.REGISTER_EVENT)) {
            throw new IllegalArgumentException("name: " + name);
        }
    }
}
