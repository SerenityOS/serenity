/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorTable;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import sun.awt.Mutex;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.datatransfer.ToolkitThreadBlockedHandler;
import sun.awt.image.ImageRepresentation;
import sun.awt.image.ToolkitImage;

import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Platform-specific support for the data transfer subsystem.
 *
 * @author David Mendenhall
 * @author Danila Sinopalnikov
 *
 * @since 1.3.1
 */
final class WDataTransferer extends DataTransferer {
    private static final String[] predefinedClipboardNames = {
            "",
            "TEXT",
            "BITMAP",
            "METAFILEPICT",
            "SYLK",
            "DIF",
            "TIFF",
            "OEM TEXT",
            "DIB",
            "PALETTE",
            "PENDATA",
            "RIFF",
            "WAVE",
            "UNICODE TEXT",
            "ENHMETAFILE",
            "HDROP",
            "LOCALE",
            "DIBV5"
    };

    private static final Map <String, Long> predefinedClipboardNameMap;
    static {
        Map <String,Long> tempMap =
                new HashMap <> (predefinedClipboardNames.length, 1.0f);
        for (int i = 1; i < predefinedClipboardNames.length; i++) {
            tempMap.put(predefinedClipboardNames[i], Long.valueOf(i));
        }
        predefinedClipboardNameMap =
                Collections.synchronizedMap(tempMap);
    }

    /**
     * from winuser.h
     */
    public static final int CF_TEXT = 1;
    public static final int CF_METAFILEPICT = 3;
    public static final int CF_DIB = 8;
    public static final int CF_ENHMETAFILE = 14;
    public static final int CF_HDROP = 15;
    public static final int CF_LOCALE = 16;

    public static final long CF_HTML = registerClipboardFormat("HTML Format");
    public static final long CFSTR_INETURL = registerClipboardFormat("UniformResourceLocator");
    public static final long CF_PNG = registerClipboardFormat("PNG");
    public static final long CF_JFIF = registerClipboardFormat("JFIF");

    public static final long CF_FILEGROUPDESCRIPTORW = registerClipboardFormat("FileGroupDescriptorW");
    public static final long CF_FILEGROUPDESCRIPTORA = registerClipboardFormat("FileGroupDescriptor");
    //CF_FILECONTENTS supported as mandatory associated clipboard

    private static final Long L_CF_LOCALE =
            predefinedClipboardNameMap.get(predefinedClipboardNames[CF_LOCALE]);

    private static final DirectColorModel directColorModel =
            new DirectColorModel(24,
                    0x00FF0000,  /* red mask   */
                    0x0000FF00,  /* green mask */
                    0x000000FF); /* blue mask  */

    private static final int[] bandmasks = new int[] {
            directColorModel.getRedMask(),
            directColorModel.getGreenMask(),
            directColorModel.getBlueMask() };

    /**
     * Singleton constructor
     */
    private WDataTransferer() {
    }

    private static WDataTransferer transferer;

    static synchronized WDataTransferer getInstanceImpl() {
        if (transferer == null) {
            transferer = new WDataTransferer();
        }
        return transferer;
    }

    @Override
    public SortedMap <Long, DataFlavor> getFormatsForFlavors(
            DataFlavor[] flavors, FlavorTable map)
    {
        SortedMap <Long, DataFlavor> retval =
                super.getFormatsForFlavors(flavors, map);

        // The Win32 native code does not support exporting LOCALE data, nor
        // should it.
        retval.remove(L_CF_LOCALE);

        return retval;
    }

    @Override
    public String getDefaultUnicodeEncoding() {
        return "utf-16le";
    }

    @Override
    public byte[] translateTransferable(Transferable contents,
                                        DataFlavor flavor,
                                        long format) throws IOException
    {
        byte[] bytes = null;
        if (format == CF_HTML) {
            if (contents.isDataFlavorSupported(DataFlavor.selectionHtmlFlavor)) {
                // if a user provides data represented by
                // DataFlavor.selectionHtmlFlavor format, we use this
                // type to store the data in the native clipboard
                bytes = super.translateTransferable(contents,
                        DataFlavor.selectionHtmlFlavor,
                        format);
            } else if (contents.isDataFlavorSupported(DataFlavor.allHtmlFlavor)) {
                // if we cannot get data represented by the
                // DataFlavor.selectionHtmlFlavor format
                // but the DataFlavor.allHtmlFlavor format is avialable
                // we belive that the user knows how to represent
                // the data and how to mark up selection in a
                // system specific manner. Therefor, we use this data
                bytes = super.translateTransferable(contents,
                        DataFlavor.allHtmlFlavor,
                        format);
            } else {
                // handle other html flavor types, including custom and
                // fragment ones
                bytes = HTMLCodec.convertToHTMLFormat(super.translateTransferable(contents, flavor, format));
            }
        } else {
            // we handle non-html types basing on  their
            // flavors
            bytes = super.translateTransferable(contents, flavor, format);
        }
        return bytes;
    }

    // The stream is closed as a closable object
    @Override
    public Object translateStream(InputStream str,
                                 DataFlavor flavor, long format,
                                 Transferable localeTransferable)
        throws IOException
    {
        if (format == CF_HTML && flavor.isFlavorTextType()) {
            str = new HTMLCodec(str,
                                 EHTMLReadMode.getEHTMLReadMode(flavor));

        }
        return super.translateStream(str, flavor, format,
                                        localeTransferable);

    }

    @Override
    public Object translateBytes(byte[] bytes, DataFlavor flavor, long format,
        Transferable localeTransferable) throws IOException
    {


        if (format == CF_FILEGROUPDESCRIPTORA || format == CF_FILEGROUPDESCRIPTORW) {
            if (bytes == null || !DataFlavor.javaFileListFlavor.equals(flavor)) {
                throw new IOException("data translation failed");
            }
            String st = new String(bytes, 0, bytes.length, UTF_16LE);
            String[] filenames = st.split("\0");
            if( 0 == filenames.length ){
                return null;
            }

            // Convert the strings to File objects
            File[] files = new File[filenames.length];
            for (int i = 0; i < filenames.length; ++i) {
                files[i] = new File(filenames[i]);
                //They are temp-files from memory Stream, so they have to be removed on exit
                files[i].deleteOnExit();
            }
            // Turn the list of Files into a List and return
            return Arrays.asList(files);
        }

        if (format == CFSTR_INETURL &&
                URL.class.equals(flavor.getRepresentationClass()))
        {
            String charset = Charset.defaultCharset().name();
            if (localeTransferable != null
                    && localeTransferable.isDataFlavorSupported(javaTextEncodingFlavor))
            {
                try {
                    charset = new String((byte[])localeTransferable.
                        getTransferData(javaTextEncodingFlavor), UTF_8);
                } catch (UnsupportedFlavorException cannotHappen) {
                }
            }
            return new URL(new String(bytes, charset));
        }

        return super.translateBytes(bytes , flavor, format,
                                        localeTransferable);

    }

    @Override
    public boolean isLocaleDependentTextFormat(long format) {
        return format == CF_TEXT || format == CFSTR_INETURL;
    }

    @Override
    public boolean isFileFormat(long format) {
        return format == CF_HDROP || format == CF_FILEGROUPDESCRIPTORA || format == CF_FILEGROUPDESCRIPTORW;
    }

    @Override
    protected Long getFormatForNativeAsLong(String str) {
        Long format = predefinedClipboardNameMap.get(str);
        if (format == null) {
            format = Long.valueOf(registerClipboardFormat(str));
        }
        return format;
    }

    @Override
    protected String getNativeForFormat(long format) {
        return (format < predefinedClipboardNames.length)
                ? predefinedClipboardNames[(int)format]
                : getClipboardFormatName(format);
    }

    private final ToolkitThreadBlockedHandler handler =
            new WToolkitThreadBlockedHandler();

    @Override
    public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
        return handler;
    }

    /**
     * Calls the Win32 RegisterClipboardFormat function to register
     * a non-standard format.
     */
    private static native long registerClipboardFormat(String str);

    /**
     * Calls the Win32 GetClipboardFormatName function which is
     * the reverse operation of RegisterClipboardFormat.
     */
    private static native String getClipboardFormatName(long format);

    @Override
    public boolean isImageFormat(long format) {
        return format == CF_DIB || format == CF_ENHMETAFILE ||
                format == CF_METAFILEPICT || format == CF_PNG ||
                format == CF_JFIF;
    }

    @Override
    protected byte[] imageToPlatformBytes(Image image, long format)
            throws IOException {
        String mimeType = null;
        if (format == CF_PNG) {
            mimeType = "image/png";
        } else if (format == CF_JFIF) {
            mimeType = "image/jpeg";
        }
        if (mimeType != null) {
            return imageToStandardBytes(image, mimeType);
        }

        int width = 0;
        int height = 0;

        if (image instanceof ToolkitImage) {
            ImageRepresentation ir = ((ToolkitImage)image).getImageRep();
            ir.reconstruct(ImageObserver.ALLBITS);
            width = ir.getWidth();
            height = ir.getHeight();
        } else {
            width = image.getWidth(null);
            height = image.getHeight(null);
        }

        // Fix for 4919639.
        // Some Windows native applications (e.g. clipbrd.exe) do not handle
        // 32-bpp DIBs correctly.
        // As a workaround we switched to 24-bpp DIBs.
        // MSDN prescribes that the bitmap array for a 24-bpp should consist of
        // 3-byte triplets representing blue, green and red components of a
        // pixel respectively. Additionally each scan line must be padded with
        // zeroes to end on a LONG data-type boundary. LONG is always 32-bit.
        // We render the given Image to a BufferedImage of type TYPE_3BYTE_BGR
        // with non-default scanline stride and pass the resulting data buffer
        // to the native code to fill the BITMAPINFO structure.
        int mod = (width * 3) % 4;
        int pad = mod > 0 ? 4 - mod : 0;

        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0};
        ColorModel colorModel =
                new ComponentColorModel(cs, nBits, false, false,
                        Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        WritableRaster raster =
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height,
                        width * 3 + pad, 3, bOffs, null);

        BufferedImage bimage = new BufferedImage(colorModel, raster, false, null);

        // Some Windows native applications (e.g. clipbrd.exe) do not understand
        // top-down DIBs.
        // So we flip the image vertically and create a bottom-up DIB.
        AffineTransform imageFlipTransform =
                new AffineTransform(1, 0, 0, -1, 0, height);

        Graphics2D g2d = bimage.createGraphics();

        try {
            g2d.drawImage(image, imageFlipTransform, null);
        } finally {
            g2d.dispose();
        }

        DataBufferByte buffer = (DataBufferByte)raster.getDataBuffer();

        byte[] imageData = buffer.getData();
        return imageDataToPlatformImageBytes(imageData, width, height, format);
    }

    private static final byte [] UNICODE_NULL_TERMINATOR =  new byte [] {0,0};

    @Override
    protected ByteArrayOutputStream convertFileListToBytes(ArrayList<String> fileList)
            throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if(fileList.isEmpty()) {
            //store empty unicode string (null terminator)
            bos.write(UNICODE_NULL_TERMINATOR);
        } else {
            for (int i = 0; i < fileList.size(); i++) {
                byte[] bytes = fileList.get(i).getBytes(getDefaultUnicodeEncoding());
                //store unicode string with null terminator
                bos.write(bytes, 0, bytes.length);
                bos.write(UNICODE_NULL_TERMINATOR);
            }
        }

        // According to MSDN the byte array have to be double NULL-terminated.
        // The array contains Unicode characters, so each NULL-terminator is
        // a pair of bytes

        bos.write(UNICODE_NULL_TERMINATOR);
        return bos;
    }

    /**
     * Returns a byte array which contains data special for the given format
     * and for the given image data.
     */
    private native byte[] imageDataToPlatformImageBytes(byte[] imageData,
                                                        int width, int height,
                                                        long format);

    /**
     * Translates either a byte array or an input stream which contain
     * platform-specific image data in the given format into an Image.
     */
    @Override
    protected Image platformImageBytesToImage(byte[] bytes, long format)
            throws IOException {
        String mimeType = null;
        if (format == CF_PNG) {
            mimeType = "image/png";
        } else if (format == CF_JFIF) {
            mimeType = "image/jpeg";
        }
        if (mimeType != null) {
            return standardImageBytesToImage(bytes, mimeType);
        }

        int[] imageData = platformImageBytesToImageData(bytes, format);
        if (imageData == null) {
            throw new IOException("data translation failed");
        }

        int len = imageData.length - 2;
        int width = imageData[len];
        int height = imageData[len + 1];

        DataBufferInt buffer = new DataBufferInt(imageData, len);
        WritableRaster raster = Raster.createPackedRaster(buffer, width,
                height, width,
                bandmasks, null);

        return new BufferedImage(directColorModel, raster, false, null);
    }

    /**
     * Translates a byte array which contains platform-specific image data in
     * the given format into an integer array which contains pixel values in
     * ARGB format. The two last elements in the array specify width and
     * height of the image respectively.
     */
    private native int[] platformImageBytesToImageData(byte[] bytes,
                                                       long format)
            throws IOException;

    @Override
    protected native String[] dragQueryFile(byte[] bytes);
}

final class WToolkitThreadBlockedHandler extends Mutex
        implements ToolkitThreadBlockedHandler {

    @Override
    public void enter() {
        if (!isOwned()) {
            throw new IllegalMonitorStateException();
        }
        unlock();
        startSecondaryEventLoop();
        lock();
    }

    @Override
    public void exit() {
        if (!isOwned()) {
            throw new IllegalMonitorStateException();
        }
        WToolkit.quitSecondaryEventLoop();
    }

    private native void startSecondaryEventLoop();
}

enum EHTMLReadMode {
    HTML_READ_ALL,
    HTML_READ_FRAGMENT,
    HTML_READ_SELECTION;

    public static EHTMLReadMode getEHTMLReadMode (DataFlavor df) {

        EHTMLReadMode mode = HTML_READ_SELECTION;

        String parameter = df.getParameter("document");

        if ("all".equals(parameter)) {
            mode = HTML_READ_ALL;
        } else if ("fragment".equals(parameter)) {
            mode = HTML_READ_FRAGMENT;
        }

        return mode;
    }
}

/**
 * on decode: This stream takes an InputStream which provides data in CF_HTML format,
 * strips off the description and context to extract the original HTML data.
 *
 * on encode: static convertToHTMLFormat is responsible for HTML clipboard header creation
 */
class HTMLCodec extends InputStream {

    public static final String VERSION = "Version:";
    public static final String START_HTML = "StartHTML:";
    public static final String END_HTML = "EndHTML:";
    public static final String START_FRAGMENT = "StartFragment:";
    public static final String END_FRAGMENT = "EndFragment:";
    public static final String START_SELECTION = "StartSelection:"; //optional
    public static final String END_SELECTION = "EndSelection:"; //optional

    public static final String START_FRAGMENT_CMT = "<!--StartFragment-->";
    public static final String END_FRAGMENT_CMT = "<!--EndFragment-->";
    public static final String SOURCE_URL = "SourceURL:";
    public static final String DEF_SOURCE_URL = "about:blank";

    public static final String EOLN = "\r\n";

    private static final String VERSION_NUM = "1.0";
    private static final int PADDED_WIDTH = 10;

    private static String toPaddedString(int n, int width) {
        String string = "" + n;
        int len = string.length();
        if (n >= 0 && len < width) {
            char[] array = new char[width - len];
            Arrays.fill(array, '0');
            string = String.valueOf(array) + string;
        }
        return string;
    }

    /**
     * convertToHTMLFormat adds the MS HTML clipboard header to byte array that
     * contains the parameters pairs.
     *
     * The consequence of parameters is fixed, but some or all of them could be
     * omitted. One parameter per one text line.
     * It looks like that:
     *
     * Version:1.0\r\n                -- current supported version
     * StartHTML:000000192\r\n        -- shift in array to the first byte after the header
     * EndHTML:000000757\r\n          -- shift in array of last byte for HTML syntax analysis
     * StartFragment:000000396\r\n    -- shift in array jast after <!--StartFragment-->
     * EndFragment:000000694\r\n      -- shift in array before start  <!--EndFragment-->
     * StartSelection:000000398\r\n   -- shift in array of the first char in copied selection
     * EndSelection:000000692\r\n     -- shift in array of the last char in copied selection
     * SourceURL:http://sun.com/\r\n  -- base URL for related referenses
     * <HTML>...<BODY>...<!--StartFragment-->.....................<!--EndFragment-->...</BODY><HTML>
     * ^                                     ^ ^                ^^                                 ^
     * \ StartHTML                           | \-StartSelection | \EndFragment              EndHTML/
     *                                       \-StartFragment    \EndSelection
     *
     *Combinations with tags sequence
     *<!--StartFragment--><HTML>...<BODY>...</BODY><HTML><!--EndFragment-->
     * or
     *<HTML>...<!--StartFragment-->...<BODY>...</BODY><!--EndFragment--><HTML>
     * are vailid too.
     */
    public static byte[] convertToHTMLFormat(byte[] bytes) {
        // Calculate section offsets
        String htmlPrefix = "";
        String htmlSuffix = "";
        {
            //we have extend the fragment to full HTML document correctly
            //to avoid HTML and BODY tags doubling
            String stContext = new String(bytes);
            String stUpContext = stContext.toUpperCase();
            if( -1 == stUpContext.indexOf("<HTML") ) {
                htmlPrefix = "<HTML>";
                htmlSuffix = "</HTML>";
                if( -1 == stUpContext.indexOf("<BODY") ) {
                    htmlPrefix = htmlPrefix +"<BODY>";
                    htmlSuffix = "</BODY>" + htmlSuffix;
                };
            };
        }

        String stBaseUrl = DEF_SOURCE_URL;
        int nStartHTML =
                VERSION.length() + VERSION_NUM.length() + EOLN.length()
                        + START_HTML.length() + PADDED_WIDTH + EOLN.length()
                        + END_HTML.length() + PADDED_WIDTH + EOLN.length()
                        + START_FRAGMENT.length() + PADDED_WIDTH + EOLN.length()
                        + END_FRAGMENT.length() + PADDED_WIDTH + EOLN.length()
                        + SOURCE_URL.length() + stBaseUrl.length() + EOLN.length()
                ;
        int nStartFragment = nStartHTML + htmlPrefix.length();
        int nEndFragment = nStartFragment + bytes.length - 1;
        int nEndHTML = nEndFragment + htmlSuffix.length();

        StringBuilder header = new StringBuilder(
                nStartFragment
                        + START_FRAGMENT_CMT.length()
        );
        //header
        header.append(VERSION);
        header.append(VERSION_NUM);
        header.append(EOLN);

        header.append(START_HTML);
        header.append(toPaddedString(nStartHTML, PADDED_WIDTH));
        header.append(EOLN);

        header.append(END_HTML);
        header.append(toPaddedString(nEndHTML, PADDED_WIDTH));
        header.append(EOLN);

        header.append(START_FRAGMENT);
        header.append(toPaddedString(nStartFragment, PADDED_WIDTH));
        header.append(EOLN);

        header.append(END_FRAGMENT);
        header.append(toPaddedString(nEndFragment, PADDED_WIDTH));
        header.append(EOLN);

        header.append(SOURCE_URL);
        header.append(stBaseUrl);
        header.append(EOLN);

        //HTML
        header.append(htmlPrefix);

        byte[] headerBytes = header.toString().getBytes(UTF_8);
        byte[] trailerBytes = htmlSuffix.getBytes(UTF_8);

        byte[] retval = new byte[headerBytes.length + bytes.length +
                trailerBytes.length];

        System.arraycopy(headerBytes, 0, retval, 0, headerBytes.length);
        System.arraycopy(bytes, 0, retval, headerBytes.length,
                bytes.length - 1);
        System.arraycopy(trailerBytes, 0, retval,
                headerBytes.length + bytes.length - 1,
                trailerBytes.length);
        retval[retval.length-1] = 0;

        return retval;
    }

    ////////////////////////////////////
    //decoder instance data and methods:

    private final BufferedInputStream bufferedStream;
    private boolean descriptionParsed = false;
    private boolean closed = false;

    // InputStreamReader uses an 8K buffer. The size is not customizable.
    public static final int BYTE_BUFFER_LEN = 8192;

    // CharToByteUTF8.getMaxBytesPerChar returns 3, so we should not buffer
    // more chars than 3 times the number of bytes we can buffer.
    public static final int CHAR_BUFFER_LEN = BYTE_BUFFER_LEN / 3;

    private static final String FAILURE_MSG =
            "Unable to parse HTML description: ";
    private static final String INVALID_MSG =
            " invalid";

    //HTML header mapping:
    private long   iHTMLStart,// StartHTML -- shift in array to the first byte after the header
            iHTMLEnd,  // EndHTML -- shift in array of last byte for HTML syntax analysis
            iFragStart,// StartFragment -- shift in array jast after <!--StartFragment-->
            iFragEnd,  // EndFragment -- shift in array before start <!--EndFragment-->
            iSelStart, // StartSelection -- shift in array of the first char in copied selection
            iSelEnd;   // EndSelection -- shift in array of the last char in copied selection
    private String stBaseURL; // SourceURL -- base URL for related referenses
    private String stVersion; // Version -- current supported version

    //Stream reader markers:
    private long iStartOffset,
            iEndOffset,
            iReadCount;

    private EHTMLReadMode readMode;

    public HTMLCodec(
            InputStream _bytestream,
            EHTMLReadMode _readMode) throws IOException
    {
        bufferedStream = new BufferedInputStream(_bytestream, BYTE_BUFFER_LEN);
        readMode = _readMode;
    }

    public synchronized String getBaseURL() throws IOException
    {
        if( !descriptionParsed ) {
            parseDescription();
        }
        return stBaseURL;
    }
    public synchronized String getVersion() throws IOException
    {
        if( !descriptionParsed ) {
            parseDescription();
        }
        return stVersion;
    }

    /**
     * parseDescription parsing HTML clipboard header as it described in
     * comment to convertToHTMLFormat
     */
    private void parseDescription() throws IOException
    {
        stBaseURL = null;
        stVersion = null;

        // initialization of array offset pointers
        // to the same "uninitialized" state.
        iHTMLEnd =
                iHTMLStart =
                        iFragEnd =
                                iFragStart =
                                        iSelEnd =
                                                iSelStart = -1;

        bufferedStream.mark(BYTE_BUFFER_LEN);
        String[] astEntries = new String[] {
                //common
                VERSION,
                START_HTML,
                END_HTML,
                START_FRAGMENT,
                END_FRAGMENT,
                //ver 1.0
                START_SELECTION,
                END_SELECTION,
                SOURCE_URL
        };
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        bufferedStream,
                        UTF_8
                ),
                CHAR_BUFFER_LEN
        );
        long iHeadSize = 0;
        long iCRSize = EOLN.length();
        int iEntCount = astEntries.length;
        boolean bContinue = true;

        for( int  iEntry = 0; iEntry < iEntCount; ++iEntry ){
            String stLine = bufferedReader.readLine();
            if( null==stLine ) {
                break;
            }
            //some header entries are optional, but the order is fixed.
            for( ; iEntry < iEntCount; ++iEntry ){
                if( !stLine.startsWith(astEntries[iEntry]) ) {
                    continue;
                }
                iHeadSize += stLine.length() + iCRSize;
                String stValue = stLine.substring(astEntries[iEntry].length()).trim();
                if( null!=stValue ) {
                    try{
                        switch( iEntry ){
                            case 0:
                                stVersion = stValue;
                                break;
                            case 1:
                                iHTMLStart = Integer.parseInt(stValue);
                                break;
                            case 2:
                                iHTMLEnd = Integer.parseInt(stValue);
                                break;
                            case 3:
                                iFragStart = Integer.parseInt(stValue);
                                break;
                            case 4:
                                iFragEnd = Integer.parseInt(stValue);
                                break;
                            case 5:
                                iSelStart = Integer.parseInt(stValue);
                                break;
                            case 6:
                                iSelEnd = Integer.parseInt(stValue);
                                break;
                            case 7:
                                stBaseURL = stValue;
                                break;
                        };
                    } catch ( NumberFormatException e ) {
                        throw new IOException(FAILURE_MSG + astEntries[iEntry]+ " value " + e + INVALID_MSG);
                    }
                }
                break;
            }
        }
        //some entries could absent in HTML header,
        //so we have find they by another way.
        if( -1 == iHTMLStart )
            iHTMLStart = iHeadSize;
        if( -1 == iFragStart )
            iFragStart = iHTMLStart;
        if( -1 == iFragEnd )
            iFragEnd = iHTMLEnd;
        if( -1 == iSelStart )
            iSelStart = iFragStart;
        if( -1 == iSelEnd )
            iSelEnd = iFragEnd;

        //one of possible modes
        switch( readMode ){
            case HTML_READ_ALL:
                iStartOffset = iHTMLStart;
                iEndOffset = iHTMLEnd;
                break;
            case HTML_READ_FRAGMENT:
                iStartOffset = iFragStart;
                iEndOffset = iFragEnd;
                break;
            case HTML_READ_SELECTION:
            default:
                iStartOffset = iSelStart;
                iEndOffset = iSelEnd;
                break;
        }

        bufferedStream.reset();
        if( -1 == iStartOffset ){
            throw new IOException(FAILURE_MSG + "invalid HTML format.");
        }

        int curOffset = 0;
        while (curOffset < iStartOffset){
            curOffset += bufferedStream.skip(iStartOffset - curOffset);
        }

        iReadCount = curOffset;

        if( iStartOffset != iReadCount ){
            throw new IOException(FAILURE_MSG + "Byte stream ends in description.");
        }
        descriptionParsed = true;
    }

    @Override
    public synchronized int read() throws IOException {
        if( closed ){
            throw new IOException("Stream closed");
        }

        if( !descriptionParsed ){
            parseDescription();
        }
        if( -1 != iEndOffset && iReadCount >= iEndOffset ) {
            return -1;
        }

        int retval = bufferedStream.read();
        if( retval == -1 ) {
            return -1;
        }
        ++iReadCount;
        return retval;
    }

    @Override
    public synchronized void close() throws IOException {
        if( !closed ){
            closed = true;
            bufferedStream.close();
        }
    }
}
