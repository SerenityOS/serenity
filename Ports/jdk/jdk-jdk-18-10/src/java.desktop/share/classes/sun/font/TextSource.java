/*
 * Copyright (c) 1998, 2003, Oracle and/or its affiliates. All rights reserved.
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
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 */

package sun.font;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/**
 * A text source represents text for rendering, plus context information.
 * All text in the source uses the same font, metrics, and render context,
 * and is at the same bidi level.
 */

public abstract class TextSource {
  /** Source character data. */
  public abstract char[] getChars();

  /** Start of source data in char array returned from getChars. */
  public abstract int getStart();

  /** Length of source data. */
  public abstract int getLength();

  /** Start of context data in char array returned from getChars. */
  public abstract int getContextStart();

  /** Length of context data. */
  public abstract int getContextLength();

  /** Return the layout flags */
  public abstract int getLayoutFlags();

  /** Bidi level of all the characters in context. */
  public abstract int getBidiLevel();

  /** Font for source data. */
  public abstract Font getFont();

  /** Font render context to use when measuring or rendering source data. */
  public abstract FontRenderContext getFRC();

  /** Line metrics for source data. */
  public abstract CoreMetrics getCoreMetrics();

  /** Get subrange of this TextSource. dir is one of the TextLineComponent constants */
  public abstract TextSource getSubSource(int start, int length, int dir);

  /** Constant for toString(boolean).  Indicates that toString should not return info
      outside of the context of this instance. */
  public static final boolean WITHOUT_CONTEXT = false;

  /** Constant for toString(boolean).  Indicates that toString should return info
      outside of the context of this instance. */
  public static final boolean WITH_CONTEXT = true;

  /** Get debugging info about this TextSource instance. Default implementation just
      returns toString.  Subclasses should implement this to match the semantics of
      the toString constants. */
  public abstract String toString(boolean withContext);
}
