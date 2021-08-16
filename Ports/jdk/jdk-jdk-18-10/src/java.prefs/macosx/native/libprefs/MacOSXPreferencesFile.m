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

/*
   Hierarchical storage layout:

   <dict>
     <key>/</key>
     <dict>
       <key>foo</key>
       <string>/foo's value</string>
       <key>foo/</key>
       <dict>
         <key>bar</key>
         <string>/foo/bar's value</string>
       </dict>
     </dict>
   </dict>

   Java pref nodes are stored in several different files. Pref nodes
   with at least three components in the node name (e.g. /com/MyCompany/MyApp/)
   are stored in a CF prefs file with the first three components as the name.
   This way, all preferences for MyApp end up in com.MyCompany.MyApp.plist .
   Pref nodes with shorter names are stored in com.apple.java.util.prefs.plist

   The filesystem is assumed to be case-insensitive (like HFS+).
   Java pref node names are case-sensitive. If two pref node names differ
   only in case, they may end up in the same pref file. This is ok
   because the CF keys identifying the node span the entire absolute path
   to the node and are case-sensitive.

   Java node names may contain '.' . When mapping to the CF file name,
   these dots are left as-is, even though '/' is mapped to '.' .
   This is ok because the CF key contains the correct node name.
*/



#include <CoreFoundation/CoreFoundation.h>

#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "java_util_prefs_MacOSXPreferencesFile.h"

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad


// Throw an OutOfMemoryError with the given message.
static void throwOutOfMemoryError(JNIEnv *env, const char *msg)
{
    static jclass exceptionClass = NULL;
    jclass c;

    (*env)->ExceptionClear(env);  // If an exception is pending, clear it before
                                  // calling FindClass() and/or ThrowNew().
    if (exceptionClass) {
        c = exceptionClass;
    } else {
        c = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if ((*env)->ExceptionOccurred(env)) return;
        exceptionClass = (*env)->NewGlobalRef(env, c);
    }

    (*env)->ThrowNew(env, c, msg);
}


// throwIfNull macro
// If var is NULL, throw an OutOfMemoryError and goto badvar.
// var must be a variable. env must be the current JNIEnv.
// fixme throw BackingStoreExceptions sometimes?
#define throwIfNull(var, msg) \
    do { \
        if (var == NULL) { \
            throwOutOfMemoryError(env, msg); \
            goto bad##var; \
        } \
    } while (0)


// Converts CFNumber, CFBoolean, CFString to CFString
// returns NULL if value is of some other type
// throws and returns NULL on memory error
// result must be released (even if value was already a CFStringRef)
// value must not be null
static CFStringRef copyToCFString(JNIEnv *env, CFTypeRef value)
{
    CFStringRef result;
    CFTypeID type;

    type = CFGetTypeID(value);

    if (type == CFStringGetTypeID()) {
        result = (CFStringRef)CFRetain(value);
    }
    else if (type == CFBooleanGetTypeID()) {
        // Java Preferences API expects "true" and "false" for boolean values.
        result = CFStringCreateCopy(NULL, (value == kCFBooleanTrue) ? CFSTR("true") : CFSTR("false"));
        throwIfNull(result, "copyToCFString failed");
    }
    else if (type == CFNumberGetTypeID()) {
        CFNumberRef number = (CFNumberRef) value;
        if (CFNumberIsFloatType(number)) {
            double d;
            CFNumberGetValue(number, kCFNumberDoubleType, &d);
            result = CFStringCreateWithFormat(NULL, NULL, CFSTR("%g"), d);
            throwIfNull(result, "copyToCFString failed");
        }
        else {
            long l;
            CFNumberGetValue(number, kCFNumberLongType, &l);
            result = CFStringCreateWithFormat(NULL, NULL, CFSTR("%ld"), l);
            throwIfNull(result, "copyToCFString failed");
        }
    }
    else {
        // unknown type - return NULL
        result = NULL;
    }

 badresult:
    return result;
}


// Create a Java string from the given CF string.
// returns NULL if cfString is NULL
// throws and returns NULL on memory error
static jstring toJavaString(JNIEnv *env, CFStringRef cfString)
{
    if (cfString == NULL) {
        return NULL;
    } else {
        jstring javaString = NULL;

        CFIndex length = CFStringGetLength(cfString);
        const UniChar *constchars = CFStringGetCharactersPtr(cfString);
        if (constchars) {
            javaString = (*env)->NewString(env, constchars, length);
        } else {
            UniChar *chars = malloc(length * sizeof(UniChar));
            throwIfNull(chars, "toJavaString failed");
            CFStringGetCharacters(cfString, CFRangeMake(0, length), chars);
            javaString = (*env)->NewString(env, chars, length);
            free(chars);
        }
    badchars:
        return javaString;
    }
}



// Create a CF string from the given Java string.
// returns NULL if javaString is NULL
// throws and returns NULL on memory error
static CFStringRef toCF(JNIEnv *env, jstring javaString)
{
    if (javaString == NULL) {
        return NULL;
    } else {
        CFStringRef result = NULL;
        jsize length = (*env)->GetStringLength(env, javaString);
        const jchar *chars = (*env)->GetStringChars(env, javaString, NULL);
        throwIfNull(chars, "toCF failed");
        result =
            CFStringCreateWithCharacters(NULL, (const UniChar *)chars, length);
        (*env)->ReleaseStringChars(env, javaString, chars);
        throwIfNull(result, "toCF failed");
    badchars:
    badresult:
        return result;
    }
}


// Create an empty Java string array of the given size.
// Throws and returns NULL on error.
static jarray createJavaStringArray(JNIEnv *env, CFIndex count)
{
    static jclass stringClass = NULL;
    jclass c;

    if (stringClass) {
        c = stringClass;
    } else {
        c = (*env)->FindClass(env, "java/lang/String");
        if ((*env)->ExceptionOccurred(env)) return NULL;
        stringClass = (*env)->NewGlobalRef(env, c);
    }

    return (*env)->NewObjectArray(env, count, c, NULL); // AWT_THREADING Safe (known object)
}


// Java accessors for CF constants.
JNIEXPORT jlong JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_currentUser(JNIEnv *env,
                                                       jobject klass)
{
    return ptr_to_jlong(kCFPreferencesCurrentUser);
}

JNIEXPORT jlong JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_anyUser(JNIEnv *env, jobject klass)
{
    return ptr_to_jlong(kCFPreferencesAnyUser);
}

JNIEXPORT jlong JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_currentHost(JNIEnv *env,
                                                       jobject klass)
{
    return ptr_to_jlong(kCFPreferencesCurrentHost);
}

JNIEXPORT jlong JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_anyHost(JNIEnv *env, jobject klass)
{
    return ptr_to_jlong(kCFPreferencesAnyHost);
}


// Create an empty node.
// Does not store the node in any prefs file.
// returns NULL on memory error
static CFMutableDictionaryRef createEmptyNode(void)
{
    return CFDictionaryCreateMutable(NULL, 0,
                                     &kCFTypeDictionaryKeyCallBacks,
                                     &kCFTypeDictionaryValueCallBacks);
}


// Create a string that consists of path minus its last component.
// path must end with '/'
// The result will end in '/' (unless path itself is '/')
static CFStringRef copyParentOf(CFStringRef path)
{
    CFRange searchRange;
    CFRange slashRange;
    CFRange parentRange;
    Boolean found;

    searchRange = CFRangeMake(0, CFStringGetLength(path) - 1);
    found = CFStringFindWithOptions(path, CFSTR("/"), searchRange,
                                    kCFCompareBackwards, &slashRange);
    if (!found) return CFSTR("");
    parentRange = CFRangeMake(0, slashRange.location + 1); // include '/'
    return CFStringCreateWithSubstring(NULL, path, parentRange);
}


// Create a string that consists of path's last component.
// path must end with '/'
// The result will end in '/'.
// The result will not start with '/' (unless path itself is '/')
static CFStringRef copyChildOf(CFStringRef path)
{
    CFRange searchRange;
    CFRange slashRange;
    CFRange childRange;
    Boolean found;
    CFIndex length = CFStringGetLength(path);

    searchRange = CFRangeMake(0, length - 1);
    found = CFStringFindWithOptions(path, CFSTR("/"), searchRange,
                                    kCFCompareBackwards, &slashRange);
    if (!found) return CFSTR("");
    childRange = CFRangeMake(slashRange.location + 1,
                             length - slashRange.location - 1); // skip '/'
    return CFStringCreateWithSubstring(NULL, path, childRange);
}


// Return the first three components of path, with leading and trailing '/'.
// If path does not have three components, return NULL.
// path must begin and end in '/'
static CFStringRef copyFirstThreeComponentsOf(CFStringRef path)
{
    CFRange searchRange;
    CFRange slashRange;
    CFRange prefixRange;
    CFStringRef prefix;
    Boolean found;
    CFIndex length = CFStringGetLength(path);

    searchRange = CFRangeMake(1, length - 1);  // skip leading '/'
    found = CFStringFindWithOptions(path, CFSTR("/"), searchRange, 0,
                                    &slashRange);
    if (!found) return NULL;  // no second slash!

    searchRange = CFRangeMake(slashRange.location + 1,
                              length - slashRange.location - 1);
    found = CFStringFindWithOptions(path, CFSTR("/"), searchRange, 0,
                                    &slashRange);
    if (!found) return NULL;  // no third slash!

    searchRange = CFRangeMake(slashRange.location + 1,
                              length - slashRange.location - 1);
    found = CFStringFindWithOptions(path, CFSTR("/"), searchRange, 0,
                                    &slashRange);
    if (!found) return NULL;  // no fourth slash!

    prefixRange = CFRangeMake(0, slashRange.location + 1); // keep last '/'
    prefix = CFStringCreateWithSubstring(NULL, path, prefixRange);

    return prefix;
}


// Copy the CFPreferences key and value at the base of path's tree.
// path must end in '/'
// topKey or topValue may be NULL
// Returns NULL on error or if there is no tree for path in this file.
static void copyTreeForPath(CFStringRef path, CFStringRef name,
                            CFStringRef user, CFStringRef host,
                            CFStringRef *topKey, CFDictionaryRef *topValue)
{
    CFStringRef key;
    CFPropertyListRef value;

    if (topKey) *topKey = NULL;
    if (topValue) *topValue = NULL;

    if (CFEqual(name, CFSTR("com.apple.java.util.prefs"))) {
        // Top-level file. Only key "/" is an acceptable root.
        key = (CFStringRef) CFRetain(CFSTR("/"));
    } else {
        // Second-level file. Key must be the first three components of path.
        key = copyFirstThreeComponentsOf(path);
        if (!key) return;
    }

    value = CFPreferencesCopyValue(key, name, user, host);
    if (value) {
        if (CFGetTypeID(value) == CFDictionaryGetTypeID()) {
            // (key, value) is acceptable
            if (topKey) *topKey = (CFStringRef)CFRetain(key);
            if (topValue) *topValue = (CFDictionaryRef)CFRetain(value);
        }
        CFRelease(value);
    }
    CFRelease(key);
}


// Find the node for path in the given tree.
// Returns NULL on error or if path doesn't have a node in this tree.
// path must end in '/'
static CFDictionaryRef copyNodeInTree(CFStringRef path, CFStringRef topKey,
                                      CFDictionaryRef topValue)
{
    CFMutableStringRef p;
    CFDictionaryRef result = NULL;

    p = CFStringCreateMutableCopy(NULL, 0, path);
    if (!p) return NULL;
    CFStringDelete(p, CFRangeMake(0, CFStringGetLength(topKey)));
    result = topValue;

    while (CFStringGetLength(p) > 0) {
        CFDictionaryRef child;
        CFStringRef part = NULL;
        CFRange slashRange = CFStringFind(p, CFSTR("/"), 0);
        // guaranteed to succeed because path must end in '/'
        CFRange partRange = CFRangeMake(0, slashRange.location + 1);
        part = CFStringCreateWithSubstring(NULL, p, partRange);
        if (!part) { result = NULL; break; }
        CFStringDelete(p, partRange);

        child = CFDictionaryGetValue(result, part);
        CFRelease(part);
        if (child  &&  CFGetTypeID(child) == CFDictionaryGetTypeID()) {
            // continue search
            result = child;
        } else {
            // didn't find target node
            result = NULL;
            break;
        }
    }

    CFRelease(p);
    if (result) return (CFDictionaryRef)CFRetain(result);
    else return NULL;
}


// Return a retained copy of the node at path from the given file.
// path must end in '/'
// returns NULL if node doesn't exist.
// returns NULL if the value for key "path" isn't a valid node.
static CFDictionaryRef copyNodeIfPresent(CFStringRef path, CFStringRef name,
                                         CFStringRef user, CFStringRef host)
{
    CFStringRef topKey;
    CFDictionaryRef topValue;
    CFDictionaryRef result;

    copyTreeForPath(path, name, user, host, &topKey, &topValue);
    if (!topKey) return NULL;

    result = copyNodeInTree(path, topKey, topValue);

    CFRelease(topKey);
    if (topValue) CFRelease(topValue);
    return result;
}


// Create a new tree that would store path in the given file.
// Only the root of the tree is created, not all of the links leading to path.
// returns NULL on error
static void createTreeForPath(CFStringRef path, CFStringRef name,
                              CFStringRef user, CFStringRef host,
                              CFStringRef *outTopKey,
                              CFMutableDictionaryRef *outTopValue)
{
    *outTopKey = NULL;
    *outTopValue = NULL;

    // if name is "com.apple.java.util.prefs" then create tree "/"
    // else create tree "/foo/bar/baz/"
    // "com.apple.java.util.prefs.plist" is also in MacOSXPreferences.java
    if (CFEqual(name, CFSTR("com.apple.java.util.prefs"))) {
        *outTopKey = CFSTR("/");
        *outTopValue = createEmptyNode();
    } else {
        CFStringRef prefix = copyFirstThreeComponentsOf(path);
        if (prefix) {
            *outTopKey = prefix;
            *outTopValue = createEmptyNode();
        }
    }
}


// Return a mutable copy of the tree containing path and the dict for
//   path itself. *outTopKey and *outTopValue can be used to write the
//   modified tree back to the prefs file.
// *outTopKey and *outTopValue must be released iff the actual return
//   value is not NULL.
static CFMutableDictionaryRef
copyMutableNode(CFStringRef path, CFStringRef name,
                CFStringRef user, CFStringRef host,
                CFStringRef *outTopKey,
                CFMutableDictionaryRef *outTopValue)
{
    CFStringRef topKey = NULL;
    CFDictionaryRef oldTopValue = NULL;
    CFMutableDictionaryRef topValue;
    CFMutableDictionaryRef result = NULL;
    CFMutableStringRef p;

    if (outTopKey) *outTopKey = NULL;
    if (outTopValue) *outTopValue = NULL;

    copyTreeForPath(path, name, user, host, &topKey, &oldTopValue);
    if (!topKey) {
        createTreeForPath(path, name, user, host, &topKey, &topValue);
    } else {
        topValue = (CFMutableDictionaryRef)
            CFPropertyListCreateDeepCopy(NULL, (CFPropertyListRef)oldTopValue,
                                         kCFPropertyListMutableContainers);
    }
    if (!topValue) goto badtopValue;

    p = CFStringCreateMutableCopy(NULL, 0, path);
    if (!p) goto badp;
    CFStringDelete(p, CFRangeMake(0, CFStringGetLength(topKey)));
    result = topValue;

    while (CFStringGetLength(p) > 0) {
        CFMutableDictionaryRef child;
        CFStringRef part = NULL;
        CFRange slashRange = CFStringFind(p, CFSTR("/"), 0);
        // guaranteed to succeed because path must end in '/'
        CFRange partRange = CFRangeMake(0, slashRange.location + 1);
        part = CFStringCreateWithSubstring(NULL, p, partRange);
        if (!part) { result = NULL; break; }
        CFStringDelete(p, partRange);

        child = (CFMutableDictionaryRef)CFDictionaryGetValue(result, part);
        if (child  &&  CFGetTypeID(child) == CFDictionaryGetTypeID()) {
            // continue search
            result = child;
        } else {
            // didn't find target node - add it and continue
            child = createEmptyNode();
            if (!child) { CFRelease(part); result = NULL; break; }
            CFDictionaryAddValue(result, part, child);
            result = child;
        }
        CFRelease(part);
    }

    if (result) {
        *outTopKey = (CFStringRef)CFRetain(topKey);
        *outTopValue = (CFMutableDictionaryRef)CFRetain(topValue);
        CFRetain(result);
    }

    CFRelease(p);
 badp:
    CFRelease(topValue);
 badtopValue:
    if (topKey) CFRelease(topKey);
    if (oldTopValue) CFRelease(oldTopValue);
    return result;
}


JNIEXPORT jboolean JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_addNode
(JNIEnv *env, jobject klass, jobject jpath,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFDictionaryRef node = NULL;
    jboolean neededNewNode = false;

    if (!path  ||  !name) goto badparams;

    node = copyNodeIfPresent(path, name, user, host);

    if (node) {
        neededNewNode = false;
        CFRelease(node);
    } else {
        CFStringRef topKey = NULL;
        CFMutableDictionaryRef topValue = NULL;

        neededNewNode = true;

        // copyMutableNode creates the node if necessary
        node = copyMutableNode(path, name, user, host, &topKey, &topValue);
        throwIfNull(node, "copyMutableNode failed");

        CFPreferencesSetValue(topKey, topValue, name, user, host);

        CFRelease(node);
        if (topKey) CFRelease(topKey);
        if (topValue) CFRelease(topValue);
    }

 badnode:
 badparams:
    if (path) CFRelease(path);
    if (name) CFRelease(name);

    return neededNewNode;
}


JNIEXPORT void JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_removeNode
(JNIEnv *env, jobject klass, jobject jpath,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFStringRef parentName;
    CFStringRef childName;
    CFDictionaryRef constParent;

    if (!path  ||  !name) goto badparams;

    parentName = copyParentOf(path);
    throwIfNull(parentName, "copyParentOf failed");
    childName  = copyChildOf(path);
    throwIfNull(childName, "copyChildOf failed");

    // root node is not allowed to be removed, so parentName is never empty

    constParent = copyNodeIfPresent(parentName, name, user, host);
    if (constParent  &&  CFDictionaryContainsKey(constParent, childName)) {
        CFStringRef topKey;
        CFMutableDictionaryRef topValue;
        CFMutableDictionaryRef parent;

        parent = copyMutableNode(parentName, name, user, host,
                                 &topKey, &topValue);
        throwIfNull(parent, "copyMutableNode failed");

        CFDictionaryRemoveValue(parent, childName);
        CFPreferencesSetValue(topKey, topValue, name, user, host);

        CFRelease(parent);
        if (topKey) CFRelease(topKey);
        if (topValue) CFRelease(topValue);
    } else {
        // might be trying to remove the root itself in a non-root file
        CFStringRef topKey;
        CFDictionaryRef topValue;
        copyTreeForPath(path, name, user, host, &topKey, &topValue);
        if (topKey) {
            if (CFEqual(topKey, path)) {
                CFPreferencesSetValue(topKey, NULL, name, user, host);
            }

            if (topKey) CFRelease(topKey);
            if (topValue) CFRelease(topValue);
        }
    }


 badparent:
    if (constParent) CFRelease(constParent);
    CFRelease(childName);
 badchildName:
    CFRelease(parentName);
 badparentName:
 badparams:
    if (path) CFRelease(path);
    if (name) CFRelease(name);
}


// child must end with '/'
JNIEXPORT Boolean JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_addChildToNode
(JNIEnv *env, jobject klass, jobject jpath, jobject jchild,
 jobject jname, jlong juser, jlong jhost)
{
    // like addNode, but can put a three-level-deep dict into the root file
    CFStringRef path = NULL;
    CFStringRef child = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        child = toCF(env, jchild);
    }
    if (child != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFMutableDictionaryRef parent;
    CFDictionaryRef node;
    CFStringRef topKey;
    CFMutableDictionaryRef topValue;
    Boolean beforeAdd = false;

    if (!path  ||  !child  ||  !name) goto badparams;

    node = createEmptyNode();
    throwIfNull(node, "createEmptyNode failed");

    // copyMutableNode creates the node if necessary
    parent = copyMutableNode(path, name, user, host, &topKey, &topValue);
    throwIfNull(parent, "copyMutableNode failed");
    beforeAdd = CFDictionaryContainsKey(parent, child);
    CFDictionaryAddValue(parent, child, node);
    if (!beforeAdd)
        beforeAdd = CFDictionaryContainsKey(parent, child);
    else
        beforeAdd = false;
    CFPreferencesSetValue(topKey, topValue, name, user, host);

    CFRelease(parent);
    if (topKey) CFRelease(topKey);
    if (topValue) CFRelease(topValue);
 badparent:
    CFRelease(node);
 badnode:
 badparams:
    if (path) CFRelease(path);
    if (child) CFRelease(child);
    if (name) CFRelease(name);
    return beforeAdd;
}


JNIEXPORT void JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_removeChildFromNode
(JNIEnv *env, jobject klass, jobject jpath, jobject jchild,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef child = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        child = toCF(env, jchild);
    }
    if (child != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFDictionaryRef constParent;

    if (!path  ||  !child  ||  !name) goto badparams;

    constParent = copyNodeIfPresent(path, name, user, host);
    if (constParent  &&  CFDictionaryContainsKey(constParent, child)) {
        CFStringRef topKey;
        CFMutableDictionaryRef topValue;
        CFMutableDictionaryRef parent;

        parent = copyMutableNode(path, name, user, host, &topKey, &topValue);
        throwIfNull(parent, "copyMutableNode failed");

        CFDictionaryRemoveValue(parent, child);
        CFPreferencesSetValue(topKey, topValue, name, user, host);

        CFRelease(parent);
        if (topKey) CFRelease(topKey);
        if (topValue) CFRelease(topValue);
    }

 badparent:
    if (constParent) CFRelease(constParent);
 badparams:
    if (path) CFRelease(path);
    if (child) CFRelease(child);
    if (name) CFRelease(name);
}



JNIEXPORT void JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_addKeyToNode
(JNIEnv *env, jobject klass, jobject jpath, jobject jkey, jobject jvalue,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef key = NULL;
    CFStringRef value = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        key = toCF(env, jkey);
    }
    if (key != NULL) {
        value = toCF(env, jvalue);
    }
    if (value != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFMutableDictionaryRef node = NULL;
    CFStringRef topKey;
    CFMutableDictionaryRef topValue;

    if (!path  ||  !key  || !value  ||  !name) goto badparams;

    // fixme optimization: check whether old value and new value are identical
    node = copyMutableNode(path, name, user, host, &topKey, &topValue);
    throwIfNull(node, "copyMutableNode failed");

    CFDictionarySetValue(node, key, value);
    CFPreferencesSetValue(topKey, topValue, name, user, host);

    CFRelease(node);
    if (topKey) CFRelease(topKey);
    if (topValue) CFRelease(topValue);

 badnode:
 badparams:
    if (path) CFRelease(path);
    if (key) CFRelease(key);
    if (value) CFRelease(value);
    if (name) CFRelease(name);
}


JNIEXPORT void JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_removeKeyFromNode
(JNIEnv *env, jobject klass, jobject jpath, jobject jkey,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef key = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        key = toCF(env, jkey);
    }
    if (key != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFDictionaryRef constNode;

    if (!path  ||  !key  ||  !name) goto badparams;

    constNode = copyNodeIfPresent(path, name, user, host);
    if (constNode  &&  CFDictionaryContainsKey(constNode, key)) {
        CFStringRef topKey;
        CFMutableDictionaryRef topValue;
        CFMutableDictionaryRef node;

        node = copyMutableNode(path, name, user, host, &topKey, &topValue);
        throwIfNull(node, "copyMutableNode failed");

        CFDictionaryRemoveValue(node, key);
        CFPreferencesSetValue(topKey, topValue, name, user, host);

        CFRelease(node);
        if (topKey) CFRelease(topKey);
        if (topValue) CFRelease(topValue);
    }

 badnode:
    if (constNode) CFRelease(constNode);
 badparams:
    if (path) CFRelease(path);
    if (key) CFRelease(key);
    if (name) CFRelease(name);
}


// path must end in '/'
JNIEXPORT jstring JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_getKeyFromNode
(JNIEnv *env, jobject klass, jobject jpath, jobject jkey,
 jobject jname, jlong juser, jlong jhost)
{
    CFStringRef path = NULL;
    CFStringRef key = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        key = toCF(env, jkey);
    }
    if (key != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFPropertyListRef value;
    CFDictionaryRef node;
    jstring result = NULL;

    if (!path  ||  !key  ||  !name) goto badparams;

    node = copyNodeIfPresent(path, name, user, host);
    if (node) {
        value = (CFPropertyListRef)CFDictionaryGetValue(node, key);
        if (!value) {
            // key doesn't exist, or other error - no Java errors available
            result = NULL;
        } else {
            CFStringRef cfString = copyToCFString(env, value);
            if ((*env)->ExceptionOccurred(env)) {
                // memory error in copyToCFString
                result = NULL;
            } else if (cfString == NULL) {
                // bogus value type in prefs file - no Java errors available
                result = NULL;
            } else {
                // good cfString
                result = toJavaString(env, cfString);
                CFRelease(cfString);
            }
        }
        CFRelease(node);
    }

 badparams:
    if (path) CFRelease(path);
    if (key) CFRelease(key);
    if (name) CFRelease(name);

    return result;
}


typedef struct {
    jarray result;
    JNIEnv *env;
    CFIndex used;
    Boolean allowSlash;
} BuildJavaArrayArgs;

// CFDictionary applier function that builds an array of Java strings
//   from a CFDictionary of CFPropertyListRefs.
// If args->allowSlash, only strings that end in '/' are added to the array,
//   with the slash removed. Otherwise, only strings that do not end in '/'
//   are added.
// args->result must already exist and be large enough to hold all
//   strings from the dictionary.
// After complete application, args->result may not be full because
//   some of the dictionary values weren't convertible to string. In
//   this case, args->used will be the count of used elements.
static void BuildJavaArrayFn(const void *key, const void *value, void *context)
{
    BuildJavaArrayArgs *args = (BuildJavaArrayArgs *)context;
    CFPropertyListRef propkey = (CFPropertyListRef)key;
    CFStringRef cfString = NULL;
    JNIEnv *env = args->env;

    if ((*env)->ExceptionOccurred(env)) return; // already failed

    cfString = copyToCFString(env, propkey);
    if ((*env)->ExceptionOccurred(env)) {
        // memory error in copyToCFString
    } else if (!cfString) {
        // bogus value type in prefs file - no Java errors available
    } else if (args->allowSlash != CFStringHasSuffix(cfString, CFSTR("/"))) {
        // wrong suffix - ignore
    } else {
        // good cfString
        jstring javaString;
        if (args->allowSlash) {
            CFRange range = CFRangeMake(0, CFStringGetLength(cfString) - 1);
            CFStringRef s = CFStringCreateWithSubstring(NULL, cfString, range);
            CFRelease(cfString);
            cfString = s;
        }
        if (CFStringGetLength(cfString) <= 0) goto bad; // ignore empty
        javaString = toJavaString(env, cfString);
        if ((*env)->ExceptionOccurred(env)) goto bad;
        (*env)->SetObjectArrayElement(env, args->result,args->used,javaString);
        if ((*env)->ExceptionOccurred(env)) goto bad;
        args->used++;
    }

 bad:
    if (cfString) CFRelease(cfString);
}


static jarray getStringsForNode(JNIEnv *env, jobject klass, jobject jpath,
                                jobject jname, jlong juser, jlong jhost,
                                Boolean allowSlash)
{
    CFStringRef path = NULL;
    CFStringRef name = NULL;

    path = toCF(env, jpath);
    if (path != NULL) {
        name = toCF(env, jname);
    }
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    CFDictionaryRef node;
    jarray result = NULL;
    CFIndex count;

    if (!path  ||  !name) goto badparams;

    node = copyNodeIfPresent(path, name, user, host);
    if (!node) {
        result = createJavaStringArray(env, 0);
    } else {
        count = CFDictionaryGetCount(node);
        result = createJavaStringArray(env, count);
        if (result) {
            BuildJavaArrayArgs args;
            args.result = result;
            args.env = env;
            args.used = 0;
            args.allowSlash = allowSlash;
            CFDictionaryApplyFunction(node, BuildJavaArrayFn, &args);
            if (!(*env)->ExceptionOccurred(env)) {
                // array construction succeeded
                if (args.used < count) {
                    // finished array is smaller than expected.
                    // Make a new array of precisely the right size.
                    jarray newresult = createJavaStringArray(env, args.used);
                    if (newresult) {
                        JVM_ArrayCopy(env,0, result,0, newresult,0, args.used);
                        result = newresult;
                    }
                }
            }
        }

        CFRelease(node);
    }

 badparams:
    if (path) CFRelease(path);
    if (name) CFRelease(name);

    return result;
}


JNIEXPORT jarray JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_getKeysForNode
(JNIEnv *env, jobject klass, jobject jpath,
 jobject jname, jlong juser, jlong jhost)
{
    return getStringsForNode(env, klass, jpath, jname, juser, jhost, false);
}

JNIEXPORT jarray JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_getChildrenForNode
(JNIEnv *env, jobject klass, jobject jpath,
 jobject jname, jlong juser, jlong jhost)
{
    return getStringsForNode(env, klass, jpath, jname, juser, jhost, true);
}


// Returns false on error instead of throwing.
JNIEXPORT jboolean JNICALL
Java_java_util_prefs_MacOSXPreferencesFile_synchronize
(JNIEnv *env, jobject klass,
 jstring jname, jlong juser, jlong jhost)
{
    CFStringRef name = toCF(env, jname);
    CFStringRef user = (CFStringRef)jlong_to_ptr(juser);
    CFStringRef host = (CFStringRef)jlong_to_ptr(jhost);
    jboolean result = 0;

    if (name) {
        result = CFPreferencesSynchronize(name, user, host);
        CFRelease(name);
    }

    return result;
}
