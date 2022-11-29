/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/CapsLockRemap.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/DumpKmallocStack.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/UBSANDeadly.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSGlobalKernelVariablesDirectory> SysFSGlobalKernelVariablesDirectory::must_create(SysFSDirectory const& parent_directory)
{
    auto global_variables_directory = adopt_lock_ref_if_nonnull(new (nothrow) SysFSGlobalKernelVariablesDirectory(parent_directory)).release_nonnull();
    MUST(global_variables_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSCapsLockRemap::must_create(*global_variables_directory));
        list.append(SysFSDumpKmallocStacks::must_create(*global_variables_directory));
        list.append(SysFSUBSANDeadly::must_create(*global_variables_directory));
        return {};
    }));
    return global_variables_directory;
}

UNMAP_AFTER_INIT SysFSGlobalKernelVariablesDirectory::SysFSGlobalKernelVariablesDirectory(SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
