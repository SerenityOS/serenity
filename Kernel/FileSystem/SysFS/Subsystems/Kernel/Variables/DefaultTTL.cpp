/*
 * Copyright (c) 2023, Mark Douglas <dmarkd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/DefaultTTL.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSDefaultTTL::SysFSDefaultTTL(SysFSDirectory const& parent_directory)
    : SysFSSystemIntegerVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDefaultTTL> SysFSDefaultTTL::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSDefaultTTL(parent_directory)).release_nonnull();
}

int32_t SysFSDefaultTTL::value() const
{
    return NetworkingManagement::default_ttl();
}
void SysFSDefaultTTL::set_value(i32 new_value)
{
    dbgln("DefaultTTL new value: {}", new_value);
    u8 const clamped_value = clamp(new_value, 1, 255);
    NetworkingManagement::set_default_ttl(clamped_value);
}

}
