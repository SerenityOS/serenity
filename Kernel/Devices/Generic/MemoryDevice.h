/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

class MemoryDevice final : public CharacterDevice {
    friend class Device;

public:
    static NonnullRefPtr<MemoryDevice> must_create();
    ~MemoryDevice();

    virtual ErrorOr<VMObjectAndMemoryType> vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64& offset, bool shared) override;

private:
    MemoryDevice();

    virtual StringView class_name() const override { return "MemoryDevice"sv; }
    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return false; }
    virtual bool is_seekable() const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return EINVAL; }
};

}
