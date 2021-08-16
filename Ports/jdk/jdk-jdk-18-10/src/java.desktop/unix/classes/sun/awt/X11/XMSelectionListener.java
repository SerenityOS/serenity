/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

public interface  XMSelectionListener {

   /*
    * This method is called when the owner changes
    */
   public void ownerChanged(int screen, XMSelection sel, long newOwner, long data, long timestamp);

   /*
    * This method is called when the owner dies
    */
   public void ownerDeath(int screen, XMSelection sel, long deadOwner);

   /*
    * This method is for selection change notification
    *
    * This method will only get called if you use the default constructor
    * or expilicitly specify PropertyChangeMask.
    */

   public void selectionChanged(int screen, XMSelection sel, long owner, XPropertyEvent event);

}
