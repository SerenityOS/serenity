/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import "CDataTransferer.h"
#include "sun_lwawt_macosx_CDataTransferer.h"

#import "JNIUtilities.h"

// ***** NOTE ***** This dictionary corresponds to the static array predefinedClipboardNames
// in CDataTransferer.java.
NSMutableDictionary *sStandardMappings = nil;

NSMutableDictionary *getMappingTable() {
    if (sStandardMappings == nil) {
        sStandardMappings = [[NSMutableDictionary alloc] init];
        [sStandardMappings setObject:NSStringPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_STRING]];
        [sStandardMappings setObject:NSFilenamesPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_FILE]];
        [sStandardMappings setObject:NSTIFFPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_TIFF]];
        [sStandardMappings setObject:NSRTFPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_RICH_TEXT]];
        [sStandardMappings setObject:NSHTMLPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_HTML]];
        [sStandardMappings setObject:NSPDFPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_PDF]];
        [sStandardMappings setObject:NSURLPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_URL]];
        [sStandardMappings setObject:NSPasteboardTypePNG
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_PNG]];
        [sStandardMappings setObject:(NSString*)kUTTypeJPEG
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_JPEG]];
        [sStandardMappings setObject:NSPICTPboardType
                              forKey:[NSNumber numberWithLong:sun_lwawt_macosx_CDataTransferer_CF_XPICT]];
    }
    return sStandardMappings;
}

/*
 * Convert from a standard NSPasteboard data type to an index in our mapping table.
 */
jlong indexForFormat(NSString *format) {
    jlong returnValue = -1;

    NSMutableDictionary *mappingTable = getMappingTable();
    NSArray *matchingKeys = [mappingTable allKeysForObject:format];

    // There should only be one matching key here...
    if ([matchingKeys count] > 0) {
        NSNumber *formatID = (NSNumber *)[matchingKeys objectAtIndex:0];
        returnValue = [formatID longValue];
    }

    // If we don't recognize the format, but it's a Java "custom" format register it
    if (returnValue == -1 && ([format hasPrefix:@"JAVA_DATAFLAVOR:"]) ) {
        returnValue = registerFormatWithPasteboard(format);
    }

    return returnValue;
}

/*
 * Inverse of above -- given a long int index, get the matching data format NSString.
 */
NSString *formatForIndex(jlong inFormatCode) {
    return [getMappingTable() objectForKey:[NSNumber numberWithLong:inFormatCode]];
}

jlong registerFormatWithPasteboard(NSString *format) {
    NSMutableDictionary *mappingTable = getMappingTable();
    NSUInteger nextID = [mappingTable count] + 1;
    [mappingTable setObject:format forKey:[NSNumber numberWithLong:nextID]];
    return nextID;
}


/*
 * Class:     sun_lwawt_macosx_CDataTransferer
 * Method:    registerFormatWithPasteboard
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CDataTransferer_registerFormatWithPasteboard
(JNIEnv *env, jobject jthis, jstring newformat)
{
    jlong returnValue = -1;
JNI_COCOA_ENTER(env);
    returnValue = registerFormatWithPasteboard(JavaStringToNSString(env, newformat));
JNI_COCOA_EXIT(env);
    return returnValue;
}

/*
 * Class:     sun_lwawt_macosx_CDataTransferer
 * Method:    formatForIndex
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_lwawt_macosx_CDataTransferer_formatForIndex
  (JNIEnv *env, jobject jthis, jlong index)
{
    jstring returnValue = NULL;
JNI_COCOA_ENTER(env);
    returnValue = NSStringToJavaString(env, formatForIndex(index));
JNI_COCOA_EXIT(env);
    return returnValue;
}

static jobjectArray CreateJavaFilenameArray(JNIEnv *env, NSArray *filenameArray)
{
    NSUInteger filenameCount = [filenameArray count];
    if (filenameCount == 0) return nil;

    // Get the java.lang.String class object:
    jclass stringClazz = (*env)->FindClass(env, "java/lang/String");
    CHECK_NULL_RETURN(stringClazz, nil);
    jobject jfilenameArray = (*env)->NewObjectArray(env, filenameCount, stringClazz, NULL);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return nil;
    }
    if (!jfilenameArray) {
        NSLog(@"CDataTransferer_CreateJavaFilenameArray: couldn't create jfilenameArray.");
        return nil;
    }
    (*env)->DeleteLocalRef(env, stringClazz);

    // Iterate through all the filenames:
    NSUInteger i;
    for (i = 0; i < filenameCount; i++) {
        NSMutableString *stringVal = [[NSMutableString alloc] initWithString:[filenameArray objectAtIndex:i]];
        CFStringNormalize((CFMutableStringRef)stringVal, kCFStringNormalizationFormC);
        const char* stringBytes = [stringVal UTF8String];

        // Create a Java String:
        jstring string = (*env)->NewStringUTF(env, stringBytes);
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            continue;
        }
        if (!string) {
            NSLog(@"CDataTransferer_CreateJavaFilenameArray: couldn't create jstring[%lu] for [%@].", (unsigned long) i, stringVal);
            continue;
        }

        // Set the Java array element with our String:
        (*env)->SetObjectArrayElement(env, jfilenameArray, i, string);
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            continue;
        }

        // Release local String reference:
        (*env)->DeleteLocalRef(env, string);
    }

    return jfilenameArray;
}

/*
 * Class:     sun_lwawt_macosx_CDataTransferer
 * Method:    draqQueryFile
 * Signature: ([B)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_lwawt_macosx_CDataTransferer_nativeDragQueryFile
(JNIEnv *env, jclass clazz, jbyteArray jbytearray)
{
    // Decodes a byte array into a set of String filenames.
    // bytes here is an XML property list containing all of the filenames in the drag.
    // Parse the XML list into strings and return an array of Java strings matching all of the
    // files in the list.

    jobjectArray jreturnArray = NULL;

JNI_COCOA_ENTER(env);
    // Get byte array elements:
    jboolean isCopy;
    jbyte* jbytes = (*env)->GetByteArrayElements(env, jbytearray, &isCopy);
    if (jbytes == NULL) {
        return NULL;
    }

    // Wrap jbytes in an NSData object:
    jsize jbytesLength = (*env)->GetArrayLength(env, jbytearray);
    NSData *xmlData = [NSData dataWithBytesNoCopy:jbytes length:jbytesLength freeWhenDone:NO];

    // Create a property list from the Java data:
    NSString *errString = nil;
    NSPropertyListFormat plistFormat = 0;
    id plist = [NSPropertyListSerialization propertyListFromData:xmlData mutabilityOption:NSPropertyListImmutable
        format:&plistFormat errorDescription:&errString];

    // The property list must be an array of strings:
    if (plist == nil || [plist isKindOfClass:[NSArray class]] == FALSE) {
        NSLog(@"CDataTransferer_dragQueryFile: plist not a valid NSArray (error %@):\n%@", errString, plist);
        (*env)->ReleaseByteArrayElements(env, jbytearray, jbytes, JNI_ABORT);
        return NULL;
    }

    // Transfer all string items from the plistArray to filenameArray. This wouldn't be necessary
    // if we could trust the array to contain all valid elements but this way we'll be sure.
    NSArray *plistArray = (NSArray *)plist;
    NSUInteger plistItemCount = [plistArray count];
    NSMutableArray *filenameArray = [[NSMutableArray alloc] initWithCapacity:plistItemCount];

    NSUInteger i;
    for (i = 0; i < plistItemCount; i++) {
        // Filenames must be strings:
        id idVal = [plistArray objectAtIndex:i];
        if ([idVal isKindOfClass:[NSString class]] == FALSE) {
            NSLog(@"CDataTransferer_dragQueryFile: plist[%lu] not an NSString:\n%@", (unsigned long) i, idVal);
            continue;
        }

        [filenameArray addObject:idVal];
    }

    // Convert our array of filenames into a Java array of String filenames:
    jreturnArray = CreateJavaFilenameArray(env, filenameArray);

    [filenameArray release];

    // We're done with the jbytes (backing the plist/plistArray):
    (*env)->ReleaseByteArrayElements(env, jbytearray, jbytes, JNI_ABORT);
JNI_COCOA_EXIT(env);
    return jreturnArray;
}
