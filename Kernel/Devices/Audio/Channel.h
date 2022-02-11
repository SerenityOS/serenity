/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AudioController;
class AudioChannel final
    : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<AudioChannel> must_create(AudioController const&, size_t channel_index);
    virtual ~AudioChannel() override { }

    // ^CharacterDevice
    virtual bool can_read(const OpenFileDescription&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, u64) const override { return true; }

    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned, Userspace<void*>) override;

private:
    AudioChannel(AudioController const&, size_t channel_index);

    // ^CharacterDevice
    virtual StringView class_name() const override { return "AudioChannel"sv; }

    WeakPtr<AudioController> m_controller;
    const size_t m_channel_index;
};
}
