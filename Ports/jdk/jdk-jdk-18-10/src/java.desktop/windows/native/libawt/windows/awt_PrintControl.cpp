/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Component.h"
#include "awt_PrintControl.h"
#include "awt.h"
#include "awt_PrintDialog.h"
#include <winspool.h>
#include <float.h>
#include <math.h>

#define ROUNDTOINT(x) ((int)((x)+0.5))
static const int DEFAULT_RES = 72;
static const double TENTHS_MM_TO_POINTS = 3.527777778;
static const double LOMETRIC_TO_POINTS = (72.0 / 254.0);


/* Values must match those defined in WPrinterJob.java */
static const DWORD SET_COLOR = 0x00000200;
static const DWORD SET_ORIENTATION = 0x00004000;
static const DWORD SET_DUP_VERTICAL = 0x00000010;
static const DWORD SET_DUP_HORIZONTAL = 0x00000020;
static const DWORD SET_RES_HIGH = 0x00000040;
static const DWORD SET_RES_LOW = 0x00000080;


/* These methods and fields are on sun.awt.windows.WPrinterJob */
jfieldID  AwtPrintControl::dialogOwnerPeerID;
jmethodID AwtPrintControl::getPrintDCID;
jmethodID AwtPrintControl::setPrintDCID;
jmethodID AwtPrintControl::getDevmodeID;
jmethodID AwtPrintControl::setDevmodeID;
jmethodID AwtPrintControl::getDevnamesID;
jmethodID AwtPrintControl::setDevnamesID;
jmethodID AwtPrintControl::getParentWindowID;
jfieldID  AwtPrintControl::driverDoesMultipleCopiesID;
jfieldID  AwtPrintControl::driverDoesCollationID;
jmethodID AwtPrintControl::getWin32MediaID;
jmethodID AwtPrintControl::setWin32MediaID;
jmethodID AwtPrintControl::getWin32MediaTrayID;
jmethodID AwtPrintControl::setWin32MediaTrayID;
jmethodID AwtPrintControl::getColorID;
jmethodID AwtPrintControl::getCopiesID;
jmethodID AwtPrintControl::getSelectID;
jmethodID AwtPrintControl::getDestID;
jmethodID AwtPrintControl::getDialogID;
jmethodID AwtPrintControl::getFromPageID;
jmethodID AwtPrintControl::getMaxPageID;
jmethodID AwtPrintControl::getMinPageID;
jmethodID AwtPrintControl::getCollateID;
jmethodID AwtPrintControl::getOrientID;
jmethodID AwtPrintControl::getQualityID;
jmethodID AwtPrintControl::getPrintToFileEnabledID;
jmethodID AwtPrintControl::getPrinterID;
jmethodID AwtPrintControl::setPrinterID;
jmethodID AwtPrintControl::getResID;
jmethodID AwtPrintControl::getSidesID;
jmethodID AwtPrintControl::getToPageID;
jmethodID AwtPrintControl::setToPageID;
jmethodID AwtPrintControl::setNativeAttID;
jmethodID AwtPrintControl::setRangeCopiesID;
jmethodID AwtPrintControl::setResID;
jmethodID AwtPrintControl::setJobAttributesID;


BOOL AwtPrintControl::IsSupportedLevel(HANDLE hPrinter, DWORD dwLevel) {
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

BOOL AwtPrintControl::FindPrinter(jstring printerName, LPBYTE pPrinterEnum,
                                  LPDWORD pcbBuf, LPTSTR * foundPrinter,
                                  LPTSTR * foundPort)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    DWORD cReturned = 0;

    if (pPrinterEnum == NULL) {
        // Compute size of buffer
        DWORD cbNeeded = 0;
        ::EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS,
                           NULL, 2, NULL, 0, &cbNeeded, &cReturned);
        ::EnumPrinters(PRINTER_ENUM_LOCAL,
                       NULL, 5, NULL, 0, pcbBuf, &cReturned);
        if (cbNeeded > (*pcbBuf)) {
            *pcbBuf = cbNeeded;
        }
        return TRUE;
    }

    DASSERT(printerName != NULL);

    DWORD cbBuf = *pcbBuf, dummyWord = 0;

    JavaStringBuffer printerNameBuf(env, printerName);
    LPCTSTR lpcPrinterName = (LPCTSTR)printerNameBuf;
    DASSERT(lpcPrinterName != NULL);

    // For NT, first do a quick check of all remote and local printers.
    // This only allows us to search by name, though. PRINTER_INFO_4
    // doesn't support port searches. So, if the user has specified the
    // printer name as "LPT1:" (even though this is actually a port
    // name), we won't find the printer here.
    if (!::EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS,
                        NULL, 4, pPrinterEnum, cbBuf, &dummyWord, &cReturned)) {
        return FALSE;
    }

    for (DWORD i = 0; i < cReturned; i++) {
        PRINTER_INFO_4 *info4 = (PRINTER_INFO_4 *)
            (pPrinterEnum + i * sizeof(PRINTER_INFO_4));
        if (info4->pPrinterName != NULL &&
            _tcsicmp(lpcPrinterName, info4->pPrinterName) == 0) {

            // Fix for BugTraq Id 4281380.
            // Get the port name since some drivers may require
            // this name to be passed to ::DeviceCapabilities().
            HANDLE hPrinter = NULL;
            if (::OpenPrinter(info4->pPrinterName, &hPrinter, NULL)) {
                // Fix for BugTraq Id 4286812.
                // Some drivers don't support PRINTER_INFO_5.
                // In this case we try PRINTER_INFO_2, and if that
                // isn't supported as well return NULL port name.
                try {
                    if (AwtPrintControl::IsSupportedLevel(hPrinter, 5)) {
                        VERIFY(::GetPrinter(hPrinter, 5, pPrinterEnum, cbBuf,
                                            &dummyWord));
                        PRINTER_INFO_5 *info5 = (PRINTER_INFO_5 *)pPrinterEnum;
                        *foundPrinter = info5->pPrinterName;
                        // pPortName may specify multiple ports. We only want one.
                        *foundPort = (info5->pPortName != NULL)
                            ? _tcstok(info5->pPortName, TEXT(",")) : NULL;
                    } else if (AwtPrintControl::IsSupportedLevel(hPrinter, 2)) {
                        VERIFY(::GetPrinter(hPrinter, 2, pPrinterEnum, cbBuf,
                                            &dummyWord));
                        PRINTER_INFO_2 *info2 = (PRINTER_INFO_2 *)pPrinterEnum;
                        *foundPrinter = info2->pPrinterName;
                        // pPortName may specify multiple ports. We only want one.
                        *foundPort = (info2->pPortName != NULL)
                            ? _tcstok(info2->pPortName, TEXT(",")) : NULL;
                    } else {
                        *foundPrinter = info4->pPrinterName;
                        // We failed to determine port name for the found printer.
                        *foundPort = NULL;
                    }
                } catch (std::bad_alloc&) {
                    VERIFY(::ClosePrinter(hPrinter));
                    throw;
                }

                VERIFY(::ClosePrinter(hPrinter));

                return TRUE;
            }

            return FALSE;
        }
    }

    // We still haven't found the printer, /* or we're using 95/98. */
    // PRINTER_INFO_5 supports both printer name and port name, so
    // we'll test both. On NT, PRINTER_ENUM_LOCAL means just local
    // printers. This is what we want, because we already tested all
    // remote printer names above (and remote printer port names are
    // the same as remote printer names). On 95/98, PRINTER_ENUM_LOCAL
    // means both remote and local printers. This is also what we want
    // because we haven't tested any printers yet.
    if (!::EnumPrinters(PRINTER_ENUM_LOCAL,
                        NULL, 5, pPrinterEnum, cbBuf, &dummyWord, &cReturned)) {
        return FALSE;
    }

    for (DWORD i = 0; i < cReturned; i++) {
        PRINTER_INFO_5 *info5 = (PRINTER_INFO_5 *)
            (pPrinterEnum + i * sizeof(PRINTER_INFO_5));
        // pPortName can specify multiple ports. Test them one at
        // a time.
        if (info5->pPortName != NULL) {
            LPTSTR port = _tcstok(info5->pPortName, TEXT(","));
            while (port != NULL) {
                if (_tcsicmp(lpcPrinterName, port) == 0) {
                    *foundPrinter = info5->pPrinterName;
                    *foundPort = port;
                    return TRUE;
                }
                port = _tcstok(NULL, TEXT(","));
            }
        }
    }

    return FALSE;
}


void AwtPrintControl::initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    jclass cls = env->FindClass("sun/awt/windows/WPrinterJob");
    CHECK_NULL(cls);

    AwtPrintControl::dialogOwnerPeerID =
      env->GetFieldID(cls, "dialogOwnerPeer", "Ljava/awt/peer/ComponentPeer;");
    DASSERT(AwtPrintControl::dialogOwnerPeerID != NULL);
    CHECK_NULL(AwtPrintControl::dialogOwnerPeerID);

    AwtPrintControl::getParentWindowID = env->GetMethodID(cls,
                                       "getParentWindowID", "()J");
    DASSERT(AwtPrintControl::getParentWindowID != NULL);
    CHECK_NULL(AwtPrintControl::getParentWindowID);

    AwtPrintControl::getPrintDCID = env->GetMethodID(cls, "getPrintDC", "()J");
    DASSERT(AwtPrintControl::getPrintDCID != NULL);
    CHECK_NULL(AwtPrintControl::getPrintDCID);

    AwtPrintControl::setPrintDCID =
        env->GetMethodID(cls, "setPrintDC", "(J)V");
    DASSERT(AwtPrintControl::setPrintDCID != NULL);
    CHECK_NULL(AwtPrintControl::setPrintDCID);

    AwtPrintControl::getDevmodeID = env->GetMethodID(cls, "getDevMode", "()J");
    DASSERT(AwtPrintControl::getDevmodeID != NULL);
    CHECK_NULL(AwtPrintControl::getDevmodeID);

    AwtPrintControl::setDevmodeID =
        env->GetMethodID(cls, "setDevMode", "(J)V");
    DASSERT(AwtPrintControl::setDevmodeID != NULL);
    CHECK_NULL(AwtPrintControl::setDevmodeID);

    AwtPrintControl::getDevnamesID =
        env->GetMethodID(cls, "getDevNames", "()J");
    DASSERT(AwtPrintControl::getDevnamesID != NULL);
    CHECK_NULL(AwtPrintControl::getDevnamesID);

    AwtPrintControl::setDevnamesID =
        env->GetMethodID(cls, "setDevNames", "(J)V");
    DASSERT(AwtPrintControl::setDevnamesID != NULL);
    CHECK_NULL(AwtPrintControl::setDevnamesID);

    AwtPrintControl::driverDoesMultipleCopiesID =
      env->GetFieldID(cls, "driverDoesMultipleCopies", "Z");
    DASSERT(AwtPrintControl::driverDoesMultipleCopiesID != NULL);
    CHECK_NULL(AwtPrintControl::driverDoesMultipleCopiesID);

    AwtPrintControl::driverDoesCollationID =
      env->GetFieldID(cls, "driverDoesCollation", "Z");
    DASSERT(AwtPrintControl::driverDoesCollationID != NULL);
    CHECK_NULL(AwtPrintControl::driverDoesCollationID);

    AwtPrintControl::getCopiesID =
      env->GetMethodID(cls, "getCopiesAttrib", "()I");
    DASSERT(AwtPrintControl::getCopiesID != NULL);
    CHECK_NULL(AwtPrintControl::getCopiesID);

    AwtPrintControl::getCollateID =
      env->GetMethodID(cls, "getCollateAttrib","()I");
    DASSERT(AwtPrintControl::getCollateID != NULL);
    CHECK_NULL(AwtPrintControl::getCollateID);

    AwtPrintControl::getOrientID =
      env->GetMethodID(cls, "getOrientAttrib", "()I");
    DASSERT(AwtPrintControl::getOrientID != NULL);
    CHECK_NULL(AwtPrintControl::getOrientID);

    AwtPrintControl::getFromPageID =
      env->GetMethodID(cls, "getFromPageAttrib", "()I");
    DASSERT(AwtPrintControl::getFromPageID != NULL);
    CHECK_NULL(AwtPrintControl::getFromPageID);

    AwtPrintControl::getToPageID =
      env->GetMethodID(cls, "getToPageAttrib", "()I");
    DASSERT(AwtPrintControl::getToPageID != NULL);
    CHECK_NULL(AwtPrintControl::getToPageID);

    AwtPrintControl::getMinPageID =
      env->GetMethodID(cls, "getMinPageAttrib", "()I");
    DASSERT(AwtPrintControl::getMinPageID != NULL);
    CHECK_NULL(AwtPrintControl::getMinPageID);

    AwtPrintControl::getMaxPageID =
      env->GetMethodID(cls, "getMaxPageAttrib", "()I");
    DASSERT(AwtPrintControl::getMaxPageID != NULL);
    CHECK_NULL(AwtPrintControl::getMaxPageID);

    AwtPrintControl::getDestID =
      env->GetMethodID(cls, "getDestAttrib", "()Z");
    DASSERT(AwtPrintControl::getDestID != NULL);
    CHECK_NULL(AwtPrintControl::getDestID);

    AwtPrintControl::getQualityID =
      env->GetMethodID(cls, "getQualityAttrib", "()I");
    DASSERT(AwtPrintControl::getQualityID != NULL);
    CHECK_NULL(AwtPrintControl::getQualityID);

    AwtPrintControl::getColorID =
      env->GetMethodID(cls, "getColorAttrib", "()I");
    DASSERT(AwtPrintControl::getColorID != NULL);
    CHECK_NULL(AwtPrintControl::getColorID);

    AwtPrintControl::getSidesID =
      env->GetMethodID(cls, "getSidesAttrib", "()I");
    DASSERT(AwtPrintControl::getSidesID != NULL);
    CHECK_NULL(AwtPrintControl::getSidesID);

    AwtPrintControl::getPrinterID =
      env->GetMethodID(cls, "getPrinterAttrib", "()Ljava/lang/String;");
    DASSERT(AwtPrintControl::getPrinterID != NULL);
    CHECK_NULL(AwtPrintControl::getPrinterID);

    AwtPrintControl::getWin32MediaID =
        env->GetMethodID(cls, "getWin32MediaAttrib", "()[I");
    DASSERT(AwtPrintControl::getWin32MediaID != NULL);
    CHECK_NULL(AwtPrintControl::getWin32MediaID);

    AwtPrintControl::setWin32MediaID =
      env->GetMethodID(cls, "setWin32MediaAttrib", "(III)V");
    DASSERT(AwtPrintControl::setWin32MediaID != NULL);
    CHECK_NULL(AwtPrintControl::setWin32MediaID);

    AwtPrintControl::getWin32MediaTrayID =
        env->GetMethodID(cls, "getMediaTrayAttrib", "()I");
    DASSERT(AwtPrintControl::getWin32MediaTrayID != NULL);
    CHECK_NULL(AwtPrintControl::getWin32MediaTrayID);

    AwtPrintControl::setWin32MediaTrayID =
      env->GetMethodID(cls, "setMediaTrayAttrib", "(I)V");
    DASSERT(AwtPrintControl::setWin32MediaTrayID != NULL);
    CHECK_NULL(AwtPrintControl::setWin32MediaTrayID);

    AwtPrintControl::getSelectID =
      env->GetMethodID(cls, "getSelectAttrib", "()I");
    DASSERT(AwtPrintControl::getSelectID != NULL);
    CHECK_NULL(AwtPrintControl::getSelectID);

    AwtPrintControl::getPrintToFileEnabledID =
      env->GetMethodID(cls, "getPrintToFileEnabled", "()Z");
    DASSERT(AwtPrintControl::getPrintToFileEnabledID != NULL);
    CHECK_NULL(AwtPrintControl::getPrintToFileEnabledID);

    AwtPrintControl::setNativeAttID =
      env->GetMethodID(cls, "setNativeAttributes", "(III)V");
    DASSERT(AwtPrintControl::setNativeAttID != NULL);
    CHECK_NULL(AwtPrintControl::setNativeAttID);

    AwtPrintControl::setRangeCopiesID =
      env->GetMethodID(cls, "setRangeCopiesAttribute", "(IIZI)V");
    DASSERT(AwtPrintControl::setRangeCopiesID != NULL);
    CHECK_NULL(AwtPrintControl::setRangeCopiesID);

    AwtPrintControl::setResID =
      env->GetMethodID(cls, "setResolutionDPI", "(II)V");
    DASSERT(AwtPrintControl::setResID != NULL);
    CHECK_NULL(AwtPrintControl::setResID);

    AwtPrintControl::setPrinterID =
      env->GetMethodID(cls, "setPrinterNameAttrib", "(Ljava/lang/String;)V");
    DASSERT(AwtPrintControl::setPrinterID != NULL);
    CHECK_NULL(AwtPrintControl::setPrinterID);

    AwtPrintControl::setJobAttributesID =
        env->GetMethodID(cls, "setJobAttributes",
        "(Ljavax/print/attribute/PrintRequestAttributeSet;IISSSSSSS)V");
    DASSERT(AwtPrintControl::setJobAttributesID != NULL);
    CHECK_NULL(AwtPrintControl::setJobAttributesID);

    CATCH_BAD_ALLOC;
}

BOOL CALLBACK PrintDlgHook(HWND hDlg, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
    TRY;

    if (iMsg == WM_INITDIALOG) {
        SetForegroundWindow(hDlg);
        return FALSE;
    }
    return FALSE;

    CATCH_BAD_ALLOC_RET(TRUE);
}

BOOL AwtPrintControl::CreateDevModeAndDevNames(PRINTDLG *ppd,
                                               LPTSTR pPrinterName,
                                               LPTSTR pPortName)
{
    DWORD cbNeeded = 0;
    LPBYTE pPrinter = NULL;
    BOOL retval = FALSE;
    HANDLE hPrinter;

    try {
        if (!::OpenPrinter(pPrinterName, &hPrinter, NULL)) {
            goto done;
        }
        VERIFY(::GetPrinter(hPrinter, 2, NULL, 0, &cbNeeded) == 0);
        if (::GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
            goto done;
        }
        pPrinter = new BYTE[cbNeeded];
        if (!::GetPrinter(hPrinter, 2, pPrinter, cbNeeded, &cbNeeded)) {
            goto done;
        }
        PRINTER_INFO_2 *info2 = (PRINTER_INFO_2 *)pPrinter;

        // Create DEVMODE, if it exists.
        if (info2->pDevMode != NULL) {
            size_t devmodeSize =
                sizeof(DEVMODE) + info2->pDevMode->dmDriverExtra;
            ppd->hDevMode = ::GlobalAlloc(GHND, devmodeSize);
            if (ppd->hDevMode == NULL) {
                throw std::bad_alloc();
            }
            DEVMODE *devmode = (DEVMODE *)::GlobalLock(ppd->hDevMode);
            DASSERT(!::IsBadWritePtr(devmode, devmodeSize));
            memcpy(devmode, info2->pDevMode, devmodeSize);
            VERIFY(::GlobalUnlock(ppd->hDevMode) == 0);
            DASSERT(::GetLastError() == NO_ERROR);
        }

        // Create DEVNAMES.
        if (pPortName != NULL) {
            info2->pPortName = pPortName;
        } else if (info2->pPortName != NULL) {
            // pPortName may specify multiple ports. We only want one.
            info2->pPortName = _tcstok(info2->pPortName, TEXT(","));
        }

        size_t lenDriverName = ((info2->pDriverName != NULL)
                                    ? _tcslen(info2->pDriverName)
                                    : 0) + 1;
        size_t lenPrinterName = ((pPrinterName != NULL)
                                     ? _tcslen(pPrinterName)
                                     : 0) + 1;
        size_t lenOutputName = ((info2->pPortName != NULL)
                                    ? _tcslen(info2->pPortName)
                                    : 0) + 1;
        size_t devnameSize= sizeof(DEVNAMES) +
                        lenDriverName*sizeof(TCHAR) +
                        lenPrinterName*sizeof(TCHAR) +
                        lenOutputName*sizeof(TCHAR);

        ppd->hDevNames = ::GlobalAlloc(GHND, devnameSize);
        if (ppd->hDevNames == NULL) {
            throw std::bad_alloc();
        }

        DEVNAMES *devnames =
            (DEVNAMES *)::GlobalLock(ppd->hDevNames);
        DASSERT(!IsBadWritePtr(devnames, devnameSize));
        LPTSTR lpcDevnames = (LPTSTR)devnames;

        // note: all sizes are in characters, not in bytes
        devnames->wDriverOffset = sizeof(DEVNAMES)/sizeof(TCHAR);
        devnames->wDeviceOffset =
            static_cast<WORD>(sizeof(DEVNAMES)/sizeof(TCHAR) + lenDriverName);
        devnames->wOutputOffset =
            static_cast<WORD>(sizeof(DEVNAMES)/sizeof(TCHAR) + lenDriverName + lenPrinterName);
        if (info2->pDriverName != NULL) {
            _tcscpy_s(lpcDevnames + devnames->wDriverOffset, devnameSize - devnames->wDriverOffset, info2->pDriverName);
        } else {
            *(lpcDevnames + devnames->wDriverOffset) = _T('\0');
        }
        if (pPrinterName != NULL) {
            _tcscpy_s(lpcDevnames + devnames->wDeviceOffset, devnameSize - devnames->wDeviceOffset, pPrinterName);
        } else {
            *(lpcDevnames + devnames->wDeviceOffset) = _T('\0');
        }
        if (info2->pPortName != NULL) {
            _tcscpy_s(lpcDevnames + devnames->wOutputOffset, devnameSize - devnames->wOutputOffset, info2->pPortName);
        } else {
            *(lpcDevnames + devnames->wOutputOffset) = _T('\0');
        }
        VERIFY(::GlobalUnlock(ppd->hDevNames) == 0);
        DASSERT(::GetLastError() == NO_ERROR);
    } catch (std::bad_alloc&) {
        if (ppd->hDevNames != NULL) {
            VERIFY(::GlobalFree(ppd->hDevNames) == NULL);
            ppd->hDevNames = NULL;
        }
        if (ppd->hDevMode != NULL) {
            VERIFY(::GlobalFree(ppd->hDevMode) == NULL);
            ppd->hDevMode = NULL;
        }
        delete [] pPrinter;
        VERIFY(::ClosePrinter(hPrinter));
        hPrinter = NULL;
        throw;
    }

    retval = TRUE;

done:
    delete [] pPrinter;
    if (hPrinter) {
        VERIFY(::ClosePrinter(hPrinter));
        hPrinter = NULL;
    }

    return retval;
}


WORD AwtPrintControl::getNearestMatchingPaper(LPTSTR printer, LPTSTR port,
                                      double origWid, double origHgt,
                                      double* newWid, double *newHgt) {
    const double epsilon = 0.50;
    const double tolerance = (1.0 * 72.0);  // # inches * 72
    int numPaperSizes = 0;
    WORD *papers = NULL;
    POINT *paperSizes = NULL;

    if ((printer== NULL) || (port == NULL)) {
        return 0;
    }

    SAVE_CONTROLWORD
    numPaperSizes = (int)DeviceCapabilities(printer, port, DC_PAPERSIZE,
                                              NULL, NULL);

    if (numPaperSizes > 0) {
        papers = (WORD*)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, sizeof(WORD), numPaperSizes);
        paperSizes = (POINT *)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, sizeof(*paperSizes),
                                          numPaperSizes);

        DWORD result1 = DeviceCapabilities(printer, port,
                                       DC_PAPERS, (LPTSTR) papers, NULL);

        DWORD result2 = DeviceCapabilities(printer, port,
                                       DC_PAPERSIZE, (LPTSTR) paperSizes,
                                       NULL);

        // REMIND: cache in papers and paperSizes
        if (result1 == -1 || result2 == -1 ) {
            free((LPTSTR) papers);
            papers = NULL;
            free((LPTSTR) paperSizes);
            paperSizes = NULL;
        }
    }
    RESTORE_CONTROLWORD

    double closestWid = 0.0;
    double closestHgt = 0.0;
    WORD   closestMatch = 0;

    if (paperSizes != NULL) {

      /* Paper sizes are in 0.1mm units. Convert to 1/72"
       * For each paper size, compute the difference from the paper size
       * passed in. Use a least-squares difference, so paper much different
       * in x or y should score poorly
       */
        double diffw = origWid;
        double diffh = origHgt;
        double least_square = diffw * diffw + diffh * diffh;
        double tmp_ls;
        double widpts, hgtpts;

        for (int i=0;i<numPaperSizes;i++) {
            widpts = paperSizes[i].x * LOMETRIC_TO_POINTS;
            hgtpts = paperSizes[i].y * LOMETRIC_TO_POINTS;

            if ((fabs(origWid - widpts) < epsilon) &&
                (fabs(origHgt - hgtpts) < epsilon)) {
                closestWid = origWid;
                closestHgt = origHgt;
                closestMatch = papers[i];
                break;
            }

            diffw = fabs(widpts - origWid);
            diffh = fabs(hgtpts - origHgt);
            tmp_ls = diffw * diffw + diffh * diffh;
            if ((diffw < tolerance) && (diffh < tolerance) &&
                (tmp_ls < least_square)) {
                least_square = tmp_ls;
                closestWid = widpts;
                closestHgt = hgtpts;
                closestMatch = papers[i];
            }
        }
    }

    if (closestWid > 0) {
        *newWid = closestWid;
    }
    if (closestHgt > 0) {
        *newHgt = closestHgt;
    }

    if (papers != NULL) {
        free((LPTSTR)papers);
    }

    if (paperSizes != NULL) {
        free((LPTSTR)paperSizes);
    }

    return closestMatch;
}

/*
 * Copy settings into a print dialog & any devmode
 */
BOOL AwtPrintControl::InitPrintDialog(JNIEnv *env,
                                      jobject printCtrl, PRINTDLG &pd) {
    HWND hwndOwner = NULL;
    jobject dialogOwner =
        env->GetObjectField(printCtrl, AwtPrintControl::dialogOwnerPeerID);
    if (dialogOwner != NULL) {
        AwtComponent *dialogOwnerComp =
          (AwtComponent *)JNI_GET_PDATA(dialogOwner);

        hwndOwner = dialogOwnerComp->GetHWnd();
        env->DeleteLocalRef(dialogOwner);
        dialogOwner = NULL;
    }
    jobject mdh = NULL;
    jobject dest = NULL;
    jobject select = NULL;
    jobject dialog = NULL;
    LPTSTR printName = NULL;
    LPTSTR portName = NULL;

    // If the user didn't specify a printer, then this call returns the
    // name of the default printer.
    jstring printerName = (jstring)
      env->CallObjectMethod(printCtrl, AwtPrintControl::getPrinterID);

    if (printerName != NULL) {

        pd.hDevMode = AwtPrintControl::getPrintHDMode(env, printCtrl);
        pd.hDevNames = AwtPrintControl::getPrintHDName(env, printCtrl);

        LPTSTR getName = (LPTSTR)JNU_GetStringPlatformChars(env,
                                                      printerName, NULL);
        if (getName == NULL) {
            env->DeleteLocalRef(printerName);
            throw std::bad_alloc();
        }

        BOOL samePrinter = FALSE;

        // check if given printername is same as the currently saved printer
        if (pd.hDevNames != NULL ) {

            DEVNAMES *devnames = (DEVNAMES *)::GlobalLock(pd.hDevNames);
            if (devnames != NULL) {
                LPTSTR lpdevnames = (LPTSTR)devnames;
                printName = lpdevnames+devnames->wDeviceOffset;

                if (!_tcscmp(printName, getName)) {

                    samePrinter = TRUE;
                    printName = _tcsdup(lpdevnames+devnames->wDeviceOffset);
                    portName = _tcsdup(lpdevnames+devnames->wOutputOffset);

                }
            }
            ::GlobalUnlock(pd.hDevNames);
        }
        JNU_ReleaseStringPlatformChars(env, printerName, getName);

        if (!samePrinter) {
            LPTSTR foundPrinter = NULL;
            LPTSTR foundPort = NULL;
            DWORD cbBuf = 0;
            VERIFY(AwtPrintControl::FindPrinter(NULL, NULL, &cbBuf,
                                                NULL, NULL));
            LPBYTE buffer = new BYTE[cbBuf];

            if (AwtPrintControl::FindPrinter(printerName, buffer, &cbBuf,
                                             &foundPrinter, &foundPort) &&
                (foundPrinter != NULL) && (foundPort != NULL)) {

                printName = _tcsdup(foundPrinter);
                portName = _tcsdup(foundPort);

                if (!AwtPrintControl::CreateDevModeAndDevNames(&pd,
                                                   foundPrinter, foundPort)) {
                    delete [] buffer;
                    if (printName != NULL) {
                      free(printName);
                    }
                    if (portName != NULL) {
                      free(portName);
                    }
                    env->DeleteLocalRef(printerName);
                    return FALSE;
                }

                DASSERT(pd.hDevNames != NULL);
            } else {
                delete [] buffer;
                if (printName != NULL) {
                  free(printName);
                }
                if (portName != NULL) {
                  free(portName);
                }
                env->DeleteLocalRef(printerName);
                return FALSE;
            }

            delete [] buffer;
        }
        env->DeleteLocalRef(printerName);
        // PrintDlg may change the values of hDevMode and hDevNames so we
        // re-initialize our saved handles.
        AwtPrintControl::setPrintHDMode(env, printCtrl, NULL);
        AwtPrintControl::setPrintHDName(env, printCtrl, NULL);
    } else {

        // There is no default printer. This means that there are no
        // printers installed at all.

        if (printName != NULL) {
          free(printName);
        }
        if (portName != NULL) {
          free(portName);
        }
        // Returning TRUE means try to display the native print dialog
        // which will either display an error message or prompt the
        // user to install a printer.
        return TRUE;
    }

    // Now, set-up the struct for the real calls to ::PrintDlg and ::CreateDC

    pd.hwndOwner = hwndOwner;
    pd.Flags = PD_ENABLEPRINTHOOK | PD_RETURNDC | PD_USEDEVMODECOPIESANDCOLLATE;
    pd.lpfnPrintHook = (LPPRINTHOOKPROC)PrintDlgHook;

    pd.nFromPage = (WORD)env->CallIntMethod(printCtrl,
                                            AwtPrintControl::getFromPageID);
    pd.nToPage = (WORD)env->CallIntMethod(printCtrl,
                                          AwtPrintControl::getToPageID);
    pd.nMinPage = (WORD)env->CallIntMethod(printCtrl,
                                           AwtPrintControl::getMinPageID);
    jint maxPage = env->CallIntMethod(printCtrl,
                                      AwtPrintControl::getMaxPageID);

    jint selectType = env->CallIntMethod(printCtrl,
                                         AwtPrintControl::getSelectID);

    pd.nMaxPage = (maxPage <= (jint)((WORD)-1)) ? (WORD)maxPage : (WORD)-1;
    // In the event that the application displays the dialog before
    // installing a Printable, but sets a page range, then max page will be 1
    // since the default state of a PrinterJob is an empty "Book" Pageable.
    // Windows pops up an error dialog in such a case which isn't very
    // forthcoming about the exact problem.
    // So if we detect this fix up such a problem here.
    if (pd.nMinPage > pd.nFromPage) pd.nMinPage = pd.nFromPage;
    if (pd.nMaxPage < pd.nToPage) pd.nMaxPage = pd.nToPage;
    if (selectType != 0 && (pd.nFromPage > pd.nMinPage || pd.nToPage < pd.nMaxPage)) {
        if (selectType == PD_SELECTION) {
            pd.Flags |= PD_SELECTION;
        } else {
            pd.Flags |= PD_PAGENUMS;
        }
    }

    if (env->CallBooleanMethod(printCtrl,
                               AwtPrintControl::getDestID)) {
      pd.Flags |= PD_PRINTTOFILE;
    }

    // selectType identifies whether No selection (2D) or
    // SunPageSelection (AWT)
    if (selectType != 0) {
      pd.Flags |= selectType;
    }

    if (!env->CallBooleanMethod(printCtrl,
                                AwtPrintControl::getPrintToFileEnabledID)) {
      pd.Flags |= PD_DISABLEPRINTTOFILE;
    }

    if (pd.hDevMode != NULL) {
      DEVMODE *devmode = (DEVMODE *)::GlobalLock(pd.hDevMode);
      DASSERT(!IsBadWritePtr(devmode, sizeof(DEVMODE)));

      WORD copies = (WORD)env->CallIntMethod(printCtrl,
                                             AwtPrintControl::getCopiesID);
      if (copies > 0) {
          devmode->dmFields |= DM_COPIES;
          devmode->dmCopies = copies;
      }

      jint orient = env->CallIntMethod(printCtrl,
                                       AwtPrintControl::getOrientID);
      if (orient == 0) {  // PageFormat.LANDSCAPE == 0
        devmode->dmFields |= DM_ORIENTATION;
        devmode->dmOrientation = DMORIENT_LANDSCAPE;
      } else if (orient == 1) { // PageFormat.PORTRAIT == 1
        devmode->dmFields |= DM_ORIENTATION;
        devmode->dmOrientation = DMORIENT_PORTRAIT;
      }

      // -1 means unset, so we'll accept the printer default.
      int collate = env->CallIntMethod(printCtrl,
                                       AwtPrintControl::getCollateID);
      if (collate == 1) {
        devmode->dmFields |= DM_COLLATE;
        devmode->dmCollate = DMCOLLATE_TRUE;
      } else if (collate == 0) {
        devmode->dmFields |= DM_COLLATE;
        devmode->dmCollate = DMCOLLATE_FALSE;
      }

      int quality = env->CallIntMethod(printCtrl,
                                       AwtPrintControl::getQualityID);
      if (quality) {
        devmode->dmFields |= DM_PRINTQUALITY;
        devmode->dmPrintQuality = quality;
      }

      int color = env->CallIntMethod(printCtrl,
                                     AwtPrintControl::getColorID);
      if (color) {
        devmode->dmFields |= DM_COLOR;
        devmode->dmColor = color;
      }

      int sides = env->CallIntMethod(printCtrl,
                                     AwtPrintControl::getSidesID);
      if (sides) {
        devmode->dmFields |= DM_DUPLEX;
        devmode->dmDuplex = (int)sides;
      }

      jintArray obj = (jintArray)env->CallObjectMethod(printCtrl,
                                       AwtPrintControl::getWin32MediaID);
      jboolean isCopy;
      jint *wid_ht = env->GetIntArrayElements(obj,
                                              &isCopy);

      double newWid = 0.0, newHt = 0.0;
      if (wid_ht != NULL && wid_ht[0] != 0 && wid_ht[1] != 0) {
        devmode->dmFields |= DM_PAPERSIZE;
        devmode->dmPaperSize = AwtPrintControl::getNearestMatchingPaper(
                                             printName,
                                             portName,
                                             (double)wid_ht[0],
                                             (double)wid_ht[1],
                                             &newWid, &newHt);

      }
      env->ReleaseIntArrayElements(obj, wid_ht, 0);
      ::GlobalUnlock(pd.hDevMode);
      devmode = NULL;
    }

    if (printName != NULL) {
      free(printName);
    }
    if (portName != NULL) {
      free(portName);
    }

    return TRUE;
}


/*
 * Copy settings from print dialog & any devmode back into attributes
 * or properties.
 */
extern "C" {
extern void setCapabilities(JNIEnv *env, jobject WPrinterJob, HDC hdc);
}
BOOL AwtPrintControl::UpdateAttributes(JNIEnv *env,
                                       jobject printCtrl, PRINTDLG &pd) {

    DEVNAMES *devnames = NULL;
    DEVMODE *devmode = NULL;
    unsigned int copies = 1;
    DWORD pdFlags = pd.Flags;
    DWORD dmFields = 0, dmValues = 0;
    bool newDC = false;

    // This call ensures that default PrintService gets updated for the
    // case where initially, there weren't any printers.
    env->CallObjectMethod(printCtrl, AwtPrintControl::getPrinterID);

    if (pd.hDevMode != NULL) {
        devmode = (DEVMODE *)::GlobalLock(pd.hDevMode);
        DASSERT(!IsBadReadPtr(devmode, sizeof(DEVMODE)));
    }

    if (devmode != NULL) {
        /* Query the settings we understand and are interested in.
         * For the flags that are set in dmFields, where the values
         * are a simple enumeration, set the same bits in a clean dmFields
         * variable, and set bits in a dmValues variable to indicate the
         * selected value. These can all be passed up to Java in one
         * call to sync up the Java view of this.
         */

        if (devmode->dmFields & DM_COPIES) {
            dmFields |= DM_COPIES;
            copies = devmode->dmCopies;
            if (pd.nCopies == 1) {
                env->SetBooleanField(printCtrl,
                                     driverDoesMultipleCopiesID,
                                     JNI_TRUE);
            } else {
              copies = pd.nCopies;
            }
        }

        if (devmode->dmFields & DM_PAPERSIZE) {
            env->CallVoidMethod(printCtrl, AwtPrintControl::setWin32MediaID,
                                devmode->dmPaperSize, devmode->dmPaperWidth,
                                devmode->dmPaperLength);

        }

        if (devmode->dmFields & DM_DEFAULTSOURCE) {
            env->CallVoidMethod(printCtrl,
                                AwtPrintControl::setWin32MediaTrayID,
                                devmode->dmDefaultSource);
        }

        if (devmode->dmFields & DM_COLOR) {
            dmFields |= DM_COLOR;
            if (devmode->dmColor == DMCOLOR_COLOR) {
                dmValues |= SET_COLOR;
            }
        }

        if (devmode->dmFields & DM_ORIENTATION) {
            dmFields |= DM_ORIENTATION;
            if (devmode->dmOrientation == DMORIENT_LANDSCAPE) {
                dmValues |= SET_ORIENTATION;
            }
        }

        if (devmode->dmFields & DM_COLLATE) {
            dmFields |= DM_COLLATE;
            if (devmode->dmCollate == DMCOLLATE_TRUE) {
                pdFlags |= PD_COLLATE;
                env->SetBooleanField(printCtrl,
                                     driverDoesCollationID,
                                     JNI_TRUE);
            } else {
                pdFlags &= ~PD_COLLATE;
            }
        }

        if (devmode->dmFields & DM_PRINTQUALITY) {
            /* value < 0 indicates quality setting.
             * value > 0 indicates X resolution. In that case
             * hopefully we will also find y-resolution specified.
             * If its not, assume its the same as x-res.
             * Maybe Java code should try to reconcile this against
             * the printers claimed set of supported resolutions.
             */
            if (devmode->dmPrintQuality < 0) {
                if (dmFields |= DM_PRINTQUALITY) {
                    if (devmode->dmPrintQuality == DMRES_HIGH) {
                        dmValues |= SET_RES_HIGH;
                    } else if ((devmode->dmPrintQuality == DMRES_LOW) ||
                               (devmode->dmPrintQuality == DMRES_DRAFT)) {
                        dmValues |= SET_RES_LOW;
                    } else if (devmode->dmPrintQuality == DMRES_MEDIUM) {
                        /* default */
                    }
                }
            } else {
                int xRes = devmode->dmPrintQuality;

                /* For some printers, printer quality can specify 1200IQ
                 * In this case, dmPrintQuality comes out 600 and
                 * dmYResolution comes out 2, similarly for 2400IQ
                 * dmPrintQuality comes out 600 and dmYResolution comes out 4
                 * which is not a valid resolution
                 * so for IQ setting, we modify y-resolution only when it is
                 * greater than 10.
                 */
                int yRes = (devmode->dmFields & DM_YRESOLUTION) &&
                           (devmode->dmYResolution > 10) ?
                           devmode->dmYResolution : devmode->dmPrintQuality;

                env->CallVoidMethod(printCtrl, AwtPrintControl::setResID,
                                    xRes, yRes);
            }
        }

        if (devmode->dmFields & DM_DUPLEX) {
            dmFields |= DM_DUPLEX;
            if (devmode->dmDuplex == DMDUP_HORIZONTAL) {
              dmValues |= SET_DUP_HORIZONTAL;
            } else if (devmode->dmDuplex == DMDUP_VERTICAL) {
                dmValues |= SET_DUP_VERTICAL;
            }
        }


        ::GlobalUnlock(pd.hDevMode);
        devmode = NULL;
    } else {
        copies = pd.nCopies;
    }

    if (pd.hDevNames != NULL) {
        DEVNAMES *devnames = (DEVNAMES*)::GlobalLock(pd.hDevNames);
        DASSERT(!IsBadReadPtr(devnames, sizeof(DEVNAMES)));
        LPTSTR lpcNames = (LPTSTR)devnames;
        LPTSTR pbuf = (_tcslen(lpcNames + devnames->wDeviceOffset) == 0 ?
                      TEXT("") : lpcNames + devnames->wDeviceOffset);
        if (pbuf != NULL) {
            jstring jstr = JNU_NewStringPlatform(env, pbuf);
            env->CallVoidMethod(printCtrl,
                                AwtPrintControl::setPrinterID,
                                jstr);
            env->DeleteLocalRef(jstr);
        }
        pbuf = (_tcslen(lpcNames + devnames->wOutputOffset) == 0 ?
                      TEXT("") : lpcNames + devnames->wOutputOffset);
        if (pbuf != NULL) {
            if (wcscmp(pbuf, L"FILE:") == 0) {
                pdFlags |= PD_PRINTTOFILE;
            }
        }
        ::GlobalUnlock(pd.hDevNames);
        devnames = NULL;
    }


    env->CallVoidMethod(printCtrl, AwtPrintControl::setNativeAttID,
                        pdFlags,  dmFields, dmValues);


    // copies  & range are always set so no need to check for any flags
    env->CallVoidMethod(printCtrl, AwtPrintControl::setRangeCopiesID,
                        pd.nFromPage, pd.nToPage, (pdFlags & PD_PAGENUMS),
                        copies);

    // repeated calls to printDialog should not leak handles
    HDC oldDC = AwtPrintControl::getPrintDC(env, printCtrl);
    if (pd.hDC != oldDC) {
        if (oldDC != NULL) {
            ::DeleteDC(oldDC);
        }
        AwtPrintControl::setPrintDC(env, printCtrl, pd.hDC);
        newDC = true;
    }
    // Need to update WPrinterJob with device resolution settings for
    // new or changed DC.
    setCapabilities(env, printCtrl, pd.hDC);

    HGLOBAL oldG = AwtPrintControl::getPrintHDMode(env, printCtrl);
    if (pd.hDevMode != oldG) {
        AwtPrintControl::setPrintHDMode(env, printCtrl, pd.hDevMode);
    }

    oldG = AwtPrintControl::getPrintHDName(env, printCtrl);
    if (pd.hDevNames != oldG) {
        AwtPrintControl::setPrintHDName(env, printCtrl, pd.hDevNames);
    }

    return newDC;
}


BOOL AwtPrintControl::getDevmode( HANDLE hPrinter,
                                 LPTSTR printerName,
                                 LPDEVMODE *pDevMode) {

    if (hPrinter == NULL || printerName == NULL || pDevMode == NULL) {
      return FALSE;
    }

    SAVE_CONTROLWORD

    DWORD dwNeeded = ::DocumentProperties(NULL, hPrinter, printerName,
                                        NULL, NULL, 0);

    RESTORE_CONTROLWORD

    if (dwNeeded <= 0) {
        *pDevMode = NULL;
        return FALSE;
    }

    *pDevMode = (LPDEVMODE)GlobalAlloc(GPTR, dwNeeded);

    if (*pDevMode == NULL) {
        return FALSE;
    }

    DWORD dwRet = ::DocumentProperties(NULL,
                                       hPrinter,
                                       printerName,
                                       *pDevMode,
                                       NULL,
                                       DM_OUT_BUFFER);

    RESTORE_CONTROLWORD

    if (dwRet != IDOK)  {
        /* if failure, cleanup and return failure */
        GlobalFree(*pDevMode);
        *pDevMode = NULL;
        return FALSE;
    }

    return TRUE;
}
