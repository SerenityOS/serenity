/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/LoadBase.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSKernelLoadBase::SysFSKernelLoadBase(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSKernelLoadBase> SysFSKernelLoadBase::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSKernelLoadBase(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSKernelLoadBase::try_generate(KBufferBuilder& builder)
{
    auto current_process_credentials = Process::current().credentials();
    if (!current_process_credentials->is_superuser())
        return EPERM;
    return builder.appendff("{}", kernel_load_base);
}

mode_t SysFSKernelLoadBase::permissions() const
{
    // Note: The kernel load address should not be exposed to non-root users
    // as it will help defeat KASLR.
    return S_IRUSR;
}

}
