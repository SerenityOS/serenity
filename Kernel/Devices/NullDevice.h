/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class NullDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
    friend class DeviceManagement;

public:
    virtual ~NullDevice() override;

    static NonnullRefPtr<NullDevice> must_initialize();

private:
    NullDevice();
    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return true; }
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual StringView class_name() const override { return "NullDevice"sv; }
    virtual bool is_seekable() const override { return true; }
};

}
