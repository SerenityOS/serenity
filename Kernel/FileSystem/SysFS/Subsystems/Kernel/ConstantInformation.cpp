/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/ConstantInformation.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

StringView SysFSSystemConstantInformation::name() const
{
    switch (m_node_name) {
    case NodeName::LoadBase:
        return "load_base"sv;
    case NodeName::CommandLine:
        return "cmdline"sv;
    case NodeName::SystemMode:
        return "system_mode"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<SysFSSystemConstantInformation> SysFSSystemConstantInformation::must_create(SysFSDirectory const& parent_directory, NonnullOwnPtr<KBuffer> constant_data_buffer, mode_t mode, ReadableByJailedProcesses readable_by_jailed_processes, NodeName name)
{
    auto node = adopt_ref_if_nonnull(new (nothrow) SysFSSystemConstantInformation(parent_directory, move(constant_data_buffer), mode, readable_by_jailed_processes, name)).release_nonnull();
    return node;
}

ErrorOr<size_t> SysFSSystemConstantInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    if (offset < 0)
        return EINVAL;
    if (static_cast<size_t>(offset) >= m_constant_data_buffer->size())
        return 0;
    if (Process::current().is_jailed() && m_readable_by_jailed_processes == ReadableByJailedProcesses::No)
        return Error::from_errno(EPERM);
    ssize_t nread = min(static_cast<off_t>(m_constant_data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(m_constant_data_buffer->data() + offset, nread));
    return nread;
}

}
