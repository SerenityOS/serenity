/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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


#import "java_awt_print_PageFormat.h"
#import "java_awt_print_Pageable.h"
#import "sun_lwawt_macosx_CPrinterJob.h"
#import "sun_lwawt_macosx_CPrinterPageDialog.h"

#import <Cocoa/Cocoa.h>

#import "PrinterView.h"
#import "PrintModel.h"
#import "ThreadUtilities.h"
#import "GeomUtilities.h"
#import "JNIUtilities.h"

static jclass sjc_Paper = NULL;
static jclass sjc_PageFormat = NULL;
static jclass sjc_CPrinterJob = NULL;
static jclass sjc_CPrinterDialog = NULL;
static jmethodID sjm_getNSPrintInfo = NULL;
static jmethodID sjm_printerJob = NULL;

#define GET_PAPER_CLASS() GET_CLASS(sjc_Paper, "java/awt/print/Paper");
#define GET_PAGEFORMAT_CLASS() GET_CLASS(sjc_PageFormat, "java/awt/print/PageFormat");
#define GET_CPRINTERDIALOG_CLASS() GET_CLASS(sjc_CPrinterDialog, "sun/lwawt/macosx/CPrinterDialog");
#define GET_CPRINTERDIALOG_CLASS_RETURN(ret) GET_CLASS_RETURN(sjc_CPrinterDialog, "sun/lwawt/macosx/CPrinterDialog", ret);
#define GET_CPRINTERJOB_CLASS() GET_CLASS(sjc_CPrinterJob, "sun/lwawt/macosx/CPrinterJob");
#define GET_CPRINTERJOB_CLASS_RETURN(ret) GET_CLASS_RETURN(sjc_CPrinterJob, "sun/lwawt/macosx/CPrinterJob", ret);

#define GET_NSPRINTINFO_METHOD_RETURN(ret) \
    GET_CPRINTERJOB_CLASS_RETURN(ret); \
    GET_METHOD_RETURN(sjm_getNSPrintInfo, sjc_CPrinterJob, "getNSPrintInfo", "()J", ret);

#define GET_CPRINTERDIALOG_FIELD_RETURN(ret) \
   GET_CPRINTERDIALOG_CLASS_RETURN(ret); \
   GET_FIELD_RETURN(sjm_printerJob, sjc_CPrinterDialog, "fPrinterJob", "Lsun/lwawt/macosx/CPrinterJob;", ret);

static NSPrintInfo* createDefaultNSPrintInfo();

static void makeBestFit(NSPrintInfo* src);

static void nsPrintInfoToJavaPaper(JNIEnv* env, NSPrintInfo* src, jobject dst);
static void javaPaperToNSPrintInfo(JNIEnv* env, jobject src, NSPrintInfo* dst);

static void nsPrintInfoToJavaPageFormat(JNIEnv* env, NSPrintInfo* src, jobject dst);
static void javaPageFormatToNSPrintInfo(JNIEnv* env, jobject srcPrinterJob, jobject srcPageFormat, NSPrintInfo* dst);

static void nsPrintInfoToJavaPrinterJob(JNIEnv* env, NSPrintInfo* src, jobject dstPrinterJob, jobject dstPageable);
static void javaPrinterJobToNSPrintInfo(JNIEnv* env, jobject srcPrinterJob, jobject srcPageable, NSPrintInfo* dst);


#ifdef __MAC_10_9 // code for SDK 10.9 or newer
#define NS_PORTRAIT NSPaperOrientationPortrait
#define NS_LANDSCAPE NSPaperOrientationLandscape
#else // code for SDK 10.8 or older
#define NS_PORTRAIT NSPortraitOrientation
#define NS_LANDSCAPE NSLandscapeOrientation
#endif

static NSPrintInfo* createDefaultNSPrintInfo(JNIEnv* env, jstring printer)
{
    NSPrintInfo* defaultPrintInfo = [[NSPrintInfo sharedPrintInfo] copy];
    if (printer != NULL)
    {
        NSPrinter* nsPrinter = [NSPrinter printerWithName:JavaStringToNSString(env, printer)];
        if (nsPrinter != nil)
        {
            [defaultPrintInfo setPrinter:nsPrinter];
        }
    }
    [defaultPrintInfo setUpPrintOperationDefaultValues];

    // cmc 05/18/04 radr://3160443 : setUpPrintOperationDefaultValues sets the
    // page margins to 72, 72, 90, 90 - need to use [NSPrintInfo imageablePageBounds]
    // to get values from the printer.
    // NOTE: currently [NSPrintInfo imageablePageBounds] does not update itself when
    // the user selects a different printer - see radr://3657453. However, rather than
    // directly querying the PPD here, we'll let AppKit printing do the work. The AppKit
    // printing bug above is set to be fixed for Tiger.
    NSRect imageableRect = [defaultPrintInfo imageablePageBounds];
    [defaultPrintInfo setLeftMargin: imageableRect.origin.x];
    [defaultPrintInfo setBottomMargin: imageableRect.origin.y]; //top and bottom are flipped because [NSPrintInfo imageablePageBounds] returns a flipped NSRect (bottom-left to top-right).
    [defaultPrintInfo setRightMargin: [defaultPrintInfo paperSize].width-imageableRect.origin.x-imageableRect.size.width];
    [defaultPrintInfo setTopMargin: [defaultPrintInfo paperSize].height-imageableRect.origin.y-imageableRect.size.height];

    return defaultPrintInfo;
}

static void makeBestFit(NSPrintInfo* src)
{
    // This will look at the NSPrintInfo's margins. If they are out of bounds to the
    // imageable area of the page, it will set them to the largest possible size.

    NSRect imageable = [src imageablePageBounds];

    NSSize paperSize = [src paperSize];

    CGFloat fullLeftM = imageable.origin.x;
    CGFloat fullRightM = paperSize.width - (imageable.origin.x + imageable.size.width);

    // These are flipped because [NSPrintInfo imageablePageBounds] returns a flipped
    //  NSRect (bottom-left to top-right).
    CGFloat fullTopM = paperSize.height - (imageable.origin.y + imageable.size.height);
    CGFloat fullBottomM = imageable.origin.y;

    if (fullLeftM > [src leftMargin])
    {
        [src setLeftMargin:fullLeftM];
    }

    if (fullRightM > [src rightMargin])
    {
        [src setRightMargin:fullRightM];
    }

    if (fullTopM > [src topMargin])
    {
        [src setTopMargin:fullTopM];
    }

    if (fullBottomM > [src bottomMargin])
    {
        [src setBottomMargin:fullBottomM];
    }
}

// In AppKit Printing, the rectangle is always oriented. In AppKit Printing, setting
//  the rectangle will always set the orientation.
// In java printing, the rectangle is oriented if accessed from PageFormat. It is
//  not oriented when accessed from Paper.

static void nsPrintInfoToJavaPaper(JNIEnv* env, NSPrintInfo* src, jobject dst)
{
    GET_PAGEFORMAT_CLASS();
    GET_PAPER_CLASS();
    DECLARE_METHOD(jm_setSize, sjc_Paper, "setSize", "(DD)V");
    DECLARE_METHOD(jm_setImageableArea, sjc_Paper, "setImageableArea", "(DDDD)V");

    jdouble jPaperW, jPaperH;

    // NSPrintInfo paperSize is oriented. java Paper is not oriented. Take
    //  the -[NSPrintInfo orientation] into account when setting the Paper
    //  rectangle.

    NSSize paperSize = [src paperSize];
    switch ([src orientation]) {
        case NS_PORTRAIT:
            jPaperW = paperSize.width;
            jPaperH = paperSize.height;
            break;

        case NS_LANDSCAPE:
            jPaperW = paperSize.height;
            jPaperH = paperSize.width;
            break;

        default:
            jPaperW = paperSize.width;
            jPaperH = paperSize.height;
            break;
    }

    (*env)->CallVoidMethod(env, dst, jm_setSize, jPaperW, jPaperH); // AWT_THREADING Safe (known object - always actual Paper)
    CHECK_EXCEPTION();

    // Set the imageable area from the margins
    CGFloat leftM = [src leftMargin];
    CGFloat rightM = [src rightMargin];
    CGFloat topM = [src topMargin];
    CGFloat bottomM = [src bottomMargin];

    jdouble jImageX = leftM;
    jdouble jImageY = topM;
    jdouble jImageW = jPaperW - (leftM + rightM);
    jdouble jImageH = jPaperH - (topM + bottomM);

    (*env)->CallVoidMethod(env, dst, jm_setImageableArea, jImageX, jImageY, jImageW, jImageH); // AWT_THREADING Safe (known object - always actual Paper)
    CHECK_EXCEPTION();
}

static void javaPaperToNSPrintInfo(JNIEnv* env, jobject src, NSPrintInfo* dst)
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    GET_PAGEFORMAT_CLASS();
    GET_PAPER_CLASS();
    DECLARE_METHOD(jm_getWidth, sjc_Paper, "getWidth", "()D");
    DECLARE_METHOD(jm_getHeight, sjc_Paper, "getHeight", "()D");
    DECLARE_METHOD(jm_getImageableX, sjc_Paper, "getImageableX", "()D");
    DECLARE_METHOD(jm_getImageableY, sjc_Paper, "getImageableY", "()D");
    DECLARE_METHOD(jm_getImageableW, sjc_Paper, "getImageableWidth", "()D");
    DECLARE_METHOD(jm_getImageableH, sjc_Paper, "getImageableHeight", "()D");

    // java Paper is always Portrait oriented. Set NSPrintInfo with this
    //  rectangle, and it's orientation may change. If necessary, be sure to call
    //  -[NSPrintInfo setOrientation] after this call, which will then
    //  adjust the -[NSPrintInfo paperSize] as well.

    jdouble jPhysicalWidth = (*env)->CallDoubleMethod(env, src, jm_getWidth); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    jdouble jPhysicalHeight = (*env)->CallDoubleMethod(env, src, jm_getHeight); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();

    [dst setPaperSize:NSMakeSize(jPhysicalWidth, jPhysicalHeight)];

    // Set the margins from the imageable area
    jdouble jImageX = (*env)->CallDoubleMethod(env, src, jm_getImageableX); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    jdouble jImageY = (*env)->CallDoubleMethod(env, src, jm_getImageableY); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    jdouble jImageW = (*env)->CallDoubleMethod(env, src, jm_getImageableW); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    jdouble jImageH = (*env)->CallDoubleMethod(env, src, jm_getImageableH); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();

    [dst setLeftMargin:(CGFloat)jImageX];
    [dst setTopMargin:(CGFloat)jImageY];
    [dst setRightMargin:(CGFloat)(jPhysicalWidth - jImageW - jImageX)];
    [dst setBottomMargin:(CGFloat)(jPhysicalHeight - jImageH - jImageY)];
}

static void nsPrintInfoToJavaPageFormat(JNIEnv* env, NSPrintInfo* src, jobject dst)
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    GET_CPRINTERJOB_CLASS();
    GET_PAGEFORMAT_CLASS();
    GET_PAPER_CLASS();
    DECLARE_METHOD(jm_setOrientation, sjc_PageFormat, "setOrientation", "(I)V");
    DECLARE_METHOD(jm_setPaper, sjc_PageFormat, "setPaper", "(Ljava/awt/print/Paper;)V");
    DECLARE_METHOD(jm_Paper_ctor, sjc_Paper, "<init>", "()V");

    jint jOrientation;
    switch ([src orientation]) {
        case NS_PORTRAIT:
            jOrientation = java_awt_print_PageFormat_PORTRAIT;
            break;

        case NS_LANDSCAPE:
            jOrientation = java_awt_print_PageFormat_LANDSCAPE; //+++gdb Are LANDSCAPE and REVERSE_LANDSCAPE still inverted?
            break;

/*
        // AppKit printing doesn't support REVERSE_LANDSCAPE. Radar 2960295.
        case NSReverseLandscapeOrientation:
            jOrientation = java_awt_print_PageFormat.REVERSE_LANDSCAPE; //+++gdb Are LANDSCAPE and REVERSE_LANDSCAPE still inverted?
            break;
*/

        default:
            jOrientation = java_awt_print_PageFormat_PORTRAIT;
            break;
    }

    (*env)->CallVoidMethod(env, dst, jm_setOrientation, jOrientation); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();

    // Create a new Paper
    jobject paper = (*env)->NewObject(env, sjc_Paper, jm_Paper_ctor); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
    if (paper == NULL) {
        return;
    }

    nsPrintInfoToJavaPaper(env, src, paper);

    // Set the Paper in the PageFormat
    (*env)->CallVoidMethod(env, dst, jm_setPaper, paper); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();

    (*env)->DeleteLocalRef(env, paper);
}

static void javaPageFormatToNSPrintInfo(JNIEnv* env, jobject srcPrintJob, jobject srcPageFormat, NSPrintInfo* dstPrintInfo)
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    GET_CPRINTERJOB_CLASS();
    GET_PAGEFORMAT_CLASS();
    GET_PAPER_CLASS();
    DECLARE_METHOD(jm_getOrientation, sjc_PageFormat, "getOrientation", "()I");
    DECLARE_METHOD(jm_getPaper, sjc_PageFormat, "getPaper", "()Ljava/awt/print/Paper;");
    DECLARE_METHOD(jm_getPrinterName, sjc_CPrinterJob, "getPrinterName", "()Ljava/lang/String;");

    // When setting page information (orientation, size) in NSPrintInfo, set the
    //  rectangle first. This is because setting the orientation will change the
    //  rectangle to match.

    // Set up the paper. This will force Portrait since java Paper is
    //  not oriented. Then setting the NSPrintInfo orientation below
    //  will flip NSPrintInfo's info as necessary.
    jobject paper = (*env)->CallObjectMethod(env, srcPageFormat, jm_getPaper); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    javaPaperToNSPrintInfo(env, paper, dstPrintInfo);
    (*env)->DeleteLocalRef(env, paper);

    switch ((*env)->CallIntMethod(env, srcPageFormat, jm_getOrientation)) { // AWT_THREADING Safe (!appKit)
        case java_awt_print_PageFormat_PORTRAIT:
            [dstPrintInfo setOrientation:NS_PORTRAIT];
            break;

        case java_awt_print_PageFormat_LANDSCAPE:
            [dstPrintInfo setOrientation:NS_LANDSCAPE]; //+++gdb Are LANDSCAPE and REVERSE_LANDSCAPE still inverted?
            break;

        // AppKit printing doesn't support REVERSE_LANDSCAPE. Radar 2960295.
        case java_awt_print_PageFormat_REVERSE_LANDSCAPE:
            [dstPrintInfo setOrientation:NS_LANDSCAPE]; //+++gdb Are LANDSCAPE and REVERSE_LANDSCAPE still inverted?
            break;

        default:
            [dstPrintInfo setOrientation:NS_PORTRAIT];
            break;
    }
    CHECK_EXCEPTION();

    // <rdar://problem/4022422> NSPrinterInfo is not correctly set to the selected printer
    // from the Java side of CPrinterJob. Has always assumed the default printer was the one we wanted.
    if (srcPrintJob == NULL) return;
    jobject printerNameObj = (*env)->CallObjectMethod(env, srcPrintJob, jm_getPrinterName);
    CHECK_EXCEPTION();
    if (printerNameObj == NULL) return;
    NSString *printerName = JavaStringToNSString(env, printerNameObj);
    if (printerName == nil) return;
    NSPrinter *printer = [NSPrinter printerWithName:printerName];
    if (printer == nil) return;
    [dstPrintInfo setPrinter:printer];
}

static void nsPrintInfoToJavaPrinterJob(JNIEnv* env, NSPrintInfo* src, jobject dstPrinterJob, jobject dstPageable)
{
    GET_CPRINTERJOB_CLASS();
    DECLARE_METHOD(jm_setService, sjc_CPrinterJob, "setPrinterServiceFromNative", "(Ljava/lang/String;)V");
    DECLARE_METHOD(jm_setCopiesAttribute, sjc_CPrinterJob, "setCopiesAttribute", "(I)V");
    DECLARE_METHOD(jm_setCollated, sjc_CPrinterJob, "setCollated", "(Z)V");
    DECLARE_METHOD(jm_setPageRangeAttribute, sjc_CPrinterJob, "setPageRangeAttribute", "(IIZ)V");
    DECLARE_METHOD(jm_setPrintToFile, sjc_CPrinterJob, "setPrintToFile", "(Z)V");
    DECLARE_METHOD(jm_setDestinationFile, sjc_CPrinterJob, "setDestinationFile", "(Ljava/lang/String;)V");

    // get the selected printer's name, and set the appropriate PrintService on the Java side
    NSString *name = [[src printer] name];
    jstring printerName = NSStringToJavaString(env, name);
    (*env)->CallVoidMethod(env, dstPrinterJob, jm_setService, printerName);
    CHECK_EXCEPTION();

    NSMutableDictionary* printingDictionary = [src dictionary];

    if (src.jobDisposition == NSPrintSaveJob) {
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setPrintToFile, true);
        CHECK_EXCEPTION();
        NSURL *url = [printingDictionary objectForKey:NSPrintJobSavingURL];
        NSString *nsStr = [url absoluteString];
        jstring str = NSStringToJavaString(env, nsStr);
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setDestinationFile, str);
        CHECK_EXCEPTION();
    } else {
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setPrintToFile, false);
        CHECK_EXCEPTION();
    }

    NSNumber* nsCopies = [printingDictionary objectForKey:NSPrintCopies];
    if ([nsCopies respondsToSelector:@selector(integerValue)])
    {
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setCopiesAttribute, [nsCopies integerValue]); // AWT_THREADING Safe (known object)
        CHECK_EXCEPTION();
    }

    NSNumber* nsCollated = [printingDictionary objectForKey:NSPrintMustCollate];
    if ([nsCollated respondsToSelector:@selector(boolValue)])
    {
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setCollated, [nsCollated boolValue] ? JNI_TRUE : JNI_FALSE); // AWT_THREADING Safe (known object)
        CHECK_EXCEPTION();
    }

    NSNumber* nsPrintAllPages = [printingDictionary objectForKey:NSPrintAllPages];
    if ([nsPrintAllPages respondsToSelector:@selector(boolValue)])
    {
        jint jFirstPage = 0, jLastPage = java_awt_print_Pageable_UNKNOWN_NUMBER_OF_PAGES;
        jboolean isRangeSet = false;
        if (![nsPrintAllPages boolValue])
        {
            NSNumber* nsFirstPage = [printingDictionary objectForKey:NSPrintFirstPage];
            if ([nsFirstPage respondsToSelector:@selector(integerValue)])
            {
                jFirstPage = [nsFirstPage integerValue] - 1;
            }

            NSNumber* nsLastPage = [printingDictionary objectForKey:NSPrintLastPage];
            if ([nsLastPage respondsToSelector:@selector(integerValue)])
            {
                jLastPage = [nsLastPage integerValue] - 1;
            }
            isRangeSet = true;
        }
        (*env)->CallVoidMethod(env, dstPrinterJob, jm_setPageRangeAttribute,
                          jFirstPage, jLastPage, isRangeSet); // AWT_THREADING Safe (known object)
        CHECK_EXCEPTION();

    }
}

static void javaPrinterJobToNSPrintInfo(JNIEnv* env, jobject srcPrinterJob, jobject srcPageable, NSPrintInfo* dst)
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    DECLARE_CLASS(jc_Pageable, "java/awt/print/Pageable");
    DECLARE_METHOD(jm_getCopies, sjc_CPrinterJob, "getCopiesInt", "()I");
    DECLARE_METHOD(jm_isCollated, sjc_CPrinterJob, "isCollated", "()Z");
    DECLARE_METHOD(jm_getFromPage, sjc_CPrinterJob, "getFromPageAttrib", "()I");
    DECLARE_METHOD(jm_getToPage, sjc_CPrinterJob, "getToPageAttrib", "()I");
    DECLARE_METHOD(jm_getMinPage, sjc_CPrinterJob, "getMinPageAttrib", "()I");
    DECLARE_METHOD(jm_getMaxPage, sjc_CPrinterJob, "getMaxPageAttrib", "()I");
    DECLARE_METHOD(jm_getSelectAttrib, sjc_CPrinterJob, "getSelectAttrib", "()I");
    DECLARE_METHOD(jm_getNumberOfPages, jc_Pageable, "getNumberOfPages", "()I");
    DECLARE_METHOD(jm_getPageFormat, sjc_CPrinterJob, "getPageFormatFromAttributes", "()Ljava/awt/print/PageFormat;");
    DECLARE_METHOD(jm_getDestinationFile, sjc_CPrinterJob, "getDestinationFile", "()Ljava/lang/String;");

    NSMutableDictionary* printingDictionary = [dst dictionary];

    jint copies = (*env)->CallIntMethod(env, srcPrinterJob, jm_getCopies); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
    [printingDictionary setObject:[NSNumber numberWithInteger:copies] forKey:NSPrintCopies];

    jboolean collated = (*env)->CallBooleanMethod(env, srcPrinterJob, jm_isCollated); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
    [printingDictionary setObject:[NSNumber numberWithBool:collated ? YES : NO] forKey:NSPrintMustCollate];
    jint selectID = (*env)->CallIntMethod(env, srcPrinterJob, jm_getSelectAttrib);
    CHECK_EXCEPTION();
    jint fromPage = (*env)->CallIntMethod(env, srcPrinterJob, jm_getFromPage);
    CHECK_EXCEPTION();
    jint toPage = (*env)->CallIntMethod(env, srcPrinterJob, jm_getToPage);
    CHECK_EXCEPTION();
    if (selectID ==0) {
        [printingDictionary setObject:[NSNumber numberWithBool:YES] forKey:NSPrintAllPages];
    } else if (selectID == 2) {
        // In Mac 10.7,  Print ALL is deselected if PrintSelection is YES whether
        // NSPrintAllPages is YES or NO
        [printingDictionary setObject:[NSNumber numberWithBool:NO] forKey:NSPrintAllPages];
        [printingDictionary setObject:[NSNumber numberWithBool:YES] forKey:NSPrintSelectionOnly];
    } else {
        jint minPage = (*env)->CallIntMethod(env, srcPrinterJob, jm_getMinPage);
        CHECK_EXCEPTION();
        jint maxPage = (*env)->CallIntMethod(env, srcPrinterJob, jm_getMaxPage);
        CHECK_EXCEPTION();

        // for PD_SELECTION or PD_NOSELECTION, check from/to page
        // to determine which radio button to select
        if (fromPage > minPage || toPage < maxPage) {
            [printingDictionary setObject:[NSNumber numberWithBool:NO] forKey:NSPrintAllPages];
        } else {
            [printingDictionary setObject:[NSNumber numberWithBool:YES] forKey:NSPrintAllPages];
        }
    }

    // setting fromPage and toPage will not be shown in the dialog if printing All pages
    [printingDictionary setObject:[NSNumber numberWithInteger:fromPage] forKey:NSPrintFirstPage];
    [printingDictionary setObject:[NSNumber numberWithInteger:toPage] forKey:NSPrintLastPage];

    jobject page = (*env)->CallObjectMethod(env, srcPrinterJob, jm_getPageFormat);
    CHECK_EXCEPTION();
    if (page != NULL) {
        javaPageFormatToNSPrintInfo(env, NULL, page, dst);
    }

    jstring dest = (*env)->CallObjectMethod(env, srcPrinterJob, jm_getDestinationFile);
    CHECK_EXCEPTION();
    if (dest != NULL) {
       [dst setJobDisposition:NSPrintSaveJob];
       NSString *nsDestStr = JavaStringToNSString(env, dest);
       NSURL *nsURL = [NSURL fileURLWithPath:nsDestStr isDirectory:NO];
       [printingDictionary setObject:nsURL forKey:NSPrintJobSavingURL];
    } else {
       [dst setJobDisposition:NSPrintSpoolJob];
    }
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    abortDoc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPrinterJob_abortDoc
  (JNIEnv *env, jobject jthis)
{
JNI_COCOA_ENTER(env);
    // This is only called during the printLoop from the printLoop thread
    NSPrintOperation* printLoop = [NSPrintOperation currentOperation];
    NSPrintInfo* printInfo = [printLoop printInfo];
    [printInfo setJobDisposition:NSPrintCancelJob];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    getDefaultPage
 * Signature: (Ljava/awt/print/PageFormat;)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPrinterJob_getDefaultPage
  (JNIEnv *env, jobject jthis, jobject page)
{
JNI_COCOA_ENTER(env);
    NSPrintInfo* printInfo = createDefaultNSPrintInfo(env, NULL);

    nsPrintInfoToJavaPageFormat(env, printInfo, page);

    [printInfo release];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    validatePaper
 * Signature: (Ljava/awt/print/Paper;Ljava/awt/print/Paper;)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPrinterJob_validatePaper
  (JNIEnv *env, jobject jthis, jobject origpaper, jobject newpaper)
{
JNI_COCOA_ENTER(env);


    NSPrintInfo* printInfo = createDefaultNSPrintInfo(env, NULL);
    javaPaperToNSPrintInfo(env, origpaper, printInfo);
    makeBestFit(printInfo);
    nsPrintInfoToJavaPaper(env, printInfo, newpaper);
    [printInfo release];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    createNSPrintInfo
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CPrinterJob_createNSPrintInfo
  (JNIEnv *env, jobject jthis)
{
    jlong result = -1;
JNI_COCOA_ENTER(env);
    // This is used to create the NSPrintInfo for this PrinterJob. Thread
    //  safety is assured by the java side of this call.

    NSPrintInfo* printInfo = createDefaultNSPrintInfo(env, NULL);

    result = ptr_to_jlong(printInfo);

JNI_COCOA_EXIT(env);
    return result;
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    dispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPrinterJob_dispose
  (JNIEnv *env, jobject jthis, jlong nsPrintInfo)
{
JNI_COCOA_ENTER(env);
    if (nsPrintInfo != -1)
    {
        NSPrintInfo* printInfo = (NSPrintInfo*)jlong_to_ptr(nsPrintInfo);
        [printInfo release];
    }
JNI_COCOA_EXIT(env);
}


/*
 * Class:     sun_lwawt_macosx_CPrinterJob
 * Method:    printLoop
 * Signature: ()V
 */
JNIEXPORT jboolean JNICALL Java_sun_lwawt_macosx_CPrinterJob_printLoop
  (JNIEnv *env, jobject jthis, jboolean blocks, jint firstPage, jint lastPage)
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    GET_CPRINTERJOB_CLASS_RETURN(NO);
    DECLARE_METHOD_RETURN(jm_getPageFormat, sjc_CPrinterJob, "getPageFormat", "(I)Ljava/awt/print/PageFormat;", NO);
    DECLARE_METHOD_RETURN(jm_getPageFormatArea, sjc_CPrinterJob, "getPageFormatArea", "(Ljava/awt/print/PageFormat;)Ljava/awt/geom/Rectangle2D;", NO);
    DECLARE_METHOD_RETURN(jm_getPrinterName, sjc_CPrinterJob, "getPrinterName", "()Ljava/lang/String;", NO);
    DECLARE_METHOD_RETURN(jm_getPageable, sjc_CPrinterJob, "getPageable", "()Ljava/awt/print/Pageable;", NO);
    DECLARE_METHOD_RETURN(jm_getPrinterTray, sjc_CPrinterJob, "getPrinterTray", "()Ljava/lang/String;", NO);

    jboolean retVal = JNI_FALSE;

JNI_COCOA_ENTER(env);
    // Get the first page's PageFormat for setting things up (This introduces
    //  and is a facet of the same problem in Radar 2818593/2708932).
    jobject page = (*env)->CallObjectMethod(env, jthis, jm_getPageFormat, 0); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
    if (page != NULL) {
        jobject pageFormatArea = (*env)->CallObjectMethod(env, jthis, jm_getPageFormatArea, page); // AWT_THREADING Safe (!appKit)
        CHECK_EXCEPTION();

        PrinterView* printerView = [[PrinterView alloc] initWithFrame:JavaToNSRect(env, pageFormatArea) withEnv:env withPrinterJob:jthis];
        [printerView setFirstPage:firstPage lastPage:lastPage];

        GET_NSPRINTINFO_METHOD_RETURN(NO)
        NSPrintInfo* printInfo = (NSPrintInfo*)jlong_to_ptr((*env)->CallLongMethod(env, jthis, sjm_getNSPrintInfo)); // AWT_THREADING Safe (known object)
        CHECK_EXCEPTION();
        jobject printerTrayObj = (*env)->CallObjectMethod(env, jthis, jm_getPrinterTray);
        CHECK_EXCEPTION();
        if (printerTrayObj != NULL) {
            NSString *printerTray = JavaStringToNSString(env, printerTrayObj);
            if (printerTray != nil) {
                [[printInfo printSettings] setObject:printerTray forKey:@"InputSlot"];
            }
        }

        // <rdar://problem/4156975> passing jthis CPrinterJob as well, so we can extract the printer name from the current job
        javaPageFormatToNSPrintInfo(env, jthis, page, printInfo);

        // <rdar://problem/4093799> NSPrinterInfo is not correctly set to the selected printer
        // from the Java side of CPrinterJob. Had always assumed the default printer was the one we wanted.
        jobject printerNameObj = (*env)->CallObjectMethod(env, jthis, jm_getPrinterName);
        CHECK_EXCEPTION();
        if (printerNameObj != NULL) {
            NSString *printerName = JavaStringToNSString(env, printerNameObj);
            if (printerName != nil) {
                NSPrinter *printer = [NSPrinter printerWithName:printerName];
                if (printer != nil) [printInfo setPrinter:printer];
            }
        }

        // <rdar://problem/4367998> JTable.print attributes are ignored
        jobject pageable = (*env)->CallObjectMethod(env, jthis, jm_getPageable); // AWT_THREADING Safe (!appKit)
        CHECK_EXCEPTION();
        javaPrinterJobToNSPrintInfo(env, jthis, pageable, printInfo);

        PrintModel* printModel = [[PrintModel alloc] initWithPrintInfo:printInfo];

        (void)[printModel runPrintLoopWithView:printerView waitUntilDone:blocks withEnv:env];

        // Only set this if we got far enough to call runPrintLoopWithView, or we will spin CPrinterJob.print() forever!
        retVal = JNI_TRUE;

        [printModel release];
        [printerView release];

        if (page != NULL)
        {
            (*env)->DeleteLocalRef(env, page);
        }

        if (pageFormatArea != NULL)
        {
            (*env)->DeleteLocalRef(env, pageFormatArea);
        }
    }
JNI_COCOA_EXIT(env);
    return retVal;
}

/*
 * Class:     sun_lwawt_macosx_CPrinterPageDialog
 * Method:    showDialog
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_lwawt_macosx_CPrinterPageDialog_showDialog
  (JNIEnv *env, jobject jthis)
{

    DECLARE_CLASS_RETURN(jc_CPrinterPageDialog, "sun/lwawt/macosx/CPrinterPageDialog", NO);
    DECLARE_FIELD_RETURN(jm_page, jc_CPrinterPageDialog, "fPage", "Ljava/awt/print/PageFormat;", NO);

    jboolean result = JNI_FALSE;
JNI_COCOA_ENTER(env);
    GET_CPRINTERDIALOG_FIELD_RETURN(NO);
    GET_NSPRINTINFO_METHOD_RETURN(NO)
    jobject printerJob = (*env)->GetObjectField(env, jthis, sjm_printerJob);
    if (printerJob == NULL) return NO;
    NSPrintInfo* printInfo = (NSPrintInfo*)jlong_to_ptr((*env)->CallLongMethod(env, printerJob, sjm_getNSPrintInfo)); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
    if (printInfo == NULL) return result;

    jobject page = (*env)->GetObjectField(env, jthis, jm_page);
    if (page == NULL) return NO;

    // <rdar://problem/4156975> passing NULL, because only a CPrinterJob has a real printer associated with it
    javaPageFormatToNSPrintInfo(env, NULL, page, printInfo);

    PrintModel* printModel = [[PrintModel alloc] initWithPrintInfo:printInfo];
    result = [printModel runPageSetup];
    [printModel release];

    if (result)
    {
        nsPrintInfoToJavaPageFormat(env, printInfo, page);
    }

    if (printerJob != NULL)
    {
        (*env)->DeleteLocalRef(env, printerJob);
    }

    if (page != NULL)
    {
        (*env)->DeleteLocalRef(env, page);
    }

JNI_COCOA_EXIT(env);
    return result;
}

/*
 * Class:     sun_lwawt_macosx_CPrinterJobDialog
 * Method:    showDialog
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_lwawt_macosx_CPrinterJobDialog_showDialog
  (JNIEnv *env, jobject jthis)
{
    DECLARE_CLASS_RETURN(jc_CPrinterJobDialog, "sun/lwawt/macosx/CPrinterJobDialog", NO);
    DECLARE_FIELD_RETURN(jm_pageable, jc_CPrinterJobDialog, "fPageable", "Ljava/awt/print/Pageable;", NO);

    jboolean result = JNI_FALSE;
JNI_COCOA_ENTER(env);
    GET_CPRINTERDIALOG_FIELD_RETURN(NO);
    jobject printerJob = (*env)->GetObjectField(env, jthis, sjm_printerJob);
    if (printerJob == NULL) return NO;
    GET_NSPRINTINFO_METHOD_RETURN(NO)
    NSPrintInfo* printInfo = (NSPrintInfo*)jlong_to_ptr((*env)->CallLongMethod(env, printerJob, sjm_getNSPrintInfo)); // AWT_THREADING Safe (known object)

    jobject pageable = (*env)->GetObjectField(env, jthis, jm_pageable);
    if (pageable == NULL) return NO;

    javaPrinterJobToNSPrintInfo(env, printerJob, pageable, printInfo);

    PrintModel* printModel = [[PrintModel alloc] initWithPrintInfo:printInfo];
    result = [printModel runJobSetup];
    [printModel release];

    if (result)
    {
        nsPrintInfoToJavaPrinterJob(env, printInfo, printerJob, pageable);
    }

    if (printerJob != NULL)
    {
        (*env)->DeleteLocalRef(env, printerJob);
    }

    if (pageable != NULL)
    {
        (*env)->DeleteLocalRef(env, pageable);
    }

JNI_COCOA_EXIT(env);
    return result;
}
