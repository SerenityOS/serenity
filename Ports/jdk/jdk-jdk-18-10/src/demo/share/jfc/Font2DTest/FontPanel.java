/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.AttributedString;
import java.util.Vector;

import javax.imageio.*;
import javax.swing.*;

import static java.awt.RenderingHints.*;

/**
 * FontPanel.java
 *
 * @author Shinsuke Fukuda
 * @author Ankit Patel [Conversion to Swing - 01/07/30]
 */

/// This panel is combination of the text drawing area of Font2DTest
/// and the custom controlled scroll bar

public final class FontPanel extends JPanel implements AdjustmentListener {

    /// Drawing Option Constants
    private final String[] STYLES =
      { "plain", "bold", "italic", "bold italic" };

    private final int NONE = 0;
    private final int SCALE = 1;
    private final int SHEAR = 2;
    private final int ROTATE = 3;
    private final String[] TRANSFORMS =
      { "with no transforms", "with scaling", "with Shearing", "with rotation" };

    private final int DRAW_STRING = 0;
    private final int DRAW_CHARS = 1;
    private final int DRAW_BYTES = 2;
    private final int DRAW_GLYPHV = 3;
    private final int TL_DRAW = 4;
    private final int GV_OUTLINE = 5;
    private final int TL_OUTLINE = 6;
    private final String[] METHODS = {
        "drawString", "drawChars", "drawBytes", "drawGlyphVector",
        "TextLayout.draw", "GlyphVector.getOutline", "TextLayout.getOutline" };

    public final int RANGE_TEXT = 0;
    public final int ALL_GLYPHS = 1;
    public final int USER_TEXT = 2;
    public final int FILE_TEXT = 3;
    private final String[] MS_OPENING =
      { " Unicode ", " Glyph Code ", " lines ", " lines " };
    private final String[] MS_CLOSING =
      { "", "", " of User Text ", " of LineBreakMeasurer-reformatted Text " };

    /// General Graphics Variable
    private final JScrollBar verticalBar;
    private final FontCanvas fc;
    private boolean updateFontMetrics = true;
    private boolean updateFont = true;
    private boolean force16Cols = false;
    public boolean showingError = false;
    private int g2Transform = NONE; /// ABP

    /// Printing constants and variables
    public final int ONE_PAGE = 0;
    public final int CUR_RANGE = 1;
    public final int ALL_TEXT = 2;
    private int printMode = ONE_PAGE;
    private PageFormat page = null;
    private PrinterJob printer = null;

    /// Text drawing variables
    private String fontName = "Dialog";
    private float fontSize = 12;
    private int fontStyle = Font.PLAIN;
    private int fontTransform = NONE;
    private Font testFont = null;
    private Object antiAliasType = VALUE_TEXT_ANTIALIAS_DEFAULT;
    private Object fractionalMetricsType = VALUE_FRACTIONALMETRICS_DEFAULT;
    private Object lcdContrast = getDefaultLCDContrast();
    private int drawMethod = DRAW_STRING;
    private int textToUse = RANGE_TEXT;
    private String[] userText = null;
    private String[] fileText = null;
    private int[] drawRange = { 0x0000, 0x007f };
    private String[] fontInfos = new String[2];
    private boolean showGrid = true;

    /// Parent Font2DTest panel
    private final Font2DTest f2dt;
    private final JFrame parent;

    public FontPanel( Font2DTest demo, JFrame f ) {
        f2dt = demo;
        parent = f;

        verticalBar = new JScrollBar ( JScrollBar.VERTICAL );
        fc = new FontCanvas();

        this.setLayout( new BorderLayout() );
        this.add( "Center", fc );
        this.add( "East", verticalBar );

        verticalBar.addAdjustmentListener( this );
        this.addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent e ) {
                updateFontMetrics = true;
            }
        });

        /// Initialize font and its infos
        testFont = new Font(fontName, fontStyle, (int)fontSize);
        if ((float)((int)fontSize) != fontSize) {
            testFont = testFont.deriveFont(fontSize);
        }
        updateFontInfo();
    }

    public Dimension getPreferredSize() {
        return new Dimension(600, 200);
    }

    /// Functions called by the main programs to set the various parameters

    public void setTransformG2( int transform ) {
        g2Transform = transform;
        updateFontMetrics = true;
        fc.repaint();
    }

    /// convenience fcn to create AffineTransform of appropriate type
    private AffineTransform getAffineTransform( int transform ) {
            /// ABP
            AffineTransform at = new AffineTransform();
            switch ( transform )
            {
            case SCALE:
              at.setToScale( 1.5f, 1.5f ); break;
            case ROTATE:
              at.setToRotation( Math.PI / 6 ); break;
            case SHEAR:
              at.setToShear( 0.4f, 0 ); break;
            case NONE:
              break;
            default:
              //System.err.println( "Illegal G2 Transform Arg: " + transform);
              break;
            }

            return at;
    }

    public void setFontParams(Object obj, float size,
                              int style, int transform) {
        setFontParams( (String)obj, size, style, transform );
    }

    public void setFontParams(String name, float size,
                              int style, int transform) {
        boolean fontModified = false;
        if ( !name.equals( fontName ) || style != fontStyle )
          fontModified = true;

        fontName = name;
        fontSize = size;
        fontStyle = style;
        fontTransform = transform;

        /// Recreate the font as specified
        testFont = new Font(fontName, fontStyle, (int)fontSize);
        if ((float)((int)fontSize) != fontSize) {
            testFont = testFont.deriveFont(fontSize);
        }

        if ( fontTransform != NONE ) {
            AffineTransform at = getAffineTransform( fontTransform );
            testFont = testFont.deriveFont( at );
        }
        updateFontMetrics = true;
        fc.repaint();
        if ( fontModified ) {
            /// Tell main panel to update the font info
            updateFontInfo();
            f2dt.fireUpdateFontInfo();
        }
    }

    public void setRenderingHints( Object aa, Object fm, Object contrast) {
        antiAliasType = ((AAValues)aa).getHint();
        fractionalMetricsType = ((FMValues)fm).getHint();
        lcdContrast = contrast;
        updateFontMetrics = true;
        fc.repaint();
    }

    public void setDrawMethod( int i ) {
        drawMethod = i;
        fc.repaint();
    }

    public void setTextToDraw( int i, int[] range,
                               String[] textSet, String[] fileData ) {
        textToUse = i;

        if ( textToUse == RANGE_TEXT )
          drawRange = range;
        else if ( textToUse == ALL_GLYPHS )
          drawMethod = DRAW_GLYPHV;
        else if ( textToUse == USER_TEXT )
          userText = textSet;
        else if ( textToUse == FILE_TEXT ) {
            fileText = fileData;
            drawMethod = TL_DRAW;
        }

        updateFontMetrics = true;
        fc.repaint();
        updateFontInfo();
    }

    public void setGridDisplay( boolean b ) {
        showGrid = b;
        fc.repaint();
    }

    public void setForce16Columns( boolean b ) {
        force16Cols = b;
        updateFontMetrics = true;
        fc.repaint();
    }

    /// Prints out the text display area
    public void doPrint( int i ) {
        if ( printer == null ) {
            printer = PrinterJob.getPrinterJob();
            page = printer.defaultPage();
        }
        printMode = i;
        printer.setPrintable( fc, page );

        if ( printer.printDialog() ) {
            try {
                printer.print();
            }
            catch ( Exception e ) {
                f2dt.fireChangeStatus( "ERROR: Printing Failed; See Stack Trace", true );
            }
        }
    }

    /// Displays the page setup dialog and updates PageFormat info
    public void doPageSetup() {
        if ( printer == null ) {
            printer = PrinterJob.getPrinterJob();
            page = printer.defaultPage();
        }
        page = printer.pageDialog( page );
    }

    /// Obtains the information about selected font
    private void updateFontInfo() {
        int numGlyphs = 0, numCharsInRange = drawRange[1] - drawRange[0] + 1;
        fontInfos[0] = "Font Face Name: " + testFont.getFontName();
        fontInfos[1] = "Glyphs in This Range: ";

        if ( textToUse == RANGE_TEXT ) {
            for ( int i = drawRange[0]; i < drawRange[1]; i++ )
              if ( testFont.canDisplay( i ))
                numGlyphs++;
            fontInfos[1] = fontInfos[1] + numGlyphs + " / " + numCharsInRange;
        }
        else
          fontInfos[1] = null;
    }

    /// Accessor for the font information
    public String[] getFontInfo() {
        return fontInfos;
    }

    /// Collects the currectly set options and returns them as string
    public String getCurrentOptions() {
        /// Create a new String to store the options
        /// The array will contain all 8 setting (font name, size...) and
        /// character range or user text data used (no file text data)
        int userTextSize = 0;
        String options;

        options = ( fontName + "\n" + fontSize  + "\n" + fontStyle + "\n" +
                    fontTransform + "\n"  + g2Transform + "\n"+
                    textToUse + "\n" + drawMethod + "\n" +
                    AAValues.getHintVal(antiAliasType) + "\n" +
                    FMValues.getHintVal(fractionalMetricsType) + "\n" +
                    lcdContrast + "\n");
        if ( textToUse == USER_TEXT )
          for ( int i = 0; i < userText.length; i++ )
            options += ( userText[i] + "\n" );

        return options;
    }

    /// Reload all options and refreshes the canvas
    public void loadOptions( boolean grid, boolean force16, int start, int end,
                             String name, float size, int style,
                             int transform, int g2transform,
                             int text, int method, int aa, int fm,
                             int contrast, String[] user ) {
        int[] range = { start, end };

        /// Since repaint call has a low priority, these functions will finish
        /// before the actual repainting is done
        setGridDisplay( grid );
        setForce16Columns( force16 );
        // previous call to readTextFile has already set the text to draw
        if (textToUse != FILE_TEXT) {
          setTextToDraw( text, range, user, null );
        }
        setFontParams( name, size, style, transform );
        setTransformG2( g2transform ); // ABP
        setDrawMethod( method );
        setRenderingHints(AAValues.getValue(aa), FMValues.getValue(fm),
                          Integer.valueOf(contrast));
    }

    /// Writes the current screen to PNG file
    public void doSavePNG( String fileName ) {
        fc.writePNG( fileName );
    }

    /// When scrolled using the scroll bar, update the backbuffer
    public void adjustmentValueChanged( AdjustmentEvent e ) {
        fc.repaint();
    }

    public void paintComponent( Graphics g ) {
        // Windows does not repaint correctly, after
        // a zoom. Thus, we need to force the canvas
        // to repaint, but only once. After the first repaint,
        // everything stabilizes. [ABP]
        fc.repaint();
    }

    /// Inner class definition...

    /// Inner panel that holds the actual drawing area and its routines
    private class FontCanvas extends JPanel implements MouseListener, MouseMotionListener, Printable {

        /// Number of characters that will fit across and down this canvas
        private int numCharAcross, numCharDown;

        /// First and last character/line that will be drawn
        /// Limit is the end of range/text where no more draw will be done
        private int drawStart, drawEnd, drawLimit;

        /// FontMetrics variables
        /// Here, gridWidth is equivalent to maxAdvance (slightly bigger though)
        /// and gridHeight is equivalent to lineHeight
        private int maxAscent, maxDescent, gridWidth = 0, gridHeight = 0;

        /// Offset from the top left edge of the canvas where the draw will start
        private int canvasInset_X = 5, canvasInset_Y = 5;

        /// LineBreak'ed TextLayout vector
        private Vector<TextLayout> lineBreakTLs = null;

        /// Whether the current draw command requested is for printing
        private boolean isPrinting = false;

        /// Other printing infos
        private int lastPage, printPageNumber, currentlyShownChar = 0;
        private final int PR_OFFSET = 10;
        private final int PR_TITLE_LINEHEIGHT = 30;

        /// Information about zooming (used with range text draw)
        private final JWindow zoomWindow;
        private BufferedImage zoomImage = null;
        private int mouseOverCharX = -1, mouseOverCharY = -1;
        private int currMouseOverChar = -1, prevZoomChar = -1;
        private float ZOOM = 2.0f;
        private boolean nowZooming = false;
        private boolean firstTime = true;
// ABP

        /// Status bar message backup
        private String backupStatusString = null;

        /// Error constants
        private final String[] ERRORS = {
            "ERROR: drawBytes cannot handle characters beyond 0x00FF. Select different range or draw methods.",
            "ERROR: Cannot fit text with the current font size. Resize the window or use smaller font size.",
            "ERROR: Cannot print with the current font size. Use smaller font size.",
        };

        private final int DRAW_BYTES_ERROR = 0;
        private final int CANT_FIT_DRAW = 1;
        private final int CANT_FIT_PRINT = 2;

        /// Other variables
        private final Cursor blankCursor;

        public FontCanvas() {
            this.addMouseListener( this );
            this.addMouseMotionListener( this );
            this.setForeground( Color.black );
            this.setBackground( Color.white );

            /// Creates an invisble pointer by giving it bogus image
            /// Possibly find a workaround for this...
            Toolkit tk = Toolkit.getDefaultToolkit();
            byte[] bogus = { (byte) 0 };
            blankCursor =
              tk.createCustomCursor( tk.createImage( bogus ), new Point(0, 0), "" );

            zoomWindow = new JWindow( parent ) {
                public void paint( Graphics g ) {
                    g.drawImage( zoomImage, 0, 0, zoomWindow );
                }
            };
            zoomWindow.setCursor( blankCursor );
            zoomWindow.pack();
        }

        public boolean firstTime() { return firstTime; }
        public void refresh() {
            firstTime = false;
            repaint();
        }

        /// Sets the font, hints, according to the set parameters
        private void setParams( Graphics2D g2 ) {
            g2.setFont( testFont );
            g2.setRenderingHint(KEY_TEXT_ANTIALIASING, antiAliasType);
            g2.setRenderingHint(KEY_FRACTIONALMETRICS, fractionalMetricsType);
            g2.setRenderingHint(KEY_TEXT_LCD_CONTRAST, lcdContrast);
            /* I am preserving a somewhat dubious behaviour of this program.
             * Outline text would be drawn anti-aliased by setting the
             * graphics anti-aliasing hint if the text anti-aliasing hint
             * was set. The dubious element here is that people simply
             * using this program may think this is built-in behaviour
             * but its not - at least not when the app explicitly draws
             * outline text.
             * This becomes more dubious in cases such as "GASP" where the
             * size at which text is AA'ed is not something you can easily
             * calculate, so mimicing that behaviour isn't going to be easy.
             * So I precisely preserve the behaviour : this is done only
             * if the AA value is "ON". Its not applied in the other cases.
             */
            if (antiAliasType == VALUE_TEXT_ANTIALIAS_ON &&
                (drawMethod == TL_OUTLINE || drawMethod == GV_OUTLINE)) {
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            } else {
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
            }
        }

        /// Draws the grid (Used for unicode/glyph range drawing)
        private void drawGrid( Graphics2D g2 ) {
            int totalGridWidth = numCharAcross * gridWidth;
            int totalGridHeight = numCharDown * gridHeight;

            g2.setColor( Color.black );
            for ( int i = 0; i < numCharDown + 1; i++ )
              g2.drawLine( canvasInset_X, i * gridHeight + canvasInset_Y,
                           canvasInset_X + totalGridWidth, i * gridHeight + canvasInset_Y );
            for ( int i = 0; i < numCharAcross + 1; i++ )
              g2.drawLine( i * gridWidth + canvasInset_X, canvasInset_Y,
                           i * gridWidth + canvasInset_X, canvasInset_Y + totalGridHeight );
        }

        /// Draws one character at time onto the canvas according to
        /// the method requested (Used for RANGE_TEXT and ALL_GLYPHS)
        public void modeSpecificDrawChar( Graphics2D g2, int charCode,
                                          int baseX, int baseY ) {
            GlyphVector gv;
            int[] oneGlyph = { charCode };
            char[] charArray = Character.toChars( charCode );

            FontRenderContext frc = g2.getFontRenderContext();
            AffineTransform oldTX = g2.getTransform();

            /// Create GlyphVector to measure the exact visual advance
            /// Using that number, adjust the position of the character drawn
            if ( textToUse == ALL_GLYPHS )
              gv = testFont.createGlyphVector( frc, oneGlyph );
            else
              gv = testFont.createGlyphVector( frc, charArray );
            Rectangle2D r2d2 = gv.getPixelBounds(frc, 0, 0);
            int shiftedX = baseX;
            // getPixelBounds returns a result in device space.
            // we need to convert back to user space to be able to
            // calculate the shift as baseX is in user space.
            try {
                 double[] pt = new double[4];
                 pt[0] = r2d2.getX();
                 pt[1] = r2d2.getY();
                 pt[2] = r2d2.getX()+r2d2.getWidth();
                 pt[3] = r2d2.getY()+r2d2.getHeight();
                 oldTX.inverseTransform(pt,0,pt,0,2);
                 shiftedX = baseX - (int) ( pt[2] / 2 + pt[0] );
            } catch (NoninvertibleTransformException e) {
            }

            /// ABP - keep track of old tform, restore it later

            g2.translate( shiftedX, baseY );
            g2.transform( getAffineTransform( g2Transform ) );

            if ( textToUse == ALL_GLYPHS )
              g2.drawGlyphVector( gv, 0f, 0f );
            else {
                if ( testFont.canDisplay( charCode ))
                  g2.setColor( Color.black );
                else {
                  g2.setColor( Color.lightGray );
                }

                switch ( drawMethod ) {
                  case DRAW_STRING:
                    g2.drawString( new String( charArray ), 0, 0 );
                    break;
                  case DRAW_CHARS:
                    g2.drawChars( charArray, 0, 1, 0, 0 );
                    break;
                  case DRAW_BYTES:
                    if ( charCode > 0xff )
                      throw new CannotDrawException( DRAW_BYTES_ERROR );
                    byte[] oneByte = { (byte) charCode };
                    g2.drawBytes( oneByte, 0, 1, 0, 0 );
                    break;
                  case DRAW_GLYPHV:
                    g2.drawGlyphVector( gv, 0f, 0f );
                    break;
                  case TL_DRAW:
                    TextLayout tl = new TextLayout( new String( charArray ), testFont, frc );
                    tl.draw( g2, 0f, 0f );
                    break;
                  case GV_OUTLINE:
                    r2d2 = gv.getVisualBounds();
                    shiftedX = baseX - (int) ( r2d2.getWidth() / 2 + r2d2.getX() );
                    g2.draw( gv.getOutline( 0f, 0f ));
                    break;
                  case TL_OUTLINE:
                    r2d2 = gv.getVisualBounds();
                    shiftedX = baseX - (int) ( r2d2.getWidth() / 2 + r2d2.getX() );
                    TextLayout tlo =
                      new TextLayout( new String( charArray ), testFont,
                                      g2.getFontRenderContext() );
                    g2.draw( tlo.getOutline( null ));
                }
            }

            /// ABP - restore old tform
            g2.setTransform ( oldTX );
        }

        /// Draws one line of text at given position
        private void modeSpecificDrawLine( Graphics2D g2, String line,
                                           int baseX, int baseY ) {
            /// ABP - keep track of old tform, restore it later
            AffineTransform oldTx = null;
            oldTx = g2.getTransform();
            g2.translate( baseX, baseY );
            g2.transform( getAffineTransform( g2Transform ) );

            switch ( drawMethod ) {
              case DRAW_STRING:
                g2.drawString( line, 0, 0 );
                break;
              case DRAW_CHARS:
                g2.drawChars( line.toCharArray(), 0, line.length(), 0, 0 );
                break;
              case DRAW_BYTES:
                try {
                    byte[] lineBytes = line.getBytes( "ISO-8859-1" );
                    g2.drawBytes( lineBytes, 0, lineBytes.length, 0, 0 );
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                }
                break;
              case DRAW_GLYPHV:
                GlyphVector gv =
                  testFont.createGlyphVector( g2.getFontRenderContext(), line );
                g2.drawGlyphVector( gv, (float) 0, (float) 0 );
                break;
              case TL_DRAW:
                TextLayout tl = new TextLayout( line, testFont,
                                                g2.getFontRenderContext() );
                tl.draw( g2, (float) 0, (float) 0 );
                break;
              case GV_OUTLINE:
                GlyphVector gvo =
                  testFont.createGlyphVector( g2.getFontRenderContext(), line );
                g2.draw( gvo.getOutline( (float) 0, (float) 0 ));
                break;
              case TL_OUTLINE:
                TextLayout tlo =
                  new TextLayout( line, testFont,
                                  g2.getFontRenderContext() );
                AffineTransform at = new AffineTransform();
                g2.draw( tlo.getOutline( at ));
            }

            /// ABP - restore old tform
            g2.setTransform ( oldTx );

        }

        /// Draws one line of text at given position
        private void tlDrawLine( Graphics2D g2, TextLayout tl,
                                           float baseX, float baseY ) {
            /// ABP - keep track of old tform, restore it later
            AffineTransform oldTx = null;
            oldTx = g2.getTransform();
            g2.translate( baseX, baseY );
            g2.transform( getAffineTransform( g2Transform ) );

            tl.draw( g2, (float) 0, (float) 0 );

            /// ABP - restore old tform
            g2.setTransform ( oldTx );

        }


        /// If textToUse is set to range drawing, then convert
        /// int to hex string and prepends 0s to make it length 4
        /// Otherwise line number was fed; simply return number + 1 converted to String
        /// (This is because first line is 1, not 0)
        private String modeSpecificNumStr( int i ) {
            if ( textToUse == USER_TEXT || textToUse == FILE_TEXT )
              return String.valueOf( i + 1 );

            StringBuffer s = new StringBuffer( Integer.toHexString( i ));
            while ( s.length() < 4 )
              s.insert( 0, "0" );
            return s.toString().toUpperCase();
        }

        /// Resets the scrollbar to display correct range of text currently on screen
        /// (This scrollbar is not part of a "ScrollPane". It merely simulates its effect by
        ///  indicating the necessary area to be drawn within the panel.
        ///  By doing this, it prevents creating gigantic panel when large text range,
        ///  i.e. CJK Ideographs, is requested)
        private void resetScrollbar( int oldValue ) {
            int totalNumRows = 1, numCharToDisplay;
            if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                if ( textToUse == RANGE_TEXT )
                  numCharToDisplay = drawRange[1] - drawRange[0];
                else /// textToUse == ALL_GLYPHS
                  numCharToDisplay = testFont.getNumGlyphs();

                totalNumRows = numCharToDisplay / numCharAcross;
                if ( numCharToDisplay % numCharAcross != 0 )
                  totalNumRows++;
                if ( oldValue / numCharAcross > totalNumRows )
                  oldValue = 0;

                verticalBar.setValues( oldValue / numCharAcross,
                                       numCharDown, 0, totalNumRows );
            }
            else {
                if ( textToUse == USER_TEXT )
                  totalNumRows = userText.length;
                else /// textToUse == FILE_TEXT;
                  totalNumRows = lineBreakTLs.size();
                verticalBar.setValues( oldValue, numCharDown, 0, totalNumRows );
            }
            if ( totalNumRows <= numCharDown && drawStart == 0) {
              verticalBar.setEnabled( false );
            }
            else {
              verticalBar.setEnabled( true );
            }
        }

        /// Calculates the font's metrics that will be used for draw
        private void calcFontMetrics( Graphics2D g2d, int w, int h ) {
            FontMetrics fm;
            Graphics2D g2 = (Graphics2D)g2d.create();

            /// ABP
            if ( g2Transform != NONE && textToUse != FILE_TEXT ) {
                g2.setFont( g2.getFont().deriveFont( getAffineTransform( g2Transform )) );
                fm = g2.getFontMetrics();
            }
            else {
                fm = g2.getFontMetrics();
            }

            maxAscent = fm.getMaxAscent();
            maxDescent = fm.getMaxDescent();
            if (maxAscent == 0) maxAscent = 10;
            if (maxDescent == 0) maxDescent = 5;
            if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                /// Give slight extra room for each character
                maxAscent += 3;
                maxDescent += 3;
                gridWidth = fm.getMaxAdvance() + 6;
                gridHeight = maxAscent + maxDescent;
                if ( force16Cols )
                  numCharAcross = 16;
                else
                  numCharAcross = ( w - 10 ) / gridWidth;
                numCharDown = ( h - 10 ) / gridHeight;

                canvasInset_X = ( w - numCharAcross * gridWidth ) / 2;
                canvasInset_Y = ( h - numCharDown * gridHeight ) / 2;
                if ( numCharDown == 0 || numCharAcross == 0 )
                  throw new CannotDrawException( isPrinting ? CANT_FIT_PRINT : CANT_FIT_DRAW );

                if ( !isPrinting )
                  resetScrollbar( verticalBar.getValue() * numCharAcross );
            }
            else {
                maxDescent += fm.getLeading();
                canvasInset_X = 5;
                canvasInset_Y = 5;
                /// gridWidth and numCharAcross will not be used in this mode...
                gridHeight = maxAscent + maxDescent;
                numCharDown = ( h - canvasInset_Y * 2 ) / gridHeight;

                if ( numCharDown == 0 )
                  throw new CannotDrawException( isPrinting ? CANT_FIT_PRINT : CANT_FIT_DRAW );
                /// If this is text loaded from file, prepares the LineBreak'ed
                /// text layout at this point
                if ( textToUse == FILE_TEXT ) {
                    if ( !isPrinting )
                      f2dt.fireChangeStatus( "LineBreaking Text... Please Wait", false );
                    lineBreakTLs = new Vector<>();
                    for ( int i = 0; i < fileText.length; i++ ) {
                        AttributedString as =
                          new AttributedString( fileText[i], g2.getFont().getAttributes() );

                        LineBreakMeasurer lbm =
                          new LineBreakMeasurer( as.getIterator(), g2.getFontRenderContext() );

                        while ( lbm.getPosition() < fileText[i].length() )
                          lineBreakTLs.add( lbm.nextLayout( (float) w ));

                    }
                }
                if ( !isPrinting )
                  resetScrollbar( verticalBar.getValue() );
            }
        }

        /// Calculates the amount of text that will be displayed on screen
        private void calcTextRange() {
            String displaying = null;

            if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                if ( isPrinting )
                  if ( printMode == ONE_PAGE )
                    drawStart = currentlyShownChar;
                  else /// printMode == CUR_RANGE
                    drawStart = numCharAcross * numCharDown * printPageNumber;
                else
                  drawStart = verticalBar.getValue() * numCharAcross;
                if ( textToUse == RANGE_TEXT ) {
                    drawStart += drawRange[0];
                    drawLimit = drawRange[1];
                }
                else
                  drawLimit = testFont.getNumGlyphs();
                drawEnd = drawStart + numCharAcross * numCharDown - 1;

                if ( drawEnd >= drawLimit )
                  drawEnd = drawLimit;
            }
            else {
                if ( isPrinting )
                  if ( printMode == ONE_PAGE )
                    drawStart = currentlyShownChar;
                  else /// printMode == ALL_TEXT
                    drawStart = numCharDown * printPageNumber;
                else {
                    drawStart = verticalBar.getValue();
                }

                drawEnd = drawStart + numCharDown - 1;

                if ( textToUse == USER_TEXT )
                  drawLimit = userText.length - 1;
                else
                  drawLimit = lineBreakTLs.size() - 1;

                if ( drawEnd >= drawLimit )
                  drawEnd = drawLimit;
            }

            // ABP
            if ( drawStart > drawEnd ) {
              drawStart = 0;
              verticalBar.setValue(drawStart);
            }


            /// Change the status bar if not printing...
            if ( !isPrinting ) {
                backupStatusString = ( "Displaying" + MS_OPENING[textToUse] +
                                       modeSpecificNumStr( drawStart ) + " to " +
                                       modeSpecificNumStr( drawEnd ) +
                                       MS_CLOSING[textToUse] );
                f2dt.fireChangeStatus( backupStatusString, false );
            }
        }

        /// Draws text according to the parameters set by Font2DTest GUI
        private void drawText( Graphics g, int w, int h ) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.white);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.black);

            /// sets font, RenderingHints.
            setParams( g2 );

            /// If flag is set, recalculate fontMetrics and reset the scrollbar
            if ( updateFontMetrics || isPrinting ) {
                /// NOTE: re-calculates in case G2 transform
                /// is something other than NONE
                calcFontMetrics( g2, w, h );
                updateFontMetrics = false;
            }
            /// Calculate the amount of text that can be drawn...
            calcTextRange();

            /// Draw according to the set "Text to Use" mode
            if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                int charToDraw = drawStart;
                if ( showGrid )
                  drawGrid( g2 );

                for ( int i = 0; i < numCharDown && charToDraw <= drawEnd; i++ ) {
                  for ( int j = 0; j < numCharAcross && charToDraw <= drawEnd; j++, charToDraw++ ) {
                      int gridLocX = j * gridWidth + canvasInset_X;
                      int gridLocY = i * gridHeight + canvasInset_Y;

                      modeSpecificDrawChar( g2, charToDraw,
                                            gridLocX + gridWidth / 2,
                                            gridLocY + maxAscent );

                  }
                }
            }
            else if ( textToUse == USER_TEXT ) {
                g2.drawRect( 0, 0, w - 1, h - 1 );
                for ( int i = drawStart; i <= drawEnd; i++ ) {
                    int lineStartX = canvasInset_Y;
                    int lineStartY = ( i - drawStart ) * gridHeight + maxAscent;
                    modeSpecificDrawLine( g2, userText[i], lineStartX, lineStartY );
                }
            }
            else {
                float xPos, yPos = (float) canvasInset_Y;
                g2.drawRect( 0, 0, w - 1, h - 1 );
                for ( int i = drawStart; i <= drawEnd; i++ ) {
                    TextLayout oneLine = lineBreakTLs.elementAt( i );
                    xPos =
                      oneLine.isLeftToRight() ?
                      canvasInset_X : ( (float) w - oneLine.getAdvance() - canvasInset_X );

                    float[] fmData = {0, oneLine.getAscent(), 0, oneLine.getDescent(), 0, oneLine.getLeading()};
                    if (g2Transform != NONE) {
                        AffineTransform at = getAffineTransform(g2Transform);
                        at.transform( fmData, 0, fmData, 0, 3);
                    }
                    //yPos += oneLine.getAscent();
                    yPos += fmData[1]; // ascent
                    //oneLine.draw( g2, xPos, yPos );
                    tlDrawLine( g2, oneLine, xPos, yPos );
                    //yPos += oneLine.getDescent() + oneLine.getLeading();
                    yPos += fmData[3] + fmData[5]; // descent + leading
                }
            }
            g2.dispose();
        }

        /// Component paintComponent function...
        /// Draws/Refreshes canvas according to flag(s) set by other functions
        public void paintComponent( Graphics g ) {
              super.paintComponent(g);

                Dimension d = this.getSize();
                isPrinting = false;
                try {
                    drawText( g, d.width, d.height );
                }
                catch ( CannotDrawException e ) {
                    super.paintComponent(g);
                    f2dt.fireChangeStatus( ERRORS[ e.id ], true );
                    return;
                }

            showingError = false;
        }

        /// Printable interface function
        /// Component print function...
        public int print( Graphics g, PageFormat pf, int pageIndex ) {
            if ( pageIndex == 0 ) {
                /// Reset the last page index to max...
                lastPage = Integer.MAX_VALUE;
                currentlyShownChar = verticalBar.getValue() * numCharAcross;
            }

            if ( printMode == ONE_PAGE ) {
                if ( pageIndex > 0 )
                  return NO_SUCH_PAGE;
            }
            else {
                if ( pageIndex > lastPage )
                  return NO_SUCH_PAGE;
            }

            int pageWidth = (int) pf.getImageableWidth();
            int pageHeight = (int) pf.getImageableHeight();
            /// Back up metrics and other drawing info before printing modifies it
            int backupDrawStart = drawStart, backupDrawEnd = drawEnd;
            int backupNumCharAcross = numCharAcross, backupNumCharDown = numCharDown;
            Vector<TextLayout> backupLineBreakTLs = null;
            if ( textToUse == FILE_TEXT )
              backupLineBreakTLs = new Vector<>(lineBreakTLs);

            printPageNumber = pageIndex;
            isPrinting = true;
            /// Push the actual draw area 60 down to allow info to be printed
            g.translate( (int) pf.getImageableX(), (int) pf.getImageableY() + 60 );
            try {
                drawText( g, pageWidth, pageHeight - 60 );
            }
            catch ( CannotDrawException e ) {
                f2dt.fireChangeStatus( ERRORS[ e.id ], true );
                return NO_SUCH_PAGE;
            }

            /// Draw information about what is being printed
            String hints = ( " with antialias " + antiAliasType + "and" +
                             " fractional metrics " + fractionalMetricsType +
                             " and lcd contrast = " + lcdContrast);
            String infoLine1 = ( "Printing" + MS_OPENING[textToUse] +
                                 modeSpecificNumStr( drawStart ) + " to " +
                                 modeSpecificNumStr( drawEnd ) + MS_CLOSING[textToUse] );
            String infoLine2 = ( "With " + fontName + " " + STYLES[fontStyle] + " at " +
                                 fontSize + " point size " + TRANSFORMS[fontTransform] );
            String infoLine3 = "Using " + METHODS[drawMethod] + hints;
            String infoLine4 = "Page: " + ( pageIndex + 1 );
            g.setFont( new Font( "dialog", Font.PLAIN, 12 ));
            g.setColor( Color.black );
            g.translate( 0, -60 );
            g.drawString( infoLine1, 15, 10 );
            g.drawString( infoLine2, 15, 22 );
            g.drawString( infoLine3, 15, 34 );
            g.drawString( infoLine4, 15, 46 );

            if ( drawEnd == drawLimit )
              /// This indicates that the draw will be completed with this page
              lastPage = pageIndex;

            /// Restore the changed values back...
            /// This is important for JScrollBar settings and LineBreak'ed TLs
            drawStart = backupDrawStart;
            drawEnd = backupDrawEnd;
            numCharAcross = backupNumCharAcross;
            numCharDown = backupNumCharDown;
            if ( textToUse == FILE_TEXT )
              lineBreakTLs = backupLineBreakTLs;
            return PAGE_EXISTS;
        }

        /// Ouputs the current canvas into a given PNG file
        public void writePNG( String fileName ) {
            try {
                int w = this.getSize().width;
                int h = this.getSize().height;
                BufferedImage buffer = (BufferedImage) this.createImage( w, h );
                Graphics2D g2 = buffer.createGraphics();
                g2.setColor(Color.white);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.black);
                updateFontMetrics = true;
                drawText(g2, w, h);
                updateFontMetrics = true;
                ImageIO.write(buffer, "png", new java.io.File(fileName));
            }
            catch ( Exception e ) {
                f2dt.fireChangeStatus( "ERROR: Failed to Save PNG image; See stack trace", true );
                e.printStackTrace();
            }
        }

        /// Figures out whether a character at the pointer location is valid
        /// And if so, updates mouse location informations, as well as
        /// the information on the status bar
        private boolean checkMouseLoc( MouseEvent e ) {
            if ( gridWidth != 0 && gridHeight != 0 )
              if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                  int charLocX = ( e.getX() - canvasInset_X ) / gridWidth;
                  int charLocY = ( e.getY() - canvasInset_Y ) / gridHeight;

                  /// Check to make sure the mouse click location is within drawn area
                  if ( charLocX >= 0 && charLocY >= 0 &&
                       charLocX < numCharAcross && charLocY < numCharDown ) {
                      int mouseOverChar =
                        charLocX + ( verticalBar.getValue() + charLocY ) * numCharAcross;
                      if ( textToUse == RANGE_TEXT )
                        mouseOverChar += drawRange[0];
                      if ( mouseOverChar > drawEnd )
                        return false;

                      mouseOverCharX = charLocX;
                      mouseOverCharY = charLocY;
                      currMouseOverChar = mouseOverChar;
                      /// Update status bar
                      f2dt.fireChangeStatus( "Pointing to" + MS_OPENING[textToUse] +
                                             modeSpecificNumStr( mouseOverChar ), false );
                      return true;
                  }
              }
            return false;
        }

        /// Shows (updates) the character zoom window
        public void showZoomed() {
            GlyphVector gv;
            Font backup = testFont;
            Point canvasLoc = this.getLocationOnScreen();

            /// Calculate the zoom area's location and size...
            int dialogOffsetX = (int) ( gridWidth * ( ZOOM - 1 ) / 2 );
            int dialogOffsetY = (int) ( gridHeight * ( ZOOM - 1 ) / 2 );
            int zoomAreaX =
              mouseOverCharX * gridWidth + canvasInset_X - dialogOffsetX;
            int zoomAreaY =
              mouseOverCharY * gridHeight + canvasInset_Y - dialogOffsetY;
            int zoomAreaWidth = (int) ( gridWidth * ZOOM );
            int zoomAreaHeight = (int) ( gridHeight * ZOOM );

            /// Position and set size of zoom window as needed
            zoomWindow.setLocation( canvasLoc.x + zoomAreaX, canvasLoc.y + zoomAreaY );
            if ( !nowZooming ) {
                if ( zoomWindow.getWarningString() != null )
                  /// If this is not opened as a "secure" window,
                  /// it has a banner below the zoom dialog which makes it look really BAD
                  /// So enlarge it by a bit
                  zoomWindow.setSize( zoomAreaWidth + 1, zoomAreaHeight + 20 );
                else
                  zoomWindow.setSize( zoomAreaWidth + 1, zoomAreaHeight + 1 );
            }

            /// Prepare zoomed image
            zoomImage =
              (BufferedImage) zoomWindow.createImage( zoomAreaWidth + 1,
                                                      zoomAreaHeight + 1 );
            Graphics2D g2 = (Graphics2D) zoomImage.getGraphics();
            testFont = testFont.deriveFont( fontSize * ZOOM );
            setParams( g2 );
            g2.setColor( Color.white );
            g2.fillRect( 0, 0, zoomAreaWidth, zoomAreaHeight );
            g2.setColor( Color.black );
            g2.drawRect( 0, 0, zoomAreaWidth, zoomAreaHeight );
            modeSpecificDrawChar( g2, currMouseOverChar,
                                  zoomAreaWidth / 2, (int) ( maxAscent * ZOOM ));
            g2.dispose();
            if ( !nowZooming )
              zoomWindow.setVisible(true);
            /// This is sort of redundant... since there is a paint function
            /// inside zoomWindow definition that does the drawImage.
            /// (I should be able to call just repaint() here)
            /// However, for some reason, that paint function fails to respond
            /// from second time and on; So I have to force the paint here...
            zoomWindow.getGraphics().drawImage( zoomImage, 0, 0, this );

            nowZooming = true;
            prevZoomChar = currMouseOverChar;
            testFont = backup;

            // Windows does not repaint correctly, after
            // a zoom. Thus, we need to force the canvas
            // to repaint, but only once. After the first repaint,
            // everything stabilizes. [ABP]
            if ( firstTime() ) {
                refresh();
            }
        }

        /// Listener Functions

        /// MouseListener interface function
        /// Zooms a character when mouse is pressed above it
        public void mousePressed( MouseEvent e ) {
            if ( !showingError) {
                if ( checkMouseLoc( e )) {
                    showZoomed();
                    this.setCursor( blankCursor );
                }
            }
        }

        /// MouseListener interface function
        /// Redraws the area that was drawn over by zoomed character
        public void mouseReleased( MouseEvent e ) {
            if ( textToUse == RANGE_TEXT || textToUse == ALL_GLYPHS ) {
                if ( nowZooming )
                  zoomWindow.setVisible(false);
                nowZooming = false;
            }
            this.setCursor( Cursor.getDefaultCursor() );
        }

        /// MouseListener interface function
        /// Resets the status bar to display range instead of a specific character
        public void mouseExited( MouseEvent e ) {
            if ( !showingError && !nowZooming )
              f2dt.fireChangeStatus( backupStatusString, false );
        }

        /// MouseMotionListener interface function
        /// Adjusts the status bar message when mouse moves over a character
        public void mouseMoved( MouseEvent e ) {
            if ( !showingError ) {
                if ( !checkMouseLoc( e ))
                  f2dt.fireChangeStatus( backupStatusString, false );
            }
        }

        /// MouseMotionListener interface function
        /// Scrolls the zoomed character when mouse is dragged
        public void mouseDragged( MouseEvent e ) {
            if ( !showingError )
              if ( nowZooming ) {
                  if ( checkMouseLoc( e ) && currMouseOverChar != prevZoomChar )
                    showZoomed();
              }
        }

        /// Empty function to comply with interface requirement
        public void mouseClicked( MouseEvent e ) {}
        public void mouseEntered( MouseEvent e ) {}
    }

    private final class CannotDrawException extends RuntimeException {
        /// Error ID
        public final int id;

        public CannotDrawException( int i ) {
            id = i;
        }
    }

    enum FMValues {
       FMDEFAULT ("DEFAULT",  VALUE_FRACTIONALMETRICS_DEFAULT),
       FMOFF     ("OFF",      VALUE_FRACTIONALMETRICS_OFF),
       FMON      ("ON",       VALUE_FRACTIONALMETRICS_ON);

        private String name;
        private Object hint;

        private static FMValues[] valArray;

        FMValues(String s, Object o) {
            name = s;
            hint = o;
        }

        public String toString() {
            return name;
        }

       public Object getHint() {
           return hint;
       }
       public static Object getValue(int ordinal) {
           if (valArray == null) {
               valArray = FMValues.values();
           }
           for (int i=0;i<valArray.length;i++) {
               if (valArray[i].ordinal() == ordinal) {
                   return valArray[i];
               }
           }
           return valArray[0];
       }
       private static FMValues[] getArray() {
           if (valArray == null) {
               valArray = FMValues.values();
           }
           return valArray;
       }

       public static int getHintVal(Object hint) {
           getArray();
           for (int i=0;i<valArray.length;i++) {
               if (valArray[i].getHint() == hint) {
                   return i;
               }
           }
           return 0;
       }
    }

   enum AAValues {
       AADEFAULT ("DEFAULT",  VALUE_TEXT_ANTIALIAS_DEFAULT),
       AAOFF     ("OFF",      VALUE_TEXT_ANTIALIAS_OFF),
       AAON      ("ON",       VALUE_TEXT_ANTIALIAS_ON),
       AAGASP    ("GASP",     VALUE_TEXT_ANTIALIAS_GASP),
       AALCDHRGB ("LCD_HRGB", VALUE_TEXT_ANTIALIAS_LCD_HRGB),
       AALCDHBGR ("LCD_HBGR", VALUE_TEXT_ANTIALIAS_LCD_HBGR),
       AALCDVRGB ("LCD_VRGB", VALUE_TEXT_ANTIALIAS_LCD_VRGB),
       AALCDVBGR ("LCD_VBGR", VALUE_TEXT_ANTIALIAS_LCD_VBGR);

        private String name;
        private Object hint;

        private static AAValues[] valArray;

        AAValues(String s, Object o) {
            name = s;
            hint = o;
        }

        public String toString() {
            return name;
        }

       public Object getHint() {
           return hint;
       }

       public static boolean isLCDMode(Object o) {
           return (o instanceof AAValues &&
                   ((AAValues)o).ordinal() >= AALCDHRGB.ordinal());
       }

       public static Object getValue(int ordinal) {
           if (valArray == null) {
               valArray = AAValues.values();
           }
           for (int i=0;i<valArray.length;i++) {
               if (valArray[i].ordinal() == ordinal) {
                   return valArray[i];
               }
           }
           return valArray[0];
       }

       private static AAValues[] getArray() {
           if (valArray == null) {
               valArray = AAValues.values();
           }
           return valArray;
       }

       public static int getHintVal(Object hint) {
           getArray();
           for (int i=0;i<valArray.length;i++) {
               if (valArray[i].getHint() == hint) {
                   return i;
               }
           }
           return 0;
       }

    }

    private static Integer defaultContrast;
    static Integer getDefaultLCDContrast() {
        if (defaultContrast == null) {
            GraphicsConfiguration gc =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
        Graphics2D g2d =
            (Graphics2D)(gc.createCompatibleImage(1,1).getGraphics());
        defaultContrast = (Integer)
            g2d.getRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST);
        }
        return defaultContrast;
    }
}
