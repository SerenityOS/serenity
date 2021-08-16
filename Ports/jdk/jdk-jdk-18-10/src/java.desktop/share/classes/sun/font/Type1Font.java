/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.FontFormatException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;

import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

import static java.nio.charset.StandardCharsets.US_ASCII;

/*
 * Adobe Technical Note 5040 details the format of PFB files.
 * the file is divided into ascii and binary sections. Each section
 * starts with a header
 * 0x8001 - start of binary data, is followed by 4 bytes length, then data
 * 0x8002 - start of ascii data, is followed by 4 bytes length, then data
 * 0x8003 - end of data segment
 * The length is organised as LSB->MSB.
 *
 * Note: I experimented with using a MappedByteBuffer and
 * there were two problems/questions.
 * 1. If a global buffer is used rather than one allocated in the calling
 * context, then we need to synchronize on all uses of that data, which
 * means more code would beed to be synchronized with probable repercussions
 * elsewhere.
 * 2. It is not clear whether to free the buffer when the file is closed.
 * If we have the contents in memory then why keep open files around?
 * The mmapped buffer doesn't need it.
 * Also regular GC is what frees the buffer. So closing the file and nulling
 * out the reference still needs to wait for the buffer to be GC'd to
 * reclaim the storage.
 * If the contents of the buffer are persistent there's no need
 * to worry about synchronization.
 * Perhaps could use a WeakReference, and when its referent is gone, and
 * need it can just reopen the file.
 * Type1 fonts thus don't use up file descriptor references, but can
 * use memory footprint in a way that's managed by the host O/S.
 * The main "pain" may be the different model means code needs to be written
 * without assumptions as to how this is handled by the different subclasses
 * of FileFont.
 */
public class Type1Font extends FileFont {

     private static class T1DisposerRecord  implements DisposerRecord {
        String fileName = null;

        T1DisposerRecord(String name) {
            fileName = name;
        }

        @SuppressWarnings("removal")
        public synchronized void dispose() {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Object>() {
                    public Object run() {

                        if (fileName != null) {
                            (new java.io.File(fileName)).delete();
                        }
                        return null;
                    }
             });
        }
    }

    WeakReference<ByteBuffer> bufferRef = new WeakReference<>(null);

    private String psName = null;

    private static HashMap<String, String> styleAbbreviationsMapping;
    private static HashSet<String> styleNameTokes;

    static {
        styleAbbreviationsMapping = new HashMap<>();
        styleNameTokes = new HashSet<>();

        /* These abbreviation rules are taken from Appendix 1 of Adobe Technical Note #5088 */
        /* NB: this list is not complete - we did not include abbreviations which contain
               several capital letters because current expansion algorithm do not support this.
               (namely we have omited MM aka "Multiple Master", OsF aka "Oldstyle figures",
                           OS aka "Oldstyle", SC aka "Small caps" and  DS aka "Display" */
        String[] nm = {"Black", "Bold", "Book", "Demi", "Heavy", "Light",
                       "Meduium", "Nord", "Poster", "Regular", "Super", "Thin",
                       "Compressed", "Condensed", "Compact", "Extended", "Narrow",
                       "Inclined", "Italic", "Kursiv", "Oblique", "Upright", "Sloped",
                       "Semi", "Ultra", "Extra",
                       "Alternate", "Alternate", "Deutsche Fraktur", "Expert", "Inline", "Ornaments",
                       "Outline", "Roman", "Rounded", "Script", "Shaded", "Swash", "Titling", "Typewriter"};
        String[] abbrv = {"Blk", "Bd", "Bk", "Dm", "Hv", "Lt",
                          "Md", "Nd", "Po", "Rg", "Su", "Th",
                          "Cm", "Cn", "Ct", "Ex", "Nr",
                          "Ic", "It", "Ks", "Obl", "Up", "Sl",
                          "Sm", "Ult", "X",
                          "A", "Alt", "Dfr", "Exp", "In", "Or",
                          "Ou", "Rm", "Rd", "Scr", "Sh", "Sw", "Ti", "Typ"};
       /* This is only subset of names from nm[] because we want to distinguish things
           like "Lucida Sans TypeWriter Bold" and "Lucida Sans Bold".
           Names from "Design and/or special purpose" group are omitted. */
       String[] styleTokens = {"Black", "Bold", "Book", "Demi", "Heavy", "Light",
                       "Medium", "Nord", "Poster", "Regular", "Super", "Thin",
                       "Compressed", "Condensed", "Compact", "Extended", "Narrow",
                       "Inclined", "Italic", "Kursiv", "Oblique", "Upright", "Sloped", "Slanted",
                       "Semi", "Ultra", "Extra"};

        for(int i=0; i<nm.length; i++) {
            styleAbbreviationsMapping.put(abbrv[i], nm[i]);
        }
        for(int i=0; i<styleTokens.length; i++) {
            styleNameTokes.add(styleTokens[i]);
        }
        }


    /**
     * Constructs a Type1 Font.
     * @param platname - Platform identifier of the font. Typically file name.
     * @param nativeNames - Native names - typically XLFDs on Unix.
     */
    public Type1Font(String platname, Object nativeNames)
        throws FontFormatException {

        this(platname, nativeNames, false);
    }

    /**
     * - does basic verification of the file
     * - reads the names (full, family).
     * - determines the style of the font.
     * @throws FontFormatException if the font can't be opened
     * or fails verification,  or there's no usable cmap
     */
    public Type1Font(String platname, Object nativeNames, boolean createdCopy)
        throws FontFormatException {
        super(platname, nativeNames);
        fontRank = Font2D.TYPE1_RANK;
        try {
            verify();
        } catch (Throwable t) {
            if (createdCopy) {
                T1DisposerRecord ref = new T1DisposerRecord(platname);
                Disposer.addObjectRecord(bufferRef, ref);
                bufferRef = null;
            }
            if (t instanceof FontFormatException) {
                throw (FontFormatException)t;
            } else {
                throw new FontFormatException("Unexpected runtime exception.");
            }
        }
    }

    private synchronized ByteBuffer getBuffer() throws FontFormatException {
        ByteBuffer bbuf = bufferRef.get();
        if (bbuf == null) {
          //System.out.println("open T1 " + platName);
            try {
                @SuppressWarnings("removal")
                RandomAccessFile raf = (RandomAccessFile)
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Object>() {
                        public Object run() {
                            try {
                                return new RandomAccessFile(platName, "r");
                            } catch (FileNotFoundException ffne) {
                            }
                            return null;
                    }
                });
                FileChannel fc = raf.getChannel();
                fileSize = (int)fc.size();
                bbuf = ByteBuffer.allocate(fileSize);
                fc.read(bbuf);
                bbuf.position(0);
                bufferRef = new WeakReference<>(bbuf);
                fc.close();
            } catch (NullPointerException e) {
                throw new FontFormatException(e.toString());
            } catch (ClosedChannelException e) {
                /* NIO I/O is interruptible, recurse to retry operation.
                 * Clear interrupts before recursing in case NIO didn't.
                 */
                Thread.interrupted();
                return getBuffer();
            } catch (IOException e) {
                throw new FontFormatException(e.toString());
            }
        }
        return bbuf;
    }

    protected void close() {
    }

    /* called from native code to read file into a direct byte buffer */
    @SuppressWarnings("removal")
    void readFile(ByteBuffer buffer) {
        RandomAccessFile raf = null;
        FileChannel fc;
        try {
            raf = (RandomAccessFile)
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Object>() {
                        public Object run() {
                            try {
                                return new RandomAccessFile(platName, "r");
                            } catch (FileNotFoundException fnfe) {
                            }
                            return null;
                    }
            });
            fc = raf.getChannel();
            while (buffer.remaining() > 0 && fc.read(buffer) != -1) {}
        } catch (NullPointerException npe) {
        } catch (ClosedChannelException e) {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch (IOException ioe) {
            }
            /* NIO I/O is interruptible, recurse to retry operation.
             * Clear interrupts before recursing in case NIO didn't.
             */
            Thread.interrupted();
            readFile(buffer);
        } catch (IOException e) {
        } finally  {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public synchronized ByteBuffer readBlock(int offset, int length) {
        ByteBuffer bbuf = null;
        try {
            bbuf = getBuffer();
            if (offset > fileSize) {
                offset = fileSize;
            }
            bbuf.position(offset);
            return bbuf.slice();
        } catch (FontFormatException e) {
            return null;
        }
    }

    private void verify() throws FontFormatException {
        /* Normal usage should not call getBuffer(), as its state
         * ie endianness, position etc, are shared. verify() can do
         * this as its called only from within the constructor before
         * there are other users of this object.
         */
        ByteBuffer bb = getBuffer();
        if (bb.capacity() < 6) {
            throw new FontFormatException("short file");
        }
        int val = bb.get(0) & 0xff;
        if ((bb.get(0) & 0xff) == 0x80) {
            verifyPFB(bb);
            bb.position(6);
        } else {
            verifyPFA(bb);
            bb.position(0);
        }
        initNames(bb);
        if (familyName == null || fullName == null) {
            throw new FontFormatException("Font name not found");
        }
        setStyle();
    }

    public int getFileSize() {
        if (fileSize == 0) {
            try {
                getBuffer();
            } catch (FontFormatException e) {
            }
        }
        return fileSize;
    }

    private void verifyPFA(ByteBuffer bb) throws FontFormatException {
        if (bb.getShort() != 0x2521) { // 0x2521 is %!
            throw new FontFormatException("bad pfa font");
        }
        // remind - additional verification needed?
    }

    private void verifyPFB(ByteBuffer bb) throws FontFormatException {

        int pos = 0;
        while (true) {
            try {
                int segType = bb.getShort(pos) & 0xffff;
                if (segType == 0x8001 || segType == 0x8002) {
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    int segLen = bb.getInt(pos+2);
                    bb.order(ByteOrder.BIG_ENDIAN);
                    if (segLen <= 0) {
                        throw new FontFormatException("bad segment length");
                    }
                    pos += segLen+6;
                } else if (segType == 0x8003) {
                    return;
                } else {
                    throw new FontFormatException("bad pfb file");
                }
            } catch (BufferUnderflowException bue) {
                throw new FontFormatException(bue.toString());
            } catch (Exception e) {
                throw new FontFormatException(e.toString());
            }
        }
    }

    private static final int PSEOFTOKEN = 0;
    private static final int PSNAMETOKEN = 1;
    private static final int PSSTRINGTOKEN = 2;

    /* Need to parse the ascii contents of the Type1 font file,
     * looking for FullName, FamilyName and FontName.
     * If explicit names are not found then extract them from first text line.
     * Operating on bytes so can't use Java String utilities, which
     * is a large part of why this is a hack.
     *
     * Also check for mandatory FontType and verify if it is supported.
     */
    private void initNames(ByteBuffer bb) throws FontFormatException {
        boolean eof = false;
        String fontType = null;
        try {
            //Parse font looking for explicit FullName, FamilyName and FontName
            //  (according to Type1 spec they are optional)
            while ((fullName == null || familyName == null || psName == null || fontType == null) && !eof) {
                int tokenType = nextTokenType(bb);
                if (tokenType == PSNAMETOKEN) {
                    int pos = bb.position();
                    if (bb.get(pos) == 'F') {
                        String s = getSimpleToken(bb);
                        if ("FullName".equals(s)) {
                            if (nextTokenType(bb)==PSSTRINGTOKEN) {
                                fullName = getString(bb);
                            }
                        } else if ("FamilyName".equals(s)) {
                            if (nextTokenType(bb)==PSSTRINGTOKEN) {
                                familyName = getString(bb);
                            }
                        } else if ("FontName".equals(s)) {
                            if (nextTokenType(bb)==PSNAMETOKEN) {
                                psName = getSimpleToken(bb);
                            }
                        } else if ("FontType".equals(s)) {
                            /* look for
                                 /FontType id def
                            */
                            String token = getSimpleToken(bb);
                            if ("def".equals(getSimpleToken(bb))) {
                                fontType = token;
                        }
                        }
                    } else {
                        while (bb.get() > ' '); // skip token
                    }
                } else if (tokenType == PSEOFTOKEN) {
                    eof = true;
                }
            }
        } catch (Exception e) {
                throw new FontFormatException(e.toString());
        }

        /* Ignore all fonts besides Type1 (e.g. Type3 fonts) */
        if (!"1".equals(fontType)) {
            throw new FontFormatException("Unsupported font type");
        }

    if (psName == null) { //no explicit FontName
                // Try to extract font name from the first text line.
                // According to Type1 spec first line consist of
                //  "%!FontType1-SpecVersion: FontName FontVersion"
                // or
                //  "%!PS-AdobeFont-1.0: FontName version"
                bb.position(0);
                if (bb.getShort() != 0x2521) { //if pfb (do not start with "%!")
                    //skip segment header and "%!"
                    bb.position(8);
                    //NB: assume that first segment is ASCII one
                    //  (is it possible to have valid Type1 font with first binary segment?)
                }
                String formatType = getSimpleToken(bb);
                if (!formatType.startsWith("FontType1-") && !formatType.startsWith("PS-AdobeFont-")) {
                        throw new FontFormatException("Unsupported font format [" + formatType + "]");
                }
                psName = getSimpleToken(bb);
        }

    //if we got to the end of file then we did not find at least one of FullName or FamilyName
    //Try to deduce missing names from present ones
    //NB: At least psName must be already initialized by this moment
        if (eof) {
            //if we find fullName or familyName then use it as another name too
            if (fullName != null) {
                familyName = fullName2FamilyName(fullName);
            } else if (familyName != null) {
                fullName = familyName;
            } else { //fallback - use postscript font name to deduce full and family names
                fullName = psName2FullName(psName);
                familyName = psName2FamilyName(psName);
            }
        }
    }

    private String fullName2FamilyName(String name) {
        String res, token;
        int len, start, end; //length of family name part

        //FamilyName is truncated version of FullName
        //Truncated tail must contain only style modifiers

        end = name.length();

        while (end > 0) {
            start = end - 1;
            while (start > 0 && name.charAt(start) != ' ')
              start--;
            //as soon as we meet first non style token truncate
            // current tail and return
                        if (!isStyleToken(name.substring(start+1, end))) {
                                return name.substring(0, end);
            }
                        end = start;
        }

                return name; //should not happen
        }

    private String expandAbbreviation(String abbr) {
        if (styleAbbreviationsMapping.containsKey(abbr))
                        return styleAbbreviationsMapping.get(abbr);
        return abbr;
    }

    private boolean isStyleToken(String token) {
        return styleNameTokes.contains(token);
    }

    private String psName2FullName(String name) {
        String res;
        int pos;

        //According to Adobe technical note #5088 psName (aka FontName) has form
        //   <Family Name><VendorID>-<Weight><Width><Slant><Character Set>
        //where spaces are not allowed.

        //Conversion: Expand abbreviations in style portion (everything after '-'),
        //            replace '-' with space and insert missing spaces
        pos = name.indexOf('-');
        if (pos >= 0) {
            res =  expandName(name.substring(0, pos), false);
            res += " " + expandName(name.substring(pos+1), true);
        } else {
            res = expandName(name, false);
        }

        return res;
    }

    private String psName2FamilyName(String name) {
        String tmp = name;

        //According to Adobe technical note #5088 psName (aka FontName) has form
        //   <Family Name><VendorID>-<Weight><Width><Slant><Character Set>
        //where spaces are not allowed.

        //Conversion: Truncate style portion (everything after '-')
        //            and insert missing spaces

        if (tmp.indexOf('-') > 0) {
            tmp = tmp.substring(0, tmp.indexOf('-'));
        }

        return expandName(tmp, false);
    }

    private int nextCapitalLetter(String s, int off) {
        for (; (off >=0) && off < s.length(); off++) {
            if (s.charAt(off) >= 'A' && s.charAt(off) <= 'Z')
                return off;
        }
        return -1;
    }

    private String expandName(String s, boolean tryExpandAbbreviations) {
        StringBuilder res = new StringBuilder(s.length() + 10);
        int start=0, end;

        while(start < s.length()) {
            end = nextCapitalLetter(s, start + 1);
            if (end < 0) {
                end = s.length();
            }

            if (start != 0) {
                res.append(" ");
            }

            if (tryExpandAbbreviations) {
                res.append(expandAbbreviation(s.substring(start, end)));
            } else {
                res.append(s.substring(start, end));
            }
            start = end;
                }

        return res.toString();
    }

    /* skip lines beginning with "%" and leading white space on a line */
    private byte skip(ByteBuffer bb) {
        byte b = bb.get();
        while (b == '%') {
            while (true) {
                b = bb.get();
                if (b == '\r' || b == '\n') {
                    break;
                }
            }
        }
        while (b <= ' ') {
            b = bb.get();
        }
        return b;
    }

    /*
     * Token types:
     * PSNAMETOKEN - /
     * PSSTRINGTOKEN - literal text string
     */
    private int nextTokenType(ByteBuffer bb) {

        try {
            byte b = skip(bb);

            while (true) {
                if (b == (byte)'/') { // PS defined name follows.
                    return PSNAMETOKEN;
                } else if (b == (byte)'(') { // PS string follows
                    return PSSTRINGTOKEN;
                } else if ((b == (byte)'\r') || (b == (byte)'\n')) {
                b = skip(bb);
                } else {
                    b = bb.get();
                }
            }
        } catch (BufferUnderflowException e) {
            return PSEOFTOKEN;
        }
    }

    /* Read simple token (sequence of non-whitespace characters)
         starting from the current position.
         Skip leading whitespaces (if any). */
    private String getSimpleToken(ByteBuffer bb) {
        while (bb.get() <= ' ');
        int pos1 = bb.position()-1;
        while (bb.get() > ' ');
        int pos2 = bb.position();
        byte[] nameBytes = new byte[pos2-pos1-1];
        bb.position(pos1);
        bb.get(nameBytes);
        return new String(nameBytes, US_ASCII);
    }

    private String getString(ByteBuffer bb) {
        int pos1 = bb.position();
        while (bb.get() != ')');
        int pos2 = bb.position();
        byte[] nameBytes = new byte[pos2-pos1-1];
        bb.position(pos1);
        bb.get(nameBytes);
        return new String(nameBytes, US_ASCII);
    }


    public String getPostscriptName() {
        return psName;
    }

    protected synchronized FontScaler getScaler() {
        if (scaler == null) {
            scaler = FontScaler.getScaler(this, 0, false, fileSize);
        }

        return scaler;
    }

    CharToGlyphMapper getMapper() {
        if (mapper == null) {
            mapper = new Type1GlyphMapper(this);
        }
        return mapper;
    }

    public int getNumGlyphs() {
        try {
            return getScaler().getNumGlyphs();
        } catch (FontScalerException e) {
            scaler = FontScaler.getNullScaler();
            return getNumGlyphs();
        }
    }

    public int getMissingGlyphCode() {
        try {
            return getScaler().getMissingGlyphCode();
        } catch (FontScalerException e) {
            scaler = FontScaler.getNullScaler();
            return getMissingGlyphCode();
        }
    }

    public int getGlyphCode(char charCode) {
        try {
            return getScaler().getGlyphCode(charCode);
        } catch (FontScalerException e) {
            scaler = FontScaler.getNullScaler();
            return getGlyphCode(charCode);
        }
    }

    public String toString() {
        return "** Type1 Font: Family="+familyName+ " Name="+fullName+
            " style="+style+" fileName="+getPublicFileName();
    }
}
