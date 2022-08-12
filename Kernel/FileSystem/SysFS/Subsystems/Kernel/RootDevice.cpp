/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/RootDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSRootDevice::SysFSRootDevice(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSRootDevice> SysFSRootDevice::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSRootDevice(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSRootDevice::try_generate(KBufferBuilder& builder)
{
    TRY(builder.append(kernel_command_line().root_device()));
    TRY(builder.append('\n'));
    return {};
}

}
