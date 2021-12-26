/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class MemoryDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    static NonnullRefPtr<MemoryDevice> must_create();
    ~MemoryDevice();

    virtual KResultOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

    // FIXME: We expose this constructor to make try_create_device helper to work
    MemoryDevice();

private:
    virtual StringView class_name() const override { return "MemoryDevice"; }
    virtual bool can_read(const OpenFileDescription&, size_t) const override { return true; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return false; }
    virtual bool is_seekable() const override { return true; }
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }

    virtual void did_seek(OpenFileDescription&, off_t) override;

    bool is_allowed_range(PhysicalAddress, Memory::VirtualRange const&) const;
};

}
