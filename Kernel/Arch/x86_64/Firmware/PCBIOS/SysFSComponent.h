/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

class SysFSPCBIOSComponent final : public SysFSComponent {
public:
    enum class Type {
        DMIEntryPoint,
        SMBIOSTable,
    };

public:
    static NonnullRefPtr<SysFSPCBIOSComponent> must_create(Type, PhysicalAddress, size_t blob_size);
    virtual StringView name() const override;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

private:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    SysFSPCBIOSComponent(Type, PhysicalAddress, size_t blob_size);

    virtual size_t size() const override { return m_blob_length; }

    PhysicalAddress const m_blob_paddr;
    size_t const m_blob_length { 0 };
    Type const m_type { Type::DMIEntryPoint };
};
}
