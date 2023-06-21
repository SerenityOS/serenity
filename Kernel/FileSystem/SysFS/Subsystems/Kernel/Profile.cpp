/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Profile.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSProfile::SysFSProfile(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSProfile> SysFSProfile::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSProfile(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSProfile::try_generate(KBufferBuilder& builder)
{
    if (!g_global_perf_events)
        return ENOENT;
    TRY(g_global_perf_events->to_json(builder));
    return {};
}

mode_t SysFSProfile::permissions() const
{
    return S_IRUSR;
}

}
