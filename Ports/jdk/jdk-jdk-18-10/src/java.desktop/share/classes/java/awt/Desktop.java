/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.OpenURIHandler;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PrintFilesHandler;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitStrategy;
import java.awt.desktop.SystemEventListener;
import java.awt.peer.DesktopPeer;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;

import javax.swing.JMenuBar;

import sun.awt.SunToolkit;
import sun.security.util.SecurityConstants;

/**
 * The {@code Desktop} class allows interact with various desktop capabilities.
 *
 * <p> Supported operations include:
 * <ul>
 *   <li>launching the user-default browser to show a specified
 *       URI;</li>
 *   <li>launching the user-default mail client with an optional
 *       {@code mailto} URI;</li>
 *   <li>launching a registered application to open, edit or print a
 *       specified file.</li>
 * </ul>
 *
 * <p> This class provides methods corresponding to these
 * operations. The methods look for the associated application
 * registered on the current platform, and launch it to handle a URI
 * or file. If there is no associated application or the associated
 * application fails to be launched, an exception is thrown.
 *
 * Please see {@link Desktop.Action} for the full list of supported operations
 * and capabilities.
 *
 * <p> An application is registered to a URI or file type.
 * The mechanism of registering, accessing, and
 * launching the associated application is platform-dependent.
 *
 * <p> Each operation is an action type represented by the {@link
 * Desktop.Action} class.
 *
 * <p> Note: when some action is invoked and the associated
 * application is executed, it will be executed on the same system as
 * the one on which the Java application was launched.
 *
 * @see Action
 *
 * @since 1.6
 * @author Armin Chen
 * @author George Zhang
 */
public class Desktop {

    /**
     * Represents an action type.  Each platform supports a different
     * set of actions.  You may use the {@link Desktop#isSupported}
     * method to determine if the given action is supported by the
     * current platform.
     * @see java.awt.Desktop#isSupported(java.awt.Desktop.Action)
     * @since 1.6
     */
    public static enum Action {
        /**
         * Represents an "open" action.
         * @see Desktop#open(java.io.File)
         */
        OPEN,
        /**
         * Represents an "edit" action.
         * @see Desktop#edit(java.io.File)
         */
        EDIT,
        /**
         * Represents a "print" action.
         * @see Desktop#print(java.io.File)
         */
        PRINT,
        /**
         * Represents a "mail" action.
         * @see Desktop#mail()
         * @see Desktop#mail(java.net.URI)
         */
        MAIL,

        /**
         * Represents a "browse" action.
         * @see Desktop#browse(java.net.URI)
         */
        BROWSE,

        /**
         * Represents an AppForegroundListener
         * @see java.awt.desktop.AppForegroundListener
         * @since 9
         */
        APP_EVENT_FOREGROUND,

        /**
         * Represents an AppHiddenListener
         * @see java.awt.desktop.AppHiddenListener
         * @since 9
         */
        APP_EVENT_HIDDEN,

        /**
         * Represents an AppReopenedListener
         * @see java.awt.desktop.AppReopenedListener
         * @since 9
         */
        APP_EVENT_REOPENED,

        /**
         * Represents a ScreenSleepListener
         * @see java.awt.desktop.ScreenSleepListener
         * @since 9
         */
        APP_EVENT_SCREEN_SLEEP,

        /**
         * Represents a SystemSleepListener
         * @see java.awt.desktop.SystemSleepListener
         * @since 9
         */
        APP_EVENT_SYSTEM_SLEEP,

        /**
         * Represents a UserSessionListener
         * @see java.awt.desktop.UserSessionListener
         * @since 9
         */
        APP_EVENT_USER_SESSION,

        /**
         * Represents an AboutHandler
         * @see #setAboutHandler(java.awt.desktop.AboutHandler)
         * @since 9
         */
        APP_ABOUT,

        /**
         * Represents a PreferencesHandler
         * @see #setPreferencesHandler(java.awt.desktop.PreferencesHandler)
         * @since 9
         */
        APP_PREFERENCES,

        /**
         * Represents an OpenFilesHandler
         * @see #setOpenFileHandler(java.awt.desktop.OpenFilesHandler)
         * @since 9
         */
        APP_OPEN_FILE,

        /**
         * Represents a PrintFilesHandler
         * @see #setPrintFileHandler(java.awt.desktop.PrintFilesHandler)
         * @since 9
         */
        APP_PRINT_FILE,

        /**
         * Represents an OpenURIHandler
         * @see #setOpenURIHandler(java.awt.desktop.OpenURIHandler)
         * @since 9
         */
        APP_OPEN_URI,

        /**
         * Represents a QuitHandler
         * @see #setQuitHandler(java.awt.desktop.QuitHandler)
         * @since 9
         */
        APP_QUIT_HANDLER,

        /**
         * Represents a QuitStrategy
         * @see #setQuitStrategy(java.awt.desktop.QuitStrategy)
         * @since 9
         */
        APP_QUIT_STRATEGY,

        /**
         * Represents a SuddenTermination
         * @see #enableSuddenTermination()
         * @since 9
         */
        APP_SUDDEN_TERMINATION,

        /**
         * Represents a requestForeground
         * @see #requestForeground(boolean)
         * @since 9
         */
        APP_REQUEST_FOREGROUND,

        /**
         * Represents a HelpViewer
         * @see #openHelpViewer()
         * @since 9
         */
        APP_HELP_VIEWER,

        /**
         * Represents a menu bar
         * @see #setDefaultMenuBar(javax.swing.JMenuBar)
         * @since 9
         */
        APP_MENU_BAR,

        /**
         * Represents a browse file directory
         * @see #browseFileDirectory(java.io.File)
         * @since 9
         */
        BROWSE_FILE_DIR,

        /**
         * Represents a move to trash
         * @see #moveToTrash(java.io.File)
         * @since 9
         */
        MOVE_TO_TRASH
    };

    private DesktopPeer peer;

    /**
     * Suppresses default constructor for noninstantiability.
     */
    private Desktop() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        // same cast as in isDesktopSupported()
        if (defaultToolkit instanceof SunToolkit) {
            peer = ((SunToolkit) defaultToolkit).createDesktopPeer(this);
        }
    }

    private void checkEventsProcessingPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission(
                    "canProcessApplicationEvents"));
        }
    }

    /**
     * Returns the {@code Desktop} instance of the current
     * desktop context. On some platforms the Desktop API may not be
     * supported; use the {@link #isDesktopSupported} method to
     * determine if the current desktop is supported.
     * @return the Desktop instance
     * @throws HeadlessException if {@link
     * GraphicsEnvironment#isHeadless()} returns {@code true}
     * @throws UnsupportedOperationException if this class is not
     * supported on the current platform
     * @see #isDesktopSupported()
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static synchronized Desktop getDesktop(){
        if (GraphicsEnvironment.isHeadless()) throw new HeadlessException();
        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop API is not " +
                                                    "supported on the current platform");
        }

        sun.awt.AppContext context = sun.awt.AppContext.getAppContext();
        Desktop desktop = (Desktop)context.get(Desktop.class);

        if (desktop == null) {
            desktop = new Desktop();
            context.put(Desktop.class, desktop);
        }

        return desktop;
    }

    /**
     * Tests whether this class is supported on the current platform.
     * If it's supported, use {@link #getDesktop()} to retrieve an
     * instance.
     *
     * @return {@code true} if this class is supported on the
     *         current platform; {@code false} otherwise
     * @see #getDesktop()
     */
    public static boolean isDesktopSupported(){
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        if (defaultToolkit instanceof SunToolkit) {
            return ((SunToolkit)defaultToolkit).isDesktopSupported();
        }
        return false;
    }

    /**
     * Tests whether an action is supported on the current platform.
     *
     * <p>Even when the platform supports an action, a file or URI may
     * not have a registered application for the action.  For example,
     * most of the platforms support the {@link Desktop.Action#OPEN}
     * action.  But for a specific file, there may not be an
     * application registered to open it.  In this case, {@link
     * #isSupported(Action)} may return {@code true}, but the corresponding
     * action method will throw an {@link IOException}.
     *
     * @param action the specified {@link Action}
     * @return {@code true} if the specified action is supported on
     *         the current platform; {@code false} otherwise
     * @see Desktop.Action
     */
    public boolean isSupported(Action action) {
        return peer.isSupported(action);
    }

    /**
     * Checks if the file is a valid file and readable.
     *
     * @throws SecurityException If a security manager exists and its
     *         {@link SecurityManager#checkRead(java.lang.String)} method
     *         denies read access to the file
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if file doesn't exist
     */
    private static void checkFileValidation(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("The file: "
                    + file.getPath() + " doesn't exist.");
        }
    }

    /**
     * Checks if the action type is supported.
     *
     * @param actionType the action type in question
     * @throws UnsupportedOperationException if the specified action type is not
     *         supported on the current platform
     */
    private void checkActionSupport(Action actionType){
        if (!isSupported(actionType)) {
            throw new UnsupportedOperationException("The " + actionType.name()
                    + " action is not supported on the current platform!");
        }
    }


    /**
     * Calls to the security manager's {@code checkPermission} method with an
     * {@code AWTPermission("showWindowWithoutWarningBanner")} permission. This
     * permission is needed, because we cannot add a security warning icon to
     * the windows of the external native application.
     */
    private void checkAWTPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AWTPermission(
                    "showWindowWithoutWarningBanner"));
        }
    }

    /**
     * Launches the associated application to open the file.
     *
     * <p> If the specified file is a directory, the file manager of
     * the current platform is launched to open it.
     *
     * @param file the file to be opened with the associated application
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalArgumentException if the specified file doesn't
     * exist
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#OPEN} action
     * @throws IOException if the specified file has no associated
     * application or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * {@link java.lang.SecurityManager#checkRead(java.lang.String)}
     * method denies read access to the file, or it denies the
     * {@code AWTPermission("showWindowWithoutWarningBanner")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @see java.awt.AWTPermission
     */
    public void open(File file) throws IOException {
        file = new File(file.getPath());
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.OPEN);
        checkFileValidation(file);

        peer.open(file);
    }

    /**
     * Launches the associated editor application and opens a file for
     * editing.
     *
     * @param file the file to be opened for editing
     * @throws NullPointerException if the specified file is {@code null}
     * @throws IllegalArgumentException if the specified file doesn't
     * exist
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#EDIT} action
     * @throws IOException if the specified file has no associated
     * editor, or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * {@link java.lang.SecurityManager#checkRead(java.lang.String)}
     * method denies read access to the file, or {@link
     * java.lang.SecurityManager#checkWrite(java.lang.String)} method
     * denies write access to the file, or it denies the
     * {@code AWTPermission("showWindowWithoutWarningBanner")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @see java.awt.AWTPermission
     */
    public void edit(File file) throws IOException {
        file = new File(file.getPath());
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.EDIT);
        file.canWrite();
        checkFileValidation(file);
        if (file.isDirectory()) {
            throw new IOException(file.getPath() + " is a directory");
        }
        peer.edit(file);
    }

    /**
     * Prints a file with the native desktop printing facility, using
     * the associated application's print command.
     *
     * @param file the file to be printed
     * @throws NullPointerException if the specified file is {@code
     * null}
     * @throws IllegalArgumentException if the specified file doesn't
     * exist
     * @throws UnsupportedOperationException if the current platform
     *         does not support the {@link Desktop.Action#PRINT} action
     * @throws IOException if the specified file has no associated
     * application that can be used to print it
     * @throws SecurityException if a security manager exists and its
     * {@link java.lang.SecurityManager#checkRead(java.lang.String)}
     * method denies read access to the file, or its {@link
     * java.lang.SecurityManager#checkPrintJobAccess()} method denies
     * the permission to print the file, or the calling thread is not
     * allowed to create a subprocess
     */
    public void print(File file) throws IOException {
        file = new File(file.getPath());
        checkExec();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPrintJobAccess();
        }
        checkActionSupport(Action.PRINT);
        checkFileValidation(file);
        if (file.isDirectory()) {
            throw new IOException(file.getPath() + " is a directory");
        }
        peer.print(file);
    }

    /**
     * Launches the default browser to display a {@code URI}.
     * If the default browser is not able to handle the specified
     * {@code URI}, the application registered for handling
     * {@code URIs} of the specified type is invoked. The application
     * is determined from the protocol and path of the {@code URI}, as
     * defined by the {@code URI} class.
     *
     * @param uri the URI to be displayed in the user default browser
     * @throws NullPointerException if {@code uri} is {@code null}
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#BROWSE} action
     * @throws IOException if the user default browser is not found,
     * or it fails to be launched, or the default handler application
     * failed to be launched
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code AWTPermission("showWindowWithoutWarningBanner")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @see java.net.URI
     * @see java.awt.AWTPermission
     */
    public void browse(URI uri) throws IOException {
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.BROWSE);
        Objects.requireNonNull(uri);
        peer.browse(uri);
    }

    /**
     * Launches the mail composing window of the user default mail
     * client.
     *
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#MAIL} action
     * @throws IOException if the user default mail client is not
     * found, or it fails to be launched
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code AWTPermission("showWindowWithoutWarningBanner")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @see java.awt.AWTPermission
     */
    public void mail() throws IOException {
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.MAIL);
        URI mailtoURI = null;
        try{
            mailtoURI = new URI("mailto:?");
            peer.mail(mailtoURI);
        } catch (URISyntaxException e){
            // won't reach here.
        }
    }

    /**
     * Launches the mail composing window of the user default mail
     * client, filling the message fields specified by a {@code
     * mailto:} URI.
     *
     * <p> A {@code mailto:} URI can specify message fields
     * including <i>"to"</i>, <i>"cc"</i>, <i>"subject"</i>,
     * <i>"body"</i>, etc.  See <a
     * href="http://www.ietf.org/rfc/rfc2368.txt">The mailto URL
     * scheme (RFC 2368)</a> for the {@code mailto:} URI specification
     * details.
     *
     * @param mailtoURI the specified {@code mailto:} URI
     * @throws NullPointerException if the specified URI is {@code
     * null}
     * @throws IllegalArgumentException if the URI scheme is not
     *         {@code "mailto"}
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#MAIL} action
     * @throws IOException if the user default mail client is not
     * found or fails to be launched
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code AWTPermission("showWindowWithoutWarningBanner")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @see java.net.URI
     * @see java.awt.AWTPermission
     */
    public  void mail(URI mailtoURI) throws IOException {
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.MAIL);
        if (mailtoURI == null) throw new NullPointerException();

        if (!"mailto".equalsIgnoreCase(mailtoURI.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not \"mailto\"");
        }

        peer.mail(mailtoURI);
    }

    private void checkExec() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new FilePermission("<<ALL FILES>>",
                    SecurityConstants.FILE_EXECUTE_ACTION));
        }
    }

    private void checkRead() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new FilePermission("<<ALL FILES>>",
                    SecurityConstants.FILE_READ_ACTION));
        }
    }

    private void checkQuitPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkExit(0);
        }
    }

    /**
     * Adds sub-types of {@link SystemEventListener} to listen for notifications
     * from the native system.
     *
     * Has no effect if SystemEventListener's sub-type is unsupported on the current
     * platform.
     *
     * @param listener listener
     *
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code RuntimePermission("canProcessApplicationEvents")}
     * permission
     *
     * @see java.awt.desktop.AppForegroundListener
     * @see java.awt.desktop.AppHiddenListener
     * @see java.awt.desktop.AppReopenedListener
     * @see java.awt.desktop.ScreenSleepListener
     * @see java.awt.desktop.SystemSleepListener
     * @see java.awt.desktop.UserSessionListener
     * @since 9
     */
    public void addAppEventListener(final SystemEventListener listener) {
        checkEventsProcessingPermission();
        peer.addAppEventListener(listener);
    }

    /**
     * Removes sub-types of {@link SystemEventListener} to listen for notifications
     * from the native system.
     *
     * Has no effect if SystemEventListener's sub-type is unsupported on  the current
     * platform.
     *
     * @param listener listener
     *
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code RuntimePermission("canProcessApplicationEvents")}
     * permission
     *
     * @see java.awt.desktop.AppForegroundListener
     * @see java.awt.desktop.AppHiddenListener
     * @see java.awt.desktop.AppReopenedListener
     * @see java.awt.desktop.ScreenSleepListener
     * @see java.awt.desktop.SystemSleepListener
     * @see java.awt.desktop.UserSessionListener
     * @since 9
     */
    public void removeAppEventListener(final SystemEventListener listener) {
        checkEventsProcessingPermission();
        peer.removeAppEventListener(listener);
    }

    /**
     * Installs a handler to show a custom About window for your application.
     * <p>
     * Setting the {@link java.awt.desktop.AboutHandler} to {@code null} reverts it to the
     * default behavior.
     *
     * @param aboutHandler the handler to respond to the
     * {@link java.awt.desktop.AboutHandler#handleAbout(AboutEvent)} message
     *
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code RuntimePermission("canProcessApplicationEvents")}
     * permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_ABOUT} action
     *
     * @since 9
     */
    public void setAboutHandler(final AboutHandler aboutHandler) {
        checkEventsProcessingPermission();
        checkActionSupport(Action.APP_ABOUT);
        peer.setAboutHandler(aboutHandler);
    }

    /**
     * Installs a handler to show a custom Preferences window for your
     * application.
     * <p>
     * Setting the {@link PreferencesHandler} to {@code null} reverts it to
     * the default behavior
     *
     * @param preferencesHandler the handler to respond to the
     * {@link PreferencesHandler#handlePreferences(PreferencesEvent)}
     *
     * @throws SecurityException if a security manager exists and it
     * denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_PREFERENCES} action
     * @since 9
     */
    public void setPreferencesHandler(final PreferencesHandler preferencesHandler) {
        checkEventsProcessingPermission();
        checkActionSupport(Action.APP_PREFERENCES);
        peer.setPreferencesHandler(preferencesHandler);
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * open a list of files.
     *
     * @implNote Please note that for macOS, notifications
     * are only sent if the Java app is a bundled application,
     * with a {@code CFBundleDocumentTypes} array present in its
     * {@code Info.plist}. Check the
     * <a href="https://developer.apple.com/documentation">
     * Apple Developer Documentation</a> for more information about
     * {@code Info.plist}.
     *
     * @param openFileHandler handler
     *
     * @throws SecurityException if a security manager exists and its
     * {@link java.lang.SecurityManager#checkRead(java.lang.String)}
     * method denies read access to the files, or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_OPEN_FILE} action
     * @since 9
     */
    public void setOpenFileHandler(final OpenFilesHandler openFileHandler) {
        checkEventsProcessingPermission();
        checkExec();
        checkRead();
        checkActionSupport(Action.APP_OPEN_FILE);
        peer.setOpenFileHandler(openFileHandler);
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * print a list of files.
     *
     * @implNote Please note that for macOS, notifications
     * are only sent if the Java app is a bundled application,
     * with a {@code CFBundleDocumentTypes} array present in its
     * {@code Info.plist}. Check the
     * <a href="https://developer.apple.com/documentation">
     * Apple Developer Documentation</a> for more information about
     * {@code Info.plist}.
     *
     * @param printFileHandler handler
     * @throws SecurityException if a security manager exists and its
     * {@link java.lang.SecurityManager#checkPrintJobAccess()} method denies
     * the permission to print or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_PRINT_FILE} action
     * @since 9
     */
    public void setPrintFileHandler(final PrintFilesHandler printFileHandler) {
        checkEventsProcessingPermission();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPrintJobAccess();
        }
        checkActionSupport(Action.APP_PRINT_FILE);
        peer.setPrintFileHandler(printFileHandler);
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * open a URL.
     *
     * Setting the handler to {@code null} causes all
     * {@link OpenURIHandler#openURI(OpenURIEvent)} requests to be
     * enqueued until another handler is set.
     *
     * @implNote Please note that for macOS, notifications
     * are only sent if the Java app is a bundled application,
     * with a {@code CFBundleDocumentTypes} array present in its
     * {@code Info.plist}. Check the
     * <a href="https://developer.apple.com/documentation">
     * Apple Developer Documentation</a> for more information about
     * {@code Info.plist}.
     *
     * @param openURIHandler handler
     *
     * {@code RuntimePermission("canProcessApplicationEvents")}
     * permission, or the calling thread is not allowed to create a
     * subprocess
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_OPEN_URI} action
     * @since 9
     */
    public void setOpenURIHandler(final OpenURIHandler openURIHandler) {
        checkEventsProcessingPermission();
        checkExec();
        checkActionSupport(Action.APP_OPEN_URI);
        peer.setOpenURIHandler(openURIHandler);
    }

    /**
     * Installs the handler which determines if the application should quit. The
     * handler is passed a one-shot {@link java.awt.desktop.QuitResponse} which can cancel or
     * proceed with the quit. Setting the handler to {@code null} causes
     * all quit requests to directly perform the default {@link QuitStrategy}.
     *
     * @param quitHandler the handler that is called when the application is
     * asked to quit
     *
     * @throws SecurityException if a security manager exists and it
     * will not allow the caller to invoke {@code System.exit} or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_QUIT_HANDLER} action
     * @since 9
     */
    public void setQuitHandler(final QuitHandler quitHandler) {
        checkEventsProcessingPermission();
        checkQuitPermission();
        checkActionSupport(Action.APP_QUIT_HANDLER);
        peer.setQuitHandler(quitHandler);
    }

    /**
     * Sets the default strategy used to quit this application. The default is
     * calling SYSTEM_EXIT_0.
     *
     * @param strategy the way this application should be shutdown
     *
     * @throws SecurityException if a security manager exists and it
     * will not allow the caller to invoke {@code System.exit} or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_QUIT_STRATEGY} action
     * @see QuitStrategy
     * @since 9
     */
    public void setQuitStrategy(final QuitStrategy strategy) {
        checkEventsProcessingPermission();
        checkQuitPermission();
        checkActionSupport(Action.APP_QUIT_STRATEGY);
        peer.setQuitStrategy(strategy);
    }

    /**
     * Enables this application to be suddenly terminated.
     *
     * Call this method to indicate your application's state is saved, and
     * requires no notification to be terminated. Letting your application
     * remain terminatable improves the user experience by avoiding re-paging in
     * your application when it's asked to quit.
     *
     * <b>Note: enabling sudden termination will allow your application to be
     * quit without notifying your QuitHandler, or running any shutdown
     * hooks.</b>
     * E.g. user-initiated Cmd-Q, logout, restart, or shutdown requests will
     * effectively "kill -KILL" your application.
     *
     * @throws SecurityException if a security manager exists and it
     * will not allow the caller to invoke {@code System.exit} or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_SUDDEN_TERMINATION} action
     * @see #disableSuddenTermination()
     * @since 9
     */
    public void enableSuddenTermination() {
        checkEventsProcessingPermission();
        checkQuitPermission();
        checkActionSupport(Action.APP_SUDDEN_TERMINATION);
        peer.enableSuddenTermination();
    }

    /**
     * Prevents this application from being suddenly terminated.
     *
     * Call this method to indicate that your application has unsaved state, and
     * may not be terminated without notification.
     *
     * @throws SecurityException if a security manager exists and it
     * will not allow the caller to invoke {@code System.exit} or it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_SUDDEN_TERMINATION} action
     * @see #enableSuddenTermination()
     * @since 9
     */
    public void disableSuddenTermination() {
        checkEventsProcessingPermission();
        checkQuitPermission();
        checkActionSupport(Action.APP_SUDDEN_TERMINATION);
        peer.disableSuddenTermination();
    }

    /**
     * Requests this application to move to the foreground.
     *
     * @param allWindows if all windows of this application should be moved to
     * the foreground, or only the foremost one
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_REQUEST_FOREGROUND} action
     * @since 9
     */
    public void requestForeground(final boolean allWindows) {
        checkEventsProcessingPermission();
        checkActionSupport(Action.APP_REQUEST_FOREGROUND);
        peer.requestForeground(allWindows);
    }

    /**
     * Opens the native help viewer application.
     *
     * @implNote Please note that for Mac OS, it opens the native help viewer
     * application if a Help Book has been added to the application bundler
     * and registered in the Info.plist with CFBundleHelpBookFolder
     *
     * @throws SecurityException if a security manager exists and it denies the
     *         {@code RuntimePermission("canProcessApplicationEvents")}
     *         permission, or it denies the
     *         {@code AWTPermission("showWindowWithoutWarningBanner")}
     *         permission, or the calling thread is not allowed to create a
     *         subprocess
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_HELP_VIEWER} action
     * @since 9
     */
    public void openHelpViewer() {
        checkAWTPermission();
        checkExec();
        checkEventsProcessingPermission();
        checkActionSupport(Action.APP_HELP_VIEWER);
        peer.openHelpViewer();
    }

    /**
     * Sets the default menu bar to use when there are no active frames.
     *
     * @param menuBar to use when no other frames are active
     * @throws SecurityException if a security manager exists and it denies the
     * {@code RuntimePermission("canProcessApplicationEvents")} permission.
     * @throws UnsupportedOperationException if the current platform
     * does not support the {@link Desktop.Action#APP_MENU_BAR} action
     * @since 9
     */
    public void setDefaultMenuBar(final JMenuBar menuBar) {
        checkEventsProcessingPermission();
        checkActionSupport(Action.APP_MENU_BAR);

        if (menuBar != null) {
            Container parent = menuBar.getParent();
            if (parent != null) {
                parent.remove(menuBar);
                menuBar.updateUI();
            }
        }

        peer.setDefaultMenuBar(menuBar);
    }

    /**
     * Opens a folder containing the {@code file} and selects it
     * in a default system file manager.
     * @param file the file
     * @throws SecurityException If a security manager exists and its
     *         {@link SecurityManager#checkRead(java.lang.String)} method
     *         denies read access to the file or to its parent, or it denies the
     *         {@code AWTPermission("showWindowWithoutWarningBanner")}
     *         permission, or the calling thread is not allowed to create a
     *         subprocess
     * @throws UnsupportedOperationException if the current platform
     *         does not support the {@link Desktop.Action#BROWSE_FILE_DIR} action
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalArgumentException if the specified file or its parent
     *         doesn't exist
     * @since 9
     */
    public void browseFileDirectory(File file) {
        file = new File(file.getPath());
        checkAWTPermission();
        checkExec();
        checkActionSupport(Action.BROWSE_FILE_DIR);
        checkFileValidation(file);
        File parentFile = file.getParentFile();
        if (parentFile == null || !parentFile.exists()) {
            throw new IllegalArgumentException("Parent folder doesn't exist");
        }
        peer.browseFileDirectory(file);
    }

    /**
     * Moves the specified file to the trash.
     *
     * @param file the file
     * @return returns true if successfully moved the file to the trash.
     * @throws SecurityException If a security manager exists and its
     *         {@link SecurityManager#checkDelete(java.lang.String)} method
     *         denies deletion of the file
     * @throws UnsupportedOperationException if the current platform
     *         does not support the {@link Desktop.Action#MOVE_TO_TRASH} action
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalArgumentException if the specified file doesn't exist
     *
     * @since 9
     */
    @SuppressWarnings("removal")
    public boolean moveToTrash(File file) {
        file = new File(file.getPath());
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkDelete(file.getPath());
        }
        checkActionSupport(Action.MOVE_TO_TRASH);
        final File finalFile = file;
        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            checkFileValidation(finalFile);
            return null;
        });
        return peer.moveToTrash(file);
    }
}
