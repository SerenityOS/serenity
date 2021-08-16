/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "java_awt_Font.h"
#import "sun_awt_PlatformFont.h"
#import "sun_awt_FontDescriptor.h"
#import "sun_font_CFont.h"
#import "sun_font_CFontManager.h"

#import "AWTFont.h"
#import "AWTStrike.h"
#import "CoreTextSupport.h"
#import "JNIUtilities.h"

@implementation AWTFont

- (id) initWithFont:(NSFont *)font {
    self = [super init];
    if (self) {
        fFont = [font retain];
        fNativeCGFont = CTFontCopyGraphicsFont((CTFontRef)font, NULL);
    }
    return self;
}

- (void) dealloc {
    [fFont release];
    fFont = nil;

    if (fNativeCGFont) {
        CGFontRelease(fNativeCGFont);
    fNativeCGFont = NULL;
    }

    [super dealloc];
}

- (void) finalize {
    if (fNativeCGFont) {
        CGFontRelease(fNativeCGFont);
    fNativeCGFont = NULL;
    }
    [super finalize];
}

static NSString* uiName = nil;
static NSString* uiBoldName = nil;

+ (AWTFont *) awtFontForName:(NSString *)name
                       style:(int)style
{
    // create font with family & size
    NSFont *nsFont = nil;

    if ((uiName != nil && [name isEqualTo:uiName]) ||
        (uiBoldName != nil && [name isEqualTo:uiBoldName])) {
        if (style & java_awt_Font_BOLD) {
            nsFont = [NSFont boldSystemFontOfSize:1.0];
        } else {
            nsFont = [NSFont systemFontOfSize:1.0];
        }
#ifdef DEBUG
        NSLog(@"nsFont-name is : %@", nsFont.familyName);
        NSLog(@"nsFont-family is : %@", nsFont.fontName);
        NSLog(@"nsFont-desc-name is : %@", nsFont.fontDescriptor.postscriptName);
#endif


    } else {
           nsFont = [NSFont fontWithName:name size:1.0];
    }

    if (nsFont == nil) {
        // if can't get font of that name, substitute system default font
        nsFont = [NSFont fontWithName:@"Lucida Grande" size:1.0];
#ifdef DEBUG
        NSLog(@"needed to substitute Lucida Grande for: %@", name);
#endif
    }

    // create an italic style (if one is installed)
    if (style & java_awt_Font_ITALIC) {
        nsFont = [[NSFontManager sharedFontManager] convertFont:nsFont toHaveTrait:NSItalicFontMask];
    }

    // create a bold style (if one is installed)
    if (style & java_awt_Font_BOLD) {
        nsFont = [[NSFontManager sharedFontManager] convertFont:nsFont toHaveTrait:NSBoldFontMask];
    }

    return [[[AWTFont alloc] initWithFont:nsFont] autorelease];
}

+ (NSFont *) nsFontForJavaFont:(jobject)javaFont env:(JNIEnv *)env {
    if (javaFont == NULL) {
#ifdef DEBUG
        NSLog(@"nil font");
#endif
        return nil;
    }

    DECLARE_CLASS_RETURN(jc_Font, "java/awt/Font", nil);

    // obtain the Font2D
    DECLARE_METHOD_RETURN(jm_Font_getFont2D, jc_Font, "getFont2D", "()Lsun/font/Font2D;", nil);
    jobject font2d = (*env)->CallObjectMethod(env, javaFont, jm_Font_getFont2D);
    CHECK_EXCEPTION();
    if (font2d == NULL) {
#ifdef DEBUG
        NSLog(@"nil font2d");
#endif
        return nil;
    }

    // if it's not a CFont, it's likely one of TTF or OTF fonts
    // from the Sun rendering loops
    DECLARE_CLASS_RETURN(jc_CFont, "sun/font/CFont", nil);
    if (!(*env)->IsInstanceOf(env, font2d, jc_CFont)) {
#ifdef DEBUG
        NSLog(@"font2d !instanceof CFont");
#endif
        return nil;
    }

    DECLARE_METHOD_RETURN(jm_CFont_getFontStrike, jc_CFont, "getStrike", "(Ljava/awt/Font;)Lsun/font/FontStrike;", nil);
    jobject fontStrike = (*env)->CallObjectMethod(env, font2d, jm_CFont_getFontStrike, javaFont);
    CHECK_EXCEPTION();
    DECLARE_CLASS_RETURN(jc_CStrike, "sun/font/CStrike", nil);
    if (!(*env)->IsInstanceOf(env, fontStrike, jc_CStrike)) {
#ifdef DEBUG
        NSLog(@"fontStrike !instanceof CStrike");
#endif
        return nil;
    }

    DECLARE_METHOD_RETURN(jm_CStrike_nativeStrikePtr, jc_CStrike, "getNativeStrikePtr", "()J", nil);
    jlong awtStrikePtr = (*env)->CallLongMethod(env, fontStrike, jm_CStrike_nativeStrikePtr);
    CHECK_EXCEPTION();
    if (awtStrikePtr == 0L) {
#ifdef DEBUG
        NSLog(@"nil nativeFontPtr from CFont");
#endif
        return nil;
    }

    AWTStrike *strike = (AWTStrike *)jlong_to_ptr(awtStrikePtr);

    return [NSFont fontWithName:[strike->fAWTFont->fFont fontName] matrix:(CGFloat *)(&(strike->fAltTx))];
}

@end


#pragma mark --- Font Discovery and Loading ---

static NSArray* sFilteredFonts = nil;
static NSDictionary* sFontFamilyTable = nil;

static NSString*
GetFamilyNameForFontName(NSString* fontname)
{
    return [sFontFamilyTable objectForKey:fontname];
}

static void addFont(CTFontUIFontType uiType,
                    NSMutableArray *allFonts,
                    NSMutableDictionary* fontFamilyTable) {

        CTFontRef font = CTFontCreateUIFontForLanguage(uiType, 0.0, NULL);
        if (font == NULL) {
            return;
        }
        CTFontDescriptorRef desc = CTFontCopyFontDescriptor(font);
        if (desc == NULL) {
            CFRelease(font);
            return;
        }
        CFStringRef family = CTFontDescriptorCopyAttribute(desc, kCTFontFamilyNameAttribute);
        if (family == NULL) {
            CFRelease(desc);
            CFRelease(font);
            return;
        }
        CFStringRef name = CTFontDescriptorCopyAttribute(desc, kCTFontNameAttribute);
        if (name == NULL) {
            CFRelease(family);
            CFRelease(desc);
            CFRelease(font);
            return;
        }
        if (uiType == kCTFontUIFontSystem) {
            uiName = (NSString*)name;
        }
        if (uiType == kCTFontUIFontEmphasizedSystem) {
            uiBoldName = (NSString*)name;
        }
        [allFonts addObject:name];
        [fontFamilyTable setObject:family forKey:name];
#ifdef DEBUG
        NSLog(@"name is : %@", (NSString*)name);
        NSLog(@"family is : %@", (NSString*)family);
#endif
        CFRelease(family);
        CFRelease(name);
        CFRelease(desc);
        CFRelease(font);
}

static NSArray*
GetFilteredFonts()
{
    if (sFilteredFonts == nil) {
        NSFontManager *fontManager = [NSFontManager sharedFontManager];
        NSUInteger fontCount = [[fontManager availableFonts] count];

        NSMutableArray *allFonts = [[NSMutableArray alloc] initWithCapacity:fontCount];
        NSMutableDictionary* fontFamilyTable = [[NSMutableDictionary alloc] initWithCapacity:fontCount];
        NSArray *allFamilies = [fontManager availableFontFamilies];

        NSUInteger familyCount = [allFamilies count];

        NSUInteger familyIndex;
        for (familyIndex = 0; familyIndex < familyCount; familyIndex++) {
            NSString *family = [allFamilies objectAtIndex:familyIndex];

            if ((family == nil) || [family characterAtIndex:0] == '.') {
                continue;
            }

            NSArray *fontFaces = [fontManager availableMembersOfFontFamily:family];
            NSUInteger faceCount = [fontFaces count];

            NSUInteger faceIndex;
            for (faceIndex = 0; faceIndex < faceCount; faceIndex++) {
                NSString* face = [[fontFaces objectAtIndex:faceIndex] objectAtIndex:0];
                if (face != nil) {
                    [allFonts addObject:face];
                    [fontFamilyTable setObject:family forKey:face];
                }
            }
        }

        /*
         * JavaFX registers these fonts and so JDK needs to do so as well.
         * If this isn't done we will have mis-matched rendering, since
         * although these may include fonts that are enumerated normally
         * they also demonstrably includes fonts that are not.
         */
        addFont(kCTFontUIFontSystem, allFonts, fontFamilyTable);
        addFont(kCTFontUIFontEmphasizedSystem, allFonts, fontFamilyTable);

        sFilteredFonts = allFonts;
        sFontFamilyTable = fontFamilyTable;
    }

    return sFilteredFonts;
}

#pragma mark --- sun.font.CFontManager JNI ---

static OSStatus CreateFSRef(FSRef *myFSRefPtr, NSString *inPath)
{
    return FSPathMakeRef((UInt8 *)[inPath fileSystemRepresentation],
                         myFSRefPtr, NULL);
}

/*
 * Class:     sun_font_CFontManager
 * Method:    loadNativeFonts
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_font_CFontManager_loadNativeFonts
    (JNIEnv *env, jobject jthis)
{
    DECLARE_CLASS(jc_CFontManager, "sun/font/CFontManager");
    DECLARE_METHOD(jm_registerFont, jc_CFontManager, "registerFont", "(Ljava/lang/String;Ljava/lang/String;)V");

    jint num = 0;

JNI_COCOA_ENTER(env);

    NSArray *filteredFonts = GetFilteredFonts();
    num = (jint)[filteredFonts count];

    jint i;
    for (i = 0; i < num; i++) {
        NSString *fontname = [filteredFonts objectAtIndex:i];
        jobject jFontName = NSStringToJavaString(env, fontname);
        jobject jFontFamilyName =
            NSStringToJavaString(env, GetFamilyNameForFontName(fontname));

        (*env)->CallVoidMethod(env, jthis, jm_registerFont, jFontName, jFontFamilyName);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, jFontName);
        (*env)->DeleteLocalRef(env, jFontFamilyName);
    }

JNI_COCOA_EXIT(env);
}

/*
 * Class:     Java_sun_font_CFontManager_loadNativeDirFonts
 * Method:    loadNativeDirFonts
 * Signature: (Ljava/lang/String;)V;
 */
JNIEXPORT void JNICALL
Java_sun_font_CFontManager_loadNativeDirFonts
(JNIEnv *env, jclass clz, jstring filename)
{
JNI_COCOA_ENTER(env);

    NSString *path = JavaStringToNSString(env, filename);
    NSURL *url = [NSURL fileURLWithPath:(NSString *)path];
    bool res = CTFontManagerRegisterFontsForURL((CFURLRef)url, kCTFontManagerScopeProcess, nil);
#ifdef DEBUG
    NSLog(@"path is : %@", (NSString*)path);
    NSLog(@"url is : %@", (NSString*)url);
    printf("res is %d\n", res);
#endif
JNI_COCOA_EXIT(env);
}

#pragma mark --- sun.font.CFont JNI ---

/*
 * Class:     sun_font_CFont
 * Method:    getPlatformFontPtrNative
 * Signature: (JI)[B
 */
JNIEXPORT jlong JNICALL
Java_sun_font_CFont_getCGFontPtrNative
    (JNIEnv *env, jclass clazz,
     jlong awtFontPtr)
{
    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    return (jlong)(awtFont->fNativeCGFont);
}

/*
 * Class:     sun_font_CFont
 * Method:    getTableBytesNative
 * Signature: (JI)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_font_CFont_getTableBytesNative
    (JNIEnv *env, jclass clazz,
     jlong awtFontPtr, jint jtag)
{
    jbyteArray jbytes = NULL;
JNI_COCOA_ENTER(env);

    CTFontTableTag tag = (CTFontTableTag)jtag;
    int i, found = 0;
    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    NSFont* nsFont = awtFont->fFont;
    CTFontRef ctfont = (CTFontRef)nsFont;
    CFArrayRef tagsArray =
        CTFontCopyAvailableTables(ctfont, kCTFontTableOptionNoOptions);
    CFIndex numTags = CFArrayGetCount(tagsArray);
    for (i=0; i<numTags; i++) {
        if (tag ==
            (CTFontTableTag)(uintptr_t)CFArrayGetValueAtIndex(tagsArray, i)) {
            found = 1;
            break;
        }
    }
    CFRelease(tagsArray);
    if (!found) {
        return NULL;
    }
    CFDataRef table = CTFontCopyTable(ctfont, tag, kCTFontTableOptionNoOptions);
    if (table == NULL) {
        return NULL;
    }

    char *tableBytes = (char*)(CFDataGetBytePtr(table));
    size_t tableLength = CFDataGetLength(table);
    if (tableBytes == NULL || tableLength == 0) {
        CFRelease(table);
        return NULL;
    }

    jbytes = (*env)->NewByteArray(env, (jsize)tableLength);
    if (jbytes == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, jbytes, 0,
                               (jsize)tableLength,
                               (jbyte*)tableBytes);
    CFRelease(table);

JNI_COCOA_EXIT(env);

    return jbytes;
}

/*
 * Class:     sun_font_CFont
 * Method:    initNativeFont
 * Signature: (Ljava/lang/String;I)J
 */
JNIEXPORT jlong JNICALL
Java_sun_font_CFont_createNativeFont
    (JNIEnv *env, jclass clazz,
     jstring nativeFontName, jint style)
{
    AWTFont *awtFont = nil;

JNI_COCOA_ENTER(env);

    awtFont =
        [AWTFont awtFontForName:JavaStringToNSString(env, nativeFontName)
         style:style]; // autoreleased

    if (awtFont) {
        CFRetain(awtFont); // GC
    }

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(awtFont);
}

/*
 * Class:     sun_font_CFont
 * Method:    getWidthNative
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL
Java_sun_font_CFont_getWidthNative
    (JNIEnv *env, jobject cfont, jlong awtFontPtr)
{
    float widthVal;
JNI_COCOA_ENTER(env);

    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    NSFont* nsFont = awtFont->fFont;
    NSFontDescriptor *fontDescriptor = nsFont.fontDescriptor;
    NSDictionary *fontTraits = [fontDescriptor objectForKey : NSFontTraitsAttribute];
    NSNumber *width = [fontTraits objectForKey : NSFontWidthTrait];
    widthVal = (float)[width floatValue];

JNI_COCOA_EXIT(env);
   return (jfloat)widthVal;
}

/*
 * Class:     sun_font_CFont
 * Method:    getWeightNative
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL
Java_sun_font_CFont_getWeightNative
    (JNIEnv *env, jobject cfont, jlong awtFontPtr)
{
    float weightVal;
JNI_COCOA_ENTER(env);

    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    NSFont* nsFont = awtFont->fFont;
    NSFontDescriptor *fontDescriptor = nsFont.fontDescriptor;
    NSDictionary *fontTraits = [fontDescriptor objectForKey : NSFontTraitsAttribute];
    NSNumber *weight = [fontTraits objectForKey : NSFontWeightTrait];
    weightVal = (float)[weight floatValue];

JNI_COCOA_EXIT(env);
   return (jfloat)weightVal;
}

/*
 * Class:     sun_font_CFont
 * Method:    disposeNativeFont
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_font_CFont_disposeNativeFont
    (JNIEnv *env, jclass clazz, jlong awtFontPtr)
{
JNI_COCOA_ENTER(env);

    if (awtFontPtr) {
        CFRelease((AWTFont *)jlong_to_ptr(awtFontPtr)); // GC
    }

JNI_COCOA_EXIT(env);
}


#pragma mark --- Miscellaneous JNI ---

#ifndef HEADLESS
/*
 * Class:     sun_awt_PlatformFont
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_PlatformFont_initIDs
    (JNIEnv *env, jclass cls)
{
}

/*
 * Class:     sun_awt_FontDescriptor
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_FontDescriptor_initIDs
    (JNIEnv *env, jclass cls)
{
}
#endif

/*
 * Class:     sun_awt_FontDescriptor
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_font_CFont_getCascadeList
    (JNIEnv *env, jclass cls, jlong awtFontPtr, jobject arrayListOfString)
{
JNI_COCOA_ENTER(env);
    jclass alc = (*env)->FindClass(env, "java/util/ArrayList");
    if (alc == NULL) return;
    jmethodID addMID = (*env)->GetMethodID(env, alc, "add", "(Ljava/lang/Object;)Z");
    if (addMID == NULL) return;

    CFIndex i;
    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    NSFont* nsFont = awtFont->fFont;
    CTFontRef font = (CTFontRef)nsFont;
    CFArrayRef codes = CFLocaleCopyISOLanguageCodes();

#ifdef DEBUG
    CFStringRef base = CTFontCopyFullName(font);
    NSLog(@"BaseFont is : %@", (NSString*)base);
    CFRelease(base);
#endif
    CFArrayRef fds = CTFontCopyDefaultCascadeListForLanguages(font, codes);
    CFRelease(codes);
    CFIndex cnt = CFArrayGetCount(fds);
    for (i=0; i<cnt; i++) {
        CTFontDescriptorRef ref = CFArrayGetValueAtIndex(fds, i);
        CFStringRef fontname =
            CTFontDescriptorCopyAttribute(ref, kCTFontNameAttribute);
#ifdef DEBUG
        NSLog(@"Font is : %@", (NSString*)fontname);
#endif
        jstring jFontName = (jstring)NSStringToJavaString(env, fontname);
        CFRelease(fontname);
        (*env)->CallBooleanMethod(env, arrayListOfString, addMID, jFontName);
        if ((*env)->ExceptionOccurred(env)) {
            CFRelease(fds);
            return;
        }
        (*env)->DeleteLocalRef(env, jFontName);
    }
    CFRelease(fds);
JNI_COCOA_EXIT(env);
}

static CFStringRef EMOJI_FONT_NAME = CFSTR("Apple Color Emoji");

bool IsEmojiFont(CTFontRef font)
{
    CFStringRef name = CTFontCopyFullName(font);
    if (name == NULL) return false;
    bool isFixedColor = CFStringCompare(name, EMOJI_FONT_NAME, 0) == kCFCompareEqualTo;
    CFRelease(name);
    return isFixedColor;
}
