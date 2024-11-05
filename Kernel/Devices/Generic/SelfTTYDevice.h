/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class SelfTTYDevice final : public CharacterDevice {
    friend class Device;

public:
    static NonnullRefPtr<SelfTTYDevice> must_create();
    virtual ~SelfTTYDevice() override;

private:
    SelfTTYDevice();

    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return true; }

    // ^CharacterDevice
    virtual ErrorOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual StringView class_name() const override { return "SelfTTYDevice"sv; }
};

}
