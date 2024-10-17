/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Devices/Storage/USB/SCSIInterface.h>
#include <Kernel/Devices/Storage/USB/UAS/Structures.h>
#include <Kernel/Devices/Storage/USB/UAS/UASStorageDevice.h>

namespace Kernel::USB {

class UASInterface : public RefCounted<UASInterface> {

    union IU {
        // Note: As the SenseIU is of flexible size we need to forcefully allocate some data here.
        //       The maximum size of the sense data the device may send is controlled by the
        //       MAXIMUM SENSE DATA LENGTH field in the Control extension mode page of the device.
        //       In theory the maximum size of the sense data is 252 bytes
        //       Meaning the maximum size of the IU is 252+16=268 bytes
        //       Just to be safe we allocate 512 bytes here, as the spec tells us that the
        //       SenseIU is to not share a USB packet with any other IU, so we can just use
        //       to the maximum packet size, which is 512 bytes (USB3 allows up to 1024 bytes)
        u8 dummy[512] {};

        InformationUnitHeader header;
        CommandIU command;
        ResponseIU response;
        SenseIU sense;
        TaskManagementIU task_management;
        ReadReadyIU read_ready;
        WriteReadyIU write_ready;
    };

    struct SendSCSICommandResult {
        SenseIU& as_sense()
        {
            VERIFY(is_sense());
            return response.sense;
        }
        bool is_sense() const { return response.header.iu_id == IUID::Sense; }

        size_t transfer_size;
        size_t response_size;
        IU response;
    };

public:
    static ErrorOr<NonnullLockRefPtr<UASInterface>> initialize(USB::Device&, USBInterface const&, NonnullOwnPtr<BulkOutPipe> command_pipe, NonnullOwnPtr<BulkInPipe> status_pipe, NonnullOwnPtr<BulkInPipe> data_in_pipe, NonnullOwnPtr<BulkOutPipe> data_out_pipe);

    ~UASInterface();

    USB::Device const& device() const { return m_device; }

    template<SCSIDataDirection Direction, typename Command, typename Data = nullptr_t>
    requires(IsNullPointer<Data>
        || IsPointer<Data>
        || (Direction == SCSIDataDirection::DataToInitiator && IsSame<Data, UserOrKernelBuffer>)
        || (Direction == SCSIDataDirection::DataToTarget && IsSameIgnoringCV<Data, UserOrKernelBuffer>))
    ErrorOr<SendSCSICommandResult> send_scsi_command(Command const& command,
        Data data = nullptr, size_t data_size = 0)
    {
        // FIXME:
        static_assert(sizeof(Command) <= sizeof(CommandIU::cdb), "Command too large for CommandIU without additional_cbd_bytes");
        // Note: Once we support USB3 streams, this should not exceed the maximum stream id
        //       Ideally this would then pull from a free-list of tags
        u16 transfer_tag = m_next_tag++;
        CommandIU command_iu {};
        command_iu.header.iu_id = IUID::Command;
        command_iu.header.tag = transfer_tag;
        // FIXME: Properly(/configurably) set the task_info
        command_iu.task_info.attribute = TaskAttribute::Simple;
        command_iu.task_info.priority = 0;
        command_iu.set_command(command);

        dbgln_if(USB_MASS_STORAGE_DEBUG, "UAS: send_scsi_command (opcode {:#x}):", *bit_cast<u8 const*>(&command));

        dbgln_if(USB_MASS_STORAGE_DEBUG, "UAS:   -> CIU: {:hex-dump}", ReadonlyBytes { &command_iu, sizeof(command_iu) });
        dbgln_if(USB_MASS_STORAGE_DEBUG, "UAS:      CDB: {:hex-dump}", ReadonlyBytes { &command, sizeof(command) });

        // FIXME: This should actually be done asynchronously and allow other commands to be sent in the meantime
        //        possibly allowing handling multiple commands to be processed in parallel
        //  Note: Different transactions are distinguished by the tag field in the IU header
        // FIXME: I think we should do more error handling here in general?
        //        For example what if the command pipe is full?
        // Note:  The spec does say that there aren't any conditions resulting in a stall
        auto command_stage_error = m_command_pipe->submit_bulk_out_transfer(sizeof(command_iu), &command_iu);
        if (command_stage_error.is_error()) {
            dmesgln("UAS: Command stage error: {}", command_stage_error.error());
            return command_stage_error.release_error();
        }

        // FIXME: On USB3 this is done through streams instead, so we would immediately wait on the data stream
        size_t transfer_size = 0;
        if constexpr (Direction != SCSIDataDirection::NoData) {
            IU ready_response;
            auto ready_response_size_or_error = m_status_pipe->submit_bulk_in_transfer(sizeof(IU), &ready_response);
            if (ready_response_size_or_error.is_error()) {
                dmesgln("UAS: Ready response error: {}", ready_response_size_or_error.error());
                return ready_response_size_or_error.release_error();
            }

            auto ready_response_size = ready_response_size_or_error.release_value();
            if constexpr (Direction == SCSIDataDirection::DataToInitiator) {
                if (ready_response_size < sizeof(ReadReadyIU)) {
                    dmesgln("UAS: Response too short, expected at least {} bytes, got {}", sizeof(ReadReadyIU), ready_response_size);
                    return EIO;
                }
                if (ready_response.header.iu_id != IUID::ReadReady) {
                    dmesgln("UAS: Expected Read Ready IU, got {:02x}", static_cast<u8>(ready_response.header.iu_id));
                    return EIO;
                }
                if (ready_response.read_ready.header.tag != transfer_tag) {
                    // Note: Once we support multiple commands in parallel, we should not return an error here
                    //       but instead continue processing the responses and match them up with the commands
                    dmesgln("UAS: Response tag mismatch, expected {}, got {}", transfer_tag, ready_response.read_ready.header.tag);
                    return EIO;
                }

                // Note: The ReadReady command does not contain any useful data other than the tag

                auto transfer_error = m_in_pipe->submit_bulk_in_transfer(data_size, data);
                if (transfer_error.is_error()) {
                    dmesgln("UAS: Data transfer error: {}", transfer_error.error());
                    return transfer_error.release_error();
                }
                transfer_size = transfer_error.release_value();
            } else if constexpr (Direction == SCSIDataDirection::DataToTarget) {
                if (ready_response_size < sizeof(WriteReadyIU)) {
                    dmesgln("UAS: Response too short, expected at least {} bytes, got {}", sizeof(WriteReadyIU), ready_response_size);
                    return EIO;
                }
                if (ready_response.header.iu_id != IUID::WriteReady) {
                    dmesgln("UAS: Expected Write Ready IU, got {:02x}", static_cast<u8>(ready_response.header.iu_id));
                    return EIO;
                }
                if (ready_response.write_ready.header.tag != transfer_tag) {
                    // Note: Once we support multiple commands in parallel, we should not return an error here
                    //       but instead continue processing the responses and match them up with the commands
                    dmesgln("UAS: Response tag mismatch, expected {}, got {}", transfer_tag, ready_response.write_ready.header.tag);
                    return EIO;
                }

                // Note: The WriteReady command does not contain any useful data other than the tag

                auto transfer_error = m_out_pipe->submit_bulk_out_transfer(data_size, data);
                if (transfer_error.is_error()) {
                    dmesgln("UAS: Data transfer error: {}", transfer_error.error());
                    return transfer_error.release_error();
                }
                transfer_size = transfer_error.release_value();
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        IU sense;
        auto sense_size = TRY(m_status_pipe->submit_bulk_in_transfer(sizeof(IU), &sense));

        // FIXME: Should this check if this is a Sense IU and handle it accordingly?
        //        Or should we just return the sense data and let the caller handle it?
        //  Note: Unless the Queue is full we should always get a Sense IU, afaict
        //        In that case we would get a Response IU instead

        dbgln_if(USB_MASS_STORAGE_DEBUG, "UAS:   <- SIU: {:hex-dump}", ReadonlyBytes { &sense, sense_size });

        SendSCSICommandResult result;
        result.transfer_size = transfer_size;
        result.response_size = sense_size;
        memcpy(&result.response, &sense, sense_size);
        return result;
    }

private:
    UASInterface(USB::Device&, USBInterface const&, NonnullOwnPtr<BulkOutPipe> command_pipe, NonnullOwnPtr<BulkInPipe> status_pipe, NonnullOwnPtr<BulkInPipe> data_in_pipe, NonnullOwnPtr<BulkOutPipe> data_out_pipe);

    void add_storage_device(UASStorageDevice& storage_device) { m_storage_devices.append(storage_device); }

    UASStorageDevice::List m_storage_devices;

    USB::Device& m_device;
    USBInterface const& m_interface;
    NonnullOwnPtr<BulkOutPipe> m_command_pipe;
    NonnullOwnPtr<BulkInPipe> m_status_pipe;
    NonnullOwnPtr<BulkInPipe> m_in_pipe;
    NonnullOwnPtr<BulkOutPipe> m_out_pipe;
    u16 m_next_tag { 1 };

    IntrusiveListNode<UASInterface, NonnullLockRefPtr<UASInterface>> m_list_node;

public:
    using List = IntrusiveList<&UASInterface::m_list_node>;
};
}
