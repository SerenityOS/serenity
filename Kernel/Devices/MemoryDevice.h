/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class MemoryDevice final : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<MemoryDevice> must_create();
    ~MemoryDevice();

    virtual ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

private:
    MemoryDevice();

    virtual StringView class_name() const override { return "MemoryDevice"sv; }
    virtual bool can_read(const OpenFileDescription&, u64) const override { return true; }
    virtual bool can_write(const OpenFileDescription&, u64) const override { return false; }
    virtual bool is_seekable() const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }

    bool is_allowed_range(PhysicalAddress, Memory::VirtualRange const&) const;
};

}
