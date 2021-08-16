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

#include <jni.h>
#include <jni_util.h>
#include <jvm_md.h>
#include <dlfcn.h>
#include <cups/cups.h>
#include <cups/ppd.h>


//#define CUPS_DEBUG

#ifdef CUPS_DEBUG
#define DPRINTF(x, y) fprintf(stderr, x, y);
#else
#define DPRINTF(x, y)
#endif

typedef const char* (*fn_cupsServer)(void);
typedef int (*fn_ippPort)(void);
typedef http_t* (*fn_httpConnect)(const char *, int);
typedef void (*fn_httpClose)(http_t *);
typedef char* (*fn_cupsGetPPD)(const char *);
typedef cups_dest_t* (*fn_cupsGetDest)(const char *name,
    const char *instance, int num_dests, cups_dest_t *dests);
typedef int (*fn_cupsGetDests)(cups_dest_t **dests);
typedef void (*fn_cupsFreeDests)(int num_dests, cups_dest_t *dests);
typedef ppd_file_t* (*fn_ppdOpenFile)(const char *);
typedef void (*fn_ppdClose)(ppd_file_t *);
typedef ppd_option_t* (*fn_ppdFindOption)(ppd_file_t *, const char *);
typedef ppd_size_t* (*fn_ppdPageSize)(ppd_file_t *, char *);

fn_cupsServer j2d_cupsServer;
fn_ippPort j2d_ippPort;
fn_httpConnect j2d_httpConnect;
fn_httpClose j2d_httpClose;
fn_cupsGetPPD j2d_cupsGetPPD;
fn_cupsGetDest j2d_cupsGetDest;
fn_cupsGetDests j2d_cupsGetDests;
fn_cupsFreeDests j2d_cupsFreeDests;
fn_ppdOpenFile j2d_ppdOpenFile;
fn_ppdClose j2d_ppdClose;
fn_ppdFindOption j2d_ppdFindOption;
fn_ppdPageSize j2d_ppdPageSize;


/*
 * Initialize library functions.
 * // REMIND : move tab , add dlClose before return
 */
JNIEXPORT jboolean JNICALL
Java_sun_print_CUPSPrinter_initIDs(JNIEnv *env,
                                         jobject printObj) {
  void *handle = dlopen(VERSIONED_JNI_LIB_NAME("cups", "2"),
                        RTLD_LAZY | RTLD_GLOBAL);

  if (handle == NULL) {
    handle = dlopen(JNI_LIB_NAME("cups"), RTLD_LAZY | RTLD_GLOBAL);
    if (handle == NULL) {
      return JNI_FALSE;
    }
  }

  j2d_cupsServer = (fn_cupsServer)dlsym(handle, "cupsServer");
  if (j2d_cupsServer == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ippPort = (fn_ippPort)dlsym(handle, "ippPort");
  if (j2d_ippPort == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_httpConnect = (fn_httpConnect)dlsym(handle, "httpConnect");
  if (j2d_httpConnect == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_httpClose = (fn_httpClose)dlsym(handle, "httpClose");
  if (j2d_httpClose == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_cupsGetPPD = (fn_cupsGetPPD)dlsym(handle, "cupsGetPPD");
  if (j2d_cupsGetPPD == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_cupsGetDest = (fn_cupsGetDest)dlsym(handle, "cupsGetDest");
  if (j2d_cupsGetDest == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_cupsGetDests = (fn_cupsGetDests)dlsym(handle, "cupsGetDests");
  if (j2d_cupsGetDests == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_cupsFreeDests = (fn_cupsFreeDests)dlsym(handle, "cupsFreeDests");
  if (j2d_cupsFreeDests == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ppdOpenFile = (fn_ppdOpenFile)dlsym(handle, "ppdOpenFile");
  if (j2d_ppdOpenFile == NULL) {
    dlclose(handle);
    return JNI_FALSE;

  }

  j2d_ppdClose = (fn_ppdClose)dlsym(handle, "ppdClose");
  if (j2d_ppdClose == NULL) {
    dlclose(handle);
    return JNI_FALSE;

  }

  j2d_ppdFindOption = (fn_ppdFindOption)dlsym(handle, "ppdFindOption");
  if (j2d_ppdFindOption == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ppdPageSize = (fn_ppdPageSize)dlsym(handle, "ppdPageSize");
  if (j2d_ppdPageSize == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

/*
 * Gets CUPS server name.
 *
 */
JNIEXPORT jstring JNICALL
Java_sun_print_CUPSPrinter_getCupsServer(JNIEnv *env,
                                         jobject printObj)
{
    jstring cServer = NULL;
    const char* server = j2d_cupsServer();
    if (server != NULL) {
        // Is this a local domain socket?
        if (strncmp(server, "/", 1) == 0) {
            cServer = JNU_NewStringPlatform(env, "localhost");
        } else {
            cServer = JNU_NewStringPlatform(env, server);
        }
    }
    return cServer;
}

/*
 * Gets CUPS port name.
 *
 */
JNIEXPORT jint JNICALL
Java_sun_print_CUPSPrinter_getCupsPort(JNIEnv *env,
                                         jobject printObj)
{
    int port = j2d_ippPort();
    return (jint) port;
}


/*
 * Gets CUPS default printer name.
 *
 */
JNIEXPORT jstring JNICALL
Java_sun_print_CUPSPrinter_getCupsDefaultPrinter(JNIEnv *env,
                                                  jobject printObj)
{
    jstring cDefPrinter = NULL;
    cups_dest_t *dests;
    char *defaultPrinter = NULL;
    int num_dests = j2d_cupsGetDests(&dests);
    int i = 0;
    cups_dest_t *dest = j2d_cupsGetDest(NULL, NULL, num_dests, dests);
    if (dest != NULL) {
        defaultPrinter = dest->name;
        if (defaultPrinter != NULL) {
            cDefPrinter = JNU_NewStringPlatform(env, defaultPrinter);
        }
    }
    j2d_cupsFreeDests(num_dests, dests);
    return cDefPrinter;
}

/*
 * Checks if connection can be made to the server.
 *
 */
JNIEXPORT jboolean JNICALL
Java_sun_print_CUPSPrinter_canConnect(JNIEnv *env,
                                      jobject printObj,
                                      jstring server,
                                      jint port)
{
    const char *serverName;
    serverName = (*env)->GetStringUTFChars(env, server, NULL);
    if (serverName != NULL) {
        http_t *http = j2d_httpConnect(serverName, (int)port);
        (*env)->ReleaseStringUTFChars(env, server, serverName);
        if (http != NULL) {
            j2d_httpClose(http);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}


/*
 * Returns list of media: pages + trays
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_print_CUPSPrinter_getMedia(JNIEnv *env,
                                         jobject printObj,
                                         jstring printer)
{
    ppd_file_t *ppd;
    ppd_option_t *optionTray, *optionPage;
    ppd_choice_t *choice;
    const char *name;
    const char *filename;
    int i, nTrays=0, nPages=0, nTotal=0;
    jstring utf_str;
    jclass cls;
    jobjectArray nameArray = NULL;

    name = (*env)->GetStringUTFChars(env, printer, NULL);
    if (name == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Could not create printer name");
        return NULL;
    }

    // NOTE: cupsGetPPD returns a pointer to a filename of a temporary file.
    // unlink() must be caled to remove the file when finished using it.
    filename = j2d_cupsGetPPD(name);
    (*env)->ReleaseStringUTFChars(env, printer, name);
    CHECK_NULL_RETURN(filename, NULL);

    cls = (*env)->FindClass(env, "java/lang/String");
    CHECK_NULL_RETURN(cls, NULL);

    if ((ppd = j2d_ppdOpenFile(filename)) == NULL) {
        unlink(filename);
        DPRINTF("CUPSfuncs::unable to open PPD  %s\n", filename);
        return NULL;
    }

    optionPage = j2d_ppdFindOption(ppd, "PageSize");
    if (optionPage != NULL) {
        nPages = optionPage->num_choices;
    }

    optionTray = j2d_ppdFindOption(ppd, "InputSlot");
    if (optionTray != NULL) {
        nTrays = optionTray->num_choices;
    }

    if ((nTotal = (nPages+nTrays) *2) > 0) {
        nameArray = (*env)->NewObjectArray(env, nTotal, cls, NULL);
        if (nameArray == NULL) {
            unlink(filename);
            j2d_ppdClose(ppd);
            DPRINTF("CUPSfuncs::bad alloc new array\n", "")
            (*env)->ExceptionClear(env);
            JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
            return NULL;
        }

        for (i = 0; optionPage!=NULL && i<nPages; i++) {
            choice = (optionPage->choices)+i;
            utf_str = JNU_NewStringPlatform(env, choice->text);
            if (utf_str == NULL) {
                unlink(filename);
                j2d_ppdClose(ppd);
                DPRINTF("CUPSfuncs::bad alloc new string ->text\n", "")
                JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
                return NULL;
            }
            (*env)->SetObjectArrayElement(env, nameArray, i*2, utf_str);
            (*env)->DeleteLocalRef(env, utf_str);
            utf_str = JNU_NewStringPlatform(env, choice->choice);
            if (utf_str == NULL) {
                unlink(filename);
                j2d_ppdClose(ppd);
                DPRINTF("CUPSfuncs::bad alloc new string ->choice\n", "")
                JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
                return NULL;
            }
            (*env)->SetObjectArrayElement(env, nameArray, i*2+1, utf_str);
            (*env)->DeleteLocalRef(env, utf_str);
        }

        for (i = 0; optionTray!=NULL && i<nTrays; i++) {
            choice = (optionTray->choices)+i;
            utf_str = JNU_NewStringPlatform(env, choice->text);
            if (utf_str == NULL) {
                unlink(filename);
                j2d_ppdClose(ppd);
                DPRINTF("CUPSfuncs::bad alloc new string text\n", "")
                JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
                return NULL;
            }
            (*env)->SetObjectArrayElement(env, nameArray,
                                          (nPages+i)*2, utf_str);
            (*env)->DeleteLocalRef(env, utf_str);
            utf_str = JNU_NewStringPlatform(env, choice->choice);
            if (utf_str == NULL) {
                unlink(filename);
                j2d_ppdClose(ppd);
                DPRINTF("CUPSfuncs::bad alloc new string choice\n", "")
                JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
                return NULL;
            }
            (*env)->SetObjectArrayElement(env, nameArray,
                                          (nPages+i)*2+1, utf_str);
            (*env)->DeleteLocalRef(env, utf_str);
        }
    }
    j2d_ppdClose(ppd);
    unlink(filename);
    return nameArray;
}


/*
 * Returns list of page sizes and imageable area.
 */
JNIEXPORT jfloatArray JNICALL
Java_sun_print_CUPSPrinter_getPageSizes(JNIEnv *env,
                                         jobject printObj,
                                         jstring printer)
{
    ppd_file_t *ppd;
    ppd_option_t *option;
    ppd_choice_t *choice;
    ppd_size_t *size;
    const char *filename = NULL;
    int i;
    jobjectArray sizeArray = NULL;
    jfloat *dims;

    const char *name = (*env)->GetStringUTFChars(env, printer, NULL);
    if (name == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Could not create printer name");
        return NULL;
    }

    // NOTE: cupsGetPPD returns a pointer to a filename of a temporary file.
    // unlink() must be called to remove the file after using it.
    filename = j2d_cupsGetPPD(name);
    (*env)->ReleaseStringUTFChars(env, printer, name);
    CHECK_NULL_RETURN(filename, NULL);
    if ((ppd = j2d_ppdOpenFile(filename)) == NULL) {
        unlink(filename);
        DPRINTF("unable to open PPD  %s\n", filename)
        return NULL;
    }
    option = j2d_ppdFindOption(ppd, "PageSize");
    if (option != NULL && option->num_choices > 0) {
        // create array of dimensions - (num_choices * 6)
        //to cover length & height
        DPRINTF( "CUPSfuncs::option->num_choices %d\n", option->num_choices)
        // +1 is for storing the default media index
        sizeArray = (*env)->NewFloatArray(env, option->num_choices*6+1);
        if (sizeArray == NULL) {
            unlink(filename);
            j2d_ppdClose(ppd);
            DPRINTF("CUPSfuncs::bad alloc new float array\n", "")
            (*env)->ExceptionClear(env);
            JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
            return NULL;
        }

        dims = (*env)->GetFloatArrayElements(env, sizeArray, NULL);
        if (dims == NULL) {
            unlink(filename);
            j2d_ppdClose(ppd);
            (*env)->ExceptionClear(env);
            JNU_ThrowOutOfMemoryError(env, "Could not create printer name");
            return NULL;
        }
        for (i = 0; i<option->num_choices; i++) {
            choice = (option->choices)+i;
            // get the index of the default page
            if (!strcmp(choice->choice, option->defchoice)) {
                dims[option->num_choices*6] = (float)i;
            }
            size = j2d_ppdPageSize(ppd, choice->choice);
            if (size != NULL) {
                // paper width and height
                dims[i*6] = size->width;
                dims[(i*6)+1] = size->length;
                // paper printable area
                dims[(i*6)+2] = size->left;
                dims[(i*6)+3] = size->top;
                dims[(i*6)+4] = size->right;
                dims[(i*6)+5] = size->bottom;
            }
        }

        (*env)->ReleaseFloatArrayElements(env, sizeArray, dims, 0);
    }

    j2d_ppdClose(ppd);
    unlink(filename);
    return sizeArray;
}

/*
 * Populates the supplied ArrayList<Integer> with resolutions.
 * The first pair of elements will be the default resolution.
 * If resolution isn't supported the list will be empty.
 * If needed we can add a 2nd ArrayList<String> which would
 * be populated with the corresponding UI name.
 * PPD specifies the syntax for resolution as either "Ndpi" or "MxNdpi",
 * eg 300dpi or 600x600dpi. The former is a shorthand where xres==yres.
 * We will always expand to the latter as we use a single array list.
 * Note: getMedia() and getPageSizes() both open the ppd file
 * This is not going to scale forever so if we add anymore we
 * should look to consolidate this.
 */
JNIEXPORT void JNICALL
Java_sun_print_CUPSPrinter_getResolutions(JNIEnv *env,
                                          jobject printObj,
                                          jstring printer,
                                          jobject arrayList)
{
    ppd_file_t *ppd = NULL;
    ppd_option_t *resolution;
    int defx = 0, defy = 0;
    int resx = 0, resy = 0;
    jclass intCls, cls;
    jmethodID intCtr, arrListAddMID;
    int i;
    const char *name = NULL;
    const char *filename = NULL;

    intCls = (*env)->FindClass(env, "java/lang/Integer");
    CHECK_NULL(intCls);
    intCtr = (*env)->GetMethodID(env, intCls, "<init>", "(I)V");
    CHECK_NULL(intCtr);
    cls = (*env)->FindClass(env, "java/util/ArrayList");
    CHECK_NULL(cls);
    arrListAddMID =
        (*env)->GetMethodID(env, cls, "add", "(Ljava/lang/Object;)Z");
    CHECK_NULL(arrListAddMID);

    name = (*env)->GetStringUTFChars(env, printer, NULL);
    if (name == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Could not create printer name");
        return;
    }


    // NOTE: cupsGetPPD returns a pointer to a filename of a temporary file.
    // unlink() must be called to remove the file after using it.
    filename = j2d_cupsGetPPD(name);
    (*env)->ReleaseStringUTFChars(env, printer, name);
    CHECK_NULL(filename);
    if ((ppd = j2d_ppdOpenFile(filename)) == NULL) {
        unlink(filename);
        DPRINTF("unable to open PPD  %s\n", filename)
    }
    resolution = j2d_ppdFindOption(ppd, "Resolution");
    if (resolution != NULL) {
        int matches = sscanf(resolution->defchoice, "%dx%ddpi", &defx, &defy);
        if (matches == 2) {
           if (defx <= 0 || defy <= 0) {
              defx = 0;
              defy = 0;
           }
        } else {
            matches = sscanf(resolution->defchoice, "%ddpi", &defx);
            if (matches == 1) {
                if (defx <= 0) {
                   defx = 0;
                } else {
                   defy = defx;
                }
            }
        }
        if (defx > 0) {
          jobject rxObj, ryObj;
          rxObj = (*env)->NewObject(env, intCls, intCtr, defx);
          CHECK_NULL(rxObj);
          ryObj = (*env)->NewObject(env, intCls, intCtr, defy);
          CHECK_NULL(ryObj);
          (*env)->CallBooleanMethod(env, arrayList, arrListAddMID, rxObj);
          (*env)->CallBooleanMethod(env, arrayList, arrListAddMID, ryObj);
        }

        for (i = 0; i < resolution->num_choices; i++) {
            char *resStr = resolution->choices[i].choice;
            int matches = sscanf(resStr, "%dx%ddpi", &resx, &resy);
            if (matches == 2) {
               if (resx <= 0 || resy <= 0) {
                  resx = 0;
                  resy = 0;
               }
            } else {
                matches = sscanf(resStr, "%ddpi", &resx);
                if (matches == 1) {
                    if (resx <= 0) {
                       resx = 0;
                    } else {
                       resy = resx;
                    }
                }
            }
            if (resx > 0 && (resx != defx || resy != defy )) {
              jobject rxObj, ryObj;
              rxObj = (*env)->NewObject(env, intCls, intCtr, resx);
              CHECK_NULL(rxObj);
              ryObj = (*env)->NewObject(env, intCls, intCtr, resy);
              CHECK_NULL(ryObj);
              (*env)->CallBooleanMethod(env, arrayList, arrListAddMID, rxObj);
              (*env)->CallBooleanMethod(env, arrayList, arrListAddMID, ryObj);
            }
        }
    }

    j2d_ppdClose(ppd);
    unlink(filename);
}
