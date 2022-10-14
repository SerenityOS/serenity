/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/CommandLine.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSCommandLine> SysFSCommandLine::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSCommandLine(parent_directory)).release_nonnull();
}

UNMAP_AFTER_INIT SysFSCommandLine::SysFSCommandLine(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

ErrorOr<void> SysFSCommandLine::try_generate(KBufferBuilder& builder)
{
    TRY(builder.append(kernel_command_line().string()));
    TRY(builder.append('\n'));
    return {};
}

}
