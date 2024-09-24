/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Devices/Storage/StorageDevice.h>

namespace Kernel::USB {

enum class CBWDirection : u8 {
    DataOut = 0,
    DataIn = 1
};

struct CommandBlockWrapper {
    LittleEndian<u32> signature { 0x43425355 };
    LittleEndian<u32> tag { 0 };
    LittleEndian<u32> transfer_length { 0 };
    union {
        u8 flags { 0 };
        struct {
            u8 flag_reserved : 6;
            u8 flag_obsolete : 1;
            CBWDirection direction : 1;
        };
    };
    u8 lun { 0 };            // only 4 bits
    u8 command_length { 0 }; // 5 bits, range 1-16
    u8 command_block[16] { 0 };

    template<typename T>
    requires(sizeof(T) <= 16)
    void set_command(T const& command)
    {
        command_length = sizeof(command);
        memcpy(&command_block, &command, sizeof(command));
    }
};
static_assert(AssertSize<CommandBlockWrapper, 31>());

enum class CSWStatus : u8 {
    Passed = 0x00,
    Failed = 0x01,
    PhaseError = 0x02
};

struct CommandStatusWrapper {
    LittleEndian<u32> signature;
    LittleEndian<u32> tag;
    LittleEndian<u32> data_residue;
    CSWStatus status;
};
static_assert(AssertSize<CommandStatusWrapper, 13>());

enum class SCSIDataDirection {
    DataToTarget,
    DataToInitiator,
    NoData
};

template<SCSIDataDirection Direction, typename Command, typename CommandData = void>
static ErrorOr<CommandStatusWrapper> send_scsi_command(
    USB::BulkOutPipe& out_pipe, USB::BulkInPipe& in_pipe,
    Command const& command,
    Conditional<Direction == SCSIDataDirection::DataToInitiator, CommandData, CommandData const>* data = nullptr, size_t data_size = 0)
{
    CommandBlockWrapper command_block {};
    command_block.transfer_length = data_size;
    if constexpr (Direction == SCSIDataDirection::DataToInitiator)
        command_block.direction = CBWDirection::DataIn;
    else
        command_block.direction = CBWDirection::DataOut;

    command_block.set_command(command);

    TRY(out_pipe.submit_bulk_out_transfer(sizeof(command_block), &command_block));

    if constexpr (Direction == SCSIDataDirection::DataToInitiator) {
        TRY(in_pipe.submit_bulk_in_transfer(data_size, data));
    } else if constexpr (Direction == SCSIDataDirection::DataToTarget) {
        TRY(out_pipe.submit_bulk_out_transfer(data_size, data));
    } else {
        static_assert(IsSame<CommandData, void>);
        VERIFY(data_size == 0);
        VERIFY(data == nullptr);
    }

    CommandStatusWrapper status;
    TRY(in_pipe.submit_bulk_in_transfer(sizeof(status), &status));
    if (status.signature != 0x53425355) {
        dmesgln("SCSI: Command status signature mismatch, expected 0x53425355, got {:#x}", status.signature);
        return EIO;
    }

    if (status.tag != command_block.tag) {
        dmesgln("SCSI: Command tag mismatch, expected {}, got {}", command_block.tag, status.tag);
        return EIO;
    }

    return status;
}

template<SCSIDataDirection Direction, typename Command>
requires(Direction != SCSIDataDirection::NoData)
static ErrorOr<CommandStatusWrapper> send_scsi_command(
    USB::BulkOutPipe& out_pipe, USB::BulkInPipe& in_pipe,
    Command const& command,
    Conditional<Direction == SCSIDataDirection::DataToInitiator, UserOrKernelBuffer, UserOrKernelBuffer const> data, size_t data_size)
{
    CommandBlockWrapper command_block {};
    command_block.transfer_length = data_size;
    if constexpr (Direction == SCSIDataDirection::DataToInitiator)
        command_block.direction = CBWDirection::DataIn;
    else
        command_block.direction = CBWDirection::DataOut;

    command_block.set_command(command);

    TRY(out_pipe.submit_bulk_out_transfer(sizeof(command_block), &command_block));

    if constexpr (Direction == SCSIDataDirection::DataToInitiator) {
        TRY(in_pipe.submit_bulk_in_transfer(data_size, data));
    } else if constexpr (Direction == SCSIDataDirection::DataToTarget) {
        TRY(out_pipe.submit_bulk_out_transfer(data_size, data));
    }

    CommandStatusWrapper status;
    TRY(in_pipe.submit_bulk_in_transfer(sizeof(status), &status));
    if (status.signature != 0x53425355) {
        dmesgln("SCSI: Command status signature mismatch, expected 0x53425355, got {:#x}", status.signature);
        return EIO;
    }

    if (status.tag != command_block.tag) {
        dmesgln("SCSI: Command tag mismatch, expected {}, got {}", command_block.tag, status.tag);
        return EIO;
    }

    return status;
}

class BulkSCSIInterface : public StorageDevice {
    // https://www.usb.org/sites/default/files/usbmassbulk_10.pdf
public:
    BulkSCSIInterface(LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, size_t sector_size, u64 max_addressable_block, USB::Device& device, NonnullOwnPtr<BulkInPipe> in_pipe, NonnullOwnPtr<BulkOutPipe> out_pipe);

    USB::Device const& device() const { return m_device; }

    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual CommandSet command_set() const override { return CommandSet::SCSI; }

private:
    USB::Device& m_device;
    NonnullOwnPtr<BulkInPipe> m_in_pipe;
    NonnullOwnPtr<BulkOutPipe> m_out_pipe;

    Optional<u16> m_optimal_transfer_length;
    Optional<u32> m_optimal_transfer_length_granularity;
    Optional<u32> m_maximum_transfer_length;

    u32 optimal_block_count(u32 blocks);

    ErrorOr<void> do_read(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);
    ErrorOr<void> do_write(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);

    ErrorOr<void> query_characteristics();

    IntrusiveListNode<BulkSCSIInterface, NonnullLockRefPtr<BulkSCSIInterface>> m_list_node;

public:
    using List = IntrusiveList<&BulkSCSIInterface::m_list_node>;
};

}
