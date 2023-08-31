/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, blackcat <b14ckcat@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/USB/PacketTypes.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

Pipe::Pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<Memory::Region> dma_buffer)
    : m_controller(controller)
    , m_type(type)
    , m_direction(direction)
    , m_device_address(device_address)
    , m_endpoint_address(endpoint_address)
    , m_max_packet_size(max_packet_size)
    , m_data_toggle(false)
    , m_dma_buffer(move(dma_buffer))
{
}

ErrorOr<NonnullOwnPtr<ControlPipe>> ControlPipe::create(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, size_t buffer_size)
{
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB device DMA buffer"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) ControlPipe(controller, endpoint_address, max_packet_size, device_address, move(dma_buffer)));
}

ControlPipe::ControlPipe(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, Type::Control, Direction::Bidirectional, endpoint_address, max_packet_size, device_address, move(dma_buffer))
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

ErrorOr<NonnullOwnPtr<BulkInPipe>> BulkInPipe::create(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) BulkInPipe(controller, endpoint_address, max_packet_size, device_address, move(dma_buffer)));
}

BulkInPipe::BulkInPipe(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, Pipe::Type::Bulk, Direction::In, endpoint_address, max_packet_size, device_address, move(dma_buffer))
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

ErrorOr<NonnullOwnPtr<BulkOutPipe>> BulkOutPipe::create(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) BulkOutPipe(controller, endpoint_address, max_packet_size, device_address, move(dma_buffer)));
}

BulkOutPipe::BulkOutPipe(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, Type::Bulk, Direction::Out, endpoint_address, max_packet_size, device_address, move(dma_buffer))

{
}

ErrorOr<size_t> BulkOutPipe::submit_bulk_out_transfer(size_t length, void* data)
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

ErrorOr<size_t> BulkOutPipe::submit_bulk_out_transfer(size_t length, UserOrKernelBuffer data)
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

ErrorOr<NonnullOwnPtr<InterruptInPipe>> InterruptInPipe::create(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, u16 poll_interval, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) InterruptInPipe(controller, endpoint_address, max_packet_size, device_address, poll_interval, move(dma_buffer)));
}

InterruptInPipe::InterruptInPipe(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, Type::Interrupt, Direction::In, endpoint_address, max_packet_size, device_address, move(dma_buffer))
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

ErrorOr<NonnullOwnPtr<InterruptOutPipe>> InterruptOutPipe::create(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, u16 poll_interval, size_t buffer_size)
{
    VERIFY(buffer_size >= max_packet_size);
    auto dma_buffer = TRY(MM.allocate_dma_buffer_pages(TRY(Memory::page_round_up(buffer_size)), "USB pipe DMA buffer"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) InterruptOutPipe(controller, endpoint_address, max_packet_size, device_address, poll_interval, move(dma_buffer)));
}

InterruptOutPipe::InterruptOutPipe(USBController const& controller, u8 endpoint_address, u16 max_packet_size, i8 device_address, u16 poll_interval, NonnullOwnPtr<Memory::Region> dma_buffer)
    : Pipe(controller, Type::Interrupt, Direction::In, endpoint_address, max_packet_size, device_address, move(dma_buffer))
    , m_poll_interval(poll_interval)
{
}

}
