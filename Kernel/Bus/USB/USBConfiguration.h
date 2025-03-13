/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBInterface.h>

namespace Kernel::USB {

class Device;

class USBConfiguration {
public:
    USBConfiguration() = delete;
    USBConfiguration(Device& device, USBConfigurationDescriptor const descriptor, u8 descriptor_index)
        : m_device(&device)
        , m_descriptor(descriptor)
        , m_descriptor_index(descriptor_index)
    {
        m_interfaces.ensure_capacity(descriptor.number_of_interfaces);
    }

private:
    USBConfiguration(USBConfiguration const&);

public:
    USBConfiguration(USBConfiguration&&);
    USBConfiguration copy() const { return USBConfiguration(*this); }

    Device const& device() const { return *m_device; }
    void set_device(Badge<Device>, Device& device) { m_device = &device; }
    USBConfigurationDescriptor const& descriptor() const { return m_descriptor; }

    u8 interface_count() const { return m_descriptor.number_of_interfaces; }
    u8 configuration_id() const { return m_descriptor.configuration_value; }
    u8 attributes() const { return m_descriptor.attributes_bitmap; }
    u16 max_power_ma() const { return m_descriptor.max_power_in_ma * 2u; } // Note: "Power" is used incorrectly here, however it's what it's called in the descriptor/documentation

    Vector<USBInterface> const& interfaces() const { return m_interfaces; }

    template<CallableAs<ErrorOr<IterationDecision>, ReadonlyBytes> Callback>
    ErrorOr<void> for_each_descriptor_in_interface(USBInterface const& interface, Callback&& callback) const
    {
        auto stream = FixedMemoryStream(m_descriptor_hierarchy_buffer.span());
        TRY(stream.seek(interface.descriptor_offset({}), SeekMode::SetPosition));

        auto const interface_descriptor = TRY(stream.read_value<USBInterfaceDescriptor>());

        VERIFY(__builtin_memcmp(&interface_descriptor, &interface.descriptor(), sizeof(USBInterfaceDescriptor)) == 0);

        while (!stream.is_eof()) {
            auto const descriptor_header = TRY(stream.read_value<USBDescriptorCommon>());

            if (descriptor_header.descriptor_type == DESCRIPTOR_TYPE_INTERFACE)
                break;

            ReadonlyBytes descriptor_data { m_descriptor_hierarchy_buffer.span().slice(stream.offset() - sizeof(USBDescriptorCommon), descriptor_header.length) };
            if (TRY(callback(descriptor_data)) == IterationDecision::Break)
                return {};

            TRY(stream.seek(descriptor_header.length - sizeof(USBDescriptorCommon), SeekMode::FromCurrentPosition));
        }

        return {};
    }

    ErrorOr<void> enumerate_interfaces();

private:
    Device* m_device;                              // Reference to the device linked to this configuration
    USBConfigurationDescriptor const m_descriptor; // Descriptor that backs this configuration
    u8 m_descriptor_index;                         // Descriptor index for {GET,SET}_DESCRIPTOR
    Vector<USBInterface> m_interfaces;             // Interfaces for this device

    FixedArray<u8> m_descriptor_hierarchy_buffer; // Buffer for us to store the entire hierarchy into
};

}
