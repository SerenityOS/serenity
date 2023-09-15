/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBConfiguration.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::USB {

ErrorOr<void> USBConfiguration::enumerate_interfaces()
{
    auto descriptor_hierarchy_buffer = TRY(FixedArray<u8>::create(m_descriptor.total_length)); // Buffer for us to store the entire hierarchy into

    // The USB spec is a little bit janky here... Interface and Endpoint descriptors aren't fetched
    // through a `GET_DESCRIPTOR` request to the device. Instead, the _entire_ hierarchy is returned
    // to us in one go.
    auto transfer_length = TRY(m_device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_CONFIGURATION << 8), 0, m_descriptor.total_length, descriptor_hierarchy_buffer.data()));

    // FIXME: Why does transfer length return the actual size +8 bytes?
    if (transfer_length < m_descriptor.total_length)
        return EIO;

    u8* interface_descriptors_base = descriptor_hierarchy_buffer.data() + sizeof(USBConfigurationDescriptor);
    USBInterfaceDescriptor* interface_descriptor = reinterpret_cast<USBInterfaceDescriptor*>(interface_descriptors_base);
    Vector<USBEndpointDescriptor> endpoint_descriptors;
    TRY(m_interfaces.try_ensure_capacity(m_descriptor.number_of_interfaces));
    for (auto interface = 0u; interface < m_descriptor.number_of_interfaces; interface++) {
        endpoint_descriptors.ensure_capacity(interface_descriptor->number_of_endpoints);

        if constexpr (USB_DEBUG) {
            dbgln("Interface Descriptor {}", interface);
            dbgln("interface_id: {:02x}", interface_descriptor->interface_id);
            dbgln("alternate_setting: {:02x}", interface_descriptor->alternate_setting);
            dbgln("number_of_endpoints: {:02x}", interface_descriptor->number_of_endpoints);
            dbgln("interface_class_code: {:02x}", interface_descriptor->interface_class_code);
            dbgln("interface_sub_class_code: {:02x}", interface_descriptor->interface_sub_class_code);
            dbgln("interface_protocol: {:02x}", interface_descriptor->interface_protocol);
            dbgln("interface_string_descriptor_index: {}", interface_descriptor->interface_string_descriptor_index);
        }

        // Get all the endpoint descriptors
        for (auto endpoint = 0u; endpoint < interface_descriptor->number_of_endpoints; endpoint++) {
            u8* raw_endpoint_descriptor_offset = interface_descriptors_base + sizeof(USBInterfaceDescriptor) + (endpoint * sizeof(USBEndpointDescriptor));

            // FIXME: It looks like HID descriptors come BEFORE the endpoint descriptors for a HID device, so we should load
            // these too eventually.
            // See here: https://www.usb.org/defined-class-codes
            if (interface_descriptor->interface_class_code == USB_CLASS_HID)
                raw_endpoint_descriptor_offset += sizeof(USBHIDDescriptor); // Skip the HID descriptor (this was worked out via buffer inspection)

            USBEndpointDescriptor endpoint_descriptor;
            memcpy(&endpoint_descriptor, raw_endpoint_descriptor_offset, sizeof(USBEndpointDescriptor));

            if constexpr (USB_DEBUG) {
                dbgln("Endpoint Descriptor {}", endpoint);
                dbgln("Endpoint Address: {}", endpoint_descriptor.endpoint_address);
                dbgln("Endpoint Attribute Bitmap: {:08b}", endpoint_descriptor.endpoint_attributes_bitmap);
                dbgln("Endpoint Maximum Packet Size: {}", endpoint_descriptor.max_packet_size);
                dbgln("Endpoint Poll Interval (in frames): {}", endpoint_descriptor.poll_interval_in_frames);
            }

            endpoint_descriptors.unchecked_append(endpoint_descriptor);
        }

        USBInterface device_interface(*this, *interface_descriptor, move(endpoint_descriptors));
        m_interfaces.unchecked_append(move(device_interface));
        interface_descriptor += interface_descriptor->number_of_endpoints * sizeof(USBEndpointDescriptor);
    }

    return {};
}

}
