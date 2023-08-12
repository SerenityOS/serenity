/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSSystemConstantInformation final : public SysFSComponent {
public:
    enum class NodeName {
        LoadBase,
        CommandLine,
        SystemMode,
    };

    enum class ReadableByJailedProcesses {
        Yes,
        No,
    };

    virtual StringView name() const override;
    static NonnullRefPtr<SysFSSystemConstantInformation> must_create(SysFSDirectory const& parent_directory, NonnullOwnPtr<KBuffer> constant_data_buffer, mode_t mode, ReadableByJailedProcesses readable_by_jailed_processes, NodeName name);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

private:
    SysFSSystemConstantInformation(SysFSDirectory const& parent_directory, NonnullOwnPtr<KBuffer> constant_data_buffer, mode_t mode, ReadableByJailedProcesses readable_by_jailed_processes, NodeName name)
        : SysFSComponent(parent_directory)
        , m_constant_data_buffer(move(constant_data_buffer))
        , m_permissions(mode)
        , m_readable_by_jailed_processes(readable_by_jailed_processes)
        , m_node_name(name)
    {
    }

    virtual size_t size() const override { return m_constant_data_buffer->size(); }
    virtual mode_t permissions() const override { return m_permissions; }

    NonnullOwnPtr<KBuffer> m_constant_data_buffer;
    mode_t const m_permissions { 0644 };
    ReadableByJailedProcesses const m_readable_by_jailed_processes { ReadableByJailedProcesses::No };
    NodeName const m_node_name { NodeName::LoadBase };
};

}
