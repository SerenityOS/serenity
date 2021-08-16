/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

//#define USE_TRACE
#define USE_ERROR


#include <jni.h>
#include <jni_util.h>
#include "SoundDefs.h"
#include "Ports.h"
#include "Utilities.h"
#include "com_sun_media_sound_PortMixer.h"


//////////////////////////////////////////// PortMixer ////////////////////////////////////////////

JNIEXPORT jlong JNICALL Java_com_sun_media_sound_PortMixer_nOpen
  (JNIEnv *env, jclass cls, jint mixerIndex) {

    jlong ret = 0;
#if USE_PORTS == TRUE
    ret = (jlong) (INT_PTR) PORT_Open(mixerIndex);
#endif
    return ret;
}

JNIEXPORT void JNICALL Java_com_sun_media_sound_PortMixer_nClose
  (JNIEnv *env, jclass cls, jlong id) {

#if USE_PORTS == TRUE
    if (id != 0) {
        PORT_Close((void*) (INT_PTR) id);
    }
#endif
}

JNIEXPORT jint JNICALL Java_com_sun_media_sound_PortMixer_nGetPortCount
  (JNIEnv *env, jclass cls, jlong id) {

    jint ret = 0;
#if USE_PORTS == TRUE
    if (id != 0) {
        ret = (jint) PORT_GetPortCount((void*) (INT_PTR) id);
    }
#endif
    return ret;
}


JNIEXPORT jint JNICALL Java_com_sun_media_sound_PortMixer_nGetPortType
  (JNIEnv *env, jclass cls, jlong id, jint portIndex) {

    jint ret = 0;
    TRACE1("Java_com_sun_media_sound_PortMixer_nGetPortType(%d).\n", portIndex);

#if USE_PORTS == TRUE
    if (id != 0) {
        ret = (jint) PORT_GetPortType((void*) (INT_PTR) id, portIndex);
    }
#endif

    TRACE1("Java_com_sun_media_sound_PortMixerProvider_nGetPortType returning %d.\n", ret);
    return ret;
}

JNIEXPORT jstring JNICALL Java_com_sun_media_sound_PortMixer_nGetPortName
  (JNIEnv *env, jclass cls, jlong id, jint portIndex) {

    char str[PORT_STRING_LENGTH];
    jstring jString = NULL;
    TRACE1("Java_com_sun_media_sound_PortMixer_nGetPortName(%d).\n", portIndex);

    str[0] = 0;
#if USE_PORTS == TRUE
    if (id != 0) {
        PORT_GetPortName((void*) (INT_PTR) id, portIndex, str, PORT_STRING_LENGTH);
    }
#endif
    jString = (*env)->NewStringUTF(env, str);

    TRACE1("Java_com_sun_media_sound_PortMixerProvider_nGetName returning \"%s\".\n", str);
    return jString;
}

JNIEXPORT void JNICALL Java_com_sun_media_sound_PortMixer_nControlSetIntValue
  (JNIEnv *env, jclass cls, jlong controlID, jint value) {
#if USE_PORTS == TRUE
    if (controlID != 0) {
        PORT_SetIntValue((void*) (UINT_PTR) controlID, (INT32) value);
    }
#endif
}

JNIEXPORT jint JNICALL Java_com_sun_media_sound_PortMixer_nControlGetIntValue
  (JNIEnv *env, jclass cls, jlong controlID) {
    INT32 ret = 0;
#if USE_PORTS == TRUE
    if (controlID != 0) {
        ret = PORT_GetIntValue((void*) (UINT_PTR) controlID);
    }
#endif
    return (jint) ret;
}

JNIEXPORT void JNICALL Java_com_sun_media_sound_PortMixer_nControlSetFloatValue
  (JNIEnv *env, jclass cls, jlong controlID, jfloat value) {
#if USE_PORTS == TRUE
    if (controlID != 0) {
        PORT_SetFloatValue((void*) (UINT_PTR) controlID, (float) value);
    }
#endif
}

JNIEXPORT jfloat JNICALL Java_com_sun_media_sound_PortMixer_nControlGetFloatValue
  (JNIEnv *env, jclass cls, jlong controlID) {
    float ret = 0;
#if USE_PORTS == TRUE
    if (controlID != 0) {
        ret = PORT_GetFloatValue((void*) (UINT_PTR) controlID);
    }
#endif
    return (jfloat) ret;
}

/* ************************************** native control creation support ********************* */

// contains all the needed references so that the platform dependent code can call JNI wrapper functions
typedef struct tag_ControlCreatorJNI {
    // this member is seen by the platform dependent code
    PortControlCreator creator;
    // general JNI variables
    JNIEnv *env;
    // the vector to be filled with controls (initialized before usage)
    jobject vector;
    jmethodID vectorAddElement;
    // control specific constructors (initialized on demand)
    jclass boolCtrlClass;
    jmethodID boolCtrlConstructor;   // signature (JLjava/lang/String;)V
    jclass controlClass;             // class of javax.sound.sampled.Control
    jclass compCtrlClass;
    jmethodID compCtrlConstructor;   // signature (Ljava/lang/String;[Ljavax/sound/sampled/Control;)V
    jclass floatCtrlClass;
    jmethodID floatCtrlConstructor1; // signature (JLjava/lang/String;FFFLjava/lang/String;)V
    jmethodID floatCtrlConstructor2; // signature (JIFFFLjava/lang/String;)V
} ControlCreatorJNI;


void* PORT_NewBooleanControl(void* creatorV, void* controlID, char* type) {
    ControlCreatorJNI* creator = (ControlCreatorJNI*) creatorV;
    jobject ctrl = NULL;
    jstring typeString;

#ifdef USE_TRACE
    if (((UINT_PTR) type) <= CONTROL_TYPE_MAX) {
        TRACE1("PORT_NewBooleanControl: creating '%d'\n", (int) (UINT_PTR) type);
    } else {
        TRACE1("PORT_NewBooleanControl: creating '%s'\n", type);
    }
#endif
    if (!creator->boolCtrlClass) {
        // retrieve class and constructor of PortMixer.BoolCtrl
        creator->boolCtrlClass = (*creator->env)->FindClass(creator->env, IMPLEMENTATION_PACKAGE_NAME"/PortMixer$BoolCtrl");
        if (!creator->boolCtrlClass) {
            ERROR0("PORT_NewBooleanControl: boolCtrlClass is NULL\n");
            return NULL;
        }
        creator->boolCtrlConstructor = (*creator->env)->GetMethodID(creator->env, creator->boolCtrlClass,
                 "<init>", "(JLjava/lang/String;)V");
        if (!creator->boolCtrlConstructor) {
            ERROR0("PORT_NewBooleanControl: boolCtrlConstructor is NULL\n");
            return NULL;
        }
    }
    if (type == CONTROL_TYPE_MUTE) {
        type = "Mute";
    }
    else if (type == CONTROL_TYPE_SELECT) {
        type = "Select";
    }

    typeString = (*creator->env)->NewStringUTF(creator->env, type);
    CHECK_NULL_RETURN(typeString, (void*) ctrl);
    ctrl = (*creator->env)->NewObject(creator->env, creator->boolCtrlClass,
                                      creator->boolCtrlConstructor,
                                      (jlong) (UINT_PTR) controlID, typeString);
    if (!ctrl) {
        ERROR0("PORT_NewBooleanControl: ctrl is NULL\n");
    }
    if ((*creator->env)->ExceptionOccurred(creator->env)) {
        ERROR0("PORT_NewBooleanControl: ExceptionOccurred!\n");
    }
    TRACE0("PORT_NewBooleanControl succeeded\n");
    return (void*) ctrl;
}

void* PORT_NewCompoundControl(void* creatorV, char* type, void** controls, int controlCount) {
    ControlCreatorJNI* creator = (ControlCreatorJNI*) creatorV;
    jobject ctrl = NULL;
    jobjectArray controlArray;
    int i;
    jstring typeString;

    TRACE2("PORT_NewCompoundControl: creating '%s' with %d controls\n", type, controlCount);
    if (!creator->compCtrlClass) {
        TRACE0("PORT_NewCompoundControl: retrieve method ids\n");
        // retrieve class and constructor of PortMixer.BoolCtrl
        creator->compCtrlClass = (*creator->env)->FindClass(creator->env, IMPLEMENTATION_PACKAGE_NAME"/PortMixer$CompCtrl");
        if (!creator->compCtrlClass) {
            ERROR0("PORT_NewCompoundControl: compCtrlClass is NULL\n");
            return NULL;
        }
        creator->compCtrlConstructor = (*creator->env)->GetMethodID(creator->env, creator->compCtrlClass,
                 "<init>", "(Ljava/lang/String;[Ljavax/sound/sampled/Control;)V");
        if (!creator->compCtrlConstructor) {
            ERROR0("PORT_NewCompoundControl: compCtrlConstructor is NULL\n");
            return NULL;
        }
        creator->controlClass = (*creator->env)->FindClass(creator->env, JAVA_SAMPLED_PACKAGE_NAME"/Control");
        if (!creator->controlClass) {
            ERROR0("PORT_NewCompoundControl: controlClass is NULL\n");
            return NULL;
        }
    }
    TRACE0("PORT_NewCompoundControl: creating array\n");
    // create new array for the controls
    controlArray = (*creator->env)->NewObjectArray(creator->env, controlCount, creator->controlClass, (jobject) NULL);
    if (!controlArray) {
        ERROR0("PORT_NewCompoundControl: controlArray is NULL\n");
        return NULL;
    }
    TRACE0("PORT_NewCompoundControl: setting array values\n");
    for (i = 0; i < controlCount; i++) {
        (*creator->env)->SetObjectArrayElement(creator->env, controlArray, i, (jobject) controls[i]);
    }
    TRACE0("PORT_NewCompoundControl: creating compound control\n");
    typeString = (*creator->env)->NewStringUTF(creator->env, type);
    CHECK_NULL_RETURN(typeString, (void*) ctrl);
    ctrl = (*creator->env)->NewObject(creator->env, creator->compCtrlClass,
                                      creator->compCtrlConstructor,
                                      typeString, controlArray);
    if (!ctrl) {
        ERROR0("PORT_NewCompoundControl: ctrl is NULL\n");
    }
    if ((*creator->env)->ExceptionOccurred(creator->env)) {
        ERROR0("PORT_NewCompoundControl: ExceptionOccurred!\n");
    }
    TRACE0("PORT_NewCompoundControl succeeded\n");
    return (void*) ctrl;
}

void* PORT_NewFloatControl(void* creatorV, void* controlID, char* type,
                           float min, float max, float precision, const char* units) {
    ControlCreatorJNI* creator = (ControlCreatorJNI*) creatorV;
    jobject ctrl = NULL;
    jstring unitsString;
    jstring typeString;

#ifdef USE_TRACE
    if (((UINT_PTR) type) <= CONTROL_TYPE_MAX) {
        TRACE1("PORT_NewFloatControl: creating '%d'\n", (int) (UINT_PTR) type);
    } else {
        TRACE1("PORT_NewFloatControl: creating '%s'\n", type);
    }
#endif
    if (!creator->floatCtrlClass) {
        // retrieve class and constructor of PortMixer.BoolCtrl
        creator->floatCtrlClass = (*creator->env)->FindClass(creator->env, IMPLEMENTATION_PACKAGE_NAME"/PortMixer$FloatCtrl");
        if (!creator->floatCtrlClass) {
            ERROR0("PORT_NewFloatControl: floatCtrlClass is NULL\n");
            return NULL;
        }
        creator->floatCtrlConstructor1 = (*creator->env)->GetMethodID(creator->env, creator->floatCtrlClass,
                 "<init>", "(JLjava/lang/String;FFFLjava/lang/String;)V");
        if (!creator->floatCtrlConstructor1) {
            ERROR0("PORT_NewFloatControl: floatCtrlConstructor1 is NULL\n");
            return NULL;
        }
        creator->floatCtrlConstructor2 = (*creator->env)->GetMethodID(creator->env, creator->floatCtrlClass,
                 "<init>", "(JIFFFLjava/lang/String;)V");
        if (!creator->floatCtrlConstructor2) {
            ERROR0("PORT_NewFloatControl: floatCtrlConstructor2 is NULL\n");
            return NULL;
        }
    }
    unitsString = (*creator->env)->NewStringUTF(creator->env, units);
    CHECK_NULL_RETURN(unitsString, (void*) ctrl);
    if (((UINT_PTR) type) <= CONTROL_TYPE_MAX) {
        // constructor with int parameter
        TRACE1("PORT_NewFloatControl: calling constructor2 with type %d\n", (int) (UINT_PTR) type);
        ctrl = (*creator->env)->NewObject(creator->env, creator->floatCtrlClass,
                                          creator->floatCtrlConstructor2,
                                          (jlong) (UINT_PTR) controlID, (jint) (UINT_PTR) type,
                                          min, max, precision, unitsString);
    } else {
        TRACE0("PORT_NewFloatControl: calling constructor1\n");
        // constructor with string parameter
        typeString = (*creator->env)->NewStringUTF(creator->env, type);
        CHECK_NULL_RETURN(typeString, (void*) ctrl);
        ctrl = (*creator->env)->NewObject(creator->env, creator->floatCtrlClass,
                                          creator->floatCtrlConstructor1,
                                          (jlong) (UINT_PTR) controlID, typeString,
                                          min, max, precision, unitsString);
    }
    if (!ctrl) {
        ERROR0("PORT_NewFloatControl: ctrl is NULL!\n");
    }
    if ((*creator->env)->ExceptionOccurred(creator->env)) {
        ERROR0("PORT_NewFloatControl: ExceptionOccurred!\n");
    }
    TRACE1("PORT_NewFloatControl succeeded %p\n", (void*) ctrl);
    return (void*) ctrl;
}

int PORT_AddControl(void* creatorV, void* control) {
    ControlCreatorJNI* creator = (ControlCreatorJNI*) creatorV;

    TRACE1("PORT_AddControl %p\n", (void*) control);
    (*creator->env)->CallVoidMethod(creator->env, creator->vector, creator->vectorAddElement, (jobject) control);
    if ((*creator->env)->ExceptionOccurred(creator->env)) {
        ERROR0("PORT_AddControl: ExceptionOccurred!\n");
    }
    TRACE0("PORT_AddControl succeeded\n");
    return TRUE;
}

JNIEXPORT void JNICALL Java_com_sun_media_sound_PortMixer_nGetControls
  (JNIEnv *env, jclass cls, jlong id, jint portIndex, jobject vector) {

    ControlCreatorJNI creator;
    jclass vectorClass;

#if USE_PORTS == TRUE
    if (id != 0) {
        memset(&creator, 0, sizeof(ControlCreatorJNI));
        creator.creator.newBooleanControl  = &PORT_NewBooleanControl;
        creator.creator.newCompoundControl = &PORT_NewCompoundControl;
        creator.creator.newFloatControl    = &PORT_NewFloatControl;
        creator.creator.addControl         = &PORT_AddControl;
        creator.env = env;
        vectorClass = (*env)->GetObjectClass(env, vector);
        if (vectorClass == NULL) {
            ERROR0("Java_com_sun_media_sound_PortMixer_nGetControls: vectorClass is NULL\n");
            return;
        }
        creator.vector = vector;
        creator.vectorAddElement = (*env)->GetMethodID(env, vectorClass, "addElement", "(Ljava/lang/Object;)V");
        if (creator.vectorAddElement == NULL) {
            ERROR0("Java_com_sun_media_sound_PortMixer_nGetControls: addElementMethodID is NULL\n");
            return;
        }
        PORT_GetControls((void*) (UINT_PTR) id, (INT32) portIndex, (PortControlCreator*) &creator);
    }
#endif
}
