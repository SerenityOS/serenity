/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <sys/time.h>
#include <sys/utsname.h>
#include <sys/types.h>
#include <errno.h>
#include <dlfcn.h>
#include "jni.h"
#include <jni_util.h>
#include "jvm_md.h"
#include "awt_Mlib.h"
#include "java_awt_image_BufferedImage.h"

#ifdef STATIC_BUILD
#include "mlib_image.h"
#endif

static void start_timer(int numsec);
static void stop_timer(int numsec, int ntimes);

#ifdef STATIC_BUILD
// Mapping functions to their names for runtime check
static mlibFnS_t sMlibFnsStatic[] = {
    {j2d_mlib_ImageConvMxN, "j2d_mlib_ImageConvMxN"},
    {j2d_mlib_ImageAffine, "j2d_mlib_ImageAffine"},
    {j2d_mlib_ImageLookUp, "j2d_mlib_ImageLookUp"},
    {j2d_mlib_ImageConvKernelConvert, "j2d_mlib_ImageConvKernelConvert"},
};

mlib_status awt_getImagingLib(JNIEnv *env, mlibFnS_t *sMlibFns,
                              mlibSysFnS_t *sMlibSysFns) {
    mlibFnS_t *mptr;
    int i;
    char *fName;
    mlibSysFnS_t tempSysFns;
    mlib_status ret = MLIB_SUCCESS;

    tempSysFns.createFP = j2d_mlib_ImageCreate;
    tempSysFns.createStructFP = j2d_mlib_ImageCreateStruct;
    tempSysFns.deleteImageFP = j2d_mlib_ImageDelete;
    *sMlibSysFns = tempSysFns;

    mptr = sMlibFns;
    i = 0;
    while (mptr[i].fname != NULL) {
        fName = mptr[i].fname;
        if(strcmp(fName, sMlibFnsStatic[i].fname) == 0) {
            mptr[i].fptr = sMlibFnsStatic[i].fptr;
        } else {
            ret = MLIB_FAILURE;
        }
        i++;
    }

    return ret;
}
#else
/*
 * This is called by awt_ImagingLib.initLib()
 */
mlib_status awt_getImagingLib(JNIEnv *env, mlibFnS_t *sMlibFns,
                              mlibSysFnS_t *sMlibSysFns) {
    int status;
    jstring jstr = NULL;
    mlibFnS_t *mptr;
    void *(*vPtr)();
    int (*intPtr)();
    mlib_status (*fPtr)();
    int i;
    void *handle = NULL;
    mlibSysFnS_t tempSysFns;
    static int s_timeIt = 0;
    static int s_verbose = 1;
    mlib_status ret = MLIB_SUCCESS;
    struct utsname name;

    handle = dlopen(JNI_LIB_NAME("mlib_image"), RTLD_LAZY);

    if (handle == NULL) {
        if (s_timeIt || s_verbose) {
            printf ("error in dlopen: %s", dlerror());
        }
        return MLIB_FAILURE;
    }

    /* So, if we are here, then either vis or generic version of
     * medialib library was sucessfuly loaded.
     * Let's try to initialize handlers...
     */
    if ((tempSysFns.createFP = (MlibCreateFP_t)dlsym(handle,
                                       "j2d_mlib_ImageCreate")) == NULL) {
        if (s_timeIt) {
            printf ("error in dlsym: %s", dlerror());
        }
        ret = MLIB_FAILURE;
    }

    if (ret == MLIB_SUCCESS) {
        if ((tempSysFns.createStructFP = (MlibCreateStructFP_t)dlsym(handle,
                                          "j2d_mlib_ImageCreateStruct")) == NULL) {
            if (s_timeIt) {
                printf ("error in dlsym: %s", dlerror());
            }
            ret = MLIB_FAILURE;
        }
    }

    if (ret == MLIB_SUCCESS) {
        if ((tempSysFns.deleteImageFP = (MlibDeleteFP_t)dlsym(handle,
                                                 "j2d_mlib_ImageDelete")) == NULL) {
            if (s_timeIt) {
                printf ("error in dlsym: %s", dlerror());
            }
            ret = MLIB_FAILURE;
        }
    }

    /* Set the system functions */
    if (ret == MLIB_SUCCESS) {
        *sMlibSysFns = tempSysFns;
    }

    /* Loop through all of the fns and load them from the next library */
    mptr = sMlibFns;
    i = 0;
    while ((ret == MLIB_SUCCESS) && (mptr[i].fname != NULL)) {
        fPtr = (mlib_status (*)())dlsym(handle, mptr[i].fname);
        if (fPtr != NULL) {
            mptr[i].fptr = fPtr;
        } else {
            ret = MLIB_FAILURE;
        }
        i++;
    }
    if (ret != MLIB_SUCCESS) {
        dlclose(handle);
    }
    return ret;
}
#endif

mlib_start_timer awt_setMlibStartTimer() {
    return start_timer;
}

mlib_stop_timer awt_setMlibStopTimer() {
    return stop_timer;
}

/***************************************************************************
 *                          Static Functions                               *
 ***************************************************************************/

static void start_timer(int numsec)
{
    struct itimerval interval;

    interval.it_interval.tv_sec = numsec;
    interval.it_interval.tv_usec = 0;
    interval.it_value.tv_sec = numsec;
    interval.it_value.tv_usec = 0;
    setitimer(ITIMER_REAL, &interval, 0);
}


static void stop_timer(int numsec, int ntimes)
{
    struct itimerval interval;
    double sec;

    getitimer(ITIMER_REAL, &interval);
    sec = (((double) (numsec - 1)) - (double) interval.it_value.tv_sec) +
            (1000000.0 - interval.it_value.tv_usec)/1000000.0;
    sec = sec/((double) ntimes);
    printf("%f msec per update\n", sec * 1000.0);
    interval.it_interval.tv_sec = 0;
    interval.it_interval.tv_usec = 0;
    interval.it_value.tv_sec = 0;
    interval.it_value.tv_usec = 0;
    setitimer(ITIMER_PROF, &interval, 0);
}
