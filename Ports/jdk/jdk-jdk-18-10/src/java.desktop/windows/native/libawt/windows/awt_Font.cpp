/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include <math.h>
#include "jlong.h"
#include "awt_Font.h"
#include "awt_Toolkit.h"

#include "java_awt_Font.h"
#include "java_awt_FontMetrics.h"
#include "java_awt_Dimension.h"

#include "sun_awt_FontDescriptor.h"
#include "sun_awt_windows_WDefaultFontCharset.h"
#include "sun_awt_windows_WFontPeer.h"
#include "awt_Component.h"
#include "Disposer.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

AwtFontCache fontCache;

extern jboolean IsMultiFont(JNIEnv *env, jobject obj)
{
    if (obj == NULL) {
        return JNI_FALSE;
    }
    if (env->EnsureLocalCapacity(2)) {
        env->ExceptionClear();
        return JNI_FALSE;
    }
    jobject peer = env->CallObjectMethod(obj, AwtFont::peerMID);
    env->ExceptionClear();
    if (peer == NULL) {
        return JNI_FALSE;
    }
    jobject fontConfig = env->GetObjectField(peer, AwtFont::fontConfigID);
    jboolean result = fontConfig != NULL;
    env->DeleteLocalRef(peer);
    env->DeleteLocalRef(fontConfig);
    return result;
}

extern jstring GetTextComponentFontName(JNIEnv *env, jobject font)
{
    DASSERT(font != NULL);
    if (env->EnsureLocalCapacity(2)) {
        env->ExceptionClear();
        return NULL;
    }
    jobject peer = env->CallObjectMethod(font, AwtFont::peerMID);
    DASSERT(peer != NULL);
    if (peer == NULL) return NULL;
    jstring textComponentFontName =
            (jstring) env->GetObjectField(peer, AwtFont::textComponentFontNameID);
    env->DeleteLocalRef(peer);
    return textComponentFontName;
}

/************************************************************************
 * AwtFont fields
 */

/* sun.awt.windows.WFontMetrics fields */
jfieldID AwtFont::widthsID;
jfieldID AwtFont::ascentID;
jfieldID AwtFont::descentID;
jfieldID AwtFont::leadingID;
jfieldID AwtFont::heightID;
jfieldID AwtFont::maxAscentID;
jfieldID AwtFont::maxDescentID;
jfieldID AwtFont::maxHeightID;
jfieldID AwtFont::maxAdvanceID;

/* java.awt.FontDescriptor fields */
jfieldID AwtFont::nativeNameID;
jfieldID AwtFont::useUnicodeID;

/* java.awt.Font fields */
jfieldID AwtFont::pDataID;
jfieldID AwtFont::nameID;
jfieldID AwtFont::sizeID;
jfieldID AwtFont::styleID;

/* java.awt.FontMetrics fields */
jfieldID AwtFont::fontID;

/* sun.awt.PlatformFont fields */
jfieldID AwtFont::fontConfigID;
jfieldID AwtFont::componentFontsID;

/* sun.awt.windows.WFontPeer fields */
jfieldID AwtFont::textComponentFontNameID;

/* sun.awt.windows.WDefaultFontCharset fields */
jfieldID AwtFont::fontNameID;

/* java.awt.Font methods */
jmethodID AwtFont::peerMID;

/* sun.awt.PlatformFont methods */
jmethodID AwtFont::makeConvertedMultiFontStringMID;

/* sun.awt.PlatformFont methods */
jmethodID AwtFont::getFontMID;

/* java.awt.FontMetrics methods */
jmethodID AwtFont::getHeightMID;


/************************************************************************
 * AwtFont methods
 */
AwtFont::AwtFont(int num, JNIEnv *env, jobject javaFont)
{
    if (num == 0) {  // not multi-font
        num = 1;
    }

    m_hFontNum = num;
    m_hFont = new HFONT[num];

    for (int i = 0; i < num; i++) {
        m_hFont[i] = NULL;
    }

    m_textInput = -1;
    m_ascent = -1;
    m_overhang = 0;
}

AwtFont::~AwtFont()
{
    delete[] m_hFont;
}

void AwtFont::Dispose() {
    for (int i = 0; i < m_hFontNum; i++) {
        HFONT font = m_hFont[i];
        if (font != NULL && fontCache.Search(font)) {
            fontCache.Remove(font);
            /*  NOTE: delete of windows HFONT happens in FontCache::Remove
                      only when the final reference to the font is disposed */
        } else if (font != NULL) {
            // if font was not in cache, its not shared and we delete it now
            DASSERT(::GetObjectType(font) == OBJ_FONT);
            VERIFY(::DeleteObject(font));
        }
        m_hFont[i] = NULL;
    }

    AwtObject::Dispose();
}

static void pDataDisposeMethod(JNIEnv *env, jlong pData)
{
    TRY_NO_VERIFY;

    AwtObject::_Dispose((PDATA)pData);

    CATCH_BAD_ALLOC;
}

AwtFont* AwtFont::GetFont(JNIEnv *env, jobject font,
                          jint angle, jfloat awScale)
{
    jlong pData = env->GetLongField(font, AwtFont::pDataID);
    AwtFont* awtFont = (AwtFont*)jlong_to_ptr(pData);

    if (awtFont != NULL) {
        return awtFont;
    }

    awtFont = Create(env, font, angle, awScale);
    if (awtFont == NULL) {
        return NULL;
    }

    env->SetLongField(font, AwtFont::pDataID,
        reinterpret_cast<jlong>(awtFont));
    return awtFont;
}

// Get suitable CHARSET from charset string provided by font configuration.
static int GetNativeCharset(LPCWSTR name)
{
    if (wcsstr(name, L"ANSI_CHARSET"))
        return ANSI_CHARSET;
    if (wcsstr(name, L"DEFAULT_CHARSET"))
        return DEFAULT_CHARSET;
    if (wcsstr(name, L"SYMBOL_CHARSET") || wcsstr(name, L"WingDings"))
        return SYMBOL_CHARSET;
    if (wcsstr(name, L"SHIFTJIS_CHARSET"))
        return SHIFTJIS_CHARSET;
    if (wcsstr(name, L"GB2312_CHARSET"))
        return GB2312_CHARSET;
    if (wcsstr(name, L"HANGEUL_CHARSET"))
        return HANGEUL_CHARSET;
    if (wcsstr(name, L"CHINESEBIG5_CHARSET"))
        return CHINESEBIG5_CHARSET;
    if (wcsstr(name, L"OEM_CHARSET"))
        return OEM_CHARSET;
    if (wcsstr(name, L"JOHAB_CHARSET"))
        return JOHAB_CHARSET;
    if (wcsstr(name, L"HEBREW_CHARSET"))
        return HEBREW_CHARSET;
    if (wcsstr(name, L"ARABIC_CHARSET"))
        return ARABIC_CHARSET;
    if (wcsstr(name, L"GREEK_CHARSET"))
        return GREEK_CHARSET;
    if (wcsstr(name, L"TURKISH_CHARSET"))
        return TURKISH_CHARSET;
    if (wcsstr(name, L"VIETNAMESE_CHARSET"))
        return VIETNAMESE_CHARSET;
    if (wcsstr(name, L"THAI_CHARSET"))
        return THAI_CHARSET;
    if (wcsstr(name, L"EASTEUROPE_CHARSET"))
        return EASTEUROPE_CHARSET;
    if (wcsstr(name, L"RUSSIAN_CHARSET"))
        return RUSSIAN_CHARSET;
    if (wcsstr(name, L"MAC_CHARSET"))
        return MAC_CHARSET;
    if (wcsstr(name, L"BALTIC_CHARSET"))
        return BALTIC_CHARSET;
    return ANSI_CHARSET;
}

AwtFont* AwtFont::Create(JNIEnv *env, jobject font, jint angle, jfloat awScale)
{
    int fontSize = env->GetIntField(font, AwtFont::sizeID);
    int fontStyle = env->GetIntField(font, AwtFont::styleID);

    AwtFont* awtFont = NULL;
    jobjectArray compFont = NULL;
    int cfnum = 0;

    try {
        if (env->EnsureLocalCapacity(3) < 0)
            return 0;

        if (IsMultiFont(env, font)) {
            compFont = GetComponentFonts(env, font);
            if (compFont != NULL) {
                cfnum = env->GetArrayLength(compFont);
            }
        } else {
            compFont = NULL;
            cfnum = 0;
        }

        LPCWSTR wName = NULL;

        awtFont = new AwtFont(cfnum, env, font);

        awtFont->textAngle = angle;
        awtFont->awScale = awScale;

        if (cfnum > 0) {
            // Ask peer class for the text component font name
            jstring jTextComponentFontName = GetTextComponentFontName(env, font);
            if (jTextComponentFontName == NULL) {
                delete awtFont;
                return NULL;
            }
            LPCWSTR textComponentFontName = JNU_GetStringPlatformChars(env, jTextComponentFontName, NULL);

            awtFont->m_textInput = -1;
            for (int i = 0; i < cfnum; i++) {
                // nativeName is a pair of platform fontname and its charset
                // tied with a comma; "Times New Roman,ANSI_CHARSET".
                jobject fontDescriptor = env->GetObjectArrayElement(compFont,
                                                                    i);
                jstring nativeName =
                    (jstring)env->GetObjectField(fontDescriptor,
                                                 AwtFont::nativeNameID);
                wName = JNU_GetStringPlatformChars(env, nativeName, NULL);
                DASSERT(wName);
                if (wName == NULL) {
                    wName = L"Arial";
                }

                //On NT platforms, if the font is not Symbol or Dingbats
                //use "W" version of Win32 APIs directly, info the FontDescription
                //no need to convert characters from Unicode to locale encodings.
                if (GetNativeCharset(wName) != SYMBOL_CHARSET) {
                    env->SetBooleanField(fontDescriptor, AwtFont::useUnicodeID, TRUE);
                }

                // Check to see if this font is suitable for input
                // on AWT TextComponent
                if ((awtFont->m_textInput == -1) &&
                        (textComponentFontName != NULL) &&
                        (wcscmp(wName, textComponentFontName) == 0)) {
                    awtFont->m_textInput = i;
                }
                HFONT hfonttmp = CreateHFont(const_cast<LPWSTR>(wName), fontStyle, fontSize,
                                             angle, awScale);
                awtFont->m_hFont[i] = hfonttmp;

                JNU_ReleaseStringPlatformChars(env, nativeName, wName);

                env->DeleteLocalRef(fontDescriptor);
                env->DeleteLocalRef(nativeName);
            }
            if (awtFont->m_textInput == -1) {
                // no text component font was identified, so default
                // to first component
                awtFont->m_textInput = 0;
            }

            JNU_ReleaseStringPlatformChars(env, jTextComponentFontName, textComponentFontName);
            env->DeleteLocalRef(jTextComponentFontName);
        } else {
            // Instantiation for English version.
            jstring fontName = (jstring)env->GetObjectField(font,
                                                            AwtFont::nameID);
            if (fontName != NULL) {
                wName = JNU_GetStringPlatformChars(env, fontName, NULL);
            }
            if (wName == NULL) {
                wName = L"Arial";
            }

            WCHAR* wEName;
            if (!wcscmp(wName, L"Helvetica") || !wcscmp(wName, L"SansSerif")) {
                wEName = L"Arial";
            } else if (!wcscmp(wName, L"TimesRoman") ||
                       !wcscmp(wName, L"Serif")) {
                wEName = L"Times New Roman";
            } else if (!wcscmp(wName, L"Courier") ||
                       !wcscmp(wName, L"Monospaced")) {
                wEName = L"Courier New";
            } else if (!wcscmp(wName, L"Dialog")) {
                wEName = L"MS Sans Serif";
            } else if (!wcscmp(wName, L"DialogInput")) {
                wEName = L"MS Sans Serif";
            } else if (!wcscmp(wName, L"ZapfDingbats")) {
                wEName = L"WingDings";
            } else
                wEName = L"Arial";

            awtFont->m_textInput = 0;
            awtFont->m_hFont[0] = CreateHFont(wEName, fontStyle, fontSize,
                                              angle, awScale);

            JNU_ReleaseStringPlatformChars(env, fontName, wName);

            env->DeleteLocalRef(fontName);
        }
        /* The several callers of this method also set the pData field.
         * That's unnecessary but harmless duplication. However we definitely
         * want only one disposer record.
         */
        env->SetLongField(font, AwtFont::pDataID,
        reinterpret_cast<jlong>(awtFont));
        Disposer_AddRecord(env, font, pDataDisposeMethod,
                       reinterpret_cast<jlong>(awtFont));
    } catch (...) {
        env->DeleteLocalRef(compFont);
        throw;
    }

    env->DeleteLocalRef(compFont);
    return awtFont;
}

static void strip_tail(wchar_t* text, wchar_t* tail) { // strips tail and any possible whitespace before it from the end of text
    if (wcslen(text)<=wcslen(tail)) {
        return;
    }
    wchar_t* p = text+wcslen(text)-wcslen(tail);
    if (!wcscmp(p, tail)) {
        while(p>text && iswspace(*(p-1)))
            p--;
        *p = 0;
    }

}

static int ScaleUpX(float x) {
    int deviceIndex = AwtWin32GraphicsDevice::DeviceIndexForWindow(
        ::GetDesktopWindow());
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(deviceIndex);
    return device == NULL ? x : device->ScaleUpX(x);
}

static int ScaleUpY(int y) {
    int deviceIndex = AwtWin32GraphicsDevice::DeviceIndexForWindow(
        ::GetDesktopWindow());
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(deviceIndex);
    return device == NULL ? y : device->ScaleUpY(y);
}

static int ScaleDownX(int x) {
    int deviceIndex = AwtWin32GraphicsDevice::DeviceIndexForWindow(
        ::GetDesktopWindow());
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(deviceIndex);
    return device == NULL ? x : device->ScaleDownX(x);
}

static int ScaleDownY(int y) {
    int deviceIndex = AwtWin32GraphicsDevice::DeviceIndexForWindow(
        ::GetDesktopWindow());
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(deviceIndex);
    return device == NULL ? y : device->ScaleDownY(y);
}

static HFONT CreateHFont_sub(LPCWSTR name, int style, int height,
                             int angle=0, float awScale=1.0f)
{
    LOGFONTW logFont;

    logFont.lfWidth = 0;
    logFont.lfEscapement = angle;
    logFont.lfOrientation = angle;
    logFont.lfUnderline = FALSE;
    logFont.lfStrikeOut = FALSE;
    logFont.lfCharSet = GetNativeCharset(name);
    if (angle == 0 && awScale == 1.0f) {
        logFont.lfOutPrecision = OUT_DEFAULT_PRECIS;
    } else {
        logFont.lfOutPrecision = OUT_TT_ONLY_PRECIS;
    }
    logFont.lfClipPrecision = CLIP_DEFAULT_PRECIS;
    logFont.lfQuality = DEFAULT_QUALITY;
    logFont.lfPitchAndFamily = DEFAULT_PITCH;

    // Set style
    logFont.lfWeight = (style & java_awt_Font_BOLD) ? FW_BOLD : FW_NORMAL;
    logFont.lfItalic = (style & java_awt_Font_ITALIC) != 0;
    logFont.lfUnderline = 0;//(style & java_awt_Font_UNDERLINE) != 0;

    // Get point size
    logFont.lfHeight = ScaleUpY(-height);

    // Set font name
    WCHAR tmpname[80];
    wcscpy(tmpname, name);
    WCHAR* delimit = wcschr(tmpname, L',');
    if (delimit != NULL)
        *delimit = L'\0';  // terminate the string after the font name.
    // strip "Bold" and "Italic" from the end of the name
    strip_tail(tmpname,L""); //strip possible trailing whitespace
    strip_tail(tmpname,L"Italic");
    strip_tail(tmpname,L"Bold");
    wcscpy(&(logFont.lfFaceName[0]), tmpname);
    HFONT hFont = ::CreateFontIndirect(&logFont);
    DASSERT(hFont != NULL);
    // get a expanded or condensed version if its specified.
    if (awScale != 1.0f) {
        HDC hDC = ::GetDC(0);
        HFONT oldFont = (HFONT)::SelectObject(hDC, hFont);
        TEXTMETRIC tm;
        DWORD avgWidth;
        GetTextMetrics(hDC, &tm);
        oldFont = (HFONT)::SelectObject(hDC, oldFont);
        if (oldFont != NULL) { // should be the same as hFont
            VERIFY(::DeleteObject(oldFont));
        }
        avgWidth = tm.tmAveCharWidth;
        logFont.lfWidth = (LONG) ScaleUpX((fabs) (avgWidth * awScale));
        hFont = ::CreateFontIndirect(&logFont);
        DASSERT(hFont != NULL);
        VERIFY(::ReleaseDC(0, hDC));
    }

    return hFont;
}

HFONT AwtFont::CreateHFont(WCHAR* name, int style, int height,
                           int angle, float awScale)
{
    WCHAR longName[80];
    // 80 > (max face name(=30) + strlen("CHINESEBIG5_CHARSET"))
    // longName doesn't have to be printable.  So, it is OK not to convert.

    wsprintf(longName, L"%ls-%d-%d", name, style, height);

    HFONT hFont = NULL;

    // only cache & share unrotated, unexpanded/uncondensed fonts
    if (angle == 0 && awScale == 1.0f) {
        hFont = fontCache.Lookup(longName);
        if (hFont != NULL) {
            fontCache.IncRefCount(hFont);
            return hFont;
        }
    }

    hFont = CreateHFont_sub(name, style, height, angle, awScale);
    if (angle == 0 && awScale == 1.0f) {
        fontCache.Add(longName, hFont);
    }
    return hFont;
}

void AwtFont::Cleanup()
{
    fontCache.Clear();
}

void AwtFont::SetupAscent(AwtFont* font)
{
    HDC hDC = ::GetDC(0);
    DASSERT(hDC != NULL);
    HGDIOBJ oldFont = ::SelectObject(hDC, font->GetHFont());

    TEXTMETRIC metrics;
    VERIFY(::GetTextMetrics(hDC, &metrics));
    font->SetAscent(metrics.tmAscent);

    ::SelectObject(hDC, oldFont);
    VERIFY(::ReleaseDC(0, hDC));
}

void AwtFont::LoadMetrics(JNIEnv *env, jobject fontMetrics)
{
    if (env->EnsureLocalCapacity(3) < 0)
        return;
    jintArray widths = env->NewIntArray(256);
    if (widths == NULL) {
        /* no local refs to delete yet. */
        return;
    }
    jobject font = env->GetObjectField(fontMetrics, AwtFont::fontID);
    AwtFont* awtFont = AwtFont::GetFont(env, font);

    if (!awtFont) {
        /* failed to get font */
        return;
    }

    HDC hDC = ::GetDC(0);
    DASSERT(hDC != NULL);

    HGDIOBJ oldFont = ::SelectObject(hDC, awtFont->GetHFont());
    TEXTMETRIC metrics;
    VERIFY(::GetTextMetrics(hDC, &metrics));

    awtFont->m_ascent = metrics.tmAscent;

    int ascent = metrics.tmAscent;
    int descent = metrics.tmDescent;
    int leading = metrics.tmExternalLeading;

    env->SetIntField(fontMetrics, AwtFont::ascentID, ScaleDownY(ascent));
    env->SetIntField(fontMetrics, AwtFont::descentID, ScaleDownY(descent));
    env->SetIntField(fontMetrics, AwtFont::leadingID, ScaleDownX(leading));
    env->SetIntField(fontMetrics, AwtFont::heightID,
        ScaleDownY(metrics.tmAscent + metrics.tmDescent + leading));
    env->SetIntField(fontMetrics, AwtFont::maxAscentID, ScaleDownY(ascent));
    env->SetIntField(fontMetrics, AwtFont::maxDescentID, ScaleDownY(descent));

    int maxHeight =  ascent + descent + leading;
    env->SetIntField(fontMetrics, AwtFont::maxHeightID, ScaleDownY(maxHeight));

    int maxAdvance = metrics.tmMaxCharWidth;
    env->SetIntField(fontMetrics, AwtFont::maxAdvanceID, ScaleDownX(maxAdvance));

    awtFont->m_overhang = metrics.tmOverhang;

    jint intWidths[256];
    memset(intWidths, 0, 256 * sizeof(int));
    VERIFY(::GetCharWidth(hDC, metrics.tmFirstChar,
                          min(metrics.tmLastChar, 255),
                          (int *)&intWidths[metrics.tmFirstChar]));
    env->SetIntArrayRegion(widths, 0, 256, intWidths);
    env->SetObjectField(fontMetrics, AwtFont::widthsID, widths);

    // Get font metrics on remaining fonts (if multifont).
    for (int j = 1; j < awtFont->GetHFontNum(); j++) {
        ::SelectObject(hDC, awtFont->GetHFont(j));
        VERIFY(::GetTextMetrics(hDC, &metrics));
        env->SetIntField(fontMetrics, AwtFont::maxAscentID,
                         ascent = max(ascent, metrics.tmAscent));
        env->SetIntField(fontMetrics, AwtFont::maxDescentID,
                         descent = max(descent, metrics.tmDescent));
        env->SetIntField(fontMetrics, AwtFont::maxHeightID,
                         maxHeight = max(maxHeight,
                                         metrics.tmAscent +
                                         metrics.tmDescent +
                                         metrics.tmExternalLeading));
        env->SetIntField(fontMetrics, AwtFont::maxAdvanceID,
                         maxAdvance = max(maxAdvance, metrics.tmMaxCharWidth));
    }

    VERIFY(::SelectObject(hDC, oldFont));
    VERIFY(::ReleaseDC(0, hDC));
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(widths);
}

SIZE AwtFont::TextSize(AwtFont* font, int columns, int rows)
{
    HDC hDC = ::GetDC(0);
    DASSERT(hDC != NULL);
    HGDIOBJ oldFont = ::SelectObject(hDC, (font == NULL)
                                           ? ::GetStockObject(SYSTEM_FONT)
                                           : font->GetHFont());

    SIZE size;
    VERIFY(::GetTextExtentPoint(hDC,
        TEXT("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"), 52, &size));

    VERIFY(::SelectObject(hDC, oldFont));
    VERIFY(::ReleaseDC(0, hDC));

    size.cx = size.cx * columns / 52;
    size.cy = size.cy * rows;
    return size;
}

int AwtFont::getFontDescriptorNumber(JNIEnv *env, jobject font,
                                     jobject fontDescriptor)
{
    int i, num = 0;
    jobject refFontDescriptor;
    jobjectArray array;

    if (env->EnsureLocalCapacity(2) < 0)
        return 0;

    if (IsMultiFont(env, font)) {
        array = GetComponentFonts(env, font);
        if (array != NULL) {
            num = env->GetArrayLength(array);
        }
    } else {
        array = NULL;
        num = 0;
    }

    for (i = 0; i < num; i++){
        // Trying to identify the same FontDescriptor by comparing the
        // pointers.
        refFontDescriptor = env->GetObjectArrayElement(array, i);
        if (env->IsSameObject(refFontDescriptor, fontDescriptor)) {
            env->DeleteLocalRef(refFontDescriptor);
            env->DeleteLocalRef(array);
            return i;
        }
        env->DeleteLocalRef(refFontDescriptor);
    }
    env->DeleteLocalRef(array);
    return 0;   // Not found.  Use default.
}

/*
 * This is a faster version of the same function, which does most of
 * the work in Java.
 */
SIZE  AwtFont::DrawStringSize_sub(jstring str, HDC hDC,
                                  jobject font, long x, long y, BOOL draw,
                                  UINT codePage)
{
    SIZE size, temp;
    size.cx = size.cy = 0;

    if (str == NULL) {
        return size;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(3) < 0)
        return size;
    jobjectArray array = 0;

    int arrayLength = 0;

    if (env->GetStringLength(str) == 0) {
        return size;
    }

    //Init AwtFont object, which will "create" a AwtFont object if necessry,
    //before calling makeconvertedMultiFontString(), otherwise, the FontDescriptor's
    //"useUnicode" field might not be initialized correctly (font in Menu Component,
    //for example").
    AwtFont* awtFont = AwtFont::GetFont(env, font);
    if (awtFont == NULL) {
        return size;
    }

    if (IsMultiFont(env, font)) {
        jobject peer = env->CallObjectMethod(font, AwtFont::peerMID);
        if (peer != NULL) {
            array = (jobjectArray)(env->CallObjectMethod(
            peer, AwtFont::makeConvertedMultiFontStringMID, str));
            DASSERT(!safe_ExceptionOccurred(env));

            if (array != NULL) {
                arrayLength = env->GetArrayLength(array);
            }
            env->DeleteLocalRef(peer);
        }
    } else {
        array = NULL;
        arrayLength = 0;
    }

    HFONT oldFont = (HFONT)::SelectObject(hDC, awtFont->GetHFont());

    if (arrayLength == 0) {
        int length = env->GetStringLength(str);
        LPCWSTR strW = JNU_GetStringPlatformChars(env, str, NULL);
        if (strW == NULL) {
            return size;
        }
        VERIFY(::SelectObject(hDC, awtFont->GetHFont()));
        if (AwtComponent::GetRTLReadingOrder()){
            VERIFY(!draw || ::ExtTextOut(hDC, x, y, ETO_RTLREADING, NULL,
                                          strW, length, NULL));
        } else {
            VERIFY(!draw || ::TextOut(hDC, x, y, strW, length));
        }
        VERIFY(::GetTextExtentPoint32(hDC, strW, length, &size));
        JNU_ReleaseStringPlatformChars(env, str, strW);
    } else {
        for (int i = 0; i < arrayLength; i = i + 2) {
            jobject fontDescriptor = env->GetObjectArrayElement(array, i);
            if (fontDescriptor == NULL) {
                break;
            }

            jbyteArray convertedBytes =
                (jbyteArray)env->GetObjectArrayElement(array, i + 1);
            if (convertedBytes == NULL) {
                env->DeleteLocalRef(fontDescriptor);
                break;
            }

            int fdIndex = getFontDescriptorNumber(env, font, fontDescriptor);
            if (env->ExceptionCheck()) {
                return size;  //fdIndex==0 return could be exception or not.
            }
            VERIFY(::SelectObject(hDC, awtFont->GetHFont(fdIndex)));

            /*
             * The strange-looking code that follows this comment is
             * the result of upstream optimizations. In the array of
             * alternating font descriptor and buffers, the buffers
             * contain their length in the first four bytes, a la
             * Pascal arrays.
             *
             * Note: the buffer MUST be unsigned, or VC++ will sign
             * extend buflen and bad things will happen.
             */
            unsigned char* buffer = NULL;
            jboolean unicodeUsed =
                env->GetBooleanField(fontDescriptor, AwtFont::useUnicodeID);
            try {
                buffer = (unsigned char *)
                    env->GetPrimitiveArrayCritical(convertedBytes, 0);
                if (buffer == NULL) {
                    return size;
                }
                int buflen = (buffer[0] << 24) | (buffer[1] << 16) |
                    (buffer[2] << 8) | buffer[3];

                DASSERT(buflen >= 0);

                /*
                 * the offsetBuffer, on the other hand, must be signed because
                 * TextOutA and GetTextExtentPoint32A expect it.
                 */
                char* offsetBuffer = (char *)(buffer + 4);

                if (unicodeUsed) {
                    VERIFY(!draw || ::TextOutW(hDC, x, y, (LPCWSTR)offsetBuffer, buflen / 2));
                    VERIFY(::GetTextExtentPoint32W(hDC, (LPCWSTR)offsetBuffer, buflen / 2, &temp));
                }
                else {
                    VERIFY(!draw || ::TextOutA(hDC, x, y, offsetBuffer, buflen));
                    VERIFY(::GetTextExtentPoint32A(hDC, offsetBuffer, buflen, &temp));
                }
            } catch (...) {
                if (buffer != NULL) {
                    env->ReleasePrimitiveArrayCritical(convertedBytes, buffer,
                                                       0);
                }
                throw;
            }
            env->ReleasePrimitiveArrayCritical(convertedBytes, buffer, 0);

            if (awtFont->textAngle == 0) {
                x += temp.cx;
            } else {
               // account for rotation of the text used in 2D printing.
               double degrees = 360.0 - (awtFont->textAngle/10.0);
               double rads = degrees/(180.0/3.1415926535);
               double dx = temp.cx * cos(rads);
               double dy = temp.cx * sin(rads);
               x += (long)floor(dx+0.5);
               y += (long)floor(dy+0.5);
            }
            size.cx += temp.cx;
            size.cy = (size.cy < temp.cy) ? temp.cy : size.cy;
            env->DeleteLocalRef(fontDescriptor);
            env->DeleteLocalRef(convertedBytes);
        }
    }
    env->DeleteLocalRef(array);

    VERIFY(::SelectObject(hDC, oldFont));
    return size;
}

/************************************************************************
 * WFontMetrics native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WFontMetrics
 * Method:    stringWidth
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WFontMetrics_stringWidth(JNIEnv *env, jobject self,
                                              jstring str)
{
    TRY;

    if (str == NULL) {
        JNU_ThrowNullPointerException(env, "str argument");
        return NULL;
    }
    HDC hDC = ::GetDC(0);    DASSERT(hDC != NULL);

    jobject font = env->GetObjectField(self, AwtFont::fontID);

    long ret = AwtFont::getMFStringWidth(hDC, font, str);
    ret = ScaleDownX(ret);
    VERIFY(::ReleaseDC(0, hDC));
    return ret;

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WFontMetrics
 * Method:    charsWidth
 * Signature: ([CII)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WFontMetrics_charsWidth(JNIEnv *env, jobject self,
                                             jcharArray str,
                                             jint off, jint len)
{
    TRY;

    if (str == NULL) {
        JNU_ThrowNullPointerException(env, "str argument");
        return 0;
    }
    if ((len < 0) || (off < 0) || (len + off < 0) ||
        (len + off > (env->GetArrayLength(str)))) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "off/len argument");
        return 0;
    }

    if (off == env->GetArrayLength(str)) {
        return 0;
    }

    jchar *strp = new jchar[len];
    env->GetCharArrayRegion(str, off, len, strp);
    jstring jstr = env->NewString(strp, len);
    jint result = 0;
    if (jstr != NULL) {
        result = Java_sun_awt_windows_WFontMetrics_stringWidth(env, self,
                                                               jstr);
    }
    delete [] strp;
    return result;

    CATCH_BAD_ALLOC_RET(0);
}


/*
 * Class:     sun_awt_windows_WFontMetrics
 * Method:    bytesWidth
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WFontMetrics_bytesWidth(JNIEnv *env, jobject self,
                                             jbyteArray str,
                                             jint off, jint len)
{
    TRY;

    if (str == NULL) {
        JNU_ThrowNullPointerException(env, "bytes argument");
        return 0;
    }
    if ((len < 0) || (off < 0) || (len + off < 0) ||
        (len + off > (env->GetArrayLength(str)))) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "off or len argument");
        return 0;
    }

    if (off == env->GetArrayLength(str)) {
        return 0;
    }

    char *pStrBody = NULL;
    jint result = 0;
    try {
        jintArray array = (jintArray)env->GetObjectField(self,
                                                         AwtFont::widthsID);
        if (array == NULL) {
            JNU_ThrowNullPointerException(env, "Can't access widths array.");
            return 0;
        }
        pStrBody = (char *)env->GetPrimitiveArrayCritical(str, 0);
        if (pStrBody == NULL) {
            JNU_ThrowNullPointerException(env, "Can't access str bytes.");
            return 0;
        }
        char *pStr = pStrBody + off;

        jint *widths = NULL;
        try {
            widths = (jint *)env->GetPrimitiveArrayCritical(array, 0);
            if (widths == NULL) {
                env->ReleasePrimitiveArrayCritical(str, pStrBody, 0);
                JNU_ThrowNullPointerException(env, "Can't access widths.");
                return 0;
            }
            for (; len; len--) {
                result += widths[*pStr++];
            }
        } catch (...) {
            if (widths != NULL) {
                env->ReleasePrimitiveArrayCritical(array, widths, 0);
            }
            throw;
        }

        env->ReleasePrimitiveArrayCritical(array, widths, 0);

    } catch (...) {
        if (pStrBody != NULL) {
            env->ReleasePrimitiveArrayCritical(str, pStrBody, 0);
        }
        throw;
    }

    env->ReleasePrimitiveArrayCritical(str, pStrBody, 0);
    return ScaleDownX(result);

    CATCH_BAD_ALLOC_RET(0);
}


/*
 * Class:     sun_awt_windows_WFontMetrics
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFontMetrics_init(JNIEnv *env, jobject self)
{
    TRY;

    jobject font = env->GetObjectField(self, AwtFont::fontID);
    if (font == NULL) {
        JNU_ThrowNullPointerException(env, "fontMetrics' font");
        return;
    }
    // This local variable is unused. Is there some subtle side-effect here?
    jlong pData = env->GetLongField(font, AwtFont::pDataID);

    AwtFont::LoadMetrics(env, self);

    CATCH_BAD_ALLOC;
}


/*
 * Class:     sun_awt_windows_WFontMetrics
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFontMetrics_initIDs(JNIEnv *env, jclass cls)
{
   CHECK_NULL(AwtFont::widthsID = env->GetFieldID(cls, "widths", "[I"));
   CHECK_NULL(AwtFont::ascentID = env->GetFieldID(cls, "ascent", "I"));
   CHECK_NULL(AwtFont::descentID = env->GetFieldID(cls, "descent", "I"));
   CHECK_NULL(AwtFont::leadingID = env->GetFieldID(cls, "leading", "I"));
   CHECK_NULL(AwtFont::heightID = env->GetFieldID(cls, "height", "I"));
   CHECK_NULL(AwtFont::maxAscentID = env->GetFieldID(cls, "maxAscent", "I"));
   CHECK_NULL(AwtFont::maxDescentID = env->GetFieldID(cls, "maxDescent", "I"));
   CHECK_NULL(AwtFont::maxHeightID = env->GetFieldID(cls, "maxHeight", "I"));
   AwtFont::maxAdvanceID = env->GetFieldID(cls, "maxAdvance", "I");
}

} /* extern "C" */


/************************************************************************
 * java.awt.Font native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Font_initIDs(JNIEnv *env, jclass cls)
{
    CHECK_NULL(AwtFont::peerMID = env->GetMethodID(cls, "getFontPeer",
         "()Ljava/awt/peer/FontPeer;"));
    CHECK_NULL(AwtFont::pDataID = env->GetFieldID(cls, "pData", "J"));
    CHECK_NULL(AwtFont::nameID =
         env->GetFieldID(cls, "name", "Ljava/lang/String;"));
    CHECK_NULL(AwtFont::sizeID = env->GetFieldID(cls, "size", "I"));
    CHECK_NULL(AwtFont::styleID = env->GetFieldID(cls, "style", "I"));
    AwtFont::getFontMID =
      env->GetStaticMethodID(cls, "getFont",
                             "(Ljava/lang/String;)Ljava/awt/Font;");
}

} /* extern "C" */


/************************************************************************
 * java.awt.FontMetric native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_FontMetrics_initIDs(JNIEnv *env, jclass cls)
{
    CHECK_NULL(AwtFont::fontID =
          env->GetFieldID(cls, "font", "Ljava/awt/Font;"));
    AwtFont::getHeightMID = env->GetMethodID(cls, "getHeight", "()I");
}

} /* extern "C" */

/************************************************************************
 * sun.awt.FontDescriptor native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_FontDescriptor_initIDs(JNIEnv *env, jclass cls)
{
    CHECK_NULL(AwtFont::nativeNameID =
               env->GetFieldID(cls, "nativeName", "Ljava/lang/String;"));
    AwtFont::useUnicodeID = env->GetFieldID(cls, "useUnicode", "Z");

}

} /* extern "C" */


/************************************************************************
 * sun.awt.PlatformFont native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_PlatformFont_initIDs(JNIEnv *env, jclass cls)
{
    CHECK_NULL(AwtFont::fontConfigID =
        env->GetFieldID(cls, "fontConfig", "Lsun/awt/FontConfiguration;"));
    CHECK_NULL(AwtFont::componentFontsID =
        env->GetFieldID(cls, "componentFonts", "[Lsun/awt/FontDescriptor;"));
    AwtFont::makeConvertedMultiFontStringMID =
        env->GetMethodID(cls, "makeConvertedMultiFontString",
                         "(Ljava/lang/String;)[Ljava/lang/Object;");
}

} /* extern "C" */


/************************************************************************
 * sun.awt.windows.WFontPeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WFontPeer_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtFont::textComponentFontNameID = env->GetFieldID(cls, "textComponentFontName", "Ljava/lang/String;");

    DASSERT(AwtFont::textComponentFontNameID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * FontCache methods
 */

void AwtFontCache::Add(WCHAR* name, HFONT font)
{
    fontCache.m_head = new Item(name, font, fontCache.m_head);
}

HFONT AwtFontCache::Lookup(WCHAR* name)
{
    Item* item = fontCache.m_head;
    Item* lastItem = NULL;

    while (item != NULL) {
        if (wcscmp(item->name, name) == 0) {
            return item->font;
        }
        lastItem = item;
        item = item->next;
    }
    return NULL;
}

BOOL AwtFontCache::Search(HFONT font)
{
    Item* item = fontCache.m_head;

    while (item != NULL) {
        if (item->font == font) {
            return TRUE;
        }
        item = item->next;
    }
    return FALSE;
}

void AwtFontCache::Remove(HFONT font)
{
    Item* item = fontCache.m_head;
    Item* lastItem = NULL;

    while (item != NULL) {
        if (item->font == font) {
            if (DecRefCount(item) <= 0){
                if (lastItem == NULL) {
                    fontCache.m_head = item->next;
                } else {
                lastItem->next = item->next;
                }
             delete item;
             }
             return;
        }
        lastItem = item;
        item = item->next;
    }
}

void AwtFontCache::Clear()
{
    Item* item = m_head;
    Item* next;

    while (item != NULL) {
        next = item->next;
        delete item;
        item = next;
    }

    m_head = NULL;
}

/* NOTE: In the interlock calls below the return value is different
         depending on which version of windows. However, all versions
         return a 0 or less than value when the count gets there. Only
         under NT 4.0 & 98 does the value actaully represent the new value. */

void AwtFontCache::IncRefCount(HFONT hFont){
    Item* item = fontCache.m_head;

    while (item != NULL){

        if (item->font == hFont){
            IncRefCount(item);
            return;
        }
        item = item->next;
    }
}

LONG AwtFontCache::IncRefCount(Item* item){
    LONG    newVal = 0;

    if(NULL != item){
        newVal = InterlockedIncrement((long*)&item->refCount);
    }
    return(newVal);
}

LONG AwtFontCache::DecRefCount(Item* item){
    LONG    newVal = 0;

    if(NULL != item){
        newVal = InterlockedDecrement((long*)&item->refCount);
    }
    return(newVal);
}

AwtFontCache::Item::Item(const WCHAR* s, HFONT f, AwtFontCache::Item* n )
{
    name = _wcsdup(s);
    font = f;
    next = n;
    refCount = 1;
}

AwtFontCache::Item::~Item() {
  VERIFY(::DeleteObject(font));
  free(name);
}

/////////////////////////////////////////////////////////////////////////////
// for canConvert native method of WDefaultFontCharset

class CSegTableComponent
{
public:
    CSegTableComponent();
    virtual ~CSegTableComponent();
    virtual void Create(LPCWSTR name);
    virtual BOOL In(USHORT iChar) { DASSERT(FALSE); return FALSE; };
    LPWSTR GetFontName(){
        DASSERT(m_lpszFontName != NULL); return m_lpszFontName;
    };

private:
    LPWSTR m_lpszFontName;
};

CSegTableComponent::CSegTableComponent()
{
    m_lpszFontName = NULL;
}

CSegTableComponent::~CSegTableComponent()
{
    if (m_lpszFontName != NULL) {
        free(m_lpszFontName);
        m_lpszFontName = NULL;
    }
}

void CSegTableComponent::Create(LPCWSTR name)
{
    if (m_lpszFontName != NULL) {
        free(m_lpszFontName);
        m_lpszFontName = NULL;
    }
    m_lpszFontName = _wcsdup(name);
    DASSERT(m_lpszFontName);
}

#define CMAPHEX 0x70616d63 // = "cmap" (reversed)

// CSegTable: Segment table describing character coverage for a font
class CSegTable : public CSegTableComponent
{
public:
    CSegTable();
    virtual ~CSegTable();
    virtual BOOL In(USHORT iChar);
    BOOL HasCmap();
    virtual BOOL IsEUDC() { DASSERT(FALSE); return FALSE; };

protected:
    virtual void GetData(DWORD dwOffset, LPVOID lpData, DWORD cbData) {
        DASSERT(FALSE); };
    void MakeTable();
    static void SwapShort(USHORT& p);
    static void SwapULong(ULONG& p);

private:
    USHORT m_cSegCount;     // number of segments
    PUSHORT m_piStart;      // pointer to array of starting values
    PUSHORT m_piEnd;        // pointer to array of ending values (inclusive)
    USHORT m_cSeg;          // current segment (cache)
};

CSegTable::CSegTable()
{
    m_cSegCount = 0;
    m_piStart = NULL;
    m_piEnd = NULL;
    m_cSeg = 0;
}

CSegTable::~CSegTable()
{
    if (m_piStart != NULL)
        delete[] m_piStart;
    if (m_piEnd != NULL)
        delete[] m_piEnd;
}

#define OFFSETERROR 0

void CSegTable::MakeTable()
{
typedef struct tagTABLE{
    USHORT  platformID;
    USHORT  encodingID;
    ULONG   offset;
} TABLE, *PTABLE;

typedef struct tagSUBTABLE{
    USHORT  format;
    USHORT  length;
    USHORT  version;
    USHORT  segCountX2;
    USHORT  searchRange;
    USHORT  entrySelector;
    USHORT  rangeShift;
} SUBTABLE, *PSUBTABLE;

    USHORT aShort[2];
    (void) GetData(0, aShort, sizeof(aShort));
    USHORT nTables = aShort[1];
    SwapShort(nTables);

    // allocate buffer to hold encoding tables
    DWORD cbData = nTables * sizeof(TABLE);
    PTABLE pTables = new TABLE[nTables];
    PTABLE pTable = pTables;

    // get array of encoding tables.
    (void) GetData(4, (PBYTE) pTable, cbData);

    ULONG offsetFormat4 = OFFSETERROR;
    USHORT i;
    for (i = 0; i < nTables; i++) {
        SwapShort(pTable->encodingID);
        SwapShort(pTable->platformID);
        //for a Unicode font for Windows, platformID == 3, encodingID == 1
        if ((pTable->platformID == 3)&&(pTable->encodingID == 1)) {
            offsetFormat4 = pTable->offset;
            SwapULong(offsetFormat4);
            break;
        }
        pTable++;
    }
    delete[] pTables;
    if (offsetFormat4 == OFFSETERROR) {
        return;
    }
//    DASSERT(offsetFormat4 != OFFSETERROR);

    SUBTABLE subTable;
    (void) GetData(offsetFormat4, &subTable, sizeof(SUBTABLE));
    SwapShort(subTable.format);
    SwapShort(subTable.segCountX2);
    DASSERT(subTable.format == 4);

    m_cSegCount = subTable.segCountX2/2;

    // read in the array of segment end values
    m_piEnd = new USHORT[m_cSegCount];

    ULONG offset = offsetFormat4
        + sizeof(SUBTABLE); //skip constant # bytes in subtable
    cbData = m_cSegCount * sizeof(USHORT);
    (void) GetData(offset, m_piEnd, cbData);
    for (i = 0; i < m_cSegCount; i++)
        SwapShort(m_piEnd[i]);
    DASSERT(m_piEnd[m_cSegCount-1] == 0xffff);

    // read in the array of segment start values
    try {
        m_piStart = new USHORT[m_cSegCount];
    } catch (std::bad_alloc&) {
        delete [] m_piEnd;
        m_piEnd = NULL;
        throw;
    }

    offset += cbData        //skip SegEnd array
        + sizeof(USHORT);   //skip reservedPad
    (void) GetData(offset, m_piStart, cbData);
    for (i = 0; i < m_cSegCount; i++)
        SwapShort(m_piStart[i]);
    DASSERT(m_piStart[m_cSegCount-1] == 0xffff);
}

BOOL CSegTable::In(USHORT iChar)
{
    if (!HasCmap()) {
        return FALSE;
    }
//    DASSERT(m_piStart);
//    DASSERT(m_piEnd);

    if (iChar > m_piEnd[m_cSeg]) {
        for (; (m_cSeg < m_cSegCount)&&(iChar > m_piEnd[m_cSeg]); m_cSeg++);
    } else if (iChar < m_piStart[m_cSeg]) {
        for (; (m_cSeg > 0)&&(iChar < m_piStart[m_cSeg]); m_cSeg--);
    }

    if ((iChar <= m_piEnd[m_cSeg])&&(iChar >= m_piStart[m_cSeg])&&(iChar != 0xffff))
        return TRUE;
    else
        return FALSE;
}

inline BOOL CSegTable::HasCmap()
{
    return (((m_piEnd)&&(m_piStart)) ? TRUE : FALSE);
}

inline void CSegTable::SwapShort(USHORT& p)
{
    SHORT temp;

    temp = (SHORT)(HIBYTE(p) + (LOBYTE(p) << 8));
    p = temp;
}

inline void CSegTable::SwapULong(ULONG& p)
{
    ULONG temp;

    temp = (LONG) ((BYTE) p);
    temp <<= 8;
    p >>= 8;

    temp += (LONG) ((BYTE) p);
    temp <<= 8;
    p >>= 8;

    temp += (LONG) ((BYTE) p);
    temp <<= 8;
    p >>= 8;

    temp += (LONG) ((BYTE) p);
    p = temp;
}

class CStdSegTable : public CSegTable
{
public:
    CStdSegTable();
    virtual ~CStdSegTable();
    BOOL IsEUDC() { return FALSE; };
    virtual void Create(LPCWSTR name);

protected:
    void GetData(DWORD dwOffset, LPVOID lpData, DWORD cbData);

private:
    HDC m_hTmpDC;
};

CStdSegTable::CStdSegTable()
{
    m_hTmpDC = NULL;
}

CStdSegTable::~CStdSegTable()
{
    DASSERT(m_hTmpDC == NULL);
}

inline void CStdSegTable::GetData(DWORD dwOffset,
                                  LPVOID lpData, DWORD cbData)
{
    DASSERT(m_hTmpDC);
    DWORD nBytes =
        ::GetFontData(m_hTmpDC, CMAPHEX, dwOffset, lpData, cbData);
    DASSERT(nBytes != GDI_ERROR);
}

void CStdSegTable::Create(LPCWSTR name)
{
    CSegTableComponent::Create(name);

    HWND hWnd = ::GetDesktopWindow();
    DASSERT(hWnd);
    m_hTmpDC = ::GetWindowDC(hWnd);
    DASSERT(m_hTmpDC);

    HFONT hFont = CreateHFont_sub(name, 0, 20);

    HFONT hOldFont = (HFONT)::SelectObject(m_hTmpDC, hFont);
    DASSERT(hOldFont);

    (void) MakeTable();

    VERIFY(::SelectObject(m_hTmpDC, hOldFont));
    VERIFY(::DeleteObject(hFont));
    VERIFY(::ReleaseDC(hWnd, m_hTmpDC) != 0);
    m_hTmpDC = NULL;
}

class CEUDCSegTable : public CSegTable
{
public:
    CEUDCSegTable();
    virtual ~CEUDCSegTable();
    BOOL IsEUDC() { return TRUE; };
    virtual void Create(LPCWSTR name);

protected:
    void GetData(DWORD dwOffset, LPVOID lpData, DWORD cbData);

private:
    HANDLE m_hTmpFile;
    ULONG m_hTmpCMapOffset;
};

CEUDCSegTable::CEUDCSegTable()
{
    m_hTmpFile = NULL;
    m_hTmpCMapOffset = 0;
}

CEUDCSegTable::~CEUDCSegTable()
{
    DASSERT(m_hTmpFile == NULL);
    DASSERT(m_hTmpCMapOffset == 0);
}

inline void CEUDCSegTable::GetData(DWORD dwOffset,
                                   LPVOID lpData, DWORD cbData)
{
    DASSERT(m_hTmpFile);
    DASSERT(m_hTmpCMapOffset);
    ::SetFilePointer(m_hTmpFile, m_hTmpCMapOffset + dwOffset,
        NULL, FILE_BEGIN);
    DWORD dwRead;
    VERIFY(::ReadFile(m_hTmpFile, lpData, cbData, &dwRead, NULL));
    DASSERT(dwRead == cbData);
}

void CEUDCSegTable::Create(LPCWSTR name)
{
typedef struct tagHEAD{
    FIXED   sfnt_version;
    USHORT  numTables;
    USHORT  searchRange;
    USHORT  entrySelector;
    USHORT  rangeShift;
} HEAD, *PHEAD;

typedef struct tagENTRY{
    ULONG   tag;
    ULONG   checkSum;
    ULONG   offset;
    ULONG   length;
} ENTRY, *PENTRY;

    CSegTableComponent::Create(name);

    // create EUDC font file and make EUDCSegTable
    // after wrapper function for CreateFileW, we use only CreateFileW
    m_hTmpFile = ::CreateFile(name, GENERIC_READ,
                               FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (m_hTmpFile == INVALID_HANDLE_VALUE){
        m_hTmpFile = NULL;
        return;
    }

    HEAD head;
    DWORD dwRead;
    VERIFY(::ReadFile(m_hTmpFile, &head, sizeof(head), &dwRead, NULL));
    DASSERT(dwRead == sizeof(HEAD));
    SwapShort(head.numTables);
    ENTRY entry;
    for (int i = 0; i < head.numTables; i++){
        VERIFY(::ReadFile(m_hTmpFile, &entry, sizeof(entry), &dwRead, NULL));
        DASSERT(dwRead == sizeof(ENTRY));
        if (entry.tag == CMAPHEX)
            break;
    }
    DASSERT(entry.tag == CMAPHEX);
    SwapULong(entry.offset);
    m_hTmpCMapOffset = entry.offset;

    (void) MakeTable();

    m_hTmpCMapOffset = 0;
    VERIFY(::CloseHandle(m_hTmpFile));
    m_hTmpFile = NULL;
}

class CSegTableManagerComponent
{
public:
    CSegTableManagerComponent();
    ~CSegTableManagerComponent();

protected:
    void MakeBiggerTable();
    CSegTableComponent **m_tables;
    int m_nTable;
    int m_nMaxTable;
};

#define TABLENUM 20

CSegTableManagerComponent::CSegTableManagerComponent()
{
    m_nTable = 0;
    m_nMaxTable = TABLENUM;
    m_tables = new CSegTableComponent*[m_nMaxTable];
}

CSegTableManagerComponent::~CSegTableManagerComponent()
{
    for (int i = 0; i < m_nTable; i++) {
        DASSERT(m_tables[i]);
        delete m_tables[i];
    }
    delete [] m_tables;
    m_tables = NULL;
}

void CSegTableManagerComponent::MakeBiggerTable()
{
    CSegTableComponent **tables =
        new CSegTableComponent*[m_nMaxTable + TABLENUM];

    for (int i = 0; i < m_nMaxTable; i++)
        tables[i] = m_tables[i];

    delete[] m_tables;

    m_tables = tables;
    m_nMaxTable += TABLENUM;
}

class CSegTableManager : public CSegTableManagerComponent
{
public:
    CSegTable* GetTable(LPCWSTR lpszFontName, BOOL fEUDC);
};

CSegTable* CSegTableManager::GetTable(LPCWSTR lpszFontName, BOOL fEUDC)
{
    for (int i = 0; i < m_nTable; i++) {
        if ((((CSegTable*)m_tables[i])->IsEUDC() == fEUDC) &&
            (wcscmp(m_tables[i]->GetFontName(),lpszFontName) == 0))
            return (CSegTable*) m_tables[i];
    }

    if (m_nTable == m_nMaxTable) {
        (void) MakeBiggerTable();
    }
    DASSERT(m_nTable < m_nMaxTable);

    if (!fEUDC) {
        m_tables[m_nTable] = new CStdSegTable;
    } else {
        m_tables[m_nTable] = new CEUDCSegTable;
    }
    m_tables[m_nTable]->Create(lpszFontName);
    return (CSegTable*) m_tables[m_nTable++];
}

CSegTableManager g_segTableManager;

#define KEYLEN 16

class CCombinedSegTable : public CSegTableComponent
{
public:
    CCombinedSegTable();
    void Create(LPCWSTR name);
    BOOL In(USHORT iChar);

private:
    LPSTR GetCodePageSubkey();
    void GetEUDCFileName(LPWSTR lpszFileName, int cchFileName);
    static char m_szCodePageSubkey[KEYLEN];
    static WCHAR m_szDefaultEUDCFile[_MAX_PATH];
    static BOOL m_fEUDCSubKeyExist;
    static BOOL m_fTTEUDCFileExist;
    CStdSegTable* m_pStdSegTable;
    CEUDCSegTable* m_pEUDCSegTable;
};

char CCombinedSegTable::m_szCodePageSubkey[KEYLEN] = "";

WCHAR CCombinedSegTable::m_szDefaultEUDCFile[_MAX_PATH] = L"";

BOOL CCombinedSegTable::m_fEUDCSubKeyExist = TRUE;

BOOL CCombinedSegTable::m_fTTEUDCFileExist = TRUE;

CCombinedSegTable::CCombinedSegTable()
{
    m_pStdSegTable = NULL;
    m_pEUDCSegTable = NULL;
}

#include <locale.h>
LPSTR CCombinedSegTable::GetCodePageSubkey()
{
    if (strlen(m_szCodePageSubkey) > 0) {
        return m_szCodePageSubkey;
    }

    LPSTR lpszLocale = setlocale(LC_CTYPE, "");
    // cf lpszLocale = "Japanese_Japan.932"
    if (lpszLocale == NULL) {
        return NULL;
    }
    LPSTR lpszCP = strchr(lpszLocale, (int) '.');
    if (lpszCP == NULL) {
        return NULL;
    }
    lpszCP++; // cf lpszCP = "932"

    char szSubKey[KEYLEN];
    strcpy(szSubKey, "EUDC\\");
    if ((strlen(szSubKey) + strlen(lpszCP)) >= KEYLEN) {
        return NULL;
    }
    strcpy(&(szSubKey[strlen(szSubKey)]), lpszCP);
    strcpy(m_szCodePageSubkey, szSubKey);
    return m_szCodePageSubkey;
}

void CCombinedSegTable::GetEUDCFileName(LPWSTR lpszFileName, int cchFileName)
{
    if (m_fEUDCSubKeyExist == FALSE)
        return;

    // get filename of typeface-specific TureType EUDC font
    LPSTR lpszSubKey = GetCodePageSubkey();
    if (lpszSubKey == NULL) {
        m_fEUDCSubKeyExist = FALSE;
        return; // can not get codepage information
    }
    HKEY hRootKey = HKEY_CURRENT_USER;
    HKEY hKey;
    LONG lRet = ::RegOpenKeyExA(hRootKey, lpszSubKey, 0, KEY_ALL_ACCESS, &hKey);
    if (lRet != ERROR_SUCCESS) {
        m_fEUDCSubKeyExist = FALSE;
        return; // no EUDC font
    }

    // get EUDC font file name
    WCHAR szFamilyName[80];
    wcscpy(szFamilyName, GetFontName());
    WCHAR* delimit = wcschr(szFamilyName, L',');
    if (delimit != NULL)
        *delimit = L'\0';
    DWORD dwType;
    UCHAR szFileName[_MAX_PATH];
    ::ZeroMemory(szFileName, sizeof(szFileName));
    DWORD dwBytes = sizeof(szFileName);
    // try Typeface-specific EUDC font
    char szTmpName[80];
    VERIFY(::WideCharToMultiByte(CP_ACP, 0, szFamilyName, -1,
        szTmpName, sizeof(szTmpName), NULL, NULL));
    LONG lStatus = ::RegQueryValueExA(hKey, (LPCSTR) szTmpName,
        NULL, &dwType, szFileName, &dwBytes);
    BOOL fUseDefault = FALSE;
    if (lStatus != ERROR_SUCCESS){ // try System default EUDC font
        if (m_fTTEUDCFileExist == FALSE)
            return;
        if (wcslen(m_szDefaultEUDCFile) > 0) {
            wcscpy(lpszFileName, m_szDefaultEUDCFile);
            return;
        }
        char szDefault[] = "SystemDefaultEUDCFont";
        lStatus = ::RegQueryValueExA(hKey, (LPCSTR) szDefault,
            NULL, &dwType, szFileName, &dwBytes);
        fUseDefault = TRUE;
        if (lStatus != ERROR_SUCCESS) {
            m_fTTEUDCFileExist = FALSE;
            // This font is associated with no EUDC font
            // and there is no system default EUDC font
            return;
        }
    }

    if (strcmp((LPCSTR) szFileName, "userfont.fon") == 0) {
        // This font is associated with no EUDC font
        // and the system default EUDC font is not TrueType
        m_fTTEUDCFileExist = FALSE;
        return;
    }

    DASSERT(strlen((LPCSTR)szFileName) > 0);
    VERIFY(::MultiByteToWideChar(CP_ACP, 0,
        (LPCSTR)szFileName, -1, lpszFileName, cchFileName) != 0);
    if (fUseDefault)
        wcscpy(m_szDefaultEUDCFile, lpszFileName);
}

void CCombinedSegTable::Create(LPCWSTR name)
{
    CSegTableComponent::Create(name);

    m_pStdSegTable =
        (CStdSegTable*) g_segTableManager.GetTable(name, FALSE/*not EUDC*/);
    WCHAR szEUDCFileName[_MAX_PATH];
    ::ZeroMemory(szEUDCFileName, sizeof(szEUDCFileName));
    (void) GetEUDCFileName(szEUDCFileName,
        sizeof(szEUDCFileName)/sizeof(WCHAR));
    if (wcslen(szEUDCFileName) > 0) {
        m_pEUDCSegTable = (CEUDCSegTable*) g_segTableManager.GetTable(
            szEUDCFileName, TRUE/*EUDC*/);
        if (m_pEUDCSegTable->HasCmap() == FALSE)
            m_pEUDCSegTable = NULL;
    }
}

BOOL CCombinedSegTable::In(USHORT iChar)
{
    DASSERT(m_pStdSegTable);
    if (m_pStdSegTable->In(iChar))
        return TRUE;

    if (m_pEUDCSegTable != NULL)
        return m_pEUDCSegTable->In(iChar);

    return FALSE;
}

class CCombinedSegTableManager : public CSegTableManagerComponent
{
public:
    CCombinedSegTable* GetTable(LPCWSTR lpszFontName);
};

CCombinedSegTable* CCombinedSegTableManager::GetTable(LPCWSTR lpszFontName)
{
    for (int i = 0; i < m_nTable; i++) {
        if (wcscmp(m_tables[i]->GetFontName(),lpszFontName) == 0)
            return (CCombinedSegTable*) m_tables[i];
    }

    if (m_nTable == m_nMaxTable) {
        (void) MakeBiggerTable();
    }
    DASSERT(m_nTable < m_nMaxTable);

    m_tables[m_nTable] = new CCombinedSegTable;
    m_tables[m_nTable]->Create(lpszFontName);

    return (CCombinedSegTable*) m_tables[m_nTable++];
}


/************************************************************************
 * WDefaultFontCharset native methos
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDefaultFontCharset_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtFont::fontNameID = env->GetFieldID(cls, "fontName",
                                          "Ljava/lang/String;");
    DASSERT(AwtFont::fontNameID != NULL);

    CATCH_BAD_ALLOC;
}


/*
 * !!!!!!!!!!!!!!!!!!!! this does not work. I am not sure why, but
 * when active, this will reliably crash HJ, with no hope of debugging
 * for java.  It doesn't seem to crash the _g version.
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 * I suspect may be running out of C stack: see alloca in
 * JNI_GET_STRING, the alloca in it.
 *
 * (the method is prefixed with XXX so that the linker won't find it) */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WDefaultFontCharset_canConvert(JNIEnv *env, jobject self,
                                                    jchar ch)
{
    TRY;

    static CCombinedSegTableManager tableManager;

    jstring fontName = (jstring)env->GetObjectField(self, AwtFont::fontNameID);
    DASSERT(fontName != NULL); // leave in for debug mode.
    CHECK_NULL_RETURN(fontName, FALSE);  // in production, just return
    LPCWSTR fontNameW = JNU_GetStringPlatformChars(env, fontName, NULL);
    CHECK_NULL_RETURN(fontNameW, FALSE);
    CCombinedSegTable* pTable = tableManager.GetTable(fontNameW);
    JNU_ReleaseStringPlatformChars(env, fontName, fontNameW);
    return (pTable->In((USHORT) ch) ? JNI_TRUE : JNI_FALSE);

    CATCH_BAD_ALLOC_RET(FALSE);
}

} /* extern "C" */
