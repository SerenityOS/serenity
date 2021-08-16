/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.tools.jconsole;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Proxy that shields GUI from plug-in exceptions.
 *
 */
final class ExceptionSafePlugin extends JConsolePlugin {

    private static boolean ignoreExceptions;
    private final JConsolePlugin plugin;

    public ExceptionSafePlugin(JConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Map<String, JPanel> getTabs() {
        try {
            return plugin.getTabs();
        } catch (RuntimeException e) {
            handleException(e);
        }
        return new HashMap<>();
    }

    @Override
    public SwingWorker<?, ?> newSwingWorker() {
        try {
            return plugin.newSwingWorker();
        } catch (RuntimeException e) {
            handleException(e);
        }
        return null;
    }

    @Override
    public void dispose() {
        try {
            plugin.dispose();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    public void executeSwingWorker(SwingWorker<?, ?> sw) {
        try {
            sw.execute();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        if (JConsole.isDebug()) {
            System.err.println("Plug-in exception:");
            e.printStackTrace();
        } else {
            if (!ignoreExceptions) {
                showExceptionDialog(e);
            }
        }
    }

    private void showExceptionDialog(Exception e) {
        Object[] buttonTexts = {
            Messages.PLUGIN_EXCEPTION_DIALOG_BUTTON_OK,
            Messages.PLUGIN_EXCEPTION_DIALOG_BUTTON_EXIT,
            Messages.PLUGIN_EXCEPTION_DIALOG_BUTTON_IGNORE
        };

        String message = String.format(
            Messages.PLUGIN_EXCEPTION_DIALOG_MESSAGE,
            plugin.getClass().getSimpleName(),
            String.valueOf(e.getMessage())
        );

        int buttonIndex = JOptionPane.showOptionDialog(
            null,
            message,
            Messages.PLUGIN_EXCEPTION_DIALOG_TITLE,
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            buttonTexts,
            buttonTexts[0]
        );

        if (buttonIndex == 1) {
            System.exit(0);
        }
        ignoreExceptions = buttonIndex == 2;
    }
}
