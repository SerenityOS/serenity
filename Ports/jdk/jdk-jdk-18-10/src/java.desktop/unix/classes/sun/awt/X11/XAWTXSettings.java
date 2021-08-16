/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

 /*
   * This code is ported to XAWT from MAWT based on awt_mgrsel.c
   * and XSettings.java code written originally by Valeriy Ushakov
   * Author : Bino George
   */


package sun.awt.X11;

import java.util.*;
import java.awt.*;
import sun.awt.XSettings;
import sun.util.logging.PlatformLogger;


class XAWTXSettings extends XSettings implements XMSelectionListener {

    private final XAtom xSettingsPropertyAtom = XAtom.get("_XSETTINGS_SETTINGS");

    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XAWTXSettings");

    /* The maximal length of the property data. */
    public static final long MAX_LENGTH = 1000000;

    XMSelection settings;

    public XAWTXSettings() {
        initXSettings();

    }

    void initXSettings() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Initializing XAWT XSettings");
        }
        settings = new XMSelection("_XSETTINGS");
        settings.addSelectionListener(this);
        initPerScreenXSettings();
    }

    void dispose() {
        settings.removeSelectionListener(this);
    }

    public void ownerDeath(int screen, XMSelection sel, long deadOwner) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Owner " + deadOwner + " died for selection " + sel + " screen "+ screen);
        }
    }


    public void ownerChanged(int screen, XMSelection sel, long newOwner, long data, long timestamp) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("New Owner "+ newOwner + " for selection = " + sel + " screen " +screen );
        }
    }

    public void selectionChanged(int screen, XMSelection sel, long owner , XPropertyEvent event) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Selection changed on sel " + sel + " screen = " + screen + " owner = " + owner + " event = " + event);
        }
        updateXSettings(screen,owner);
    }

    void initPerScreenXSettings() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Updating Per XSettings changes");
        }

        /*
         * As toolkit cannot yet cope with per-screen desktop properties,
         * only report XSETTINGS changes on the default screen.  This
         * should be "good enough" for most cases.
         */

        Map<String, Object> updatedSettings = null;
        XToolkit.awtLock();
        try {
            long display = XToolkit.getDisplay();
            int screen = (int) XlibWrapper.DefaultScreen(display);
            updatedSettings = getUpdatedSettings(settings.getOwner(screen));
        } finally {
            XToolkit.awtUnlock();
        }
        // we must not  invoke this under Awt Lock
        ((XToolkit)Toolkit.getDefaultToolkit()).parseXSettings(0,updatedSettings);
    }

    private void updateXSettings(int screen, long owner) {
        final Map<String, Object> updatedSettings = getUpdatedSettings(owner);
        // this method is called under awt lock and usually on toolkit thread
        // but parseXSettings() causes public code execution, so we need to transfer
        // this to EDT
        EventQueue.invokeLater( new Runnable() {
            public void run() {
                ((XToolkit) Toolkit.getDefaultToolkit()).parseXSettings( 0, updatedSettings);
            }
        });
    }

    private Map<String, Object> getUpdatedSettings(final long owner) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("owner =" + owner);
        }
        if (0 == owner) {
            return null;
        }

        Map<String, Object> settings = null;
        try {
            WindowPropertyGetter getter =
                new WindowPropertyGetter(owner, xSettingsPropertyAtom, 0, MAX_LENGTH,
                        false, xSettingsPropertyAtom.getAtom() );
            try {
                int status = getter.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

                if (status != XConstants.Success || getter.getData() == 0) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("OH OH : getter failed  status = " + status );
                    }
                    settings = null;
                }

                long ptr = getter.getData();

                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("noItems = " + getter.getNumberOfItems());
                }
                byte[] array = Native.toBytes(ptr,getter.getNumberOfItems());
                if (array != null) {
                    settings = update(array);
                }
            } finally {
                getter.dispose();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return settings;
    }



}
