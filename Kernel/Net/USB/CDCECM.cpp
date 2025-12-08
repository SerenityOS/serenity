/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/USB/CDCECM.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::USB::CDC {

ErrorOr<NonnullRefPtr<NetworkAdapter>> CDCECMNetworkAdapter::create(
    USB::Device& device, USB::USBInterface const& control, USB::USBInterface const& data)
{

    u16 max_segment_size;
    u8 mac_string_index;
    TRY(control.configuration().for_each_descriptor_in_interface(control, [&](ReadonlyBytes raw_descriptor) -> ErrorOr<IterationDecision> {
        auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(raw_descriptor.data());
        if (descriptor_header.descriptor_type != static_cast<u8>(CDC::ClassSpecificDescriptorCodes::CS_Interface))
            return IterationDecision::Continue;

        auto subtype = raw_descriptor[2];
        if (static_cast<CDC::ClassSpecificInterfaceDescriptorCodes>(subtype) != CDC::ClassSpecificInterfaceDescriptorCodes::EthernetNetworking)
            return IterationDecision::Continue;

        auto stream = FixedMemoryStream(raw_descriptor.slice(sizeof(USBDescriptorCommon) + 1));
        mac_string_index = TRY(stream.read_value<u8>());
        [[maybe_unused]] u32 bm_stats = TRY(stream.read_value<LittleEndian<u32>>());
        max_segment_size = TRY(stream.read_value<LittleEndian<u16>>());
        [[maybe_unused]] u16 w_number_mc_filters = TRY(stream.read_value<LittleEndian<u16>>());
        [[maybe_unused]] u8 b_number_power_filters = TRY(stream.read_value<u8>());
        return IterationDecision::Break;
    }));

    auto mac_string = TRY(device.get_string_descriptor(mac_string_index));
    if (mac_string->length() != 12) {
        dbgln("USB CDC-ECM: Invalid MAC address string length: {}", mac_string->length());
        return EINVAL;
    }
    MACAddress mac_address;
    for (size_t i = 0; i < 6; ++i) {
        auto byte_str = mac_string->view().substring_view(i * 2, 2);
        auto byte_value = AK::StringUtils::convert_to_uint_from_hex<u8>(byte_str);
        if (!byte_value.has_value()) {
            dbgln("USB CDC-ECM: Invalid MAC address string: {}", mac_string->view());
            return EINVAL;
        }
        mac_address[i] = byte_value.value();
    }
    dmesgln("USB CDC-ECM: Using MAC address: {}", TRY(mac_address.to_string()));

    TRY(device.set_configuration_and_interface(data));

    u8 in_pipe_endpoint_number = 0xff;
    u16 in_max_packet_size;
    u8 out_pipe_endpoint_number = 0xff;
    u16 out_max_packet_size;

    if (data.descriptor().number_of_endpoints < 2) {
        dmesgln("CDC-ECM: Data Interface does not provide enough endpoints; Rejecting");
        return ENOTSUP;
    }

    for (auto const& endpoint : data.endpoints()) {
        if (endpoint.endpoint_attributes_bitmap != USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_BULK)
            continue;
        // The upper bit of the Endpoint address is set to 1, iff it is the Bulk-In Endpoint
        if (endpoint.endpoint_address & 0x80) {
            in_pipe_endpoint_number = endpoint.endpoint_address & 0b1111;
            in_max_packet_size = endpoint.max_packet_size;
        } else {
            out_pipe_endpoint_number = endpoint.endpoint_address & 0b1111;
            out_max_packet_size = endpoint.max_packet_size;
        }
    }

    if (in_pipe_endpoint_number == 0xff || out_pipe_endpoint_number == 0xff) {
        // FIXME: We may also get isochronous endpoints, handle those too
        dmesgln("CDC-ECM: Data Interface did not advertise two Bulk Endpoints; Rejecting");
        return ENOTSUP;
    }

    auto in_pipe = TRY(BulkInPipe::create(device.controller(), device, in_pipe_endpoint_number, in_max_packet_size));
    auto out_pipe = TRY(BulkOutPipe::create(device.controller(), device, out_pipe_endpoint_number, out_max_packet_size));

    // FIXME: Maybe also set-up the notification interrupt pipe from the control interface

    return adopt_nonnull_ref_or_enomem(new (nothrow) CDCECMNetworkAdapter(
        device,
        mac_address,
        move(in_pipe),
        move(out_pipe),
        max_segment_size));
}

CDCECMNetworkAdapter::CDCECMNetworkAdapter(USB::Device& device,
    MACAddress const& mac_address,
    NonnullOwnPtr<BulkInPipe> in_pipe,
    NonnullOwnPtr<BulkOutPipe> out_pipe,
    u16 max_segment_size)
    : NetworkAdapter("cdc-ecm"sv) // FIXME: Choose the propper name
                                  // FIXME: We may want to make this unique if we have multiple CDC-ECM devices
    , m_device(device)
    , m_in_pipe(move(in_pipe))
    , m_out_pipe(move(out_pipe))
{
    set_mtu(max_segment_size);
    set_mac_address(mac_address);
}

CDCECMNetworkAdapter::~CDCECMNetworkAdapter() = default;

ErrorOr<void> CDCECMNetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    // FIXME: Listen for Notifications
    auto [process, poll_thread] = TRY(Process::create_kernel_process("CDC-ECM"sv, [this]() { this->poll_thread(); }));
    poll_thread->set_name("CDC-ECM Poll"sv);
    m_process = move(process);
    return {};
}

bool CDCECMNetworkAdapter::link_up()
{
    // FIXME: Listen for a NetworkConnection Notification?
    //  Note: We would likely need to listen to that before activating the alternative interface
    //        As that seems to be the step that initializes the ethernet controller
    return true;
}

i32 CDCECMNetworkAdapter::link_speed()
{
    // FIXME: Listen for a ConnectionSpeedChange Notification
    //  Note: See above
    return 1000; // Let's assume gigabit for now
}

void CDCECMNetworkAdapter::send_raw(ReadonlyBytes packet)
{
    // FIXME: Handle errors properly
    dmesgln("CDC-ECM: SENDING");
    //  Note: CDC-ECM requires that a frame is ended by a short packet,
    //        Splitting of the packet is handled by the controller or host-driver.
    //        So we only need to optionally insert a zero-length packet at the end
    //        in case the sent packet is an exact multiple of the max transfer size.
    size_t const max_transfer_size = m_out_pipe->max_packet_size();
    m_out_pipe->submit_bulk_out_transfer(packet.size(), packet.data()).release_value_but_fixme_should_propagate_errors();
    if (packet.size() % max_transfer_size == 0) {
        // Send a zero-length packet to indicate end of frame
        m_out_pipe->submit_bulk_out_transfer(0, nullptr).release_value_but_fixme_should_propagate_errors();
    }
    dmesgln("CDC-ECM: Sent");
}

void CDCECMNetworkAdapter::poll_thread()
{
    // FIXME: Listen for a ResponseAvailable Notification
    size_t const max_packet_size = m_in_pipe->max_packet_size();
    auto buffer = ByteBuffer::create_uninitialized(mtu()).release_value_but_fixme_should_propagate_errors();
    // Note: The stitching is most likely not needed, as the USB controller should
    //       already stitch packets together for us until it hits a short packet,
    //       or the buffer is full. (which's size matches the device advertised MTU/max segment size, so it should be fine)
    size_t offset = 0;
    while (!Process::current().is_dying()) {
        dmesgln("POLL");
        size_t received_length = m_in_pipe->submit_bulk_in_transfer(buffer.size() - offset, buffer.data() + offset).release_value_but_fixme_should_propagate_errors();
        dmesgln("POLL END");
        if (received_length != max_packet_size) {
            dmesgln("POLL Full packet received: {}", offset + received_length);
            // A small packet indicates the end of a frame
            did_receive(buffer.span().slice(0, offset + received_length));
            offset = 0;
        } else {
            offset += received_length;
        }
    }
}

}
