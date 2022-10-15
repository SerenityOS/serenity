/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::USB {

class USBController;

//
// A pipe is the logical connection between a memory buffer on the PC (host) and
// an endpoint on the device. In this implementation, the data buffer the pipe connects
// to is the physical buffer created when a Transfer is allocated.
//
class Pipe {
public:
    enum class Type : u8 {
        Control = 0,
        Isochronous = 1,
        Bulk = 2,
        Interrupt = 3
    };

    enum class Direction : u8 {
        Out = 0,
        In = 1,
        Bidirectional = 2
    };

    enum class DeviceSpeed : u8 {
        LowSpeed,
        FullSpeed
    };

    static ErrorOr<NonnullOwnPtr<Pipe>> try_create_pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, size_t buffer_size = PAGE_SIZE, u8 poll_interval = 0);

    Type type() const { return m_type; }
    Direction direction() const { return m_direction; }
    DeviceSpeed device_speed() const { return m_speed; }

    i8 device_address() const { return m_device_address; }
    u8 endpoint_address() const { return m_endpoint_address; }
    u16 max_packet_size() const { return m_max_packet_size; }
    u8 poll_interval() const { return m_poll_interval; }
    bool data_toggle() const { return m_data_toggle; }

    void set_max_packet_size(u16 max_size) { m_max_packet_size = max_size; }
    void set_toggle(bool toggle) { m_data_toggle = toggle; }
    void set_device_address(i8 addr) { m_device_address = addr; }

    ErrorOr<size_t> control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data);
    ErrorOr<size_t> bulk_transfer(u16 length, void* data);

    Pipe(USBController const& controller, Type type, Direction direction, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer);
    Pipe(USBController const& controller, Type type, Direction direction, USBEndpointDescriptor& endpoint, NonnullOwnPtr<Memory::Region> dma_buffer);
    Pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, u8 poll_interval, i8 device_address, NonnullOwnPtr<Memory::Region> dma_buffer);

private:
    friend class Device;

    NonnullLockRefPtr<USBController> m_controller;

    Type m_type;
    Direction m_direction;
    DeviceSpeed m_speed;

    i8 m_device_address { 0 };    // Device address of this pipe
    u8 m_endpoint_address { 0 };  // Corresponding endpoint address for this pipe
    u16 m_max_packet_size { 0 };  // Max packet size for this pipe
    u8 m_poll_interval { 0 };     // Polling interval (in frames)
    bool m_data_toggle { false }; // Data toggle for stuffing bit

    Mutex m_dma_buffer_lock { "USB pipe mutex"sv };

    NonnullOwnPtr<Memory::Region> m_dma_buffer;
};
}
