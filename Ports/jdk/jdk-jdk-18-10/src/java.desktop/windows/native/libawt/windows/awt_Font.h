/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_FONT_H
#define AWT_FONT_H

#include "awt.h"
#include "awt_Object.h"

#include "java_awt_Font.h"
#include "sun_awt_windows_WFontMetrics.h"
#include "sun_awt_FontDescriptor.h"
#include "sun_awt_PlatformFont.h"


/************************************************************************
 * AwtFont utilities
 */

extern jboolean IsMultiFont(JNIEnv *env, jobject obj);

#define GET_PLATFORMFONT(font)\
    (env->CallObjectMethod(env, font, AwtFont::peerMID))


/************************************************************************
 * AwtFont class
 */

class AwtFont : public AwtObject {
public:

    /* int[] width field for sun.awt.windows.WFontDescriptor */
    static jfieldID widthsID;

    /* int fields for sun.awt.windows.WFontDescriptor */
    static jfieldID ascentID;
    static jfieldID descentID;
    static jfieldID leadingID;
    static jfieldID heightID;
    static jfieldID maxAscentID;
    static jfieldID maxDescentID;
    static jfieldID maxHeightID;
    static jfieldID maxAdvanceID;

    /* sun.awt.FontDescriptor fontDescriptor field of sun.awt.CharsetString */
    static jfieldID fontDescriptorID;
    /* java.lang.String charsetString field of sun.awt.CharsetString */
    static jfieldID charsetStringID;

    /* java.lang.String nativeName field of sun.awt.FontDescriptor*/
    static jfieldID nativeNameID;
    /* boolean useUnicode field of sun.awt.FontDescriptor*/
    static jfieldID useUnicodeID;

    /* long field pData of java.awt.Font */
    static jfieldID pDataID;
    /* java.awt.peer.FontPeer field peer of java.awt.Font */
    static jmethodID peerMID;
    /* java.lang.String name field of java.awt.Font */
    static jfieldID nameID;
    /* int size field of java.awt.Font */
    static jfieldID sizeID;
    /* int style field of java.awt.Font */
    static jfieldID styleID;

    /* java.awt.Font peer field of java.awt.FontMetrics */
    static jfieldID fontID;

    /* sun.awt.FontConfiguration fontConfig field of sun.awt.PlatformFont */
    static jfieldID fontConfigID;
    /* FontDescriptor[] componentFonts field of sun.awt.PlatformFont */
    static jfieldID componentFontsID;

    /* String textComponentFontName field of sun.awt.windows.WFontPeer */
    static jfieldID textComponentFontNameID;

    /* String fontName field of sun.awt.windows.WDefaultFontCharset fields */
    static jfieldID fontNameID;

    static jmethodID makeConvertedMultiFontStringMID;

    /* java.awt.Font methods */
    static jmethodID getFontMID;

    /* java.awt.FontMetrics methods */
    static jmethodID getHeightMID;

    /*
     * The argument is used to determine how many handles of
     * Windows font the instance has.
     */
    AwtFont(int num, JNIEnv *env, jobject javaFont);
    ~AwtFont();    /* Releases all resources */

    virtual void Dispose();

    /*
     * Access methods
     */
    INLINE int GetHFontNum()     { return m_hFontNum; }
    INLINE HFONT GetHFont(int i) {
        DASSERT(m_hFont[i] != NULL);
        return m_hFont[i];
    }

    /* Used to keep English version unchanged as much as possiple. */
    INLINE HFONT GetHFont() {
        DASSERT(m_hFont[0] != NULL);
        return m_hFont[0];
    }
    INLINE int GetInputHFontIndex() { return m_textInput; }

    INLINE void SetAscent(int ascent) { m_ascent = ascent; }
    INLINE int GetAscent()           { return m_ascent; }
    INLINE int GetOverhang()         { return m_overhang; }

    /*
     * Font methods
     */

    /*
     * Returns the AwtFont object associated with the pFontJavaObject.
     * If none exists, create one.
     */
    static AwtFont* GetFont(JNIEnv *env, jobject font,
                            jint angle=0, jfloat awScale=1.0f);

    /*
     * Creates the specified font.  name names the font.  style is a bit
     * vector that describes the style of the font.  height is the point
     * size of the font.
     */
    static AwtFont* Create(JNIEnv *env, jobject font,
                           jint angle = 0, jfloat awScale=1.0f);
    static HFONT CreateHFont(WCHAR* name, int style, int height,
                             int angle = 0, float awScale=1.0f);

    static void Cleanup();

    /*
     * FontMetrics methods
     */

    /*
     * Loads the metrics of the associated font.  See Font.GetFont for
     * purpose of pWS.  (Also, client should provide Font java object
     * instead of getting it from the FontMetrics instance variable.)
     */
    static void LoadMetrics(JNIEnv *env, jobject fontMetrics);

    /* Returns the AwtFont associated with this metrics. */
    static AwtFont* GetFontFromMetrics(JNIEnv *env, jobject fontMetrics);

    /*
     * Sets the ascent of the font.  This member should be called if
     * font->m_nAscent < 0.
     */
    static void SetupAscent(AwtFont* font);

    /*
     * Determines the average dimension of the character in the
     * specified font 'font' and multiplies it by the specified number
     * of rows and columns.  'font' can be a temporary object.
     */
    static SIZE TextSize(AwtFont* font, int columns, int rows);

    /*
     * If 'font' is NULL, the SYSTEM_FONT is used to compute the size.
     * 'font' can be a temporary object.
     */
    static int getFontDescriptorNumber(JNIEnv *env, jobject font,
                                       jobject fontDescriptor);

    /*
     * 'font' is of type java.awt.Font.
     */
    static SIZE DrawStringSize_sub(jstring str, HDC hDC, jobject font,
                                   long x, long y, BOOL draw,
                                   UINT codePage = 0);

    INLINE static SIZE drawMFStringSize(HDC hDC, jobject font,
                                        jstring str, long x, long y,
                                        UINT codePage = 0)
    {
        return DrawStringSize_sub(str, hDC, font, x, y, TRUE , codePage);
    }


    INLINE static SIZE getMFStringSize(HDC hDC, jobject font, jstring str,
                                       UINT codePage = 0)
    {
        return DrawStringSize_sub(str, hDC, font, 0, 0, FALSE, codePage);
    }


    INLINE static long getMFStringWidth(HDC hDC, jobject font,
                                            jstring str) {
        return getMFStringSize(hDC, font, str).cx;
    }

    INLINE static void drawMFString(HDC hDC, jobject font, jstring str,
                                    long x, long y, UINT codePage = 0)
    {
        DrawStringSize_sub(str, hDC, font, x, y, TRUE, codePage);
    }

    INLINE static jobjectArray GetComponentFonts(JNIEnv *env,
                                                     jobject font) {
      jobject platformFont = env->CallObjectMethod(font, AwtFont::peerMID);
      if (platformFont != NULL) {
          jobjectArray result =
              (jobjectArray)(env->GetObjectField(platformFont,
                                                 AwtFont::componentFontsID));
          env->DeleteLocalRef(platformFont);
          return result;
      }
      return NULL;
    }

   /*
    * Variables
    */

private:
    /* The array of associated font handles */
    HFONT* m_hFont;
    /* The number of handles. */
    int    m_hFontNum;
    /* The index of the handle used to be set to AwtTextComponent. */
    int    m_textInput;
    /* The ascent of this font. */
    int m_ascent;
    /* The overhang, or amount added to a string's width, of this font. */
    int m_overhang;
    /* angle of text rotation in 10'ths of a degree*/
    int textAngle;
    /* average width scale factor to be applied */
    float awScale;
};



class AwtFontCache {
private:
    class Item {
    public:
        Item(const WCHAR* s, HFONT f, Item* n = NULL);
        ~Item();

        WCHAR*      name;
        HFONT       font;
        Item*       next;
        DWORD       refCount;   /*  The same HFONT can be associated with
                                    multiple Java objects.*/
    };

public:
    AwtFontCache() { m_head = NULL; }
    void    Add(WCHAR* name, HFONT font);
    HFONT   Lookup(WCHAR* name);
    BOOL    Search(HFONT font);
    void    Remove(HFONT font);
    void    Clear();
    void    IncRefCount(HFONT hFont);
    LONG    IncRefCount(Item* item);
    LONG    DecRefCount(Item* item);


    Item* m_head;
};

#define GET_FONT(target, OBJ) \
    ((jobject)env->CallObjectMethod(target, AwtComponent::getFontMID))

#endif /* AWT_FONT_H */
