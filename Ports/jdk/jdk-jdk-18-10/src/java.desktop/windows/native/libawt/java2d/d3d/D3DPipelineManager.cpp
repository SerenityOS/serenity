/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DBadHardware.h"
#include "D3DPipelineManager.h"
#include "D3DRenderQueue.h"
#include "WindowsFlags.h"
#include "awt_Win32GraphicsDevice.h"

// state of the adapter prior to initialization
#define CONTEXT_NOT_INITED 0
// this state is set if adapter initialization had failed
#define CONTEXT_INIT_FAILED (-1)
// this state is set if adapter was successfully created
#define CONTEXT_CREATED 1

static BOOL bNoHwCheck = (getenv("J2D_D3D_NO_HWCHECK") != NULL);

D3DPipelineManager *D3DPipelineManager::pMgr = NULL;


D3DPipelineManager * D3DPipelineManager::CreateInstance(void)
{
    if (!IsD3DEnabled() ||
        FAILED((D3DPipelineManager::CheckOSVersion())) ||
        FAILED((D3DPipelineManager::GDICheckForBadHardware())))
    {
        return NULL;
    }

    if (pMgr == NULL) {
        pMgr = new D3DPipelineManager();
        if (FAILED(pMgr->InitD3D())) {
            SAFE_DELETE(pMgr);
        }
    } else {
        // this should never happen so to be on the safe side do not
        // use this unexpected pointer, do not try to release it, just null
        // it out and fail safely
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "D3DPPLM::CreateInstance: unexpected instance: 0x%x,"\
                       " abort.", pMgr);
        pMgr = NULL;
    }
    return pMgr;
}

void D3DPipelineManager::DeleteInstance()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::DeleteInstance()");
    SAFE_DELETE(pMgr);
}

D3DPipelineManager * D3DPipelineManager::GetInstance(void)
{
    return pMgr;
}

D3DPipelineManager::D3DPipelineManager(void)
{
    pd3d9 = NULL;
    hLibD3D9 = NULL;
    pAdapters = NULL;
    adapterCount = 0;
    currentFSFocusAdapter = -1;
    defaultFocusWindow = 0;
    devType = SelectDeviceType();
}

D3DPipelineManager::~D3DPipelineManager(void)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::~D3DPipelineManager()");
    ReleaseD3D();
}

HRESULT D3DPipelineManager::ReleaseD3D(void)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::ReleaseD3D()");

    ReleaseAdapters();

    SAFE_RELEASE(pd3d9);

    if (hLibD3D9 != NULL) {
        ::FreeLibrary(hLibD3D9);
        hLibD3D9 = NULL;
    }

    return S_OK;
}

// Creates a Direct3D9 object and initializes adapters.
// If succeeded, returns S_OK, otherwise returns the error code.
HRESULT D3DPipelineManager::InitD3D(void)
{
    typedef IDirect3D9 * WINAPI FnDirect3DCreate9(UINT SDKVersion);

    hLibD3D9 = JDK_LoadSystemLibrary("d3d9.dll");
    if (hLibD3D9 == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "InitD3D: no d3d9.dll");
        return E_FAIL;
    }

    FnDirect3DCreate9 *d3dcreate9 = NULL;
    d3dcreate9 = (FnDirect3DCreate9*)
        ::GetProcAddress(hLibD3D9, "Direct3DCreate9");
    if (d3dcreate9 == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "InitD3D: no Direct3DCreate9");
        ::FreeLibrary(hLibD3D9);
        return E_FAIL;
    }

    pd3d9 = d3dcreate9(D3D_SDK_VERSION);
    if (pd3d9 == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "InitD3D: unable to create IDirect3D9 object");
        ::FreeLibrary(hLibD3D9);
        return E_FAIL;
    }

    HRESULT res;
    if (FAILED(res = InitAdapters())) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "InitD3D: failed to init adapters");
        ReleaseD3D();
        return res;
    }

    return S_OK;
}

HRESULT D3DPipelineManager::ReleaseAdapters()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::ReleaseAdapters()");

    D3DRQ_ResetCurrentContextAndDestination();
    if (pAdapters != NULL) {
        for (UINT i = 0; i < adapterCount; i++) {
            if (pAdapters[i].pd3dContext != NULL) {
                delete pAdapters[i].pd3dContext;
            }
        }
        delete[] pAdapters;
        pAdapters = NULL;
    }
    if (defaultFocusWindow != 0) {
        DestroyWindow(defaultFocusWindow);
        UnregisterClass(L"D3DFocusWindow", GetModuleHandle(NULL));
        defaultFocusWindow = 0;
    }
    currentFSFocusAdapter = -1;
    return S_OK;
}

UINT D3DPipelineManager::GetAdapterOrdinalForScreen(jint gdiScreen)
{
    HMONITOR mHnd = AwtWin32GraphicsDevice::GetMonitor(gdiScreen);
    if (mHnd == (HMONITOR)0) {
        return D3DADAPTER_DEFAULT;
    }
    return GetAdapterOrdinalByHmon((HMONITOR)mHnd);
}

// static
HRESULT D3DPipelineManager::HandleAdaptersChange(HMONITOR *pHMONITORs, UINT monNum)
{
    HRESULT res = S_OK;
    BOOL bResetD3D = FALSE, bFound;

    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    RETURN_STATUS_IF_NULL(pHMONITORs, E_FAIL);
    if (pMgr == NULL) {
        // NULL pMgr is valid when the pipeline is not enabled or if it hasn't
        // been created yet
        return S_OK;
    }
    RETURN_STATUS_IF_NULL(pMgr->pAdapters, E_FAIL);
    RETURN_STATUS_IF_NULL(pMgr->pd3d9, E_FAIL);

    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::HandleAdaptersChange");

    if (monNum != pMgr->adapterCount) {
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                   "  number of adapters changed (old=%d, new=%d)",
                   pMgr->adapterCount, monNum);
        bResetD3D = TRUE;
    } else {
        for (UINT i = 0; i < pMgr->adapterCount; i++) {
            HMONITOR hMon = pMgr->pd3d9->GetAdapterMonitor(i);
            if (hMon == (HMONITOR)0x0) {
                J2dTraceLn1(J2D_TRACE_VERBOSE, "  adapter %d: removed", i);
                bResetD3D = TRUE;
                break;
            }
            bFound = FALSE;
            for (UINT mon = 0; mon < monNum; mon++) {
                if (pHMONITORs[mon] == hMon) {
                    J2dTraceLn3(J2D_TRACE_VERBOSE,
                            "  adapter %d: found hmnd[%d]=0x%x", i, mon, hMon);
                    bFound = TRUE;
                    break;
                }
            }
            if (!bFound) {
                J2dTraceLn2(J2D_TRACE_VERBOSE,
                            "  adapter %d: could not find hmnd=0x%x "\
                            "in the list of new hmnds", i, hMon);
                bResetD3D = TRUE;
                break;
            }
        }
    }

    if (bResetD3D) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  adapters changed: resetting d3d");
        pMgr->ReleaseD3D();
        res = pMgr->InitD3D();
    }
    return res;
}

HRESULT D3DPipelineManager::HandleLostDevices()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::HandleLostDevices()");
    BOOL bAllClear = TRUE;

    HWND hwnd = GetCurrentFocusWindow();
    if (hwnd != defaultFocusWindow) {
        // we're in full-screen mode
        WINDOWPLACEMENT wp;
        ::ZeroMemory(&wp, sizeof(WINDOWPLACEMENT));
        wp.length = sizeof(WINDOWPLACEMENT);
        ::GetWindowPlacement(hwnd, &wp);

        // Only attempt to restore the devices if we're in full-screen mode
        // and the fs window is active; sleep otherwise.
        // Restoring a window while minimized causes problems on Vista:
        // sometimes we restore the window too quickly and it pops up back from
        // minimized state when the device is restored.
        //
        // WARNING: this is a sleep on the Toolkit thread! We may reconsider
        // this if we find any issues later.
        if ((wp.showCmd & SW_SHOWMINNOACTIVE) && !(wp.showCmd & SW_SHOWNORMAL)){
            static DWORD prevCallTime = 0;
            J2dTraceLn(J2D_TRACE_VERBOSE, "  fs focus window is minimized");
            DWORD currentTime = ::GetTickCount();
            if ((currentTime - prevCallTime) < 100) {
                J2dTraceLn(J2D_TRACE_VERBOSE, "  tight loop detected, sleep");
                ::Sleep(100);
            }
            prevCallTime = currentTime;
            return D3DERR_DEVICELOST;
        }
    }
    if (pAdapters != NULL) {
        for (UINT i = 0; i < adapterCount; i++) {
            if (pAdapters[i].pd3dContext != NULL) {
                J2dTraceLn1(J2D_TRACE_VERBOSE,
                            "  HandleLostDevices: checking adapter %d", i);
                D3DContext *d3dc = pAdapters[i].pd3dContext;
                if (FAILED(d3dc->CheckAndResetDevice())) {
                    bAllClear = FALSE;
                }
            }
        }
    }
    return bAllClear ? S_OK : D3DERR_DEVICELOST;
}

HRESULT D3DPipelineManager::InitAdapters()
{
    HRESULT res = E_FAIL;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::InitAdapters()");
    if (pAdapters != NULL) {
        ReleaseAdapters();
    }

    adapterCount = pd3d9->GetAdapterCount();
    pAdapters = new D3DAdapter[adapterCount];
    if (pAdapters == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "InitAdapters: out of memory");
        adapterCount = 0;
        return E_FAIL;
    }
    ZeroMemory(pAdapters, adapterCount * sizeof(D3DAdapter));

    res = CheckAdaptersInfo();
    RETURN_STATUS_IF_FAILED(res);

    currentFSFocusAdapter = -1;
    if (CreateDefaultFocusWindow() == 0) {
        return E_FAIL;
    }

    return S_OK;
}

// static
HRESULT
D3DPipelineManager::CheckOSVersion()
{
    // require Windows XP or newer client-class OS
    if (IS_WINVER_ATLEAST(5, 1) &&
        !D3DPPLM_OsVersionMatches(OS_WINSERV_2008R2|OS_WINSERV_2008|
                                  OS_WINSERV_2003))
    {
        J2dTraceLn(J2D_TRACE_INFO,
                   "D3DPPLM::CheckOSVersion: Windows XP or newer client-classs"\
                   " OS detected, passed");
        return S_OK;
    }
    J2dRlsTraceLn(J2D_TRACE_ERROR,
                  "D3DPPLM::CheckOSVersion: Windows 2000 or earlier (or a "\
                  "server) OS detected, failed");
    if (bNoHwCheck) {
        J2dRlsTraceLn(J2D_TRACE_WARNING,
                      "  OS check overridden via J2D_D3D_NO_HWCHECK");
        return S_OK;
    }
    return E_FAIL;
}

// static
HRESULT
D3DPipelineManager::GDICheckForBadHardware()
{
    DISPLAY_DEVICE dd;
    dd.cb = sizeof(DISPLAY_DEVICE);

    int failedDevices = 0;
    int attachedDevices = 0;
    int i = 0;
    WCHAR *id;
    WCHAR vendorId[5];
    WCHAR deviceId[5];
    DWORD dwDId, dwVId;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::GDICheckForBadHardware");

    // i<20 is to guard against buggy drivers
    while (EnumDisplayDevices(NULL, i, &dd, 0) && i < 20) {
        if (dd.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            attachedDevices++;
            id = dd.DeviceID;
            if (wcslen(id) > 21) {
                // get vendor ID
                wcsncpy(vendorId, id+8, 4);
                int args1 = swscanf(vendorId, L"%X", &dwVId);

                // get device ID
                wcsncpy(deviceId, id+17, 4);
                int args2 = swscanf(deviceId, L"%X", &dwDId);

                if (args1 == 1 && args2 == 1) {
                    J2dTraceLn2(J2D_TRACE_VERBOSE,
                                "  device: vendorID=0x%04x, deviceId=0x%04x",
                                dwVId, dwDId);
                    // since we don't have a driver version here we will
                    // just ask to ignore the version for now; bad hw
                    // entries with specific drivers information will be
                    // processed later when d3d is initialized and we can
                    // obtain a driver version
                    if (FAILED(CheckForBadHardware(dwVId, dwDId, MAX_VERSION))){
                        failedDevices++;
                    }
                }
            }
        }

        i++;
    }

    if (failedDevices == attachedDevices) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DPPLM::GDICheckForBadHardware: no suitable devices found");
        return E_FAIL;
    }

    return S_OK;
}

BOOL D3DPPLM_OsVersionMatches(USHORT osInfo) {
    static USHORT currentOS = OS_UNDEFINED;

    if (currentOS == OS_UNDEFINED) {
        BOOL bVersOk;
        OSVERSIONINFOEX osvi;

        ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
        osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);

        bVersOk = GetVersionEx((OSVERSIONINFO *) &osvi);

        J2dRlsTrace(J2D_TRACE_INFO, "[I] OS Version = ");
        if (bVersOk && osvi.dwPlatformId == VER_PLATFORM_WIN32_NT &&
            osvi.dwMajorVersion > 4)
        {
            if (osvi.dwMajorVersion >= 6 && osvi.dwMinorVersion == 0) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_VISTA\n");
                    currentOS = OS_VISTA;
                } else {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_WINSERV_2008\n");
                    currentOS = OS_WINSERV_2008;
                }
            } else if (osvi.dwMajorVersion >= 6 && osvi.dwMinorVersion >= 1) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_WINDOWS7 or newer\n");
                    currentOS = OS_WINDOWS7;
                } else {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_WINSERV_2008R2 or newer\n");
                    currentOS = OS_WINSERV_2008R2;
                }
            } else if (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 2) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_WINXP_64\n");
                    currentOS = OS_WINXP_64;
                } else {
                    J2dRlsTrace(J2D_TRACE_INFO, "OS_WINSERV_2003\n");
                    currentOS = OS_WINSERV_2003;
                }
            } else if (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 1) {
                J2dRlsTrace(J2D_TRACE_INFO, "OS_WINXP ");
                currentOS = OS_WINXP;
                if (osvi.wSuiteMask & VER_SUITE_PERSONAL) {
                    J2dRlsTrace(J2D_TRACE_INFO, "Home\n");
                } else {
                    J2dRlsTrace(J2D_TRACE_INFO, "Pro\n");
                }
            } else {
                J2dRlsTrace2(J2D_TRACE_INFO,
                            "OS_UNKNOWN: dwMajorVersion=%d dwMinorVersion=%d\n",
                             osvi.dwMajorVersion, osvi.dwMinorVersion);
                currentOS = OS_UNKNOWN;
            }
        } else {
            if (bVersOk) {
                J2dRlsTrace2(J2D_TRACE_INFO,
                             "OS_UNKNOWN: dwPlatformId=%d dwMajorVersion=%d\n",
                             osvi.dwPlatformId, osvi.dwMajorVersion);
            } else {
                J2dRlsTrace(J2D_TRACE_INFO,"OS_UNKNOWN: GetVersionEx failed\n");
            }
            currentOS = OS_UNKNOWN;
        }
    }
    return (currentOS & osInfo);
}

// static
HRESULT
D3DPipelineManager::CheckForBadHardware(DWORD vId, DWORD dId, LONGLONG version)
{
    DWORD vendorId, deviceId;
    UINT adapterInfo = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::CheckForBadHardware");

    while ((vendorId = badHardware[adapterInfo].VendorId) != 0x0000 &&
           (deviceId = badHardware[adapterInfo].DeviceId) != 0x0000)
    {
        if (vendorId == vId && (deviceId == dId || deviceId == ALL_DEVICEIDS)) {
            LONGLONG goodVersion = badHardware[adapterInfo].DriverVersion;
            USHORT osInfo = badHardware[adapterInfo].OsInfo;
            // the hardware check fails if:
            // - we have an entry for this OS and
            // - hardware is bad for all driver versions (NO_VERSION), or
            //   we have a driver version which is older than the
            //   minimum required for this OS
            if (D3DPPLM_OsVersionMatches(osInfo) &&
                (goodVersion == NO_VERSION || version < goodVersion))
            {
                J2dRlsTraceLn2(J2D_TRACE_ERROR,
                    "D3DPPLM::CheckForBadHardware: found matching "\
                    "hardware: VendorId=0x%04x DeviceId=0x%04x",
                    vendorId, deviceId);
                if (goodVersion != NO_VERSION) {
                    // this was a match by the driver version
                    LARGE_INTEGER li;
                    li.QuadPart = goodVersion;
                    J2dRlsTraceLn(J2D_TRACE_ERROR,
                                  "  bad driver found, device disabled");
                    J2dRlsTraceLn4(J2D_TRACE_ERROR,
                                   "  update your driver to at "\
                                   "least version %d.%d.%d.%d",
                                   HIWORD(li.HighPart), LOWORD(li.HighPart),
                                   HIWORD(li.LowPart),  LOWORD(li.LowPart));
                } else {
                    // this was a match by the device (no good driver for this
                    // device)
                    J2dRlsTraceLn(J2D_TRACE_ERROR,
                                  "D3DPPLM::CheckForBadHardware: bad hardware "\
                                  "found, device disabled");
                }
                if (!bNoHwCheck) {
                    return D3DERR_INVALIDDEVICE;
                }
                J2dRlsTraceLn(J2D_TRACE_WARNING, "  Warning: hw/driver match "\
                              "overridden (via J2D_D3D_NO_HWCHECK)");
            }
        }
        adapterInfo++;
    }

    return S_OK;
}

HRESULT D3DPipelineManager::CheckAdaptersInfo()
{
    D3DADAPTER_IDENTIFIER9 aid;
    UINT failedAdaptersCount = 0;

    J2dRlsTraceLn(J2D_TRACE_INFO, "CheckAdaptersInfo");
    J2dRlsTraceLn(J2D_TRACE_INFO, "------------------");
    for (UINT Adapter = 0; Adapter < adapterCount; Adapter++) {

        if (FAILED(pd3d9->GetAdapterIdentifier(Adapter, 0, &aid))) {
            pAdapters[Adapter].state = CONTEXT_INIT_FAILED;
            failedAdaptersCount++;
            continue;
        }

        J2dRlsTraceLn1(J2D_TRACE_INFO, "Adapter Ordinal  : %d", Adapter);
        J2dRlsTraceLn1(J2D_TRACE_INFO, "Adapter Handle   : 0x%x",
                       pd3d9->GetAdapterMonitor(Adapter));
        J2dRlsTraceLn1(J2D_TRACE_INFO, "Description      : %s",
                       aid.Description);
        J2dRlsTraceLn2(J2D_TRACE_INFO, "GDI Name, Driver : %s, %s",
                       aid.DeviceName, aid.Driver);
        J2dRlsTraceLn1(J2D_TRACE_INFO, "Vendor Id        : 0x%04x",
                       aid.VendorId);
        J2dRlsTraceLn1(J2D_TRACE_INFO, "Device Id        : 0x%04x",
                       aid.DeviceId);
        J2dRlsTraceLn1(J2D_TRACE_INFO, "SubSys Id        : 0x%x",
                       aid.SubSysId);
        J2dRlsTraceLn4(J2D_TRACE_INFO, "Driver Version   : %d.%d.%d.%d",
                       HIWORD(aid.DriverVersion.HighPart),
                       LOWORD(aid.DriverVersion.HighPart),
                       HIWORD(aid.DriverVersion.LowPart),
                       LOWORD(aid.DriverVersion.LowPart));
        J2dRlsTrace3(J2D_TRACE_INFO,
                     "[I] GUID             : {%08X-%04X-%04X-",
                       aid.DeviceIdentifier.Data1,
                       aid.DeviceIdentifier.Data2,
                       aid.DeviceIdentifier.Data3);
        J2dRlsTrace4(J2D_TRACE_INFO, "%02X%02X-%02X%02X",
                       aid.DeviceIdentifier.Data4[0],
                       aid.DeviceIdentifier.Data4[1],
                       aid.DeviceIdentifier.Data4[2],
                       aid.DeviceIdentifier.Data4[3]);
        J2dRlsTrace4(J2D_TRACE_INFO, "%02X%02X%02X%02X}\n",
                       aid.DeviceIdentifier.Data4[4],
                       aid.DeviceIdentifier.Data4[5],
                       aid.DeviceIdentifier.Data4[6],
                       aid.DeviceIdentifier.Data4[7]);

        if (FAILED(CheckForBadHardware(aid.VendorId, aid.DeviceId,
                                       aid.DriverVersion.QuadPart)) ||
            FAILED(CheckDeviceCaps(Adapter))  ||
            FAILED(D3DEnabledOnAdapter(Adapter)))
        {
            pAdapters[Adapter].state = CONTEXT_INIT_FAILED;
            failedAdaptersCount++;
        }
        J2dRlsTraceLn(J2D_TRACE_INFO, "------------------");
    }

    if (failedAdaptersCount == adapterCount) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "D3DPPLM::CheckAdaptersInfo: no suitable adapters found");
        return E_FAIL;
    }

    return S_OK;
}

D3DDEVTYPE D3DPipelineManager::SelectDeviceType()
{
    char *pRas = getenv("J2D_D3D_RASTERIZER");
    D3DDEVTYPE dtype = D3DDEVTYPE_HAL;
    if (pRas != NULL) {
        J2dRlsTrace(J2D_TRACE_WARNING, "[W] D3DPPLM::SelectDeviceType: ");
        if (strncmp(pRas, "ref", 3) == 0 || strncmp(pRas, "rgb", 3) == 0) {
            J2dRlsTrace(J2D_TRACE_WARNING, "ref rasterizer selected");
            dtype = D3DDEVTYPE_REF;
        } else if (strncmp(pRas, "hal",3) == 0 || strncmp(pRas, "tnl",3) == 0) {
            J2dRlsTrace(J2D_TRACE_WARNING, "hal rasterizer selected");
            dtype = D3DDEVTYPE_HAL;
        } else if (strncmp(pRas, "nul", 3) == 0) {
            J2dRlsTrace(J2D_TRACE_WARNING, "nullref rasterizer selected");
            dtype = D3DDEVTYPE_NULLREF;
        } else {
            J2dRlsTrace1(J2D_TRACE_WARNING,
                "unknown rasterizer: %s, only (ref|hal|nul) "\
                "supported, hal selected instead", pRas);
        }
        J2dRlsTrace(J2D_TRACE_WARNING, "\n");
    }
    return dtype;
}

#define CHECK_CAP(FLAG, CAP) \
    do {    \
        if (!((FLAG)&CAP)) { \
            J2dRlsTraceLn2(J2D_TRACE_ERROR, \
                           "D3DPPLM::CheckDeviceCaps: adapter %d: Failed "\
                           "(cap %s not supported)", \
                           adapter, #CAP); \
            return E_FAIL; \
        } \
    } while (0)

HRESULT D3DPipelineManager::CheckDeviceCaps(UINT adapter)
{
    HRESULT res;
    D3DCAPS9 d3dCaps;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::CheckDeviceCaps");

    res = pd3d9->GetDeviceCaps(adapter, devType, &d3dCaps);
    RETURN_STATUS_IF_FAILED(res);

    CHECK_CAP(d3dCaps.DevCaps, D3DDEVCAPS_DRAWPRIMTLVERTEX);

    // by requiring hardware tnl we are hoping for better drivers quality
    if (!IsD3DForced()) {
        // fail if not hw tnl unless d3d was forced
        CHECK_CAP(d3dCaps.DevCaps, D3DDEVCAPS_HWTRANSFORMANDLIGHT);
    }
    if (d3dCaps.DeviceType == D3DDEVTYPE_HAL) {
        CHECK_CAP(d3dCaps.DevCaps, D3DDEVCAPS_HWRASTERIZATION);
    }

    CHECK_CAP(d3dCaps.RasterCaps, D3DPRASTERCAPS_SCISSORTEST);

    CHECK_CAP(d3dCaps.Caps3, D3DCAPS3_ALPHA_FULLSCREEN_FLIP_OR_DISCARD);

    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_CULLNONE);
    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_BLENDOP);
    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_MASKZ);

    CHECK_CAP(d3dCaps.ZCmpCaps, D3DPCMPCAPS_ALWAYS);
    CHECK_CAP(d3dCaps.ZCmpCaps, D3DPCMPCAPS_LESS);

    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_ZERO);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_ONE);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_SRCALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_DESTALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_INVSRCALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_INVDESTALPHA);

    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_ZERO);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_ONE);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_SRCALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_DESTALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_INVSRCALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_INVDESTALPHA);

    CHECK_CAP(d3dCaps.TextureAddressCaps, D3DPTADDRESSCAPS_CLAMP);
    CHECK_CAP(d3dCaps.TextureAddressCaps, D3DPTADDRESSCAPS_WRAP);

    CHECK_CAP(d3dCaps.TextureOpCaps, D3DTEXOPCAPS_MODULATE);

    if (d3dCaps.PixelShaderVersion < D3DPS_VERSION(2,0) && !IsD3DForced()) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "D3DPPLM::CheckDeviceCaps: adapter %d: Failed "\
                       "(pixel shaders 2.0 required)", adapter);
        return E_FAIL;
    }

    J2dRlsTraceLn1(J2D_TRACE_INFO,
                   "D3DPPLM::CheckDeviceCaps: adapter %d: Passed", adapter);
    return S_OK;
}


HRESULT D3DPipelineManager::D3DEnabledOnAdapter(UINT adapter)
{
    HRESULT res;
    D3DDISPLAYMODE dm;

    res = pd3d9->GetAdapterDisplayMode(adapter, &dm);
    RETURN_STATUS_IF_FAILED(res);

    res = pd3d9->CheckDeviceType(adapter, devType, dm.Format, dm.Format, TRUE);
    if (FAILED(res)) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                "D3DPPLM::D3DEnabledOnAdapter: no " \
                "suitable d3d device on adapter %d", adapter);
    }

    return res;
}

UINT D3DPipelineManager::GetAdapterOrdinalByHmon(HMONITOR hMon)
{
    UINT ret = D3DADAPTER_DEFAULT;

    if (pd3d9 != NULL) {
        UINT adapterCount = pd3d9->GetAdapterCount();
        for (UINT adapter = 0; adapter < adapterCount; adapter++) {
            HMONITOR hm = pd3d9->GetAdapterMonitor(adapter);
            if (hm == hMon) {
                ret = adapter;
                break;
            }
        }
    }
    return ret;
}

D3DFORMAT
D3DPipelineManager::GetMatchingDepthStencilFormat(UINT adapterOrdinal,
                                                  D3DFORMAT adapterFormat,
                                                  D3DFORMAT renderTargetFormat)
{
    static D3DFORMAT formats[] =
        { D3DFMT_D16, D3DFMT_D32, D3DFMT_D24S8, D3DFMT_D24X8 };
    D3DFORMAT newFormat = D3DFMT_UNKNOWN;
    HRESULT res;
    for (int i = 0; i < 4; i++) {
        res = pd3d9->CheckDeviceFormat(adapterOrdinal,
                devType, adapterFormat, D3DUSAGE_DEPTHSTENCIL,
                D3DRTYPE_SURFACE, formats[i]);
        if (FAILED(res)) continue;

        res = pd3d9->CheckDepthStencilMatch(adapterOrdinal,
                devType, adapterFormat, renderTargetFormat, formats[i]);
        if (FAILED(res)) continue;
        newFormat = formats[i];
        break;
    }
    return newFormat;
}

HWND D3DPipelineManager::CreateDefaultFocusWindow()
{
    UINT adapterOrdinal = D3DADAPTER_DEFAULT;

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DPPLM::CreateDefaultFocusWindow: adapter=%d",
                adapterOrdinal);

    if (defaultFocusWindow != 0) {
        J2dRlsTraceLn(J2D_TRACE_WARNING,
                      "D3DPPLM::CreateDefaultFocusWindow: "\
                      "existing default focus window!");
        return defaultFocusWindow;
    }

    WNDCLASS wc;
    ZeroMemory(&wc, sizeof(WNDCLASS));
    wc.hInstance = GetModuleHandle(NULL);
    wc.lpfnWndProc = DefWindowProc;
    wc.lpszClassName = L"D3DFocusWindow";
    if (RegisterClass(&wc) == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "D3DPPLM::CreateDefaultFocusWindow: "\
                      "error registering window class");
        return 0;
    }

    MONITORINFO mi;
    ZeroMemory(&mi, sizeof(MONITORINFO));
    mi.cbSize = sizeof(MONITORINFO);
    HMONITOR hMon = pd3d9->GetAdapterMonitor(adapterOrdinal);
    if (hMon == 0 || !GetMonitorInfo(hMon, (LPMONITORINFO)&mi)) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
            "D3DPPLM::CreateDefaultFocusWindow: "\
            "error getting monitor info for adapter=%d", adapterOrdinal);
        return 0;
    }

    HWND hWnd = CreateWindow(L"D3DFocusWindow", L"D3DFocusWindow", WS_POPUP,
        mi.rcMonitor.left, mi.rcMonitor.top, 1, 1,
        NULL, NULL, GetModuleHandle(NULL), NULL);
    if (hWnd == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DPPLM::CreateDefaultFocusWindow: CreateWindow failed");
    } else {
        J2dTraceLn2(J2D_TRACE_INFO,
            "  Created default focus window %x for adapter %d",
            hWnd, adapterOrdinal);
        defaultFocusWindow = hWnd;
    }
    return hWnd;
}

HWND D3DPipelineManager::GetCurrentFocusWindow()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::GetCurrentFocusWindow");
    if (currentFSFocusAdapter < 0) {
        J2dTraceLn1(J2D_TRACE_VERBOSE,
                    "  no fs windows, using default focus window=0x%x",
                    defaultFocusWindow);
        return defaultFocusWindow;
    }
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  using fs window=0x%x",
                pAdapters[currentFSFocusAdapter].fsFocusWindow);
    return pAdapters[currentFSFocusAdapter].fsFocusWindow;
}

HWND D3DPipelineManager::SetFSFocusWindow(UINT adapterOrdinal, HWND hWnd)
{
    J2dTraceLn2(J2D_TRACE_INFO,"D3DPPLM::SetFSFocusWindow hwnd=0x%x adapter=%d",
                hWnd, adapterOrdinal);

    HWND prev = pAdapters[adapterOrdinal].fsFocusWindow;
    pAdapters[adapterOrdinal].fsFocusWindow = hWnd;
    if (currentFSFocusAdapter < 0) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  first full-screen window");
        // first fs window
        currentFSFocusAdapter = adapterOrdinal;
        // REMIND: we might want to reset the rest of the context here as well
        // like we do when the an adapter exits fs mode; currently they will
        // be reset sometime later
    } else {
        // there's already a fs window
        if (currentFSFocusAdapter == adapterOrdinal) {
            // it's current fs window => we're exiting fs mode on this adapter;
            // look for a new fs focus window
            if (hWnd == 0) {
                UINT i;
                currentFSFocusAdapter = -1;
                for (i = 0; i < adapterCount; i++) {
                    if (pAdapters[i].fsFocusWindow != 0) {
                        J2dTraceLn1(J2D_TRACE_VERBOSE,
                                    "  adapter %d is still in fs mode", i);
                        currentFSFocusAdapter = i;
                        break;
                    }
                }
                // we have to reset all devices any time current focus device
                // exits fs mode, and also to prevent some of them being left in
                // a lost state when the last device exits fs - when non-last
                // adapters exit fs mode they would not be able to create the
                // device and will be put in a lost state forever
                HRESULT res;
                J2dTraceLn(J2D_TRACE_VERBOSE,
                           "  adapter exited full-screen, reset all adapters");
                for (i = 0; i < adapterCount; i++) {
                    if (pAdapters[i].pd3dContext != NULL) {
                        res = pAdapters[i].pd3dContext->ResetContext();
                        D3DRQ_MarkLostIfNeeded(res,
                            D3DRQ_GetCurrentDestination());
                    }
                }
            } else {
                J2dTraceLn1(J2D_TRACE_WARNING,
                            "D3DPM::SetFSFocusWindow: setting the fs "\
                            "window again for adapter %d", adapterOrdinal);
            }
        }
    }
    return prev;
}

HRESULT D3DPipelineManager::GetD3DContext(UINT adapterOrdinal,
                                          D3DContext **ppd3dContext)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DPPLM::GetD3DContext");

    HRESULT res = S_OK;
    if (adapterOrdinal < 0 || adapterOrdinal >= adapterCount ||
        pAdapters == NULL ||
        pAdapters[adapterOrdinal].state == CONTEXT_INIT_FAILED)
    {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
            "D3DPPLM::GetD3DContext: invalid parameters or "\
            "failed init for adapter %d", adapterOrdinal);
        *ppd3dContext = NULL;
        return E_FAIL;
    }

    if (pAdapters[adapterOrdinal].state == CONTEXT_NOT_INITED) {
        D3DContext *pCtx = NULL;

        if (pAdapters[adapterOrdinal].pd3dContext != NULL) {
            J2dTraceLn1(J2D_TRACE_ERROR, "  non-null context in "\
                        "uninitialized adapter %d", adapterOrdinal);
            res = E_FAIL;
        } else {
            J2dTraceLn1(J2D_TRACE_VERBOSE,
                        "  initializing context for adapter %d",adapterOrdinal);

            if (SUCCEEDED(res = D3DEnabledOnAdapter(adapterOrdinal))) {
                res = D3DContext::CreateInstance(pd3d9, adapterOrdinal, &pCtx);
                if (FAILED(res)) {
                    J2dRlsTraceLn1(J2D_TRACE_ERROR,
                        "D3DPPLM::GetD3DContext: failed to create context "\
                        "for adapter=%d", adapterOrdinal);
                }
            } else {
                J2dRlsTraceLn1(J2D_TRACE_ERROR,
                    "D3DPPLM::GetContext: no d3d on adapter %d",adapterOrdinal);
            }
        }
        pAdapters[adapterOrdinal].state =
            SUCCEEDED(res) ? CONTEXT_CREATED : CONTEXT_INIT_FAILED;
        pAdapters[adapterOrdinal].pd3dContext = pCtx;
    }
    *ppd3dContext = pAdapters[adapterOrdinal].pd3dContext;
    return res;
}


//==============================================================
// D3DInitializer
//==============================================================

D3DInitializer D3DInitializer::theInstance;

D3DInitializer::D3DInitializer()
    : bComInitialized(false), pAdapterIniters(NULL)
{
}

D3DInitializer::~D3DInitializer()
{
    if (pAdapterIniters) {
        delete[] pAdapterIniters;
    }
}

void D3DInitializer::InitImpl()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "D3DInitializer::InitImpl");
    if (SUCCEEDED(::CoInitialize(NULL))) {
        bComInitialized = true;
    }
    D3DPipelineManager *pMgr = D3DPipelineManager::CreateInstance();
    if (pMgr != NULL) {
        // init adapters if we are preloading
        if (AwtToolkit::GetInstance().GetPreloadThread().OnPreloadThread()) {
            UINT adapterCount = pMgr->adapterCount;

            pAdapterIniters = new D3DAdapterInitializer[adapterCount];
            for (UINT i=0; i<adapterCount; i++) {
                pAdapterIniters[i].setAdapter(i);
                AwtToolkit::GetInstance().GetPreloadThread().AddAction(&pAdapterIniters[i]);
            }
        }
    }
}

void D3DInitializer::CleanImpl(bool reInit)
{
    J2dRlsTraceLn1(J2D_TRACE_INFO, "D3DInitializer::CleanImpl (%s)",
                                    reInit ? "RELAUNCH" : "normal");
    D3DPipelineManager::DeleteInstance();
    if (bComInitialized) {
        CoUninitialize();
    }
}


void D3DInitializer::D3DAdapterInitializer::InitImpl()
{
    J2dRlsTraceLn1(J2D_TRACE_INFO, "D3DAdapterInitializer::InitImpl(%d) started", adapter);

    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (pMgr == NULL) {
        return;
    }

    D3DContext *pd3dContext;
    pMgr->GetD3DContext(adapter, &pd3dContext);

    J2dRlsTraceLn1(J2D_TRACE_INFO, "D3DAdapterInitializer::InitImpl(%d) finished", adapter);
}

void D3DInitializer::D3DAdapterInitializer::CleanImpl(bool reInit)
{
    // nothing to do - D3DPipelineManager cleans adapters
}


extern "C" {
/*
 * Export function to start D3D preloading
 * (called from java/javaw - see src/windows/bin/java-md.c)
 */
__declspec(dllexport) int preloadD3D()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "AWT warmup: preloadD3D");
    AwtToolkit::GetInstance().GetPreloadThread().AddAction(&D3DInitializer::GetInstance());
    return 1;
}

}

