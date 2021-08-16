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

#import "apple_security_KeychainStore.h"
#import "jni_util.h"
#import <Security/Security.h>
#import <Security/SecImportExport.h>
#import <CoreServices/CoreServices.h>  // (for require() macros)
#import <Cocoa/Cocoa.h>

static jstring getLabelFromItem(JNIEnv *env, SecKeychainItemRef inItem)
{
    OSStatus status;
    jstring returnValue = NULL;
    char *attribCString = NULL;

    SecKeychainAttribute itemAttrs[] = { { kSecLabelItemAttr, 0, NULL } };
    SecKeychainAttributeList attrList = { sizeof(itemAttrs) / sizeof(itemAttrs[0]), itemAttrs };

    status = SecKeychainItemCopyContent(inItem, NULL, &attrList, NULL, NULL);

    if(status) {
        cssmPerror("getLabelFromItem: SecKeychainItemCopyContent", status);
        goto errOut;
    }

    attribCString = malloc(itemAttrs[0].length + 1);
    if (attribCString == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
        goto errOut;
    }

    strncpy(attribCString, itemAttrs[0].data, itemAttrs[0].length);
    attribCString[itemAttrs[0].length] = '\0';
    returnValue = (*env)->NewStringUTF(env, attribCString);

errOut:
    SecKeychainItemFreeContent(&attrList, NULL);
    if (attribCString) free(attribCString);
    return returnValue;
}

static jlong getModDateFromItem(JNIEnv *env, SecKeychainItemRef inItem)
{
    OSStatus status;
    SecKeychainAttribute itemAttrs[] = { { kSecModDateItemAttr, 0, NULL } };
    SecKeychainAttributeList attrList = { sizeof(itemAttrs) / sizeof(itemAttrs[0]), itemAttrs };
    jlong returnValue = 0;

    status = SecKeychainItemCopyContent(inItem, NULL, &attrList, NULL, NULL);

    if(status) {
        // This is almost always missing, so don't dump an error.
        // cssmPerror("getModDateFromItem: SecKeychainItemCopyContent", status);
        goto errOut;
    }

    memcpy(&returnValue, itemAttrs[0].data, itemAttrs[0].length);

errOut:
    SecKeychainItemFreeContent(&attrList, NULL);
    return returnValue;
}

static void setLabelForItem(NSString *inLabel, SecKeychainItemRef inItem)
{
    OSStatus status;
    const char *labelCString = [inLabel UTF8String];

    // Set up attribute vector (each attribute consists of {tag, length, pointer}):
    SecKeychainAttribute attrs[] = {
        { kSecLabelItemAttr, strlen(labelCString), (void *)labelCString }
    };

    const SecKeychainAttributeList attributes = { sizeof(attrs) / sizeof(attrs[0]), attrs };

    // Not changing data here, just attributes.
    status = SecKeychainItemModifyContent(inItem, &attributes, 0, NULL);

    if(status) {
        cssmPerror("setLabelForItem: SecKeychainItemModifyContent", status);
    }
}

/*
 * Given a SecIdentityRef, do our best to construct a complete, ordered, and
 * verified cert chain, returning the result in a CFArrayRef. The result is
 * can be passed back to Java as a chain for a private key.
 */
static OSStatus completeCertChain(
                                     SecIdentityRef         identity,
                                     SecCertificateRef    trustedAnchor,    // optional additional trusted anchor
                                     bool                 includeRoot,     // include the root in outArray
                                     CFArrayRef            *outArray)        // created and RETURNED
{
    SecTrustRef                    secTrust = NULL;
    SecPolicyRef                policy = NULL;
    SecPolicySearchRef            policySearch = NULL;
    SecTrustResultType            secTrustResult;
    CSSM_TP_APPLE_EVIDENCE_INFO *dummyEv;            // not used
    CFArrayRef                    certChain = NULL;   // constructed chain, CERTS ONLY
    CFMutableArrayRef             subjCerts;            // passed to SecTrust
    CFMutableArrayRef             certArray;            // returned array starting with
                                                    //   identity
    CFIndex                     numResCerts;
    CFIndex                     dex;
    OSStatus                     ortn;
      SecCertificateRef             certRef;

    /* First element in out array is the SecIdentity */
    certArray = CFArrayCreateMutable(NULL, 0, &kCFTypeArrayCallBacks);
    CFArrayAppendValue(certArray, identity);

    /* the single element in certs-to-be-evaluated comes from the identity */
       ortn = SecIdentityCopyCertificate(identity, &certRef);
    if(ortn) {
        /* should never happen */
        cssmPerror("SecIdentityCopyCertificate", ortn);
        return ortn;
    }

    /*
     * Now use SecTrust to get a complete cert chain, using all of the
     * user's keychains to look for intermediate certs.
     * NOTE this does NOT handle root certs which are not in the system
     * root cert DB.
     */
    subjCerts = CFArrayCreateMutable(NULL, 1, &kCFTypeArrayCallBacks);
    CFArraySetValueAtIndex(subjCerts, 0, certRef);

    /* the array owns the subject cert ref now */
    CFRelease(certRef);

    /* Get a SecPolicyRef for generic X509 cert chain verification */
    ortn = SecPolicySearchCreate(CSSM_CERT_X_509v3,
                                 &CSSMOID_APPLE_X509_BASIC,
                                 NULL,                // value
                                 &policySearch);
    if(ortn) {
        /* should never happen */
        cssmPerror("SecPolicySearchCreate", ortn);
        goto errOut;
    }
    ortn = SecPolicySearchCopyNext(policySearch, &policy);
    if(ortn) {
        /* should never happen */
        cssmPerror("SecPolicySearchCopyNext", ortn);
        goto errOut;
    }

    /* build a SecTrustRef for specified policy and certs */
    ortn = SecTrustCreateWithCertificates(subjCerts,
                                          policy, &secTrust);
    if(ortn) {
        cssmPerror("SecTrustCreateWithCertificates", ortn);
        goto errOut;
    }

    if(trustedAnchor) {
        /*
        * Tell SecTrust to trust this one in addition to the current
         * trusted system-wide anchors.
         */
        CFMutableArrayRef newAnchors;
        CFArrayRef currAnchors;

        ortn = SecTrustCopyAnchorCertificates(&currAnchors);
        if(ortn) {
            /* should never happen */
            cssmPerror("SecTrustCopyAnchorCertificates", ortn);
            goto errOut;
        }
        newAnchors = CFArrayCreateMutableCopy(NULL,
                                              CFArrayGetCount(currAnchors) + 1,
                                              currAnchors);
        CFRelease(currAnchors);
        CFArrayAppendValue(newAnchors, trustedAnchor);
        ortn = SecTrustSetAnchorCertificates(secTrust, newAnchors);
        CFRelease(newAnchors);
        if(ortn) {
            cssmPerror("SecTrustSetAnchorCertificates", ortn);
            goto errOut;
        }
    }

    /* evaluate: GO */
    ortn = SecTrustEvaluate(secTrust, &secTrustResult);
    if(ortn) {
        cssmPerror("SecTrustEvaluate", ortn);
        goto errOut;
    }
    switch(secTrustResult) {
        case kSecTrustResultUnspecified:
            /* cert chain valid, no special UserTrust assignments; drop thru */
        case kSecTrustResultProceed:
            /* cert chain valid AND user explicitly trusts this */
            break;
        default:
            /*
             * Cert chain construction failed.
             * Just go with the single subject cert we were given; maybe the
             * peer can complete the chain.
             */
            ortn = noErr;
            goto errOut;
    }

    /* get resulting constructed cert chain */
    ortn = SecTrustGetResult(secTrust, &secTrustResult, &certChain, &dummyEv);
    if(ortn) {
        cssmPerror("SecTrustEvaluate", ortn);
        goto errOut;
    }

    /*
     * Copy certs from constructed chain to our result array, skipping
     * the leaf (which is already there, as a SecIdentityRef) and possibly
     * a root.
     */
    numResCerts = CFArrayGetCount(certChain);
    if(numResCerts < 1) {
        /*
         * Can't happen: If chain doesn't verify to a root, we'd
         * have bailed after SecTrustEvaluate().
         */
        ortn = noErr;
        goto errOut;
    }
    if(!includeRoot) {
        /* skip the last (root) cert) */
        numResCerts--;
    }
    for(dex=1; dex<numResCerts; dex++) {
        certRef = (SecCertificateRef)CFArrayGetValueAtIndex(certChain, dex);
        CFArrayAppendValue(certArray, certRef);
    }
errOut:
        /* clean up */
        if(secTrust) {
            CFRelease(secTrust);
        }
    if(subjCerts) {
        CFRelease(subjCerts);
    }
    if(policy) {
        CFRelease(policy);
    }
    if(policySearch) {
        CFRelease(policySearch);
    }
    *outArray = certArray;
    return ortn;
}

static void addIdentitiesToKeystore(JNIEnv *env, jobject keyStore)
{
    // Search the user keychain list for all identities. Identities are a certificate/private key association that
    // can be chosen for a purpose such as signing or an SSL connection.
    SecIdentitySearchRef identitySearch = NULL;
    // Pass 0 if you want all identities returned by this search
    OSStatus err = SecIdentitySearchCreate(NULL, 0, &identitySearch);
    SecIdentityRef theIdentity = NULL;
    OSErr searchResult = noErr;

    jclass jc_KeychainStore = (*env)->FindClass(env, "apple/security/KeychainStore");
    CHECK_NULL(jc_KeychainStore);
    jmethodID jm_createKeyEntry = (*env)->GetMethodID(env, jc_KeychainStore, "createKeyEntry", "(Ljava/lang/String;JJ[J[[B)V");
    CHECK_NULL(jm_createKeyEntry);
    do {
        searchResult = SecIdentitySearchCopyNext(identitySearch, &theIdentity);

        if (searchResult == noErr) {
            // Get the cert from the identity, then generate a chain.
            SecCertificateRef certificate;
            SecIdentityCopyCertificate(theIdentity, &certificate);
            CFArrayRef certChain = NULL;

            // *** Should do something with this error...
            err = completeCertChain(theIdentity, NULL, TRUE, &certChain);

            CFIndex i, certCount = CFArrayGetCount(certChain);

            // Make a java array of certificate data from the chain.
            jclass byteArrayClass = (*env)->FindClass(env, "[B");
            if (byteArrayClass == NULL) {
                goto errOut;
            }
            jobjectArray javaCertArray = (*env)->NewObjectArray(env, certCount, byteArrayClass, NULL);
            // Cleanup first then check for a NULL return code
            (*env)->DeleteLocalRef(env, byteArrayClass);
            if (javaCertArray == NULL) {
                goto errOut;
            }

            // And, make an array of the certificate refs.
            jlongArray certRefArray = (*env)->NewLongArray(env, certCount);
            if (certRefArray == NULL) {
                goto errOut;
            }

            SecCertificateRef currCertRef = NULL;

            for (i = 0; i < certCount; i++) {
                CSSM_DATA currCertData;

                if (i == 0)
                    currCertRef = certificate;
                else
                    currCertRef = (SecCertificateRef)CFArrayGetValueAtIndex(certChain, i);

                bzero(&currCertData, sizeof(CSSM_DATA));
                err = SecCertificateGetData(currCertRef, &currCertData);
                jbyteArray encodedCertData = (*env)->NewByteArray(env, currCertData.Length);
                if (encodedCertData == NULL) {
                    goto errOut;
                }
                (*env)->SetByteArrayRegion(env, encodedCertData, 0, currCertData.Length, (jbyte *)currCertData.Data);
                (*env)->SetObjectArrayElement(env, javaCertArray, i, encodedCertData);
                jlong certRefElement = ptr_to_jlong(currCertRef);
                (*env)->SetLongArrayRegion(env, certRefArray, i, 1, &certRefElement);
            }

            // Get the private key.  When needed we'll export the data from it later.
            SecKeyRef privateKeyRef;
            err = SecIdentityCopyPrivateKey(theIdentity, &privateKeyRef);

            // Find the label.  It's a 'blob', but we interpret as characters.
            jstring alias = getLabelFromItem(env, (SecKeychainItemRef)certificate);
            if (alias == NULL) {
                goto errOut;
            }

            // Find the creation date.
            jlong creationDate = getModDateFromItem(env, (SecKeychainItemRef)certificate);

            // Call back to the Java object to create Java objects corresponding to this security object.
            jlong nativeKeyRef = ptr_to_jlong(privateKeyRef);
            (*env)->CallVoidMethod(env, keyStore, jm_createKeyEntry, alias, creationDate, nativeKeyRef, certRefArray, javaCertArray);
            JNU_CHECK_EXCEPTION(env);
        }
    } while (searchResult == noErr);

errOut:
    if (identitySearch != NULL) {
        CFRelease(identitySearch);
    }
}

static void addCertificatesToKeystore(JNIEnv *env, jobject keyStore)
{
    // Search the user keychain list for all X509 certificates.
    SecKeychainSearchRef keychainItemSearch = NULL;
    OSStatus err = SecKeychainSearchCreateFromAttributes(NULL, kSecCertificateItemClass, NULL, &keychainItemSearch);
    SecKeychainItemRef theItem = NULL;
    OSErr searchResult = noErr;

    jclass jc_KeychainStore = (*env)->FindClass(env, "apple/security/KeychainStore");
    CHECK_NULL(jc_KeychainStore);
    jmethodID jm_createTrustedCertEntry = (*env)->GetMethodID(
            env, jc_KeychainStore, "createTrustedCertEntry", "(Ljava/lang/String;JJ[B)V");
    CHECK_NULL(jm_createTrustedCertEntry);
    do {
        searchResult = SecKeychainSearchCopyNext(keychainItemSearch, &theItem);

        if (searchResult == noErr) {
            // Make a byte array with the DER-encoded contents of the certificate.
            SecCertificateRef certRef = (SecCertificateRef)theItem;
            CSSM_DATA currCertificate;
            err = SecCertificateGetData(certRef, &currCertificate);
            jbyteArray certData = (*env)->NewByteArray(env, currCertificate.Length);
            if (certData == NULL) {
                goto errOut;
            }
            (*env)->SetByteArrayRegion(env, certData, 0, currCertificate.Length, (jbyte *)currCertificate.Data);

            // Find the label.  It's a 'blob', but we interpret as characters.
            jstring alias = getLabelFromItem(env, theItem);
            if (alias == NULL) {
                goto errOut;
            }

            // Find the creation date.
            jlong creationDate = getModDateFromItem(env, theItem);

            // Call back to the Java object to create Java objects corresponding to this security object.
            jlong nativeRef = ptr_to_jlong(certRef);
            (*env)->CallVoidMethod(env, keyStore, jm_createTrustedCertEntry, alias, nativeRef, creationDate, certData);
            JNU_CHECK_EXCEPTION(env);
        }
    } while (searchResult == noErr);

errOut:
    if (keychainItemSearch != NULL) {
        CFRelease(keychainItemSearch);
    }
}

/*
 * Class:     apple_security_KeychainStore
 * Method:    _getEncodedKeyData
 * Signature: (J)[B
     */
JNIEXPORT jbyteArray JNICALL Java_apple_security_KeychainStore__1getEncodedKeyData
(JNIEnv *env, jobject this, jlong keyRefLong, jcharArray passwordObj)
{
    SecKeyRef keyRef = (SecKeyRef)jlong_to_ptr(keyRefLong);
    SecKeyImportExportParameters paramBlock;
    OSStatus err = noErr;
    CFDataRef exportedData = NULL;
    jbyteArray returnValue = NULL;
    CFStringRef passwordStrRef = NULL;

    jsize passwordLen = 0;
    jchar *passwordChars = NULL;

    if (passwordObj) {
        passwordLen = (*env)->GetArrayLength(env, passwordObj);

        if (passwordLen > 0) {
            passwordChars = (*env)->GetCharArrayElements(env, passwordObj, NULL);
            if (passwordChars == NULL) {
                goto errOut;
            }

            passwordStrRef = CFStringCreateWithCharactersNoCopy(NULL, passwordChars, passwordLen, kCFAllocatorNull);
            if (passwordStrRef == NULL) {
                goto errOut;
            }
        }
    }

    paramBlock.version = SEC_KEY_IMPORT_EXPORT_PARAMS_VERSION;
    // Note that setting the flags field **requires** you to pass in a password of some kind.  The keychain will not prompt you.
    paramBlock.flags = 0;
    paramBlock.passphrase = passwordStrRef;
    paramBlock.alertTitle = NULL;
    paramBlock.alertPrompt = NULL;
    paramBlock.accessRef = NULL;
    paramBlock.keyUsage = CSSM_KEYUSE_ANY;
    paramBlock.keyAttributes = CSSM_KEYATTR_RETURN_DEFAULT;

    err = SecKeychainItemExport(keyRef, kSecFormatPKCS12, 0, &paramBlock, &exportedData);

    if (err == noErr) {
        CFIndex size = CFDataGetLength(exportedData);
        returnValue = (*env)->NewByteArray(env, size);
        if (returnValue == NULL) {
            goto errOut;
        }
        (*env)->SetByteArrayRegion(env, returnValue, 0, size, (jbyte *)CFDataGetBytePtr(exportedData));
    }

errOut:
    if (exportedData) CFRelease(exportedData);
    if (passwordStrRef) CFRelease(passwordStrRef);
    if (passwordChars) {
        // clear the password and release
        memset(passwordChars, 0, passwordLen);
        (*env)->ReleaseCharArrayElements(env, passwordObj, passwordChars,
            JNI_ABORT);
    }
    return returnValue;
}


/*
 * Class:     apple_security_KeychainStore
 * Method:    _scanKeychain
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_apple_security_KeychainStore__1scanKeychain
(JNIEnv *env, jobject this)
{
    // Look for 'identities' -- private key and certificate chain pairs -- and add those.
    // Search for these first, because a certificate that's found here as part of an identity will show up
    // again later as a certificate.
    addIdentitiesToKeystore(env, this);

    JNU_CHECK_EXCEPTION(env);

    // Scan current keychain for trusted certificates.
    addCertificatesToKeystore(env, this);

}

NSString* JavaStringToNSString(JNIEnv *env, jstring jstr) {
     if (jstr == NULL) {
         return NULL;
     }
     jsize len = (*env)->GetStringLength(env, jstr);
     const jchar *chars = (*env)->GetStringChars(env, jstr, NULL);
     if (chars == NULL) {
         return NULL;
     }
     NSString *result = [NSString stringWithCharacters:(UniChar *)chars length:len];
     (*env)->ReleaseStringChars(env, jstr, chars);
     return result;
}

/*
 * Class:     apple_security_KeychainStore
 * Method:    _addItemToKeychain
 * Signature: (Ljava/lang/String;[B)I
*/
JNIEXPORT jlong JNICALL Java_apple_security_KeychainStore__1addItemToKeychain
(JNIEnv *env, jobject this, jstring alias, jboolean isCertificate, jbyteArray rawDataObj, jcharArray passwordObj)
{
    OSStatus err;
    jlong returnValue = 0;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
    @try {
        jsize dataSize = (*env)->GetArrayLength(env, rawDataObj);
        jbyte *rawData = (*env)->GetByteArrayElements(env, rawDataObj, NULL);
        if (rawData == NULL) {
            goto errOut;
        }

        CFDataRef cfDataToImport = CFDataCreate(kCFAllocatorDefault, (UInt8 *)rawData, dataSize);
        CFArrayRef createdItems = NULL;

        SecKeychainRef defaultKeychain = NULL;
        SecKeychainCopyDefault(&defaultKeychain);

        SecExternalFormat dataFormat = (isCertificate == JNI_TRUE ? kSecFormatX509Cert : kSecFormatWrappedPKCS8);

        // Convert the password obj into a CFStringRef that the keychain importer can use for encryption.
        SecKeyImportExportParameters paramBlock;
        CFStringRef passwordStrRef = NULL;

        jsize passwordLen = 0;
        jchar *passwordChars = NULL;

        if (passwordObj) {
            passwordLen = (*env)->GetArrayLength(env, passwordObj);

            if (passwordLen > 0) {
                passwordChars = (*env)->GetCharArrayElements(env, passwordObj, NULL);
                if (passwordChars == NULL) {
                    goto errOut;
                }

                passwordStrRef = CFStringCreateWithCharactersNoCopy(NULL, passwordChars, passwordLen, kCFAllocatorNull);
                if (passwordStrRef == NULL) {
                    goto errOut;
                }
            }
        }

        paramBlock.version = SEC_KEY_IMPORT_EXPORT_PARAMS_VERSION;
        // Note that setting the flags field **requires** you to pass in a password of some kind.  The keychain will not prompt you.
        paramBlock.flags = 0;
        paramBlock.passphrase = passwordStrRef;
        paramBlock.alertTitle = NULL;
        paramBlock.alertPrompt = NULL;
        paramBlock.accessRef = NULL;
        paramBlock.keyUsage = CSSM_KEYUSE_ANY;
        paramBlock.keyAttributes = CSSM_KEYATTR_RETURN_DEFAULT;

        err = SecKeychainItemImport(cfDataToImport, NULL, &dataFormat, NULL,
                                    0, &paramBlock, defaultKeychain, &createdItems);
        if (cfDataToImport != NULL) {
            CFRelease(cfDataToImport);
        }

        if (err == noErr) {
            SecKeychainItemRef anItem = (SecKeychainItemRef)CFArrayGetValueAtIndex(createdItems, 0);

            // Don't bother labeling keys. They become part of an identity, and are not an accessible part of the keychain.
            if (CFGetTypeID(anItem) == SecCertificateGetTypeID()) {
                setLabelForItem(JavaStringToNSString(env, alias), anItem);
            }

            // Retain the item, since it will be released once when the array holding it gets released.
            CFRetain(anItem);
            returnValue = ptr_to_jlong(anItem);
        } else {
            cssmPerror("_addItemToKeychain: SecKeychainItemImport", err);
        }

        if (createdItems != NULL) {
            CFRelease(createdItems);
        }

    errOut:
        if (rawData) {
            (*env)->ReleaseByteArrayElements(env, rawDataObj, rawData, JNI_ABORT);
        }

        if (passwordStrRef) CFRelease(passwordStrRef);
        if (passwordChars) {
            // clear the password and release
            memset(passwordChars, 0, passwordLen);
            (*env)->ReleaseCharArrayElements(env, passwordObj, passwordChars,
                JNI_ABORT);
        }
    } @catch (NSException *e) {
        NSLog(@"%@", [e callStackSymbols]);
    } @finally {
        [pool drain];
    }
    return returnValue;
}

/*
 * Class:     apple_security_KeychainStore
 * Method:    _removeItemFromKeychain
 * Signature: (J)I
*/
JNIEXPORT jint JNICALL Java_apple_security_KeychainStore__1removeItemFromKeychain
(JNIEnv *env, jobject this, jlong keychainItem)
{
    SecKeychainItemRef itemToRemove = jlong_to_ptr(keychainItem);
    return SecKeychainItemDelete(itemToRemove);
}

/*
 * Class:     apple_security_KeychainStore
 * Method:    _releaseKeychainItemRef
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_apple_security_KeychainStore__1releaseKeychainItemRef
(JNIEnv *env, jobject this, jlong keychainItem)
{
    SecKeychainItemRef itemToFree = jlong_to_ptr(keychainItem);
    CFRelease(itemToFree);
}
