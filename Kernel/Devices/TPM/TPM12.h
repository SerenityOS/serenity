/*
 * Copyright (c) 2024, Logkos <65683493+logkos@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/TPM/Definitions.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {
class TPMDevice : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullLockRefPtr<TPMDevice> create();
    virtual ~TPMDevice() override;

private:
    TPMDevice();
    virtual StringView class_name() const override { return "TrustedPlatformModule"sv; }

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned, Userspace<void*>) override;
    virtual ErrorOr<void> transmit(u8 const buffer[]);
    bool can_read(OpenFileDescription const&, u64) const override;
    bool can_write(OpenFileDescription const&, u64) const override;

    ErrorOr<void> initialize();
    Memory::TypedMapping<TPM12MMIORegistersLocality0 volatile> m_registers;
    bool m_initialized { false };
};

}
