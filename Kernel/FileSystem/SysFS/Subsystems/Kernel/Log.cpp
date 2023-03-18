/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Log.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSKernelLog::SysFSKernelLog(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSKernelLog> SysFSKernelLog::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSKernelLog(parent_directory)).release_nonnull();
}

mode_t SysFSKernelLog::permissions() const
{
    return S_IRUSR;
}

ErrorOr<void> SysFSKernelLog::try_generate(KBufferBuilder& builder)
{
    VERIFY(DeviceManagement::the().is_console_device_attached());
    SpinlockLocker lock(g_console_lock);
    for (char ch : DeviceManagement::the().console_device().logbuffer()) {
        TRY(builder.append(ch));
    }
    return {};
}

}
