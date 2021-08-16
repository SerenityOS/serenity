/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.awt.Font;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

public class FontFamily {

    private static ConcurrentHashMap<String, FontFamily>
        familyNameMap = new ConcurrentHashMap<String, FontFamily>();
    private static HashMap<String, FontFamily> allLocaleNames;

    protected String familyName;
    protected Font2D plain;
    protected Font2D bold;
    protected Font2D italic;
    protected Font2D bolditalic;
    protected boolean logicalFont = false;
    protected int familyRank;

    public static FontFamily getFamily(String name) {
        return familyNameMap.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static String[] getAllFamilyNames() {
        return null;
    }

    /* Only for use by FontManager.deRegisterBadFont(..).
     * If this was the only font in the family, the family is removed
     * from the map
     */
    static void remove(Font2D font2D) {

        String name = font2D.getFamilyName(Locale.ENGLISH);
        FontFamily family = getFamily(name);
        if (family == null) {
            return;
        }
        if (family.plain == font2D) {
            family.plain = null;
        }
        if (family.bold == font2D) {
            family.bold = null;
        }
        if (family.italic == font2D) {
            family.italic = null;
        }
        if (family.bolditalic == font2D) {
            family.bolditalic = null;
        }
        if (family.plain == null && family.bold == null &&
            family.italic == null && family.bolditalic == null) {
            familyNameMap.remove(name);
        }
    }

    public FontFamily(String name, boolean isLogFont, int rank) {
        logicalFont = isLogFont;
        familyName = name;
        familyRank = rank;
        familyNameMap.put(name.toLowerCase(Locale.ENGLISH), this);
    }

    /* Create a family for created fonts which aren't listed in the
     * main map.
     */
    FontFamily(String name) {
        logicalFont = false;
        familyName = name;
        familyRank = Font2D.DEFAULT_RANK;
    }

    public String getFamilyName() {
        return familyName;
    }

    public int getRank() {
        return familyRank;
    }

    private boolean isFromSameSource(Font2D font) {
        if (!(font instanceof FileFont)) {
            return false;
        }

        FileFont existingFont = null;
        if (plain instanceof FileFont) {
            existingFont = (FileFont)plain;
        } else if (bold instanceof FileFont) {
            existingFont = (FileFont)bold;
        } else if (italic instanceof FileFont) {
             existingFont = (FileFont)italic;
        } else if (bolditalic instanceof FileFont) {
             existingFont = (FileFont)bolditalic;
        }
        // A family isn't created until there's a font.
        // So if we didn't find a file font it means this
        // isn't a file-based family.
        if (existingFont == null) {
            return false;
        }
        File existDir = (new File(existingFont.platName)).getParentFile();

        FileFont newFont = (FileFont)font;
        File newDir = (new File(newFont.platName)).getParentFile();
        if (existDir != null) {
            try {
                existDir = existDir.getCanonicalFile();
            } catch (IOException ignored) {}
        }
        if (newDir != null) {
            try {
                newDir = newDir.getCanonicalFile();
            } catch (IOException ignored) {}
        }
        return java.util.Objects.equals(newDir, existDir);
    }

    /*
     * We want a family to be of the same width and prefer medium/normal width.
     * Once we find a particular width we accept more of the same width
     * until we find one closer to normal when we 'evict' all existing fonts.
     * So once we see a 'normal' width font we evict all members that are not
     * normal width and then accept only new ones that are normal width.
     *
     * Once a font passes the width test we subject it to the weight test.
     * For Plain we target the weight the closest that is <= NORMAL (400)
     * For Bold we target the weight that is closest to BOLD (700).
     *
     * In the future, rather than discarding these fonts, we should
     * extend the family to include these so lookups on these properties
     * can locate them, as presently they will only be located by full name
     * based lookup.
     */

    private int familyWidth = 0;
    private boolean preferredWidth(Font2D font) {

        int newWidth = font.getWidth();

        if (familyWidth == 0) {
            familyWidth = newWidth;
            return true;
        }

        if (newWidth == familyWidth) {
            return true;
        }

        if (Math.abs(Font2D.FWIDTH_NORMAL - newWidth) <
            Math.abs(Font2D.FWIDTH_NORMAL - familyWidth))
        {
           if (FontUtilities.debugFonts()) {
               FontUtilities.logInfo(
               "Found more preferred width. New width = " + newWidth +
               " Old width = " + familyWidth + " in font " + font +
               " nulling out fonts plain: " + plain + " bold: " + bold +
               " italic: " + italic + " bolditalic: " + bolditalic);
           }
           familyWidth = newWidth;
           plain = bold = italic = bolditalic = null;
           return true;
        } else if (FontUtilities.debugFonts()) {
               FontUtilities.logInfo(
               "Family rejecting font " + font +
               " of less preferred width " + newWidth);
        }
        return false;
    }

    private boolean closerWeight(Font2D currFont, Font2D font, int style) {
        if (familyWidth != font.getWidth()) {
            return false;
        }

        if (currFont == null) {
            return true;
        }

        if (FontUtilities.debugFonts()) {
            FontUtilities.logInfo(
            "New weight for style " + style + ". Curr.font=" + currFont +
            " New font="+font+" Curr.weight="+ + currFont.getWeight()+
            " New weight="+font.getWeight());
        }

        int newWeight = font.getWeight();
        switch (style) {
            case Font.PLAIN:
            case Font.ITALIC:
                return (newWeight <= Font2D.FWEIGHT_NORMAL &&
                        newWeight > currFont.getWeight());

            case Font.BOLD:
            case Font.BOLD|Font.ITALIC:
                return (Math.abs(newWeight - Font2D.FWEIGHT_BOLD) <
                        Math.abs(currFont.getWeight() - Font2D.FWEIGHT_BOLD));

            default:
               return false;
        }
    }

    public void setFont(Font2D font, int style) {

        if (FontUtilities.isLogging()) {
            String msg;
            if (font instanceof CompositeFont) {
                msg = "Request to add " + font.getFamilyName(null) +
                      " with style " + style + " to family " + familyName;
            } else {
                msg = "Request to add " + font +
                      " with style " + style + " to family " + this;
            }
            FontUtilities.logInfo(msg);
        }
        /* Allow a lower-rank font only if its a file font
         * from the exact same source as any previous font.
         */
        if ((font.getRank() > familyRank) && !isFromSameSource(font)) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logWarning("Rejecting adding " + font +
                                         " of lower rank " + font.getRank() +
                                         " to family " + this +
                                         " of rank " + familyRank);
            }
            return;
        }

        switch (style) {

        case Font.PLAIN:
            if (preferredWidth(font) && closerWeight(plain, font, style)) {
                plain = font;
            }
            break;

        case Font.BOLD:
            if (preferredWidth(font) && closerWeight(bold, font, style)) {
                bold = font;
            }
            break;

        case Font.ITALIC:
            if (preferredWidth(font) && closerWeight(italic, font, style)) {
                italic = font;
            }
            break;

        case Font.BOLD|Font.ITALIC:
            if (preferredWidth(font) && closerWeight(bolditalic, font, style)) {
                bolditalic = font;
            }
            break;

        default:
            break;
        }
    }

    public Font2D getFontWithExactStyleMatch(int style) {

        switch (style) {

        case Font.PLAIN:
            return plain;

        case Font.BOLD:
            return bold;

        case Font.ITALIC:
            return italic;

        case Font.BOLD|Font.ITALIC:
            return bolditalic;

        default:
            return null;
        }
    }

    /* REMIND: if the callers of this method are operating in an
     * environment in which not all fonts are registered, the returned
     * font may be a algorithmically styled one, where in fact if loadfonts
     * were executed, a styled font may be located. Our present "solution"
     * to this is to register all fonts in a directory and assume that this
     * registered all the styles of a font, since they would all be in the
     * same location.
     */
    public Font2D getFont(int style) {

        switch (style) {

        case Font.PLAIN:
            return plain;

        case Font.BOLD:
            if (bold != null) {
                return bold;
            } else if (plain != null && plain.canDoStyle(style)) {
                    return plain;
            } else {
                return null;
            }

        case Font.ITALIC:
            if (italic != null) {
                return italic;
            } else if (plain != null && plain.canDoStyle(style)) {
                    return plain;
            } else {
                return null;
            }

        case Font.BOLD|Font.ITALIC:
            if (bolditalic != null) {
                return bolditalic;
            } else if (bold != null && bold.canDoStyle(style)) {
                return bold;
            } else if (italic != null && italic.canDoStyle(style)) {
                    return italic;
            } else if (plain != null && plain.canDoStyle(style)) {
                    return plain;
            } else {
                return null;
            }
        default:
            return null;
        }
    }

    /* Only to be called if getFont(style) returns null
     * This method will only return null if the family is completely empty!
     * Note that it assumes the font of the style you need isn't in the
     * family. The logic here is that if we must substitute something
     * it might as well be from the same family.
     */
     Font2D getClosestStyle(int style) {

        switch (style) {
            /* if you ask for a plain font try to return a non-italic one,
             * then a italic one, finally a bold italic one */
        case Font.PLAIN:
            if (bold != null) {
                return bold;
            } else if (italic != null) {
                return italic;
            } else {
                return bolditalic;
            }

            /* if you ask for a bold font try to return a non-italic one,
             * then a bold italic one, finally an italic one */
        case Font.BOLD:
            if (plain != null) {
                return plain;
            } else if (bolditalic != null) {
                return bolditalic;
            } else {
                return italic;
            }

            /* if you ask for a italic font try to return a  bold italic one,
             * then a plain one, finally an bold one */
        case Font.ITALIC:
            if (bolditalic != null) {
                return bolditalic;
            } else if (plain != null) {
                return plain;
            } else {
                return bold;
            }

        case Font.BOLD|Font.ITALIC:
            if (italic != null) {
                return italic;
            } else if (bold != null) {
                return bold;
            } else {
                return plain;
            }
        }
        return null;
    }

    /* Font may have localized names. Store these in a separate map, so
     * that only clients who use these names need be affected.
     */
    static synchronized void addLocaleNames(FontFamily family, String[] names){
        if (allLocaleNames == null) {
            allLocaleNames = new HashMap<String, FontFamily>();
        }
        for (int i=0; i<names.length; i++) {
            allLocaleNames.put(names[i].toLowerCase(), family);
        }
    }

    public static synchronized FontFamily getLocaleFamily(String name) {
        if (allLocaleNames == null) {
            return null;
        }
        return allLocaleNames.get(name.toLowerCase());
    }

    public static FontFamily[] getAllFontFamilies() {
       Collection<FontFamily> families = familyNameMap.values();
       return families.toArray(new FontFamily[0]);
    }

    public String toString() {
        return
            "Font family: " + familyName +
            " plain="+plain+
            " bold=" + bold +
            " italic=" + italic +
            " bolditalic=" + bolditalic;

    }

}
