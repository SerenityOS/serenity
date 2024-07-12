/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Audio/Management.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AudioChannel>> AudioChannel::create(AudioController const& controller, size_t channel_index)
{
    auto channel = TRY(Device::try_create_device<AudioChannel>(controller, channel_index));

    // FIXME: Ideally, we would want the audio controller to run a channel at a device's initial sample
    //        rate instead of hardcoding 44.1 KHz here. However, most audio is provided at 44.1 KHz and as
    //        long as Audio::Resampler introduces significant audio artifacts, let's set a sensible sample
    //        rate here. Remove this after implementing a higher quality Audio::Resampler.
    TRY(const_cast<AudioController&>(controller).set_pcm_output_sample_rate(channel_index, 44100));

    return *channel;
}

AudioChannel::AudioChannel(AudioController const& controller, size_t channel_index)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Audio, AudioManagement::the().generate_storage_minor_number())
    , m_controller(controller)
    , m_channel_index(channel_index)
{
}

ErrorOr<void> AudioChannel::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    auto controller = m_controller.strong_ref();
    if (!controller)
        return Error::from_errno(EIO);
    switch (request) {
    case SOUNDCARD_IOCTL_GET_SAMPLE_RATE: {
        auto output = static_ptr_cast<u32*>(arg);
        u32 sample_rate = 0;
        sample_rate = TRY(controller->get_pcm_output_sample_rate(m_channel_index));
        return copy_to_user(output, &sample_rate);
    }
    case SOUNDCARD_IOCTL_SET_SAMPLE_RATE: {
        auto sample_rate = static_cast<u32>(arg.ptr());
        TRY(controller->set_pcm_output_sample_rate(m_channel_index, sample_rate));
        return {};
    }
    default:
        return EINVAL;
    }
}

bool AudioChannel::can_read(OpenFileDescription const&, u64) const
{
    // FIXME: Implement input from device
    return false;
}

ErrorOr<size_t> AudioChannel::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement input from device
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<size_t> AudioChannel::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    auto controller = m_controller.strong_ref();
    if (!controller)
        return Error::from_errno(EIO);
    return controller->write(m_channel_index, buffer, size);
}

}
