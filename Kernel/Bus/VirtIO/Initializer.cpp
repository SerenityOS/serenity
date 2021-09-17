/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Console.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Initializer.h>
#include <Kernel/Bus/VirtIO/RNG.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

static InitializationResult try_to_initialize_device(const PCI::Address& address, PCI::ID id)
{
    if (address.is_null() || id.is_null())
        return InitializationResult(InitializationState::Unknown);
    // TODO: We should also be checking that the device_id is in between 0x1000 - 0x107F inclusive
    if (id.vendor_id != PCI::VendorID::VirtIO)
        return InitializationResult(InitializationState::Unknown);
    switch (id.device_id) {
    case PCI::DeviceID::VirtIOConsole: {
        auto console = TRY(Console::try_create(address));
        [[maybe_unused]] auto& unused = console.leak_ref();
        return InitializationResult(InitializationState::OK);
    }
    case PCI::DeviceID::VirtIOEntropy: {
        auto rng = TRY(RNG::try_create(address));
        [[maybe_unused]] auto& unused = rng.leak_ref();
        return InitializationResult(InitializationState::OK);
    }
    case PCI::DeviceID::VirtIOGPU: {
        // This should have been initialized by the graphics subsystem
        break;
    }
    default:
        dbgln_if(VIRTIO_DEBUG, "VirtIO: Unknown VirtIO device with ID: {}", id.device_id);
        break;
    }
    return InitializationResult(InitializationState::Unknown);
}

UNMAP_AFTER_INIT void Initializer::detect()
{
    if (kernel_command_line().disable_virtio())
        return;
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        // FIXME: Do something with the result
        [[maybe_unused]] auto unused = try_to_initialize_device(address, id);
    });
}

}
