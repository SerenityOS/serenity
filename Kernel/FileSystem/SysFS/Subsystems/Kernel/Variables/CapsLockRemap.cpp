/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/CapsLockRemap.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSCapsLockRemap::SysFSCapsLockRemap(SysFSDirectory const& parent_directory)
    : SysFSSystemBoolean(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSCapsLockRemap> SysFSCapsLockRemap::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSCapsLockRemap(parent_directory)).release_nonnull();
}

bool SysFSCapsLockRemap::value() const
{
    MutexLocker locker(m_lock);
    return g_caps_lock_remapped_to_ctrl.load();
}
void SysFSCapsLockRemap::set_value(bool new_value)
{
    MutexLocker locker(m_lock);
    g_caps_lock_remapped_to_ctrl.exchange(new_value);
}

}
