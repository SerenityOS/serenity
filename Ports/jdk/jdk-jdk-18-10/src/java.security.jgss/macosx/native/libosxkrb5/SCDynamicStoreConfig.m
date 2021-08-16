/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>
#import <SystemConfiguration/SystemConfiguration.h>
#import "jni_util.h"

#define KERBEROS_DEFAULT_REALMS @"Kerberos-Default-Realms"
#define KERBEROS_DEFAULT_REALM_MAPPINGS @"Kerberos-Domain-Realm-Mappings"
#define KERBEROS_REALM_INFO @"Kerberos:%@"

JavaVM *localVM;

void _SCDynamicStoreCallBack(SCDynamicStoreRef store, CFArrayRef changedKeys, void *info) {
    NSArray *keys = (NSArray *)changedKeys;
    if ([keys count] == 0) return;
    if (![keys containsObject:KERBEROS_DEFAULT_REALMS] && ![keys containsObject:KERBEROS_DEFAULT_REALM_MAPPINGS]) return;

    JNIEnv *env;
    bool createdFromAttach = FALSE;
    jint status = (*localVM)->GetEnv(localVM, (void**)&env, JNI_VERSION_1_2);
    if (status == JNI_EDETACHED) {
        status = (*localVM)->AttachCurrentThreadAsDaemon(localVM, (void**)&env, NULL);
        createdFromAttach = TRUE;
    }
    if (status == 0) {
        jclass jc_Config = (*env)->FindClass(env, "sun/security/krb5/Config");
        CHECK_NULL(jc_Config);
        jmethodID jm_Config_refresh = (*env)->GetStaticMethodID(env, jc_Config, "refresh", "()V");
        CHECK_NULL(jm_Config_refresh);
        (*env)->CallStaticVoidMethod(env, jc_Config, jm_Config_refresh);
        if ((*env)->ExceptionOccurred(env) != NULL) {
            (*env)->ExceptionClear(env);
        }
        if (createdFromAttach) {
            (*localVM)->DetachCurrentThread(localVM);
        }
    }
}

/*
 * Class:     sun_security_krb5_SCDynamicStoreConfig
 * Method:    installNotificationCallback
 */
JNIEXPORT void JNICALL Java_sun_security_krb5_SCDynamicStoreConfig_installNotificationCallback(JNIEnv *env, jclass klass) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
    @try {
        (*env)->GetJavaVM(env, &localVM);
        SCDynamicStoreRef store = SCDynamicStoreCreate(NULL, CFSTR("java"), _SCDynamicStoreCallBack, NULL);
        if (store == NULL) {
            return;
        }

        NSArray *keys = [NSArray arrayWithObjects:KERBEROS_DEFAULT_REALMS, KERBEROS_DEFAULT_REALM_MAPPINGS, nil];
        SCDynamicStoreSetNotificationKeys(store, (CFArrayRef) keys, NULL);

        CFRunLoopSourceRef rls = SCDynamicStoreCreateRunLoopSource(NULL, store, 0);
        if (rls != NULL) {
            CFRunLoopAddSource(CFRunLoopGetMain(), rls, kCFRunLoopDefaultMode);
            CFRelease(rls);
        }

        CFRelease(store);
    } @catch (NSException *e) {
        NSLog(@"%@", [e callStackSymbols]);
    } @finally {
        [pool drain];
    }
}

#define ADD(list, str) { \
    jobject localeObj = (*env)->NewStringUTF(env, [str UTF8String]); \
    (*env)->CallBooleanMethod(env, list, jm_listAdd, localeObj); \
    (*env)->DeleteLocalRef(env, localeObj); \
}

#define ADDNULL(list) (*env)->CallBooleanMethod(env, list, jm_listAdd, NULL)

/*
 * Class:     sun_security_krb5_SCDynamicStoreConfig
 * Method:    getKerberosConfig
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_sun_security_krb5_SCDynamicStoreConfig_getKerberosConfig(JNIEnv *env, jclass klass) {

    jobject newList = 0;

    SCDynamicStoreRef store = NULL;
    CFTypeRef realms = NULL;
    CFTypeRef realmMappings = NULL;
    CFTypeRef realmInfo = NULL;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
    @try {
        SCDynamicStoreRef store = SCDynamicStoreCreate(NULL, CFSTR("java-kerberos"), NULL, NULL);
        if (store == NULL) {
            return NULL;
        }

        CFTypeRef realms = SCDynamicStoreCopyValue(store, (CFStringRef) KERBEROS_DEFAULT_REALMS);
        if (realms == NULL || CFGetTypeID(realms) != CFArrayGetTypeID()) {
            return NULL;
        }

        // This methods returns a ArrayList<String>:
        // (realm kdc* null) null (mapping-domain mapping-realm)*
        jclass jc_arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
        CHECK_NULL_RETURN(jc_arrayListClass, NULL);
        jmethodID jm_arrayListCons = (*env)->GetMethodID(env, jc_arrayListClass, "<init>", "()V");
        CHECK_NULL_RETURN(jm_arrayListCons, NULL);
        jmethodID jm_listAdd = (*env)->GetMethodID(env, jc_arrayListClass, "add", "(Ljava/lang/Object;)Z");
        CHECK_NULL_RETURN(jm_listAdd, NULL);
        newList = (*env)->NewObject(env, jc_arrayListClass, jm_arrayListCons);
        CHECK_NULL_RETURN(newList, NULL);

        for (NSString *realm in (NSArray*)realms) {
            if (realmInfo) CFRelease(realmInfo); // for the previous realm
            realmInfo = SCDynamicStoreCopyValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, realm]);
            if (realmInfo == NULL || CFGetTypeID(realmInfo) != CFDictionaryGetTypeID()) {
                continue;
            }

            ADD(newList, realm);
            NSDictionary* ri = (NSDictionary*)realmInfo;
            for (NSDictionary* k in (NSArray*)ri[@"kdc"]) {
                ADD(newList, k[@"host"]);
            }
            ADDNULL(newList);
        }
        ADDNULL(newList);

        CFTypeRef realmMappings = SCDynamicStoreCopyValue(store, (CFStringRef) KERBEROS_DEFAULT_REALM_MAPPINGS);
        if (realmMappings != NULL && CFGetTypeID(realmMappings) == CFArrayGetTypeID()) {
            for (NSDictionary* d in (NSArray *)realmMappings) {
                for (NSString* s in d) {
                    ADD(newList, s);
                    ADD(newList, d[s]);
                }
            }
        }
    } @catch (NSException *e) {
        NSLog(@"%@", [e callStackSymbols]);
    } @finally {
        [pool drain];
        if (realmInfo) CFRelease(realmInfo);
        if (realmMappings) CFRelease(realmMappings);
        if (realms) CFRelease(realms);
        if (store) CFRelease(store);
    }
    return newList;
}
