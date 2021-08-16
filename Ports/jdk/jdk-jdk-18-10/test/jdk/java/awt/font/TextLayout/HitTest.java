/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary verify Hit index with supplementary characters.
 * @bug 8173028
 */

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;

public class HitTest {

  public static void main(String args[]) {
      String s = new String(new int[]{0x1d400, 0x61}, 0, 2);
      Font font = new Font("Dialog", Font.PLAIN, 12);
      FontRenderContext frc = new FontRenderContext(null, false, false);
      TextLayout tl = new TextLayout(s, font, frc);
      TextHitInfo currHit = TextHitInfo.beforeOffset(3);
      TextHitInfo prevHit = tl.getNextLeftHit(currHit);
      System.out.println("index=" + prevHit.getCharIndex()+
                         " leading edge=" + prevHit.isLeadingEdge());
      if (prevHit.getCharIndex() != 2) {
          throw new RuntimeException("Expected 2 for hit index");
      }
   }
}
