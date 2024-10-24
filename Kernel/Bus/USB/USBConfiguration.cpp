/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/MemoryStream.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBConfiguration.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::USB {

USBConfiguration::USBConfiguration(USBConfiguration const& other)
    : m_device(other.m_device)
    , m_descriptor(other.m_descriptor)
    , m_descriptor_index(other.m_descriptor_index)
    , m_interfaces(other.m_interfaces)
{
    // FIXME: This can definitely OOM
    for (auto& interface : m_interfaces)
        interface.set_configuration({}, *this);
}

USBConfiguration::USBConfiguration(USBConfiguration&& other)
    : m_device(other.m_device)
    , m_descriptor(other.m_descriptor)
    , m_descriptor_index(other.m_descriptor_index)
    , m_interfaces(move(other.m_interfaces))
{
    for (auto& interface : m_interfaces)
        interface.set_configuration({}, *this);
}

ErrorOr<void> USBConfiguration::enumerate_interfaces()
{
    if (m_descriptor.total_length < sizeof(USBConfigurationDescriptor))
        return EINVAL;

    m_descriptor_hierarchy_buffer = TRY(FixedArray<u8>::create(m_descriptor.total_length)); // Buffer for us to store the entire hierarchy into

    // The USB spec is a little bit janky here... Interface and Endpoint descriptors aren't fetched
    // through a `GET_DESCRIPTOR` request to the device. Instead, the _entire_ hierarchy is returned
    // to us in one go.
    auto transfer_length = TRY(m_device->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_CONFIGURATION << 8) | m_descriptor_index, 0, m_descriptor.total_length, m_descriptor_hierarchy_buffer.data()));

    FixedMemoryStream stream { m_descriptor_hierarchy_buffer.span() };

    // FIXME: Why does transfer length return the actual size +8 bytes?
    if (transfer_length < m_descriptor.total_length)
        return EIO;

    auto const configuration_descriptor = TRY(stream.read_value<USBConfigurationDescriptor>());
    if (configuration_descriptor.descriptor_header.length < sizeof(USBConfigurationDescriptor))
        return EINVAL;

    if (configuration_descriptor.total_length != m_descriptor.total_length)
        return EINVAL;

    USBInterface* current_interface = nullptr;

    TRY(m_interfaces.try_ensure_capacity(m_descriptor.number_of_interfaces));

    auto read_descriptor = [&stream]<typename T>(USBDescriptorCommon const& header) -> ErrorOr<T> {
        if (header.length < sizeof(T))
            return EINVAL;

        auto descriptor = TRY(stream.read_value<T>());

        // Skip additional bytes.
        TRY(stream.seek(header.length - sizeof(T), SeekMode::FromCurrentPosition));

        return descriptor;
    };

    while (!stream.is_eof()) {
        // Peek the descriptor header.
        auto const descriptor_header = TRY(stream.read_value<USBDescriptorCommon>());
        MUST(stream.seek(-sizeof(USBDescriptorCommon), SeekMode::FromCurrentPosition));

        switch (descriptor_header.descriptor_type) {
        case DESCRIPTOR_TYPE_INTERFACE: {
            auto offset = stream.offset();
            auto interface_descriptor = TRY(read_descriptor.operator()<USBInterfaceDescriptor>(descriptor_header));

            if constexpr (USB_DEBUG) {
                dbgln("Interface Descriptor:");
                dbgln("  interface_id: {:02x}", interface_descriptor.interface_id);
                dbgln("  alternate_setting: {:02x}", interface_descriptor.alternate_setting);
                dbgln("  number_of_endpoints: {:02x}", interface_descriptor.number_of_endpoints);
                dbgln("  interface_class_code: {:02x}", interface_descriptor.interface_class_code);
                dbgln("  interface_sub_class_code: {:02x}", interface_descriptor.interface_sub_class_code);
                dbgln("  interface_protocol: {:02x}", interface_descriptor.interface_protocol);
                dbgln("  interface_string_descriptor_index: {}", interface_descriptor.interface_string_descriptor_index);
            }

            TRY(m_interfaces.try_empend(*this, interface_descriptor, offset));
            current_interface = &m_interfaces.last();
            break;
        }

        case DESCRIPTOR_TYPE_ENDPOINT: {
            auto endpoint_descriptor = TRY(read_descriptor.operator()<USBEndpointDescriptor>(descriptor_header));

            if constexpr (USB_DEBUG) {
                dbgln("Endpoint Descriptor:");
                dbgln("  Endpoint Address: {}", endpoint_descriptor.endpoint_address);
                dbgln("  Endpoint Attribute Bitmap: {:08b}", endpoint_descriptor.endpoint_attributes_bitmap);
                dbgln("  Endpoint Maximum Packet Size: {}", endpoint_descriptor.max_packet_size);
                dbgln("  Endpoint Poll Interval (in frames): {}", endpoint_descriptor.poll_interval_in_frames);
            }

            if (current_interface == nullptr)
                return EINVAL;

            TRY(current_interface->add_endpoint_descriptor({}, endpoint_descriptor));

            break;
        }

        default:
            dbgln_if(USB_DEBUG, "Skipping descriptor of unknown type {}", descriptor_header.descriptor_type);
            TRY(stream.seek(descriptor_header.length, SeekMode::FromCurrentPosition));
            break;
        }
    }

    return {};
}

}
