/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Devices/Storage/USB/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/SCSIComands.h>

namespace Kernel::USB {

BulkSCSIInterface::BulkSCSIInterface(LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, size_t sector_size, u64 max_addressable_block, USB::Device& device, NonnullOwnPtr<BulkInPipe> in_pipe, NonnullOwnPtr<BulkOutPipe> out_pipe)
    : StorageDevice(logical_unit_number_address, hardware_relative_controller_id, sector_size, max_addressable_block)
    , m_device(device)
    , m_in_pipe(move(in_pipe))
    , m_out_pipe(move(out_pipe))
{
}

void BulkSCSIInterface::start_request(AsyncBlockDeviceRequest& request)
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

ErrorOr<void> BulkSCSIInterface::do_read(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t)
{
    // FIXME: Error Handling and proper device reset on exit
    CommandBlockWrapper command;
    SCSI::Read10 read_command;

    u32 block_index_to_read = block_index;
    u32 blocks_read = 0;
    UserOrKernelBuffer destination_buffer = buffer;
    while (blocks_read < block_count) {
        read_command.logical_block_address = block_index_to_read;

        u16 transfer_length_bytes = min((block_count - blocks_read) * block_size(), AK::NumericLimits<u16>::max());

        read_command.transfer_length = transfer_length_bytes / block_size();

        command.transfer_length = transfer_length_bytes;
        command.direction = CBWDirection::DataIn;
        command.set_command(read_command);

        TRY(m_out_pipe->submit_bulk_out_transfer(sizeof(command), &command));

        TRY(m_in_pipe->submit_bulk_in_transfer(transfer_length_bytes, destination_buffer));

        CommandStatusWrapper status;
        TRY(m_in_pipe->submit_bulk_in_transfer(sizeof(status), &status));

        if (status.status != CSWStatus::Passed) {
            // FIXME: Actually handle the error
            //        See usbmassbulk 5.3, 6.4 and 6.5
            dmesgln("SCSI/BBB: Read failed with code {}", to_underlying(status.status));
            return EIO;
        }

        u32 bytes_transferred = transfer_length_bytes - status.data_residue;
        u32 blocks_read_in_transfer = bytes_transferred / block_size();

        blocks_read += blocks_read_in_transfer;
        block_index_to_read += blocks_read_in_transfer;
    }

    return {};
}

ErrorOr<void> BulkSCSIInterface::do_write(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t)
{
    // FIXME: Error Handling and proper device reset on exit

    CommandBlockWrapper command;
    SCSI::Write10 read_command;

    u32 block_index_to_read = block_index;
    u32 blocks_read = 0;
    UserOrKernelBuffer destination_buffer = buffer;
    while (blocks_read < block_count) {
        read_command.logical_block_address = block_index_to_read;

        u16 transfer_length_bytes = min((block_count - blocks_read) * block_size(), AK::NumericLimits<u16>::max());

        read_command.transfer_length = transfer_length_bytes / block_size();

        command.transfer_length = transfer_length_bytes;
        command.direction = CBWDirection::DataOut;
        command.set_command(read_command);

        TRY(m_out_pipe->submit_bulk_out_transfer(sizeof(command), &command));

        TRY(m_out_pipe->submit_bulk_out_transfer(transfer_length_bytes, destination_buffer));

        CommandStatusWrapper status;
        TRY(m_in_pipe->submit_bulk_in_transfer(sizeof(status), &status));

        if (status.status != CSWStatus::Passed) {
            // FIXME: Actually handle the error
            //        See usbmassbulk 5.3, 6.4 and 6.5
            dmesgln("SCSI/BBB: Write failed with code {}", to_underlying(status.status));
            return EIO;
        }

        u32 bytes_transferred = transfer_length_bytes - status.data_residue;
        u32 blocks_read_in_transfer = bytes_transferred / block_size();
        blocks_read += blocks_read_in_transfer;
        block_index_to_read += blocks_read_in_transfer;
    }

    return {};
}
}
