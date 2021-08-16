/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageProducer;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This is a support class to make it easier for people to provide
 * BeanInfo classes.
 * <p>
 * It defaults to providing "noop" information, and can be selectively
 * overriden to provide more explicit information on chosen topics.
 * When the introspector sees the "noop" values, it will apply low
 * level introspection and design patterns to automatically analyze
 * the target bean.
 *
 * @since 1.1
 */
public class SimpleBeanInfo implements BeanInfo {

    /**
     * Constructs a {@code SimpleBeanInfo}.
     */
    public SimpleBeanInfo() {}

    /**
     * Deny knowledge about the class and customizer of the bean.
     * You can override this if you wish to provide explicit info.
     */
    @Override
    public BeanDescriptor getBeanDescriptor() {
        return null;
    }

    /**
     * Deny knowledge of properties. You can override this
     * if you wish to provide explicit property info.
     */
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return null;
    }

    /**
     * Deny knowledge of a default property. You can override this
     * if you wish to define a default property for the bean.
     */
    @Override
    public int getDefaultPropertyIndex() {
        return -1;
    }

    /**
     * Deny knowledge of event sets. You can override this
     * if you wish to provide explicit event set info.
     */
    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        return null;
    }

    /**
     * Deny knowledge of a default event. You can override this
     * if you wish to define a default event for the bean.
     */
    @Override
    public int getDefaultEventIndex() {
        return -1;
    }

    /**
     * Deny knowledge of methods. You can override this
     * if you wish to provide explicit method info.
     */
    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        return null;
    }

    /**
     * Claim there are no other relevant BeanInfo objects.  You
     * may override this if you want to (for example) return a
     * BeanInfo for a base class.
     */
    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }

    /**
     * Claim there are no icons available.  You can override
     * this if you want to provide icons for your bean.
     */
    @Override
    public Image getIcon(final int iconKind) {
        final BeanDescriptor descriptor = getBeanDescriptor();
        if (descriptor != null) {
            final Class<?> type = descriptor.getBeanClass();
            if (type != null && type.getClassLoader() == null
                    && type.getAnnotation(JavaBean.class) != null) {
                final String name = type.getName();
                final int index = name.lastIndexOf('.');
                if (name.substring(0, index).equals("javax.swing")) {
                    final String className = type.getSimpleName();
                    switch (iconKind) {
                        case ICON_COLOR_32x32:
                            return loadImage(className, "Color32.gif");
                        case ICON_COLOR_16x16:
                            return loadImage(className, "Color16.gif");
                        case ICON_MONO_32x32:
                            return loadImage(className, "Mono32.gif");
                        case ICON_MONO_16x16:
                            return loadImage(className, "Mono16.gif");
                    }
                }
            }
        }
        return null;
    }

    /**
     * This is a utility method to help in loading standard icon images.
     *
     * @param  resourceName A pathname relative to the directory holding the
     *         class file of the current class
     * @return an image object. May be null if the load failed.
     * @see java.beans.SimpleBeanInfo#loadImage(String)
     */
    @SuppressWarnings("removal")
    private Image loadStandardImage(final String resourceName) {
        return AccessController.doPrivileged(
                (PrivilegedAction<Image>) () -> loadImage(resourceName));
    }

    /**
     * This is a utility method to help in loading standard icon images.
     *
     * @param  resourceName A pathname relative to the directory holding the
     *         class file of the current class
     * @param  suffix A {@code String} containing a file suffix (<i>e.g.</i>,
     *         "Color32.gif" or "Mono32.gif")
     * @return an image object. May be null if the load failed.
     * @see java.beans.SimpleBeanInfo#loadImage(String)
     */
    private Image loadImage(final String resourceName, final String suffix) {
        final String prefix = "/javax/swing/beaninfo/images/";
        final Image image = loadStandardImage(prefix + resourceName + suffix);
        return image == null ? loadStandardImage(prefix + "JComponent" + suffix)
                             : image;
    }

    /**
     * This is a utility method to help in loading icon images. It takes the
     * name of a resource file associated with the current object's class file
     * and loads an image object from that file. Typically images will be GIFs.
     *
     * @param  resourceName A pathname relative to the directory holding the
     *         class file of the current class. For example, "wombat.gif".
     * @return an image object or null if the resource is not found or the
     *         resource could not be loaded as an Image
     */
    public Image loadImage(final String resourceName) {
        try {
            final URL url = getClass().getResource(resourceName);
            if (url != null) {
                final ImageProducer ip = (ImageProducer) url.getContent();
                if (ip != null) {
                    return Toolkit.getDefaultToolkit().createImage(ip);
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }
}
