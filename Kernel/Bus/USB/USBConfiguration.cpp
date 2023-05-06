/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBConfiguration.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/HID/Management.h>
#include <Kernel/Devices/HID/USB/MouseDevice.h>
#include <Kernel/Devices/HID/USB/KeyboardDevice.h>
#include <Kernel/StdLib.h>

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
            if (interface_descriptor->interface_class_code == USB_CLASS_HID) {
                USBEndpointDescriptor endpoint_descriptor;
                memcpy(&endpoint_descriptor, raw_endpoint_descriptor_offset, sizeof(USBEndpointDescriptor));

                if constexpr (USB_DEBUG) {
                    dbgln("Endpoint Descriptor {}", endpoint);
                    dbgln("Endpoint Address: {}", endpoint_descriptor.endpoint_address);
                    dbgln("Endpoint Attribute Bitmap: {:08b}", endpoint_descriptor.endpoint_attributes_bitmap);
                    dbgln("Endpoint Maximum Packet Size: {}", endpoint_descriptor.max_packet_size);
                    dbgln("Endpoint Poll Interval (in frames): {}", endpoint_descriptor.poll_interval_in_frames);
                }

                // FIXME: Allow to set different configurations...
                USBConfigurationDescriptor descriptor;
                memcpy(&descriptor, &m_descriptor, sizeof(USBConfigurationDescriptor));
                transfer_length = TRY(m_device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_SET_CONFIGURATION, 1, 0, sizeof(USBConfigurationDescriptor), &descriptor));
                if constexpr (USB_DEBUG) {
                    dbgln("USB device configuration was set!");
                }

                // FIXME: Detect USB HID mouse in an abstracted way...
                if (interface_descriptor->interface_sub_class_code == 0x1 && interface_descriptor->interface_protocol == 0x2) {
                    auto interrupt_in_pipe = TRY(USB::InterruptInPipe::create(device().controller(), endpoint_descriptor.endpoint_address, endpoint_descriptor.max_packet_size, device().address(), 10));
                    m_device.set_interrupt_in_pipe({}, move(interrupt_in_pipe));
                    auto mouse_device = TRY(USBMouseDevice::try_create_instance(m_device));
                    HIDManagement::the().attach_standalone_hid_device(*mouse_device);
                }

                // FIXME: Detect USB HID keyboard in an abstracted way...
                if (interface_descriptor->interface_sub_class_code == 0x1 && interface_descriptor->interface_protocol == 0x1) {
                    auto interrupt_in_pipe = TRY(USB::InterruptInPipe::create(device().controller(), endpoint_descriptor.endpoint_address, endpoint_descriptor.max_packet_size, device().address(), 10));
                    m_device.set_interrupt_in_pipe({}, move(interrupt_in_pipe));
                    auto keyboard_device = TRY(USBKeyboardDevice::try_create_instance(m_device));
                    HIDManagement::the().attach_standalone_hid_device(*keyboard_device);
                }

                raw_endpoint_descriptor_offset += sizeof(USBHIDDescriptor); // Skip the HID descriptor (this was worked out via buffer inspection)
                endpoint_descriptors.append(endpoint_descriptor);
                continue;
            }

            USBEndpointDescriptor endpoint_descriptor;
            memcpy(&endpoint_descriptor, raw_endpoint_descriptor_offset, sizeof(USBEndpointDescriptor));

            if constexpr (USB_DEBUG) {
                dbgln("Endpoint Descriptor {}", endpoint);
                dbgln("Endpoint Address: {}", endpoint_descriptor.endpoint_address);
                dbgln("Endpoint Attribute Bitmap: {:08b}", endpoint_descriptor.endpoint_attributes_bitmap);
                dbgln("Endpoint Maximum Packet Size: {}", endpoint_descriptor.max_packet_size);
                dbgln("Endpoint Poll Interval (in frames): {}", endpoint_descriptor.poll_interval_in_frames);
            }

            endpoint_descriptors.append(endpoint_descriptor);
        }

        USBInterface device_interface(*this, *interface_descriptor, endpoint_descriptors);
        m_interfaces.append(device_interface);
        interface_descriptor += interface_descriptor->number_of_endpoints * sizeof(USBEndpointDescriptor);
    }

    return {};
}

}
