/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/Detect.h>
#include <Kernel/Devices/Input/VirtIO/Input.h>
#include <Kernel/Devices/Serial/VirtIO/Console.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random/VirtIO/RNG.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT void detect_pci_instances()
{
    if (kernel_command_line().disable_virtio())
        return;
    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        if (device_identifier.hardware_id().is_null())
            return;
        // TODO: We should also be checking that the device_id is in between 0x1000 - 0x107F inclusive
        if (device_identifier.hardware_id().vendor_id != PCI::VendorID::VirtIO)
            return;
        switch (device_identifier.hardware_id().device_id) {
        case PCI::DeviceID::VirtIOConsole: {
            auto& console = Console::must_create_for_pci_instance(device_identifier).leak_ref();
            MUST(console.initialize_virtio_resources());
            break;
        }
        case PCI::DeviceID::VirtIOEntropy: {
            auto& rng = RNG::must_create_for_pci_instance(device_identifier).leak_ref();
            MUST(rng.initialize_virtio_resources());
            break;
        }
        case PCI::DeviceID::VirtIOGPU: {
            // This should have been initialized by the graphics subsystem
            break;
        }
        case PCI::DeviceID::VirtIOBlockDevice: {
            // This should have been initialized by the storage subsystem
            break;
        }
        case PCI::DeviceID::VirtIOInput: {
            auto& input = Input::must_create_for_pci_instance(device_identifier).leak_ref();
            MUST(input.initialize_virtio_resources());
            break;
        }
        default:
            dbgln_if(VIRTIO_DEBUG, "VirtIO: Unknown VirtIO device with ID: {}", device_identifier.hardware_id().device_id);
            break;
        }
    }));
}

}
