/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/USB/SCSIComands.h>
#include <Kernel/Devices/Storage/USB/UAS/UASInterface.h>
#include <Kernel/Devices/Storage/USB/UAS/UASStorageDevice.h>

namespace Kernel::USB {

UASStorageDevice::UASStorageDevice(UASInterface& interface, LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, size_t sector_size, u64 max_addressable_block)
    : StorageDevice(logical_unit_number_address, hardware_relative_controller_id, sector_size, max_addressable_block)
    , m_interface(interface)
{
    // Note: If this fails, it only means that we may be inefficient in our way
    //       of talking to the device.
    (void)query_characteristics();
}

ErrorOr<void> UASStorageDevice::query_characteristics()
{
    SCSI::Inquiry inquiry_command {};
    inquiry_command.enable_vital_product_data = 1;
    alignas(SCSI::SupportedVitalProductPages) u8 vital_product_page_buffer[0xfc];
    SCSI::SupportedVitalProductPages& vital_product_page = *reinterpret_cast<SCSI::SupportedVitalProductPages*>(vital_product_page_buffer);

    inquiry_command.page_code = SCSI::VitalProductDataPageCode::SupportedVitalProductDataPages;
    inquiry_command.allocation_length = sizeof(vital_product_page_buffer);

    auto status = TRY(m_interface.send_scsi_command<SCSIDataDirection::DataToInitiator>(inquiry_command, &vital_product_page, sizeof(vital_product_page_buffer)));

    if (!status.is_sense()) {
        dmesgln("SCSI/UAS: Expected Sense IU, got ID {:02x} instead", static_cast<u8>(status.response.header.iu_id));
        return EIO;
    }

    if (auto& sense = status.as_sense(); sense.status != SCSI::StatusCode::Good) {
        dbgln("SCSI/UAS: Inquiry failed to inquire supported vital product data pages with code {}", sense.status);
        // FIXME: Maybe request sense here
        // FIXME: Treating this as an error for now
        // Some HW seems to stall this and/or send garbage...
        return EIO;
    }

    if (vital_product_page.page_code != SCSI::VitalProductDataPageCode::SupportedVitalProductDataPages) {
        dmesgln("SCSI/UAS: Returned wrong page code for supported vital product data pages: {:#02x}", to_underlying(vital_product_page.page_code));
        return EIO;
    }

    if ((vital_product_page.page_length + 4uz) > sizeof(vital_product_page_buffer)) {
        // Note: This should not be possible, as there are less than 253 page codes allocated
        dmesgln("SCSI/UAS: Warning: Returned page length for supported vital product data pages is bigger than the allocated buffer, we might be missing some supported pages");
    }

    // FIXME: Maybe check status.residual_data here
    auto available_pages = min(vital_product_page.page_length, sizeof(vital_product_page_buffer) - sizeof(SCSI::VitalProductPage));
    bool found_block_limits = false;
    for (size_t i = 0; i < available_pages; i++) {
        if (vital_product_page.supported_pages[i] == SCSI::VitalProductDataPageCode::BlockLimits) {
            found_block_limits = true;
            break;
        }
        if (to_underlying(vital_product_page.supported_pages[i]) >= to_underlying(SCSI::VitalProductDataPageCode::BlockLimits)) {
            // The available pages are (supposedly) sorted in ascending order
            // so we can break here early
            break;
        }
    }

    if (!found_block_limits) {
        dmesgln("SCSI/UAS: Device does not support block limits page");
        // This is not an error, we just won't be able to optimize our transfers
        return {};
    }

    inquiry_command.page_code = SCSI::VitalProductDataPageCode::BlockLimits;
    SCSI::BlockLimitsPage block_limits_page {};
    inquiry_command.allocation_length = sizeof(SCSI::BlockLimitsPage);
    status = TRY(m_interface.send_scsi_command<SCSIDataDirection::DataToInitiator>(inquiry_command, &block_limits_page, sizeof(SCSI::BlockLimitsPage)));

    if (!status.is_sense()) {
        dmesgln("SCSI/UAS: Expected Sense IU, got ID {:02x} instead", static_cast<u8>(status.response.header.iu_id));
        return EIO;
    }
    if (auto sense = status.as_sense(); sense.status != SCSI::StatusCode::Good) {
        dbgln("SCSI/UAS: Inquiry failed to inquire block limits with code {}", sense.status);
        // FIXME: Maybe request sense here
    }

    if (block_limits_page.page_code != SCSI::VitalProductDataPageCode::BlockLimits) {
        dmesgln("SCSI/UAS: Returned wrong page code for block limits {:#02x}", to_underlying(block_limits_page.page_code));
        return EIO;
    }

    if (block_limits_page.page_length != sizeof(SCSI::BlockLimitsPage) - 4) {
        dmesgln("SCSI/UAS: Returned wrong page length for block limits {}", block_limits_page.page_length);
        return EIO;
    }

    if (block_limits_page.maximum_transfer_length != 0)
        m_maximum_transfer_length = block_limits_page.maximum_transfer_length;
    if (block_limits_page.optimal_transfer_length != 0)
        m_optimal_transfer_length = block_limits_page.optimal_transfer_length;
    if (block_limits_page.optimal_transfer_length_granularity != 0)
        m_optimal_transfer_length_granularity = block_limits_page.optimal_transfer_length_granularity;

    dbgln("SCSI/UAS: Maximum transfer length: {}", m_maximum_transfer_length);
    dbgln("SCSI/UAS: Optimal transfer length: {}", m_optimal_transfer_length);
    dbgln("SCSI/UAS: Optimal transfer length granularity: {}", m_optimal_transfer_length_granularity);

    return {};
}

u32 UASStorageDevice::optimal_block_count(u32 blocks)
{
    if (m_maximum_transfer_length.has_value() && blocks > m_maximum_transfer_length.value())
        return m_maximum_transfer_length.value();
    // quot. OPTIMAL TRANSFER LENGTH field:
    // "[...] If a device server receives one of these commands with a transfer size greater than this value,
    //  then the device server may incur significant delays in processing the command."
    if (m_optimal_transfer_length.has_value() && blocks > m_optimal_transfer_length.value())
        return m_optimal_transfer_length.value();

    if (!m_optimal_transfer_length_granularity.has_value())
        return blocks;

    // quot. OPTIMAL TRANSFER LENGTH GRANULARITY field:
    // "[...] If a device server receives one of these commands with a transfer size that
    //  is not equal to a multiple of this value, then the device server may incur significant
    //  delays in processing the command."
    // FIXME: This sounds like it may be faster to align up to the granularity in some cases
    //        But that might be difficult to accomplish in some cases (Ie writing)
    if (blocks < m_optimal_transfer_length_granularity.value())
        return blocks;

    return blocks - (blocks % m_optimal_transfer_length_granularity.value());
}

void UASStorageDevice::start_request(AsyncBlockDeviceRequest& request)
{
    if (request.request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
        if (do_read(request.block_index(), request.block_count(), request.buffer(), request.buffer_size()).is_error()) {
            request.complete(AsyncDeviceRequest::RequestResult::Failure);
        } else {
            request.complete(AsyncDeviceRequest::RequestResult::Success);
        }
    } else {
        if (do_write(request.block_index(), request.block_count(), request.buffer(), request.buffer_size()).is_error()) {
            request.complete(AsyncDeviceRequest::RequestResult::Failure);
        } else {
            request.complete(AsyncDeviceRequest::RequestResult::Success);
        }
    }
}

ErrorOr<void> UASStorageDevice::do_read(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t)
{
    // FIXME: Error Handling and proper device reset on exit
    SCSI::Read10 read_command;

    u32 block_index_to_read = block_index;
    u32 blocks_read = 0;
    UserOrKernelBuffer destination_buffer = buffer;
    while (blocks_read < block_count) {
        read_command.logical_block_address = block_index_to_read;

        // FIXME: We only use READ(10) so we only ever read u16::max blocks at a time
        auto blocks_to_transfer = optimal_block_count(block_count - blocks_read);
        u16 transfer_length_bytes = min(blocks_to_transfer * block_size(), AK::NumericLimits<u16>::max());

        read_command.transfer_length = transfer_length_bytes / block_size();

        auto status = TRY(m_interface.send_scsi_command<SCSIDataDirection::DataToInitiator>(read_command, destination_buffer, transfer_length_bytes));

        if (!status.is_sense()) {
            dmesgln("SCSI/UAS: Read did not return Sense IU, aborting");
            return EIO;
        }

        if (auto sense = status.as_sense(); sense.status != SCSI::StatusCode::Good) {
            // FIXME: Actually handle the error
            dmesgln("SCSI/UAS: Read failed with status {}", sense.status);
            return EIO;
        }

        u32 bytes_transferred = status.transfer_size;
        u32 blocks_read_in_transfer = bytes_transferred / block_size();

        blocks_read += blocks_read_in_transfer;
        block_index_to_read += blocks_read_in_transfer;
    }

    return {};
}

ErrorOr<void> UASStorageDevice::do_write(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t)
{
    // FIXME: Error Handling and proper device reset on exit
    SCSI::Write10 read_command;

    u32 block_index_to_read = block_index;
    u32 blocks_read = 0;
    UserOrKernelBuffer source_buffer = buffer;
    while (blocks_read < block_count) {
        read_command.logical_block_address = block_index_to_read;

        // FIXME: We only use WRITE(10) so we only ever write u16::max blocks at a time
        auto blocks_to_transfer = optimal_block_count(block_count - blocks_read);
        u16 transfer_length_bytes = min(blocks_to_transfer * block_size(), AK::NumericLimits<u16>::max());

        read_command.transfer_length = transfer_length_bytes / block_size();

        auto status = TRY(m_interface.send_scsi_command<SCSIDataDirection::DataToTarget>(read_command, source_buffer, transfer_length_bytes));

        if (!status.is_sense()) {
            dmesgln("SCSI/UAS: Write did not return Sense IU, aborting");
            return EIO;
        }

        if (auto sense = status.as_sense(); sense.status != SCSI::StatusCode::Good) {
            // FIXME: Actually handle the error
            dmesgln("SCSI/UAS: Write failed with status {}", sense.status);
            return EIO;
        }

        u32 bytes_transferred = status.transfer_size;
        u32 blocks_read_in_transfer = bytes_transferred / block_size();
        blocks_read += blocks_read_in_transfer;
        block_index_to_read += blocks_read_in_transfer;
    }

    return {};
}

}
