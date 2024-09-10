/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, blackcat <b14ckcat@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::USB {

class USBController;
class Transfer;
class Device;

using USBAsyncCallback = Function<void(Transfer* transfer)>;

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

    Device const& device() const { return m_device; }
    Device& device() { return m_device; }

    Type type() const { return m_type; }
    Direction direction() const { return m_direction; }

    u8 endpoint_address() const { return (direction() == Direction::In ? 0x80 : 0) | m_endpoint_number; }
    u8 endpoint_number() const { return m_endpoint_number; }
    u16 max_packet_size() const { return m_max_packet_size; }
    bool data_toggle() const { return m_data_toggle; }

    void set_max_packet_size(u16 max_size) { m_max_packet_size = max_size; }
    void set_toggle(bool toggle) { m_data_toggle = toggle; }

    ErrorOr<void> clear_halt();

protected:
    friend class Device;

    Pipe(USBController const& controller, Device& device, Type type, Direction direction, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer);

    NonnullLockRefPtr<USBController> m_controller;
    Device& m_device;

    Type m_type;
    Direction m_direction;

    u8 m_endpoint_number { 0 };   // Corresponding endpoint number for this pipe
    u16 m_max_packet_size { 0 };  // Max packet size for this pipe
    bool m_data_toggle { false }; // Data toggle for stuffing bit

    Mutex m_dma_buffer_lock { "USB pipe mutex"sv };

    NonnullOwnPtr<Memory::Region> m_dma_buffer;
};

class ControlPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<ControlPipe>> create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size = PAGE_SIZE);

    ErrorOr<size_t> submit_control_transfer(u8 request_type, u8 request, u16 value, u16 index, size_t length, void* data);

private:
    ControlPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer);
};

class BulkInPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<BulkInPipe>> create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size = PAGE_SIZE);

    ErrorOr<size_t> submit_bulk_in_transfer(size_t length, void* data);
    ErrorOr<size_t> submit_bulk_in_transfer(size_t length, UserOrKernelBuffer data);

private:
    BulkInPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer);
};

class BulkOutPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<BulkOutPipe>> create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size = PAGE_SIZE);

    ErrorOr<size_t> submit_bulk_out_transfer(size_t length, void const* data);
    ErrorOr<size_t> submit_bulk_out_transfer(size_t length, UserOrKernelBuffer const data);

private:
    BulkOutPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer);
};

class InterruptInPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<InterruptInPipe>> create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, size_t buffer_size = PAGE_SIZE);

    ErrorOr<NonnullLockRefPtr<Transfer>> submit_interrupt_in_transfer(size_t length, u16 ms_interval, USBAsyncCallback callback);

    u16 poll_interval() const { return m_poll_interval; }

private:
    InterruptInPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_pool);

    u16 m_poll_interval;
};

class InterruptOutPipe : public Pipe {
public:
    static ErrorOr<NonnullOwnPtr<InterruptOutPipe>> create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, size_t buffer_size = PAGE_SIZE);

    u16 poll_interval() const { return m_poll_interval; }

private:
    InterruptOutPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_pool);

    u16 m_poll_interval;
};

class IsochronousInPipe : public Pipe {
    // TODO
public:
private:
};

class IsochronousOutPipe : public Pipe {
    // TODO
public:
private:
};

}
