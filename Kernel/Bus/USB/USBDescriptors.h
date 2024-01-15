/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

// https://www.usb.org/sites/default/files/usb_20_20230224.zip
// -> usb_20.pdf Chapter 9.6
// https://www.usb.org/sites/default/files/usb_32_202206_0.zip
// -> USB 3.2 Revision 1.1.pdf Chapter 9.6 Standard USB Descriptor Definitions

namespace Kernel::USB {
// USB 2.0: Table 9-5.  Descriptor Types
// USB 3.2: Table 9-6.  Descriptor Types
enum class DescriptorType : u8 {
    Device = 1,
    Configuration = 2,
    String = 3,
    Interface = 4,
    Endpoint = 5,
    DeviceQualifier = 6,         // Reserved in USB 3.2
    OtherSpeedConfiguration = 7, // Reserved in USB 3.2
    InterfacePower = 8,
    OTG = 9,
    Debug = 10,
    InterfaceAssociation = 11,

    BOS = 15,
    DeviceCapability = 16,

    SuperSpeedUSBEndpointCompanion = 48,
    SuperSpeedPlusIsochronousEndpointCompanion = 49,
    // USB 2.0: 11.23.2.1  Hub Descriptor -> 29h
    Hub = 0x29,
    // USB 3.2: 10.15.2.1 Hub Descriptor
    EnhancedSuperSpeedHub = 0x2A
};

// USB 3.2
// Table 9-14.  Device Capability Type Codes
enum class DeviceCapabilityType : u8 {
    // 0x00 Reserved
    WirelessUSB = 0x01,
    USB20Extension = 0x02,
    SuperSpeedUSB = 0x03,
    ContainerID = 0x04,
    Platform = 0x05,
    PowerDeliveryCapability = 0x06,
    BatteryInfoCapability = 0x07,
    PDConsumerPortCapability = 0x08,
    PDProviderPortCapability = 0x09,
    SuperSpeedPlus = 0x0A,
    PrecisionTimeMeasurement = 0x0B,
    WirelessUSBExt = 0x0C,
    Billboard = 0x0D,
    Authentication = 0x0E,
    BillboardEx = 0x0F,
    ConfigurationSummary = 0x10,
    FWStatusCapability = 0x11
    // 0x12-0xFF Reserved

};

struct [[gnu::packed]] USBDescriptorCommon {
    u8 length;                      // bLength
    DescriptorType descriptor_type; // bDescriptorType
};

// USB 3.2: 9.6.1 Device
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

// USB 3.2: 9.6.2 Device_Qualifier
//  The device_qualifier descriptor describes information about a high-speed capable device
//  that would change if the device were operating at the other speed
struct USBDeviceQualifierDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 usb_spec_compliance_bcd;
    u8 device_class;
    u8 device_sub_class;
    u8 device_protocol;
    u8 max_packet_size;
    u8 num_configurations;
    u8 reserved;
};
static_assert(sizeof(USBDeviceQualifierDescriptor) == 10);

// USB 3.2: 9.6.2 Binary Device Object Store (BOS)
struct USBBinaryObjectStoreDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 total_length;
    u8 num_device_caps;
    u8 capabilities[];
};
static_assert(sizeof(USBBinaryObjectStoreDescriptor) == 6);

struct USBDeviceCapabilityDescriptorBase : USBDescriptorCommon {
    DeviceCapabilityType capability_type; // bDevCapabilityType
    // Data....
};

// USB 3.2: 9.6.2.1  USB 2.0 Extension
struct USB20ExtensionDescriptor : USBDeviceCapabilityDescriptorBase {
    u8 attributes_bitmap;             // bmAttributes
    u8 attributes_bitmap_reserved[3]; //
};
static_assert(AssertSize<USB20ExtensionDescriptor, 7>());

// USB 3.2: 9.6.2.2  SuperSpeed USB  Device Capability
struct SuperSpeedUSBDeviceCapability : USBDeviceCapabilityDescriptorBase {
    u8 attributes_bitmap; // bmAttributes
    union {
        struct {
            u16 low_speed : 1;
            u16 full_speed : 1;
            u16 high_speed : 1;
            u16 gen1_speed : 1; // WTF USB
            u16 : 12;           // Reserved - Shall be 0
        };
        u16 raw;                // This might be needed as bFunctionalitySupport links back to this
    } speeds_supported;         // wSpeedsSupported
    u8 functionality_support;   // bFunctionalitySupport
    u8 U1_device_exit_latency;  // bU1DevExitLat
    u16 U2_device_exit_latency; // bU2DevExitLat
};
static_assert(AssertSize<SuperSpeedUSBDeviceCapability, 10>());

// USB 3.2: 9.6.2.3  Container ID
struct ContainerIDDescriptor : USBDeviceCapabilityDescriptorBase {
    u8 reserved; // bReserved
    u8 UUID[16]; // ContainerID
};
static_assert(AssertSize<ContainerIDDescriptor, 20>());

// FIXME: USB 3.2: 9.6.2.4  Platform Descriptor
// USB 3.2: 9.6.2.5  SuperSpeedPlus USB Device Capability
struct SuperSpeedPlusUSBDeviceCapability : USBDeviceCapabilityDescriptorBase {
    u8 reserved0; // bReserved
    struct {
        u32 super_speed_attribute_count : 5; // SSAC
        u32 sublink_speed_id_count : 4;      // SSIC
        u32 : 23;                            // Reserved
    } attributes;                            // bmAttributes
    struct {
        u16 sublink_speed_attribute_id : 4; // SSID
        u16 : 4;                            // Reserved
        u16 min_rx_lane_count : 4;
        u16 min_tx_lane_count : 4;
    } functionality_support; // wFunctionalitySupport
    u16 reserved1;           // wReserved
    struct SublinkSpeedAttribute {
        u32 sublink_speed_attribute_id : 4; // SSID
        enum class LaneSpeedExponent {
            bs = 0,
            Kbs = 1,
            Mbs = 2,
            Gbs = 3
        } lane_speed_exponent : 2; // LSE
        u32 sublink_type : 2;      // ST
        u32 : 6;                   // Reserved
        enum class LinkProtocol {
            SuperSpeed = 0,
            SuperSpeedPlus = 1,
            // 2-3 Reserved
        } link_protocol : 2;          // LP
        u32 lane_speed_mantissa : 16; // LSM
    } sublink_speed_attribute[];      // bmSublinkSpeedAttr[]
};
static_assert(__builtin_offsetof(SuperSpeedPlusUSBDeviceCapability, sublink_speed_attribute) == 12);
static_assert(AssertSize<SuperSpeedPlusUSBDeviceCapability::SublinkSpeedAttribute, 4>());

// USB 3.2: 9.6.3 Configuration
//  Configuration Descriptor
//  ========================
//
//  A USB device can have multiple configurations, which tells us about how the
//  device is physically configured (e.g how it's powered, max power consumption etc).
//
struct [[gnu::packed]] USBConfigurationDescriptor {
    USBDescriptorCommon descriptor_header;
    u16 total_length;                         // wTotalLength
    u8 number_of_interfaces;                  // bNumInterfaces
    u8 configuration_value;                   // bConfigurationValue
    u8 configuration_string_descriptor_index; // iConfiguration
    u8 attributes_bitmap;                     // bmAttributes
    u8 max_power_in_ma;                       // bMaxPower
};

// USB 2.0: 9.6.4 Other_Speed_Configuration
// The other_speed_configuration descriptor shown in Table 9-11 describes a configuration
// of a high-speed capable device if it were operating at its other possible speed
struct USBOtherSpeedConfigurationDescriptor : USBDescriptorCommon {
    u16 total_length;                         // wTotalLength
    u8 number_of_interfaces;                  // bNumInterfaces
    u8 configuration_value;                   // bConfigurationValue
    u8 configuration_string_descriptor_index; // iConfiguration
    u8 attributes_bitmap;                     // bmAttributes
    u8 max_power_in_ma;                       // bMaxPower
};

// USB 2.0: 9.6.5 Interface
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
    u8 interface_id;                      // bInterfaceNumber
    u8 alternate_setting;                 // bAlternateSetting
    u8 number_of_endpoints;               // bNumEndpoints
    u8 interface_class_code;              // bInterfaceClass
    u8 interface_sub_class_code;          // bInterfaceSubClass
    u8 interface_protocol;                // bInterfaceProtocol
    u8 interface_string_descriptor_index; // iInterface
};

// USB 2.0: 9.6.6 Endpoint
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
    u8 endpoint_address;           // bEndpointAddress
    u8 endpoint_attributes_bitmap; // bmAttributes
    u16 max_packet_size;           // wMaxPacketSize
    u8 poll_interval_in_frames;    // bInterval
};

//
//  USB 1.1/2.0 Hub Descriptor
//  ==============
//
struct [[gnu::packed]] USBHubDescriptor {
    USBDescriptorCommon descriptor_header;
    u8 number_of_downstream_ports;  // bNbrPorts
    u16 hub_characteristics;        // wHubCharacteristics
    u8 power_on_to_power_good_time; // bPwrOn2PwrGood
    u8 hub_controller_current;      // bHubContrCurrent
    // NOTE: This does not contain DeviceRemovable or PortPwrCtrlMask because a struct cannot have two VLAs in a row.
};

// USB 3.2: 10.15.2.1 Hub Descriptor
struct [[gnu::packed]] EnhancedSuperSpeedHubDescriptor : USBDescriptorCommon {
    u8 n_ports;                          // bNbrPorts
    u16 hub_characteristics;             // wHubCharacteristics
    u8 power_on_to_power_good_time;      // bPwrOn2PwrGood
    u8 hub_controller_current;           // bHubContrCurrent
    u8 hub_packet_header_decode_latency; // bHubHdrDecLat
    u16 hub_delay;                       // wHubDelay
    u16 device_removable;                // DeviceRemovable -> 1 indexed bit-field
};
static_assert(AssertSize<EnhancedSuperSpeedHubDescriptor, 12>());

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

}
