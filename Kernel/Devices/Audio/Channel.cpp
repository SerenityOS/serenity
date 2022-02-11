/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Audio/Management.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<AudioChannel> AudioChannel::must_create(AudioController const& controller, size_t channel_index)
{
    auto audio_device_or_error = DeviceManagement::try_create_device<AudioChannel>(controller, channel_index);
    // FIXME: Find a way to propagate errors
    VERIFY(!audio_device_or_error.is_error());
    return audio_device_or_error.release_value();
}

AudioChannel::AudioChannel(AudioController const& controller, size_t channel_index)
    : CharacterDevice(AudioManagement::the().audio_type_major_number(), AudioManagement::the().generate_storage_minor_number())
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

bool AudioChannel::can_read(const OpenFileDescription&, u64) const
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
