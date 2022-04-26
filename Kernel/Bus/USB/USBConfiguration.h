/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBInterface.h>

namespace Kernel::USB {

class Device;

class USBConfiguration {
public:
    USBConfiguration() = delete;
    USBConfiguration(Device& device, USBConfigurationDescriptor const descriptor)
        : m_device(device)
        , m_descriptor(descriptor)
    {
        m_interfaces.ensure_capacity(descriptor.number_of_interfaces);
    }

    Device const& device() const { return m_device; }
    USBConfigurationDescriptor const& descriptor() const { return m_descriptor; }

    u8 interface_count() const { return m_descriptor.number_of_interfaces; }
    u8 configuration_id() const { return m_descriptor.configuration_value; }
    u8 attributes() const { return m_descriptor.attributes_bitmap; }
    u16 max_power_ma() const { return m_descriptor.max_power_in_ma * 2u; } // Note: "Power" is used incorrectly here, however it's what it's called in the descriptor/documentation

    ErrorOr<void> get_interfaces();

private:
    Device& m_device;                              // Reference to the device linked to this configuration
    USBConfigurationDescriptor const m_descriptor; // Descriptor that backs this configuration
    Vector<USBInterface> m_interfaces;             // Interfaces for this device
};

}
