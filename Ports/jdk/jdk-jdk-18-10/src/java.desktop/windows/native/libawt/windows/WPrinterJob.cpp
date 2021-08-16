/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"

#include "stdhdrs.h"
#include <commdlg.h>
#include <winspool.h>
#include <limits.h>
#include <float.h>

#include "awt_Toolkit.h"
#include "awt_PrintControl.h"

/* values for parameter "type" of XXX_getJobStatus() */
#define GETJOBCOUNT  1
#define ACCEPTJOB    2

static const char *HPRINTER_STR = "hPrintJob";

/* constants for DeviceCapability buffer lengths */
#define PAPERNAME_LENGTH 64
#define TRAYNAME_LENGTH 24


static BOOL IsSupportedLevel(HANDLE hPrinter, DWORD dwLevel) {
    BOOL isSupported = FALSE;
    DWORD cbBuf = 0;
    LPBYTE pPrinter = NULL;

    DASSERT(hPrinter != NULL);

    VERIFY(::GetPrinter(hPrinter, dwLevel, NULL, 0, &cbBuf) == 0);
    if (::GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
        pPrinter = new BYTE[cbBuf];
        if (::GetPrinter(hPrinter, dwLevel, pPrinter, cbBuf, &cbBuf)) {
            isSupported = TRUE;
        }
        delete[] pPrinter;
    }

    return isSupported;
}


extern "C" {

JNIEXPORT jstring JNICALL
Java_sun_print_PrintServiceLookupProvider_getDefaultPrinterName(JNIEnv *env,
                                                             jobject peer)
{
    TRY;

    TCHAR cBuffer[250];
    OSVERSIONINFO osv;
    PRINTER_INFO_2 *ppi2 = NULL;
    DWORD dwNeeded = 0;
    DWORD dwReturned = 0;
    LPTSTR pPrinterName = NULL;
    jstring jPrinterName;

    // What version of Windows are you running?
    osv.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
    GetVersionEx(&osv);

    // If Windows 2000, XP, Vista
    if (osv.dwPlatformId == VER_PLATFORM_WIN32_NT) {

       // Retrieve the default string from Win.ini (the registry).
       // String will be in form "printername,drivername,portname".

       if (GetProfileString(TEXT("windows"), TEXT("device"), TEXT(",,,"),
                            cBuffer, 250) <= 0) {
           return NULL;
       }
       // Copy printer name into passed-in buffer...
       int index = 0;
       int len = lstrlen(cBuffer);
       while ((index < len) && cBuffer[index] != _T(',')) {
              index++;
       }
       if (index==0) {
         return NULL;
       }

       pPrinterName = (LPTSTR)GlobalAlloc(GPTR, (index+1)*sizeof(TCHAR));
       lstrcpyn(pPrinterName, cBuffer, index+1);
       jPrinterName = JNU_NewStringPlatform(env, pPrinterName);
       GlobalFree(pPrinterName);
       return jPrinterName;
    } else {
        return NULL;
    }

    CATCH_BAD_ALLOC_RET(NULL);
}


static jobjectArray getPrinterNames(JNIEnv *env, DWORD flags) {
    TRY;

    DWORD cbNeeded = 0;
    DWORD cReturned = 0;
    LPBYTE pPrinterEnum = NULL;

    jstring utf_str;
    jclass clazz = env->FindClass("java/lang/String");
    if (clazz == NULL) {
        return NULL;
    }
    jobjectArray nameArray;

    try {
        ::EnumPrinters(flags, NULL, 4, NULL,
                       0, &cbNeeded, &cReturned);

        BOOL bResult;
        int nCount = 0;
        do {
            if (pPrinterEnum != NULL) {
                delete [] pPrinterEnum;
            }
            pPrinterEnum = new BYTE[cbNeeded];

            bResult = ::EnumPrinters(flags, NULL, 4, pPrinterEnum,
                                     cbNeeded, &cbNeeded, &cReturned);
        } while (!bResult && ++nCount < 5);

        if (!bResult) {
            // No printers in case of error
            cReturned = 0;
        }

        if (cReturned > 0) {
            nameArray = env->NewObjectArray(cReturned, clazz, NULL);
            if (nameArray == NULL) {
                throw std::bad_alloc();
            }
        } else {
            nameArray = NULL;
        }


        for (DWORD i = 0; i < cReturned; i++) {
            PRINTER_INFO_4 *info4 = (PRINTER_INFO_4 *)
                (pPrinterEnum + i * sizeof(PRINTER_INFO_4));
            utf_str = JNU_NewStringPlatform(env, info4->pPrinterName);
            if (utf_str == NULL) {
                throw std::bad_alloc();
            }
            env->SetObjectArrayElement(nameArray, i, utf_str);
            env->DeleteLocalRef(utf_str);
        }
    } catch (std::bad_alloc&) {
        delete [] pPrinterEnum;
        throw;
    }

    delete [] pPrinterEnum;
    return nameArray;

    CATCH_BAD_ALLOC_RET(NULL);
}

JNIEXPORT jobjectArray JNICALL
Java_sun_print_PrintServiceLookupProvider_getAllPrinterNames(JNIEnv *env,
                                                             jobject peer)
{
    return getPrinterNames(env, PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS);
}


JNIEXPORT void JNICALL
Java_sun_print_PrintServiceLookupProvider_notifyLocalPrinterChange(JNIEnv *env,
                                                                   jobject peer)
{
    jclass cls = env->GetObjectClass(peer);
    CHECK_NULL(cls);
    jmethodID refresh = env->GetMethodID(cls, "refreshServices", "()V");
    CHECK_NULL(refresh);

    HANDLE hPrinter;
    LPTSTR printerName = NULL; // NULL indicates the local printer server
    if (!::OpenPrinter(printerName, &hPrinter, NULL)) {
        return;
    }
    // PRINTER_CHANGE_PRINTER = PRINTER_CHANGE_ADD_PRINTER |
    //                          PRINTER_CHANGE_SET_PRINTER |
    //                          PRINTER_CHANGE_DELETE_PRINTER |
    //                          PRINTER_CHANGE_FAILED_CONNECTION_PRINTER
    HANDLE chgObj = FindFirstPrinterChangeNotification(hPrinter,
                                                       PRINTER_CHANGE_PRINTER,
                                                       0,
                                                       NULL);
    if (chgObj != INVALID_HANDLE_VALUE) {
        BOOL keepMonitoring;
        do {
            keepMonitoring = FALSE;
            if (WaitForSingleObject(chgObj, INFINITE) == WAIT_OBJECT_0) {
                DWORD dwChange;
                keepMonitoring = FindNextPrinterChangeNotification(
                                                 chgObj, &dwChange, NULL, NULL);
            }
            if (keepMonitoring) {
                env->CallVoidMethod(peer, refresh);
            }
        } while (keepMonitoring && !env->ExceptionCheck());

        FindClosePrinterChangeNotification(chgObj);
    }
    ::ClosePrinter(hPrinter);
}

JNIEXPORT void JNICALL
Java_sun_print_PrintServiceLookupProvider_notifyRemotePrinterChange(JNIEnv *env,
                                                                    jobject peer)
{
    jclass cls = env->GetObjectClass(peer);
    CHECK_NULL(cls);
    jmethodID refresh = env->GetMethodID(cls, "refreshServices", "()V");
    CHECK_NULL(refresh);

    HKEY hKey;
    if (ERROR_SUCCESS != RegOpenKeyEx(HKEY_CURRENT_USER,
                                      _T("Printers\\Connections"),
                                      0, KEY_NOTIFY, &hKey)) {
        return;
    }

    BOOL keepMonitoring;
    do {
        keepMonitoring =
                ERROR_SUCCESS == RegNotifyChangeKeyValue(hKey, TRUE,
                                                         REG_NOTIFY_CHANGE_NAME,
                                                         NULL,
                                                         FALSE);
        if (keepMonitoring) {
            env->CallVoidMethod(peer, refresh);
        }
    } while (keepMonitoring && !env->ExceptionCheck());

    RegCloseKey(hKey);
}


JNIEXPORT jfloatArray JNICALL
Java_sun_print_Win32PrintService_getMediaPrintableArea(JNIEnv *env,
                                                  jobject peer,
                                                  jstring printer,
                                                  jint  papersize)
{
    TRY;

    LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env,
                                                            printer, NULL);
    if (printerName == NULL) {
        return NULL;
    }

    jfloatArray printableArray = NULL;

    SAVE_CONTROLWORD
    HDC pdc = CreateDC(TEXT("WINSPOOL"), printerName, NULL, NULL);
    RESTORE_CONTROLWORD
    if (pdc) {
        HANDLE hPrinter;
        /* Start by opening the printer */
        if (!::OpenPrinter(printerName, &hPrinter, NULL)) {
            JNU_ReleaseStringPlatformChars(env, printer, printerName);
            return printableArray;
        }

        PDEVMODE pDevMode;

        if (!AwtPrintControl::getDevmode(hPrinter, printerName, &pDevMode)) {
            /* if failure, cleanup and return failure */

            if (pDevMode != NULL) {
                ::GlobalFree(pDevMode);
            }
            DeleteDC(pdc);
            ::ClosePrinter(hPrinter);
            JNU_ReleaseStringPlatformChars(env, printer, printerName);
            return printableArray;
        }

        pDevMode->dmFields |= (DM_PAPERSIZE | DM_ORIENTATION);
        pDevMode->dmPaperSize = (short)papersize;
        pDevMode->dmOrientation = DMORIENT_PORTRAIT;
        ::ResetDC(pdc, pDevMode);
        RESTORE_CONTROLWORD

        int left = GetDeviceCaps(pdc, PHYSICALOFFSETX);
        int top = GetDeviceCaps(pdc, PHYSICALOFFSETY);
        int width = GetDeviceCaps(pdc, HORZRES);
        int height = GetDeviceCaps(pdc, VERTRES);

        int resx = GetDeviceCaps(pdc, LOGPIXELSX);
        int resy = GetDeviceCaps(pdc, LOGPIXELSY);

        printableArray=env->NewFloatArray(4);
        if (printableArray != NULL) {
            jfloat *iPrintables =
                env->GetFloatArrayElements(printableArray, NULL);
            if (iPrintables != NULL) {
                iPrintables[0] = (float)left/resx;
                iPrintables[1] = (float)top/resy;
                iPrintables[2] = (float)width/resx;
                iPrintables[3] = (float)height/resy;
                env->ReleaseFloatArrayElements(printableArray, iPrintables, 0);
            }
        }
        GlobalFree(pDevMode);
        DeleteDC(pdc);
    }

    JNU_ReleaseStringPlatformChars(env, printer, printerName);

    return printableArray;

    CATCH_BAD_ALLOC_RET(NULL);
}

jintArray getIDs(JNIEnv *env, jstring printer, jstring port, int dm_id)
{

  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  SAVE_CONTROLWORD
  int numIDs = ::DeviceCapabilities(printerName, printerPort, dm_id,
                                    NULL, NULL);
  RESTORE_CONTROLWORD

  jintArray idArray = NULL;
  if (numIDs > 0) {
      idArray = env->NewIntArray(numIDs);
      if (idArray != NULL) {
          jint *jpcIndices = env->GetIntArrayElements(idArray, NULL);
          if (jpcIndices != NULL) {
              jint *saveFormats = jpcIndices;
              LPTSTR buf = NULL;
              try {
                  buf = (LPTSTR)new char[numIDs * sizeof(WORD)];
              } catch (std::bad_alloc&) {
                  buf = NULL;
              }
              if (buf != NULL) {
                  if (::DeviceCapabilities(printerName, printerPort,
                                           dm_id, buf, NULL) != -1) {
                      WORD *id = (WORD *)buf;
                      for (int i = 0; i < numIDs; i++, id++) {
                          jpcIndices[i] = *id;
                      }
                  }
                  RESTORE_CONTROLWORD
                  delete[] buf;
              }
              env->ReleaseIntArrayElements(idArray, saveFormats, 0);
          }
      }
  }

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);
  return idArray;
}

JNIEXPORT jintArray JNICALL
Java_sun_print_Win32PrintService_getAllMediaIDs(JNIEnv *env,
                                                jobject peer,
                                                jstring printer,
                                                jstring port)
{
    return getIDs(env, printer, port, DC_PAPERS);
}


JNIEXPORT jintArray JNICALL
Java_sun_print_Win32PrintService_getAllMediaTrays(JNIEnv *env,
                                                  jobject peer,
                                                  jstring printer,
                                                  jstring port)
{
    return getIDs(env, printer, port, DC_BINS);
}


JNIEXPORT jintArray JNICALL
Java_sun_print_Win32PrintService_getAllMediaSizes(JNIEnv *env,
                                                  jobject peer,
                                                  jstring printer,
                                                  jstring port)
{
  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  SAVE_CONTROLWORD
  int nPapers = ::DeviceCapabilities(printerName, printerPort, DC_PAPERSIZE,
                                     NULL, NULL) ;
  RESTORE_CONTROLWORD

  jintArray mediaArray = NULL;
  jint *saveFormats = NULL;

  if (nPapers > 0) {
      mediaArray = env->NewIntArray(nPapers*2);
      if (mediaArray != NULL) {
          jint *jpcIndices = env->GetIntArrayElements(mediaArray, NULL);
          if (jpcIndices != NULL) {
              saveFormats = jpcIndices;
              LPTSTR buf = NULL;
              try {
                  buf = (LPTSTR)new char[nPapers * sizeof(POINT)];
              } catch (std::bad_alloc&) {
                  buf = NULL;
              }
              if (buf != NULL) {
                  if (::DeviceCapabilities(printerName, printerPort,
                                           DC_PAPERSIZE, buf, NULL) != -1) {
                      POINT *pDim = (POINT *)buf;
                      for (int i = 0; i < nPapers; i++) {
                          jpcIndices[i*2] = (pDim+i)->x;
                          jpcIndices[i*2+1] = (pDim+i)->y;
                      }
                  }
                  RESTORE_CONTROLWORD
                  delete[] buf;
              }
              env->ReleaseIntArrayElements(mediaArray, saveFormats, 0);
              saveFormats = NULL;
          }
      }
  }

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);
  if (mediaArray != NULL && saveFormats != NULL) {
      env->ReleaseIntArrayElements(mediaArray, saveFormats, 0);
  }
  return mediaArray;

}


jobjectArray getAllDCNames(JNIEnv *env, jobject peer, jstring printer,
                 jstring port, unsigned int dc_id, unsigned int buf_len)
{

  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  jstring utf_str;
  jobjectArray names = NULL;
  LPTSTR buf = NULL;
  SAVE_CONTROLWORD
  int cReturned = ::DeviceCapabilities(printerName, printerPort,
                                         dc_id, NULL, NULL);
  RESTORE_CONTROLWORD
  if (cReturned <= 0) {
      JNU_ReleaseStringPlatformChars(env, printer, printerName);
      JNU_ReleaseStringPlatformChars(env, port, printerPort);
      return NULL;
  }

  try {
      buf = (LPTSTR)new char[cReturned * buf_len * sizeof(TCHAR)];
  } catch (std::bad_alloc&) {
      buf = NULL;
  }
  if (buf == NULL) {
      JNU_ReleaseStringPlatformChars(env, printer, printerName);
      JNU_ReleaseStringPlatformChars(env, port, printerPort);
      JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
     return NULL;
  }

  cReturned = ::DeviceCapabilities(printerName, printerPort,
                                   dc_id, buf, NULL);
  RESTORE_CONTROLWORD

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);

  if (cReturned > 0) {
      jclass cls = env->FindClass("java/lang/String");
      if (cls != NULL) {
          names = env->NewObjectArray(cReturned, cls, NULL);
      }
      if (names == NULL || cls == NULL) {
          delete[] buf;
          return names;
      }

      for (int i = 0; i < cReturned; i++) {
          utf_str = JNU_NewStringPlatform(env, buf+(buf_len*i));
            if (utf_str == NULL) {
                delete[] buf;
                return names;
            }
            env->SetObjectArrayElement(names, i, utf_str);
            env->DeleteLocalRef(utf_str);
        }
    }
    delete[] buf;
    return names;

}


JNIEXPORT jobjectArray JNICALL
Java_sun_print_Win32PrintService_getAllMediaNames(JNIEnv *env,
                                                  jobject peer,
                                                  jstring printer,
                                                  jstring port)
{
  return getAllDCNames(env, peer, printer, port, DC_PAPERNAMES, PAPERNAME_LENGTH);
}


JNIEXPORT jobjectArray JNICALL
Java_sun_print_Win32PrintService_getAllMediaTrayNames(JNIEnv *env,
                                                  jobject peer,
                                                  jstring printer,
                                                  jstring port)
{
  return getAllDCNames(env, peer, printer, port, DC_BINNAMES, TRAYNAME_LENGTH);
}


JNIEXPORT jint JNICALL
Java_sun_print_Win32PrintService_getCopiesSupported(JNIEnv *env,
                                                    jobject peer,
                                                    jstring printer,
                                                    jstring port)
{
  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return 1;
  }

  SAVE_CONTROLWORD
  int numCopies = ::DeviceCapabilities(printerName, printerPort,
                                       DC_COPIES,   NULL, NULL);
  RESTORE_CONTROLWORD

  if (numCopies == -1)
    return 1; // default

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);

  return numCopies;
}


/*
PostScript Drivers return wrong support info for the following code:

 DWORD dmFields = (::DeviceCapabilities(printerName,
                                         NULL, DC_FIELDS,   NULL, NULL)) ;

  if ((dmFields & DM_YRESOLUTION) )
    isSupported = true;

Returns not supported even if it supports resolution. Therefore, we use the
function _getAllResolutions.
*/
JNIEXPORT jintArray JNICALL
Java_sun_print_Win32PrintService_getAllResolutions(JNIEnv *env,
                                                   jobject peer,
                                                   jstring printer,
                                                   jstring port)
{
  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

 if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  SAVE_CONTROLWORD
  int nResolutions = ::DeviceCapabilities(printerName, printerPort,
                                          DC_ENUMRESOLUTIONS, NULL, NULL);
  RESTORE_CONTROLWORD

  jintArray resolutionArray = NULL;
  if (nResolutions > 0) {
    resolutionArray = env->NewIntArray(nResolutions*2);
    if (resolutionArray != NULL) {
        jint *jpcIndices = env->GetIntArrayElements(resolutionArray, NULL);
        if (jpcIndices != NULL) {
            jint *saveFormats = jpcIndices;
            LPTSTR resBuf = NULL;
            try {
                resBuf = (LPTSTR)new char[nResolutions * sizeof(LONG) * 2];
            } catch (std::bad_alloc&) {
                resBuf = NULL;
            }
            if (resBuf != NULL) {
                if (::DeviceCapabilities(printerName, printerPort,
                                         DC_ENUMRESOLUTIONS, resBuf,
                                         NULL) != -1) {
                    LONG *pResolution = (LONG *)resBuf;
                    for (int i = 0; i < nResolutions; i++) {
                        jpcIndices[i*2] = *pResolution++;
                        jpcIndices[i*2+1] = *pResolution++;
                    }
                }
                RESTORE_CONTROLWORD
                delete[] resBuf;
            }
            env->ReleaseIntArrayElements(resolutionArray, saveFormats, 0);
        }
    }
  }

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);
  return resolutionArray;
}


static BOOL IsDCPostscript( HDC hDC )
{
    int         nEscapeCode;
    CHAR        szTechnology[MAX_PATH] = "";

    // If it supports POSTSCRIPT_PASSTHROUGH, it must be PS.
    nEscapeCode = POSTSCRIPT_PASSTHROUGH;
    if( ::ExtEscape( hDC, QUERYESCSUPPORT, sizeof(int),
                     (LPCSTR)&nEscapeCode, 0, NULL ) > 0 )
        return TRUE;

    // If it doesn't support GETTECHNOLOGY, we won't be able to tell.
    nEscapeCode = GETTECHNOLOGY;
    if( ::ExtEscape( hDC, QUERYESCSUPPORT, sizeof(int),
                     (LPCSTR)&nEscapeCode, 0, NULL ) <= 0 )
        return FALSE;

    // Get the technology string and check if the word "postscript" is in it.
    if( ::ExtEscape( hDC, GETTECHNOLOGY, 0, NULL, MAX_PATH,
                     (LPSTR)szTechnology ) <= 0 )
        return FALSE;
    _strupr_s(szTechnology, MAX_PATH);
    if(!strstr( szTechnology, "POSTSCRIPT" ) == NULL )
        return TRUE;

    // The word "postscript" was not found and it didn't support
    //   POSTSCRIPT_PASSTHROUGH, so it's not a PS printer.
        return FALSE;
}


JNIEXPORT jstring JNICALL
Java_sun_print_Win32PrintService_getPrinterPort(JNIEnv *env,
                                                jobject peer,
                                                jstring printer)
{

  if (printer == NULL) {
    return NULL;
  }

  jstring jPort;
  LPTSTR printerName = NULL, printerPort = TEXT("LPT1");
  LPBYTE buffer = NULL;
  DWORD cbBuf = 0;

  try {
    VERIFY(AwtPrintControl::FindPrinter(NULL, NULL, &cbBuf, NULL, NULL));
    buffer = new BYTE[cbBuf];
    AwtPrintControl::FindPrinter(printer, buffer, &cbBuf,
                                      &printerName, &printerPort);
  } catch (std::bad_alloc&) {
    delete [] buffer;
    JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
    return NULL;
  }

  if (printerPort == NULL) {
    printerPort = TEXT("LPT1");
  }
  jPort = JNU_NewStringPlatform(env, printerPort);
  delete [] buffer;
  return jPort;

}


JNIEXPORT jint JNICALL
Java_sun_print_Win32PrintService_getCapabilities(JNIEnv *env,
                                                 jobject peer,
                                                 jstring printer,
                                                 jstring port)
{
  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  // 0x1000 is a flag to indicate that getCapabilities has already been called.
  // 0x0001 is a flag for color support and supported is the default.
  jint ret = 0x1001;
  DWORD dmFields;

  // get Duplex
  SAVE_CONTROLWORD
  DWORD isDuplex = (::DeviceCapabilities(printerName, printerPort,
                                         DC_DUPLEX,   NULL, NULL)) ;

  /*
    Check if duplexer is installed either physically or manually thru the
    printer setting dialog by checking if DM_DUPLEX is set.
  */
  dmFields = (::DeviceCapabilities(printerName, printerPort,
                                   DC_FIELDS,   NULL, NULL)) ;

  if ((dmFields & DM_DUPLEX) && isDuplex) {
      ret |= 0x0002;
  }

  // get Collation
  if ((dmFields & DM_COLLATE) ) {
      ret |= 0x0004;
  }

  // get Print Quality
  if ((dmFields & DM_PRINTQUALITY) ) {
      ret |= 0x0008;
  }

  HDC pdc = CreateDC(TEXT("WINSPOOL"), printerName, NULL, NULL);
  if (pdc != NULL) {
      // get Color
      int bpp = GetDeviceCaps(pdc, BITSPIXEL);
      int nColors = GetDeviceCaps(pdc, NUMCOLORS);

      if (!(dmFields & DM_COLOR) || ((bpp == 1)
                                     && ((nColors == 2) || (nColors == 256)))) {
          ret &= ~0x0001;
      }

      // check support for PostScript
      if (IsDCPostscript(pdc)) {
            ret |= 0x0010;
      }

      DeleteDC(pdc);
  }

  RESTORE_CONTROLWORD
  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);
  return ret;
}


#define GETDEFAULT_ERROR        -50
#define NDEFAULT 9

JNIEXPORT jintArray JNICALL
Java_sun_print_Win32PrintService_getDefaultSettings(JNIEnv *env,
                                                    jobject peer,
                                                    jstring printer,
                                                    jstring port)
{
  HANDLE      hPrinter;
  LPDEVMODE   pDevMode = NULL;

  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  LPTSTR printerPort = (LPTSTR)JNU_GetStringPlatformChars(env, port, NULL);

  if (printerName == NULL || printerPort == NULL) {
      if (printerName != NULL) {
          JNU_ReleaseStringPlatformChars(env, printer, printerName);
      }
      if (printerPort != NULL) {
          JNU_ReleaseStringPlatformChars(env, port, printerPort);
      }
      return NULL;
  }

  jint* defIndices = NULL;
  jintArray defaultArray = env->NewIntArray(NDEFAULT);
  if (defaultArray != NULL) {
      defIndices = env->GetIntArrayElements(defaultArray, NULL);
  }
  if (defIndices == NULL) {
      JNU_ReleaseStringPlatformChars(env, printer, printerName);
      JNU_ReleaseStringPlatformChars(env, port, printerPort);
      return NULL;
  }

  jint *saveFormats = defIndices;

  for (int i=0; i < NDEFAULT; i++) {
      defIndices[i] = GETDEFAULT_ERROR;
  }

  /* Start by opening the printer */
  if (!::OpenPrinter(printerName, &hPrinter, NULL)) {
      env->ReleaseIntArrayElements(defaultArray, saveFormats, 0);
      JNU_ReleaseStringPlatformChars(env, printer, printerName);
      JNU_ReleaseStringPlatformChars(env, port, printerPort);
      return defaultArray;
  }

  if (!AwtPrintControl::getDevmode(hPrinter, printerName, &pDevMode)) {
      /* if failure, cleanup and return failure */
      if (pDevMode != NULL) {
          ::GlobalFree(pDevMode);
      }
      ::ClosePrinter(hPrinter);
      env->ReleaseIntArrayElements(defaultArray, saveFormats, 0);
      JNU_ReleaseStringPlatformChars(env, printer, printerName);
      JNU_ReleaseStringPlatformChars(env, port, printerPort);
      return defaultArray;
  }

  /* Have seen one driver which reports a default paper id which is not
   * one of their supported paper ids. If what is returned is not
   * a supported paper, use one of the supported sizes instead.
   *
   */
  if (pDevMode->dmFields & DM_PAPERSIZE) {
      defIndices[0] = pDevMode->dmPaperSize;

      SAVE_CONTROLWORD

      int numSizes = ::DeviceCapabilities(printerName, printerPort,
                                          DC_PAPERS, NULL, NULL);
      if (numSizes > 0) {
          LPTSTR papers;
          try {
              papers = (LPTSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, numSizes, sizeof(WORD));
          } catch (const std::bad_alloc&) {
              papers = NULL;
          }

          if (papers != NULL &&
              ::DeviceCapabilities(printerName, printerPort,
                                   DC_PAPERS, papers, NULL) != -1) {
              int present = 0;
              for (int i=0;i<numSizes;i++) {
                  if (papers[i] == pDevMode->dmPaperSize) {
                      present = 1;
                  }
              }
              if (!present) {
                  defIndices[0] = papers[0];
              }
          }
          // If DeviceCapabilities fails, then also free paper allocation
          if (papers != NULL) {
              free((char*)papers);
          }
      }
      RESTORE_CONTROLWORD
  }

  if (pDevMode->dmFields & DM_MEDIATYPE) {
      defIndices[1] = pDevMode->dmMediaType;
  }

  /*
   * For some printer like Brother HL-2240D series
   * pDevMode->dmYResolution is not set in pDevMode->dmFields
   * even though pDevMode->dmYResolution is populated
   * via ::DocumentProperties API, so for this case
   * we populate the resolution index in default array
   */
  if (pDevMode->dmFields & DM_YRESOLUTION || pDevMode->dmYResolution > 0) {
      defIndices[2]  = pDevMode->dmYResolution;
  }

  /*
   * For some printer like Brother HL-2240D series
   * pDevMode->dmPrintQuality is not set in pDevMode->dmFields
   * even though pDevMode->dmPrintQuality is populated
   * via ::DocumentProperties API, so for this case
   * we populate the print quality index in default array
   */
  if (pDevMode->dmFields & DM_PRINTQUALITY || pDevMode->dmPrintQuality != 0) {
      defIndices[3] = pDevMode->dmPrintQuality;
  }

  if (pDevMode->dmFields & DM_COPIES) {
      defIndices[4] = pDevMode->dmCopies;
  }

  if (pDevMode->dmFields & DM_ORIENTATION) {
      defIndices[5] = pDevMode->dmOrientation;
  }

  if (pDevMode->dmFields & DM_DUPLEX) {
      defIndices[6] = pDevMode->dmDuplex;
  }

  if (pDevMode->dmFields & DM_COLLATE) {
      defIndices[7] = pDevMode->dmCollate;
  }

  if (pDevMode->dmFields & DM_COLOR) {
      defIndices[8] = pDevMode->dmColor;
  }

  GlobalFree(pDevMode);
  ::ClosePrinter(hPrinter);

  env->ReleaseIntArrayElements(defaultArray, saveFormats, 0);

  JNU_ReleaseStringPlatformChars(env, printer, printerName);
  JNU_ReleaseStringPlatformChars(env, port, printerPort);

  return defaultArray;
}


JNIEXPORT jint JNICALL
Java_sun_print_Win32PrintService_getJobStatus(JNIEnv *env,
                                          jobject peer,
                                          jstring printer,
                                          jint type)
{
    HANDLE hPrinter;
    DWORD  cByteNeeded;
    DWORD  cByteUsed;
    PRINTER_INFO_2 *pPrinterInfo = NULL;
    int ret=0;

    LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
    if (printerName == NULL) {
        return -1;
    }

    // Start by opening the printer
    if (!::OpenPrinter(printerName, &hPrinter, NULL)) {
        JNU_ReleaseStringPlatformChars(env, printer, printerName);
        return -1;
    }

    if (!::GetPrinter(hPrinter, 2, NULL, 0, &cByteNeeded)) {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
            ::ClosePrinter(hPrinter);
            JNU_ReleaseStringPlatformChars(env, printer, printerName);
            return -1;
        }
    }

    pPrinterInfo = (PRINTER_INFO_2 *)::GlobalAlloc(GPTR, cByteNeeded);
    if (!(pPrinterInfo)) {
        /* failure to allocate memory */
        ::ClosePrinter(hPrinter);
        JNU_ReleaseStringPlatformChars(env, printer, printerName);
        return -1;
    }

    /* get the printer info */
    if (!::GetPrinter(hPrinter,
                      2,
                      (LPBYTE)pPrinterInfo,
                      cByteNeeded,
                      &cByteUsed))
        {
            /* failure to access the printer */
            ::GlobalFree(pPrinterInfo);
            pPrinterInfo = NULL;
            ::ClosePrinter(hPrinter);
            JNU_ReleaseStringPlatformChars(env, printer, printerName);
            return -1;
        }

    if (type == GETJOBCOUNT) {
        ret = pPrinterInfo->cJobs;
    } else if (type == ACCEPTJOB) {
        if (pPrinterInfo->Status & PRINTER_STATUS_PENDING_DELETION) {
            ret = 0;
        }
        else {
            ret = 1;
        }
    }

    ::GlobalFree(pPrinterInfo);
    ::ClosePrinter(hPrinter);
    JNU_ReleaseStringPlatformChars(env, printer, printerName);
    return ret;
}


static jfieldID getIdOfLongField(JNIEnv *env, jobject self,
                                 const char *fieldName) {
  jclass myClass = env->GetObjectClass(self);
  jfieldID fieldId = env->GetFieldID(myClass, fieldName, "J");
  DASSERT(fieldId != 0);
  return fieldId;
}


static inline HANDLE getHPrinter(JNIEnv *env, jobject self) {
  jfieldID fieldId = getIdOfLongField(env, self, HPRINTER_STR);
  if (fieldId == (jfieldID)0) {
      return (HANDLE)NULL;
  }
  return (HANDLE)(env->GetLongField(self, fieldId));
}


JNIEXPORT jboolean JNICALL
Java_sun_print_Win32PrintJob_startPrintRawData(JNIEnv *env,
                                               jobject peer,
                                               jstring printer,
                                               jstring jobname)
{
  HANDLE      hPrinter;
  DOC_INFO_1  DocInfo;
  LPTSTR printerName = (LPTSTR)JNU_GetStringPlatformChars(env, printer, NULL);
  if (printerName == NULL) {
      return false;
  }
  DASSERT(jobname != NULL);
  LPTSTR lpJobName = (LPTSTR)JNU_GetStringPlatformChars(env, jobname, NULL);
  LPTSTR jname = _tcsdup(lpJobName);
  JNU_ReleaseStringPlatformChars(env, jobname, lpJobName);

  // Start by opening the printer
  if (!::OpenPrinter(printerName, &hPrinter, NULL)) {
    JNU_ReleaseStringPlatformChars(env, printer, printerName);
    free((LPTSTR)jname);
    return false;
  }

  JNU_ReleaseStringPlatformChars(env, printer, printerName);

  // Fill in the structure with info about this "document."
  DocInfo.pDocName = jname;
  DocInfo.pOutputFile = NULL;
  DocInfo.pDatatype = TEXT("RAW");

  // Inform the spooler the document is beginning.
  if( (::StartDocPrinter(hPrinter, 1, (LPBYTE)&DocInfo)) == 0 ) {
    ::ClosePrinter( hPrinter );
    free((LPTSTR)jname);
    return false;
  }

  free((LPTSTR)jname);

  // Start a page.
  if( ! ::StartPagePrinter( hPrinter ) ) {
    ::EndDocPrinter( hPrinter );
    ::ClosePrinter( hPrinter );
    return false;
  }

  // store handle
  jfieldID fieldId = getIdOfLongField(env, peer, HPRINTER_STR);
  if (fieldId == (jfieldID)0) {
      return false;
  } else {
      env->SetLongField(peer, fieldId, reinterpret_cast<jlong>(hPrinter));
      return true;
  }
}


JNIEXPORT jboolean JNICALL
Java_sun_print_Win32PrintJob_printRawData(JNIEnv *env,
                                          jobject peer,
                                          jbyteArray dataArray,
                                          jint count)
{
  jboolean  ret=true;
  jint      dwBytesWritten;
  jbyte*    data = NULL;

  // retrieve handle
  HANDLE    hPrinter = getHPrinter(env, peer);
  if (hPrinter == NULL) {
    return false;
  }

  try {
    data=(jbyte *)env->GetPrimitiveArrayCritical(dataArray, 0);
    if (data == NULL) {
        return false;
    }

    // Send the data to the printer.
    if( ! ::WritePrinter(hPrinter, data, count,(LPDWORD)&dwBytesWritten)) {
      env->ReleasePrimitiveArrayCritical(dataArray, data, 0);
      return false;
    }

    // Check to see if correct number of bytes were written.
    if( dwBytesWritten != count ) {
      ret = false;
    }

  } catch (...) {
    if (data != NULL) {
      env->ReleasePrimitiveArrayCritical(dataArray, data, 0);
    }
    JNU_ThrowInternalError(env, "Problem in Win32PrintJob_printRawData");
    return false;
  }

  env->ReleasePrimitiveArrayCritical(dataArray, data, 0);
  return ret;
}


JNIEXPORT jboolean JNICALL
Java_sun_print_Win32PrintJob_endPrintRawData(JNIEnv *env,
                                          jobject peer)
{
  // retrieve handle
  HANDLE hPrinter = getHPrinter(env, peer);
  if (hPrinter == NULL) {
    return false;
  }

  if ((::EndPagePrinter(hPrinter) != 0) &&
      (::EndDocPrinter(hPrinter) != 0) &&
      (::ClosePrinter(hPrinter) != 0)) {
    return true;
  } else {
    return false;
  }
}

} /* extern "C" */
