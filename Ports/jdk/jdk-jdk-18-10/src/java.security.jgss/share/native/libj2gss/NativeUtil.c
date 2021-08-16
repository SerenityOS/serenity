/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "NativeUtil.h"
#include "NativeFunc.h"
#include "jlong.h"
#include <jni.h>
#include "jni_util.h"

const int JAVA_DUPLICATE_TOKEN_CODE = 19; /* DUPLICATE_TOKEN */
const int JAVA_OLD_TOKEN_CODE = 20; /* OLD_TOKEN */
const int JAVA_UNSEQ_TOKEN_CODE = 21; /* UNSEQ_TOKEN */
const int JAVA_GAP_TOKEN_CODE = 22; /* GAP_TOKEN */
const int JAVA_ERROR_CODE[] = {
  2,  /* BAD_MECH */
  3,  /* BAD_NAME */
  4,  /* BAD_NAMETYPE */
  1,  /* BAD_BINDINGS */
  5,  /* BAD_STATUS */
  6,  /* BAD_MIC */
  13, /* NO_CRED */
  12, /* NO_CONTEXT */
  10, /* DEFECTIVE_TOKEN */
  9,  /* DEFECTIVE_CREDENTIAL */
  8,  /* CREDENTIAL_EXPIRED */
  7,  /* CONTEXT_EXPIRED */
  11, /* FAILURE */
  14, /* BAD_QOP */
  15, /* UNAUTHORIZED */
  16, /* UNAVAILABLE */
  17, /* DUPLICATE_ELEMENT */
  18, /* NAME_NOT_MN */
};
const char SPNEGO_BYTES[] = {
 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02
};

jclass CLS_Object;
jclass CLS_String;
jclass CLS_Oid;
jclass CLS_GSSException;
jclass CLS_GSSNameElement;
jclass CLS_GSSCredElement;
jclass CLS_NativeGSSContext;
jclass CLS_SunNativeProvider;
jmethodID MID_String_ctor;
jmethodID MID_Oid_ctor1;
jmethodID MID_Oid_getDER;
jmethodID MID_MessageProp_getPrivacy;
jmethodID MID_MessageProp_getQOP;
jmethodID MID_MessageProp_setPrivacy;
jmethodID MID_MessageProp_setQOP;
jmethodID MID_MessageProp_setSupplementaryStates;
jmethodID MID_GSSException_ctor3;
jmethodID MID_ChannelBinding_getInitiatorAddr;
jmethodID MID_ChannelBinding_getAcceptorAddr;
jmethodID MID_ChannelBinding_getAppData;
jmethodID MID_InetAddress_getAddr;
jmethodID MID_GSSNameElement_ctor;
jmethodID MID_GSSCredElement_ctor;
jmethodID MID_NativeGSSContext_ctor;
jfieldID FID_GSSLibStub_pMech;
jfieldID FID_NativeGSSContext_pContext;
jfieldID FID_NativeGSSContext_srcName;
jfieldID FID_NativeGSSContext_targetName;
jfieldID FID_NativeGSSContext_isInitiator;
jfieldID FID_NativeGSSContext_isEstablished;
jfieldID FID_NativeGSSContext_delegatedCred;
jfieldID FID_NativeGSSContext_flags;
jfieldID FID_NativeGSSContext_lifetime;
jfieldID FID_NativeGSSContext_actualMech;

int JGSS_DEBUG;

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *jvm, void *reserved) {
  JNIEnv *env;
  jclass cls;

  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
    return JNI_EVERSION; /* JNI version not supported */
  }
  /* Retrieve and store the classes in global ref */
  cls = (*env)->FindClass(env, "java/lang/Object");
  if (cls == NULL) {
    printf("Couldn't find Object class\n");
    return JNI_ERR;
  }
  CLS_Object = (*env)->NewGlobalRef(env, cls);
  if (CLS_Object == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "java/lang/String");
  if (cls == NULL) {
    printf("Couldn't find String class\n");
    return JNI_ERR;
  }
  CLS_String = (*env)->NewGlobalRef(env, cls);
  if (CLS_String == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "org/ietf/jgss/Oid");
  if (cls == NULL) {
    printf("Couldn't find org.ietf.jgss.Oid class\n");
    return JNI_ERR;
  }
  CLS_Oid = (*env)->NewGlobalRef(env, cls);
  if (CLS_Oid == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "org/ietf/jgss/GSSException");
  if (cls == NULL) {
    printf("Couldn't find org.ietf.jgss.GSSException class\n");
    return JNI_ERR;
  }
  CLS_GSSException = (*env)->NewGlobalRef(env, cls);
  if (CLS_GSSException == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "sun/security/jgss/wrapper/GSSNameElement");
  if (cls == NULL) {
    printf("Couldn't find sun.security.jgss.wrapper.GSSNameElement class\n");
    return JNI_ERR;
  }
  CLS_GSSNameElement = (*env)->NewGlobalRef(env, cls);
  if (CLS_GSSNameElement == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "sun/security/jgss/wrapper/GSSCredElement");
  if (cls == NULL) {
    printf("Couldn't find sun.security.jgss.wrapper.GSSCredElement class\n");
    return JNI_ERR;
  }
  CLS_GSSCredElement = (*env)->NewGlobalRef(env, cls);
  if (CLS_GSSCredElement == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "sun/security/jgss/wrapper/NativeGSSContext");
  if (cls == NULL) {
    printf("Couldn't find sun.security.jgss.wrapper.NativeGSSContext class\n");
    return JNI_ERR;
  }
  CLS_NativeGSSContext = (*env)->NewGlobalRef(env, cls);
  if (CLS_NativeGSSContext == NULL) {
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "sun/security/jgss/wrapper/SunNativeProvider");
  if (cls == NULL) {
    printf("Couldn't find sun.security.jgss.wrapper.SunNativeProvider class\n");
    return JNI_ERR;
  }
  CLS_SunNativeProvider = (*env)->NewGlobalRef(env, cls);
  if (CLS_SunNativeProvider == NULL) {
    return JNI_ERR;
  }
  /* Compute and cache the method ID */
  MID_String_ctor = (*env)->GetMethodID(env, CLS_String,
                                        "<init>", "([B)V");
  if (MID_String_ctor == NULL) {
    printf("Couldn't find String(byte[]) constructor\n");
    return JNI_ERR;
  }
  MID_Oid_ctor1 =
    (*env)->GetMethodID(env, CLS_Oid, "<init>", "([B)V");
  if (MID_Oid_ctor1 == NULL) {
    printf("Couldn't find Oid(byte[]) constructor\n");
    return JNI_ERR;
  }
  MID_Oid_getDER = (*env)->GetMethodID(env, CLS_Oid, "getDER", "()[B");
  if (MID_Oid_getDER == NULL) {
    printf("Couldn't find Oid.getDER() method\n");
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "org/ietf/jgss/MessageProp");
  if (cls == NULL) {
    printf("Couldn't find org.ietf.jgss.MessageProp class\n");
    return JNI_ERR;
  }
  MID_MessageProp_getPrivacy =
    (*env)->GetMethodID(env, cls, "getPrivacy", "()Z");
  if (MID_MessageProp_getPrivacy == NULL) {
    printf("Couldn't find MessageProp.getPrivacy() method\n");
    return JNI_ERR;
  }
  MID_MessageProp_getQOP = (*env)->GetMethodID(env, cls, "getQOP", "()I");
  if (MID_MessageProp_getQOP == NULL) {
    printf("Couldn't find MessageProp.getQOP() method\n");
    return JNI_ERR;
  }
  MID_MessageProp_setPrivacy =
    (*env)->GetMethodID(env, cls, "setPrivacy", "(Z)V");
  if (MID_MessageProp_setPrivacy == NULL) {
    printf("Couldn't find MessageProp.setPrivacy(boolean) method\n");
    return JNI_ERR;
  }
  MID_MessageProp_setQOP = (*env)->GetMethodID(env, cls, "setQOP", "(I)V");
  if (MID_MessageProp_setQOP == NULL) {
    printf("Couldn't find MessageProp.setQOP(int) method\n");
    return JNI_ERR;
  }
  MID_MessageProp_setSupplementaryStates =
    (*env)->GetMethodID(env, cls, "setSupplementaryStates",
                        "(ZZZZILjava/lang/String;)V");
  if (MID_MessageProp_setSupplementaryStates == NULL) {
    printf("Couldn't find MessageProp.setSupplementaryStates(...) method\n");
    return JNI_ERR;
  }
  MID_GSSException_ctor3 = (*env)->GetMethodID
    (env, CLS_GSSException, "<init>", "(IILjava/lang/String;)V");
  if (MID_GSSException_ctor3 == NULL) {
    printf("Couldn't find GSSException(int, int, String) constructor\n");
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "org/ietf/jgss/ChannelBinding");
  if (cls == NULL) {
    printf("Couldn't find org.ietf.jgss.ChannelBinding class\n");
    return JNI_ERR;
  }
  MID_ChannelBinding_getInitiatorAddr =
    (*env)->GetMethodID(env, cls, "getInitiatorAddress",
                        "()Ljava/net/InetAddress;");
  if (MID_ChannelBinding_getInitiatorAddr == NULL) {
    printf("Couldn't find ChannelBinding.getInitiatorAddress() method\n");
    return JNI_ERR;
  }
  MID_ChannelBinding_getAcceptorAddr =
    (*env)->GetMethodID(env, cls, "getAcceptorAddress",
                        "()Ljava/net/InetAddress;");
  if (MID_ChannelBinding_getAcceptorAddr == NULL) {
    printf("Couldn't find ChannelBinding.getAcceptorAddress() method\n");
    return JNI_ERR;
  }
  MID_ChannelBinding_getAppData =
    (*env)->GetMethodID(env, cls, "getApplicationData", "()[B");
  if (MID_ChannelBinding_getAppData == NULL) {
    printf("Couldn't find ChannelBinding.getApplicationData() method\n");
    return JNI_ERR;
  }
  cls = (*env)->FindClass(env, "java/net/InetAddress");
  if (cls == NULL) {
    printf("Couldn't find java.net.InetAddress class\n");
    return JNI_ERR;
  }
  MID_InetAddress_getAddr = (*env)->GetMethodID(env, cls, "getAddress",
                                                "()[B");
  if (MID_InetAddress_getAddr == NULL) {
    printf("Couldn't find InetAddress.getAddress() method\n");
    return JNI_ERR;
  }
  MID_GSSNameElement_ctor =
    (*env)->GetMethodID(env, CLS_GSSNameElement,
                        "<init>", "(JLsun/security/jgss/wrapper/GSSLibStub;)V");
  if (MID_GSSNameElement_ctor == NULL) {
    printf("Couldn't find GSSNameElement(long, GSSLibStub) constructor\n");
    return JNI_ERR;
  }
  MID_GSSCredElement_ctor =
    (*env)->GetMethodID(env, CLS_GSSCredElement, "<init>",
        "(JLsun/security/jgss/wrapper/GSSNameElement;Lorg/ietf/jgss/Oid;)V");
  if (MID_GSSCredElement_ctor == NULL) {
    printf("Couldn't find GSSCredElement(long, GSSLibStub) constructor\n");
    return JNI_ERR;
  }
  MID_NativeGSSContext_ctor =
    (*env)->GetMethodID(env, CLS_NativeGSSContext, "<init>",
                        "(JLsun/security/jgss/wrapper/GSSLibStub;)V");
  if (MID_NativeGSSContext_ctor == NULL) {
    printf("Couldn't find NativeGSSContext(long, GSSLibStub) constructor\n");
    return JNI_ERR;
  }
  /* Compute and cache the field ID */
  cls = (*env)->FindClass(env, "sun/security/jgss/wrapper/GSSLibStub");
  if (cls == NULL) {
    printf("Couldn't find sun.security.jgss.wrapper.GSSLibStub class\n");
    return JNI_ERR;
  }
  FID_GSSLibStub_pMech =
    (*env)->GetFieldID(env, cls, "pMech", "J");
  if (FID_GSSLibStub_pMech == NULL) {
    printf("Couldn't find GSSLibStub.pMech field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_pContext =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "pContext", "J");
  if (FID_NativeGSSContext_pContext == NULL) {
    printf("Couldn't find NativeGSSContext.pContext field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_srcName =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "srcName",
                       "Lsun/security/jgss/wrapper/GSSNameElement;");
  if (FID_NativeGSSContext_srcName == NULL) {
    printf("Couldn't find NativeGSSContext.srcName field\n");
   return JNI_ERR;
  }
  FID_NativeGSSContext_targetName =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "targetName",
                       "Lsun/security/jgss/wrapper/GSSNameElement;");
  if (FID_NativeGSSContext_targetName == NULL) {
    printf("Couldn't find NativeGSSContext.targetName field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_isInitiator =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "isInitiator", "Z");
  if (FID_NativeGSSContext_isInitiator == NULL) {
    printf("Couldn't find NativeGSSContext.isInitiator field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_isEstablished =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "isEstablished", "Z");
  if (FID_NativeGSSContext_isEstablished == NULL) {
    printf("Couldn't find NativeGSSContext.isEstablished field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_delegatedCred =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "delegatedCred",
                       "Lsun/security/jgss/wrapper/GSSCredElement;");
  if (FID_NativeGSSContext_delegatedCred == NULL) {
    printf("Couldn't find NativeGSSContext.delegatedCred field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_flags =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "flags", "I");
  if (FID_NativeGSSContext_flags == NULL) {
    printf("Couldn't find NativeGSSContext.flags field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_lifetime =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "lifetime", "I");
  if (FID_NativeGSSContext_lifetime == NULL) {
    printf("Couldn't find NativeGSSContext.lifetime field\n");
    return JNI_ERR;
  }
  FID_NativeGSSContext_actualMech =
    (*env)->GetFieldID(env, CLS_NativeGSSContext, "actualMech",
                       "Lorg/ietf/jgss/Oid;");
  if (FID_NativeGSSContext_actualMech == NULL) {
    printf("Couldn't find NativeGSSContext.actualMech field\n");
    return JNI_ERR;
  }
  return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL
DEF_JNI_OnUnload(JavaVM *jvm, void *reserved) {
  JNIEnv *env;

  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
    return;
  }
  /* Delete the global refs */
  (*env)->DeleteGlobalRef(env, CLS_Object);
  (*env)->DeleteGlobalRef(env, CLS_String);
  (*env)->DeleteGlobalRef(env, CLS_Oid);
  (*env)->DeleteGlobalRef(env, CLS_GSSException);
  (*env)->DeleteGlobalRef(env, CLS_GSSNameElement);
  (*env)->DeleteGlobalRef(env, CLS_GSSCredElement);
  (*env)->DeleteGlobalRef(env, CLS_SunNativeProvider);
  return;
}

const OM_uint32 JAVA_MAX = GSS_C_INDEFINITE/2;

/*
 * Utility routine for converting the C unsigned integer time
 * to Java signed integer time.
 */
jint getJavaTime(OM_uint32 ctime) {
  jint result;

  /* special handle values equals or more than JAVA_MAX */
  if (ctime == GSS_C_INDEFINITE) {
    result = JAVA_MAX;
  } else if (ctime >= JAVA_MAX) {
    result = JAVA_MAX-1;
  } else {
    result = ctime;
  }
  return result;
}
/*
 * Utility routine for converting the Java signed integer time
 * to C unsigned integer time.
 */
OM_uint32 getGSSTime(jint jtime) {
  OM_uint32 result;

  /* special handle values equal to JAVA_MAX */
  if (jtime == (jint)JAVA_MAX) {
    result = GSS_C_INDEFINITE;
  } else {
    result = jtime;
  }
  return result;
}
/*
 * Utility routine for mapping the C error code to the
 * Java one. The routine errors really should have
 * shared the same values but unfortunately don't.
 */
jint getJavaErrorCode(int cNonCallingErr) {
  int cRoutineErr, cSuppStatus;
  /* map the routine errors */
  cRoutineErr = GSS_ROUTINE_ERROR(cNonCallingErr) >> 16;
  if (cRoutineErr != GSS_S_COMPLETE) {
    return JAVA_ERROR_CODE[cRoutineErr-1];
  }
  /* map the supplementary infos */
  cSuppStatus = GSS_SUPPLEMENTARY_INFO(cNonCallingErr);
  if (cSuppStatus & GSS_S_DUPLICATE_TOKEN) {
    return JAVA_DUPLICATE_TOKEN_CODE;
  } else if (cSuppStatus & GSS_S_OLD_TOKEN) {
    return JAVA_OLD_TOKEN_CODE;
  } else if (cSuppStatus & GSS_S_UNSEQ_TOKEN) {
    return JAVA_UNSEQ_TOKEN_CODE;
  } else if (cSuppStatus & GSS_S_GAP_TOKEN) {
    return JAVA_GAP_TOKEN_CODE;
  }
  return GSS_S_COMPLETE;
}


/* Throws a Java Exception by name */
void throwByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);

    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
}

void throwOutOfMemoryError(JNIEnv *env, const char *message) {
    throwByName(env, "java/lang/OutOfMemoryError", message);
}

/*
 * Utility routine for creating a java.lang.String object
 * using the specified gss_buffer_t structure. The specified
 * gss_buffer_t structure is always released.
 */
jstring getJavaString(JNIEnv *env, gss_buffer_t bytes) {
  jstring result = NULL;
  OM_uint32 minor;
  int len;
  jbyteArray jbytes;

  if (bytes != NULL) {
    /* constructs the String object with new String(byte[])
       NOTE: do NOT include the trailing NULL */
    len = (int) bytes->length;
    jbytes = (*env)->NewByteArray(env, len);
    if (jbytes == NULL) {
      goto finish;
    }

    (*env)->SetByteArrayRegion(env, jbytes, 0, len, (jbyte *) bytes->value);
    if ((*env)->ExceptionCheck(env)) {
      goto finish;
    }

    result = (*env)->NewObject(env, CLS_String, MID_String_ctor,
                               jbytes);
  finish:
    (*env)->DeleteLocalRef(env, jbytes);
    (*ftab->releaseBuffer)(&minor, bytes);
    return result;
  } /* else fall through */
  return NULL;
}
/*
 * Utility routine for generate message for the specified minor
 * status code.
 */
jstring getMinorMessage(JNIEnv *env, jobject jstub, OM_uint32 statusValue) {
  OM_uint32 messageContext, minor, major;
  gss_buffer_desc statusString;
  gss_OID mech;

  messageContext = 0;
  if (jstub != NULL) {
    mech = (gss_OID) jlong_to_ptr((*env)->GetLongField(env, jstub, FID_GSSLibStub_pMech));
  } else {
    mech = GSS_C_NO_OID;
  }

  /* gss_display_status(...) => GSS_S_BAD_MECH, GSS_S_BAD_STATUS */
  // TBD: check messageContext value and repeat the call if necessary
  major = (*ftab->displayStatus)(&minor, statusValue, GSS_C_MECH_CODE, mech,
                                 &messageContext, &statusString);

  return getJavaString(env, &statusString);
}

/*
 * Utility routine checking the specified major and minor
 * status codes. GSSExceptions will be thrown if they are
 * not GSS_S_COMPLETE (i.e. 0).
 */
void checkStatus(JNIEnv *env, jobject jstub, OM_uint32 major,
                 OM_uint32 minor, char* methodName) {
  int callingErr, routineErr, supplementaryInfo;
  jint jmajor, jminor;
  char* msg;
  jstring jmsg;
  jthrowable gssEx;

  if (major == GSS_S_COMPLETE) return;

  callingErr = GSS_CALLING_ERROR(major);
  routineErr = GSS_ROUTINE_ERROR(major);
  supplementaryInfo = GSS_SUPPLEMENTARY_INFO(major);

  TRACE3("%s Status major/minor = %x/%d", methodName, major, minor);
  TRACE3("c/r/s = %d/%d/%d ", callingErr>>24, routineErr>>16,
          supplementaryInfo);

  jmajor = getJavaErrorCode(routineErr | supplementaryInfo);
  jminor = minor;
  if (jmajor != GSS_S_COMPLETE) {
    jmsg = NULL;
    if (minor != 0) {
      jmsg = getMinorMessage(env, jstub, minor);
      if ((*env)->ExceptionCheck(env)) {
        return;
      }
    }

    gssEx = (*env)->NewObject(env, CLS_GSSException,
                              MID_GSSException_ctor3,
                              jmajor, jminor, jmsg);
    if (gssEx != NULL) {
      (*env)->Throw(env, gssEx);
    }
  } else {
    /* Error in calling the GSS api */
    if (callingErr == GSS_S_CALL_INACCESSIBLE_READ) {
      msg = "A required input parameter cannot be read";
    } else if (callingErr == GSS_S_CALL_INACCESSIBLE_WRITE) {
      msg = "A required output parameter cannot be write";
    } else {
      msg = "A parameter was malformed";
    }
    jmajor = 13; /* use GSSException.FAILURE for now */
    jmsg = (*env)->NewStringUTF(env, msg);
    if (jmsg == NULL) {
      return;
    }
    gssEx = (*env)->NewObject(env, CLS_GSSException,
                              MID_GSSException_ctor3,
                              jmajor, jminor, jmsg);
    if (gssEx != NULL) {
      (*env)->Throw(env, gssEx);
    }
  }
}

/*
 * Utility routine for initializing gss_buffer_t structure
 * with the byte[] in the specified jbyteArray object.
 * NOTE: must call resetGSSBuffer() to free up the resources
 * inside the gss_buffer_t structure.
 */
void initGSSBuffer(JNIEnv *env, jbyteArray jbytes,
                     gss_buffer_t cbytes) {

  int len;
  void* value;

  if (jbytes != NULL) {
    len = (*env)->GetArrayLength(env, jbytes);
    value = malloc(len);
    if (value == NULL) {
      throwOutOfMemoryError(env, NULL);
      return;
    } else {
      (*env)->GetByteArrayRegion(env, jbytes, 0, len, value);
      if ((*env)->ExceptionCheck(env)) {
        free(value);
        return;
      } else {
        cbytes->length = len;
        cbytes->value = value;
      }
    }
  } else {
    cbytes->length = 0;
    cbytes->value = NULL;
  }
}

/*
 * Utility routine for freeing the bytes malloc'ed
 * in initGSSBuffer() method.
 * NOTE: used in conjunction with initGSSBuffer(...).
 */
void resetGSSBuffer(gss_buffer_t cbytes) {
  if ((cbytes != NULL) && (cbytes != GSS_C_NO_BUFFER)) {
    free(cbytes->value);
    cbytes->length = 0;
    cbytes->value = NULL;
  }
}

/*
 * Utility routine for creating a jbyteArray object using
 * the byte[] value in specified gss_buffer_t structure.
 * NOTE: the specified gss_buffer_t structure is always
 * released.
 */
jbyteArray getJavaBuffer(JNIEnv *env, gss_buffer_t cbytes) {
  jbyteArray result = NULL;
  OM_uint32 minor; // don't care, just so it compiles

  if (cbytes != NULL) {
    if ((cbytes != GSS_C_NO_BUFFER) && (cbytes->length != 0)) {
      result = (*env)->NewByteArray(env, (int) cbytes->length);
      if (result == NULL) {
        goto finish;
      }
      (*env)->SetByteArrayRegion(env, result, 0, (int) cbytes->length,
                                 cbytes->value);
      if ((*env)->ExceptionCheck(env)) {
        result = NULL;
      }
    }
  finish:
    (*ftab->releaseBuffer)(&minor, cbytes);
    return result;
  }
  return NULL;
}

/*
 * Utility routine for creating a non-mech gss_OID using
 * the specified org.ietf.jgss.Oid object.
 * NOTE: must call deleteGSSOID(...) to free up the gss_OID.
 */
gss_OID newGSSOID(JNIEnv *env, jobject jOid) {
  jbyteArray jbytes;
  gss_OID cOid;
  if (jOid != NULL) {
    jbytes = (*env)->CallObjectMethod(env, jOid, MID_Oid_getDER);
    if ((*env)->ExceptionCheck(env)) {
      return GSS_C_NO_OID;
    }
    cOid = malloc(sizeof(struct gss_OID_desc_struct));
    if (cOid == NULL) {
      throwOutOfMemoryError(env,NULL);
      return GSS_C_NO_OID;
    }
    cOid->length = (*env)->GetArrayLength(env, jbytes) - 2;
    cOid->elements = malloc(cOid->length);
    if (cOid->elements == NULL) {
      throwOutOfMemoryError(env,NULL);
      goto cleanup;
    }
    (*env)->GetByteArrayRegion(env, jbytes, 2, cOid->length,
                               cOid->elements);
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
    return cOid;
  } else {
    return GSS_C_NO_OID;
  }
  cleanup:
    (*env)->DeleteLocalRef(env, jbytes);
    free(cOid->elements);
    free(cOid);
    return GSS_C_NO_OID;
}

/*
 * Utility routine for releasing the specified gss_OID
 * structure.
 * NOTE: used in conjunction with newGSSOID(...).
 */
void deleteGSSOID(gss_OID oid) {
  if (oid != GSS_C_NO_OID) {
    free(oid->elements);
    free(oid);
  }
}

/*
 * Utility routine for creating a org.ietf.jgss.Oid
 * object using the specified gss_OID structure.
 */
jobject getJavaOID(JNIEnv *env, gss_OID cOid) {
  int cLen;
  char oidHdr[2];
  jbyteArray jbytes;
  jobject result = NULL;

  if ((cOid == NULL) || (cOid == GSS_C_NO_OID)) {
    return NULL;
  }
  cLen = cOid->length;
  oidHdr[0] = 6;
  oidHdr[1] = cLen;
  jbytes = (*env)->NewByteArray(env, cLen+2);
  if (jbytes == NULL) {
    return NULL;
  }
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->SetByteArrayRegion(env, jbytes, 0, 2, (jbyte *) oidHdr);
  }
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->SetByteArrayRegion(env, jbytes, 2, cLen, (jbyte *) cOid->elements);
  }
  if (!(*env)->ExceptionCheck(env)) {
    result = (*env)->NewObject(env, CLS_Oid, MID_Oid_ctor1, jbytes);
  }
  (*env)->DeleteLocalRef(env, jbytes);
  return result;
}
/*
 * Utility routine for creating a gss_OID_set structure
 * using the specified gss_OID.
 * NOTE: need to call deleteGSSOIDSet(...) afterwards
 * to release the created gss_OID_set structure.
 */
gss_OID_set newGSSOIDSet(gss_OID oid) {
  gss_OID_set oidSet;
  OM_uint32 minor; // don't care; just so it compiles

  if (oid->length != 6 ||
      memcmp(oid->elements, SPNEGO_BYTES, 6) != 0) {
      (*ftab->createEmptyOidSet)(&minor, &oidSet);
      (*ftab->addOidSetMember)(&minor, oid, &oidSet);
      return oidSet;
  } else {
      // Use all mechs for SPNEGO in order to work with
      // various native GSS impls
      return (ftab->mechs);
  }
}
/*
 * Utility routine for releasing a gss_OID_set structure.
 * NOTE: used in conjunction with newGSSOIDSet(...).
 */
void deleteGSSOIDSet(gss_OID_set oidSet) {
  OM_uint32 minor; /* don't care; just so it compiles */

  if ((oidSet != ftab->mechs) &&
      (oidSet != NULL) && (oidSet != GSS_C_NO_OID_SET)) {
    (*ftab->releaseOidSet)(&minor, &oidSet);
  }
}
/*
 * Utility routine for creating a org.ietf.jgss.Oid[]
 * using the specified gss_OID_set structure.
 */
jobjectArray getJavaOIDArray(JNIEnv *env, gss_OID_set cOidSet) {
  int numOfOids = 0;
  jobjectArray jOidSet;
  jobject jOid;
  int i;

  if (cOidSet != NULL && cOidSet != GSS_C_NO_OID_SET) {
    numOfOids = (int) cOidSet->count;
    jOidSet = (*env)->NewObjectArray(env, numOfOids, CLS_Oid, NULL);
    if ((*env)->ExceptionCheck(env)) {
      return NULL;
    }
    for (i = 0; i < numOfOids; i++) {
      jOid = getJavaOID(env, &(cOidSet->elements[i]));
      if ((*env)->ExceptionCheck(env)) {
        return NULL;
      }
      (*env)->SetObjectArrayElement(env, jOidSet, i, jOid);
      if ((*env)->ExceptionCheck(env)) {
        return NULL;
      }
      (*env)->DeleteLocalRef(env, jOid);
    }
    return jOidSet;
  }
  return NULL;
}

int sameMech(gss_OID mech, gss_OID mech2) {
  int result = JNI_FALSE; // default to not equal

  if (mech->length == mech2->length) {
    result = (memcmp(mech->elements, mech2->elements, mech->length) == 0);
  }
  return result;
}
