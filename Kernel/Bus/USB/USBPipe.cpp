/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, blackcat <b14ckcat@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/USB/PacketTypes.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

Pipe::Pipe(USBController const& controller, Device& device, Type type, Direction direction, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer)
    : m_controller(controller)
    , m_device(device)
    , m_type(type)
    , m_direction(direction)
    , m_endpoint_number(endpoint_number)
    , m_max_packet_size(max_packet_size)
    , m_data_toggle(false)
    , m_dma_buffer(move(dma_buffer))
{
    // Valid endpoint numbers are between 0x0 and 0xf, inclusive.
    VERIFY(endpoint_number <= 0xf);
}

ErrorOr<void> Pipe::clear_halt()
{
    return m_controller->reset_pipe(m_device, *this);
}

ErrorOr<NonnullOwnPtr<ControlPipe>> ControlPipe::create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size)
{
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB device DMA buffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    return adopt_nonnull_own_or_enomem(new (nothrow) ControlPipe(controller, device, endpoint_number, max_packet_size, move(dma_buffer)));
}

ControlPipe::ControlPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, device, Type::Control, Direction::Bidirectional, endpoint_number, max_packet_size, move(dma_buffer))
{
}

ErrorOr<size_t> ControlPipe::submit_control_transfer(u8 request_type, u8 request, u16 value, u16 index, size_t length, void* data)
{
    VERIFY(length <= m_dma_buffer->size());

    MutexLocker lock(m_dma_buffer_lock);

    USBRequestData usb_request;

    usb_request.request_type = request_type;
    usb_request.request = request;
    usb_request.value = value;
    usb_request.index = index;
    usb_request.length = length;

    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer));
    transfer->set_setup_packet(usb_request);

    dbgln_if(USB_DEBUG, "ControlPipe: Transfer allocated @ {}", transfer->buffer_physical());
    auto transfer_length = TRY(m_controller->submit_control_transfer(*transfer));

    // TODO: Check transfer for completion and copy data from transfer buffer into data
    if (length > 0)
        memcpy(reinterpret_cast<u8*>(data), transfer->buffer().as_ptr() + sizeof(USBRequestData), length);

    dbgln_if(USB_DEBUG, "Pipe: Control Transfer complete!");
    return transfer_length;
}

ErrorOr<NonnullOwnPtr<BulkInPipe>> BulkInPipe::create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    return adopt_nonnull_own_or_enomem(new (nothrow) BulkInPipe(controller, device, endpoint_number, max_packet_size, move(dma_buffer)));
}

BulkInPipe::BulkInPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, device, Pipe::Type::Bulk, Direction::In, endpoint_number, max_packet_size, move(dma_buffer))
{
}

ErrorOr<size_t> BulkInPipe::submit_bulk_in_transfer(size_t length, void* data)
{
    VERIFY(length <= m_dma_buffer->size());

    MutexLocker lock(m_dma_buffer_lock);

    size_t transfer_length = 0;

    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer));

    dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer allocated @ {}", transfer->buffer_physical());
    transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
    memcpy(data, transfer->buffer().as_ptr(), min(length, transfer_length));
    dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer complete!");

    return transfer_length;
}

ErrorOr<size_t> BulkInPipe::submit_bulk_in_transfer(size_t length, UserOrKernelBuffer data)
{
    VERIFY(length <= m_dma_buffer->size());

    MutexLocker lock(m_dma_buffer_lock);

    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer));

    dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer allocated @ {}", transfer->buffer_physical());
    size_t transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
    TRY(data.write(transfer->buffer().as_ptr(), min(length, transfer_length)));
    dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer complete!");

    return transfer_length;
}

ErrorOr<NonnullOwnPtr<BulkOutPipe>> BulkOutPipe::create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    return adopt_nonnull_own_or_enomem(new (nothrow) BulkOutPipe(controller, device, endpoint_number, max_packet_size, move(dma_buffer)));
}

BulkOutPipe::BulkOutPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, device, Type::Bulk, Direction::Out, endpoint_number, max_packet_size, move(dma_buffer))

{
}

ErrorOr<size_t> BulkOutPipe::submit_bulk_out_transfer(size_t length, void const* data)
{
    VERIFY(length <= m_dma_buffer->size());

    MutexLocker lock(m_dma_buffer_lock);

    size_t transfer_length = 0;
    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer));

    TRY(transfer->write_buffer(length, data));
    dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer allocated @ {}", transfer->buffer_physical());
    transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
    dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer complete!");

    return transfer_length;
}

ErrorOr<size_t> BulkOutPipe::submit_bulk_out_transfer(size_t length, UserOrKernelBuffer const data)
{
    VERIFY(length <= m_dma_buffer->size());

    MutexLocker lock(m_dma_buffer_lock);

    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer));

    TRY(transfer->write_buffer(length, data));
    dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer allocated @ {}", transfer->buffer_physical());
    size_t transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
    dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer complete!");

    return transfer_length;
}

ErrorOr<NonnullOwnPtr<InterruptInPipe>> InterruptInPipe::create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    return adopt_nonnull_own_or_enomem(new (nothrow) InterruptInPipe(controller, device, endpoint_number, max_packet_size, poll_interval, move(dma_buffer)));
}

InterruptInPipe::InterruptInPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, device, Type::Interrupt, Direction::In, endpoint_number, max_packet_size, move(dma_buffer))
    , m_poll_interval(poll_interval)
{
}

ErrorOr<NonnullLockRefPtr<Transfer>> InterruptInPipe::submit_interrupt_in_transfer(size_t length, u16 ms_interval, USBAsyncCallback callback)
{
    VERIFY(length <= m_dma_buffer->size());

    auto transfer = TRY(Transfer::create(*this, length, *m_dma_buffer, move(callback)));
    dbgln_if(USB_DEBUG, "Pipe: Interrupt in transfer allocated @ {}", transfer->buffer_physical());
    TRY(m_controller->submit_async_interrupt_transfer(transfer, ms_interval));
    return transfer;
}

ErrorOr<NonnullOwnPtr<InterruptOutPipe>> InterruptOutPipe::create(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    return adopt_nonnull_own_or_enomem(new (nothrow) InterruptOutPipe(controller, device, endpoint_number, max_packet_size, poll_interval, move(dma_buffer)));
}

InterruptOutPipe::InterruptOutPipe(USBController const& controller, Device& device, u8 endpoint_number, u16 max_packet_size, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, device, Type::Interrupt, Direction::In, endpoint_number, max_packet_size, move(dma_buffer))
    , m_poll_interval(poll_interval)
{
}

}
