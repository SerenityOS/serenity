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
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/Storage/USB/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/Codes.h>
#include <Kernel/Devices/Storage/USB/SCSIComands.h>

namespace Kernel::USB {

using namespace MassStorage;

USB_DEVICE_DRIVER(MassStorageDriver);

void MassStorageDriver::init()
{
    auto driver = MUST(adopt_nonnull_lock_ref_or_enomem(new MassStorageDriver()));
    USBManagement::register_driver(driver);
}

ErrorOr<void> MassStorageDriver::checkout_interface(USB::Device& device, USBInterface const& interface)
{
    auto const& descriptor = interface.descriptor();

    if (descriptor.interface_class_code != USB_CLASS_MASS_STORAGE)
        return ENOTSUP;

    dmesgln("USB MassStorage Interface for device {}:{} found:", device.device_descriptor().vendor_id, device.device_descriptor().product_id);
    dmesgln("    Subclass: {} [{:#02x}]", MassStorage::subclass_string((SubclassCode)descriptor.interface_sub_class_code), descriptor.interface_sub_class_code);
    dmesgln("    Protocol: {} [{:#02x}]", MassStorage::transport_protocol_string((TransportProtocol)descriptor.interface_protocol), descriptor.interface_protocol);

    // FIXME: Find a nice way of handling multiple device subclasses and protocols
    if (descriptor.interface_protocol == to_underlying(TransportProtocol::BBB))
        return initialise_bulk_only_device(device, interface);

    return ENOTSUP;
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
        bool has_accepted_an_interface = false;
        for (auto const& interface : config.interfaces()) {
            // FIXME: Handle multiple interfaces
            //        Interface may coexist at the same time,
            //        but having multiple handles on the same data storage seems like a bad idea,
            //        so:
            // FIXME: Choose the best supported interface
            //        UAS for example is supposed to be better than BBB, but BBB will always
            //        be the first listed interface of them, when both are supported
            auto result = checkout_interface(device, interface);
            if (result.is_error())
                continue;

            has_accepted_an_interface = true;
        }

        if (has_accepted_an_interface)
            return {};
    }

    return ENOTSUP;
}

ErrorOr<void> MassStorageDriver::initialise_bulk_only_device(USB::Device& device, USBInterface const& interface)
{
    auto const& descriptor = interface.descriptor();
    auto const& configuration = interface.configuration();

    if (descriptor.interface_sub_class_code != to_underlying(MassStorage::SubclassCode::SCSI_transparent))
        return ENOTSUP;

    TRY(device.control_transfer(
        USB_REQUEST_RECIPIENT_DEVICE | USB_REQUEST_TYPE_STANDARD | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        USB_REQUEST_SET_CONFIGURATION, configuration.configuration_id(), 0, 0, nullptr));

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

    SCSI::Inquiry inquiry_command {};
    inquiry_command.allocation_length = sizeof(SCSI::StandardInquiryData);

    SCSI::StandardInquiryData inquiry_data;

    auto inquiry_response = TRY(send_scsi_command<SCSIDataDirection::DataToInitiator>(*out_pipe, *in_pipe, inquiry_command, &inquiry_data, sizeof(inquiry_data)));
    if (inquiry_response.status != CSWStatus::Passed) {
        dmesgln("SCSI/BBB: Inquiry failed with code {}", to_underlying(inquiry_response.status));
        return EIO;
    }
    dmesgln("    Device Type: {}", inquiry_data.device_type_string());
    dmesgln("    Peripheral Qualifier: {:#03b}", (u8)inquiry_data.peripheral_info.qualifier);
    dmesgln("    Removable: {}", (inquiry_data.removable & 0x80) == 0x80);
    dmesgln("    Version: {:#02x}", inquiry_data.version);
    dmesgln("    Vendor: {}", StringView { inquiry_data.vendor_id, 8 });
    dmesgln("    Product: {}", StringView { inquiry_data.product_id, 16 });
    dmesgln("    Revision: {}", StringView { inquiry_data.product_revision_level, 4 });
    if (inquiry_data.peripheral_info.device_type != SCSI::StandardInquiryData::DeviceType::DirectAccessBlockDevice) {
        dmesgln("SCSI/BBB: Device is not a Direct Access Block device; Rejecting");
        return ENOTSUP;
    }
    if (inquiry_data.version < 3 || inquiry_data.version > 7) {
        dmesgln("SCSI/BBB: Device SCSI version not supported ({:#02x}); Rejecting", inquiry_data.version);
        return ENOTSUP;
    }
    if (inquiry_data.response_data.response_data_format != 2) {
        // SCSI Commands Reference Manual, Rev. J states that only format 2 is valid,
        // and that format 1 is obsolete, but does not actually specify what format 1 would have been
        // so ENOTSUP to be safe
        dmesgln("SCSI/BBB: Device does not support response data format 2 (got {} instead); Rejecting", (u8)inquiry_data.response_data.response_data_format);
        return ENOTSUP;
    }

    // FIXME: Re-query INQUIRY if the DRIVE SERIAL NUMBER field is present (see the ADDITIONAL LENGTH field), to record it
    //        (bytes 36-43 ~ 8 bytes)

    size_t tries = 0;
    constexpr size_t max_tries = 5;
    while (tries < max_tries) {
        SCSI::TestUnitReady test_unit_ready_command {};
        auto test_unit_ready_response = TRY(send_scsi_command<SCSIDataDirection::NoData>(*out_pipe, *in_pipe, test_unit_ready_command));

        if (test_unit_ready_response.status == CSWStatus::Passed)
            break;

        SCSI::RequestSense request_sense_command {};
        SCSI::FixedFormatSenseData sense_data;

        request_sense_command.allocation_length = sizeof(sense_data);

        auto request_sense_response = TRY(send_scsi_command<SCSIDataDirection::DataToInitiator>(*out_pipe, *in_pipe, request_sense_command, &sense_data, sizeof(sense_data)));
        if (request_sense_response.status != CSWStatus::Passed) {
            dmesgln("SCSI/BBB: Request Sense failed with code {}, possibly unimplemented", to_underlying(request_sense_response.status));
            return EIO;
        }
        // FIXME: Maybe hide this behind a debug flag, as some hardware fails once after startup
        dbgln("SCSI/BBB: TestUnitReady Failed:");
        // FIXME: to_string() these
        dbgln("    Sense Key: {:#02x}", (u8)sense_data.sense_key);
        dbgln("    Additional Sense Code: {:#02x}", (u8)sense_data.additional_sense_code);
        dbgln("    Additional Sense Code Qualifier: {:#02x}", (u8)sense_data.additional_sense_code_qualifier);

        ++tries;
    }
    if (tries == max_tries) {
        dmesgln("SCSI/BBB: TestUnitReady failed too many times");
        return EIO;
    }

    SCSI::ReadCapacity10Parameters capacity;
    auto status = TRY(send_scsi_command<SCSIDataDirection::DataToInitiator>(*out_pipe, *in_pipe, SCSI::ReadCapacity10 {}, &capacity, sizeof(capacity)));

    if (status.data_residue != 0) {
        dmesgln("SCSI/BBB: Read Capacity returned with non-zero data residue; Rejecting");
        return EIO;
    }

    if (status.status != CSWStatus::Passed) {
        dmesgln("SCSI/BBB: Failed to query USB Drive capacity; Rejecting");
        // FIXME: More error handling
        return ENOTSUP;
    }

    dmesgln("    Block Size: {}B", capacity.block_size);
    dmesgln("    Block Count: {}", capacity.block_count);
    dmesgln("    Total Size: {}MiB", (u64)capacity.block_size * capacity.block_count / MiB);

    StorageDevice::LUNAddress lun = {
        device.controller().storage_controller_id(),
        device.address(),
        // FIXME: Again, support multiple LUNs per device
        0
    };

    auto bulk_scsi_interface = TRY(DeviceManagement::try_create_device<BulkSCSIInterface>(
        lun,
        device.address(), // FIXME: Figure out a better ID to put here
        capacity.block_size,
        capacity.block_count,
        device,
        move(in_pipe),
        move(out_pipe)));

    m_interfaces.append(bulk_scsi_interface);
    StorageManagement::the().add_device(bulk_scsi_interface);

    return {};
}

void MassStorageDriver::detach(USB::Device& device)
{
    auto&& interface = AK::find_if(m_interfaces.begin(), m_interfaces.end(), [&device](auto& interface) { return &interface.device() == &device; });

    StorageManagement::the().remove_device(*interface);
    m_interfaces.remove(*interface);
}

}
