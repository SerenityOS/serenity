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
public:
    NullDevice();
    virtual ~NullDevice() override;

    static void initialize();
    static NullDevice& the();

    // ^Device
    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return "null"; }

private:
    // ^CharacterDevice
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_write(FileDescription const&, size_t) const override { return true; }
    virtual bool can_read(FileDescription const&, size_t) const override;
    virtual StringView class_name() const override { return "NullDevice"; }
    virtual bool is_seekable() const override { return true; }
};

}
