/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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


/**
 * This class encapsulates the array of Win32GraphicsDevices,
 * allowing it to be accessed and recreated from multiple
 * threads in a thread-safe manner.
 *
 * The MT-safeness of the array is assured in the following ways:
 *      - hide the actual array being used so that access to
 *        it can only be made from this class
 *      - Do not delete the array until all references to the
 *        array have released it.  That way, anyone that happens
 *        to have a pointer to an element of the array can still
 *        safely refer to that item, even if the situation has
 *        changed and the array is out of date.
 *      - ensure that the user of the array always gets a non-disposed
 *        instance (before the user is handed over a reference to the
 *        instance, a ref counter of the instance is increased atomically)
 *      - The act of replacing an old encapsulated array
 *        of devices with the new one is protected via common lock
 *
 * Expected usage patterns:
 * 1. The array element will not be used outside of this code block.
 *   {
 *     // first, get the reference to the Devices instance through InstanceAccess
 *     // subclass (this automatically increases ref count of this instance)
 *     Devices::InstanceAccess devices; // increases the ref count of current instance
 *     // Then the object can be used, for example, to retrieve the awt device.
 *     // (note: ref count is not increased with GetDevice())
 *     AwtWin32GraphicsDevice *dev = devices->GetDevice(idx);
 *     dev->DoStuff();
 *     Data data = dev->GetData();
 *     return data;
 *     // don't need to release the reference, it's done automatically in
 *     // InstanceAccess destructor
 *   }
 *
 * 2. The array element will be used outside of this code block (i.e.
 *    saved for later use).
 *   {
 *     Devices::InstanceAccess devices; // increases the ref count
 *     // next call increases the ref count of the instance again
 *     AwtWin32GraphicsDevice *dev = devices->GetDeviceReference(idx);
 *     wsdo->device = dev;
 *     // we saved the ref to the device element, the first reference
 *     // will be released automatically in the InstanceAccess destructor
 *   }
 *
 *   {
 *     wsdo->device->DoStuff(); // safe because we hold a reference
 *     // then, sometime later (different thread, method, whatever)
 *     // release the reference to the array element, which in
 *     // turn will decrease the ref count of the instance of Devices class
 *     // this element belongs to
 *     wsdo->device->Release();
 *     wsdo->device = NULL; // this reference can no longer be used
 *   }
 */

#include "Devices.h"
#include "Trace.h"
#include "D3DPipelineManager.h"


/* Some helper functions (from awt_MMStub.h/cpp) */

int g_nMonitorCounter;
int g_nMonitorLimit;
HMONITOR* g_hmpMonitors;

// Callback for CountMonitors below
BOOL WINAPI clb_fCountMonitors(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP)
{
    g_nMonitorCounter ++;
    return TRUE;
}

int WINAPI CountMonitors(void)
{
    g_nMonitorCounter = 0;
    ::EnumDisplayMonitors(NULL, NULL, clb_fCountMonitors, 0L);
    return g_nMonitorCounter;

}

// Callback for CollectMonitors below
BOOL WINAPI clb_fCollectMonitors(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP)
{

    if ((g_nMonitorCounter < g_nMonitorLimit) && (NULL != g_hmpMonitors)) {
        g_hmpMonitors[g_nMonitorCounter] = hMon;
        g_nMonitorCounter ++;
    }

    return TRUE;
}

int WINAPI CollectMonitors(HMONITOR* hmpMonitors, int nNum)
{
    int retCode = 0;

    if (NULL != hmpMonitors) {

        g_nMonitorCounter   = 0;
        g_nMonitorLimit     = nNum;
        g_hmpMonitors       = hmpMonitors;

        ::EnumDisplayMonitors(NULL, NULL, clb_fCollectMonitors, 0L);

        retCode             = g_nMonitorCounter;

        g_nMonitorCounter   = 0;
        g_nMonitorLimit     = 0;
        g_hmpMonitors       = NULL;

    }
    return retCode;
}

BOOL WINAPI MonitorBounds(HMONITOR hmMonitor, RECT* rpBounds)
{
    BOOL retCode = FALSE;

    if ((NULL != hmMonitor) && (NULL != rpBounds)) {
        MONITORINFOEX miInfo;

        memset((void*)(&miInfo), 0, sizeof(MONITORINFOEX));
        miInfo.cbSize = sizeof(MONITORINFOEX);

        if (TRUE == (retCode = ::GetMonitorInfo(hmMonitor, &miInfo))) {
            (*rpBounds) = miInfo.rcMonitor;
        }
    }
    return retCode;
}

/* End of helper functions */

Devices* Devices::theInstance = NULL;
CriticalSection Devices::arrayLock;

/**
 * Create a new Devices object with numDevices elements.
 */
Devices::Devices(int numDevices)
{
    J2dTraceLn1(J2D_TRACE_INFO, "Devices::Devices numDevices=%d", numDevices);
    this->numDevices = numDevices;
    this->refCount = 0;
    devices = (AwtWin32GraphicsDevice**)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc,
        numDevices, sizeof(AwtWin32GraphicsDevice *));
}

/**
 * Static method which updates the array of the devices
 * while holding global lock.
 *
 * If the update was successful, method returns TRUE,
 * otherwise it returns FALSE.
 */
// static
BOOL Devices::UpdateInstance(JNIEnv *env)
{
    J2dTraceLn(J2D_TRACE_INFO, "Devices::UpdateInstance");

    int numScreens = CountMonitors();
    HMONITOR *monHds = (HMONITOR *)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc,
            numScreens, sizeof(HMONITOR));
    if (numScreens != CollectMonitors(monHds, numScreens)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "Devices::UpdateInstance: Failed to get all "\
                      "monitor handles.");
        free(monHds);
        return FALSE;
    }

    Devices *newDevices = new Devices(numScreens);
    // This way we know that the array will not be disposed of
    // at least until we replaced it with a new one.
    newDevices->AddReference();

    // Create all devices first, then initialize them.  This allows
    // correct configuration of devices after contruction of the
    // primary device (which may not be device 0).
    AwtWin32GraphicsDevice** rawDevices = newDevices->GetRawArray();
    int i;
    for (i = 0; i < numScreens; ++i) {
        J2dTraceLn2(J2D_TRACE_VERBOSE, "  hmon[%d]=0x%x", i, monHds[i]);
        rawDevices[i] = new AwtWin32GraphicsDevice(i, monHds[i], newDevices);
    }
    for (i = 0; i < numScreens; ++i) {
        rawDevices[i]->Initialize();
    }
    {
        CriticalSection::Lock l(arrayLock);

        // install the new devices array
        Devices *oldDevices = theInstance;
        theInstance = newDevices;

        if (oldDevices) {
            // Invalidate the devices with indexes out of the new set of
            // devices. This doesn't cover all cases when the device
            // might should be invalidated (like if it's not the last device
            // that was removed), but it will have to do for now.
            int oldNumScreens = oldDevices->GetNumDevices();
            int newNumScreens = theInstance->GetNumDevices();
            J2dTraceLn(J2D_TRACE_VERBOSE, "  Invalidating removed devices");
            for (int i = newNumScreens; i < oldNumScreens; i++) {
                // removed device, needs to be invalidated
                J2dTraceLn1(J2D_TRACE_WARNING,
                            "Devices::UpdateInstance: device removed: %d", i);
                oldDevices->GetDevice(i)->Invalidate(env);
            }
            // Now that we have a new array in place, remove this (possibly the
            // last) reference to the old instance.
            oldDevices->Release();
        }
        D3DPipelineManager::HandleAdaptersChange((HMONITOR*)monHds,
                                                 theInstance->GetNumDevices());
    }
    free(monHds);

    return TRUE;
}

/**
 * Add a reference to the array.  This could be someone that wants
 * to register interest in the array, versus someone that actually
 * holds a reference to an array item (in which case they would
 * call GetDeviceReference() instead).  This mechanism can keep
 * the array from being deleted when it has no elements being
 * referenced but is still a valid array to use for new elements
 * or references.
 */
void Devices::AddReference()
{
    J2dTraceLn(J2D_TRACE_INFO, "Devices::AddReference");
    CriticalSection::Lock l(arrayLock);
    refCount++;
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  refCount=%d", refCount);
}

/**
 * Static method for getting a reference
 * to the instance of the current devices array.
 * The instance will automatically have reference count increased.
 *
 * The caller thus must call Release() when done dealing with
 * the array.
 */
// static
Devices* Devices::GetInstance()
{
    J2dTraceLn(J2D_TRACE_INFO, "Devices::GetInstance");
    CriticalSection::Lock l(arrayLock);
    if (theInstance != NULL) {
        theInstance->AddReference();
    } else {
        J2dTraceLn(J2D_TRACE_ERROR,
                   "Devices::GetInstance NULL instance");
    }
    return theInstance;
}

/**
 * Retrieve a pointer to an item in the array and register a
 * reference to the array.  This increases the refCount of the
 * instance, used to track when the array can be deleted.
 *
 * This method must be called while holding a reference to the instance.
 *
 * If adjust parameter is true (default), adjust the index into the
 * devices array so that it falls within the current devices array.
 * This is needed because the devices array can be changed at any
 * time, and the index may be from the old array. But in some
 * cases we prefer to know that the index is incorrect.
 *
 */
AwtWin32GraphicsDevice *Devices::GetDeviceReference(int index,
                                                    BOOL adjust)
{
    J2dTraceLn2(J2D_TRACE_INFO,
                "Devices::GetDeviceReference index=%d adjust?=%d",
                index, adjust);

    AwtWin32GraphicsDevice * ret = GetDevice(index, adjust);
    if (ret != NULL) {
        AddReference();
    }
    return ret;
}

/**
 * Returns a reference to a device with the passed index.
 *
 * This method does not increase the ref count of the Devices instance.
 *
 * This method must be called while holding a reference to the instance.
 */
AwtWin32GraphicsDevice *Devices::GetDevice(int index, BOOL adjust)
{
    J2dTraceLn2(J2D_TRACE_INFO,
                "Devices::GetDevice index=%d adjust?=%d",
                index, adjust);
    if (index < 0 || index >= numDevices) {
        if (!adjust) {
            J2dTraceLn1(J2D_TRACE_WARNING,
                        "Devices::GetDevice: "\
                        "incorrect index %d, returning NULL.", index);
            return NULL;
        }
        J2dTraceLn1(J2D_TRACE_WARNING,
                    "Devices::GetDevice: "\
                    "adjusted index %d to 0.", index);
        index = 0;
    }
    return devices[index];
}

/**
 * Returns a raw reference to the incapsulated array.
 *
 * This method does not increase the ref count of the Devices instance.
 *
 * This method must be called while holding a reference to the instance.
 */
AwtWin32GraphicsDevice **Devices::GetRawArray()
{
    J2dTraceLn(J2D_TRACE_INFO, "Devices::GetRawArray");
    return devices;
}


/**
 * Decreases the reference count of the array. If the refCount goes to 0,
 * then there are no more references to the array and all of the
 * array elements, the array itself, and this object can be destroyed.
 *
 * Returns the number of references left after it was decremented.
 */
int Devices::Release()
{
    J2dTraceLn(J2D_TRACE_INFO, "Devices::Release");
    CriticalSection::Lock l(arrayLock);

    int refs = --refCount;

    J2dTraceLn1(J2D_TRACE_VERBOSE, "  refCount=%d", refs);

    if (refs == 0) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  disposing the array");
        if (devices != NULL) {
            for (int i = 0; i < numDevices; ++i) {
                if (devices[i] != NULL) {
                    delete devices[i];
                    devices[i] = NULL;
                }
            }
            free(devices);
            // null out data, can help with debugging
            devices = NULL;
        }
        // it's safe to delete the instance and only
        // then release the static lock
        delete this;
        // for safety return immediately after committing suicide
        // (note: can not reference refCount here!)
        return refs;
    } else if (refs < 0) {
        J2dTraceLn1(J2D_TRACE_ERROR,
                    "Devices::Release: Negative ref count! refCount=%d",
                    refs);
    }

    return refs;
}
