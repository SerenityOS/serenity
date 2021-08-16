/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */


package com.sun.java.swing.action;

import javax.swing.KeyStroke;

// Referenced classes of package com.sun.java.swing.action:
//            DelegateAction, ActionManager

public class OpenAction extends DelegateAction
{

    public OpenAction()
    {
        this("general/Open16.gif");
    }

    public OpenAction(String iconPath)
    {
        super("Open...", ActionManager.getIcon(iconPath));
        putValue("ActionCommandKey", "open-command");
        putValue("ShortDescription", "Open the specified object.");
        putValue("LongDescription", "Open the specified object.");
        putValue("MnemonicKey", VALUE_MNEMONIC);
        putValue("AcceleratorKey", VALUE_ACCELERATOR);
    }

    public static final String VALUE_COMMAND = "open-command";
    public static final String VALUE_NAME = "Open...";
    public static final String VALUE_SMALL_ICON = "general/Open16.gif";
    public static final String VALUE_LARGE_ICON = "general/Open24.gif";
    public static final Integer VALUE_MNEMONIC = 79;
    public static final KeyStroke VALUE_ACCELERATOR = KeyStroke.getKeyStroke(79, 2);
    public static final String VALUE_SHORT_DESCRIPTION = "Open the specified object.";
    public static final String VALUE_LONG_DESCRIPTION = "Open the specified object.";

}
