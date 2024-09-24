/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/PowerStateSwitch.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

mode_t SysFSPowerStateSwitchNode::permissions() const
{
    return S_IRUSR | S_IRGRP | S_IWUSR | S_IWGRP;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSPowerStateSwitchNode> SysFSPowerStateSwitchNode::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSPowerStateSwitchNode(parent_directory)).release_nonnull();
}

UNMAP_AFTER_INIT SysFSPowerStateSwitchNode::SysFSPowerStateSwitchNode(SysFSDirectory const& parent_directory)
    : SysFSComponent(parent_directory)
{
}

ErrorOr<void> SysFSPowerStateSwitchNode::truncate(u64 size)
{
    // Note: This node doesn't store any useful data anyway, so we can safely
    // truncate this to zero (essentially ignoring the request without failing).
    if (size != 0)
        return EPERM;
    return {};
}

ErrorOr<size_t> SysFSPowerStateSwitchNode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& data, OpenFileDescription*)
{
    // Note: If we are in a jail, don't let the current process to change the power state.
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return Error::from_errno(EOVERFLOW);
    if (offset > 0)
        return Error::from_errno(EINVAL);
    if (count > 1)
        return Error::from_errno(EINVAL);
    char buf[1];
    TRY(data.read(buf, 1));
    switch (buf[0]) {
    case '1':
        PowerStateSwitchTask::reboot();
        return 1;
    case '2':
        PowerStateSwitchTask::shutdown();
        return 1;
    default:
        return Error::from_errno(EINVAL);
    }
}

}
