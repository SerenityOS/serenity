/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.print;

/**
 * Services may optionally provide UIs which allow different styles of
 * interaction in different roles. One role may be end-user browsing and setting
 * of print options. Another role may be administering the print service.
 * <p>
 * Although the Print Service API does not presently provide standardised
 * support for administering a print service, monitoring of the print service is
 * possible and a UI may provide for private update mechanisms.
 * <p>
 * The basic design intent is to allow applications to lazily locate and
 * initialize services only when needed without any API dependencies except in
 * an environment in which they are used.
 * <p>
 * Swing UIs are preferred as they provide a more consistent {@literal L&F} and
 * can support accessibility APIs.
 * <p>
 * Example usage:
 * <pre>
 *  ServiceUIFactory factory = printService.getServiceUIFactory();
 *  if (factory != null) {
 *      JComponent swingui = (JComponent)factory.getUI(
 *                                         ServiceUIFactory.MAIN_UIROLE,
 *                                         ServiceUIFactory.JCOMPONENT_UI);
 *      if (swingui != null) {
 *          tabbedpane.add("Custom UI", swingui);
 *      }
 *  }
 * </pre>
 */
public abstract class ServiceUIFactory {

    /**
     * Constructor for subclasses to call.
     */
    protected ServiceUIFactory() {}

    /**
     * Denotes a UI implemented as a Swing component. The value of the string is
     * the fully qualified classname : "javax.swing.JComponent".
     */
    public static final String JCOMPONENT_UI = "javax.swing.JComponent";

    /**
     * Denotes a UI implemented as an AWT panel. The value of the string is the
     * fully qualified classname : "java.awt.Panel"
     */
    public static final String PANEL_UI = "java.awt.Panel";

    /**
     * Denotes a UI implemented as an AWT dialog. The value of the string is the
     * fully qualified classname : "java.awt.Dialog"
     */
    public static final String DIALOG_UI = "java.awt.Dialog";

    /**
     * Denotes a UI implemented as a Swing dialog. The value of the string is
     * the fully qualified classname : "javax.swing.JDialog"
     */
    public static final String JDIALOG_UI = "javax.swing.JDialog";

    /**
     * Denotes a UI which performs an informative "About" role.
     */
    public static final int ABOUT_UIROLE = 1;

    /**
     * Denotes a UI which performs an administrative role.
     */
    public static final int ADMIN_UIROLE = 2;

    /**
     * Denotes a UI which performs the normal end user role.
     */
    public static final int MAIN_UIROLE = 3;

    /**
     * Not a valid role but role id's greater than this may be used for private
     * roles supported by a service. Knowledge of the function performed by this
     * role is required to make proper use of it.
     */
    public static final int RESERVED_UIROLE = 99;

    /**
     * Get a UI object which may be cast to the requested UI type by the
     * application and used in its user interface.
     *
     * @param  role requested. Must be one of the standard roles or a private
     *         role supported by this factory.
     * @param  ui type in which the role is requested
     * @return the UI role or {@code null} if the requested UI role is not
     *         available from this factory
     * @throws IllegalArgumentException if the role or ui is neither one of the
     *         standard ones, nor a private one supported by the factory
     */
    public abstract Object getUI(int role, String ui) ;

    /**
     * Given a UI role obtained from this factory obtain the UI types available
     * from this factory which implement this role. The returned {@code Strings}
     * should refer to the static variables defined in this class so that
     * applications can use equality of reference ("==").
     *
     * @param  role to be looked up
     * @return the UI types supported by this class for the specified role,
     *         {@code null} if no UIs are available for the role
     * @throws IllegalArgumentException is the role is a non-standard role not
     *         supported by this factory
     */
    public abstract String[] getUIClassNamesForRole(int role) ;

}
