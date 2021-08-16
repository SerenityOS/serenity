/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#import <jni.h>

#define KERBEROS_DEFAULT_REALMS @"Kerberos-Default-Realms"
#define KERBEROS_DEFAULT_REALM_MAPPINGS @"Kerberos-Domain-Realm-Mappings"
#define KERBEROS_REALM_INFO @"Kerberos:%@"

int removeAll(SCDynamicStoreRef store) {
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) KERBEROS_DEFAULT_REALMS));
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, @"A.COM"]));
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, @"B.COM"]));
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) KERBEROS_DEFAULT_REALM_MAPPINGS));
    return 1;
}

int removeRealm(SCDynamicStoreRef store) {
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, @"A.COM"]));
    return 1;
}

int removeMapping(SCDynamicStoreRef store) {
    fprintf(stderr, "%d\n", SCDynamicStoreRemoveValue(store, (CFStringRef) KERBEROS_DEFAULT_REALM_MAPPINGS));
    return 1;
}

int addMapping(SCDynamicStoreRef store) {
    NSDictionary *dict = [NSDictionary dictionaryWithObjectsAndKeys:
        @"a", @"A",
        @"b", @"B",
        @"c", @"C",
        @"d", @"D",
        nil];
    fprintf(stderr, "%d\n", SCDynamicStoreSetValue(store, (CFStringRef) KERBEROS_DEFAULT_REALM_MAPPINGS, [NSArray arrayWithObjects: dict, nil]));
    return 1;
}

int addAll(SCDynamicStoreRef store) {
    NSArray *keys = [NSArray arrayWithObjects:@"A.COM", @"B.COM", nil];
    Boolean b = SCDynamicStoreSetValue(store, (CFStringRef) KERBEROS_DEFAULT_REALMS, keys);
    fprintf(stderr, "%d\n", b);
    if (!b) return 0;

    NSDictionary *k1 = [NSDictionary dictionaryWithObjectsAndKeys:
        @"kdc1.a.com", @"host", nil];
    NSDictionary *k2 = [NSDictionary dictionaryWithObjectsAndKeys:
        @"kdc2.a.com", @"host", nil];
    NSDictionary *dict = [NSDictionary dictionaryWithObjectsAndKeys:
        [NSArray arrayWithObjects: k1, k2, nil], @"kdc",
        nil];
    fprintf(stderr, "%d\n", SCDynamicStoreSetValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, @"A.COM"], dict));

    k1 = [NSDictionary dictionaryWithObjectsAndKeys:
        @"kdc1.b.com", @"host", nil];
    k2 = [NSDictionary dictionaryWithObjectsAndKeys:
        @"kdc2.b.com", @"host", nil];
    dict = [NSDictionary dictionaryWithObjectsAndKeys:
        [NSArray arrayWithObjects: k1, k2, nil], @"kdc",
        nil];
    fprintf(stderr, "%d\n", SCDynamicStoreSetValue(store, (CFStringRef) [NSString stringWithFormat:KERBEROS_REALM_INFO, @"B.COM"], dict));
    addMapping(store);
    return 1;
}

JNIEXPORT jint JNICALL Java_TestDynamicStore_actionInternal(JNIEnv *env, jclass clazz, jchar what, jchar whom) {
    SCDynamicStoreRef store = SCDynamicStoreCreate(NULL, CFSTR("java-kerberos"), NULL, NULL);
    fprintf(stderr, ">>> action: %c %c\n", what, whom);
    @try {
        switch (what) {
            case 'a':
                switch (whom) {
                    case 'a': return addAll(store);
                    case 'm': return addMapping(store);
                }
                break;
            case 'r':
                switch (whom) {
                    case 'a': return removeAll(store);
                    case 'r': return removeRealm(store);
                    case 'm': return removeMapping(store);
                }
                break;
        }
        return 0;
    } @finally {
        CFRelease(store);
    }
}
