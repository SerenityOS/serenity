/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.*;


/**
 * This class has been obsoleted by the 1.4 focus APIs. While client code may
 * still use this class, developers are strongly encouraged to use
 * <code>java.awt.KeyboardFocusManager</code> and
 * <code>java.awt.DefaultKeyboardFocusManager</code> instead.
 * <p>
 * Please see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
 * How to Use the Focus Subsystem</a>,
 * a section in <em>The Java Tutorial</em>, and the
 * <a href="../../java/awt/doc-files/FocusSpec.html">Focus Specification</a>
 * for more information.
 *
 * @see <a href="../../java/awt/doc-files/FocusSpec.html">Focus Specification</a>
 *
 * @author Arnaud Weber
 * @author David Mendenhall
 * @since 1.2
 */
public abstract class FocusManager extends DefaultKeyboardFocusManager {

    /**
     * This field is obsolete, and its use is discouraged since its
     * specification is incompatible with the 1.4 focus APIs.
     * The current FocusManager is no longer a property of the UI.
     * Client code must query for the current FocusManager using
     * <code>KeyboardFocusManager.getCurrentKeyboardFocusManager()</code>.
     * See the Focus Specification for more information.
     *
     * @see java.awt.KeyboardFocusManager#getCurrentKeyboardFocusManager
     * @see <a href="../../java/awt/doc-files/FocusSpec.html">Focus Specification</a>
     */
    public static final String FOCUS_MANAGER_CLASS_PROPERTY =
        "FocusManagerClassName";

    private static boolean enabled = true;

    /**
     * Constructor for subclasses to call.
     */
    protected FocusManager() {}

    /**
     * Returns the current <code>KeyboardFocusManager</code> instance
     * for the calling thread's context.
     *
     * @return this thread's context's <code>KeyboardFocusManager</code>
     * @see #setCurrentManager
     */
    public static FocusManager getCurrentManager() {
        KeyboardFocusManager manager =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (manager instanceof FocusManager) {
            return (FocusManager)manager;
        } else {
            return new DelegatingDefaultFocusManager(manager);
        }
    }

    /**
     * Sets the current <code>KeyboardFocusManager</code> instance
     * for the calling thread's context. If <code>null</code> is
     * specified, then the current <code>KeyboardFocusManager</code>
     * is replaced with a new instance of
     * <code>DefaultKeyboardFocusManager</code>.
     * <p>
     * If a <code>SecurityManager</code> is installed,
     * the calling thread must be granted the <code>AWTPermission</code>
     * "replaceKeyboardFocusManager" in order to replace the
     * the current <code>KeyboardFocusManager</code>.
     * If this permission is not granted,
     * this method will throw a <code>SecurityException</code>,
     * and the current <code>KeyboardFocusManager</code> will be unchanged.
     *
     * @param aFocusManager the new <code>KeyboardFocusManager</code>
     *     for this thread's context
     * @see #getCurrentManager
     * @see java.awt.DefaultKeyboardFocusManager
     * @throws SecurityException if the calling thread does not have permission
     *         to replace the current <code>KeyboardFocusManager</code>
     */
    public static void setCurrentManager(FocusManager aFocusManager)
        throws SecurityException
    {
        // Note: This method is not backward-compatible with 1.3 and earlier
        // releases. It now throws a SecurityException in an applet, whereas
        // in previous releases, it did not. This issue was discussed at
        // length, and ultimately approved by Hans.
        KeyboardFocusManager toSet =
            (aFocusManager instanceof DelegatingDefaultFocusManager)
                ? ((DelegatingDefaultFocusManager)aFocusManager).getDelegate()
                : aFocusManager;
        KeyboardFocusManager.setCurrentKeyboardFocusManager(toSet);
    }

    /**
     * Changes the current <code>KeyboardFocusManager</code>'s default
     * <code>FocusTraversalPolicy</code> to
     * <code>DefaultFocusTraversalPolicy</code>.
     *
     * @see java.awt.DefaultFocusTraversalPolicy
     * @see java.awt.KeyboardFocusManager#setDefaultFocusTraversalPolicy
     * @deprecated as of 1.4, replaced by
     * <code>KeyboardFocusManager.setDefaultFocusTraversalPolicy(FocusTraversalPolicy)</code>
     */
    @Deprecated
    public static void disableSwingFocusManager() {
        if (enabled) {
            enabled = false;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setDefaultFocusTraversalPolicy(
                    new DefaultFocusTraversalPolicy());
        }
    }

    /**
     * Returns whether the application has invoked
     * <code>disableSwingFocusManager()</code>.
     *
     * @return {@code true} if focus manager is enabled.
     * @see #disableSwingFocusManager
     * @deprecated As of 1.4, replaced by
     *   <code>KeyboardFocusManager.getDefaultFocusTraversalPolicy()</code>
     */
    @Deprecated
    public static boolean isFocusManagerEnabled() {
        return enabled;
    }
}
