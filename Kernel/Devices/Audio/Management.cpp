/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Devices/Audio/AC97/AC97.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>
#include <Kernel/Devices/Audio/Management.h>

namespace Kernel {

static Singleton<AudioManagement> s_the;
static Atomic<u32> s_device_minor_number;

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

struct PCIAudioDriverInitializer {
    ErrorOr<bool> (*probe)(PCI::DeviceIdentifier const&) = nullptr;
    ErrorOr<NonnullRefPtr<AudioController>> (*create)(PCI::DeviceIdentifier const&) = nullptr;
};

static constexpr PCIAudioDriverInitializer s_initializers[] = {
    { AC97::probe, AC97::create },
    { Audio::IntelHDA::Controller::probe, Audio::IntelHDA::Controller::create },
};

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AudioController>> AudioManagement::determine_audio_device(PCI::DeviceIdentifier const& device_identifier) const
{
    for (auto& initializer : s_initializers) {
        auto initializer_probe_found_driver_match_or_error = initializer.probe(device_identifier);
        if (initializer_probe_found_driver_match_or_error.is_error()) {
            dmesgln("AudioManagement: Failed to probe device {}, due to {}", device_identifier.address(), initializer_probe_found_driver_match_or_error.error());
            continue;
        }
        auto initializer_probe_found_driver_match = initializer_probe_found_driver_match_or_error.release_value();
        if (initializer_probe_found_driver_match) {
            auto device = TRY(initializer.create(device_identifier));
            TRY(device->initialize({}));
            return device;
        }
    }
    dmesgln("AudioManagement: Failed to initialize device {}, unsupported audio device", device_identifier.address());
    return Error::from_errno(ENODEV);
}

UNMAP_AFTER_INIT void AudioManagement::enumerate_hardware_controllers()
{
    if (PCI::Access::is_disabled())
        return;
    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        // Only consider PCI multimedia devices
        if (device_identifier.class_code() != PCI::ClassID::Multimedia)
            return;

        auto result = determine_audio_device(device_identifier);
        if (result.is_error()) {
            dmesgln("Failed to initialize audio device ({} {}): {}", device_identifier.address(), device_identifier.hardware_id(), result.error());
            return;
        }
        m_controllers_list.with([&](auto& list) { list.append(result.release_value()); });
    }));
}

UNMAP_AFTER_INIT bool AudioManagement::initialize()
{
    enumerate_hardware_controllers();
    auto list_empty = m_controllers_list.with([&](auto& list) -> bool {
        return list.is_empty();
    });
    if (list_empty)
        dbgln("AudioManagement: no audio controller was initialized.");
    return !list_empty;
}

}
