/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/SystemMode.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSSystemMode::SysFSSystemMode(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSSystemMode> SysFSSystemMode::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSSystemMode(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSSystemMode::try_generate(KBufferBuilder& builder)
{
    TRY(builder.append(kernel_command_line().system_mode()));
    TRY(builder.append('\n'));
    return {};
}

}
