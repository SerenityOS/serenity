/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.util.spi;

import java.util.ResourceBundle;

/**
 * An interface for service providers that provide implementations of {@link
 * java.util.ResourceBundle.Control}. The <a
 * href="../ResourceBundle.html#default_behavior">default resource bundle loading
 * behavior</a> of the {@code ResourceBundle.getBundle} factory methods that take
 * no {@link java.util.ResourceBundle.Control} instance can be modified with {@code
 * ResourceBundleControlProvider} implementations.
 *
 * <p>Provider implementations are loaded from the application's class path
 * using {@link java.util.ServiceLoader} at the first invocation of the
 * {@code ResourceBundle.getBundle} factory method that takes no
 * {@link java.util.ResourceBundle.Control} instance.
 *
 * <p>All {@code ResourceBundleControlProvider}s are ignored in named modules.
 *
 * @author Masayoshi Okutsu
 * @since 1.8
 * @revised 9
 * @see ResourceBundle#getBundle(String, java.util.Locale, ClassLoader, ResourceBundle.Control)
 *      ResourceBundle.getBundle
 * @see java.util.ServiceLoader#load(Class)
 */
public interface ResourceBundleControlProvider {
    /**
     * Returns a {@code ResourceBundle.Control} instance that is used
     * to handle resource bundle loading for the given {@code
     * baseName}. This method must return {@code null} if the given
     * {@code baseName} isn't handled by this provider.
     *
     * @param baseName the base name of the resource bundle
     * @return a {@code ResourceBundle.Control} instance,
     *         or {@code null} if the given {@code baseName} is not
     *         applicable to this provider.
     * @throws NullPointerException if {@code baseName} is {@code null}
     */
    public ResourceBundle.Control getControl(String baseName);
}
