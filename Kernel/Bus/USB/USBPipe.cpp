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

Pipe::Pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool)
    : m_controller(controller)
    , m_type(type)
    , m_direction(direction)
    , m_device_address(device_address)
    , m_endpoint_address(endpoint_address)
    , m_max_packet_size(max_packet_size)
    , m_data_toggle(false)
    , m_dma_pool(move(dma_pool))
{
}

ErrorOr<NonnullOwnPtr<ControlPipe>> ControlPipe::try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address)
{
    auto dma_pool = TRY(USBDMAPool<USBDMAHandle>::try_create("Buffer pool"sv, max_packet_size, 64));
    return adopt_nonnull_own_or_enomem(new (nothrow) ControlPipe(controller, direction, endpoint_address, max_packet_size, device_address, move(dma_pool)));
}

ControlPipe::ControlPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool)
    : Pipe(controller, Pipe::Type::Control, direction, endpoint_address, max_packet_size, device_address, move(dma_pool))
{
}

ErrorOr<size_t> ControlPipe::control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data)
{
    USBRequestData usb_request;

    usb_request.request_type = request_type;
    usb_request.request = request;
    usb_request.value = value;
    usb_request.index = index;
    usb_request.length = length;

    auto transfer = TRY(Transfer::try_create(*this, length, *m_dma_pool.ptr()));
    transfer->set_setup_packet(usb_request);

    dbgln_if(USB_DEBUG, "Pipe: Transfer allocated @ {}", transfer->buffer_physical());
    auto transfer_length = TRY(m_controller->submit_control_transfer(*transfer));

    // TODO: Check transfer for completion and copy data from transfer buffer into data
    if (length > 0)
        memcpy(reinterpret_cast<u8*>(data), transfer->buffer().as_ptr() + sizeof(USBRequestData), length);

    dbgln_if(USB_DEBUG, "Pipe: Control Transfer complete!");
    return transfer_length;
}

ErrorOr<NonnullOwnPtr<BulkPipe>> BulkPipe::try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address)
{
    auto dma_pool = TRY(USBDMAPool<USBDMAHandle>::try_create("Buffer pool"sv, max_packet_size, 64));
    return adopt_nonnull_own_or_enomem(new (nothrow) BulkPipe(controller, direction, endpoint_address, max_packet_size, device_address, move(dma_pool)));
}

BulkPipe::BulkPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool)
    : Pipe(controller, Pipe::Type::Bulk, direction, endpoint_address, max_packet_size, device_address, move(dma_pool))
{
}

ErrorOr<size_t> BulkPipe::bulk_transfer(u16 length, void* data)
{
    size_t transfer_length = 0;
    auto transfer = TRY(Transfer::try_create(*this, length, *m_dma_pool.ptr()));

    if (m_direction == Direction::In) {
        dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer allocated @ {}", transfer->buffer_physical());
        transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
        memcpy(data, transfer->buffer().as_ptr(), min(length, transfer_length));
        dbgln_if(USB_DEBUG, "Pipe: Bulk in transfer complete!");
    } else if (m_direction == Direction::Out) {
        TRY(transfer->write_buffer(length, data));
        dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer allocated @ {}", transfer->buffer_physical());
        transfer_length = TRY(m_controller->submit_bulk_transfer(*transfer));
        dbgln_if(USB_DEBUG, "Pipe: Bulk out transfer complete!");
    }

    return transfer_length;
}

ErrorOr<NonnullOwnPtr<InterruptPipe>> InterruptPipe::try_create_pipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, u8 poll_interval)
{
    auto dma_pool = TRY(USBDMAPool<USBDMAHandle>::try_create("Buffer pool"sv, max_packet_size, 64));
    return adopt_nonnull_own_or_enomem(new (nothrow) InterruptPipe(controller, direction, endpoint_address, max_packet_size, device_address, poll_interval, move(dma_pool)));
}

InterruptPipe::InterruptPipe(USBController const& controller, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, u8 poll_interval, NonnullOwnPtr<USBDMAPool<USBDMAHandle>> dma_pool)
    : Pipe(controller, Pipe::Type::Interrupt, direction, endpoint_address, max_packet_size, device_address, move(dma_pool))
    , m_poll_interval(poll_interval)
{
}

}
