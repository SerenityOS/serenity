/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, blackcat <b14ckcat@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/USBDMAPool.h>
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

    Type type() const { return m_type; }
    Direction direction() const { return m_direction; }
    DeviceSpeed device_speed() const { return m_speed; }

    i8 device_address() const { return m_device_address; }
    u8 endpoint_address() const { return m_endpoint_address; }
    u16 max_packet_size() const { return m_max_packet_size; }
    bool data_toggle() const { return m_data_toggle; }

    void set_max_packet_size(u16 max_size) { m_max_packet_size = max_size; }
    void set_toggle(bool toggle) { m_data_toggle = toggle; }
    void set_device_address(i8 addr) { m_device_address = addr; }

protected:
    friend class Device;

    Pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool);

    NonnullLockRefPtr<USBController> m_controller;

    Type m_type;
    Direction m_direction;
    DeviceSpeed m_speed;

    i8 m_device_address { 0 };    // Device address of this pipe
    u8 m_endpoint_address { 0 };  // Corresponding endpoint address for this pipe
    u16 m_max_packet_size { 0 };  // Max packet size for this pipe
    u8 m_poll_interval { 0 };     // Polling interval (in frames)
    bool m_data_toggle { false }; // Data toggle for stuffing bit
                                  //
    OwnPtr<USBDMAPool<USBDMAHandle>> m_dma_pool;
};

class ControlPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<ControlPipe>> try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address);

    ErrorOr<size_t> control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data);

private:
    ControlPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool);
};

class BulkPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<BulkPipe>> try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address);

    ErrorOr<size_t> bulk_transfer(u16 length, void* data);

private:
    BulkPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool);
};

class InterruptPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<InterruptPipe>> try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, u8 poll_interval);

    u8 poll_interval() const { return m_poll_interval; }
    ErrorOr<size_t> interrupt_transfer(u16 length, void* data);

private:
    InterruptPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, u8 poll_interval, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool);

    u8 m_poll_interval;
};

class IsochronousPipe : public Pipe {
    // TODO
public:
private:
};

}
