/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBPipe.h>

namespace Kernel::USB {

//
// An endpoint is the "end point" of communication of a USB device. That is, data is read from and written
// to an endpoint via a USB pipe. As an example, during device enumeration (where we assign an address to the
// device), we communicate with the device over the default endpoint, endpoint0, which all devices _must_
// contain to be compliant with the USB specification.
//
// And endpoint describes characteristics about the transfer between the host and the device, such as:
//  - The endpoint number
//  - Max packet size of send/recv of the endpoint
//  - Transfer type (bulk, interrupt, isochronous etc)
//
// Take for example a USB multifunction device, such as a keyboard/mouse combination. The mouse
// may need to be polled every n milliseconds, meaning the transfer may be isochronous (streamed),
// while the keyboard part would only generate data once we push a key (hence an interrupt transfer).
// Each of these data sources would be a _different_ endpoint on the device that we read from.
class USBEndpoint {
public:
    static constexpr u8 ENDPOINT_ADDRESS_NUMBER_MASK = 0x0f;
    static constexpr u8 ENDPOINT_ADDRESS_DIRECTION_MASK = 0x80;
    static constexpr u8 ENDPOINT_ADDRESS_DIRECTION_OUT = 0x00;
    static constexpr u8 ENDPOINT_ADDRESS_DIRECTION_IN = 0x80;

    static constexpr u8 ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK = 0x03;
    static constexpr u8 ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_CONTROL = 0x00;
    static constexpr u8 ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_ISOCHRONOUS = 0x01;
    static constexpr u8 ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_BULK = 0x02;
    static constexpr u8 ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_INTERRUPT = 0x03;

    static constexpr u8 ENDPOINT_ATTRIBUTES_ISO_MODE_SYNC_TYPE = 0x0c;
    static constexpr u8 ENDPOINT_ATTRIBUTES_ISO_MODE_USAGE_TYPE = 0x30;

    USBEndpointDescriptor const& descriptor() const { return m_descriptor; }

    bool is_control() const { return (m_descriptor.endpoint_attributes_bitmap & ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) == ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_CONTROL; }
    bool is_isochronous() const { return (m_descriptor.endpoint_attributes_bitmap & ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) == ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_ISOCHRONOUS; }
    bool is_bulk() const { return (m_descriptor.endpoint_attributes_bitmap & ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) == ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_BULK; }
    bool is_interrupt() const { return (m_descriptor.endpoint_attributes_bitmap & ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) == ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_INTERRUPT; }

    u16 max_packet_size() const { return m_descriptor.max_packet_size; }
    u8 polling_interval() const { return m_descriptor.poll_interval_in_frames; }

private:
    USBEndpointDescriptor m_descriptor;

    Pipe m_pipe;
};

}
