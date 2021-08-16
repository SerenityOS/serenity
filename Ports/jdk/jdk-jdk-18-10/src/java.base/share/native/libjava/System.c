/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "java_props.h"

#include "java_lang_System.h"
#include "jdk_internal_util_SystemProps_Raw.h"

#define OBJ "Ljava/lang/Object;"

/* Only register the performance-critical methods */
static JNINativeMethod methods[] = {
    {"currentTimeMillis", "()J",              (void *)&JVM_CurrentTimeMillis},
    {"nanoTime",          "()J",              (void *)&JVM_NanoTime},
    {"arraycopy",     "(" OBJ "I" OBJ "II)V", (void *)&JVM_ArrayCopy},
};

#undef OBJ

JNIEXPORT void JNICALL
Java_java_lang_System_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls,
                            methods, sizeof(methods)/sizeof(methods[0]));
}

JNIEXPORT jint JNICALL
Java_java_lang_System_identityHashCode(JNIEnv *env, jobject this, jobject x)
{
    return JVM_IHashCode(env, x);
}

/* VENDOR, VENDOR_URL, VENDOR_URL_BUG are set in VersionProps.java.template. */

/*
 * Store the UTF-8 string encoding of the value in the array
 * at the index if the value is non-null.  Store nothing if the value is null.
 * On any error, return from Java_jdk_internal_util_SystemProps_00024Raw_platformProperties.
 */
#define PUTPROP(array, prop_index, val)                    \
    if (val != NULL) {                                     \
        jstring jval = (*env)->NewStringUTF(env, val);     \
        if (jval == NULL)                                  \
            return NULL;                                   \
        (*env)->SetObjectArrayElement(env, array, jdk_internal_util_SystemProps_Raw_##prop_index, jval); \
        if ((*env)->ExceptionOccurred(env))                \
            return NULL;                                   \
        (*env)->DeleteLocalRef(env, jval);                 \
    }

/*
 * Store the Platform string encoding of the value in the array
 * at the index if the value is non-null.  Store nothing if the value is null.
 * On any error, return from Java_jdk_internal_util_SystemProps_00024Raw_platformProperties.
 */
#define PUTPROP_PlatformString(array, prop_index, val)     \
    if (val != NULL) {                                     \
        jstring jval = GetStringPlatform(env, val);        \
        if (jval == NULL)                                  \
            return NULL;                                   \
        (*env)->SetObjectArrayElement(env, array, jdk_internal_util_SystemProps_Raw_##prop_index, jval); \
        if ((*env)->ExceptionOccurred(env))                \
            return NULL;                                   \
        (*env)->DeleteLocalRef(env, jval);                 \
    }

/*
 * Gather the system properties and return as a String[].
 * The first FIXED_LENGTH entries are the platform defined property values, no names.
 * The remaining array indices are alternating key/value pairs
 * supplied by the VM including those defined on the command line
 * using -Dkey=value that may override the platform defined value.
 * The caller is responsible for replacing platform provided values as needed.
 *
 * Class:     jdk_internal_util_SystemProps_Raw
 * Method:    platformProperties
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_jdk_internal_util_SystemProps_00024Raw_platformProperties(JNIEnv *env, jclass cla)
{
    java_props_t *sprops;
    jobject propArray = NULL;
    jclass classString;
    int nstrings = jdk_internal_util_SystemProps_Raw_FIXED_LENGTH;

    // Get the platform specific values
    sprops = GetJavaProperties(env);
    CHECK_NULL_RETURN(sprops, NULL);

    /*
     * !!! DO NOT call PUTPROP_PlatformString (NewStringPlatform) before this line !!!
     */
    InitializeEncoding(env, sprops->sun_jnu_encoding);

    // Ensure capacity for the array and for a string for each fixed length element
    if ((*env)->EnsureLocalCapacity(env, nstrings + 2) < 0) {
        return NULL;
    }

    // Allocate an array of String for all the well known props
    classString = JNU_ClassString(env);
    CHECK_NULL_RETURN(classString, NULL);

    propArray = (*env)->NewObjectArray(env, nstrings, classString, NULL);
    CHECK_NULL_RETURN(propArray, NULL);

    /* os properties */
    PUTPROP(propArray, _os_name_NDX, sprops->os_name);
    PUTPROP(propArray, _os_version_NDX, sprops->os_version);
    PUTPROP(propArray, _os_arch_NDX, sprops->os_arch);

#ifdef JDK_ARCH_ABI_PROP_NAME
    PUTPROP(propArray, _sun_arch_abi_NDX, sprops->sun_arch_abi);
#endif

    /* file system properties */
    PUTPROP(propArray, _file_separator_NDX, sprops->file_separator);
    PUTPROP(propArray, _path_separator_NDX, sprops->path_separator);
    PUTPROP(propArray, _line_separator_NDX, sprops->line_separator);

    PUTPROP(propArray, _file_encoding_NDX, sprops->encoding);
    PUTPROP(propArray, _sun_jnu_encoding_NDX, sprops->sun_jnu_encoding);

    /*
     * file encoding for stdout and stderr
     */
    PUTPROP(propArray, _sun_stdout_encoding_NDX, sprops->sun_stdout_encoding);
    PUTPROP(propArray, _sun_stderr_encoding_NDX, sprops->sun_stderr_encoding);

    /* unicode_encoding specifies the default endianness */
    PUTPROP(propArray, _sun_io_unicode_encoding_NDX, sprops->unicode_encoding);
    PUTPROP(propArray, _sun_cpu_endian_NDX, sprops->cpu_endian);
    PUTPROP(propArray, _sun_cpu_isalist_NDX, sprops->cpu_isalist);

#ifdef MACOSX
    /* Proxy setting properties */
    if (sprops->httpProxyEnabled) {
        PUTPROP(propArray, _http_proxyHost_NDX, sprops->httpHost);
        PUTPROP(propArray, _http_proxyPort_NDX, sprops->httpPort);
    }

    if (sprops->httpsProxyEnabled) {
        PUTPROP(propArray, _https_proxyHost_NDX, sprops->httpsHost);
        PUTPROP(propArray, _https_proxyPort_NDX, sprops->httpsPort);
    }

    if (sprops->ftpProxyEnabled) {
        PUTPROP(propArray, _ftp_proxyHost_NDX, sprops->ftpHost);
        PUTPROP(propArray, _ftp_proxyPort_NDX, sprops->ftpPort);
    }

    if (sprops->socksProxyEnabled) {
        PUTPROP(propArray, _socksProxyHost_NDX, sprops->socksHost);
        PUTPROP(propArray, _socksProxyPort_NDX, sprops->socksPort);
    }

    // Mac OS X only has a single proxy exception list which applies
    // to all protocols
    if (sprops->exceptionList) {
        PUTPROP(propArray, _http_nonProxyHosts_NDX, sprops->exceptionList);
        PUTPROP(propArray, _ftp_nonProxyHosts_NDX, sprops->exceptionList);
        PUTPROP(propArray, _socksNonProxyHosts_NDX, sprops->exceptionList);
    }
#endif

    /* data model */
    if (sizeof(sprops) == 4) {
        sprops->data_model = "32";
    } else if (sizeof(sprops) == 8) {
        sprops->data_model = "64";
    } else {
        sprops->data_model = "unknown";
    }
    PUTPROP(propArray, _sun_arch_data_model_NDX, sprops->data_model);

    /* patch level */
    PUTPROP(propArray, _sun_os_patch_level_NDX, sprops->patch_level);

    PUTPROP_PlatformString(propArray, _java_io_tmpdir_NDX, sprops->tmp_dir);

    PUTPROP_PlatformString(propArray, _user_name_NDX, sprops->user_name);
    PUTPROP_PlatformString(propArray, _user_home_NDX, sprops->user_home);
    PUTPROP_PlatformString(propArray, _user_dir_NDX, sprops->user_dir);

   /*
    * Set i18n related property fields from platform.
    */
   PUTPROP(propArray, _display_language_NDX, sprops->display_language);
   PUTPROP(propArray, _display_script_NDX, sprops->display_script);
   PUTPROP(propArray, _display_country_NDX, sprops->display_country);
   PUTPROP(propArray, _display_variant_NDX, sprops->display_variant);

   PUTPROP(propArray, _format_language_NDX, sprops->format_language);
   PUTPROP(propArray, _format_script_NDX, sprops->format_script);
   PUTPROP(propArray, _format_country_NDX, sprops->format_country);
   PUTPROP(propArray, _format_variant_NDX, sprops->format_variant);

   return propArray;
}

/*
 * Gather the VM and command line properties and return as a String[].
 * The array indices are alternating key/value pairs
 * supplied by the VM including those defined on the command line
 * using -Dkey=value that may override the platform defined value.
 *
 * Note: The platform encoding must have been set.
 *
 * Class:     jdk_internal_util_SystemProps_Raw
 * Method:    vmProperties
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_jdk_internal_util_SystemProps_00024Raw_vmProperties(JNIEnv *env, jclass cla)
{
    jobjectArray cmdProps = JVM_GetProperties(env);
    return cmdProps;
}

/*
 * The following three functions implement setter methods for
 * java.lang.System.{in, out, err}. They are natively implemented
 * because they violate the semantics of the language (i.e. set final
 * variable).
 */
JNIEXPORT void JNICALL
Java_java_lang_System_setIn0(JNIEnv *env, jclass cla, jobject stream)
{
    jfieldID fid =
        (*env)->GetStaticFieldID(env,cla,"in","Ljava/io/InputStream;");
    if (fid == 0)
        return;
    (*env)->SetStaticObjectField(env,cla,fid,stream);
}

JNIEXPORT void JNICALL
Java_java_lang_System_setOut0(JNIEnv *env, jclass cla, jobject stream)
{
    jfieldID fid =
        (*env)->GetStaticFieldID(env,cla,"out","Ljava/io/PrintStream;");
    if (fid == 0)
        return;
    (*env)->SetStaticObjectField(env,cla,fid,stream);
}

JNIEXPORT void JNICALL
Java_java_lang_System_setErr0(JNIEnv *env, jclass cla, jobject stream)
{
    jfieldID fid =
        (*env)->GetStaticFieldID(env,cla,"err","Ljava/io/PrintStream;");
    if (fid == 0)
        return;
    (*env)->SetStaticObjectField(env,cla,fid,stream);
}

static void cpchars(jchar *dst, char *src, int n)
{
    int i;
    for (i = 0; i < n; i++) {
        dst[i] = src[i];
    }
}

JNIEXPORT jstring JNICALL
Java_java_lang_System_mapLibraryName(JNIEnv *env, jclass ign, jstring libname)
{
    int len;
    int prefix_len = (int) strlen(JNI_LIB_PREFIX);
    int suffix_len = (int) strlen(JNI_LIB_SUFFIX);

    jchar chars[256];
    if (libname == NULL) {
        JNU_ThrowNullPointerException(env, 0);
        return NULL;
    }
    len = (*env)->GetStringLength(env, libname);
    if (len > 240) {
        JNU_ThrowIllegalArgumentException(env, "name too long");
        return NULL;
    }
    cpchars(chars, JNI_LIB_PREFIX, prefix_len);
    (*env)->GetStringRegion(env, libname, 0, len, chars + prefix_len);
    len += prefix_len;
    cpchars(chars + len, JNI_LIB_SUFFIX, suffix_len);
    len += suffix_len;

    return (*env)->NewString(env, chars, len);
}
