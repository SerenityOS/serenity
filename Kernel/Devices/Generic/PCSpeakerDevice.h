/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class PCSpeakerDevice final : public CharacterDevice {
    friend class Device;

public:
    virtual ~PCSpeakerDevice() override;

    static NonnullRefPtr<PCSpeakerDevice> must_create();

private:
    PCSpeakerDevice();

    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return true; }

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual StringView class_name() const override { return "PCSpeakerDevice"sv; }
    virtual bool is_seekable() const override { return true; }
};

}
