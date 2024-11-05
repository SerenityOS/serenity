/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class AudioController;
class AudioChannel final
    : public CharacterDevice {
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<AudioChannel>> create(AudioController const&, size_t channel_index);
    virtual ~AudioChannel() override = default;

    // ^CharacterDevice
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned, Userspace<void*>) override;

private:
    AudioChannel(AudioController const&, size_t channel_index);

    // ^CharacterDevice
    virtual StringView class_name() const override { return "AudioChannel"sv; }

    LockWeakPtr<AudioController> m_controller;
    size_t const m_channel_index;
};
}
