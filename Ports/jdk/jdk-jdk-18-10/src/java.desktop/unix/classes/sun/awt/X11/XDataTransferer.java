/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Image;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

import sun.datatransfer.DataFlavorUtil;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.datatransfer.ToolkitThreadBlockedHandler;

import java.io.ByteArrayOutputStream;

/**
 * Platform-specific support for the data transfer subsystem.
 */
public class XDataTransferer extends DataTransferer {
    static final XAtom FILE_NAME_ATOM = XAtom.get("FILE_NAME");
    static final XAtom DT_NET_FILE_ATOM = XAtom.get("_DT_NETFILE");
    static final XAtom PNG_ATOM = XAtom.get("PNG");
    static final XAtom JFIF_ATOM = XAtom.get("JFIF");
    static final XAtom TARGETS_ATOM = XAtom.get("TARGETS");
    static final XAtom INCR_ATOM = XAtom.get("INCR");
    static final XAtom MULTIPLE_ATOM = XAtom.get("MULTIPLE");

    /**
     * Singleton constructor
     */
    private XDataTransferer() {
    }

    private static XDataTransferer transferer;

    static synchronized XDataTransferer getInstanceImpl() {
        if (transferer == null) {
            transferer = new XDataTransferer();
        }
        return transferer;
    }

    @Override
    public String getDefaultUnicodeEncoding() {
        return "iso-10646-ucs-2";
    }

    @Override
    public boolean isLocaleDependentTextFormat(long format) {
        return false;
    }

    @Override
    public boolean isTextFormat(long format) {
        return super.isTextFormat(format)
            || isMimeFormat(format, "text");
    }

    @Override
    protected String getCharsetForTextFormat(Long lFormat) {
        if (isMimeFormat(lFormat, "text")) {
            String nat = getNativeForFormat(lFormat);
            DataFlavor df = new DataFlavor(nat, null);
            // Ignore the charset parameter of the MIME type if the subtype
            // doesn't support charset.
            if (!DataFlavorUtil.doesSubtypeSupportCharset(df)) {
                return null;
            }
            String charset = df.getParameter("charset");
            if (charset != null) {
                return charset;
            }
        }
        return super.getCharsetForTextFormat(lFormat);
    }

    @Override
    protected boolean isURIListFormat(long format) {
        String nat = getNativeForFormat(format);
        if (nat == null) {
            return false;
        }
        try {
            DataFlavor df = new DataFlavor(nat);
            if (df.getPrimaryType().equals("text") && df.getSubType().equals("uri-list")) {
                return true;
            }
        } catch (Exception e) {
            // Not a MIME format.
        }
        return false;
    }

    @Override
    public boolean isFileFormat(long format) {
        return format == FILE_NAME_ATOM.getAtom() ||
            format == DT_NET_FILE_ATOM.getAtom();
    }

    @Override
    public boolean isImageFormat(long format) {
        return format == PNG_ATOM.getAtom() ||
            format == JFIF_ATOM.getAtom() ||
            isMimeFormat(format, "image");
    }

    @Override
    protected Long getFormatForNativeAsLong(String str) {
        // Just get the atom. If it has already been retrived
        // once, we'll get a copy so this should be very fast.
        return XAtom.get(str).getAtom();
    }

    @Override
    protected String getNativeForFormat(long format) {
        return getTargetNameForAtom(format);
    }

    public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
        return XToolkitThreadBlockedHandler.getToolkitThreadBlockedHandler();
    }

    /**
     * Gets an format name for a given format (atom)
     */
    private String getTargetNameForAtom(long atom) {
        return XAtom.get(atom).getName();
    }

    @Override
    protected byte[] imageToPlatformBytes(Image image, long format)
      throws IOException {
        String mimeType = null;
        if (format == PNG_ATOM.getAtom()) {
            mimeType = "image/png";
        } else if (format == JFIF_ATOM.getAtom()) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return imageToStandardBytes(image, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation to " + nativeFormat +
                                  " is not supported.");
        }
    }

    @Override
    protected ByteArrayOutputStream convertFileListToBytes(ArrayList<String> fileList)
        throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < fileList.size(); i++)
        {
               byte[] bytes = fileList.get(i).getBytes();
               if (i != 0) bos.write(0);
               bos.write(bytes, 0, bytes.length);
        }
        return bos;
    }

    /**
     * Translates either a byte array or an input stream which contain
     * platform-specific image data in the given format into an Image.
     */
    @Override
    protected Image platformImageBytesToImage(
        byte[] bytes, long format) throws IOException
    {
        String mimeType = null;
        if (format == PNG_ATOM.getAtom()) {
            mimeType = "image/png";
        } else if (format == JFIF_ATOM.getAtom()) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return standardImageBytesToImage(bytes, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation from " + nativeFormat +
                                  " is not supported.");
        }
    }

    @Override
    protected String[] dragQueryFile(byte[] bytes) {
        XToolkit.awtLock();
        try {
            return XlibWrapper.XTextPropertyToStringList(bytes,
                                                         XAtom.get("STRING").getAtom());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    @Override
    protected URI[] dragQueryURIs(InputStream stream,
                                  long format,
                                  Transferable localeTransferable)
      throws IOException {

        String charset = getBestCharsetForTextFormat(format, localeTransferable);
        try (InputStreamReader isr = new InputStreamReader(stream, charset);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            ArrayList<URI> uriList = new ArrayList<>();
            URI uri;
            while ((line = reader.readLine()) != null) {
                try {
                    uri = new URI(line);
                } catch (URISyntaxException uriSyntaxException) {
                    throw new IOException(uriSyntaxException);
                }
                uriList.add(uri);
            }
            return uriList.toArray(new URI[uriList.size()]);
        }
    }

    /**
     * Returns true if and only if the name of the specified format Atom
     * constitutes a valid MIME type with the specified primary type.
     */
    private boolean isMimeFormat(long format, String primaryType) {
        String nat = getNativeForFormat(format);

        if (nat == null) {
            return false;
        }

        try {
            DataFlavor df = new DataFlavor(nat);
            if (primaryType.equals(df.getPrimaryType())) {
                return true;
            }
        } catch (Exception e) {
            // Not a MIME format.
        }

        return false;
    }

    /*
     * The XDnD protocol prescribes that the Atoms used as targets for data
     * transfer should have string names that represent the corresponding MIME
     * types.
     * To meet this requirement we check if the passed native format constitutes
     * a valid MIME and return a list of flavors to which the data in this MIME
     * type can be translated by the Data Transfer subsystem.
     */
    @Override
    public LinkedHashSet<DataFlavor> getPlatformMappingsForNative(String nat) {
        LinkedHashSet<DataFlavor> flavors = new LinkedHashSet<>();

        if (nat == null) {
            return flavors;
        }

        DataFlavor df;
        try {
            df = new DataFlavor(nat);
        } catch (Exception e) {
            // The string doesn't constitute a valid MIME type.
            return flavors;
        }

        DataFlavor value = df;
        final String primaryType = df.getPrimaryType();
        final String baseType = primaryType + "/" + df.getSubType();

        // For text formats we map natives to MIME strings instead of data
        // flavors to enable dynamic text native-to-flavor mapping generation.
        // See SystemFlavorMap.getFlavorsForNative() for details.
        if ("image".equals(primaryType)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(baseType);
            if (readers.hasNext()) {
                flavors.add(DataFlavor.imageFlavor);
            }
        }

        flavors.add(value);

        return flavors;
    }

    private static ImageTypeSpecifier defaultSpecifier = null;

    private ImageTypeSpecifier getDefaultImageTypeSpecifier() {
        if (defaultSpecifier == null) {
            ColorModel model = ColorModel.getRGBdefault();
            WritableRaster raster =
                model.createCompatibleWritableRaster(10, 10);

            BufferedImage bufferedImage =
                new BufferedImage(model, raster, model.isAlphaPremultiplied(),
                                  null);

            defaultSpecifier = new ImageTypeSpecifier(bufferedImage);
        }

        return defaultSpecifier;
    }

    /*
     * The XDnD protocol prescribes that the Atoms used as targets for data
     * transfer should have string names that represent the corresponding MIME
     * types.
     * To meet this requirement we return a list of formats that represent
     * MIME types to which the data in this flavor can be translated by the Data
     * Transfer subsystem.
     */
    @Override
    public LinkedHashSet<String> getPlatformMappingsForFlavor(DataFlavor df) {
        LinkedHashSet<String> natives = new LinkedHashSet<>(1);

        if (df == null) {
            return natives;
        }

        String charset = df.getParameter("charset");
        String baseType = df.getPrimaryType() + "/" + df.getSubType();
        String mimeType = baseType;

        if (charset != null && DataFlavorUtil.isFlavorCharsetTextType(df)) {
            mimeType += ";charset=" + charset;
        }

        // Add a mapping to the MIME native whenever the representation class
        // doesn't require translation.
        if (df.getRepresentationClass() != null &&
            (df.isRepresentationClassInputStream() ||
             df.isRepresentationClassByteBuffer() ||
             byte[].class.equals(df.getRepresentationClass()))) {
            natives.add(mimeType);
        }

        if (DataFlavor.imageFlavor.equals(df)) {
            String[] mimeTypes = ImageIO.getWriterMIMETypes();
            if (mimeTypes != null) {
                for (String mime : mimeTypes) {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mime);
                    while (writers.hasNext()) {
                        ImageWriter imageWriter = writers.next();
                        ImageWriterSpi writerSpi = imageWriter.getOriginatingProvider();

                        if (writerSpi != null &&
                                writerSpi.canEncodeImage(getDefaultImageTypeSpecifier())) {
                            natives.add(mime);
                            break;
                        }
                    }
                }
            }
        } else if (DataFlavorUtil.isFlavorCharsetTextType(df)) {
            // stringFlavor is semantically equivalent to the standard
            // "text/plain" MIME type.
            if (DataFlavor.stringFlavor.equals(df)) {
                baseType = "text/plain";
            }

            for (String encoding : DataFlavorUtil.standardEncodings()) {
                if (!encoding.equals(charset)) {
                    natives.add(baseType + ";charset=" + encoding);
                }
            }

            // Add a MIME format without specified charset.
            if (!natives.contains(baseType)) {
                natives.add(baseType);
            }
        }

        return natives;
    }
}
