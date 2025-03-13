/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <Kernel/Bus/USB/Drivers/MassStorage/MassStorageDriver.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/Storage/USB/BOT/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/BOT/Codes.h>
#include <Kernel/Devices/Storage/USB/UAS/Structures.h>
#include <Kernel/Devices/Storage/USB/UAS/UASInterface.h>

namespace Kernel::USB {

using namespace MassStorage;

USB_DEVICE_DRIVER(MassStorageDriver);

void MassStorageDriver::init()
{
    auto driver = MUST(adopt_nonnull_lock_ref_or_enomem(new MassStorageDriver()));
    USBManagement::register_driver(driver);
}

ErrorOr<void> MassStorageDriver::probe(USB::Device& device)
{
    // USB massbulk Table 4.1:
    if (device.device_descriptor().device_class != USB_CLASS_DEVICE
        || device.device_descriptor().device_sub_class != 0x00
        || device.device_descriptor().device_protocol != 0x00)
        return ENOTSUP;

    for (auto const& config : device.configurations()) {
        // FIXME: There might be multiple MassStorage configs present,
        //        figure out how to decide which one to take,
        //        although that's very unlikely
        Optional<USBInterface const&> bot_interface;
        Optional<USBInterface const&> uas_interface;

        for (auto const& interface : config.interfaces()) {
            if (interface.descriptor().interface_class_code != USB_CLASS_MASS_STORAGE)
                continue;

            if (interface.descriptor().interface_protocol == to_underlying(TransportProtocol::UAS))
                uas_interface = interface;
            else if (interface.descriptor().interface_protocol == to_underlying(TransportProtocol::BBB))
                bot_interface = interface;
            else
                dmesgln("USB MassStorage Interface for device {:04x}:{:04x} has unsupported protocol {}", device.device_descriptor().vendor_id, device.device_descriptor().product_id,
                    transport_protocol_string(static_cast<TransportProtocol>(interface.descriptor().interface_protocol)));
        }

        if (!bot_interface.has_value() && !uas_interface.has_value())
            continue;

        dmesgln("USB MassStorage Interfaces for device {:04x}:{:04x} found:", device.device_descriptor().vendor_id, device.device_descriptor().product_id);
        dmesgln("    Configuration: {}", config.configuration_id());
        dmesgln("    BOT Interface: {}", bot_interface.has_value());
        dmesgln("    UAS Interface: {}", uas_interface.has_value());

        if (uas_interface.has_value() && device.speed() != USB::Device::DeviceSpeed::SuperSpeed) {
            // FIXME: We only support UAS on version < 3.0 devices
            //        as we don't support streams, which are mandatory for UAS on USB 3.0 devices,
            // as they replace the Read/WriteReady signals to and leverage stream IDs instead
            dmesgln("    Using UAS interface");
            TRY(initialise_uas_device(device, *uas_interface));
            return {};
        }

        if (bot_interface.has_value()) {
            dmesgln("    Using BOT interface");
            TRY(initialise_bulk_only_device(device, *bot_interface));
            return {};
        }
    }

    return ENOTSUP;
}

ErrorOr<void> MassStorageDriver::initialise_bulk_only_device(USB::Device& device, USBInterface const& interface)
{
    auto const& descriptor = interface.descriptor();

    if (descriptor.interface_sub_class_code != to_underlying(MassStorage::SubclassCode::SCSI_transparent))
        return ENOTSUP;

    TRY(device.set_configuration_and_interface(interface));

    u8 max_luns;
    TRY(device.control_transfer(
        USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_INTERFACE | USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST,
        to_underlying(MassStorage::RequestCodes::GetMaxLun), 0, interface.descriptor().interface_id, 1, &max_luns));
    // FIXME: Devices that do not support multiple LUNs may STALL this command
    // FIXME: Support multiple LUNs
    if (max_luns != 0)
        dmesgln("SCSI/BBB: WARNING: USB Mass Storage Device supports multiple LUNs ({}) only targetting first LUN", max_luns);

    u8 in_pipe_endpoint_number = 0xff;
    u16 in_max_packet_size;
    u8 out_pipe_endpoint_number = 0xff;
    u16 out_max_packet_size;

    if (interface.descriptor().number_of_endpoints < 2) {
        dmesgln("SCSI/BBB: Interface does not provide enough endpoints for advertised Bulk-only transfer protocol; Rejecting");
        return ENOTSUP;
    }

    for (auto const& endpoint : interface.endpoints()) {
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
        dmesgln("SCSI/BBB: Interface did not advertise two Bulk Endpoints; Rejecting");
        return ENOTSUP;
    }

    auto in_pipe = TRY(BulkInPipe::create(device.controller(), device, in_pipe_endpoint_number, in_max_packet_size));
    auto out_pipe = TRY(BulkOutPipe::create(device.controller(), device, out_pipe_endpoint_number, out_max_packet_size));

    auto bulk_scsi_interface = TRY(BulkSCSIInterface::initialize(
        device,
        interface,
        move(in_pipe),
        move(out_pipe)));

    m_bot_interfaces.append(move(bulk_scsi_interface));

    return {};
}

ErrorOr<void> MassStorageDriver::initialise_uas_device(USB::Device& device, USBInterface const& interface)
{
    auto const& descriptor = interface.descriptor();
    auto const& configuration = interface.configuration();

    if (descriptor.interface_sub_class_code != to_underlying(MassStorage::SubclassCode::SCSI_transparent))
        return ENOTSUP;

    TRY(device.set_configuration_and_interface(interface));

    Optional<u8> command_pipe_endpoint_number;
    u16 command_max_packet_size;
    Optional<u8> status_pipe_endpoint_number;
    u16 status_max_packet_size;
    Optional<u8> in_pipe_endpoint_number;
    u16 in_max_packet_size;
    Optional<u8> out_pipe_endpoint_number;
    u16 out_max_packet_size;

    if (interface.descriptor().number_of_endpoints < 4) {
        dmesgln("SCSI/UAS: Interface does not provide enough endpoints for advertised UAS transfer protocol; Rejecting");
        return EIO;
    }

    Optional<u8> last_seen_endpoint_number;
    u16 last_seen_max_packet_size;

    TRY(configuration.for_each_descriptor_in_interface(interface,
        [&](ReadonlyBytes descriptor_data) -> ErrorOr<IterationDecision> {
            auto const& descriptor_header = *bit_cast<USBDescriptorCommon const*>(descriptor_data.data());

            if (descriptor_header.descriptor_type == DESCRIPTOR_TYPE_ENDPOINT) {
                auto descriptor = bit_cast<USBEndpointDescriptor const*>(descriptor_data.data());
                if ((descriptor->endpoint_attributes_bitmap & USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) != USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_BULK)
                    return IterationDecision::Continue;
                last_seen_endpoint_number = descriptor->endpoint_address & 0b1111;
                last_seen_max_packet_size = descriptor->max_packet_size;
                return IterationDecision::Continue;
            }

            // Note: The spec says that the Pipe Usage Descriptor should be the first descriptor after the Endpoint Descriptor,
            //       but we don't enforce that here
            //       As other descriptors, like the SuperSpeed Endpoint Companion Descriptor, may be present in between
            if (descriptor_header.descriptor_type != UAS_PIPE_USAGE_DESCRIPTOR)
                return IterationDecision::Continue;

            if (descriptor_data.size() < sizeof(PipeUsageDescriptor)) {
                dmesgln("SCSI/UAS: Provided Pipe Usage Descriptor is too small; Rejecting");
                return EIO;
            }

            auto descriptor = *bit_cast<PipeUsageDescriptor const*>(descriptor_data.data());

            if (!last_seen_endpoint_number.has_value()) {
                dmesgln("SCSI/UAS: Found Pipe Usage Descriptor without preceding Endpoint Descriptor; Rejecting");
                return EIO;
            }

            using enum PipeID;
            switch (descriptor.pipe_id) {
            case CommandPipe:
                command_pipe_endpoint_number = last_seen_endpoint_number;
                command_max_packet_size = last_seen_max_packet_size;
                break;
            case StatusPipe:
                status_pipe_endpoint_number = last_seen_endpoint_number;
                status_max_packet_size = last_seen_max_packet_size;
                break;
            case DataInPipe:
                in_pipe_endpoint_number = last_seen_endpoint_number;
                in_max_packet_size = last_seen_max_packet_size;
                break;
            case DataOutPipe:
                out_pipe_endpoint_number = last_seen_endpoint_number;
                out_max_packet_size = last_seen_max_packet_size;
                break;
            }

            last_seen_endpoint_number.clear();
            last_seen_max_packet_size = 0;

            return IterationDecision::Continue;
        }));

    if (!in_pipe_endpoint_number.has_value()
        || !out_pipe_endpoint_number.has_value()
        || !command_pipe_endpoint_number.has_value()
        || !status_pipe_endpoint_number.has_value()) {
        dmesgln("SCSI/UAS: Interface did not advertise all required Bulk Endpoints; Rejecting");
        return EIO;
    }

    auto command_pipe = TRY(BulkOutPipe::create(device.controller(), device, *command_pipe_endpoint_number, command_max_packet_size));
    auto status_pipe = TRY(BulkInPipe::create(device.controller(), device, *status_pipe_endpoint_number, status_max_packet_size));
    auto in_pipe = TRY(BulkInPipe::create(device.controller(), device, *in_pipe_endpoint_number, in_max_packet_size));
    auto out_pipe = TRY(BulkOutPipe::create(device.controller(), device, *out_pipe_endpoint_number, out_max_packet_size));

    auto uas_interface = TRY(UASInterface::initialize(
        device,
        interface,
        move(command_pipe),
        move(status_pipe),
        move(in_pipe),
        move(out_pipe)));

    m_uas_interfaces.append(move(uas_interface));

    return {};
}

void MassStorageDriver::detach(USB::Device& device)
{
    if (auto&& interface = AK::find_if(m_bot_interfaces.begin(), m_bot_interfaces.end(), [&device](auto& interface) { return &interface.device() == &device; }); interface != m_bot_interfaces.end()) {
        m_bot_interfaces.remove(*interface);
        return;
    }
    if (auto&& interface = AK::find_if(m_uas_interfaces.begin(), m_uas_interfaces.end(), [&device](auto& interface) { return &interface.device() == &device; }); interface != m_uas_interfaces.end()) {
        m_uas_interfaces.remove(*interface);
        return;
    }
    VERIFY_NOT_REACHED();
}

}
