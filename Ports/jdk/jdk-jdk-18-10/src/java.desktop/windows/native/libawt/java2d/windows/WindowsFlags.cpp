/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include <awt.h>
#include "Trace.h"
#include "WindowsFlags.h"

BOOL      useD3D = TRUE;      // d3d enabled flag
                              // initially is TRUE to allow D3D preloading
BOOL      forceD3DUsage;      // force d3d on or off
jboolean  g_offscreenSharing; // JAWT accelerated surface sharing
BOOL      setHighDPIAware;    // Whether to set the high-DPI awareness flag

extern WCHAR *j2dAccelKey;       // Name of java2d root key
extern WCHAR *j2dAccelDriverKey; // Name of j2d per-device key

static jfieldID d3dEnabledID;
static jfieldID d3dSetID;
static jclass   wFlagsClassID;

void SetIDs(JNIEnv *env, jclass wFlagsClass)
{
    wFlagsClassID = (jclass)env->NewGlobalRef(wFlagsClass);
    d3dEnabledID = env->GetStaticFieldID(wFlagsClass, "d3dEnabled", "Z");
    CHECK_NULL(d3dEnabledID);
    d3dSetID = env->GetStaticFieldID(wFlagsClass, "d3dSet", "Z");
    CHECK_NULL(d3dSetID);
}

BOOL GetStaticBoolean(JNIEnv *env, jclass wfClass, const char *fieldName)
{
    jfieldID fieldID = env->GetStaticFieldID(wfClass, fieldName, "Z");
    CHECK_NULL_RETURN(fieldID, FALSE);
    return env->GetStaticBooleanField(wfClass, fieldID);
}

jobject GetStaticObject(JNIEnv *env, jclass wfClass, const char *fieldName,
                        const char *signature)
{
    jfieldID fieldID = env->GetStaticFieldID(wfClass, fieldName, signature);
    CHECK_NULL_RETURN(fieldID, NULL);
    return env->GetStaticObjectField(wfClass, fieldID);
}

void GetFlagValues(JNIEnv *env, jclass wFlagsClass)
{
    jboolean d3dEnabled = env->GetStaticBooleanField(wFlagsClass, d3dEnabledID);
    jboolean d3dSet = env->GetStaticBooleanField(wFlagsClass, d3dSetID);
    if (!d3dSet) {
        // Only check environment variable if user did not set Java
        // command-line parameter; values of sun.java2d.d3d override
        // any setting of J2D_D3D environment variable.
        char *d3dEnv = getenv("J2D_D3D");
        if (d3dEnv) {
            if (strcmp(d3dEnv, "false") == 0) {
                // printf("Java2D Direct3D usage disabled by J2D_D3D env\n");
                d3dEnabled = FALSE;
                d3dSet = TRUE;
                SetD3DEnabledFlag(env, d3dEnabled, d3dSet);
            } else if (strcmp(d3dEnv, "true") == 0) {
                // printf("Java2D Direct3D usage forced on by J2D_D3D env\n");
                d3dEnabled = TRUE;
                d3dSet = TRUE;
                SetD3DEnabledFlag(env, d3dEnabled, d3dSet);
            }
        }
    }
    useD3D = d3dEnabled;
    forceD3DUsage = d3dSet;
    g_offscreenSharing = GetStaticBoolean(env, wFlagsClass,
                                          "offscreenSharingEnabled");
    JNU_CHECK_EXCEPTION(env);

    setHighDPIAware =
        (IS_WINVISTA && GetStaticBoolean(env, wFlagsClass, "setHighDPIAware"));
    JNU_CHECK_EXCEPTION(env);

    J2dTraceLn(J2D_TRACE_INFO, "WindowsFlags (native):");
    J2dTraceLn1(J2D_TRACE_INFO, "  d3dEnabled = %s",
                (useD3D ? "true" : "false"));
    J2dTraceLn1(J2D_TRACE_INFO, "  d3dSet = %s",
                (forceD3DUsage ? "true" : "false"));
    J2dTraceLn1(J2D_TRACE_INFO, "  offscreenSharing = %s",
                (g_offscreenSharing ? "true" : "false"));
    J2dTraceLn1(J2D_TRACE_INFO, "  setHighDPIAware = %s",
                (setHighDPIAware ? "true" : "false"));
}

void SetD3DEnabledFlag(JNIEnv *env, BOOL d3dEnabled, BOOL d3dSet)
{
    useD3D = d3dEnabled;
    forceD3DUsage = d3dSet;
    if (env == NULL) {
        env = (JNIEnv * ) JNU_GetEnv(jvm, JNI_VERSION_1_2);
    }
    env->SetStaticBooleanField(wFlagsClassID, d3dEnabledID, d3dEnabled);
    if (d3dSet) {
        env->SetStaticBooleanField(wFlagsClassID, d3dSetID, d3dSet);
    }
}

BOOL IsD3DEnabled() {
    return useD3D;
}

BOOL IsD3DForced() {
    return forceD3DUsage;
}

extern "C" {

/**
 * This function is called from WindowsFlags.initFlags() and initializes
 * the native side of our runtime flags.  There are a couple of important
 * things that happen at the native level after we set the Java flags:
 * - set native variables based on the java flag settings (such as useDD
 * based on whether ddraw was enabled by a runtime flag)
 * - override java level settings if there user has set an environment
 * variable but not a runtime flag.  For example, if the user runs
 * with sun.java2d.d3d=true but also uses the J2D_D3D=false environment
 * variable, then we use the java-level true value.  But if they do
 * not use the runtime flag, then the env variable will force d3d to
 * be disabled.  Any native env variable overriding must up-call to
 * Java to change the java level flag settings.
 * - A later error in initialization may result in disabling some
 * native property that must be propagated to the Java level.  For
 * example, d3d is enabled by default, but we may find later that
 * we must disable it do to some runtime configuration problem (such as
 * a bad video card).  This will happen through mechanisms in this native
 * file to change the value of the known Java flags (in this d3d example,
 * we would up-call to set the value of d3dEnabled to Boolean.FALSE).
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_WindowsFlags_initNativeFlags(JNIEnv *env,
                                                     jclass wFlagsClass)
{
    SetIDs(env, wFlagsClass);
    JNU_CHECK_EXCEPTION(env);
    GetFlagValues(env, wFlagsClass);
}

} // extern "C"
