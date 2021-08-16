/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.applet.Applet;

import java.beans.beancontext.BeanContext;

/**
 * This interface is designed to work in collusion with java.beans.Beans.instantiate.
 * The interface is intended to provide mechanism to allow the proper
 * initialization of JavaBeans that are also Applets, during their
 * instantiation by java.beans.Beans.instantiate().
 *
 * @see java.beans.Beans#instantiate
 *
 * @since 1.2
 *
 * @deprecated The Applet API is deprecated. See the
 * <a href="../applet/package-summary.html"> java.applet package
 * documentation</a> for further information.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletInitializer {

    /**
     * <p>
     * If passed to the appropriate variant of java.beans.Beans.instantiate
     * this method will be called in order to associate the newly instantiated
     * Applet (JavaBean) with its AppletContext, AppletStub, and Container.
     * </p>
     * <p>
     * Conformant implementations shall:
     * <ol>
     * <li> Associate the newly instantiated Applet with the appropriate
     * AppletContext.
     *
     * <li> Instantiate an AppletStub() and associate that AppletStub with
     * the Applet via an invocation of setStub().
     *
     * <li> If BeanContext parameter is null, then it shall associate the
     * Applet with its appropriate Container by adding that Applet to its
     * Container via an invocation of add(). If the BeanContext parameter is
     * non-null, then it is the responsibility of the BeanContext to associate
     * the Applet with its Container during the subsequent invocation of its
     * addChildren() method.
     * </ol>
     *
     * @param newAppletBean  The newly instantiated JavaBean
     * @param bCtxt          The BeanContext intended for this Applet, or
     *                       null.
     */
    @SuppressWarnings("removal")
    void initialize(Applet newAppletBean, BeanContext bCtxt);

    /**
     * <p>
     * Activate, and/or mark Applet active. Implementors of this interface
     * shall mark this Applet as active, and optionally invoke its start()
     * method.
     * </p>
     *
     * @param newApplet  The newly instantiated JavaBean
     */
    @SuppressWarnings("removal")
    void activate(Applet newApplet);
}
