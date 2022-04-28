/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

struct [[gnu::packed]] USBDescriptorCommon {
    u8 length;
    u8 descriptor_type;
};

//
//  Device Descriptor
//  =================
//
//  This descriptor type (stored on the device), represents the device, and gives
//  information related to it, such as the USB specification it complies to,
//  as well as the vendor and product ID of the device.
//
//  https://beyondlogic.org/usbnutshell/usb5.shtml#DeviceDescriptors
struct [[gnu::packed]] USBDeviceDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 usb_spec_compliance_bcd;
    u8 device_class;
    u8 device_sub_class;
    u8 device_protocol;
    u8 max_packet_size;
    u16 vendor_id;
    u16 product_id;
    u16 device_release_bcd;
    u8 manufacturer_id_descriptor_index;
    u8 product_string_descriptor_index;
    u8 serial_number_descriptor_index;
    u8 num_configurations;
};
static_assert(sizeof(USBDeviceDescriptor) == 18);

//
//  Configuration Descriptor
//  ========================
//
//  A USB device can have multiple configurations, which tells us about how the
//  device is physically configured (e.g how it's powered, max power consumption etc).
//
struct [[gnu::packed]] USBConfigurationDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 total_length;
    u8 number_of_interfaces;
    u8 configuration_value;
    u8 configuration_string_descriptor_index;
    u8 attributes_bitmap;
    u8 max_power_in_ma;
};

//
//  Interface Descriptor
//  ====================
//
//  An interface descriptor describes to us one or more endpoints, grouped
//  together to define a singular function of a device.
//  As an example, a USB webcam might have two interface descriptors; one
//  for the camera, and one for the microphone.
//
struct [[gnu::packed]] USBInterfaceDescriptor {
    USBDescriptorCommon descriptor_header;
    u8 interface_id;
    u8 alternate_setting;
    u8 number_of_endpoints;
    u8 interface_class_code;
    u8 interface_sub_class_code;
    u8 interface_protocol;
    u8 interface_string_descriptor_index;
};

//
//  Endpoint Descriptor
//  ===================
//
//  The lowest leaf in the configuration tree. And endpoint descriptor describes
//  the physical transfer properties of the endpoint (that isn't endpoint0).
//  The description given by this structure is used by a pipe to create a
//  "connection" from the host to the device.
//  https://docs.microsoft.com/en-us/windows-hardware/drivers/usbcon/usb-endpoints-and-their-pipes
struct [[gnu::packed]] USBEndpointDescriptor {
    USBDescriptorCommon descriptor_header;
    u8 endpoint_address;
    u8 endpoint_attributes_bitmap;
    u16 max_packet_size;
    u8 poll_interval_in_frames;
};

//
//  USB 1.1/2.0 Hub Descriptor
//  ==============
//
struct [[gnu::packed]] USBHubDescriptor {
    USBDescriptorCommon descriptor_header;
    u8 number_of_downstream_ports;
    u16 hub_characteristics;
    u8 power_on_to_power_good_time;
    u8 hub_controller_current;
    // NOTE: This does not contain DeviceRemovable or PortPwrCtrlMask because a struct cannot have two VLAs in a row.
};

//
//  USB Human Interface Device (HID) Descriptor
//  ==============
//
struct [[gnu::packed]] USBHIDDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 hid_bcd;
    u8 country_code;
    u8 number_of_report_descriptors;
    u8 following_descriptor_type;
    u16 hid_report_descriptor_size;
};

static constexpr u8 DESCRIPTOR_TYPE_DEVICE = 0x01;
static constexpr u8 DESCRIPTOR_TYPE_CONFIGURATION = 0x02;
static constexpr u8 DESCRIPTOR_TYPE_STRING = 0x03;
static constexpr u8 DESCRIPTOR_TYPE_INTERFACE = 0x04;
static constexpr u8 DESCRIPTOR_TYPE_ENDPOINT = 0x05;
static constexpr u8 DESCRIPTOR_TYPE_DEVICE_QUALIFIER = 0x06;
static constexpr u8 DESCRIPTOR_TYPE_HUB = 0x29;

}
