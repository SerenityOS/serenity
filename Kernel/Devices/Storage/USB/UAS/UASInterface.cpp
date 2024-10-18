/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/Storage/USB/SCSIComands.h>
#include <Kernel/Devices/Storage/USB/UAS/UASInterface.h>
#include <Kernel/Devices/Storage/USB/UAS/UASStorageDevice.h>

namespace Kernel::USB {

UASInterface::UASInterface(USB::Device& device, USBInterface const& interface, NonnullOwnPtr<BulkOutPipe> command_pipe, NonnullOwnPtr<BulkInPipe> status_pipe, NonnullOwnPtr<BulkInPipe> data_in_pipe, NonnullOwnPtr<BulkOutPipe> data_out_pipe)
    : m_device(device)
    , m_interface(interface)
    , m_command_pipe(move(command_pipe))
    , m_status_pipe(move(status_pipe))
    , m_in_pipe(move(data_in_pipe))
    , m_out_pipe(move(data_out_pipe))
{
}

ErrorOr<NonnullLockRefPtr<UASInterface>> UASInterface::initialize(USB::Device& device, USBInterface const& interface, NonnullOwnPtr<BulkOutPipe> command_pipe, NonnullOwnPtr<BulkInPipe> status_pipe, NonnullOwnPtr<BulkInPipe> data_in_pipe, NonnullOwnPtr<BulkOutPipe> data_out_pipe)
{
    auto uas_interface = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) UASInterface(
        device,
        interface,
        move(command_pipe),
        move(status_pipe),
        move(data_in_pipe),
        move(data_out_pipe))));

    // FIXME: This has a lot of duplication with the BulkSCSIInterface::initialize function

    SCSI::Inquiry inquiry_command {};
    inquiry_command.allocation_length = sizeof(SCSI::StandardInquiryData);

    SCSI::StandardInquiryData inquiry_data;

    auto inquiry_response = TRY(uas_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(inquiry_command, &inquiry_data, sizeof(inquiry_data)));
    if (auto& sense = inquiry_response.as_sense(); sense.status != SCSI::StatusCode::Good) {
        dmesgln("SCSI/UAS: Inquiry failed with code {}", sense.status);
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
        dmesgln("SCSI/UAS: Device is not a Direct Access Block device; Rejecting");
        return ENOTSUP;
    }
    if ((inquiry_data.version < 3 || inquiry_data.version > 7) && inquiry_data.version != 0) {
        dmesgln("SCSI/UAS: Device SCSI version not supported ({:#02x}); Rejecting", inquiry_data.version);
        return ENOTSUP;
    }
    if (inquiry_data.response_data.response_data_format != 2) {
        // SCSI Commands Reference Manual, Rev. J states that only format 2 is valid,
        // and that format 1 is obsolete, but does not actually specify what format 1 would have been
        // so ENOTSUP to be safe
        dmesgln("SCSI/UAS: Device does not support response data format 2 (got {} instead); Rejecting", (u8)inquiry_data.response_data.response_data_format);
        return ENOTSUP;
    }

    // FIXME: Re-query INQUIRY if the DRIVE SERIAL NUMBER field is present (see the ADDITIONAL LENGTH field), to record it
    //        (bytes 36-43 ~ 8 bytes)

    size_t tries = 0;
    constexpr size_t max_tries = 5;
    while (tries < max_tries) {
        SCSI::TestUnitReady test_unit_ready_command {};
        auto test_unit_ready_response = TRY(uas_interface->send_scsi_command<SCSIDataDirection::NoData>(test_unit_ready_command));

        if (!test_unit_ready_response.is_sense()) {
            dmesgln("SCSI/UAS: TestUnitReady did not return Sense IU, aborting");
            return EIO;
        }
        auto& test_unit_ready_sense = test_unit_ready_response.as_sense();

        if (test_unit_ready_sense.status == SCSI::StatusCode::Good)
            break;

        dmesgln("SCSI/UAS: TestUnitReady Failed:");
        dmesgln("    Status Code: {}", test_unit_ready_sense.status);
        dmesgln("    Status Qualifier: {:04x}", test_unit_ready_sense.status_qualifier);
        // FIXME: Check if we have additional sense data and print it
        ++tries;
    }
    if (tries == max_tries) {
        dmesgln("SCSI/UAS: TestUnitReady failed too many times");
        return EIO;
    }

    // FIXME: Inquire Queue Depth and other capabilities
    // FIXME: Configure auto sense

    do {
        // Inquire LUNs
        // Note: Even if this fails it should be fine in most cases
        //       as LUN 0 should always be available
        //       (Technically the spec says that LUN 0 isn't mandatory and devices can
        //        have a specific version of the REPORT LUNS command instead, but let's ignore that for now
        //        as you still would need to talk to LUN 0 to get all the info and we only support 1 LUN anyway)
        SCSI::ReportLUNs report_luns_command {};
        alignas(SCSI::ReportLUNsParameterData) u8 report_luns_response_buffer[512];
        report_luns_command.allocation_length = sizeof(report_luns_response_buffer);

        auto report_luns_response = TRY(uas_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(report_luns_command, report_luns_response_buffer, sizeof(report_luns_response_buffer)));

        if (!report_luns_response.is_sense()) {
            dmesgln("SCSI/UAS: ReportLUNs did not return Sense IU; Assuming LUN 0 is available");
            break;
        }

        if (auto& sense = report_luns_response.as_sense(); sense.status != SCSI::StatusCode::Good) {
            dmesgln("SCSI/UAS: Failed to query LUNs: {}; Using LUN 0", sense.status);
            break;
        }

        bool has_lun_0 = false;
        auto& report_luns_response_data = *bit_cast<SCSI::ReportLUNsParameterData*>(&report_luns_response_buffer[0]);
        dmesgln("    Found {} LUN(s):", report_luns_response_data.lun_list_length / 8);
        for (size_t i = 0; i < report_luns_response_data.lun_list_length / 8; ++i) {
            // FIXME: Properly parse the LUNs, and create a device for each

            dmesgln("        {:016x}", report_luns_response_data.lun_list[i]);

            if (report_luns_response_data.lun_list[i] == 0)
                has_lun_0 = true;
        }

        if (!has_lun_0) {
            // FIXME?: See above about the availability of LUN 0
            dmesgln("SCSI/UAS: WARNING: LUN 0 not reported; Using LUN 0 anyway");
            break;
        }
    } while (false);

    SCSI::ReadCapacity10Parameters capacity;
    auto status = TRY(uas_interface->send_scsi_command<SCSIDataDirection::DataToInitiator>(SCSI::ReadCapacity10 {}, &capacity, sizeof(capacity)));

    // FIXME: BOT/BBB checks the data residue here,
    //        UAS does not seem to provide a similar field
    //        Should we do something similar?

    if (!status.is_sense()) {
        dmesgln("SCSI/UAS: ReadCapacity returned non Sense IU {:02x}; Rejecting", to_underlying(status.response.header.iu_id));
        return EIO;
    }

    if (auto& sense = status.as_sense(); sense.status != SCSI::StatusCode::Good) {
        dmesgln("SCSI/UAS: Failed to query USB Drive capacity: {}; Rejecting", sense.status);
        // FIXME: More error handling
        return ENOTSUP;
    }

    dmesgln("    Block Size: {}B", capacity.block_size);
    dmesgln("    Block Count: {}", capacity.block_count);
    dmesgln("    Total Size: {}MiB", (u64)capacity.block_size * capacity.block_count / MiB);

    // FIXME: UAS LUNs can be 64 bits,
    //        Possibly containing a Bus Number
    //        We only have space for 32 bits in the last field
    //        We also might want to re-evaluate our LUN layout/internal LUN handling
    StorageDevice::LUNAddress lun = {
        device.controller().storage_controller_id(),
        device.address(),
        // FIXME: Support multiple LUNs per device
        0
    };

    auto storage_device = TRY(::Device::try_create_device<UASStorageDevice>(
        *uas_interface,
        lun,
        device.address(), // FIXME: Figure out a better ID to put here
        capacity.block_size,
        capacity.block_count));

    uas_interface->add_storage_device(storage_device);

    StorageManagement::the().add_device(storage_device);

    return uas_interface;
}

UASInterface::~UASInterface()
{
    for (auto& storage_device : m_storage_devices)
        StorageManagement::the().remove_device(storage_device);
}

}
