/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/USB/PacketTypes.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

ErrorOr<NonnullOwnPtr<Pipe>> Pipe::try_create_pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, i8 device_address, u8 poll_interval)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Pipe(controller, type, direction, endpoint_address, max_packet_size, poll_interval, device_address));
}

Pipe::Pipe(USBController const& controller, Type type, Pipe::Direction direction, u16 max_packet_size)
    : m_controller(controller)
    , m_type(type)
    , m_direction(direction)
    , m_endpoint_address(0)
    , m_max_packet_size(max_packet_size)
    , m_poll_interval(0)
    , m_data_toggle(false)
{
}

Pipe::Pipe(USBController const& controller, Type type, Direction direction, USBEndpointDescriptor& endpoint [[maybe_unused]])
    : m_controller(controller)
    , m_type(type)
    , m_direction(direction)
{
    // TODO: decode endpoint structure
}

Pipe::Pipe(USBController const& controller, Type type, Direction direction, u8 endpoint_address, u16 max_packet_size, u8 poll_interval, i8 device_address)
    : m_controller(controller)
    , m_type(type)
    , m_direction(direction)
    , m_device_address(device_address)
    , m_endpoint_address(endpoint_address)
    , m_max_packet_size(max_packet_size)
    , m_poll_interval(poll_interval)
    , m_data_toggle(false)
{
}

ErrorOr<size_t> Pipe::control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data)
{
    USBRequestData usb_request;

    usb_request.request_type = request_type;
    usb_request.request = request;
    usb_request.value = value;
    usb_request.index = index;
    usb_request.length = length;

    auto transfer = TRY(Transfer::try_create(*this, length));
    transfer->set_setup_packet(usb_request);

    dbgln_if(USB_DEBUG, "Pipe: Transfer allocated @ {}", transfer->buffer_physical());
    auto transfer_length = TRY(m_controller->submit_control_transfer(*transfer));

    // TODO: Check transfer for completion and copy data from transfer buffer into data
    if (length > 0)
        memcpy(reinterpret_cast<u8*>(data), transfer->buffer().as_ptr() + sizeof(USBRequestData), length);

    dbgln_if(USB_DEBUG, "Pipe: Control Transfer complete!");
    return transfer_length;
}

ErrorOr<size_t> Pipe::bulk_transfer(u16 length, void* data)
{
    size_t transfer_length = 0;
    auto transfer = TRY(Transfer::try_create(*this, length));

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

}
