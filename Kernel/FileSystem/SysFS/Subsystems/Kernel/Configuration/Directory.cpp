/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/CapsLockRemap.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/CoredumpDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/DumpKmallocStack.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/UBSANDeadly.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSKernelConfigurationDirectory> SysFSKernelConfigurationDirectory::must_create(SysFSDirectory const& parent_directory)
{
    auto global_variables_directory = adopt_ref_if_nonnull(new (nothrow) SysFSKernelConfigurationDirectory(parent_directory)).release_nonnull();
    MUST(global_variables_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSCapsLockRemap::must_create(*global_variables_directory));
        list.append(SysFSDumpKmallocStacks::must_create(*global_variables_directory));
        list.append(SysFSUBSANDeadly::must_create(*global_variables_directory));
        list.append(SysFSCoredumpDirectory::must_create(*global_variables_directory));
        return {};
    }));
    return global_variables_directory;
}

UNMAP_AFTER_INIT SysFSKernelConfigurationDirectory::SysFSKernelConfigurationDirectory(SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
