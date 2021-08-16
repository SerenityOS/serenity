/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

import java.lang.ref.Reference;
import javax.swing.SwingContainer;

/**
 * A BeanDescriptor provides global information about a "bean",
 * including its Java class, its displayName, etc.
 * <p>
 * This is one of the kinds of descriptor returned by a BeanInfo object,
 * which also returns descriptors for properties, method, and events.
 *
 * @since 1.1
 */

public class BeanDescriptor extends FeatureDescriptor {

    private Reference<? extends Class<?>> beanClassRef;
    private Reference<? extends Class<?>> customizerClassRef;

    /**
     * Create a BeanDescriptor for a bean that doesn't have a customizer.
     *
     * @param beanClass  The Class object of the Java class that implements
     *          the bean.  For example sun.beans.OurButton.class.
     */
    public BeanDescriptor(Class<?> beanClass) {
        this(beanClass, null);
    }

    /**
     * Create a BeanDescriptor for a bean that has a customizer.
     *
     * @param beanClass  The Class object of the Java class that implements
     *          the bean.  For example sun.beans.OurButton.class.
     * @param customizerClass  The Class object of the Java class that implements
     *          the bean's Customizer.  For example sun.beans.OurButtonCustomizer.class.
     */
    public BeanDescriptor(Class<?> beanClass, Class<?> customizerClass) {
        this.beanClassRef = getWeakReference(beanClass);
        this.customizerClassRef = getWeakReference(customizerClass);

        String name = beanClass.getName();
        while (name.indexOf('.') >= 0) {
            name = name.substring(name.indexOf('.')+1);
        }
        setName(name);

        JavaBean annotation = beanClass.getAnnotation(JavaBean.class);
        if (annotation != null) {
            setPreferred(true);
            String description = annotation.description();
            if (!description.isEmpty()) {
                setShortDescription(description);
            }
        }
        SwingContainer container = beanClass.getAnnotation(SwingContainer.class);
        if (container != null) {
            setValue("isContainer", container.value());
            setValue("containerDelegate", container.delegate());
        }
    }

    /**
     * Gets the bean's Class object.
     *
     * @return The Class object for the bean.
     */
    public Class<?> getBeanClass() {
        return (this.beanClassRef != null)
                ? this.beanClassRef.get()
                : null;
    }

    /**
     * Gets the Class object for the bean's customizer.
     *
     * @return The Class object for the bean's customizer.  This may
     * be null if the bean doesn't have a customizer.
     */
    public Class<?> getCustomizerClass() {
        return (this.customizerClassRef != null)
                ? this.customizerClassRef.get()
                : null;
    }

    /*
     * Package-private dup constructor
     * This must isolate the new object from any changes to the old object.
     */
    BeanDescriptor(BeanDescriptor old) {
        super(old);
        beanClassRef = old.beanClassRef;
        customizerClassRef = old.customizerClassRef;
    }

    void appendTo(StringBuilder sb) {
        appendTo(sb, "beanClass", this.beanClassRef);
        appendTo(sb, "customizerClass", this.customizerClassRef);
    }
}
