/*
 * Copyright (c) 2026, bogdan-glitchm <github.34373732+copyright@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/RequestOops.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Library/Oops.hpp>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSSystemRequestOops> SysFSSystemRequestOops::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSSystemRequestOops(parent_directory)).release_nonnull();
}

ErrorOr<size_t> SysFSSystemRequestOops::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    return Error::from_errno(ENOTSUP);
}

ErrorOr<size_t> SysFSSystemRequestOops::write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    Kernel::oops(Kernel::OopsCode::RequestOopsWriteBytes, "SysFSSystemRequestOops::write_bytes");
    VERIFY_NOT_REACHED();
}

ErrorOr<void> SysFSSystemRequestOops::truncate(u64)
{
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
    Kernel::oops(Kernel::OopsCode::RequestOopsTruncate, "SysFSSystemRequestOops::truncate");
    VERIFY_NOT_REACHED();
}

}
