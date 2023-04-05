/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Uptime.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSUptime::SysFSUptime(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSUptime> SysFSUptime::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSUptime(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSUptime::try_generate(KBufferBuilder& builder)
{
    return builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
}

}
