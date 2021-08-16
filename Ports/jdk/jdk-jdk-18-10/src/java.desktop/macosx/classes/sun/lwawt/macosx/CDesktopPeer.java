/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import com.apple.eawt.Application;

import javax.swing.*;
import java.awt.Desktop.Action;
import java.awt.desktop.*;
import java.awt.peer.DesktopPeer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;


/**
 * Concrete implementation of the interface {@code DesktopPeer} for MacOS X
 *
 * @see DesktopPeer
 */
public final class CDesktopPeer implements DesktopPeer {

    @Override
    public boolean isSupported(Action action) {
        return true;
    }

    @Override
    public void open(File file) throws IOException {
        this.lsOpenFile(file, false);
    }

    @Override
    public void edit(File file) throws IOException {
        this.lsOpenFile(file, false);
    }

    @Override
    public void print(File file) throws IOException {
        this.lsOpenFile(file, true);
    }

    @Override
    public void mail(URI uri) throws IOException {
        this.lsOpen(uri);
    }

    @Override
    public void browse(URI uri) throws IOException {
        this.lsOpen(uri);
    }

    @Override
    public void addAppEventListener(SystemEventListener listener) {
        Application.getApplication().addAppEventListener(listener);
    }

    @Override
    public void removeAppEventListener(SystemEventListener listener) {
        Application.getApplication().removeAppEventListener(listener);
    }

    @Override
    public void setAboutHandler(AboutHandler aboutHandler) {
        Application.getApplication().setAboutHandler(aboutHandler);
    }

    @Override
    public void setPreferencesHandler(PreferencesHandler preferencesHandler) {
        Application.getApplication().setPreferencesHandler(preferencesHandler);
    }

    @Override
    public void setOpenFileHandler(OpenFilesHandler openFileHandler) {
        Application.getApplication().setOpenFileHandler(openFileHandler);
    }

    @Override
    public void setPrintFileHandler(PrintFilesHandler printFileHandler) {
        Application.getApplication().setPrintFileHandler(printFileHandler);
    }

    @Override
    public void setOpenURIHandler(OpenURIHandler openURIHandler) {
        Application.getApplication().setOpenURIHandler(openURIHandler);
    }

    @Override
    public void setQuitHandler(QuitHandler quitHandler) {
        Application.getApplication().setQuitHandler(quitHandler);
    }

    @Override
    public void setQuitStrategy(QuitStrategy strategy) {
        Application.getApplication().setQuitStrategy(strategy);
    }

    @Override
    public void enableSuddenTermination() {
        Application.getApplication().enableSuddenTermination();
    }

    @Override
    public void disableSuddenTermination() {
        Application.getApplication().disableSuddenTermination();
    }

    @Override
    public void requestForeground(boolean allWindows) {
        Application.getApplication().requestForeground(allWindows);
    }

    @Override
    public void openHelpViewer() {
        Application.getApplication().openHelpViewer();
    }

    @Override
    public void setDefaultMenuBar(JMenuBar menuBar) {
        Application.getApplication().setDefaultMenuBar(menuBar);
    }

    @Override
    public boolean browseFileDirectory(File file) {
        try {
            return com.apple.eio.FileManager.revealInFinder(file);
        } catch (FileNotFoundException ex) {
            return false; //handled in java.awt.Desktop
        }
    }

    @Override
    public boolean moveToTrash(File file) {
        try {
            return com.apple.eio.FileManager.moveToTrash(file);
        } catch (FileNotFoundException ex) {
            return false; //handled in java.awt.Desktop
        }
    }

    private void lsOpen(URI uri) throws IOException {
        int status = _lsOpenURI(uri.toString());

        if (status != 0 /* noErr */) {
            throw new IOException("Failed to mail or browse " + uri + ". Error code: " + status);
        }
    }

    private void lsOpenFile(File file, boolean print) throws IOException {
        int status = _lsOpenFile(file.getCanonicalPath(), print);

        if (status != 0 /* noErr */) {
            throw new IOException("Failed to open, edit or print " + file + ". Error code: " + status);
        }
    }

    private static native int _lsOpenURI(String uri);

    private static native int _lsOpenFile(String path, boolean print);

}
