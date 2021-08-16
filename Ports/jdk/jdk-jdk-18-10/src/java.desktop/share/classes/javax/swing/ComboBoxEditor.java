/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.*;
import java.awt.event.*;

/**
 * The editor component used for JComboBox components.
 *
 * @author Arnaud Weber
 * @since 1.2
 */
public interface ComboBoxEditor {

  /**
   * Returns the component that should be added to the tree hierarchy for
   * this editor
   *
   * @return the component
   */
  public Component getEditorComponent();

  /**
   * Set the item that should be edited. Cancel any editing if necessary
   *
   * @param anObject an item
   */
  public void setItem(Object anObject);

  /**
   * Returns the edited item
   *
   * @return the edited item
   */
  public Object getItem();

  /**
   * Ask the editor to start editing and to select everything
   */
  public void selectAll();

  /**
   * Add an ActionListener. An action event is generated when the edited item changes
   *
   * @param l an {@code ActionListener}
   */
  public void addActionListener(ActionListener l);

  /**
   * Remove an ActionListener
   *
   * @param l an {@code ActionListener}
   */
  public void removeActionListener(ActionListener l);
}
