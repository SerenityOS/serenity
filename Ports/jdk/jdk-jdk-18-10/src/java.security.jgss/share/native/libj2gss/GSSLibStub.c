/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_security_jgss_wrapper_GSSLibStub.h"
#include "NativeUtil.h"
#include "NativeFunc.h"
#include "jlong.h"
#include <jni.h>

/* Constants for indicating what type of info is needed for inquiries */
const int TYPE_CRED_NAME = 10;
const int TYPE_CRED_TIME = 11;
const int TYPE_CRED_USAGE = 12;

static jclass tlsCBCl = NULL;

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    init
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_init(JNIEnv *env,
                                               jclass jcls,
                                               jstring jlibName,
                                               jboolean jDebug) {
    const char *libName;
    int failed;
    char *error = NULL;

    if (!jDebug) {
      JGSS_DEBUG = 0;
    } else {
      JGSS_DEBUG = 1;
    }

    if (jlibName == NULL) {
        TRACE0("[GSSLibStub_init] GSS lib name is NULL");
        return JNI_FALSE;
    }

    libName = (*env)->GetStringUTFChars(env, jlibName, NULL);
    if (libName == NULL) {
        return JNI_FALSE;
    }
    TRACE1("[GSSLibStub_init] libName=%s", libName);

    /* initialize global function table */
    failed = loadNative(libName);
    (*env)->ReleaseStringUTFChars(env, jlibName, libName);

    if (tlsCBCl == NULL) {

        /* initialize TLS Channel Binding class wrapper */
        jclass cl = (*env)->FindClass(env,
                    "sun/security/jgss/krb5/internal/TlsChannelBindingImpl");
        if (cl == NULL) {           /* exception thrown */
            return JNI_FALSE;
        }
        tlsCBCl = (*env)->NewGlobalRef(env, cl);
    }

    if (!failed) {
        return JNI_TRUE;
    } else {
        if (JGSS_DEBUG) {
#ifdef WIN32
            #define MAX_MSG_SIZE 256
            static CHAR szMsgBuf[MAX_MSG_SIZE];
            DWORD dwRes;
            DWORD dwError = GetLastError();
            dwRes = FormatMessage (
                    FORMAT_MESSAGE_FROM_SYSTEM,
                    NULL,
                    dwError,
                    0,
                    szMsgBuf,
                    MAX_MSG_SIZE,
                    NULL);
            if (0 == dwRes) {
                printf("GSS-API: Unknown failure %d\n", dwError);
            } else {
                printf("GSS-API: %s\n",szMsgBuf);
            }
#else
            char* error = dlerror();
            if (error) {
                TRACE0(error);
            }
#endif
        }
        return JNI_FALSE;
    }
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getMechPtr
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getMechPtr(JNIEnv *env,
                                                     jclass jcls,
                                                     jbyteArray jbytes) {
  gss_OID cOid;
  unsigned int i, len;
  jbyte* bytes;
  int found;

  if (jbytes != NULL) {
    found = 0;
    len = (unsigned int)((*env)->GetArrayLength(env, jbytes) - 2);
    bytes = (*env)->GetByteArrayElements(env, jbytes, NULL);
    if (bytes == NULL) {
      return ptr_to_jlong(NULL);
    }
    for (i = 0; i < ftab->mechs->count; i++) {
      cOid = &(ftab->mechs->elements[i]);
      if (len == cOid->length &&
          (memcmp(cOid->elements, (bytes + 2), len) == 0)) {
        // Found a match
        found = 1;
        break;
      }
    }
    (*env)->ReleaseByteArrayElements(env, jbytes, bytes, 0);

    if (found != 1) {
      checkStatus(env, NULL, GSS_S_BAD_MECH, 0, "[GSSLibStub_getMechPtr]");
      return ptr_to_jlong(NULL);
    } else {
      return ptr_to_jlong(cOid);
    }
  } else {
    return ptr_to_jlong(GSS_C_NO_OID);
  }
}

/*
 * Utility routine which releases the specified gss_channel_bindings_t
 * structure.
 */
void deleteGSSCB(gss_channel_bindings_t cb) {

  if (cb == GSS_C_NO_CHANNEL_BINDINGS) return;

  /* release initiator address */
  if (cb->initiator_addrtype != GSS_C_AF_NULLADDR &&
      cb->initiator_addrtype != GSS_C_AF_UNSPEC) {
    resetGSSBuffer(&(cb->initiator_address));
  }
  /* release acceptor address */
  if (cb->acceptor_addrtype != GSS_C_AF_NULLADDR &&
      cb->acceptor_addrtype != GSS_C_AF_UNSPEC) {
    resetGSSBuffer(&(cb->acceptor_address));
  }
  /* release application data */
  if (cb->application_data.length != 0) {
    resetGSSBuffer(&(cb->application_data));
  }
  free(cb);
}

/*
 * Utility routine which creates a gss_channel_bindings_t structure
 * using the specified org.ietf.jgss.ChannelBinding object.
 * NOTE: must call deleteGSSCB() to free up the resources.
 */
gss_channel_bindings_t newGSSCB(JNIEnv *env, jobject jcb) {
  gss_channel_bindings_t cb;
  jobject jinetAddr;
  jbyteArray value;

  if (jcb == NULL) {
    return GSS_C_NO_CHANNEL_BINDINGS;
  }

  cb = malloc(sizeof(struct gss_channel_bindings_struct));
  if (cb == NULL) {
    throwOutOfMemoryError(env,NULL);
    return NULL;
  }

  // initialize addrtype in CB first
  // LDAP TLS Channel Binding requires GSS_C_AF_UNSPEC address type
  // for unspecified initiator and acceptor addresses.
  // GSS_C_AF_NULLADDR value should be used for unspecified address
  // in all other cases.

  if ((*env)->IsInstanceOf(env, jcb, tlsCBCl)) {
      // TLS Channel Binding requires unspecified addrtype=0
      cb->initiator_addrtype = GSS_C_AF_UNSPEC;
      cb->acceptor_addrtype = GSS_C_AF_UNSPEC;
  } else {
      cb->initiator_addrtype = GSS_C_AF_NULLADDR;
      cb->acceptor_addrtype = GSS_C_AF_NULLADDR;
  }
  // addresses needs to be initialized to empty
  memset(&cb->initiator_address, 0, sizeof(cb->initiator_address));
  memset(&cb->acceptor_address, 0, sizeof(cb->acceptor_address));

  /* set up initiator address */
  jinetAddr = (*env)->CallObjectMethod(env, jcb,
      MID_ChannelBinding_getInitiatorAddr);
  if ((*env)->ExceptionCheck(env)) {
    goto cleanup;
  }
  if (jinetAddr != NULL) {
    value = (*env)->CallObjectMethod(env, jinetAddr,
                                     MID_InetAddress_getAddr);
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
    cb->initiator_addrtype = GSS_C_AF_INET;
    initGSSBuffer(env, value, &(cb->initiator_address));
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
  }
  /* set up acceptor address */
  jinetAddr = (*env)->CallObjectMethod(env, jcb,
      MID_ChannelBinding_getAcceptorAddr);
  if ((*env)->ExceptionCheck(env)) {
    goto cleanup;
  }
  if (jinetAddr != NULL) {
    value = (*env)->CallObjectMethod(env, jinetAddr,
                                     MID_InetAddress_getAddr);
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
    cb->acceptor_addrtype = GSS_C_AF_INET;
    initGSSBuffer(env, value, &(cb->acceptor_address));
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
  }
  /* set up application data */
  value = (*env)->CallObjectMethod(env, jcb,
                                   MID_ChannelBinding_getAppData);
  if ((*env)->ExceptionCheck(env)) {
    goto cleanup;
  }
  initGSSBuffer(env, value, &(cb->application_data));
  if ((*env)->ExceptionCheck(env)) {
    goto cleanup;
  }
  return cb;
cleanup:
  deleteGSSCB(cb);
  return NULL;
}

/*
 * Utility routine for storing the supplementary information
 * into the specified org.ietf.jgss.MessageProp object.
 */
void setSupplementaryInfo(JNIEnv *env, jobject jstub, jobject jprop,
                          int suppInfo, int minor) {
  jboolean isDuplicate, isOld, isUnseq, hasGap;
  jstring minorMsg;

  if (suppInfo != GSS_S_COMPLETE) {
    isDuplicate = ((suppInfo & GSS_S_DUPLICATE_TOKEN) != 0);
    isOld = ((suppInfo & GSS_S_OLD_TOKEN) != 0);
    isUnseq = ((suppInfo & GSS_S_UNSEQ_TOKEN) != 0);
    hasGap = ((suppInfo & GSS_S_GAP_TOKEN) != 0);
    minorMsg = getMinorMessage(env, jstub, minor);
    if ((*env)->ExceptionCheck(env)) {
      return;
    }
    (*env)->CallVoidMethod(env, jprop, MID_MessageProp_setSupplementaryStates,
                           isDuplicate, isOld, isUnseq, hasGap, minor,
                           minorMsg);
  }
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    indicateMechs
 * Signature: ()[Lorg/ietf/jgss/Oid;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_indicateMechs(JNIEnv *env,
                                                        jclass jcls)
{
  if (ftab->mechs != NULL && ftab->mechs != GSS_C_NO_OID_SET) {
    return getJavaOIDArray(env, ftab->mechs);
  } else return NULL;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    inquireNamesForMech
 * Signature: ()[Lorg/ietf/jgss/Oid;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_inquireNamesForMech(JNIEnv *env,
                                                              jobject jobj)
{
  OM_uint32 minor, major;
  gss_OID mech;
  gss_OID_set nameTypes;
  jobjectArray result;

  if (ftab->inquireNamesForMech != NULL) {
    mech = (gss_OID)jlong_to_ptr((*env)->GetLongField(env, jobj, FID_GSSLibStub_pMech));
    nameTypes = GSS_C_NO_OID_SET;

    /* gss_inquire_names_for_mech(...) => N/A */
    major = (*ftab->inquireNamesForMech)(&minor, mech, &nameTypes);

    /* release intermediate buffers before checking status */
    result = getJavaOIDArray(env, nameTypes);
    deleteGSSOIDSet(nameTypes);
    if ((*env)->ExceptionCheck(env)) {
      return NULL;
    }

    checkStatus(env, jobj, major, minor, "[GSSLibStub_inquireNamesForMech]");
    if ((*env)->ExceptionCheck(env)) {
      return NULL;
    }
    return result;
  }
  return NULL;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    releaseName
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_releaseName(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pName)
{
  OM_uint32 minor, major;
  gss_name_t nameHdl;

  nameHdl = (gss_name_t) jlong_to_ptr(pName);

  TRACE1("[GSSLibStub_releaseName] %ld", (long) pName);

  if (nameHdl != GSS_C_NO_NAME) {
    /* gss_release_name(...) => GSS_S_BAD_NAME */
    major = (*ftab->releaseName)(&minor, &nameHdl);
    checkStatus(env, jobj, major, minor, "[GSSLibStub_releaseName]");
  }
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    importName
 * Signature: ([BLorg/ietf/jgss/Oid;)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_importName(JNIEnv *env,
                                                     jobject jobj,
                                                     jbyteArray jnameVal,
                                                     jobject jnameType)
{
  OM_uint32 minor, major;
  gss_buffer_desc nameVal;
  gss_OID nameType;
  gss_name_t nameHdl;
  nameHdl = GSS_C_NO_NAME;

  TRACE0("[GSSLibStub_importName]");

  initGSSBuffer(env, jnameVal, &nameVal);
  if ((*env)->ExceptionCheck(env)) {
      return jlong_zero;
  }

  nameType = newGSSOID(env, jnameType);
  if ((*env)->ExceptionCheck(env)) {
    resetGSSBuffer(&nameVal);
    return jlong_zero;
  }

  /* gss_import_name(...) => GSS_S_BAD_NAMETYPE, GSS_S_BAD_NAME,
     GSS_S_BAD_MECH */
  major = (*ftab->importName)(&minor, &nameVal, nameType, &nameHdl);

  TRACE1("[GSSLibStub_importName] %" PRIuPTR  "", (uintptr_t) nameHdl);

  /* release intermediate buffers */
  deleteGSSOID(nameType);
  resetGSSBuffer(&nameVal);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_importName]");
  if ((*env)->ExceptionCheck(env)) {
    return jlong_zero;
  }
  return ptr_to_jlong(nameHdl);
}


/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    compareName
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_compareName(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pName1,
                                                      jlong pName2)
{
  OM_uint32 minor, major;
  gss_name_t nameHdl1, nameHdl2;
  int isEqual;

  isEqual = 0;
  nameHdl1 = (gss_name_t) jlong_to_ptr(pName1);
  nameHdl2 = (gss_name_t) jlong_to_ptr(pName2);

  TRACE2("[GSSLibStub_compareName] %ld %ld", (long)pName1, (long)pName2);

  if ((nameHdl1 != GSS_C_NO_NAME) && (nameHdl2 != GSS_C_NO_NAME)) {

    /* gss_compare_name(...) => GSS_S_BAD_NAMETYPE, GSS_S_BAD_NAME(!) */
    major = (*ftab->compareName)(&minor, nameHdl1, nameHdl2, &isEqual);

    checkStatus(env, jobj, major, minor, "[GSSLibStub_compareName]");
  }
  return (isEqual != 0);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    canonicalizeName
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_canonicalizeName(JNIEnv *env,
                                                           jobject jobj,
                                                           jlong pName)
{
  OM_uint32 minor, major;
  gss_name_t nameHdl, mnNameHdl;
  gss_OID mech;

  nameHdl = (gss_name_t) jlong_to_ptr(pName);

  TRACE1("[GSSLibStub_canonicalizeName] %ld", (long) pName);

  if (nameHdl != GSS_C_NO_NAME) {
    mech = (gss_OID) jlong_to_ptr((*env)->GetLongField(env, jobj, FID_GSSLibStub_pMech));
    mnNameHdl = GSS_C_NO_NAME;

    /* gss_canonicalize_name(...) may return GSS_S_BAD_NAMETYPE,
       GSS_S_BAD_NAME, GSS_S_BAD_MECH */
    major = (*ftab->canonicalizeName)(&minor, nameHdl, mech, &mnNameHdl);

    TRACE1("[GSSLibStub_canonicalizeName] MN=%" PRIuPTR "", (uintptr_t)mnNameHdl);

    checkStatus(env, jobj, major, minor, "[GSSLibStub_canonicalizeName]");
    if ((*env)->ExceptionCheck(env)) {
      return ptr_to_jlong(GSS_C_NO_NAME);
    }
    return ptr_to_jlong(mnNameHdl);
  }
  return ptr_to_jlong(GSS_C_NO_NAME);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    exportName
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_exportName(JNIEnv *env,
                                                     jobject jobj,
                                                     jlong pName) {
  OM_uint32 minor, major;
  gss_name_t nameHdl, mNameHdl;
  gss_buffer_desc outBuf;
  jbyteArray jresult;

  nameHdl = (gss_name_t) jlong_to_ptr(pName);

  TRACE1("[GSSLibStub_exportName] %ld", (long) pName);

  /* gss_export_name(...) => GSS_S_NAME_NOT_MN, GSS_S_BAD_NAMETYPE,
     GSS_S_BAD_NAME */
  major = (*ftab->exportName)(&minor, nameHdl, &outBuf);

  /* canonicalize the internal name to MN and retry */
  if (major == GSS_S_NAME_NOT_MN) {
    /* release intermediate buffers before retrying */
    (*ftab->releaseBuffer)(&minor, &outBuf);

    TRACE0("[GSSLibStub_exportName] canonicalize and re-try");

    mNameHdl = (gss_name_t)jlong_to_ptr(
        Java_sun_security_jgss_wrapper_GSSLibStub_canonicalizeName
                                        (env, jobj, pName));
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }

    major = (*ftab->exportName)(&minor, mNameHdl, &outBuf);
    Java_sun_security_jgss_wrapper_GSSLibStub_releaseName
                                        (env, jobj, ptr_to_jlong(mNameHdl));
    if ((*env)->ExceptionCheck(env)) {
      /* release intermediate buffers */
      (*ftab->releaseBuffer)(&minor, &outBuf);
      return NULL;
    }
  }

  /* release intermediate buffers before checking status */
  jresult = getJavaBuffer(env, &outBuf);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  checkStatus(env, jobj, major, minor, "[GSSLibStub_exportName]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    displayName
 * Signature: (J)[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_displayName(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pName) {
  OM_uint32 minor, major;
  gss_name_t nameHdl;
  gss_buffer_desc outNameBuf;
  gss_OID outNameType;
  jstring jname;
  jobject jtype;
  jobjectArray jresult;

  nameHdl = (gss_name_t) jlong_to_ptr(pName);

  TRACE1("[GSSLibStub_displayName] %ld", (long) pName);

  if (nameHdl == GSS_C_NO_NAME) {
    checkStatus(env, jobj, GSS_S_BAD_NAME, 0, "[GSSLibStub_displayName]");
    return NULL;
  }

  /* gss_display_name(...) => GSS_S_BAD_NAME */
  major = (*ftab->displayName)(&minor, nameHdl, &outNameBuf, &outNameType);

  /* release intermediate buffers before checking status */
  jname = getJavaString(env, &outNameBuf);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  checkStatus(env, jobj, major, minor, "[GSSLibStub_displayName]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  jtype = getJavaOID(env, outNameType);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  jresult = (*env)->NewObjectArray(env, 2, CLS_Object, NULL);
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  (*env)->SetObjectArrayElement(env, jresult, 0, jname);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  (*env)->SetObjectArrayElement(env, jresult, 1, jtype);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    acquireCred
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_acquireCred(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pName,
                                                      jint reqTime,
                                                      jint usage)
{
  OM_uint32 minor, major;
  gss_OID mech;
  gss_OID_set mechs;
  gss_cred_usage_t credUsage;
  gss_name_t nameHdl;
  gss_cred_id_t credHdl;
  credHdl = GSS_C_NO_CREDENTIAL;

  TRACE0("[GSSLibStub_acquireCred]");

  mech = (gss_OID) jlong_to_ptr((*env)->GetLongField(env, jobj, FID_GSSLibStub_pMech));
  mechs = newGSSOIDSet(mech);
  credUsage = (gss_cred_usage_t) usage;
  nameHdl = (gss_name_t) jlong_to_ptr(pName);

  TRACE2("[GSSLibStub_acquireCred] pName=%ld, usage=%d", (long)pName, usage);

  /* gss_acquire_cred(...) => GSS_S_BAD_MECH, GSS_S_BAD_NAMETYPE,
     GSS_S_BAD_NAME, GSS_S_CREDENTIALS_EXPIRED, GSS_S_NO_CRED */
  major =
    (*ftab->acquireCred)(&minor, nameHdl, reqTime, mechs,
                     credUsage, &credHdl, NULL, NULL);
  /* release intermediate buffers */
  deleteGSSOIDSet(mechs);

  TRACE1("[GSSLibStub_acquireCred] pCred=%" PRIuPTR "", (uintptr_t) credHdl);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_acquireCred]");
  if ((*env)->ExceptionCheck(env)) {
    return jlong_zero;
  }
  return ptr_to_jlong(credHdl);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    releaseCred
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_releaseCred(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pCred)
{
  OM_uint32 minor, major;
  gss_cred_id_t credHdl;

  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);

  TRACE1("[GSSLibStub_releaseCred] %ld", (long int)pCred);

  if (credHdl != GSS_C_NO_CREDENTIAL) {
    /* gss_release_cred(...) => GSS_S_NO_CRED(!) */
    major = (*ftab->releaseCred)(&minor, &credHdl);

    checkStatus(env, jobj, major, minor, "[GSSLibStub_releaseCred]");
    if ((*env)->ExceptionCheck(env)) {
      return jlong_zero;
    }
  }
  return ptr_to_jlong(credHdl);
}

/*
 * Utility routine for obtaining info about a credential.
 */
void inquireCred(JNIEnv *env, jobject jobj, gss_cred_id_t pCred,
                 jint type, void *result) {
  OM_uint32 minor=0, major=0;
  OM_uint32 routineErr;
  gss_cred_id_t credHdl;

  credHdl = pCred;

  TRACE1("[gss_inquire_cred] %" PRIuPTR "", (uintptr_t) pCred);

  /* gss_inquire_cred(...) => GSS_S_DEFECTIVE_CREDENTIAL(!),
     GSS_S_CREDENTIALS_EXPIRED(!), GSS_S_NO_CRED(!) */
  if (type == TYPE_CRED_NAME) {
    major = (*ftab->inquireCred)(&minor, credHdl, result, NULL, NULL, NULL);
  } else if (type == TYPE_CRED_TIME) {
    major = (*ftab->inquireCred)(&minor, credHdl, NULL, result, NULL, NULL);
  } else if (type == TYPE_CRED_USAGE) {
    major = (*ftab->inquireCred)(&minor, credHdl, NULL, NULL, result, NULL);
  }

  routineErr = GSS_ROUTINE_ERROR(major);
  if (routineErr == GSS_S_CREDENTIALS_EXPIRED) {
    /* ignore GSS_S_CREDENTIALS_EXPIRED for query  */
    major = GSS_CALLING_ERROR(major) |
      GSS_SUPPLEMENTARY_INFO(major);
  } else if (routineErr == GSS_S_NO_CRED) {
    /* twik since Java API throws BAD_MECH instead of NO_CRED */
    major = GSS_CALLING_ERROR(major) |
      GSS_S_BAD_MECH  | GSS_SUPPLEMENTARY_INFO(major);
  }
  checkStatus(env, jobj, major, minor, "[gss_inquire_cred]");
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getCredName
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getCredName(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pCred)
{
  gss_name_t nameHdl;
  gss_cred_id_t credHdl;

  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);

  TRACE1("[GSSLibStub_getCredName] %ld", (long int)pCred);

  nameHdl = GSS_C_NO_NAME;
  inquireCred(env, jobj, credHdl, TYPE_CRED_NAME, &nameHdl);
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return jlong_zero;
  }

  TRACE1("[GSSLibStub_getCredName] pName=%" PRIuPTR "", (uintptr_t) nameHdl);
  return ptr_to_jlong(nameHdl);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getCredTime
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getCredTime(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pCred)
{
  gss_cred_id_t credHdl;
  OM_uint32 lifetime;

  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);

  TRACE1("[GSSLibStub_getCredTime] %ld", (long int)pCred);

  lifetime = 0;
  inquireCred(env, jobj, credHdl, TYPE_CRED_TIME, &lifetime);
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return 0;
  }
  return getJavaTime(lifetime);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getCredUsage
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getCredUsage(JNIEnv *env,
                                                       jobject jobj,
                                                       jlong pCred)
{
  gss_cred_usage_t usage;
  gss_cred_id_t credHdl;

  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);

  TRACE1("[GSSLibStub_getCredUsage] %ld", (long int)pCred);

  inquireCred(env, jobj, credHdl, TYPE_CRED_USAGE, &usage);
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return -1;
  }
  return (jint) usage;
}
/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    importContext
 * Signature: ([B)Lsun/security/jgss/wrapper/NativeGSSContext;
 */
JNIEXPORT jobject JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_importContext(JNIEnv *env,
                                                        jobject jobj,
                                                        jbyteArray jctxtToken)
{
  OM_uint32 minor, major;
  gss_buffer_desc ctxtToken;
  gss_ctx_id_t contextHdl;
  gss_OID mech, mech2;

  TRACE0("[GSSLibStub_importContext]");

  contextHdl = GSS_C_NO_CONTEXT;
  initGSSBuffer(env, jctxtToken, &ctxtToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* gss_import_sec_context(...) => GSS_S_NO_CONTEXT, GSS_S_DEFECTIVE_TOKEN,
     GSS_S_UNAVAILABLE, GSS_S_UNAUTHORIZED */
  major = (*ftab->importSecContext)(&minor, &ctxtToken, &contextHdl);

  TRACE1("[GSSLibStub_importContext] pContext=%" PRIuPTR "", (uintptr_t) contextHdl);

  /* release intermediate buffers */
  resetGSSBuffer(&ctxtToken);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_importContext]");
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* now that the context has been imported, proceed to find out
     its mech */
  major = (*ftab->inquireContext)(&minor, contextHdl, NULL, NULL,
                              NULL, &mech, NULL, NULL, NULL);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_importContext] getMech");
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  mech2 = (gss_OID) jlong_to_ptr((*env)->GetLongField(env, jobj,
      FID_GSSLibStub_pMech));

  if (sameMech(mech, mech2) == JNI_TRUE) {
    /* mech match - return the context object */
    return (*env)->NewObject(env, CLS_NativeGSSContext,
                                 MID_NativeGSSContext_ctor,
                                 ptr_to_jlong(contextHdl), jobj);
  } else {
    /* mech mismatch - clean up then return null */
    major = (*ftab->deleteSecContext)(&minor, &contextHdl, GSS_C_NO_BUFFER);
    checkStatus(env, jobj, major, minor,
        "[GSSLibStub_importContext] cleanup");
    return NULL;
  }
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    initContext
 * Signature: (JJLorg/ietf/jgss/ChannelBinding;[BLsun/security/jgss/wrapper/NativeGSSContext;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_initContext(JNIEnv *env,
                                                      jobject jobj,
                                                      jlong pCred,
                                                      jlong pName,
                                                      jobject jcb,
                                                      jbyteArray jinToken,
                                                      jobject jcontextSpi)
{
  OM_uint32 minor, major;
  gss_cred_id_t credHdl ;
  gss_ctx_id_t contextHdl, contextHdlSave;
  gss_name_t targetName;
  gss_OID mech;
  OM_uint32 flags, aFlags;
  OM_uint32 time, aTime;
  gss_channel_bindings_t cb;
  gss_buffer_desc inToken;
  gss_buffer_desc outToken;
  jbyteArray jresult;
/* UNCOMMENT after SEAM bug#6287358 is backported to S10
  gss_OID aMech;
  jobject jMech;
*/

  TRACE0("[GSSLibStub_initContext]");

  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);
  contextHdl = contextHdlSave = (gss_ctx_id_t) jlong_to_ptr(
    (*env)->GetLongField(env, jcontextSpi, FID_NativeGSSContext_pContext));
  targetName = (gss_name_t) jlong_to_ptr(pName);
  mech = (gss_OID) jlong_to_ptr((*env)->GetLongField(env, jobj, FID_GSSLibStub_pMech));
  flags = (OM_uint32) (*env)->GetIntField(env, jcontextSpi,
                                          FID_NativeGSSContext_flags);
  time = getGSSTime((*env)->GetIntField(env, jcontextSpi,
                                        FID_NativeGSSContext_lifetime));
  cb = newGSSCB(env, jcb);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  initGSSBuffer(env, jinToken, &inToken);
  if ((*env)->ExceptionCheck(env)) {
    deleteGSSCB(cb);
    return NULL;
  }

  TRACE2( "[GSSLibStub_initContext] before: pCred=%" PRIuPTR ", pContext=%" PRIuPTR "",
          (uintptr_t)credHdl, (uintptr_t)contextHdl);

  /* gss_init_sec_context(...) => GSS_S_CONTINUE_NEEDED(!),
     GSS_S_DEFECTIVE_TOKEN, GSS_S_NO_CRED, GSS_S_DEFECTIVE_CREDENTIAL(!),
     GSS_S_CREDENTIALS_EXPIRED, GSS_S_BAD_BINDINGS, GSS_S_BAD_MIC,
     GSS_S_OLD_TOKEN, GSS_S_DUPLICATE_TOKEN, GSS_S_NO_CONTEXT(!),
     GSS_S_BAD_NAMETYPE, GSS_S_BAD_NAME(!), GSS_S_BAD_MECH */
  major = (*ftab->initSecContext)(&minor, credHdl,
                                 &contextHdl, targetName, mech,
                                 flags, time, cb, &inToken, NULL /*aMech*/,
                                 &outToken, &aFlags, &aTime);

  TRACE2("[GSSLibStub_initContext] after: pContext=%" PRIuPTR ", outToken len=%ld",
            (uintptr_t)contextHdl, (long)outToken.length);

  // update context handle with the latest value if changed
  // this is to work with both MIT and Solaris. Former deletes half-built
  // context if error occurs
  if (contextHdl != contextHdlSave) {
    (*env)->SetLongField(env, jcontextSpi, FID_NativeGSSContext_pContext,
                         ptr_to_jlong(contextHdl));
    TRACE1("[GSSLibStub_initContext] set pContext=%" PRIuPTR "", (uintptr_t)contextHdl);
  }

  if (GSS_ERROR(major) == GSS_S_COMPLETE) {
    /* update member values if needed */
    (*env)->SetIntField(env, jcontextSpi, FID_NativeGSSContext_flags, aFlags);
    TRACE1("[GSSLibStub_initContext] set flags=0x%x", aFlags);

    if (major == GSS_S_COMPLETE) {
      (*env)->SetIntField(env, jcontextSpi, FID_NativeGSSContext_lifetime,
                          getJavaTime(aTime));
      TRACE0("[GSSLibStub_initContext] context established");

      (*env)->SetBooleanField(env, jcontextSpi,
                              FID_NativeGSSContext_isEstablished,
                              JNI_TRUE);

/* UNCOMMENT after SEAM bug#6287358 is backported to S10
      jMech = getJavaOID(env, aMech);
      (*env)->SetObjectField(env, jcontextSpi,
                             FID_NativeGSSContext_actualMech, jMech);
*/
    } else if (major & GSS_S_CONTINUE_NEEDED) {
      TRACE0("[GSSLibStub_initContext] context not established");
      major -= GSS_S_CONTINUE_NEEDED;
    }
  }

  /* release intermediate buffers before checking status */
  deleteGSSCB(cb);
  resetGSSBuffer(&inToken);
  jresult = getJavaBuffer(env, &outToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  checkStatus(env, jobj, major, minor, "[GSSLibStub_initContext]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    acceptContext
 * Signature: (JLorg/ietf/jgss/ChannelBinding;[BLsun/security/jgss/wrapper/NativeGSSContext;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_acceptContext(JNIEnv *env,
                                                        jobject jobj,
                                                        jlong pCred,
                                                        jobject jcb,
                                                        jbyteArray jinToken,
                                                        jobject jcontextSpi)
{
  OM_uint32 minor, major;
  OM_uint32 minor2, major2;
  gss_ctx_id_t contextHdl, contextHdlSave;
  gss_cred_id_t credHdl;
  gss_buffer_desc inToken;
  gss_channel_bindings_t cb;
  gss_name_t srcName;
  gss_buffer_desc outToken;
  gss_OID aMech;
  OM_uint32 aFlags;
  OM_uint32 aTime;
  gss_cred_id_t delCred;
  jobject jsrcName = NULL;
  jobject jdelCred;
  jobject jMech;
  jboolean setTarget;
  gss_name_t targetName;
  jobject jtargetName;

  TRACE0("[GSSLibStub_acceptContext]");

  contextHdl = contextHdlSave = (gss_ctx_id_t)jlong_to_ptr(
    (*env)->GetLongField(env, jcontextSpi, FID_NativeGSSContext_pContext));
  credHdl = (gss_cred_id_t) jlong_to_ptr(pCred);
  initGSSBuffer(env, jinToken, &inToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  cb = newGSSCB(env, jcb);
  if ((*env)->ExceptionCheck(env)) {
    resetGSSBuffer(&inToken);
    return NULL;
  }
  srcName = targetName = GSS_C_NO_NAME;
  delCred = GSS_C_NO_CREDENTIAL;
  setTarget = (credHdl == GSS_C_NO_CREDENTIAL);
  aFlags = 0;

  TRACE2( "[GSSLibStub_acceptContext] before: pCred=%" PRIuPTR ", pContext=%" PRIuPTR "",
          (uintptr_t) credHdl, (uintptr_t) contextHdl);

  /* gss_accept_sec_context(...) => GSS_S_CONTINUE_NEEDED(!),
     GSS_S_DEFECTIVE_TOKEN, GSS_S_DEFECTIVE_CREDENTIAL(!),
     GSS_S_NO_CRED, GSS_S_CREDENTIALS_EXPIRED, GSS_S_BAD_BINDINGS,
     GSS_S_NO_CONTEXT(!), GSS_S_BAD_MIC, GSS_S_OLD_TOKEN,
     GSS_S_DUPLICATE_TOKEN, GSS_S_BAD_MECH */
  major =
    (*ftab->acceptSecContext)(&minor, &contextHdl, credHdl,
                           &inToken, cb, &srcName, &aMech, &outToken,
                           &aFlags, &aTime, &delCred);
  /* release intermediate buffers before checking status */

  deleteGSSCB(cb);
  resetGSSBuffer(&inToken);

  TRACE3("[GSSLibStub_acceptContext] after: pCred=%" PRIuPTR ", pContext=%" PRIuPTR ", pDelegCred=%" PRIuPTR "",
        (uintptr_t)credHdl, (uintptr_t)contextHdl, (uintptr_t) delCred);

  // update context handle with the latest value if changed
  // this is to work with both MIT and Solaris. Former deletes half-built
  // context if error occurs
  if (contextHdl != contextHdlSave) {
    (*env)->SetLongField(env, jcontextSpi, FID_NativeGSSContext_pContext,
                         ptr_to_jlong(contextHdl));
    TRACE1("[GSSLibStub_acceptContext] set pContext=%" PRIuPTR "", (uintptr_t)contextHdl);
  }

  if (GSS_ERROR(major) == GSS_S_COMPLETE) {
    /* update member values if needed */
    // WORKAROUND for a Heimdal bug
    if (delCred == GSS_C_NO_CREDENTIAL) {
        aFlags &= 0xfffffffe;
    }
    (*env)->SetIntField(env, jcontextSpi, FID_NativeGSSContext_flags, aFlags);
    TRACE1("[GSSLibStub_acceptContext] set flags=0x%x", aFlags);

    if (setTarget) {
      major2 = (*ftab->inquireContext)(&minor2, contextHdl, NULL,
                              &targetName, NULL, NULL, NULL,
                              NULL, NULL);
      checkStatus(env, jobj, major2, minor2,
                    "[GSSLibStub_acceptContext] inquire");
      if ((*env)->ExceptionCheck(env)) {
         goto error;
      }

      jtargetName = (*env)->NewObject(env, CLS_GSSNameElement,
                                MID_GSSNameElement_ctor,
                                ptr_to_jlong(targetName), jobj);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }

      TRACE1("[GSSLibStub_acceptContext] set targetName=%" PRIuPTR "",
              (uintptr_t)targetName);

      (*env)->SetObjectField(env, jcontextSpi, FID_NativeGSSContext_targetName,
                             jtargetName);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }
    }
    if (srcName != GSS_C_NO_NAME) {
      jsrcName = (*env)->NewObject(env, CLS_GSSNameElement,
                                   MID_GSSNameElement_ctor,
                                   ptr_to_jlong(srcName), jobj);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }

      TRACE1("[GSSLibStub_acceptContext] set srcName=%" PRIuPTR "", (uintptr_t)srcName);

      (*env)->SetObjectField(env, jcontextSpi, FID_NativeGSSContext_srcName,
                             jsrcName);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }
    }
    if (major == GSS_S_COMPLETE) {
      TRACE0("[GSSLibStub_acceptContext] context established");

      (*env)->SetIntField(env, jcontextSpi, FID_NativeGSSContext_lifetime,
                          getJavaTime(aTime));
      (*env)->SetBooleanField(env, jcontextSpi,
                              FID_NativeGSSContext_isEstablished,
                              JNI_TRUE);
      jMech = getJavaOID(env, aMech);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }
      (*env)->SetObjectField(env, jcontextSpi,
                             FID_NativeGSSContext_actualMech, jMech);
      if ((*env)->ExceptionCheck(env)) {
        goto error;
      }
      if (delCred != GSS_C_NO_CREDENTIAL) {
        jdelCred = (*env)->NewObject(env, CLS_GSSCredElement,
                                     MID_GSSCredElement_ctor,
                                     ptr_to_jlong(delCred), jsrcName, jMech);
        if ((*env)->ExceptionCheck(env)) {
          goto error;
        }
        (*env)->SetObjectField(env, jcontextSpi,
                               FID_NativeGSSContext_delegatedCred,
                               jdelCred);
        TRACE1("[GSSLibStub_acceptContext] set delegatedCred=%" PRIuPTR "",
                (uintptr_t) delCred);

        if ((*env)->ExceptionCheck(env)) {
          goto error;
        }
      }
    } else if (major & GSS_S_CONTINUE_NEEDED) {
      TRACE0("[GSSLibStub_acceptContext] context not established");

      if (aFlags & GSS_C_PROT_READY_FLAG) {
        (*env)->SetIntField(env, jcontextSpi, FID_NativeGSSContext_lifetime,
                            getJavaTime(aTime));
      }
      major -= GSS_S_CONTINUE_NEEDED;
    }
  }
  return getJavaBuffer(env, &outToken);

error:
  (*ftab->releaseBuffer)(&minor, &outToken);
  if (srcName != GSS_C_NO_NAME) {
    (*ftab->releaseName)(&minor, &srcName);
  }
  if (targetName != GSS_C_NO_NAME) {
    (*ftab->releaseName)(&minor, &targetName);
  }
  if (delCred != GSS_C_NO_CREDENTIAL) {
    (*ftab->releaseCred) (&minor, &delCred);
  }
  return NULL;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    inquireContext
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_inquireContext(JNIEnv *env,
                                                         jobject jobj,
                                                         jlong pContext)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  gss_name_t srcName, targetName;
  OM_uint32 time;
  OM_uint32 flags;
  int isInitiator, isEstablished;
#if defined (_WIN32) && defined (_MSC_VER)
  __declspec(align(8))
#endif
  jlong result[6];
  jlongArray jresult;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_inquireContext] %" PRIuPTR "", (uintptr_t)contextHdl);

  srcName = targetName = GSS_C_NO_NAME;
  time = 0;
  flags = isInitiator = isEstablished = 0;

  /* gss_inquire_context(...) => GSS_S_NO_CONTEXT(!) */
  major = (*ftab->inquireContext)(&minor, contextHdl, &srcName,
                              &targetName, &time, NULL, &flags,
                              &isInitiator, &isEstablished);
  /* update member values if needed */
  TRACE2("[GSSLibStub_inquireContext] srcName %" PRIuPTR ", targetName %" PRIuPTR "",
      (uintptr_t)srcName, (uintptr_t)targetName);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_inquireContext]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  result[0] = ptr_to_jlong(srcName);
  result[1] = ptr_to_jlong(targetName);
  result[2] = (jlong) isInitiator;
  result[3] = (jlong) isEstablished;
  result[4] = (jlong) flags;
  result[5] = (jlong) getJavaTime(time);

  jresult = (*env)->NewLongArray(env, 6);
  if (jresult == NULL) {
    return NULL;
  }
  (*env)->SetLongArrayRegion(env, jresult, 0, 6, result);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getContextMech
 * Signature: (J)Lorg/ietf/jgss/Oid;
 */
JNIEXPORT jobject JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getContextMech(JNIEnv *env,
                                                         jobject jobj,
                                                         jlong pContext)
{
  OM_uint32 minor, major;
  gss_OID mech;
  gss_ctx_id_t contextHdl;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_getContextMech] %ld", (long int)pContext);

  major = (*ftab->inquireContext)(&minor, contextHdl, NULL, NULL,
                                NULL, &mech, NULL,  NULL, NULL);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_getContextMech]");
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return getJavaOID(env, mech);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getContextName
 * Signature: (JZ)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getContextName(JNIEnv *env,
  jobject jobj, jlong pContext, jboolean isSrc)
{
  OM_uint32 minor, major;
  gss_name_t nameHdl;
  gss_ctx_id_t contextHdl;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE2("[GSSLibStub_getContextName] %" PRIuPTR ", isSrc=%d",
          (uintptr_t)contextHdl, isSrc);

  nameHdl = GSS_C_NO_NAME;
  if (isSrc == JNI_TRUE) {
    major = (*ftab->inquireContext)(&minor, contextHdl, &nameHdl, NULL,
                                NULL, NULL, NULL,  NULL, NULL);
  } else {
    major = (*ftab->inquireContext)(&minor, contextHdl, NULL, &nameHdl,
                                NULL, NULL, NULL,  NULL, NULL);
  }

  checkStatus(env, jobj, major, minor, "[GSSLibStub_inquireContextAll]");
  /* return immediately if an exception has occurred */
  if ((*env)->ExceptionCheck(env)) {
    return jlong_zero;
  }

  TRACE1("[GSSLibStub_getContextName] pName=%" PRIuPTR "", (uintptr_t) nameHdl);

  return ptr_to_jlong(nameHdl);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getContextTime
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getContextTime(JNIEnv *env,
                                                         jobject jobj,
                                                         jlong pContext) {
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  OM_uint32 time;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_getContextTime] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) return 0;

  /* gss_context_time(...) => GSS_S_CONTEXT_EXPIRED(!),
     GSS_S_NO_CONTEXT(!) */
  major = (*ftab->contextTime)(&minor, contextHdl, &time);
  if (GSS_ROUTINE_ERROR(major) == GSS_S_CONTEXT_EXPIRED) {
    major = GSS_CALLING_ERROR(major) | GSS_SUPPLEMENTARY_INFO(major);
  }
  checkStatus(env, jobj, major, minor, "[GSSLibStub_getContextTime]");
  if ((*env)->ExceptionCheck(env)) {
    return 0;
  }
  return getJavaTime(time);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    deleteContext
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_deleteContext(JNIEnv *env,
                                                        jobject jobj,
                                                        jlong pContext)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_deleteContext] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) return ptr_to_jlong(GSS_C_NO_CONTEXT);

  /* gss_delete_sec_context(...) => GSS_S_NO_CONTEXT(!) */
  major = (*ftab->deleteSecContext)(&minor, &contextHdl, GSS_C_NO_BUFFER);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_deleteContext]");
  if ((*env)->ExceptionCheck(env)) {
    return jlong_zero;
  }
  return (jlong) ptr_to_jlong(contextHdl);
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    wrapSizeLimit
 * Signature: (JIII)I
 */
JNIEXPORT jint JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_wrapSizeLimit(JNIEnv *env,
                                                        jobject jobj,
                                                        jlong pContext,
                                                        jint reqFlag,
                                                        jint jqop,
                                                        jint joutSize)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  OM_uint32 outSize, maxInSize;
  gss_qop_t qop;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_wrapSizeLimit] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_NO_CONTEXT, 0,
        "[GSSLibStub_wrapSizeLimit]");
    return 0;
  }

  qop = (gss_qop_t) jqop;
  outSize = (OM_uint32) joutSize;
  /* gss_wrap_size_limit(...) => GSS_S_NO_CONTEXT(!), GSS_S_CONTEXT_EXPIRED,
     GSS_S_BAD_QOP */
  major = (*ftab->wrapSizeLimit)(&minor, contextHdl, reqFlag,
                              qop, outSize, &maxInSize);

  checkStatus(env, jobj, major, minor, "[GSSLibStub_wrapSizeLimit]");
  if ((*env)->ExceptionCheck(env)) {
    return 0;
  }
  return (jint) maxInSize;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    exportContext
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_exportContext(JNIEnv *env,
                                                        jobject jobj,
                                                        jlong pContext)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  gss_buffer_desc interProcToken;
  jbyteArray jresult;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_exportContext] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_NO_CONTEXT, 0, "[GSSLibStub_exportContext]");
    return NULL;
  }
  /* gss_export_sec_context(...) => GSS_S_CONTEXT_EXPIRED,
     GSS_S_NO_CONTEXT, GSS_S_UNAVAILABLE */
  major =
    (*ftab->exportSecContext)(&minor, &contextHdl, &interProcToken);

  /* release intermediate buffers */
  jresult = getJavaBuffer(env, &interProcToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  checkStatus(env, jobj, major, minor, "[GSSLibStub_exportContext]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    getMic
 * Signature: (JI[B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_getMic(JNIEnv *env, jobject jobj,
                                                 jlong pContext, jint jqop,
                                                 jbyteArray jmsg)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  gss_qop_t qop;
  gss_buffer_desc msg;
  gss_buffer_desc msgToken;
  jbyteArray jresult;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_getMic] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_CONTEXT_EXPIRED, 0, "[GSSLibStub_getMic]");
    return NULL;
  }
  qop = (gss_qop_t) jqop;
  initGSSBuffer(env, jmsg, &msg);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* gss_get_mic(...) => GSS_S_CONTEXT_EXPIRED, GSS_S_NO_CONTEXT(!),
     GSS_S_BAD_QOP */
  major =
    (*ftab->getMic)(&minor, contextHdl, qop, &msg, &msgToken);

  /* release intermediate buffers */
  resetGSSBuffer(&msg);
  jresult = getJavaBuffer(env, &msgToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  checkStatus(env, jobj, major, minor, "[GSSLibStub_getMic]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    verifyMic
 * Signature: (J[B[BLorg/ietf/jgss/MessageProp;)V
 */
JNIEXPORT void JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_verifyMic(JNIEnv *env,
                                                    jobject jobj,
                                                    jlong pContext,
                                                    jbyteArray jmsgToken,
                                                    jbyteArray jmsg,
                                                    jobject jprop)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  gss_buffer_desc msg;
  gss_buffer_desc msgToken;
  gss_qop_t qop;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_verifyMic] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_CONTEXT_EXPIRED, 0,
        "[GSSLibStub_verifyMic]");
    return;
  }

  qop = (gss_qop_t) (*env)->CallIntMethod(env, jprop, MID_MessageProp_getQOP);
  if ((*env)->ExceptionCheck(env)) { return; }

  initGSSBuffer(env, jmsg, &msg);
  if ((*env)->ExceptionCheck(env)) { return; }

  initGSSBuffer(env, jmsgToken, &msgToken);
  if ((*env)->ExceptionCheck(env)) {
    resetGSSBuffer(&msg);
    return;
  }

  /* gss_verify_mic(...) => GSS_S_DEFECTIVE_TOKEN, GSS_S_BAD_MIC,
     GSS_S_CONTEXT_EXPIRED, GSS_S_DUPLICATE_TOKEN(!), GSS_S_OLD_TOKEN(!),
     GSS_S_UNSEQ_TOKEN(!), GSS_S_GAP_TOKEN(!), GSS_S_NO_CONTEXT(!) */
  major =
    (*ftab->verifyMic)(&minor, contextHdl, &msg, &msgToken, &qop);

  /* release intermediate buffers */
  resetGSSBuffer(&msg);
  resetGSSBuffer(&msgToken);

  checkStatus(env, jobj, GSS_ERROR(major), minor, "[GSSLibStub_verifyMic]");
  if ((*env)->ExceptionCheck(env)) {
    return;
  }

  (*env)->CallVoidMethod(env, jprop, MID_MessageProp_setQOP, qop);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }

  setSupplementaryInfo(env, jobj, jprop, GSS_SUPPLEMENTARY_INFO(major),
                       minor);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    wrap
 * Signature: (J[BLorg/ietf/jgss/MessageProp;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_wrap(JNIEnv *env,
                                               jobject jobj,
                                               jlong pContext,
                                               jbyteArray jmsg,
                                               jobject jprop)
{
  OM_uint32 minor, major;
  jboolean confFlag;
  gss_qop_t qop;
  gss_buffer_desc msg;
  gss_buffer_desc msgToken;
  int confState;
  gss_ctx_id_t contextHdl;
  jbyteArray jresult;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_wrap] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_CONTEXT_EXPIRED, 0, "[GSSLibStub_wrap]");
    return NULL;
  }

  confFlag =
    (*env)->CallBooleanMethod(env, jprop, MID_MessageProp_getPrivacy);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  qop = (gss_qop_t)
    (*env)->CallIntMethod(env, jprop, MID_MessageProp_getQOP);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  initGSSBuffer(env, jmsg, &msg);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* gss_wrap(...) => GSS_S_CONTEXT_EXPIRED, GSS_S_NO_CONTEXT(!),
     GSS_S_BAD_QOP */
  major = (*ftab->wrap)(&minor, contextHdl, confFlag, qop, &msg, &confState,
                   &msgToken);

  /* release intermediate buffers */
  resetGSSBuffer(&msg);
  jresult = getJavaBuffer(env, &msgToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  checkStatus(env, jobj, major, minor, "[GSSLibStub_wrap]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  (*env)->CallVoidMethod(env, jprop, MID_MessageProp_setPrivacy,
                         (confState? JNI_TRUE:JNI_FALSE));
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  return jresult;
}

/*
 * Class:     sun_security_jgss_wrapper_GSSLibStub
 * Method:    unwrap
 * Signature: (J[BLorg/ietf/jgss/MessageProp;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_security_jgss_wrapper_GSSLibStub_unwrap(JNIEnv *env,
                                                 jobject jobj,
                                                 jlong pContext,
                                                 jbyteArray jmsgToken,
                                                 jobject jprop)
{
  OM_uint32 minor, major;
  gss_ctx_id_t contextHdl;
  gss_buffer_desc msgToken;
  gss_buffer_desc msg;
  int confState;
  gss_qop_t qop;
  jbyteArray jresult;

  contextHdl = (gss_ctx_id_t) jlong_to_ptr(pContext);

  TRACE1("[GSSLibStub_unwrap] %" PRIuPTR "", (uintptr_t)contextHdl);

  if (contextHdl == GSS_C_NO_CONTEXT) {
    // Twik per javadoc
    checkStatus(env, jobj, GSS_S_CONTEXT_EXPIRED, 0, "[GSSLibStub_unwrap]");
    return NULL;
  }

  initGSSBuffer(env, jmsgToken, &msgToken);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  confState = 0;
  qop = GSS_C_QOP_DEFAULT;
  /* gss_unwrap(...) => GSS_S_DEFECTIVE_TOKEN, GSS_S_BAD_MIC,
     GSS_S_CONTEXT_EXPIRED, GSS_S_DUPLICATE_TOKEN(!), GSS_S_OLD_TOKEN(!),
     GSS_S_UNSEQ_TOKEN(!), GSS_S_GAP_TOKEN(!), GSS_S_NO_CONTEXT(!) */
  major =
    (*ftab->unwrap)(&minor, contextHdl, &msgToken, &msg, &confState, &qop);

  /* release intermediate buffers */
  resetGSSBuffer(&msgToken);
  jresult = getJavaBuffer(env, &msg);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  checkStatus(env, jobj, GSS_ERROR(major), minor, "[GSSLibStub_unwrap]");
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* update the message prop with relevant info */
  (*env)->CallVoidMethod(env, jprop, MID_MessageProp_setPrivacy,
                         (confState != 0));
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  (*env)->CallVoidMethod(env, jprop, MID_MessageProp_setQOP, qop);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }
  setSupplementaryInfo(env, jobj, jprop, GSS_SUPPLEMENTARY_INFO(major),
                         minor);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return jresult;
}
