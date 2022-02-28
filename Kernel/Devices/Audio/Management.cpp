/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/Audio/AC97.h>
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
    PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        // Note: Only consider PCI audio controllers
        if (device_identifier.class_code().value() != to_underlying(PCI::ClassID::Multimedia)
            || device_identifier.subclass_code().value() != to_underlying(PCI::Multimedia::SubclassID::AudioController))
            return;

        dbgln("AC97: found audio controller at {}", device_identifier.address());
        auto ac97_device = AC97::try_create(device_identifier);
        if (ac97_device.is_error()) {
            // FIXME: Propagate errors properly
            dbgln("AudioManagement: failed to initialize AC97 device: {}", ac97_device.error());
            return;
        }
        m_controllers_list.append(ac97_device.release_value());
    });
}

UNMAP_AFTER_INIT void AudioManagement::enumerate_hardware_audio_channels()
{
    for (auto& controller : m_controllers_list)
        controller.detect_hardware_audio_channels({});
}

UNMAP_AFTER_INIT bool AudioManagement::initialize()
{

    /* Explanation on the flow:
     * 1. Enumerate all audio controllers connected to the system:
     *  a. Try to find the SB16 ISA-based controller.
     *  b. Enumerate the PCI bus and try to find audio controllers there too
     * 2. Ask each controller to detect the audio channels and instantiate AudioChannel objects.
     */
    enumerate_hardware_controllers();
    enumerate_hardware_audio_channels();

    if (m_controllers_list.is_empty()) {
        dbgln("No audio controller was initialized.");
        return false;
    }
    return true;
}

}
