/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class ZeroDevice final : public CharacterDevice {
    friend class Device;

public:
    static NonnullRefPtr<ZeroDevice> must_create();
    virtual ~ZeroDevice() override;

private:
    ZeroDevice();

    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return true; }

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual StringView class_name() const override { return "ZeroDevice"sv; }
};

}
