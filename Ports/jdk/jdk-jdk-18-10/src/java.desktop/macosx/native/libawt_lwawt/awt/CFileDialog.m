/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#import <sys/stat.h>
#import <Cocoa/Cocoa.h>

#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import "CFileDialog.h"

#import "java_awt_FileDialog.h"
#import "sun_lwawt_macosx_CFileDialog.h"

@implementation CFileDialog

- (id)initWithFilter:(jboolean)inHasFilter
          fileDialog:(jobject)inDialog
               title:(NSString *)inTitle
           directory:(NSString *)inPath
                file:(NSString *)inFile
                mode:(jint)inMode
        multipleMode:(BOOL)inMultipleMode
      shouldNavigate:(BOOL)inNavigateApps
canChooseDirectories:(BOOL)inChooseDirectories
             withEnv:(JNIEnv*)env;
{
  if (self = [super init]) {
        fHasFileFilter = inHasFilter;
        fFileDialog = (*env)->NewGlobalRef(env, inDialog);
        fDirectory = inPath;
        [fDirectory retain];
        fFile = inFile;
        [fFile retain];
        fTitle = inTitle;
        [fTitle retain];
        fMode = inMode;
        fMultipleMode = inMultipleMode;
        fNavigateApps = inNavigateApps;
        fChooseDirectories = inChooseDirectories;
        fPanelResult = NSCancelButton;
    }

    return self;
}

-(void) disposer {
    if (fFileDialog != NULL) {
        JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
        (*env)->DeleteGlobalRef(env, fFileDialog);
        fFileDialog = NULL;
    }
}

-(void) dealloc {
    [fDirectory release];
    fDirectory = nil;

    [fFile release];
    fFile = nil;

    [fTitle release];
    fTitle = nil;

    [fURLs release];
    fURLs = nil;

    [super dealloc];
}

- (void)safeSaveOrLoad {
    NSSavePanel *thePanel = nil;

    /*
     * 8013553: turns off extension hiding for the native file dialog.
     * This way is used because setExtensionHidden(NO) doesn't work
     * as expected.
     */
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setBool:NO forKey:@"NSNavLastUserSetHideExtensionButtonState"];

    if (fMode == java_awt_FileDialog_SAVE) {
        thePanel = [NSSavePanel savePanel];
        [thePanel setAllowsOtherFileTypes:YES];
    } else {
        thePanel = [NSOpenPanel openPanel];
    }

    if (thePanel != nil) {
        [thePanel setTitle:fTitle];

        if (fNavigateApps) {
            [thePanel setTreatsFilePackagesAsDirectories:YES];
        }

        if (fMode == java_awt_FileDialog_LOAD) {
            NSOpenPanel *openPanel = (NSOpenPanel *)thePanel;
            [openPanel setAllowsMultipleSelection:fMultipleMode];
            [openPanel setCanChooseFiles:!fChooseDirectories];
            [openPanel setCanChooseDirectories:fChooseDirectories];
            [openPanel setCanCreateDirectories:YES];
        }

        [thePanel setDelegate:self];
        fPanelResult = [thePanel runModalForDirectory:fDirectory file:fFile];
        [thePanel setDelegate:nil];

        if ([self userClickedOK]) {
            if (fMode == java_awt_FileDialog_LOAD) {
                NSOpenPanel *openPanel = (NSOpenPanel *)thePanel;
                fURLs = [openPanel URLs];
            } else {
                fURLs = [NSArray arrayWithObject:[thePanel URL]];
            }
            [fURLs retain];
        }
    }

    [self disposer];
}

- (BOOL) askFilenameFilter:(NSString *)filename {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jstring jString = NormalizedPathJavaStringFromNSString(env, filename);

    DECLARE_CLASS_RETURN(jc_CFileDialog, "sun/lwawt/macosx/CFileDialog", NO);
    DECLARE_METHOD_RETURN(jm_queryFF, jc_CFileDialog, "queryFilenameFilter", "(Ljava/lang/String;)Z", NO);
    BOOL returnValue = (*env)->CallBooleanMethod(env, fFileDialog, jm_queryFF, jString);
    CHECK_EXCEPTION();
    (*env)->DeleteLocalRef(env, jString);

    return returnValue;
}

- (BOOL)panel:(id)sender shouldEnableURL:(NSURL *)url {
    if (!fHasFileFilter) return YES; // no filter, no problem!

    // check if it's not a normal file
    NSNumber *isFile = nil;
    if ([url getResourceValue:&isFile forKey:NSURLIsRegularFileKey error:nil]) {
        if (![isFile boolValue]) return YES; // always show directories and non-file entities (browsing servers/mounts, etc)
    }

    // if in directory-browsing mode, don't offer files
    if ((fMode != java_awt_FileDialog_LOAD) && (fMode != java_awt_FileDialog_SAVE)) {
        return NO;
    }

    // ask the file filter up in Java
    NSString* filePath = (NSString*)CFURLCopyFileSystemPath((CFURLRef)url, kCFURLPOSIXPathStyle);
    BOOL shouldEnableFile = [self askFilenameFilter:filePath];
    [filePath release];
    return shouldEnableFile;
}

- (BOOL) userClickedOK {
    return fPanelResult == NSOKButton;
}

- (NSArray *)URLs {
    return [[fURLs retain] autorelease];
}
@end

/*
 * Class:     sun_lwawt_macosx_CFileDialog
 * Method:    nativeRunFileDialog
 * Signature: (Ljava/lang/String;ILjava/io/FilenameFilter;
 *             Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_lwawt_macosx_CFileDialog_nativeRunFileDialog
(JNIEnv *env, jobject peer, jstring title, jint mode, jboolean multipleMode,
 jboolean navigateApps, jboolean chooseDirectories, jboolean hasFilter,
 jstring directory, jstring file)
{
    jobjectArray returnValue = NULL;

JNI_COCOA_ENTER(env);
    NSString *dialogTitle = JavaStringToNSString(env, title);
    if ([dialogTitle length] == 0) {
        dialogTitle = @" ";
    }

    CFileDialog *dialogDelegate = [[CFileDialog alloc] initWithFilter:hasFilter
                                                           fileDialog:peer
                                                                title:dialogTitle
                                                            directory:JavaStringToNSString(env, directory)
                                                                 file:JavaStringToNSString(env, file)
                                                                 mode:mode
                                                         multipleMode:multipleMode
                                                       shouldNavigate:navigateApps
                                                 canChooseDirectories:chooseDirectories
                                                              withEnv:env];

    [ThreadUtilities performOnMainThread:@selector(safeSaveOrLoad)
                                 on:dialogDelegate
                         withObject:nil
                      waitUntilDone:YES];

    if ([dialogDelegate userClickedOK]) {
        NSArray *urls = [dialogDelegate URLs];
        jsize count = [urls count];

        DECLARE_CLASS_RETURN(jc_String, "java/lang/String", NULL);
        returnValue = (*env)->NewObjectArray(env, count, jc_String, NULL);

        [urls enumerateObjectsUsingBlock:^(id url, NSUInteger index, BOOL *stop) {
            jstring filename = NormalizedPathJavaStringFromNSString(env, [url path]);
            (*env)->SetObjectArrayElement(env, returnValue, index, filename);
            (*env)->DeleteLocalRef(env, filename);
        }];
    }

    [dialogDelegate release];
JNI_COCOA_EXIT(env);
    return returnValue;
}
