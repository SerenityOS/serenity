/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AppForegroundEvent;
import java.awt.desktop.AppHiddenEvent;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PrintFilesEvent;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.ScreenSleepEvent;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionEvent.Reason;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @test
 * @bug 8203224
 * @summary tests that the correct exceptions are thrown by the events classes
 *          in {code java.awt.desktop} package
 * @run main/othervm DesktopEventsExceptions
 * @run main/othervm -Djava.awt.headless=true DesktopEventsExceptions
 */
public final class DesktopEventsExceptions {

    public static void main(final String[] args) {
        // Each element of the list will creates one object to test
        final List<Runnable> constructors = List.of(
                AboutEvent::new,
                AppForegroundEvent::new,
                AppHiddenEvent::new,
                AppReopenedEvent::new,
                QuitEvent::new,
                ScreenSleepEvent::new,
                SystemSleepEvent::new,
                PreferencesEvent::new,
                () -> new PrintFilesEvent(Collections.emptyList()),
                () -> new UserSessionEvent(Reason.UNSPECIFIED),
                () -> new OpenFilesEvent(Collections.emptyList(), ""),
                () -> new OpenURIEvent(new File("").toURI())
        );

        for (final Runnable test : constructors) {
            try {
                test.run();
                checkHeadless(true);
                checkSupported(true);
            } catch (HeadlessException ex) {
                checkHeadless(false);
            } catch (UnsupportedOperationException ex) {
                checkSupported(false);
            }
        }
    }

    private static void checkSupported(final boolean isSupported) {
        if (isSupported != Desktop.isDesktopSupported()) {
            throw new RuntimeException();
        }
    }

    private static void checkHeadless(final boolean isHeadless) {
        if (isHeadless == GraphicsEnvironment.isHeadless()) {
            throw new RuntimeException();
        }
    }
}
