/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Desktop.Action;
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
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.JMenuBar;

/**
 * The {@code DesktopPeer} interface provides methods for the operation
 * of open, edit, print, browse and mail with the given URL or file, by
 * launching the associated application.
 * <p>
 * Each platform has an implementation class for this interface.
 *
 */
public interface DesktopPeer {

    /**
     * Returns whether the given action is supported on the current platform.
     * @param action the action type to be tested if it's supported on the
     *        current platform.
     * @return {@code true} if the given action is supported on
     *         the current platform; {@code false} otherwise.
     */
    boolean isSupported(Action action);

    /**
     * Launches the associated application to open the given file. The
     * associated application is registered to be the default file viewer for
     * the file type of the given file.
     *
     * @param file the given file.
     * @throws IOException If the given file has no associated application,
     *         or the associated application fails to be launched.
     */
    void open(File file) throws IOException;

    /**
     * Launches the associated editor and opens the given file for editing. The
     * associated editor is registered to be the default editor for the file
     * type of the given file.
     *
     * @param file the given file.
     * @throws IOException If the given file has no associated editor, or
     *         the associated application fails to be launched.
     */
    void edit(File file) throws IOException;

    /**
     * Prints the given file with the native desktop printing facility, using
     * the associated application's print command.
     *
     * @param file the given file.
     * @throws IOException If the given file has no associated application
     *         that can be used to print it.
     */
    void print(File file) throws IOException;

    /**
     * Launches the mail composing window of the user default mail client,
     * filling the message fields including to, cc, etc, with the values
     * specified by the given mailto URL.
     *
     * @param mailtoURL represents a mailto URL with specified values of the message.
     *        The syntax of mailto URL is defined by
     *        <a href="http://www.ietf.org/rfc/rfc2368.txt">RFC2368: The mailto
     *        URL scheme</a>
     * @throws IOException If the user default mail client is not found,
     *         or it fails to be launched.
     */
    void mail(URI mailtoURL) throws IOException;

    /**
     * Launches the user default browser to display the given URI.
     *
     * @param uri the given URI.
     * @throws IOException If the user default browser is not found,
     *         or it fails to be launched.
     */
    void browse(URI uri) throws IOException;

    /**
     * Adds sub-types of {@link SystemEventListener} to listen for notifications
     * from the native system.
     *
     * @param listener listener
     * @see java.awt.desktop.AppForegroundListener
     * @see java.awt.desktop.AppHiddenListener
     * @see java.awt.desktop.AppReopenedListener
     * @see java.awt.desktop.ScreenSleepListener
     * @see java.awt.desktop.SystemSleepListener
     * @see java.awt.desktop.UserSessionListener
     */
    default void addAppEventListener(final SystemEventListener listener) {
    }

    /**
     * Removes sub-types of {@link SystemEventListener} to listen for notifications
     * from the native system.
     *
     * @param listener listener
     * @see java.awt.desktop.AppForegroundListener
     * @see java.awt.desktop.AppHiddenListener
     * @see java.awt.desktop.AppReopenedListener
     * @see java.awt.desktop.ScreenSleepListener
     * @see java.awt.desktop.SystemSleepListener
     * @see java.awt.desktop.UserSessionListener
     */
    default void removeAppEventListener(final SystemEventListener listener) {
    }

    /**
     * Installs a handler to show a custom About window for your application.
     * <p>
     * Setting the {@link AboutHandler} to {@code null} reverts it to the
     * default behavior.
     *
     * @param aboutHandler the handler to respond to the
     * {@link AboutHandler#handleAbout} )} message
     */
    default void setAboutHandler(final AboutHandler aboutHandler) {
    }

    /**
     * Installs a handler to show a custom Preferences window for your
     * application.
     * <p>
     * Setting the {@link PreferencesHandler} to {@code null} reverts it to
     * the default behavior
     *
     * @param preferencesHandler the handler to respond to the
     * {@link java.awt.desktop.PreferencesHandler#handlePreferences(PreferencesEvent) }
     */
    default void setPreferencesHandler(final PreferencesHandler preferencesHandler) {
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * open a list of files.
     *
     * @param openFileHandler handler
     *
     */
    default void setOpenFileHandler(final OpenFilesHandler openFileHandler) {
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * print a list of files.
     *
     * @param printFileHandler handler
     */
    default void setPrintFileHandler(final PrintFilesHandler printFileHandler) {
    }

    /**
     * Installs the handler which is notified when the application is asked to
     * open a URL.
     *
     * Setting the handler to {@code null} causes all
     * {@link OpenURIHandler#openURI(OpenURIEvent)} requests to be
     * enqueued until another handler is set.
     *
     * @param openURIHandler handler
     */
    default void setOpenURIHandler(final OpenURIHandler openURIHandler) {
    }

    /**
     * Installs the handler which determines if the application should quit.
     *
     * @param quitHandler the handler that is called when the application is
     * asked to quit
     * @see java.awt.Desktop#setQuitHandler(java.awt.desktop.QuitHandler)
     */
    default void setQuitHandler(final QuitHandler quitHandler) {
    }

    /**
     * Sets the default strategy used to quit this application. The default is
     * calling SYSTEM_EXIT_0.
     *
     * @param strategy the way this application should be shutdown
     */
    default void setQuitStrategy(final QuitStrategy strategy) {
    }

    /**
     * Enables this application to be suddenly terminated.
     *
     * @see java.awt.Desktop#disableSuddenTermination()
     */
    default void enableSuddenTermination() {
    }

   /**
     * Prevents this application from being suddenly terminated.
     *
     * @see java.awt.Desktop#enableSuddenTermination()
     */
    default void disableSuddenTermination() {
    }

    /**
     * Requests this application to move to the foreground.
     *
     * @param allWindows if all windows of this application should be moved to
     * the foreground, or only the foremost one
     */
    default void requestForeground(final boolean allWindows) {
    }

    /**
     * Opens the native help viewer application.
     */
    default void openHelpViewer() {
    }

    /**
     * Sets the default menu bar to use when there are no active frames.
     *
     * @param menuBar to use when no other frames are active
     */
    default void setDefaultMenuBar(final JMenuBar menuBar) {
    }

    /**
     * Opens a folder containing the {@code file} in a default system file manager.
     * @param file the file
     * @return returns true if successfully opened
     */
    default boolean browseFileDirectory(File file) {
        return false;
    }
    /**
     * Moves the specified file to the trash.
     *
     * @param file the file
     * @return returns true if successfully moved the file to the trash.
     */
    default boolean moveToTrash(File file) {
        return false;
    }

}
