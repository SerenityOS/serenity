/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/Storage/USB/BOT/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/BOT/Codes.h>
#include <Kernel/Devices/Storage/USB/SCSIComands.h>

namespace Kernel::USB {

BulkSCSIInterface::BulkSCSIInterface(USB::Device& device, USBInterface const& interface, NonnullOwnPtr<BulkInPipe> in_pipe, NonnullOwnPtr<BulkOutPipe> out_pipe)
    : m_device(device)
    , m_interface(interface)
    , m_in_pipe(move(in_pipe))
    , m_out_pipe(move(out_pipe))
{
}

ErrorOr<NonnullLockRefPtr<BulkSCSIInterface>> BulkSCSIInterface::initialize(USB::Device& device, USBInterface const& interface, NonnullOwnPtr<BulkInPipe> in_pipe, NonnullOwnPtr<BulkOutPipe> out_pipe)
{
    auto bulk_scsi_interface = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) BulkSCSIInterface(
        device,
        interface,
        move(in_pipe),
        move(out_pipe))));

    SCSI::Inquiry inquiry_command {};
    inquiry_command.allocation_length = sizeof(SCSI::StandardInquiryData);

    SCSI::StandardInquiryData inquiry_data;

    auto inquiry_response = TRY(bulk_scsi_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(inquiry_command, &inquiry_data, sizeof(inquiry_data)));
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
    if ((inquiry_data.version < 3 || inquiry_data.version > 7) && inquiry_data.version != 0) {
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
        auto test_unit_ready_response = TRY(bulk_scsi_interface->send_scsi_command<SCSIDataDirection::NoData>(test_unit_ready_command));

        if (test_unit_ready_response.status == CSWStatus::Passed)
            break;

        SCSI::RequestSense request_sense_command {};
        SCSI::FixedFormatSenseData sense_data;

        request_sense_command.allocation_length = sizeof(sense_data);

        auto request_sense_response = TRY(bulk_scsi_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(request_sense_command, &sense_data, sizeof(sense_data)));
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
    auto status = TRY(bulk_scsi_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(SCSI::ReadCapacity10 {}, &capacity, sizeof(capacity)));

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
        // FIXME: Support multiple LUNs per device
        0
    };

    auto storage_device = TRY(Kernel::Device::try_create_device<BulkSCSIStorageDevice>(
        *bulk_scsi_interface,
        lun,
        device.address(), // FIXME: Figure out a better ID to put here
        capacity.block_size,
        capacity.block_count));

    bulk_scsi_interface->add_storage_device(storage_device);

    StorageManagement::the().add_device(storage_device);

    return bulk_scsi_interface;
}

ErrorOr<void> BulkSCSIInterface::perform_reset_recovery()
{
    // 5.3.4 Reset Recovery

    TRY(m_device.control_transfer(
        USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_INTERFACE | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        to_underlying(MassStorage::RequestCodes::BulkOnlyMassStorageReset), 0, m_interface.descriptor().interface_id, 0, nullptr));

    TRY(m_device.control_transfer(
        USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_ENDPOINT | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        USB_REQUEST_CLEAR_FEATURE, USB_FEATURE_ENDPOINT_HALT, m_in_pipe->endpoint_address(), 0, nullptr));

    TRY(m_device.control_transfer(
        USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_ENDPOINT | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        USB_REQUEST_CLEAR_FEATURE, USB_FEATURE_ENDPOINT_HALT, m_out_pipe->endpoint_address(), 0, nullptr));

    return {};
}

BulkSCSIInterface::~BulkSCSIInterface()
{
    for (auto& storage_device : m_storage_devices)
        StorageManagement::the().remove_device(storage_device);
}

}
