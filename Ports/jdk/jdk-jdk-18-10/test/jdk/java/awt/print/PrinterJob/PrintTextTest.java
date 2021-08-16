/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6425068 7157659 8132890
 * @summary Confirm that text prints where we expect to the length we expect.
 * @run main/manual=yesno PrintTextTest
 */

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.swing.*;

public class PrintTextTest extends Component implements Printable {

    static int preferredSize;
    Font textFont;
    AffineTransform gxTx;
    String page;
    boolean useFM;

    public static void main(String args[]) {
        String[] instructions =
        {
            "This tests that printed text renders similarly to on-screen",
            "under a variety of APIs and graphics and font transforms",
            "Print to your preferred printer. Collect the output.",
            "Refer to the onscreen buttons to cycle through the on-screen",
            "content",
            "For each page, confirm that the printed content corresponds to",
            "the on-screen rendering for that *same* page.",
            "Some cases may look odd but its intentional. Verify",
            "it looks the same on screen and on the printer.",
            "Note that text does not scale linearly from screen to printer",
            "so some differences are normal and not a bug.",
            "The easiest way to spot real problems is to check that",
            "any underlines are the same length as the underlined text",
            "and that any rotations are the same in each case.",
            "Note that each on-screen page is printed in both portrait",
            "and landscape mode",
            "So for example, Page 1/Portrait, and Page 1/Landscape when",
            "rotated to view properly, should both match Page 1 on screen.",
        };
        Sysout.createDialogWithInstructions(instructions);


        PrinterJob pjob = PrinterJob.getPrinterJob();
        PageFormat portrait = pjob.defaultPage();
        portrait.setOrientation(PageFormat.PORTRAIT);
        preferredSize = (int)portrait.getImageableWidth();

        PageFormat landscape = pjob.defaultPage();
        landscape.setOrientation(PageFormat.LANDSCAPE);

        Book book = new Book();

        JTabbedPane p = new JTabbedPane();

        int page = 1;
        Font font = new Font("Dialog", Font.PLAIN, 18);
        String name = "Page " + new Integer(page++);
        PrintTextTest ptt = new PrintTextTest(name, font, null, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = new Font("Dialog", Font.PLAIN, 18);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, null, true);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = getPhysicalFont();
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, null, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = getPhysicalFont();
        AffineTransform rotTx = AffineTransform.getRotateInstance(0.15);
        rotTx.translate(60,0);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, rotTx, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = new Font("Dialog", Font.PLAIN, 18);
        AffineTransform scaleTx = AffineTransform.getScaleInstance(1.25, 1.25);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, scaleTx, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = new Font("Dialog", Font.PLAIN, 18);
        scaleTx = AffineTransform.getScaleInstance(-1.25, 1.25);
        scaleTx.translate(-preferredSize/1.25, 0);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, scaleTx, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = new Font("Dialog", Font.PLAIN, 18);
        scaleTx = AffineTransform.getScaleInstance(1.25, -1.25);
        scaleTx.translate(0, -preferredSize/1.25);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, scaleTx, false);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = font.deriveFont(rotTx);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, null, false);
        p.add(ptt, BorderLayout.CENTER);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        font = new Font("Monospaced", Font.PLAIN, 12);
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, font, null, false);
        p.add(ptt, BorderLayout.CENTER);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        Font xfont = font.deriveFont(AffineTransform.getScaleInstance(1.5, 1));
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, xfont, null, false);
        p.add(ptt, BorderLayout.CENTER);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        Font yfont = font.deriveFont(AffineTransform.getScaleInstance(1, 1.5));
        name = "Page " + new Integer(page++);
        ptt = new PrintTextTest(name, yfont, null, false);
        p.add(ptt, BorderLayout.CENTER);
        p.add(name, ptt);
        book.append(ptt, portrait);
        book.append(ptt, landscape);

        if (System.getProperty("os.name").startsWith("Windows")) {
            font = new Font("MS Gothic", Font.PLAIN, 12);
            name = "Page " + new Integer(page++);
            ptt = new PrintJAText(name, font, null, true);
            p.add(ptt, BorderLayout.CENTER);
            p.add(name, ptt);
            book.append(ptt, portrait);
            book.append(ptt, landscape);

            font = new Font("MS Gothic", Font.PLAIN, 12);
            name = "Page " + new Integer(page++);
            rotTx = AffineTransform.getRotateInstance(0.15);
            ptt = new PrintJAText(name, font, rotTx, true);
            p.add(ptt, BorderLayout.CENTER);
            p.add(name, ptt);
            book.append(ptt, portrait);
            book.append(ptt, landscape);
        }

        pjob.setPageable(book);

        JFrame f = new JFrame();
        f.add(BorderLayout.CENTER, p);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.pack();
        f.show();

        try {
            if (pjob.printDialog()) {
                pjob.print();
            }
        } catch (PrinterException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // The test needs a physical font that supports Latin.
    private static Font physicalFont;
    private static Font getPhysicalFont() {
        if (physicalFont != null) {
            return physicalFont;
        }
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] names = ge.getAvailableFontFamilyNames();

        for (String n : names) {
            switch (n.toLowerCase()) {
                case "dialog":
                case "dialoginput":
                case "serif":
                case "sansserif":
                case "monospaced":
                     break;
                default:
                    Font f = new Font(n, Font.PLAIN, 18);
                    if (f.canDisplayUpTo("AZaz09") == -1) {
                        physicalFont = f;
                        return f;
                    }
             }
        }
        physicalFont = new Font(Font.DIALOG, Font.PLAIN, 18);
        return physicalFont;
    }

    public PrintTextTest(String page, Font font, AffineTransform gxTx,
                         boolean fm) {
        this.page = page;
        textFont = font;
        this.gxTx = gxTx;
        this.useFM = fm;
        setBackground(Color.white);
    }

    public static AttributedCharacterIterator getIterator(String s) {
        return new AttributedString(s).getIterator();
    }

    static String orient(PageFormat pf) {
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
            return "Portrait";
        } else {
            return "Landscape";
        }
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {

        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(pf.getImageableX(),  pf.getImageableY());
        g.drawString(page+" "+orient(pf),50,20);
        g.translate(0, 25);
        paint(g);
        return PAGE_EXISTS;
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        return new Dimension(preferredSize, preferredSize);
    }

    public void paint(Graphics g) {

        /* fill with white before any transformation is applied */
        g.setColor(Color.white);
        g.fillRect(0, 0, getSize().width, getSize().height);


        Graphics2D g2d = (Graphics2D) g;
        if (gxTx != null) {
            g2d.transform(gxTx);
        }
        if (useFM) {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();

        String s;
        int LS = 30;
        int ix=10, iy=LS+10;
        g.setColor(Color.black);

        s = "drawString(String str, int x, int y)";
        g.drawString(s, ix, iy);
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+fm.stringWidth(s), iy+1);
        }

        iy += LS;
        s = "drawString(AttributedCharacterIterator iterator, int x, int y)";
        g.drawString(getIterator(s), ix, iy);

        iy += LS;
        s = "\tdrawChars(\t\r\nchar[], int off, int len, int x, int y\t)";
        g.drawChars(s.toCharArray(), 0, s.length(), ix, iy);
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+fm.stringWidth(s), iy+1);
        }

        iy += LS;
        s = "drawBytes(byte[], int off, int len, int x, int y)";
        byte data[] = new byte[s.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) s.charAt(i);
        }
        g.drawBytes(data, 0, data.length, ix, iy);

        Font f = g2d.getFont();
        FontRenderContext frc = g2d.getFontRenderContext();

        iy += LS;
        s = "drawString(String s, float x, float y)";
        g2d.drawString(s, (float) ix, (float) iy);
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+fm.stringWidth(s), iy+1);
        }

        iy += LS;
        s = "drawString(AttributedCharacterIterator iterator, "+
            "float x, float y)";
        g2d.drawString(getIterator(s), (float) ix, (float) iy);

        iy += LS;
        s = "drawGlyphVector(GlyphVector g, float x, float y)";
        GlyphVector gv = f.createGlyphVector(frc, s);
        g2d.drawGlyphVector(gv, ix, iy);
        Point2D adv = gv.getGlyphPosition(gv.getNumGlyphs());
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+(int)adv.getX(), iy+1);
        }

        iy += LS;
        s = "GlyphVector with position adjustments";

        gv = f.createGlyphVector(frc, s);
        int ng = gv.getNumGlyphs();
        adv = gv.getGlyphPosition(ng);
        for (int i=0; i<ng; i++) {
            Point2D gp = gv.getGlyphPosition(i);
            double gx = gp.getX();
            double gy = gp.getY();
            if (i % 2 == 0) {
                gy+=5;
            } else {
                gy-=5;
            }
            gp.setLocation(gx, gy);
            gv.setGlyphPosition(i, gp);
        }
        g2d.drawGlyphVector(gv, ix, iy);
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+(int)adv.getX(), iy+1);
        }

        iy +=LS;
        s = "drawString: \u0924\u094d\u0930 \u0915\u0948\u0930\u0947 End.";
        g.drawString(s, ix, iy);
        if (!textFont.isTransformed()) {
            g.drawLine(ix, iy+1, ix+fm.stringWidth(s), iy+1);
        }

        iy += LS;
        s = "TextLayout 1: \u0924\u094d\u0930 \u0915\u0948\u0930\u0947 End.";
        TextLayout tl = new TextLayout(s, new HashMap(), frc);
        tl.draw(g2d,  ix, iy);

        iy += LS;
        s = "TextLayout 2: \u0924\u094d\u0930 \u0915\u0948\u0930\u0947 End.";
        tl = new TextLayout(s, f, frc);
        tl.draw(g2d,  ix, iy);
    }
}

class PrintJAText extends PrintTextTest {


    public PrintJAText(String page, Font font, AffineTransform gxTx,
                         boolean fm) {
        super(page, font, gxTx, fm);
    }

    private static final String TEXT =
        "\u3042\u3044\u3046\u3048\u304a\u30a4\u30ed\u30cf" +
        "\u30cb\u30db\u30d8\u30c8\u4e00\u4e01\u4e02\u4e05\uff08";


    public void paint(Graphics g) {

        /* fill with white before any transformation is applied */
        g.setColor(Color.white);
        g.fillRect(0, 0, getSize().width, getSize().height);


        Graphics2D g2d = (Graphics2D) g;
        if (gxTx != null) {
            g2d.transform(gxTx);
        }
        if (useFM) {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

        String text = TEXT + TEXT + TEXT;
        g.setColor(Color.black);
        int y = 20;
        float origSize = 7f;
        for (int i=0;i<11;i++) {
            float size = origSize+(i*0.1f);
            g2d.translate(0, size+6);
            Font f = textFont.deriveFont(size);
            g2d.setFont(f);
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int stringWidth = fontMetrics.stringWidth(text);
            g.drawLine(0, y+1, stringWidth, y+1);
            g.drawString(text, 0, y);
            y +=10;
        }
    }
}

class Sysout
 {
   private static TestDialog dialog;

   public static void createDialogWithInstructions( String[] instructions )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      dialog.printInstructions( instructions );
      dialog.show();
      println( "Any messages for the tester will display here." );
    }

   public static void createDialog( )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      String[] defInstr = { "Instructions will appear here. ", "" } ;
      dialog.printInstructions( defInstr );
      dialog.show();
      println( "Any messages for the tester will display here." );
    }


   public static void printInstructions( String[] instructions )
    {
      dialog.printInstructions( instructions );
    }


   public static void println( String messageIn )
    {
      dialog.displayMessage( messageIn );
    }

 }// Sysout  class

/**
  This is part of the standard test machinery.  It provides a place for the
   test instructions to be displayed, and a place for interactive messages
   to the user to be displayed.
  To have the test instructions displayed, see Sysout.
  To have a message to the user be displayed, see Sysout.
  Do not call anything in this dialog directly.
  */
class TestDialog extends Dialog
 {

   TextArea instructionsText;
   TextArea messageText;
   int maxStringLength = 80;

   //DO NOT call this directly, go through Sysout
   public TestDialog( Frame frame, String name )
    {
      super( frame, name );
      int scrollBoth = TextArea.SCROLLBARS_BOTH;
      instructionsText = new TextArea( "", 20, maxStringLength, scrollBoth );
      add( "North", instructionsText );

      messageText = new TextArea( "", 5, maxStringLength, scrollBoth );
      add("South", messageText);

      pack();

      show();
    }// TestDialog()

   //DO NOT call this directly, go through Sysout
   public void printInstructions( String[] instructions )
    {
      //Clear out any current instructions
      instructionsText.setText( "" );

      //Go down array of instruction strings

      String printStr, remainingStr;
      for( int i=0; i < instructions.length; i++ )
       {
     //chop up each into pieces maxSringLength long
     remainingStr = instructions[ i ];
     while( remainingStr.length() > 0 )
      {
        //if longer than max then chop off first max chars to print
        if( remainingStr.length() >= maxStringLength )
         {
           //Try to chop on a word boundary
           int posOfSpace = remainingStr.
          lastIndexOf( ' ', maxStringLength - 1 );

           if( posOfSpace <= 0 ) posOfSpace = maxStringLength - 1;

           printStr = remainingStr.substring( 0, posOfSpace + 1 );
           remainingStr = remainingStr.substring( posOfSpace + 1 );
         }
        //else just print
        else
         {
           printStr = remainingStr;
           remainingStr = "";
         }

            instructionsText.append( printStr + "\n" );

      }// while

       }// for

    }//printInstructions()

   //DO NOT call this directly, go through Sysout
   public void displayMessage( String messageIn )
    {
      messageText.append( messageIn + "\n" );
    }

}// TestDialog  class
