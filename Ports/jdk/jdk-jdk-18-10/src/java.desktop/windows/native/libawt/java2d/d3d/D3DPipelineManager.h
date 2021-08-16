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
#pragma once

#include "D3DPipeline.h"
#include "D3DContext.h"
#include "awt_Toolkit.h"

typedef class D3DPipelineManager *LPD3DPIPELINEMANAGER;

typedef struct D3DAdapter
{
    D3DContext *pd3dContext;
    DWORD state;
    HWND fsFocusWindow;
} D3DAdapter;

class D3DPIPELINE_API D3DPipelineManager
{
    friend class D3DInitializer;
private:
    // creates and initializes instance of D3DPipelineManager, may return NULL
    static D3DPipelineManager* CreateInstance(void);

    // deletes the single instance of the manager
    static void DeleteInstance();

public:
    // returns the single instance of the manager, may return NULL
    static D3DPipelineManager* GetInstance(void);

    HRESULT GetD3DContext(UINT adapterOrdinal, D3DContext **ppd3dContext);

    HRESULT HandleLostDevices();
    // Checks if adapters were added or removed, or if the order had changed
    // (which may happen with primary display is changed). If that's the case
    // releases current adapters and d3d9 instance, reinitializes the pipeline.
    // @param *monHds list of monitor handles retrieved from GDI
    // @param monNum number of gdi monitors
    static
    HRESULT HandleAdaptersChange(HMONITOR *monHds, UINT monNum);
    // returns depth stencil buffer format matching adapterFormat and render target
    // format for the device specified by adapterOrdinal/devType
    D3DFORMAT GetMatchingDepthStencilFormat(UINT adapterOrdinal,
                                            D3DFORMAT adapterFormat,
                                            D3DFORMAT renderTargetFormat);

    HWND GetCurrentFocusWindow();
    // returns previous fs window
    HWND SetFSFocusWindow(UINT, HWND);

    LPDIRECT3D9 GetD3DObject() { return pd3d9; }
    D3DDEVTYPE GetDeviceType() { return devType; }

    // returns the d3d adapter ordinal given GDI screen number:
    // these may differ depending on which display is primary
    UINT GetAdapterOrdinalForScreen(jint gdiScreen);

private:
    D3DPipelineManager(void);
   ~D3DPipelineManager(void);

    // Creates a Direct3D9 object and initializes adapters.
    HRESULT InitD3D(void);
    // Releases adapters, Direct3D9 object and the d3d9 library.
    HRESULT ReleaseD3D();

    // selects the device type based on user input and available
    // device types
    D3DDEVTYPE SelectDeviceType();

    // creates array of adapters (releases the old one first)
    HRESULT InitAdapters();
    // releases each adapter's context, and then releases the array
    HRESULT ReleaseAdapters();

    HWND    CreateDefaultFocusWindow();
    // returns S_OK if the adapter is capable of running the Direct3D
    // pipeline
    HRESULT D3DEnabledOnAdapter(UINT Adapter);
    // returns adapterOrdinal given a HMONITOR handle
    UINT    GetAdapterOrdinalByHmon(HMONITOR hMon);
    HRESULT CheckAdaptersInfo();
    HRESULT CheckDeviceCaps(UINT Adapter);
    // Check the OS, succeeds if the OS is XP or newer client-class OS
static HRESULT CheckOSVersion();
    // used to check attached adapters using GDI against known bad hw database
    // prior to the instantiation of the pipeline manager
static HRESULT GDICheckForBadHardware();
    // given VendorId, DeviceId and driver version, checks against a database
    // of known bad hardware/driver combinations.
    // If the driver version is not known MAX_VERSION can be used
    // which is guaranteed to satisfy the check
static HRESULT CheckForBadHardware(DWORD vId, DWORD dId, LONGLONG version);

private:

    // current adapter count
    UINT adapterCount;
    // Pointer to Direct3D9 Object mainained by the pipeline manager
    LPDIRECT3D9 pd3d9;
    // d3d9.dll lib
    HINSTANCE hLibD3D9;

    int currentFSFocusAdapter;
    HWND defaultFocusWindow;

    D3DDEVTYPE devType;

    D3DAdapter *pAdapters;
    // instance of this object
    static LPD3DPIPELINEMANAGER pMgr;
};

#define OS_UNDEFINED    (0 << 0)
#define OS_VISTA        (1 << 0)
#define OS_WINSERV_2008 (1 << 1)
#define OS_WINXP        (1 << 2)
#define OS_WINXP_64     (1 << 3)
#define OS_WINSERV_2003 (1 << 4)
#define OS_WINDOWS7     (1 << 5)
#define OS_WINSERV_2008R2 (1 << 6)
#define OS_ALL (OS_VISTA|OS_WINSERV_2008|OS_WINXP|OS_WINXP_64|OS_WINSERV_2003|\
                OS_WINDOWS7|OS_WINSERV_2008R2)
#define OS_UNKNOWN      (~OS_ALL)
BOOL D3DPPLM_OsVersionMatches(USHORT osInfo);


class D3DInitializer : public AwtToolkit::PreloadAction {
private:
    D3DInitializer();
    ~D3DInitializer();

protected:
    // PreloadAction overrides
    virtual void InitImpl();
    virtual void CleanImpl(bool reInit);

public:
    static D3DInitializer& GetInstance() { return theInstance; }

private:
    // single instance
    static D3DInitializer theInstance;

    // adapter initializer class
    class D3DAdapterInitializer : public AwtToolkit::PreloadAction {
    public:
        void setAdapter(UINT adapter) { this->adapter = adapter; }
    protected:
        // PreloadAction overrides
        virtual void InitImpl();
        virtual void CleanImpl(bool reInit);
    private:
        UINT adapter;
    };

    // the flag indicates success of COM initialization
    bool bComInitialized;
    D3DAdapterInitializer *pAdapterIniters;

};

