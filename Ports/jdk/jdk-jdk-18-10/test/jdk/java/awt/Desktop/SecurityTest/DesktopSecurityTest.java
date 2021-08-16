/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.Desktop;
import java.awt.desktop.AppForegroundEvent;
import java.awt.desktop.AppForegroundListener;
import java.awt.desktop.AppHiddenEvent;
import java.awt.desktop.AppHiddenListener;
import java.awt.desktop.AppReopenedListener;
import java.awt.desktop.QuitStrategy;
import java.awt.desktop.ScreenSleepEvent;
import java.awt.desktop.ScreenSleepListener;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @test
 * @bug 6255196 8143227
 * @key headful
 * @summary Tests that Desktop.browse() throws SecurityException without
 *          permission of java.awt.AWTPermission showWindowWithoutWarningBanner
 * @run main/othervm/policy=desktop.policy DesktopSecurityTest
 */
public final class DesktopSecurityTest {

    public static void main(String[] args) throws Exception {
        if(!Desktop.isDesktopSupported()){
            System.out.println("Desktop is not supported");
            return;
        }

        URI webURI = URI.create(System.getProperty("java.vendor.url",
                                                   "http://www.sun.com"));
        File testFile = new File("JDIC-test.txt").getAbsoluteFile();
        try {
            try (Writer writer = new FileWriter(testFile)) {
                writer.write("temp file used to test print() method of Desktop.");
                writer.flush();
            }
            test(webURI, testFile);
        } finally {
            Files.delete(Paths.get(testFile.getAbsolutePath()));
        }
    }

    private static void test(URI webURI, File testFile) throws Exception {
        Desktop desktop = Desktop.getDesktop();
        for (Desktop.Action action : Desktop.Action.values()) {
            if (!desktop.isSupported(action)) {
                continue;   // skip if this action is unsupported.
            }

           try {
               switch (action) {
                   case OPEN -> desktop.open(testFile);
                   case EDIT -> desktop.edit(testFile);
                   case PRINT -> desktop.print(testFile);
                   case MAIL -> desktop.mail();
                   case BROWSE -> desktop.browse(webURI);
                   case APP_EVENT_FOREGROUND -> {
                       desktop.addAppEventListener(new AppForegroundListener() {
                           @Override
                           public void appRaisedToForeground(AppForegroundEvent e) {
                           }
                           @Override
                           public void appMovedToBackground(AppForegroundEvent e) {
                           }
                       });
                   }
                   case APP_EVENT_HIDDEN -> {
                       desktop.addAppEventListener(new AppHiddenListener() {
                           @Override
                           public void appHidden(AppHiddenEvent e) {
                           }
                           @Override
                           public void appUnhidden(AppHiddenEvent e) {
                           }
                       });
                   }
                   case APP_EVENT_REOPENED -> {
                       desktop.addAppEventListener((AppReopenedListener) e -> {});
                   }
                   case APP_EVENT_SCREEN_SLEEP -> {
                       desktop.addAppEventListener(new ScreenSleepListener() {
                           @Override
                           public void screenAboutToSleep(ScreenSleepEvent e) {
                           }
                           @Override
                           public void screenAwoke(ScreenSleepEvent e) {
                           }
                       });
                   }
                   case APP_EVENT_SYSTEM_SLEEP -> {
                       desktop.addAppEventListener(new SystemSleepListener() {
                           @Override
                           public void systemAboutToSleep(SystemSleepEvent e) {
                           }
                           @Override
                           public void systemAwoke(SystemSleepEvent e) {
                           }
                       });
                   }
                   case APP_EVENT_USER_SESSION -> {
                       desktop.addAppEventListener(new UserSessionListener() {
                           @Override
                           public void userSessionDeactivated(UserSessionEvent e) {
                           }
                           @Override
                           public void userSessionActivated(UserSessionEvent e) {
                           }
                       });
                   }
                   case APP_ABOUT -> {
                       desktop.setAboutHandler(e -> {});
                   }
                   case APP_PREFERENCES -> {
                       desktop.setPreferencesHandler(e -> {});
                   }
                   case APP_OPEN_FILE -> {
                       desktop.setOpenFileHandler(e -> {});
                   }
                   case APP_PRINT_FILE -> {
                       desktop.setPrintFileHandler(e -> {});
                   }
                   case APP_OPEN_URI -> {
                       desktop.setOpenURIHandler(e -> {});
                   }
                   case APP_QUIT_HANDLER -> {
                       desktop.setQuitHandler((e, response) -> {});
                   }
                   case APP_QUIT_STRATEGY -> {
                       desktop.setQuitStrategy(QuitStrategy.NORMAL_EXIT);
                   }
                   case APP_SUDDEN_TERMINATION -> {
                        desktop.enableSuddenTermination();
                   }
                   case APP_REQUEST_FOREGROUND -> {
                       desktop.requestForeground(true);
                   }
                   case APP_HELP_VIEWER -> {
                       desktop.openHelpViewer();
                   }
                   case APP_MENU_BAR -> {
                       desktop.setDefaultMenuBar(null);
                   }
                   case BROWSE_FILE_DIR -> {
                       desktop.browseFileDirectory(testFile.getParentFile());
                   }
                   case MOVE_TO_TRASH -> {
                       // The test have permission to create/delete files, skip
                       continue;
                   }
                   default -> throw new IllegalStateException(
                           "Unexpected value: " + action);
               }
               // no exception has been thrown.
               throw new RuntimeException(
                       "SecurityException wax expected for: " + action);
           } catch (SecurityException ignored) {
               // expected
           }
       }
    }
}
