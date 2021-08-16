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

package java.awt;

import java.awt.peer.TaskbarPeer;
import sun.awt.SunToolkit;

/**
 * The {@code Taskbar} class allows a Java application to interact with
 * the system task area (taskbar, Dock, etc.).
 *
 * <p>
 * There are a variety of interactions depending on the current platform such as
 * displaying progress of some task, appending user-specified menu to the application
 * icon context menu, etc.
 *
 * @implNote Linux support is currently limited to Unity. However to make these
 * features work on Unity, the app should be run from a .desktop file with
 * specified {@code java.desktop.appName} system property set to this .desktop
 * file name:
 * {@code Exec=java -Djava.desktop.appName=MyApp.desktop -jar /path/to/myapp.jar}
 * see <a href="https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles">
 * https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles</a>
 *
 * @since 9
 */

public class Taskbar {

    /**
     * List of provided features. Each platform supports a different
     * set of features.  You may use the {@link Taskbar#isSupported}
     * method to determine if the given feature is supported by the
     * current platform.
     */
    public static enum Feature {

        /**
         * Represents a textual icon badge feature.
         * @see #setIconBadge(java.lang.String)
         */
        ICON_BADGE_TEXT,

        /**
         * Represents a numerical icon badge feature.
         * @see #setIconBadge(java.lang.String)
         */
        ICON_BADGE_NUMBER,

        /**
         * Represents a graphical icon badge feature for a window.
         * @see #setWindowIconBadge(java.awt.Window, java.awt.Image)
         */
        ICON_BADGE_IMAGE_WINDOW,

        /**
         * Represents an icon feature.
         * @see #setIconImage(java.awt.Image)
         */
        ICON_IMAGE,

        /**
         * Represents a menu feature.
         * @see #setMenu(java.awt.PopupMenu)
         * @see #getMenu()
         */
        MENU,

        /**
         * Represents a progress state feature for a specified window.
         * @see #setWindowProgressState(java.awt.Window, State)
         */
        PROGRESS_STATE_WINDOW,

        /**
         * Represents a progress value feature.
         * @see #setProgressValue(int)
         */
        PROGRESS_VALUE,

        /**
         * Represents a progress value feature for a specified window.
         * @see #setWindowProgressValue(java.awt.Window, int)
         */
        PROGRESS_VALUE_WINDOW,

        /**
         * Represents a user attention request feature.
         * @see #requestUserAttention(boolean, boolean)
         */
        USER_ATTENTION,

        /**
         * Represents a user attention request feature for a specified window.
         * @see #requestWindowUserAttention(java.awt.Window)
         */
        USER_ATTENTION_WINDOW
    }

    /**
     * Kinds of available window progress states.
     *
     * @see #setWindowProgressState(java.awt.Window, java.awt.Taskbar.State)
     */
    public static enum State {
        /**
         * Stops displaying the progress.
         */
        OFF,
        /**
         * The progress indicator displays with normal color and determinate
         * mode.
         */
        NORMAL,
        /**
         * Shows progress as paused, progress can be resumed by the user.
         * Switches to the determinate display.
         */
        PAUSED,
        /**
         * The progress indicator displays activity without specifying what
         * proportion of the progress is complete.
         */
        INDETERMINATE,
        /**
         * Shows that an error has occurred. Switches to the determinate
         * display.
         */
        ERROR
    }

    private TaskbarPeer peer;

    /**
     * Tests whether a {@code Feature} is supported on the current platform.
     * @param feature the specified {@link Feature}
     * @return true if the specified feature is supported on the current platform
     */
    public boolean isSupported(Feature feature) {
        return peer.isSupported(feature);
    }

    /**
     * Checks if the feature type is supported.
     *
     * @param featureType the action type in question
     * @throws UnsupportedOperationException if the specified action type is not
     *         supported on the current platform
     */
    private void checkFeatureSupport(Feature featureType){
        if (!isSupported(featureType)) {
            throw new UnsupportedOperationException("The " + featureType.name()
                    + " feature is not supported on the current platform!");
        }
    }

    /**
     *  Calls to the security manager's {@code checkPermission} method with
     *  an {@code RuntimePermission("canProcessApplicationEvents")} permissions.
     */
    private void checkEventsProcessingPermission(){
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission(
                    "canProcessApplicationEvents"));
        }
    }

    private Taskbar() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        if (defaultToolkit instanceof SunToolkit) {
            peer = ((SunToolkit) defaultToolkit).createTaskbarPeer(this);
        }
    }

    /**
     * Returns the {@code Taskbar} instance of the current
     * taskbar context.  On some platforms the Taskbar API may not be
     * supported; use the {@link #isTaskbarSupported} method to
     * determine if the current taskbar is supported.
     * @return the Taskbar instance
     * @throws HeadlessException if {@link
     * GraphicsEnvironment#isHeadless()} returns {@code true}
     * @throws UnsupportedOperationException if this class is not
     * supported on the current platform
     * @see #isTaskbarSupported()
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static synchronized Taskbar getTaskbar(){
        if (GraphicsEnvironment.isHeadless()) throw new HeadlessException();

        if (!Taskbar.isTaskbarSupported()) {
            throw new UnsupportedOperationException("Taskbar API is not " +
                                                    "supported on the current platform");
        }

        sun.awt.AppContext context = sun.awt.AppContext.getAppContext();
        Taskbar taskbar = (Taskbar)context.get(Taskbar.class);

        if (taskbar == null) {
            taskbar = new Taskbar();
            context.put(Taskbar.class, taskbar);
        }

        return taskbar;
    }

    /**
     * Tests whether this class is supported on the current platform.
     * If it's supported, use {@link #getTaskbar()} to retrieve an
     * instance.
     *
     * @return {@code true} if this class is supported on the
     *         current platform; {@code false} otherwise
     * @see #getTaskbar()
     */
    public static boolean isTaskbarSupported(){
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        if (defaultToolkit instanceof SunToolkit) {
            return ((SunToolkit)defaultToolkit).isTaskbarSupported();
        }
        return false;
    }

    /**
     * Requests user attention to this application.
     *
     * Depending on the platform, this may be visually indicated by a bouncing
     * or flashing icon in the task area. It may have no effect on an already active
     * application.
     *
     * On some platforms (e.g. Mac OS) this effect may disappear upon app activation
     * and cannot be dismissed by setting {@code enabled} to false.
     * Other platforms may require an additional call
     * {@link #requestUserAttention} to dismiss this request
     * with {@code enabled} parameter set to false.
     *
     * @param enabled disables this request if false
     * @param critical if this is an important request
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#USER_ATTENTION} feature
     */
    public void requestUserAttention(final boolean enabled, final boolean critical) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.USER_ATTENTION);
        peer.requestUserAttention(enabled, critical);
    }

    /**
     * Requests user attention to the specified window.
     *
     * Has no effect if a window representation is not displayable in
     * the task area. Whether it is displayable is dependent on all
     * of window type, platform, and implementation.
     *
     * @param w window
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#USER_ATTENTION_WINDOW} feature
     */
    public void requestWindowUserAttention(Window w) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.USER_ATTENTION_WINDOW);
        peer.requestWindowUserAttention(w);
    }

    /**
     * Attaches the contents of the provided PopupMenu to the application icon
     * in the task area.
     *
     * @param menu the PopupMenu to attach to this application
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#MENU} feature
     */
    public void setMenu(final PopupMenu menu) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.MENU);
        peer.setMenu(menu);
    }

    /**
     * Gets PopupMenu used to add items to this application's icon in system task area.
     *
     * @return the PopupMenu
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#MENU} feature
     */
    public PopupMenu getMenu() {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.MENU);
        return peer.getMenu();
    }

    /**
     * Requests the system to change this application's icon to the provided {@code image}.
     *
     * @param image to change
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#ICON_IMAGE} feature
     */
    public void setIconImage(final Image image) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.ICON_IMAGE);
        peer.setIconImage(image);
    }

    /**
     * Obtains an image of this application's icon.
     *
     * @apiNote The returned icon image may not be equal
     * to an image set by {@link java.awt.Taskbar#setIconImage},
     * but should be visually similar.
     *
     * @return an image of this application's icon
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#ICON_IMAGE} feature
     */
    public Image getIconImage() {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.ICON_IMAGE);
        return peer.getIconImage();
    }

    /**
     * Affixes a small system-provided badge to this application's icon.
     * Usually a number.
     *
     * Some platforms do not support string values and accept only integer
     * values. In this case, pass an integer represented as a string as parameter.
     * This can be tested by {@code Feature.ICON_BADGE_TEXT} and
     * {@code Feature.ICON_BADGE_NUMBER}.
     *
     * Passing {@code null} as parameter hides the badge.
     * @param badge label to affix to the icon
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#ICON_BADGE_NUMBER}
     * or {@link Taskbar.Feature#ICON_BADGE_TEXT} feature
     */
    public void setIconBadge(final String badge) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.ICON_BADGE_NUMBER);
        peer.setIconBadge(badge);
    }

    /**
     * Affixes a small badge to this application's icon in the task area
     * for the specified window.
     * It may be disabled by system settings.
     *
     * Has no effect if a window representation is not displayable in
     * the task area. Whether it is displayable is dependent on all
     * of window type, platform, and implementation.
     *
     * @param w window to update
     * @param badge image to affix to the icon
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#ICON_BADGE_IMAGE_WINDOW} feature
     */
    public void setWindowIconBadge(Window w, final Image badge) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.ICON_BADGE_IMAGE_WINDOW);
        if (w != null) {
            peer.setWindowIconBadge(w, badge);
        }
    }


    /**
     * Affixes a small system-provided progress bar to this application's icon.
     *
     * @param value from 0 to 100, other to disable progress indication
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#PROGRESS_VALUE} feature
     */
    public void setProgressValue(int value) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.PROGRESS_VALUE);
        peer.setProgressValue(value);
    }

    /**
     * Displays a determinate progress bar in the task area for the specified
     * window.
     *
     * Has no effect if a window representation is not displayable in
     * the task area. Whether it is displayable is dependent on all
     * of window type, platform, and implementation.
     *
     * <br>
     * The visual behavior is platform and {@link State} dependent.
     * <br>
     * This call cancels the {@link State#INDETERMINATE INDETERMINATE} state
     * of the window.
     * <br>
     * Note that when multiple windows is grouped in the task area
     * the behavior is platform specific.
     *
     * @param w window to update
     * @param value from 0 to 100, other to switch to {@link State#OFF} state
     *              and disable progress indication
     * @see #setWindowProgressState(java.awt.Window, State)
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#PROGRESS_VALUE_WINDOW} feature
     */
    public void setWindowProgressValue(Window w, int value) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.PROGRESS_VALUE_WINDOW);
        if (w != null) {
            peer.setWindowProgressValue(w, value);
        }
    }

    /**
     * Sets a progress state for a specified window.
     *
     * Has no effect if a window representation is not displayable in
     * the task area. Whether it is displayable is dependent on all
     * of window type, platform, and implementation.
     * <br>
     * Each state displays a progress in a platform-dependent way.
     * <br>
     * Note than switching from {@link State#INDETERMINATE INDETERMINATE} state
     * to any of determinate states may reset value set by
     * {@link #setWindowProgressValue(java.awt.Window, int) setWindowProgressValue}
     *
     * @param w window
     * @param state to change to
     * @see State#OFF
     * @see State#NORMAL
     * @see State#PAUSED
     * @see State#ERROR
     * @see State#INDETERMINATE
     * @see #setWindowProgressValue(java.awt.Window, int)
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Taskbar.Feature#PROGRESS_STATE_WINDOW} feature
     */
    public void setWindowProgressState(Window w, State state) {
        checkEventsProcessingPermission();
        checkFeatureSupport(Feature.PROGRESS_STATE_WINDOW);
        if (w != null) {
            peer.setWindowProgressState(w, state);
        }
    }
}
