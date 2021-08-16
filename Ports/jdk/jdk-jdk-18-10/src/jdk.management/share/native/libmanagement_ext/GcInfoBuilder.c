/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include "management_ext.h"
#include "com_sun_management_internal_GcInfoBuilder.h"

JNIEXPORT jint JNICALL Java_com_sun_management_internal_GcInfoBuilder_getNumGcExtAttributes
  (JNIEnv *env, jobject dummy, jobject gc) {
    jlong value;

    if (gc == NULL) {
        JNU_ThrowNullPointerException(env, "Invalid GarbageCollectorMXBean");
        return 0;
    }
    value = jmm_interface->GetLongAttribute(env, gc,
                                            JMM_GC_EXT_ATTRIBUTE_INFO_SIZE);
    return (jint) value;
}

JNIEXPORT void JNICALL Java_com_sun_management_internal_GcInfoBuilder_fillGcAttributeInfo
  (JNIEnv *env, jobject dummy, jobject gc,
   jint num_attributes, jobjectArray attributeNames,
   jcharArray types, jobjectArray descriptions) {

    jmmExtAttributeInfo* ext_att_info;
    jchar* nativeTypes;
    jstring attName = NULL;
    jstring desc = NULL;
    jint ret = 0;
    jint i;

    if (gc == NULL) {
        JNU_ThrowNullPointerException(env, "Invalid GarbageCollectorMXBean");
        return;
    }

    if (num_attributes <= 0) {
        JNU_ThrowIllegalArgumentException(env, "Invalid num_attributes");
        return;
    }

    ext_att_info = (jmmExtAttributeInfo*) malloc((size_t)num_attributes *
                                                 sizeof(jmmExtAttributeInfo));
    if (ext_att_info == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return;
    }
    ret = jmm_interface->GetGCExtAttributeInfo(env, gc,
                                               ext_att_info, num_attributes);
    if (ret != num_attributes) {
        JNU_ThrowInternalError(env, "Unexpected num_attributes");
        free(ext_att_info);
        return;
    }

    nativeTypes = (jchar*) malloc((size_t)num_attributes * sizeof(jchar));
    if (nativeTypes == NULL) {
        free(ext_att_info);
        JNU_ThrowOutOfMemoryError(env, 0);
        return;
    }
    for (i = 0; i < num_attributes; i++) {
        nativeTypes[i] = ext_att_info[i].type;
        attName = (*env)->NewStringUTF(env, ext_att_info[i].name);
        if ((*env)->ExceptionCheck(env)) {
           free(ext_att_info);
           free(nativeTypes);
           return;
        }

        (*env)->SetObjectArrayElement(env, attributeNames, i, attName);
        if ((*env)->ExceptionCheck(env)) {
           free(ext_att_info);
           free(nativeTypes);
           return;
        }

        desc = (*env)->NewStringUTF(env, ext_att_info[i].description);
        if ((*env)->ExceptionCheck(env)) {
           free(ext_att_info);
           free(nativeTypes);
           return;
        }

        (*env)->SetObjectArrayElement(env, descriptions, i, desc);
        if ((*env)->ExceptionCheck(env)) {
           free(ext_att_info);
           free(nativeTypes);
           return;
        }
    }
    (*env)->SetCharArrayRegion(env, types, 0, num_attributes, nativeTypes);

    if (ext_att_info != NULL) {
        free(ext_att_info);
    }
    if (nativeTypes != NULL) {
        free(nativeTypes);
    }
}

static void setLongValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                      jsize index, jlong value) {
    static const char* class_name = "java/lang/Long";
    static const char* signature = "(J)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setBooleanValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                         jsize index, jboolean value) {
    static const char* class_name = "java/lang/Boolean";
    static const char* signature = "(Z)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setByteValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                      jsize index, jbyte value) {
    static const char* class_name = "java/lang/Byte";
    static const char* signature = "(B)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setIntValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                     jsize index, jint value) {
    static const char* class_name = "java/lang/Integer";
    static const char* signature = "(I)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setShortValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                       jsize index, jshort value) {
    static const char* class_name = "java/lang/Short";
    static const char* signature = "(S)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setDoubleValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                        jsize index, jdouble value) {
    static const char* class_name = "java/lang/Double";
    static const char* signature = "(D)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setFloatValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                       jsize index, jfloat value) {
    static const char* class_name = "java/lang/Float";
    static const char* signature = "(D)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

static void setCharValueAtObjectArray(JNIEnv *env, jobjectArray array,
                                      jsize index, jchar value) {
    static const char* class_name = "java/lang/Character";
    static const char* signature = "(C)V";
    jobject obj = JNU_NewObjectByName(env, class_name, signature, value);

    (*env)->SetObjectArrayElement(env, array, index, obj);
}

JNIEXPORT jobject JNICALL Java_com_sun_management_internal_GcInfoBuilder_getLastGcInfo0
  (JNIEnv *env, jobject builder, jobject gc,
   jint ext_att_count, jobjectArray ext_att_values, jcharArray ext_att_types,
   jobjectArray usageBeforeGC, jobjectArray usageAfterGC) {

    jmmGCStat   gc_stat;
    jchar*      nativeTypes;
    jsize       i;
    jvalue      v;

    if (gc == NULL) {
        JNU_ThrowNullPointerException(env, "Invalid GarbageCollectorMXBean");
        return 0;
    }

    if (ext_att_count <= 0) {
        JNU_ThrowIllegalArgumentException(env, "Invalid ext_att_count");
        return 0;
    }

    gc_stat.usage_before_gc = usageBeforeGC;
    gc_stat.usage_after_gc = usageAfterGC;
    gc_stat.gc_ext_attribute_values_size = ext_att_count;
    if (ext_att_count > 0) {
        gc_stat.gc_ext_attribute_values = (jvalue*) malloc((size_t)ext_att_count *
                                                           sizeof(jvalue));
        if (gc_stat.gc_ext_attribute_values == NULL) {
            JNU_ThrowOutOfMemoryError(env, 0);
            return 0;
        }
    } else {
        gc_stat.gc_ext_attribute_values = NULL;
    }


    jmm_interface->GetLastGCStat(env, gc, &gc_stat);
    if (gc_stat.gc_index == 0) {
        if (gc_stat.gc_ext_attribute_values != NULL) {
            free(gc_stat.gc_ext_attribute_values);
        }
        return 0;
    }

    // convert the ext_att_types to native types
    nativeTypes = (jchar*) malloc((size_t)ext_att_count * sizeof(jchar));
    if (nativeTypes == NULL) {
        if (gc_stat.gc_ext_attribute_values != NULL) {
            free(gc_stat.gc_ext_attribute_values);
        }
        JNU_ThrowOutOfMemoryError(env, 0);
        return 0;
    }
    (*env)->GetCharArrayRegion(env, ext_att_types, 0, ext_att_count, nativeTypes);
    for (i = 0; i < ext_att_count; i++) {
       v = gc_stat.gc_ext_attribute_values[i];
       switch (nativeTypes[i]) {
            case 'Z':
                setBooleanValueAtObjectArray(env, ext_att_values, i, v.z);
                break;
            case 'B':
                setByteValueAtObjectArray(env, ext_att_values, i, v.b);
                break;
            case 'C':
                setCharValueAtObjectArray(env, ext_att_values, i, v.c);
                break;
            case 'S':
                setShortValueAtObjectArray(env, ext_att_values, i, v.s);
                break;
            case 'I':
                setIntValueAtObjectArray(env, ext_att_values, i, v.i);
                break;
            case 'J':
                setLongValueAtObjectArray(env, ext_att_values, i, v.j);
                break;
            case 'F':
                setFloatValueAtObjectArray(env, ext_att_values, i, v.f);
                break;
            case 'D':
                setDoubleValueAtObjectArray(env, ext_att_values, i, v.d);
                break;
            default:
                if (gc_stat.gc_ext_attribute_values != NULL) {
                    free(gc_stat.gc_ext_attribute_values);
                }
                if (nativeTypes != NULL) {
                    free(nativeTypes);
                }
                JNU_ThrowInternalError(env, "Unsupported attribute type");
                return 0;
       }
    }
    if (gc_stat.gc_ext_attribute_values != NULL) {
        free(gc_stat.gc_ext_attribute_values);
    }
    if (nativeTypes != NULL) {
        free(nativeTypes);
    }

    return JNU_NewObjectByName(env,
       "com/sun/management/GcInfo",
       "(Lcom/sun/management/internal/GcInfoBuilder;JJJ[Ljava/lang/management/MemoryUsage;[Ljava/lang/management/MemoryUsage;[Ljava/lang/Object;)V",
       builder,
       gc_stat.gc_index,
       gc_stat.start_time,
       gc_stat.end_time,
       usageBeforeGC,
       usageAfterGC,
       ext_att_values);
}
