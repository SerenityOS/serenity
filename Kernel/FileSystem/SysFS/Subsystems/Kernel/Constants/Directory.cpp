/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Constants/ConstantInformation.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Constants/Directory.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSGlobalKernelConstantsDirectory> SysFSGlobalKernelConstantsDirectory::must_create(SysFSDirectory const& parent_directory)
{
    auto global_constants_directory = adopt_ref_if_nonnull(new (nothrow) SysFSGlobalKernelConstantsDirectory(parent_directory)).release_nonnull();
    MUST(global_constants_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.appendff("{}", kernel_load_base));
            auto load_base_buffer = builder.build();
            VERIFY(load_base_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_constants_directory, load_base_buffer.release_nonnull(), S_IRUSR, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::LoadBase));
        }

        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.append(kernel_command_line().string()));
            MUST(builder.append('\n'));
            auto command_line_buffer = builder.build();
            VERIFY(command_line_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_constants_directory, command_line_buffer.release_nonnull(), S_IRUSR | S_IRGRP | S_IROTH, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::CommandLine));
        }

        {
            auto builder = TRY(KBufferBuilder::try_create());
            MUST(builder.append(kernel_command_line().system_mode()));
            MUST(builder.append('\n'));
            auto system_mode_buffer = builder.build();
            VERIFY(system_mode_buffer);
            list.append(SysFSSystemConstantInformation::must_create(*global_constants_directory, system_mode_buffer.release_nonnull(), S_IRUSR | S_IRGRP | S_IROTH, SysFSSystemConstantInformation::ReadableByJailedProcesses::No, SysFSSystemConstantInformation::NodeName::SystemMode));
        }
        return {};
    }));
    return global_constants_directory;
}

UNMAP_AFTER_INIT SysFSGlobalKernelConstantsDirectory::SysFSGlobalKernelConstantsDirectory(SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
