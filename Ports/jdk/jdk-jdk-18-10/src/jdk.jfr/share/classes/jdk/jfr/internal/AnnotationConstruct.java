/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Unsigned;

public final class AnnotationConstruct {

    private static final class AnnotationInvokationHandler implements InvocationHandler {

        private final AnnotationElement annotationElement;

        AnnotationInvokationHandler(AnnotationElement a) {
            this.annotationElement = a;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            int parameters = method.getTypeParameters().length;
            if (parameters == 0 && annotationElement.hasValue(methodName)) {
                return annotationElement.getValue(methodName);
            }
            throw new UnsupportedOperationException("Flight Recorder proxy only supports members declared in annotation interfaces, i.e. not toString, equals etc.");
        }
    }

    private List<AnnotationElement> annotationElements;
    private byte unsignedFlag = -1;
    public AnnotationConstruct(List<AnnotationElement> elements) {
        this.annotationElements = List.copyOf(elements);
    }

    public AnnotationConstruct() {
        this(List.of());
    }

    public void setAnnotationElements(List<AnnotationElement> elements) {
        annotationElements = List.copyOf(elements);
    }

    public String getLabel() {
        Label label = getAnnotation(Label.class);
        if (label == null) {
            return null;
        }
        return label.value();
    }

    public String getDescription() {
        Description description = getAnnotation(Description.class);
        if (description == null) {
            return null;
        }
        return description.value();
    }

    @SuppressWarnings("unchecked")
    public final <T> T getAnnotation(Class<? extends Annotation> clazz) {
        AnnotationElement ae = getAnnotationElement(clazz);
        if (ae != null) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new AnnotationInvokationHandler(ae));
        }
        return null;
    }

    public List<AnnotationElement> getUnmodifiableAnnotationElements() {
        return annotationElements;
    }

    private AnnotationElement getAnnotationElement(Class<? extends Annotation> clazz) {
        // if multiple annotation elements with the same name exists, prioritize
        // the one with the same id. Note, id alone is not a guarantee, since it
        // may differ between JVM instances.
        long id = Type.getTypeId(clazz);
        String className = clazz.getName();
        for (AnnotationElement a : getUnmodifiableAnnotationElements()) {
            if (a.getTypeId() == id && a.getTypeName().equals(className)) {
                return a;
            }
        }
        for (AnnotationElement a : getUnmodifiableAnnotationElements()) {
            if (a.getTypeName().equals(className)) {
                return a;
            }
        }
        return null;
    }

    public boolean hasUnsigned() {
        // Must be initialized lazily since some annotation elements
        // are added after construction
        if (unsignedFlag < 0) {
            Unsigned unsigned = getAnnotation(Unsigned.class);
            unsignedFlag = (byte) (unsigned == null ? 0 :1);
        }
        return unsignedFlag == (byte)1 ? true : false;
    }
}
