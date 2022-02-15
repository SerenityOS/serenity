/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Audio/Channel.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>

namespace Kernel {

class AudioManagement;
class AudioController
    : public RefCounted<AudioController>
    , public Weakable<AudioController> {
    friend class AudioManagement;

public:
    virtual ~AudioController() = default;

    virtual RefPtr<AudioChannel> audio_channel(u32 index) const = 0;
    virtual ErrorOr<size_t> write(size_t channel_index, UserOrKernelBuffer const& data, size_t length) = 0;

    virtual void detect_hardware_audio_channels(Badge<AudioManagement>) = 0;

    virtual ErrorOr<void> set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate) = 0;
    // Note: The return value is rate of samples per second
    virtual ErrorOr<u32> get_pcm_output_sample_rate(size_t channel_index) = 0;

private:
    IntrusiveListNode<AudioController, RefPtr<AudioController>> m_node;
};
}
