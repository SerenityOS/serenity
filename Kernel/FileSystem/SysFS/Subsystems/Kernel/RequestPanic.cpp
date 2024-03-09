/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/RequestPanic.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSSystemRequestPanic> SysFSSystemRequestPanic::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSSystemRequestPanic(parent_directory)).release_nonnull();
}

ErrorOr<size_t> SysFSSystemRequestPanic::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    return Error::from_errno(ENOTSUP);
}

ErrorOr<size_t> SysFSSystemRequestPanic::write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    PANIC("SysFSSystemRequestPanic::write_bytes");
    VERIFY_NOT_REACHED();
}

ErrorOr<void> SysFSSystemRequestPanic::truncate(u64)
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    PANIC("SysFSSystemRequestPanic::truncate");
    VERIFY_NOT_REACHED();
}

}
