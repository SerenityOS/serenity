/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Shape;

import java.awt.font.FontRenderContext;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.awt.print.Pageable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.awt.print.PrinterJob;

import javax.print.PrintService;
import javax.print.StreamPrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Sides;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import sun.awt.CharsetString;
import sun.awt.FontConfiguration;
import sun.awt.PlatformFont;
import sun.awt.SunToolkit;
import sun.font.FontAccess;
import sun.font.FontUtilities;

import java.nio.charset.*;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;

//REMIND: Remove use of this class when IPPPrintService is moved to share directory.
import java.lang.reflect.Method;
import javax.print.attribute.Attribute;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;

/**
 * A class which initiates and executes a PostScript printer job.
 *
 * @author Richard Blanchard
 */
public class PSPrinterJob extends RasterPrinterJob {

 /* Class Constants */

    /**
     * Passed to the {@code setFillMode}
     * method this value forces fills to be
     * done using the even-odd fill rule.
     */
    protected static final int FILL_EVEN_ODD = 1;

    /**
     * Passed to the {@code setFillMode}
     * method this value forces fills to be
     * done using the non-zero winding rule.
     */
    protected static final int FILL_WINDING = 2;

    /* PostScript has a 64K maximum on its strings.
     */
    private static final int MAX_PSSTR = (1024 * 64 - 1);

    private static final int RED_MASK = 0x00ff0000;
    private static final int GREEN_MASK = 0x0000ff00;
    private static final int BLUE_MASK = 0x000000ff;

    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 0;

    private static final int LOWNIBBLE_MASK = 0x0000000f;
    private static final int HINIBBLE_MASK =  0x000000f0;
    private static final int HINIBBLE_SHIFT = 4;
    private static final byte[] hexDigits = {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3',
        (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'A', (byte)'B',
        (byte)'C', (byte)'D', (byte)'E', (byte)'F'
    };

    private static final int PS_XRES = 300;
    private static final int PS_YRES = 300;

    private static final String ADOBE_PS_STR =  "%!PS-Adobe-3.0";
    private static final String EOF_COMMENT =   "%%EOF";
    private static final String PAGE_COMMENT =  "%%Page: ";

    private static final String READIMAGEPROC = "/imStr 0 def /imageSrc " +
        "{currentfile /ASCII85Decode filter /RunLengthDecode filter " +
        " imStr readstring pop } def";

    private static final String COPIES =        "/#copies exch def";
    private static final String PAGE_SAVE =     "/pgSave save def";
    private static final String PAGE_RESTORE =  "pgSave restore";
    private static final String SHOWPAGE =      "showpage";
    private static final String IMAGE_SAVE =    "/imSave save def";
    private static final String IMAGE_STR =     " string /imStr exch def";
    private static final String IMAGE_RESTORE = "imSave restore";

    private static final String SetFontName = "F";

    private static final String DrawStringName = "S";

    /**
     * The PostScript invocation to fill a path using the
     * even-odd rule. (eofill)
     */
    private static final String EVEN_ODD_FILL_STR = "EF";

    /**
     * The PostScript invocation to fill a path using the
     * non-zero winding rule. (fill)
     */
    private static final String WINDING_FILL_STR = "WF";

    /**
     * The PostScript to set the clip to be the current path
     * using the even odd rule. (eoclip)
     */
    private static final String EVEN_ODD_CLIP_STR = "EC";

    /**
     * The PostScript to set the clip to be the current path
     * using the non-zero winding rule. (clip)
     */
    private static final String WINDING_CLIP_STR = "WC";

    /**
     * Expecting two numbers on the PostScript stack, this
     * invocation moves the current pen position. (moveto)
     */
    private static final String MOVETO_STR = " M";
    /**
     * Expecting two numbers on the PostScript stack, this
     * invocation draws a PS line from the current pen
     * position to the point on the stack. (lineto)
     */
    private static final String LINETO_STR = " L";

    /**
     * This PostScript operator takes two control points
     * and an ending point and using the current pen
     * position as a starting point adds a bezier
     * curve to the current path. (curveto)
     */
    private static final String CURVETO_STR = " C";

    /**
     * The PostScript to pop a state off of the printer's
     * gstate stack. (grestore)
     */
    private static final String GRESTORE_STR = "R";
    /**
     * The PostScript to push a state on to the printer's
     * gstate stack. (gsave)
     */
    private static final String GSAVE_STR = "G";

    /**
     * Make the current PostScript path an empty path. (newpath)
     */
    private static final String NEWPATH_STR = "N";

    /**
     * Close the current subpath by generating a line segment
     * from the current position to the start of the subpath. (closepath)
     */
    private static final String CLOSEPATH_STR = "P";

    /**
     * Use the three numbers on top of the PS operator
     * stack to set the rgb color. (setrgbcolor)
     */
    private static final String SETRGBCOLOR_STR = " SC";

    /**
     * Use the top number on the stack to set the printer's
     * current gray value. (setgray)
     */
    private static final String SETGRAY_STR = " SG";

 /* Instance Variables */

   private int mDestType;

   private String mDestination = "lp";

   private boolean mNoJobSheet = false;

   private String mOptions;

   private Font mLastFont;

   private Color mLastColor;

   private Shape mLastClip;

   private AffineTransform mLastTransform;

   private double xres = PS_XRES;
   private double yres = PS_XRES;

   /* non-null if printing EPS for Java Plugin */
   private EPSPrinter epsPrinter = null;

   /**
    * The metrics for the font currently set.
    */
   FontMetrics mCurMetrics;

   /**
    * The output stream to which the generated PostScript
    * is written.
    */
   PrintStream mPSStream;

   /* The temporary file to which we spool before sending to the printer  */

   File spoolFile;

   /**
    * This string holds the PostScript operator to
    * be used to fill a path. It can be changed
    * by the {@code setFillMode} method.
    */
    private String mFillOpStr = WINDING_FILL_STR;

   /**
    * This string holds the PostScript operator to
    * be used to clip to a path. It can be changed
    * by the {@code setFillMode} method.
    */
    private String mClipOpStr = WINDING_CLIP_STR;

   /**
    * A stack that represents the PostScript gstate stack.
    */
   ArrayList<GState> mGStateStack = new ArrayList<>();

   /**
    * The x coordinate of the current pen position.
    */
   private float mPenX;

   /**
    * The y coordinate of the current pen position.
    */
   private float mPenY;

   /**
    * The x coordinate of the starting point of
    * the current subpath.
    */
   private float mStartPathX;

   /**
    * The y coordinate of the starting point of
    * the current subpath.
    */
   private float mStartPathY;

   /**
    * An optional mapping of fonts to PostScript names.
    */
   private static Properties mFontProps = null;

   private static boolean isMac;

    /* Class static initialiser block */
    static {
        initStatic();
    }

    @SuppressWarnings("removal")
    private static void initStatic() {
       //enable priviledges so initProps can access system properties,
        // open the property file, etc.
        java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction<Object>() {
            public Object run() {
                mFontProps = initProps();
                String osName = System.getProperty("os.name");
                isMac = osName.startsWith("Mac");
                return null;
            }
        });
    }

    /*
     * Initialize PostScript font properties.
     * Copied from PSPrintStream
     */
    private static Properties initProps() {
        // search psfont.properties for fonts
        // and create and initialize fontProps if it exist.

        String jhome = System.getProperty("java.home");

        if (jhome != null){
            String ulocale = SunToolkit.getStartupLocale().getLanguage();
            try {

                File f = new File(jhome + File.separator +
                                  "lib" + File.separator +
                                  "psfontj2d.properties." + ulocale);

                if (!f.canRead()){

                    f = new File(jhome + File.separator +
                                      "lib" + File.separator +
                                      "psfont.properties." + ulocale);
                    if (!f.canRead()){

                        f = new File(jhome + File.separator + "lib" +
                                     File.separator + "psfontj2d.properties");

                        if (!f.canRead()){

                            f = new File(jhome + File.separator + "lib" +
                                         File.separator + "psfont.properties");

                            if (!f.canRead()){
                                return (Properties)null;
                            }
                        }
                    }
                }

                // Load property file
                InputStream in =
                    new BufferedInputStream(new FileInputStream(f.getPath()));
                Properties props = new Properties();
                props.load(in);
                in.close();
                return props;
            } catch (Exception e){
                return (Properties)null;
            }
        }
        return (Properties)null;
    }

 /* Constructors */

    public PSPrinterJob()
    {
    }

 /* Instance Methods */

   /**
     * Presents the user a dialog for changing properties of the
     * print job interactively.
     * @return false if the user cancels the dialog and
     *         true otherwise.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public boolean printDialog() throws HeadlessException {

        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        if (attributes == null) {
            attributes = new HashPrintRequestAttributeSet();
        }
        attributes.add(new Copies(getCopies()));
        attributes.add(new JobName(getJobName(), null));

        boolean doPrint = false;
        DialogTypeSelection dts =
            (DialogTypeSelection)attributes.get(DialogTypeSelection.class);
        if (dts == DialogTypeSelection.NATIVE) {
            // Remove DialogTypeSelection.NATIVE to prevent infinite loop in
            // RasterPrinterJob.
            attributes.remove(DialogTypeSelection.class);
            doPrint = printDialog(attributes);
            // restore attribute
            attributes.add(DialogTypeSelection.NATIVE);
        } else {
            doPrint = printDialog(attributes);
        }

        if (doPrint) {
            JobName jobName = (JobName)attributes.get(JobName.class);
            if (jobName != null) {
                setJobName(jobName.getValue());
            }
            Copies copies = (Copies)attributes.get(Copies.class);
            if (copies != null) {
                setCopies(copies.getValue());
            }

            Destination dest = (Destination)attributes.get(Destination.class);

            if (dest != null) {
                try {
                    mDestType = RasterPrinterJob.FILE;
                    mDestination = (new File(dest.getURI())).getPath();
                } catch (Exception e) {
                    mDestination = "out.ps";
                }
            } else {
                mDestType = RasterPrinterJob.PRINTER;
                PrintService pServ = getPrintService();
                if (pServ != null) {
                    mDestination = pServ.getName();
                   if (isMac) {
                        PrintServiceAttributeSet psaSet = pServ.getAttributes() ;
                        if (psaSet != null) {
                            mDestination = psaSet.get(PrinterName.class).toString();
                        }
                    }
                }
            }
        }

        return doPrint;
    }

    @Override
    protected void setAttributes(PrintRequestAttributeSet attributes)
                                 throws PrinterException {
        super.setAttributes(attributes);
        if (attributes == null) {
            return; // now always use attributes, so this shouldn't happen.
        }
        Attribute attr = attributes.get(Media.class);
        if (attr instanceof CustomMediaTray) {
            CustomMediaTray customTray = (CustomMediaTray)attr;
            String choice = customTray.getChoiceName();
            if (choice != null) {
                mOptions = " InputSlot="+ choice;
            }
        }
    }

    /**
     * Invoked by the RasterPrinterJob super class
     * this method is called to mark the start of a
     * document.
     */
    @SuppressWarnings("removal")
    protected void startDoc() throws PrinterException {

        // A security check has been performed in the
        // java.awt.print.printerJob.getPrinterJob method.
        // We use an inner class to execute the privilged open operations.
        // Note that we only open a file if it has been nominated by
        // the end-user in a dialog that we ouselves put up.

        OutputStream output = null;

        if (epsPrinter == null) {
            if (getPrintService() instanceof PSStreamPrintService) {
                StreamPrintService sps = (StreamPrintService)getPrintService();
                mDestType = RasterPrinterJob.STREAM;
                if (sps.isDisposed()) {
                    throw new PrinterException("service is disposed");
                }
                output = sps.getOutputStream();
                if (output == null) {
                    throw new PrinterException("Null output stream");
                }
            } else {
                /* REMIND: This needs to be more maintainable */
                mNoJobSheet = super.noJobSheet;
                if (super.destinationAttr != null) {
                    mDestType = RasterPrinterJob.FILE;
                    mDestination = super.destinationAttr;
                }
                if (mDestType == RasterPrinterJob.FILE) {
                    try {
                        spoolFile = new File(mDestination);
                        output =  new FileOutputStream(spoolFile);
                    } catch (IOException ex) {
                        abortDoc();
                        throw new PrinterIOException(ex);
                    }
                } else {
                    PrinterOpener po = new PrinterOpener();
                    java.security.AccessController.doPrivileged(po);
                    if (po.pex != null) {
                        throw po.pex;
                    }
                    output = po.result;
                }
            }

            mPSStream = new PrintStream(new BufferedOutputStream(output));
            mPSStream.println(ADOBE_PS_STR);
        }

        mPSStream.println("%%BeginProlog");
        mPSStream.println(READIMAGEPROC);
        mPSStream.println("/BD {bind def} bind def");
        mPSStream.println("/D {def} BD");
        mPSStream.println("/C {curveto} BD");
        mPSStream.println("/L {lineto} BD");
        mPSStream.println("/M {moveto} BD");
        mPSStream.println("/R {grestore} BD");
        mPSStream.println("/G {gsave} BD");
        mPSStream.println("/N {newpath} BD");
        mPSStream.println("/P {closepath} BD");
        mPSStream.println("/EC {eoclip} BD");
        mPSStream.println("/WC {clip} BD");
        mPSStream.println("/EF {eofill} BD");
        mPSStream.println("/WF {fill} BD");
        mPSStream.println("/SG {setgray} BD");
        mPSStream.println("/SC {setrgbcolor} BD");
        mPSStream.println("/ISOF {");
        mPSStream.println("     dup findfont dup length 1 add dict begin {");
        mPSStream.println("             1 index /FID eq {pop pop} {D} ifelse");
        mPSStream.println("     } forall /Encoding ISOLatin1Encoding D");
        mPSStream.println("     currentdict end definefont");
        mPSStream.println("} BD");
        mPSStream.println("/NZ {dup 1 lt {pop 1} if} BD");
        /* The following procedure takes args: string, x, y, desiredWidth.
         * It calculates using stringwidth the width of the string in the
         * current font and subtracts it from the desiredWidth and divides
         * this by stringLen-1. This gives us a per-glyph adjustment in
         * the spacing needed (either +ve or -ve) to make the string
         * print at the desiredWidth. The ashow procedure call takes this
         * per-glyph adjustment as an argument. This is necessary for WYSIWYG
         */
        mPSStream.println("/"+DrawStringName +" {");
        mPSStream.println("     moveto 1 index stringwidth pop NZ sub");
        mPSStream.println("     1 index length 1 sub NZ div 0");
        mPSStream.println("     3 2 roll ashow newpath} BD");
        mPSStream.println("/FL [");
        if (mFontProps == null){
            mPSStream.println(" /Helvetica ISOF");
            mPSStream.println(" /Helvetica-Bold ISOF");
            mPSStream.println(" /Helvetica-Oblique ISOF");
            mPSStream.println(" /Helvetica-BoldOblique ISOF");
            mPSStream.println(" /Times-Roman ISOF");
            mPSStream.println(" /Times-Bold ISOF");
            mPSStream.println(" /Times-Italic ISOF");
            mPSStream.println(" /Times-BoldItalic ISOF");
            mPSStream.println(" /Courier ISOF");
            mPSStream.println(" /Courier-Bold ISOF");
            mPSStream.println(" /Courier-Oblique ISOF");
            mPSStream.println(" /Courier-BoldOblique ISOF");
        } else {
            int cnt = Integer.parseInt(mFontProps.getProperty("font.num", "9"));
            for (int i = 0; i < cnt; i++){
                mPSStream.println("    /" + mFontProps.getProperty
                           ("font." + String.valueOf(i), "Courier ISOF"));
            }
        }
        mPSStream.println("] D");

        mPSStream.println("/"+SetFontName +" {");
        mPSStream.println("     FL exch get exch scalefont");
        mPSStream.println("     [1 0 0 -1 0 0] makefont setfont} BD");

        mPSStream.println("%%EndProlog");

        mPSStream.println("%%BeginSetup");
        if (epsPrinter == null) {
            // Set Page Size using first page's format.
            PageFormat pageFormat = getPageable().getPageFormat(0);
            double paperHeight = pageFormat.getPaper().getHeight();
            double paperWidth = pageFormat.getPaper().getWidth();

            /* PostScript printers can always generate uncollated copies.
             */
            mPSStream.print("<< /PageSize [" +
                                           paperWidth + " "+ paperHeight+"]");

            final PrintService pservice = getPrintService();
            Boolean isPS = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Boolean>() {
                    public Boolean run() {
                       try {
                           Class<?> psClass = Class.forName("sun.print.IPPPrintService");
                           if (psClass.isInstance(pservice)) {
                               Method isPSMethod = psClass.getMethod("isPostscript",
                                                                     (Class[])null);
                               return (Boolean)isPSMethod.invoke(pservice, (Object[])null);
                           }
                       } catch (Throwable t) {
                       }
                       return Boolean.TRUE;
                    }
                }
            );
            if (isPS) {
                mPSStream.print(" /DeferredMediaSelection true");
            }

            mPSStream.print(" /ImagingBBox null /ManualFeed false");
            mPSStream.print(isCollated() ? " /Collate true":"");
            mPSStream.print(" /NumCopies " +getCopiesInt());

            if (sidesAttr != Sides.ONE_SIDED) {
                if (sidesAttr == Sides.TWO_SIDED_LONG_EDGE) {
                    mPSStream.print(" /Duplex true ");
                } else if (sidesAttr == Sides.TWO_SIDED_SHORT_EDGE) {
                    mPSStream.print(" /Duplex true /Tumble true ");
                }
            }
            mPSStream.println(" >> setpagedevice ");
        }
        mPSStream.println("%%EndSetup");
    }

    // Inner class to run "privileged" to open the printer output stream.

    private class PrinterOpener implements java.security.PrivilegedAction<OutputStream> {
        PrinterException pex;
        OutputStream result;

        public OutputStream run() {
            try {

                    /* Write to a temporary file which will be spooled to
                     * the printer then deleted. In the case that the file
                     * is not removed for some reason, request that it is
                     * removed when the VM exits.
                     */
                    spoolFile = Files.createTempFile("javaprint", ".ps").toFile();
                    spoolFile.deleteOnExit();

                result = new FileOutputStream(spoolFile);
                return result;
            } catch (IOException ex) {
                // If there is an IOError we subvert it to a PrinterException.
                pex = new PrinterIOException(ex);
            }
            return null;
        }
    }

    // Inner class to run "privileged" to invoke the system print command

    private class PrinterSpooler implements java.security.PrivilegedAction<Object> {
        PrinterException pex;

        private void handleProcessFailure(final Process failedProcess,
                final String[] execCmd, final int result) throws IOException {
            try (StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw)) {
                pw.append("error=").append(Integer.toString(result));
                pw.append(" running:");
                for (String arg: execCmd) {
                    pw.append(" '").append(arg).append("'");
                }
                try (InputStream is = failedProcess.getErrorStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr)) {
                    while (br.ready()) {
                        pw.println();
                        pw.append("\t\t").append(br.readLine());
                    }
                } finally {
                    pw.flush();
                }
                throw new IOException(sw.toString());
            }
        }

        public Object run() {
            if (spoolFile == null || !spoolFile.exists()) {
               pex = new PrinterException("No spool file");
               return null;
            }
            try {
                /**
                 * Spool to the printer.
                 */
                String fileName = spoolFile.getAbsolutePath();
                String[] execCmd = printExecCmd(mDestination, mOptions,
                               mNoJobSheet, getJobNameInt(),
                                                1, fileName);

                Process process = Runtime.getRuntime().exec(execCmd);
                process.waitFor();
                final int result = process.exitValue();
                if (0 != result) {
                    handleProcessFailure(process, execCmd, result);
                }
            } catch (IOException ex) {
                pex = new PrinterIOException(ex);
            } catch (InterruptedException ie) {
                pex = new PrinterException(ie.toString());
            } finally {
                spoolFile.delete();
            }
            return null;
        }
    }


    /**
     * Invoked if the application cancelled the printjob.
     */
    @SuppressWarnings("removal")
    protected void abortDoc() {
        if (mPSStream != null && mDestType != RasterPrinterJob.STREAM) {
            mPSStream.close();
        }
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {

            public Object run() {
               if (spoolFile != null && spoolFile.exists()) {
                   spoolFile.delete();
               }
               return null;
            }
        });
    }

    /**
     * Invoked by the RasterPrintJob super class
     * this method is called after that last page
     * has been imaged.
     */
    @SuppressWarnings("removal")
    protected void endDoc() throws PrinterException {
        if (mPSStream != null) {
            mPSStream.println(EOF_COMMENT);
            mPSStream.flush();
            if (mPSStream.checkError()) {
                abortDoc();
                throw new PrinterException("Error while writing to file");
            }
            if (mDestType != RasterPrinterJob.STREAM) {
                mPSStream.close();
            }
        }
        if (mDestType == RasterPrinterJob.PRINTER) {
            PrintService pServ = getPrintService();
            if (pServ != null) {
                mDestination = pServ.getName();
               if (isMac) {
                    PrintServiceAttributeSet psaSet = pServ.getAttributes();
                    if (psaSet != null) {
                        mDestination = psaSet.get(PrinterName.class).toString() ;
                    }
                }
            }
            PrinterSpooler spooler = new PrinterSpooler();
            java.security.AccessController.doPrivileged(spooler);
            if (spooler.pex != null) {
                throw spooler.pex;
            }
        }
    }

    private String getCoordPrep() {
        return " 0 exch translate "
             + "1 -1 scale"
             + "[72 " + getXRes() + " div "
             + "0 0 "
             + "72 " + getYRes() + " div "
             + "0 0]concat";
    }

    /**
     * The RasterPrintJob super class calls this method
     * at the start of each page.
     */
    protected void startPage(PageFormat pageFormat, Printable painter,
                             int index, boolean paperChanged)
        throws PrinterException
    {
        double paperHeight = pageFormat.getPaper().getHeight();
        double paperWidth = pageFormat.getPaper().getWidth();
        int pageNumber = index + 1;

        /* Place an initial gstate on to our gstate stack.
         * It will have the default PostScript gstate
         * attributes.
         */
        mGStateStack = new ArrayList<>();
        mGStateStack.add(new GState());

        mPSStream.println(PAGE_COMMENT + pageNumber + " " + pageNumber);

        /* Check current page's pageFormat against the previous pageFormat,
         */
        if (index > 0 && paperChanged) {

            mPSStream.print("<< /PageSize [" +
                            paperWidth + " " + paperHeight + "]");

            final PrintService pservice = getPrintService();
            @SuppressWarnings("removal")
            Boolean isPS = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        try {
                            Class<?> psClass =
                                Class.forName("sun.print.IPPPrintService");
                            if (psClass.isInstance(pservice)) {
                                Method isPSMethod =
                                    psClass.getMethod("isPostscript",
                                                      (Class[])null);
                                return (Boolean)
                                    isPSMethod.invoke(pservice,
                                                      (Object[])null);
                            }
                        } catch (Throwable t) {
                        }
                        return Boolean.TRUE;
                    }
                    }
                );

            if (isPS) {
                mPSStream.print(" /DeferredMediaSelection true");
            }
            mPSStream.println(" >> setpagedevice");
        }
        mPSStream.println(PAGE_SAVE);
        mPSStream.println(paperHeight + getCoordPrep());
    }

    /**
     * The RastePrintJob super class calls this method
     * at the end of each page.
     */
    protected void endPage(PageFormat format, Printable painter,
                           int index)
        throws PrinterException
    {
        mPSStream.println(PAGE_RESTORE);
        mPSStream.println(SHOWPAGE);
    }

   /**
     * Convert the 24 bit BGR image buffer represented by
     * {@code image} to PostScript. The image is drawn at
     * {@code (destX, destY)} in device coordinates.
     * The image is scaled into a square of size
     * specified by {@code destWidth} and
     * {@code destHeight}. The portion of the
     * source image copied into that square is specified
     * by {@code srcX}, {@code srcY},
     * {@code srcWidth}, and srcHeight.
     */
    protected void drawImageBGR(byte[] bgrData,
                                   float destX, float destY,
                                   float destWidth, float destHeight,
                                   float srcX, float srcY,
                                   float srcWidth, float srcHeight,
                                   int srcBitMapWidth, int srcBitMapHeight) {

        /* We draw images at device resolution so we probably need
         * to change the current PostScript transform.
         */
        setTransform(new AffineTransform());
        prepDrawing();

        int intSrcWidth = (int) srcWidth;
        int intSrcHeight = (int) srcHeight;

        mPSStream.println(IMAGE_SAVE);

        /* Create a PS string big enough to hold a row of pixels.
         */
        int psBytesPerRow = 3 * intSrcWidth;
        while (psBytesPerRow > MAX_PSSTR) {
            psBytesPerRow /= 2;
        }

        mPSStream.println(psBytesPerRow + IMAGE_STR);

        /* Scale and translate the unit image.
         */
        mPSStream.println("[" + destWidth + " 0 "
                          + "0 " + destHeight
                          + " " + destX + " " + destY
                          +"]concat");

        /* Color Image invocation.
         */
        mPSStream.println(intSrcWidth + " " + intSrcHeight + " " + 8 + "["
                          + intSrcWidth + " 0 "
                          + "0 " + intSrcHeight
                          + " 0 " + 0 + "]"
                          + "/imageSrc load false 3 colorimage");

        /* Image data.
         */
        int index = 0;
        byte[] rgbData = new byte[intSrcWidth * 3];

        try {
            /* Skip the parts of the image that are not part
             * of the source rectangle.
             */
            index = (int) srcY * srcBitMapWidth;

            for(int i = 0; i < intSrcHeight; i++) {

                /* Skip the left part of the image that is not
                 * part of the source rectangle.
                 */
                index += (int) srcX;

                index = swapBGRtoRGB(bgrData, index, rgbData);
                byte[] encodedData = rlEncode(rgbData);
                byte[] asciiData = ascii85Encode(encodedData);
                mPSStream.write(asciiData);
                mPSStream.println("");
            }

            /*
             * If there is an IOError we subvert it to a PrinterException.
             * Fix: There has got to be a better way, maybe define
             * a PrinterIOException and then throw that?
             */
        } catch (IOException e) {
            //throw new PrinterException(e.toString());
        }

        mPSStream.println(IMAGE_RESTORE);
    }

    /**
     * Prints the contents of the array of ints, 'data'
     * to the current page. The band is placed at the
     * location (x, y) in device coordinates on the
     * page. The width and height of the band is
     * specified by the caller. Currently the data
     * is 24 bits per pixel in BGR format.
     */
    protected void printBand(byte[] bgrData, int x, int y,
                             int width, int height)
        throws PrinterException
    {

        mPSStream.println(IMAGE_SAVE);

        /* Create a PS string big enough to hold a row of pixels.
         */
        int psBytesPerRow = 3 * width;
        while (psBytesPerRow > MAX_PSSTR) {
            psBytesPerRow /= 2;
        }

        mPSStream.println(psBytesPerRow + IMAGE_STR);

        /* Scale and translate the unit image.
         */
        mPSStream.println("[" + width + " 0 "
                          + "0 " + height
                          + " " + x + " " + y
                          +"]concat");

        /* Color Image invocation.
         */
        mPSStream.println(width + " " + height + " " + 8 + "["
                          + width + " 0 "
                          + "0 " + -height
                          + " 0 " + height + "]"
                          + "/imageSrc load false 3 colorimage");

        /* Image data.
         */
        int index = 0;
        byte[] rgbData = new byte[width*3];

        try {
            for(int i = 0; i < height; i++) {
                index = swapBGRtoRGB(bgrData, index, rgbData);
                byte[] encodedData = rlEncode(rgbData);
                byte[] asciiData = ascii85Encode(encodedData);
                mPSStream.write(asciiData);
                mPSStream.println("");
            }

        } catch (IOException e) {
            throw new PrinterIOException(e);
        }

        mPSStream.println(IMAGE_RESTORE);
    }

    /**
     * Examine the metrics captured by the
     * {@code PeekGraphics} instance and
     * if capable of directly converting this
     * print job to the printer's control language
     * or the native OS's graphics primitives, then
     * return a {@code PSPathGraphics} to perform
     * that conversion. If there is not an object
     * capable of the conversion then return
     * {@code null}. Returning {@code null}
     * causes the print job to be rasterized.
     */

    protected Graphics2D createPathGraphics(PeekGraphics peekGraphics,
                                            PrinterJob printerJob,
                                            Printable painter,
                                            PageFormat pageFormat,
                                            int pageIndex) {

        PSPathGraphics pathGraphics;
        PeekMetrics metrics = peekGraphics.getMetrics();

        /* If the application has drawn anything that
         * out PathGraphics class can not handle then
         * return a null PathGraphics.
         */
        if (forcePDL == false && (forceRaster == true
                        || metrics.hasNonSolidColors()
                        || metrics.hasCompositing())) {

            pathGraphics = null;
        } else {

            BufferedImage bufferedImage = new BufferedImage(8, 8,
                                            BufferedImage.TYPE_INT_RGB);
            Graphics2D bufferedGraphics = bufferedImage.createGraphics();
            boolean canRedraw = peekGraphics.getAWTDrawingOnly() == false;

            pathGraphics =  new PSPathGraphics(bufferedGraphics, printerJob,
                                               painter, pageFormat, pageIndex,
                                               canRedraw);
        }

        return pathGraphics;
    }

    /**
     * Intersect the gstate's current path with the
     * current clip and make the result the new clip.
     */
    protected void selectClipPath() {

        mPSStream.println(mClipOpStr);
    }

    protected void setClip(Shape clip) {

        mLastClip = clip;
    }

    protected void setTransform(AffineTransform transform) {
        mLastTransform = transform;
    }

    /**
     * Set the current PostScript font.
     * Taken from outFont in PSPrintStream.
     */
     protected boolean setFont(Font font) {
        mLastFont = font;
        return true;
    }

    /**
     * Given an array of CharsetStrings that make up a run
     * of text, this routine converts each CharsetString to
     * an index into our PostScript font list. If one or more
     * CharsetStrings can not be represented by a PostScript
     * font, then this routine will return a null array.
     */
     private int[] getPSFontIndexArray(Font font, CharsetString[] charSet) {
        int[] psFont = null;

        if (mFontProps != null) {
            psFont = new int[charSet.length];
        }

        for (int i = 0; i < charSet.length && psFont != null; i++){

            /* Get the encoding of the run of text.
             */
            CharsetString cs = charSet[i];

            CharsetEncoder fontCS = cs.fontDescriptor.encoder;
            String charsetName = cs.fontDescriptor.getFontCharsetName();
            /*
             * sun.awt.Symbol perhaps should return "symbol" for encoding.
             * Similarly X11Dingbats should return "dingbats"
             * Forced to check for win32 & x/unix names for these converters.
             */

            if ("Symbol".equals(charsetName)) {
                charsetName = "symbol";
            } else if ("WingDings".equals(charsetName) ||
                       "X11Dingbats".equals(charsetName)) {
                charsetName = "dingbats";
            } else {
                charsetName = makeCharsetName(charsetName, cs.charsetChars);
            }

            int styleMask = font.getStyle() |
                FontUtilities.getFont2D(font).getStyle();

            String style = FontConfiguration.getStyleString(styleMask);

            /* First we map the font name through the properties file.
             * This mapping provides alias names for fonts, for example,
             * "timesroman" is mapped to "serif".
             */
            String fontName = font.getFamily().toLowerCase(Locale.ENGLISH);
            fontName = fontName.replace(' ', '_');
            String name = mFontProps.getProperty(fontName, "");

            /* Now map the alias name, character set name, and style
             * to a PostScript name.
             */
            String psName =
                mFontProps.getProperty(name + "." + charsetName + "." + style,
                                      null);

            if (psName != null) {

                /* Get the PostScript font index for the PostScript font.
                 */
                try {
                    psFont[i] =
                        Integer.parseInt(mFontProps.getProperty(psName));

                /* If there is no PostScript font for this font name,
                 * then we want to termintate the loop and the method
                 * indicating our failure. Setting the array to null
                 * is used to indicate these failures.
                 */
                } catch(NumberFormatException e){
                    psFont = null;
                }

            /* There was no PostScript name for the font, character set,
             * and style so give up.
             */
            } else {
                psFont = null;
            }
        }

         return psFont;
     }


    private static String escapeParens(String str) {
        if (str.indexOf('(') == -1 && str.indexOf(')') == -1 ) {
            return str;
        } else {
            int count = 0;
            int pos = 0;
            while ((pos = str.indexOf('(', pos)) != -1) {
                count++;
                pos++;
            }
            pos = 0;
            while ((pos = str.indexOf(')', pos)) != -1) {
                count++;
                pos++;
            }
            char []inArr = str.toCharArray();
            char []outArr = new char[inArr.length+count];
            pos = 0;
            for (int i=0;i<inArr.length;i++) {
                if (inArr[i] == '(' || inArr[i] == ')') {
                    outArr[pos++] = '\\';
                }
                outArr[pos++] = inArr[i];
            }
            return new String(outArr);

        }
    }

    /* return of 0 means unsupported. Other return indicates the number
     * of distinct PS fonts needed to draw this text. This saves us
     * doing this processing one extra time.
     */
    protected int platformFontCount(Font font, String str) {
        if (mFontProps == null) {
            return 0;
        }
        PlatformFont peer = (PlatformFont) FontAccess.getFontAccess()
                                                     .getFontPeer(font);
        CharsetString[] acs = peer.makeMultiCharsetString(str, false);
        if (acs == null) {
            /* AWT can't convert all chars so use 2D path */
            return 0;
        }
        int[] psFonts = getPSFontIndexArray(font, acs);
        return (psFonts == null) ? 0 : psFonts.length;
    }

     protected boolean textOut(Graphics g, String str, float x, float y,
                               Font mLastFont, FontRenderContext frc,
                               float width) {
        boolean didText = true;

        if (mFontProps == null) {
            return false;
        } else {
            prepDrawing();

            /* On-screen drawString renders most control chars as the missing
             * glyph and have the non-zero advance of that glyph.
             * Exceptions are \t, \n and \r which are considered zero-width.
             * Postscript handles control chars mostly as a missing glyph.
             * But we use 'ashow' specifying a width for the string which
             * assumes zero-width for those three exceptions, and Postscript
             * tries to squeeze the extra char in, with the result that the
             * glyphs look compressed or even overlap.
             * So exclude those control chars from the string sent to PS.
             */
            str = removeControlChars(str);
            if (str.length() == 0) {
                return true;
            }
            PlatformFont peer = (PlatformFont) FontAccess.getFontAccess()
                                                         .getFontPeer(mLastFont);
            CharsetString[] acs = peer.makeMultiCharsetString(str, false);
            if (acs == null) {
                /* AWT can't convert all chars so use 2D path */
                return false;
            }
            /* Get an array of indices into our PostScript name
             * table. If all of the runs can not be converted
             * to PostScript fonts then null is returned and
             * we'll want to fall back to printing the text
             * as shapes.
             */
            int[] psFonts = getPSFontIndexArray(mLastFont, acs);
            if (psFonts != null) {

                for (int i = 0; i < acs.length; i++){
                    CharsetString cs = acs[i];
                    CharsetEncoder fontCS = cs.fontDescriptor.encoder;

                    StringBuilder nativeStr = new StringBuilder();
                    byte[] strSeg = new byte[cs.length * 2];
                    int len = 0;
                    try {
                        ByteBuffer bb = ByteBuffer.wrap(strSeg);
                        fontCS.encode(CharBuffer.wrap(cs.charsetChars,
                                                      cs.offset,
                                                      cs.length),
                                      bb, true);
                        bb.flip();
                        len = bb.limit();
                    } catch(IllegalStateException xx){
                        continue;
                    } catch(CoderMalfunctionError xx){
                        continue;
                    }
                    /* The width to fit to may either be specified,
                     * or calculated. Specifying by the caller is only
                     * valid if the text does not need to be decomposed
                     * into multiple calls.
                     */
                    float desiredWidth;
                    if (acs.length == 1 && width != 0f) {
                        desiredWidth = width;
                    } else {
                        Rectangle2D r2d =
                            mLastFont.getStringBounds(cs.charsetChars,
                                                      cs.offset,
                                                      cs.offset+cs.length,
                                                      frc);
                        desiredWidth = (float)r2d.getWidth();
                    }
                    /* unprintable chars had width of 0, causing a PS error
                     */
                    if (desiredWidth == 0) {
                        return didText;
                    }
                    nativeStr.append('<');
                    for (int j = 0; j < len; j++){
                        byte b = strSeg[j];
                        // to avoid encoding conversion with println()
                        String hexS = Integer.toHexString(b);
                        int length = hexS.length();
                        if (length > 2) {
                            hexS = hexS.substring(length - 2, length);
                        } else if (length == 1) {
                            hexS = "0" + hexS;
                        } else if (length == 0) {
                            hexS = "00";
                        }
                        nativeStr.append(hexS);
                    }
                    nativeStr.append('>');
                    /* This comment costs too much in output file size */
//                  mPSStream.println("% Font[" + mLastFont.getName() + ", " +
//                             FontConfiguration.getStyleString(mLastFont.getStyle()) + ", "
//                             + mLastFont.getSize2D() + "]");
                    getGState().emitPSFont(psFonts[i], mLastFont.getSize2D());

                    // out String
                    mPSStream.println(nativeStr.toString() + " " +
                                      desiredWidth + " " + x + " " + y + " " +
                                      DrawStringName);
                    x += desiredWidth;
                }
            } else {
                didText = false;
            }
        }

        return didText;
     }
    /**
     * Set the current path rule to be either
     * {@code FILL_EVEN_ODD} (using the
     * even-odd file rule) or {@code FILL_WINDING}
     * (using the non-zero winding rule.)
     */
    protected void setFillMode(int fillRule) {

        switch (fillRule) {

         case FILL_EVEN_ODD:
            mFillOpStr = EVEN_ODD_FILL_STR;
            mClipOpStr = EVEN_ODD_CLIP_STR;
            break;

         case FILL_WINDING:
             mFillOpStr = WINDING_FILL_STR;
             mClipOpStr = WINDING_CLIP_STR;
             break;

         default:
             throw new IllegalArgumentException();
        }

    }

    /**
     * Set the printer's current color to be that
     * defined by {@code color}
     */
    protected void setColor(Color color) {
        mLastColor = color;
    }

    /**
     * Fill the current path using the current fill mode
     * and color.
     */
    protected void fillPath() {

        mPSStream.println(mFillOpStr);
    }

    /**
     * Called to mark the start of a new path.
     */
    protected void beginPath() {

        prepDrawing();
        mPSStream.println(NEWPATH_STR);

        mPenX = 0;
        mPenY = 0;
    }

    /**
     * Close the current subpath by appending a straight
     * line from the current point to the subpath's
     * starting point.
     */
    protected void closeSubpath() {

        mPSStream.println(CLOSEPATH_STR);

        mPenX = mStartPathX;
        mPenY = mStartPathY;
    }


    /**
     * Generate PostScript to move the current pen
     * position to {@code (x, y)}.
     */
    protected void moveTo(float x, float y) {

        mPSStream.println(trunc(x) + " " + trunc(y) + MOVETO_STR);

        /* moveto marks the start of a new subpath
         * and we need to remember that starting
         * position so that we know where the
         * pen returns to with a close path.
         */
        mStartPathX = x;
        mStartPathY = y;

        mPenX = x;
        mPenY = y;
    }
    /**
     * Generate PostScript to draw a line from the
     * current pen position to {@code (x, y)}.
     */
    protected void lineTo(float x, float y) {

        mPSStream.println(trunc(x) + " " + trunc(y) + LINETO_STR);

        mPenX = x;
        mPenY = y;
    }

    /**
     * Add to the current path a bezier curve formed
     * by the current pen position and the method parameters
     * which are two control points and an ending
     * point.
     */
    protected void bezierTo(float control1x, float control1y,
                                float control2x, float control2y,
                                float endX, float endY) {

//      mPSStream.println(control1x + " " + control1y
//                        + " " + control2x + " " + control2y
//                        + " " + endX + " " + endY
//                        + CURVETO_STR);
        mPSStream.println(trunc(control1x) + " " + trunc(control1y)
                          + " " + trunc(control2x) + " " + trunc(control2y)
                          + " " + trunc(endX) + " " + trunc(endY)
                          + CURVETO_STR);


        mPenX = endX;
        mPenY = endY;
    }

    String trunc(float f) {
        float af = Math.abs(f);
        if (af >= 1f && af <=1000f) {
            f = Math.round(f*1000)/1000f;
        }
        return Float.toString(f);
    }

    /**
     * Return the x coordinate of the pen in the
     * current path.
     */
    protected float getPenX() {

        return mPenX;
    }
    /**
     * Return the y coordinate of the pen in the
     * current path.
     */
    protected float getPenY() {

        return mPenY;
    }

    /**
     * Return the x resolution of the coordinates
     * to be rendered.
     */
    protected double getXRes() {
        return xres;
    }
    /**
     * Return the y resolution of the coordinates
     * to be rendered.
     */
    protected double getYRes() {
        return yres;
    }

    /**
     * Set the resolution at which to print.
     */
    protected void setXYRes(double x, double y) {
        xres = x;
        yres = y;
    }

    /**
     * For PostScript the origin is in the upper-left of the
     * paper not at the imageable area corner.
     */
    protected double getPhysicalPrintableX(Paper p) {
        return 0;

    }

    /**
     * For PostScript the origin is in the upper-left of the
     * paper not at the imageable area corner.
     */
    protected double getPhysicalPrintableY(Paper p) {
        return 0;
    }

    protected double getPhysicalPrintableWidth(Paper p) {
        return p.getImageableWidth();
    }

    protected double getPhysicalPrintableHeight(Paper p) {
        return p.getImageableHeight();
    }

    protected double getPhysicalPageWidth(Paper p) {
        return p.getWidth();
    }

    protected double getPhysicalPageHeight(Paper p) {
        return p.getHeight();
    }

   /**
     * Returns how many times each page in the book
     * should be consecutively printed by PrintJob.
     * If the printer makes copies itself then this
     * method should return 1.
     */
    protected int getNoncollatedCopies() {
        return 1;
    }

    protected int getCollatedCopies() {
        return 1;
    }

    private String[] printExecCmd(String printer, String options,
                                  boolean noJobSheet,
                                  String jobTitle, int copies, String spoolFile) {
        int PRINTER = 0x1;
        int OPTIONS = 0x2;
        int JOBTITLE  = 0x4;
        int COPIES  = 0x8;
        int NOSHEET = 0x10;
        int pFlags = 0;
        String[] execCmd;
        int ncomps = 2; // minimum number of print args
        int n = 0;

        if (printer != null && !printer.isEmpty() && !printer.equals("lp")) {
            pFlags |= PRINTER;
            ncomps+=1;
        }
        if (options != null && !options.isEmpty()) {
            pFlags |= OPTIONS;
            ncomps+=1;
        }
        if (jobTitle != null && !jobTitle.isEmpty()) {
            pFlags |= JOBTITLE;
            ncomps+=1;
        }
        if (copies > 1) {
            pFlags |= COPIES;
            ncomps+=1;
        }
        if (noJobSheet) {
            pFlags |= NOSHEET;
            ncomps+=1;
        } else if (getPrintService().
                        isAttributeCategorySupported(JobSheets.class)) {
            ncomps+=1; // for jobsheet
        }

        String osname = System.getProperty("os.name");
        if (osname.equals("Linux") || osname.contains("OS X")) {
            execCmd = new String[ncomps];
            execCmd[n++] = "/usr/bin/lpr";
            if ((pFlags & PRINTER) != 0) {
                execCmd[n++] = "-P" + printer;
            }
            if ((pFlags & JOBTITLE) != 0) {
                execCmd[n++] = "-J"  + jobTitle;
            }
            if ((pFlags & COPIES) != 0) {
                execCmd[n++] = "-#" + copies;
            }
            if ((pFlags & NOSHEET) != 0) {
                execCmd[n++] = "-h";
            } else if (getPrintService().
                        isAttributeCategorySupported(JobSheets.class)) {
                execCmd[n++] = "-o job-sheets=standard";
            }
            if ((pFlags & OPTIONS) != 0) {
                execCmd[n++] = "-o" + options;
            }
        } else {
            ncomps+=1; //add 1 arg for lp
            execCmd = new String[ncomps];
            execCmd[n++] = "/usr/bin/lp";
            execCmd[n++] = "-c";           // make a copy of the spool file
            if ((pFlags & PRINTER) != 0) {
                execCmd[n++] = "-d" + printer;
            }
            if ((pFlags & JOBTITLE) != 0) {
                execCmd[n++] = "-t"  + jobTitle;
            }
            if ((pFlags & COPIES) != 0) {
                execCmd[n++] = "-n" + copies;
            }
            if ((pFlags & NOSHEET) != 0) {
                execCmd[n++] = "-o nobanner";
            } else if (getPrintService().
                        isAttributeCategorySupported(JobSheets.class)) {
                execCmd[n++] = "-o job-sheets=standard";
            }
            if ((pFlags & OPTIONS) != 0) {
                execCmd[n++] = "-o" + options;
            }
        }
        execCmd[n++] = spoolFile;
        return execCmd;
    }

    private static int swapBGRtoRGB(byte[] image, int index, byte[] dest) {
        int destIndex = 0;
        while(index < image.length-2 && destIndex < dest.length-2) {
            dest[destIndex++] = image[index+2];
            dest[destIndex++] = image[index+1];
            dest[destIndex++] = image[index+0];
            index+=3;
        }
        return index;
    }

    /*
     * Currently CharToByteConverter.getCharacterEncoding() return values are
     * not fixed yet. These are used as the part of the key of
     * psfont.properties. When those name are fixed this routine can
     * be erased.
     */
    private String makeCharsetName(String name, char[] chs) {
        if (name.equals("Cp1252") || name.equals("ISO8859_1")) {
            return "latin1";
        } else if (name.equals("UTF8")) {
            // same as latin 1 if all chars < 256
            for (int i=0; i < chs.length; i++) {
                if (chs[i] > 255) {
                    return name.toLowerCase();
                }
            }
            return "latin1";
        } else if (name.startsWith("ISO8859")) {
            // same as latin 1 if all chars < 128
            for (int i=0; i < chs.length; i++) {
                if (chs[i] > 127) {
                    return name.toLowerCase();
                }
            }
            return "latin1";
        } else {
            return name.toLowerCase();
        }
    }

    private void prepDrawing() {

        /* Pop gstates until we can set the needed clip
         * and transform or until we are at the outer most
         * gstate.
         */
        while (isOuterGState() == false
               && (getGState().canSetClip(mLastClip) == false
                   || getGState().mTransform.equals(mLastTransform) == false)) {


            grestore();
        }

        /* Set the color. This can push the color to the
         * outer most gsave which is often a good thing.
         */
        getGState().emitPSColor(mLastColor);

        /* We do not want to change the outermost
         * transform or clip so if we are at the
         * outer clip the generate a gsave.
         */
        if (isOuterGState()) {
            gsave();
            getGState().emitTransform(mLastTransform);
            getGState().emitPSClip(mLastClip);
        }

        /* Set the font if we have been asked to. It is
         * important that the font is set after the
         * transform in order to get the font size
         * correct.
         */
//      if (g != null) {
//          getGState().emitPSFont(g, mLastFont);
//      }

    }

    /**
     * Return the GState that is currently on top
     * of the GState stack. There should always be
     * a GState on top of the stack. If there isn't
     * then this method will throw an IndexOutOfBounds
     * exception.
     */
    private GState getGState() {
        int count = mGStateStack.size();
        return mGStateStack.get(count - 1);
    }

    /**
     * Emit a PostScript gsave command and add a
     * new GState on to our stack which represents
     * the printer's gstate stack.
     */
    private void gsave() {
        GState oldGState = getGState();
        mGStateStack.add(new GState(oldGState));
        mPSStream.println(GSAVE_STR);
    }

    /**
     * Emit a PostScript grestore command and remove
     * a GState from our stack which represents the
     * printer's gstate stack.
     */
    private void grestore() {
        int count = mGStateStack.size();
        mGStateStack.remove(count - 1);
        mPSStream.println(GRESTORE_STR);
    }

    /**
     * Return true if the current GState is the
     * outermost GState and therefore should not
     * be restored.
     */
    private boolean isOuterGState() {
        return mGStateStack.size() == 1;
    }

    /**
     * A stack of GStates is maintained to model the printer's
     * gstate stack. Each GState holds information about
     * the current graphics attributes.
     */
    private class GState{
        Color mColor;
        Shape mClip;
        Font mFont;
        AffineTransform mTransform;

        GState() {
            mColor = Color.black;
            mClip = null;
            mFont = null;
            mTransform = new AffineTransform();
        }

        GState(GState copyGState) {
            mColor = copyGState.mColor;
            mClip = copyGState.mClip;
            mFont = copyGState.mFont;
            mTransform = copyGState.mTransform;
        }

        boolean canSetClip(Shape clip) {

            return mClip == null || mClip.equals(clip);
        }


        void emitPSClip(Shape clip) {
            if (clip != null
                && (mClip == null || mClip.equals(clip) == false)) {
                String saveFillOp = mFillOpStr;
                String saveClipOp = mClipOpStr;
                convertToPSPath(clip.getPathIterator(new AffineTransform()));
                selectClipPath();
                mClip = clip;
                /* The clip is a shape and has reset the winding rule state */
                mClipOpStr = saveFillOp;
                mFillOpStr = saveFillOp;
            }
        }

        void emitTransform(AffineTransform transform) {

            if (transform != null && transform.equals(mTransform) == false) {
                double[] matrix = new double[6];
                transform.getMatrix(matrix);
                mPSStream.println("[" + (float)matrix[0]
                                  + " " + (float)matrix[1]
                                  + " " + (float)matrix[2]
                                  + " " + (float)matrix[3]
                                  + " " + (float)matrix[4]
                                  + " " + (float)matrix[5]
                                  + "] concat");

                mTransform = transform;
            }
        }

        void emitPSColor(Color color) {
            if (color != null && color.equals(mColor) == false) {
                float[] rgb = color.getRGBColorComponents(null);

                /* If the color is a gray value then use
                 * setgray.
                 */
                if (rgb[0] == rgb[1] && rgb[1] == rgb[2]) {
                    mPSStream.println(rgb[0] + SETGRAY_STR);

                /* It's not gray so use setrgbcolor.
                 */
                } else {
                    mPSStream.println(rgb[0] + " "
                                      + rgb[1] + " "
                                      + rgb[2] + " "
                                      + SETRGBCOLOR_STR);
                }

                mColor = color;

            }
        }

        void emitPSFont(int psFontIndex, float fontSize) {
            mPSStream.println(fontSize + " " +
                              psFontIndex + " " + SetFontName);
        }
    }

       /**
        * Given a Java2D {@code PathIterator} instance,
        * this method translates that into a PostScript path..
        */
        void convertToPSPath(PathIterator pathIter) {

            float[] segment = new float[6];
            int segmentType;

            /* Map the PathIterator's fill rule into the PostScript
             * fill rule.
             */
            int fillRule;
            if (pathIter.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                fillRule = FILL_EVEN_ODD;
            } else {
                fillRule = FILL_WINDING;
            }

            beginPath();

            setFillMode(fillRule);

            while (pathIter.isDone() == false) {
                segmentType = pathIter.currentSegment(segment);

                switch (segmentType) {
                 case PathIterator.SEG_MOVETO:
                    moveTo(segment[0], segment[1]);
                    break;

                 case PathIterator.SEG_LINETO:
                    lineTo(segment[0], segment[1]);
                    break;

                /* Convert the quad path to a bezier.
                 */
                 case PathIterator.SEG_QUADTO:
                    float lastX = getPenX();
                    float lastY = getPenY();
                    float c1x = lastX + (segment[0] - lastX) * 2 / 3;
                    float c1y = lastY + (segment[1] - lastY) * 2 / 3;
                    float c2x = segment[2] - (segment[2] - segment[0]) * 2/ 3;
                    float c2y = segment[3] - (segment[3] - segment[1]) * 2/ 3;
                    bezierTo(c1x, c1y,
                             c2x, c2y,
                             segment[2], segment[3]);
                    break;

                 case PathIterator.SEG_CUBICTO:
                    bezierTo(segment[0], segment[1],
                             segment[2], segment[3],
                             segment[4], segment[5]);
                    break;

                 case PathIterator.SEG_CLOSE:
                    closeSubpath();
                    break;
                }


                pathIter.next();
            }
        }

    /*
     * Fill the path defined by {@code pathIter}
     * with the specified color.
     * The path is provided in current user space.
     */
    protected void deviceFill(PathIterator pathIter, Color color,
                              AffineTransform tx, Shape clip) {

        if (Double.isNaN(tx.getScaleX()) ||
            Double.isNaN(tx.getScaleY()) ||
            Double.isNaN(tx.getShearX()) ||
            Double.isNaN(tx.getShearY()) ||
            Double.isNaN(tx.getTranslateX()) ||
            Double.isNaN(tx.getTranslateY())) {
            return;
        }
        setTransform(tx);
        setClip(clip);
        setColor(color);
        convertToPSPath(pathIter);
        /* Specify the path to fill as the clip, this ensures that only
         * pixels which are inside the path will be filled, which is
         * what the Java 2D APIs specify
         */
        mPSStream.println(GSAVE_STR);
        selectClipPath();
        fillPath();
        mPSStream.println(GRESTORE_STR + " " + NEWPATH_STR);
    }

    /*
     * Run length encode byte array in a form suitable for decoding
     * by the PS Level 2 filter RunLengthDecode.
     * Array data to encode is inArr. Encoded data is written to outArr
     * outArr must be long enough to hold the encoded data but this
     * can't be known ahead of time.
     * A safe assumption is to use double the length of the input array.
     * This is then copied into a new array of the correct length which
     * is returned.
     * Algorithm:
     * Encoding is a lead byte followed by data bytes.
     * Lead byte of 0->127 indicates leadByte + 1 distinct bytes follow
     * Lead byte of 129->255 indicates 257 - leadByte is the number of times
     * the following byte is repeated in the source.
     * 128 is a special lead byte indicating end of data (EOD) and is
     * written as the final byte of the returned encoded data.
     */
     private byte[] rlEncode(byte[] inArr) {

         int inIndex = 0;
         int outIndex = 0;
         int startIndex = 0;
         int runLen = 0;
         byte[] outArr = new byte[(inArr.length * 2) +2];
         while (inIndex < inArr.length) {
             if (runLen == 0) {
                 startIndex = inIndex++;
                 runLen=1;
             }

             while (runLen < 128 && inIndex < inArr.length &&
                    inArr[inIndex] == inArr[startIndex]) {
                 runLen++; // count run of same value
                 inIndex++;
             }

             if (runLen > 1) {
                 outArr[outIndex++] = (byte)(257 - runLen);
                 outArr[outIndex++] = inArr[startIndex];
                 runLen = 0;
                 continue; // back to top of while loop.
             }

             // if reach here have a run of different values, or at the end.
             while (runLen < 128 && inIndex < inArr.length &&
                    inArr[inIndex] != inArr[inIndex-1]) {
                 runLen++; // count run of different values
                 inIndex++;
             }
             outArr[outIndex++] = (byte)(runLen - 1);
             for (int i = startIndex; i < startIndex+runLen; i++) {
                 outArr[outIndex++] = inArr[i];
             }
             runLen = 0;
         }
         outArr[outIndex++] = (byte)128;
         byte[] encodedData = new byte[outIndex];
         System.arraycopy(outArr, 0, encodedData, 0, outIndex);

         return encodedData;
     }

    /* written acc. to Adobe Spec. "Filtered Files: ASCIIEncode Filter",
     * "PS Language Reference Manual, 2nd edition: Section 3.13"
     */
    private byte[] ascii85Encode(byte[] inArr) {
        byte[]  outArr = new byte[((inArr.length+4) * 5 / 4) + 2];
        long p1 = 85;
        long p2 = p1*p1;
        long p3 = p1*p2;
        long p4 = p1*p3;
        byte pling = '!';

        int i = 0;
        int olen = 0;
        long val, rem;

        while (i+3 < inArr.length) {
            val = ((long)((inArr[i++]&0xff))<<24) +
                  ((long)((inArr[i++]&0xff))<<16) +
                  ((long)((inArr[i++]&0xff))<< 8) +
                  ((long)(inArr[i++]&0xff));
            if (val == 0) {
                outArr[olen++] = 'z';
            } else {
                rem = val;
                outArr[olen++] = (byte)(rem / p4 + pling); rem = rem % p4;
                outArr[olen++] = (byte)(rem / p3 + pling); rem = rem % p3;
                outArr[olen++] = (byte)(rem / p2 + pling); rem = rem % p2;
                outArr[olen++] = (byte)(rem / p1 + pling); rem = rem % p1;
                outArr[olen++] = (byte)(rem + pling);
            }
        }
        // input not a multiple of 4 bytes, write partial output.
        if (i < inArr.length) {
            int n = inArr.length - i; // n bytes remain to be written

            val = 0;
            while (i < inArr.length) {
                val = (val << 8) + (inArr[i++]&0xff);
            }

            int append = 4 - n;
            while (append-- > 0) {
                val = val << 8;
            }
            byte []c = new byte[5];
            rem = val;
            c[0] = (byte)(rem / p4 + pling); rem = rem % p4;
            c[1] = (byte)(rem / p3 + pling); rem = rem % p3;
            c[2] = (byte)(rem / p2 + pling); rem = rem % p2;
            c[3] = (byte)(rem / p1 + pling); rem = rem % p1;
            c[4] = (byte)(rem + pling);

            for (int b = 0; b < n+1 ; b++) {
                outArr[olen++] = c[b];
            }
        }

        // write EOD marker.
        outArr[olen++]='~'; outArr[olen++]='>';

        /* The original intention was to insert a newline after every 78 bytes.
         * This was mainly intended for legibility but I decided against this
         * partially because of the (small) amount of extra space, and
         * partially because for line breaks either would have to hardwire
         * ascii 10 (newline) or calculate space in bytes to allocate for
         * the platform's newline byte sequence. Also need to be careful
         * about where its inserted:
         * Ascii 85 decoder ignores white space except for one special case:
         * you must ensure you do not split the EOD marker across lines.
         */
        byte[] retArr = new byte[olen];
        System.arraycopy(outArr, 0, retArr, 0, olen);
        return retArr;

    }

    /**
     * PluginPrinter generates EPSF wrapped with a header and trailer
     * comment. This conforms to the new requirements of Mozilla 1.7
     * and FireFox 1.5 and later. Earlier versions of these browsers
     * did not support plugin printing in the general sense (not just Java).
     * A notable limitation of these browsers is that they handle plugins
     * which would span page boundaries by scaling plugin content to fit on a
     * single page. This means white space is left at the bottom of the
     * previous page and its impossible to print these cases as they appear on
     * the web page. This is contrast to how the same browsers behave on
     * Windows where it renders as on-screen.
     * Cases where the content fits on a single page do work fine, and they
     * are the majority of cases.
     * The scaling that the browser specifies to make the plugin content fit
     * when it is larger than a single page can hold is non-uniform. It
     * scales the axis in which the content is too large just enough to
     * ensure it fits. For content which is extremely long this could lead
     * to noticeable distortion. However that is probably rare enough that
     * its not worth compensating for that here, but we can revisit that if
     * needed, and compensate by making the scale for the other axis the
     * same.
     */
    public static class PluginPrinter implements Printable {

        private EPSPrinter epsPrinter;
        private Component applet;
        private PrintStream stream;
        private String epsTitle;
        private int bx, by, bw, bh;
        private int width, height;

        /**
         * This is called from the Java Plug-in to print an Applet's
         * contents as EPS to a postscript stream provided by the browser.
         * @param applet the applet component to print.
         * @param stream the print stream provided by the plug-in
         * @param x the x location of the applet panel in the browser window
         * @param y the y location of the applet panel in the browser window
         * @param w the width of the applet panel in the browser window
         * @param h the width of the applet panel in the browser window
         */
        @SuppressWarnings("deprecation")
        public PluginPrinter(Component applet,
                             PrintStream stream,
                             int x, int y, int w, int h) {

            this.applet = applet;
            this.epsTitle = "Java Plugin Applet";
            this.stream = stream;
            bx = x;
            by = y;
            bw = w;
            bh = h;
            width = applet.size().width;
            height = applet.size().height;
            epsPrinter = new EPSPrinter(this, epsTitle, stream,
                                        0, 0, width, height);
        }

        public void printPluginPSHeader() {
            stream.println("%%BeginDocument: JavaPluginApplet");
        }

        public void printPluginApplet() {
            try {
                epsPrinter.print();
            } catch (PrinterException e) {
            }
        }

        public void printPluginPSTrailer() {
            stream.println("%%EndDocument: JavaPluginApplet");
            stream.flush();
        }

        public void printAll() {
            printPluginPSHeader();
            printPluginApplet();
            printPluginPSTrailer();
        }

        public int print(Graphics g, PageFormat pf, int pgIndex) {
            if (pgIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            } else {
                // "aware" client code can detect that its been passed a
                // PrinterGraphics and could theoretically print
                // differently. I think this is more likely useful than
                // a problem.
                applet.printAll(g);
                return Printable.PAGE_EXISTS;
            }
        }

    }

    /*
     * This class can take an application-client supplied printable object
     * and send the result to a stream.
     * The application does not need to send any postscript to this stream
     * unless it needs to specify a translation etc.
     * It assumes that its importing application obeys all the conventions
     * for importation of EPS. See Appendix H - Encapsulated Postscript File
     * Format - of the Adobe Postscript Language Reference Manual, 2nd edition.
     * This class could be used as the basis for exposing the ability to
     * generate EPSF from 2D graphics as a StreamPrintService.
     * In that case a MediaPrintableArea attribute could be used to
     * communicate the bounding box.
     */
    public static class EPSPrinter implements Pageable {

        private PageFormat pf;
        private PSPrinterJob job;
        private int llx, lly, urx, ury;
        private Printable printable;
        private PrintStream stream;
        private String epsTitle;

        public EPSPrinter(Printable printable, String title,
                          PrintStream stream,
                          int x, int y, int wid, int hgt) {

            this.printable = printable;
            this.epsTitle = title;
            this.stream = stream;
            llx = x;
            lly = y;
            urx = llx+wid;
            ury = lly+hgt;
            // construct a PageFormat with zero margins representing the
            // exact bounds of the applet. ie construct a theoretical
            // paper which happens to exactly match applet panel size.
            Paper p = new Paper();
            p.setSize((double)wid, (double)hgt);
            p.setImageableArea(0.0,0.0, (double)wid, (double)hgt);
            pf = new PageFormat();
            pf.setPaper(p);
        }

        public void print() throws PrinterException {
            stream.println("%!PS-Adobe-3.0 EPSF-3.0");
            stream.println("%%BoundingBox: " +
                           llx + " " + lly + " " + urx + " " + ury);
            stream.println("%%Title: " + epsTitle);
            stream.println("%%Creator: Java Printing");
            stream.println("%%CreationDate: " + new java.util.Date());
            stream.println("%%EndComments");
            stream.println("/pluginSave save def");
            stream.println("mark"); // for restoring stack state on return

            job = new PSPrinterJob();
            job.epsPrinter = this; // modifies the behaviour of PSPrinterJob
            job.mPSStream = stream;
            job.mDestType = RasterPrinterJob.STREAM; // prevents closure

            job.startDoc();
            try {
                job.printPage(this, 0);
            } catch (Throwable t) {
                if (t instanceof PrinterException) {
                    throw (PrinterException)t;
                } else {
                    throw new PrinterException(t.toString());
                }
            } finally {
                stream.println("cleartomark"); // restore stack state
                stream.println("pluginSave restore");
                job.endDoc();
            }
            stream.flush();
        }

        public int getNumberOfPages() {
            return 1;
        }

        public PageFormat getPageFormat(int pgIndex) {
            if (pgIndex > 0) {
                throw new IndexOutOfBoundsException("pgIndex");
            } else {
                return pf;
            }
        }

        public Printable getPrintable(int pgIndex) {
            if (pgIndex > 0) {
                throw new IndexOutOfBoundsException("pgIndex");
            } else {
            return printable;
            }
        }

    }
}
