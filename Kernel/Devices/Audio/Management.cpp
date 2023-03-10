/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/Audio/AC97.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>
#include <Kernel/Devices/Audio/Management.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<AudioManagement> s_the;
static Atomic<u32> s_device_minor_number;

MajorNumber AudioManagement::audio_type_major_number()
{
    return 116;
}
MinorNumber AudioManagement::generate_storage_minor_number()
{
    auto minor_number = s_device_minor_number.load();
    s_device_minor_number++;
    return minor_number;
}

AudioManagement& AudioManagement::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT AudioManagement::AudioManagement()
{
}

UNMAP_AFTER_INIT void AudioManagement::enumerate_hardware_controllers()
{
    if (PCI::Access::is_disabled())
        return;
    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        // Only consider PCI multimedia devices
        if (device_identifier.class_code().value() != to_underlying(PCI::ClassID::Multimedia))
            return;

        auto create_audio_controller = [](PCI::DeviceIdentifier const& device_identifier) -> ErrorOr<NonnullLockRefPtr<AudioController>> {
            switch (static_cast<PCI::Multimedia::SubclassID>(device_identifier.subclass_code().value())) {
            case PCI::Multimedia::SubclassID::AudioController:
                return AC97::try_create(device_identifier);
            case PCI::Multimedia::SubclassID::HDACompatibleController:
                return Audio::IntelHDA::Controller::create(device_identifier);
            default:
                return ENOTSUP;
            }
        };

        dbgln("AudioManagement: found audio controller {} at {}", device_identifier.hardware_id(), device_identifier.address());
        auto audio_controller_device = create_audio_controller(device_identifier);
        if (audio_controller_device.is_error()) {
            dbgln("AudioManagement: failed to initialize audio controller: {}", audio_controller_device.error());
            return;
        }
        m_controllers_list.append(audio_controller_device.release_value());
    }));
}

UNMAP_AFTER_INIT void AudioManagement::enumerate_hardware_audio_channels()
{
    for (auto& controller : m_controllers_list)
        controller.detect_hardware_audio_channels({});
}

UNMAP_AFTER_INIT bool AudioManagement::initialize()
{

    /* Explanation on the flow:
     * 1. Enumerate the PCI bus and try to find audio controllers
     * 2. Ask each controller to detect the audio channels and instantiate AudioChannel objects.
     */
    enumerate_hardware_controllers();
    enumerate_hardware_audio_channels();

    if (m_controllers_list.is_empty()) {
        dbgln("AudioManagement: no audio controller was initialized.");
        return false;
    }
    return true;
}

}
