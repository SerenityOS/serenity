/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 * See TestJustification.html for main test.
 */

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;

public class TestJustification extends Applet {
  JustificationPanel panel;

  public void init() {
    setLayout(new BorderLayout());
    panel = new JustificationPanel("Bitstream Cyberbit");
    add("Center", panel);
  }

  public void destroy() {
    remove(panel);
  }

  // calls system.exit, not for use in tests.
  public static void main(String args[]) {
    TestJustification justificationTest = new TestJustification();
    justificationTest.init();
    justificationTest.start();

    Frame f = new Frame("Test Justification");
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    f.add("Center", justificationTest);
    f.setSize(500, 500);
    f.show();
  }

  public String getAppletInfo() {
    return "Test TextLayout.getJustifiedLayout()";
  }

  static class JustificationPanel extends Panel {
    TextLayout[] layouts;
    String fontname;
    float height;
    float oldfsize;

    AttributedCharacterIterator lineText;
    TextLayout[] lines;
    int linecount;
    float oldwidth;

    JustificationPanel(String fontname) {
      this.fontname = fontname;
    }

    private static final String[] texts = {
      "This is an english Highlighting demo.", "Highlighting",
      "This is an arabic \u0627\u0628\u062a\u062c \u062e\u0644\u0627\u062e demo.", "arabic \u0627\u0628\u062a\u062c",
      "This is a hebrew \u05d0\u05d1\u05d2 \u05d3\u05d4\u05d5 demo.", "hebrew \u05d0\u05d1\u05d2",
      "This is a cjk \u4e00\u4e01\u4e02\uac00\uac01\uc4fa\uf900\uf901\uf902 demo.", "cjk",
      "NoSpaceCJK:\u4e00\u4e01\u4e02and\uac00\uac01\uc4faand\uf900\uf901\uf902", "No",
      "NoSpaceRoman", "Space"
    };

    public void paint(Graphics g) {
      Graphics2D g2d = (Graphics2D)g;

      Dimension d = getSize();
      Insets insets = getInsets();

      float w = d.width - insets.left - insets.right;
      float h = d.height - insets.top - insets.bottom;
      int fsize = (int)w/25;

      FontRenderContext frc = g2d.getFontRenderContext();

      if (layouts == null || fsize != oldfsize) {
        oldfsize = fsize;

        Font f0 = new Font(fontname, Font.PLAIN, fsize);
        Font f1 = new Font(fontname, Font.ITALIC, (int)(fsize * 1.5));

        if (layouts == null) {
          layouts = new TextLayout[texts.length / 2];
        }

        height = 0;
        for (int i = 0; i < layouts.length; ++i) {
          String text = texts[i*2];
          String target = texts[i*2+1];

          AttributedString astr = new AttributedString(text);
          astr.addAttribute(TextAttribute.FONT, f0, 0, text.length());

          int start = text.indexOf(target);
          int limit = start + target.length();
          astr.addAttribute(TextAttribute.FONT, f1, start, limit);

          TextLayout layout = new TextLayout(astr.getIterator(), frc);

          layout = layout.getJustifiedLayout(w - 20);

          layouts[i] = layout;

          height += layout.getAscent() + layout.getDescent() + layout.getLeading();
        }
      }

      g2d.setColor(Color.white);
      g2d.fill(new Rectangle.Float(insets.left, insets.top, w, h));

      float basey = 20;

      for (int i = 0; i < layouts.length; ++i) {
        TextLayout layout = layouts[i];

        float la = layout.getAscent();
        float ld = layout.getDescent();
        float ll = layout.getLeading();
        float lw = layout.getAdvance();
        float lh = la + ld + ll;
        float lx = (w - lw) / 2f;
        float ly = basey + layout.getAscent();

        g2d.setColor(Color.black);
        g2d.translate(insets.left + lx, insets.top + ly);

        Rectangle2D bounds = new Rectangle2D.Float(0, -la, lw, lh);
        g2d.draw(bounds);

        layout.draw(g2d, 0, 0);

        g2d.setColor(Color.red);
        for (int j = 0, e = layout.getCharacterCount(); j <= e; ++j) {
          Shape[] carets = layout.getCaretShapes(j, bounds);
          g2d.draw(carets[0]);
        }

        g2d.translate(-insets.left - lx, -insets.top - ly);
        basey += layout.getAscent() + layout.getDescent() + layout.getLeading();
      }

      // add LineBreakMeasurer-generated layouts

      if (lineText == null) {
        String text = "This is a long line of text that should be broken across multiple "
          + "lines and then justified to fit the break width.  This test should pass if "
          + "these lines are justified to the same width, and fail otherwise.  It should "
          + "also format the hebrew (\u05d0\u05d1\u05d2 \u05d3\u05d4\u05d5) and arabic "
          + "(\u0627\u0628\u062a\u062c \u062e\u0644\u0627\u062e) and CJK "
          + "(\u4e00\u4e01\u4e02\uac00\uac01\uc4fa\u67b1\u67b2\u67b3\u67b4\u67b5\u67b6\u67b7"
          + "\u67b8\u67b9) text correctly.";

        Float regular = new Float(16.0);
        Float big = new Float(24.0);
        AttributedString astr = new AttributedString(text);
        astr.addAttribute(TextAttribute.SIZE, regular, 0, text.length());
        astr.addAttribute(TextAttribute.FAMILY, fontname, 0, text.length());

        int ix = text.indexOf("broken");
        astr.addAttribute(TextAttribute.SIZE, big, ix, ix + 6);
        ix = text.indexOf("hebrew");
        astr.addAttribute(TextAttribute.SIZE, big, ix, ix + 6);
        ix = text.indexOf("arabic");
        astr.addAttribute(TextAttribute.SIZE, big, ix, ix + 6);
        ix = text.indexOf("CJK");
        astr.addAttribute(TextAttribute.SIZE, big, ix, ix + 3);

        lineText = astr.getIterator();
      }

      float width = w - 20;
      if (lines == null || width != oldwidth) {
        oldwidth = width;

        lines = new TextLayout[10];
        linecount = 0;

        LineBreakMeasurer measurer = new LineBreakMeasurer(lineText, frc);

        for (;;) {
          TextLayout layout = measurer.nextLayout(width);
          if (layout == null) {
            break;
          }

          // justify all but last line
          if (linecount > 0) {
            lines[linecount - 1] = lines[linecount - 1].getJustifiedLayout(width);
          }

          if (linecount == lines.length) {
            TextLayout[] nlines = new TextLayout[lines.length * 2];
            System.arraycopy(lines, 0, nlines, 0, lines.length);
            lines = nlines;
          }

          lines[linecount++] = layout;
        }
      }

      float basex = insets.left + 10;
      basey += 10;
      g2d.setColor(Color.black);

      for (int i = 0; i < linecount; ++i) {
        TextLayout layout = lines[i];

        basey += layout.getAscent();
        float adv = layout.getAdvance();
        float dx = layout.isLeftToRight() ? 0 : width - adv;

        layout.draw(g2d, basex + dx, basey);

        basey += layout.getDescent() + layout.getLeading();
      }
    }
  }
}
