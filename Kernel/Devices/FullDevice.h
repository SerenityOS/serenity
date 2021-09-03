/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class FullDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
    friend class DeviceManagement;

public:
    static NonnullRefPtr<FullDevice> must_create();
    virtual ~FullDevice() override;

private:
    FullDevice();

    // ^CharacterDevice
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_read() const override;
    virtual bool can_write() const override { return true; }
    virtual StringView class_name() const override { return "FullDevice"; }
};

}
