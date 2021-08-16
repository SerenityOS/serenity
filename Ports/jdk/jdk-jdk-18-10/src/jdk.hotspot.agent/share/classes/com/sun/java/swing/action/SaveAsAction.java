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


// Referenced classes of package com.sun.java.swing.action:
//            DelegateAction, ActionManager

public class SaveAsAction extends DelegateAction
{

    public SaveAsAction()
    {
        this("general/SaveAs16.gif");
    }

    public SaveAsAction(String iconPath)
    {
        super("Save As", ActionManager.getIcon(iconPath));
        putValue("ActionCommandKey", "save-as-command");
        putValue("ShortDescription", "Save as a new file");
        putValue("LongDescription", "Saves the current object as another object");
        putValue("MnemonicKey", VALUE_MNEMONIC);
    }

    public static final String VALUE_COMMAND = "save-as-command";
    public static final String VALUE_NAME = "Save As";
    public static final String VALUE_SMALL_ICON = "general/SaveAs16.gif";
    public static final String VALUE_LARGE_ICON = "general/SaveAs24.gif";
    public static final Integer VALUE_MNEMONIC = 65;
    public static final String VALUE_SHORT_DESCRIPTION = "Save as a new file";
    public static final String VALUE_LONG_DESCRIPTION = "Saves the current object as another object";

}
