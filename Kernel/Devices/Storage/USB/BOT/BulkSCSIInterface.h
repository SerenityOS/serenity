/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Devices/Storage/USB/BOT/BulkSCSIStorageDevice.h>
#include <Kernel/Devices/Storage/USB/SCSIInterface.h>

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

class BulkSCSIInterface : public AK::RefCounted<BulkSCSIInterface> {
    // https://www.usb.org/sites/default/files/usbmassbulk_10.pdf
public:
    static ErrorOr<NonnullLockRefPtr<BulkSCSIInterface>> initialize(USB::Device&, USBInterface const&, NonnullOwnPtr<BulkInPipe>, NonnullOwnPtr<BulkOutPipe>);

    ~BulkSCSIInterface();

    USB::Device const& device() const { return m_device; }

    ErrorOr<void> perform_reset_recovery();

    template<SCSIDataDirection Direction, typename Command, typename Data = nullptr_t>
    requires(IsNullPointer<Data>
        || IsPointer<Data>
        || (Direction == SCSIDataDirection::DataToInitiator && IsSame<Data, UserOrKernelBuffer>)
        || (Direction == SCSIDataDirection::DataToTarget && IsSameIgnoringCV<Data, UserOrKernelBuffer>))
    ErrorOr<CommandStatusWrapper> send_scsi_command(
        Command const& command,
        Data data = nullptr, size_t data_size = 0)
    {
        static_assert(sizeof(Command) >= 1);

        CommandBlockWrapper command_block {};
        command_block.transfer_length = data_size;
        if constexpr (Direction == SCSIDataDirection::DataToInitiator)
            command_block.direction = CBWDirection::DataIn;
        else
            command_block.direction = CBWDirection::DataOut;

        command_block.set_command(command);

        dbgln_if(USB_MASS_STORAGE_DEBUG, "send_scsi_command (opcode {:#x}):", *bit_cast<u8 const*>(&command));

        dbgln_if(USB_MASS_STORAGE_DEBUG, "  -> CBW: {:hex-dump}", ReadonlyBytes { &command_block, sizeof(command_block) });
        dbgln_if(USB_MASS_STORAGE_DEBUG, "     CDB: {:hex-dump}", ReadonlyBytes { &command, sizeof(command) });

        auto command_stage_result = m_out_pipe->submit_bulk_out_transfer(sizeof(command_block), &command_block);
        if (command_stage_result.is_error()) {
            auto error = command_stage_result.release_error();
            dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Command Error: {}]", error);
            if (error.code() == ESHUTDOWN) {
                // usbmassbulk 5.3.1/6.6.1
                TRY(perform_reset_recovery());
                return EIO;
            }

            return error;
        }
        dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Transferred: {} bytes]", command_stage_result.release_value());

        if constexpr (Direction == SCSIDataDirection::DataToInitiator) {
            static_assert(!IsNullPointer<Data>);
            VERIFY(data_size != 0);

            auto data_stage_result = m_in_pipe->submit_bulk_in_transfer(data_size, data);
            if (data_stage_result.is_error()) {
                auto error = data_stage_result.release_error();
                dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Data Error: {}]", error);
                if (error.code() == ESHUTDOWN) {
                    // usbmassbulk 6.7.2 "On a STALL condition receiving data [...]"
                    TRY(m_in_pipe->clear_halt());
                } else {
                    return error;
                }
            }

            if (!data_stage_result.is_error()) {
                if constexpr (IsPointer<Data>)
                    dbgln_if(USB_MASS_STORAGE_DEBUG, "  <- Data: {:hex-dump}", ReadonlyBytes { data, data_size });
                else
                    dbgln_if(USB_MASS_STORAGE_DEBUG, "  <- Data");
                dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Transferred: {} bytes]", data_stage_result.release_value());
            }
        } else if constexpr (Direction == SCSIDataDirection::DataToTarget) {
            static_assert(!IsNullPointer<Data>);
            VERIFY(data_size != 0);

            if constexpr (IsPointer<Data>)
                dbgln_if(USB_MASS_STORAGE_DEBUG, "  -> Data: {:hex-dump}", ReadonlyBytes { data, data_size });
            else
                dbgln_if(USB_MASS_STORAGE_DEBUG, "  -> Data");

            auto data_stage_result = m_out_pipe->submit_bulk_out_transfer(data_size, data);
            if (data_stage_result.is_error()) {
                auto error = data_stage_result.release_error();
                dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Data Error: {}]", error);
                if (error.code() == ESHUTDOWN) {
                    // usbmassbulk 6.7.3 "On a STALL condition sending data [...]"
                    TRY(m_out_pipe->clear_halt());
                } else {
                    return error;
                }
            }

            if (!data_stage_result.is_error())
                dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Transferred: {} bytes]", data_stage_result.release_value());
        } else {
            static_assert(IsNullPointer<Data>);
            VERIFY(data_size == 0);
        }

        CommandStatusWrapper status;
        auto status_stage_result = m_in_pipe->submit_bulk_in_transfer(sizeof(status), &status);
        if (status_stage_result.is_error()) {
            auto error = status_stage_result.release_error();
            dbgln_if(USB_MASS_STORAGE_DEBUG, "  [Status Error: {}]", error);
            if (error.code() == ESHUTDOWN) {
                // Sequence diagram in usbmassbulk 5.3 and 6.7.* "On a STALL condition when receiving the CSW [...]"
                auto clear_halt_result = m_in_pipe->clear_halt();
                if (clear_halt_result.is_error()) {
                    dbgln_if(USB_MASS_STORAGE_DEBUG, "  [Clear Halt Error: {}]", clear_halt_result.error());
                    return clear_halt_result.release_error();
                }

                status_stage_result = m_in_pipe->submit_bulk_in_transfer(sizeof(status), &status);
                if (status_stage_result.is_error()) {
                    dbgln_if(USB_MASS_STORAGE_DEBUG, "  [Status x2 Error: {}]", error);
                    auto error = status_stage_result.release_error();
                    if (error.code() == ESHUTDOWN) {
                        TRY(perform_reset_recovery());
                        return EIO;
                    }

                    return error;
                }
            } else {
                return error;
            }
        }
        dbgln_if(USB_MASS_STORAGE_DEBUG, "  <- CSW: {:hex-dump}", ReadonlyBytes { &status, sizeof(status) });
        dbgln_if(USB_MASS_STORAGE_DEBUG, "     signature: {:#x}, data_residue: {:#x}, status: {:#x}", (u32)status.signature, (u32)status.data_residue, to_underlying(status.status));
        dbgln_if(USB_MASS_STORAGE_DEBUG, "     [Transferred: {} bytes]", status_stage_result.release_value());

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

private:
    BulkSCSIInterface(USB::Device&, USBInterface const&, NonnullOwnPtr<BulkInPipe>, NonnullOwnPtr<BulkOutPipe>);

    void add_storage_device(NonnullRefPtr<BulkSCSIStorageDevice> storage_device) { m_storage_devices.append(storage_device); }

    BulkSCSIStorageDevice::List m_storage_devices;

    USB::Device& m_device;
    USBInterface const& m_interface;
    NonnullOwnPtr<BulkInPipe> m_in_pipe;
    NonnullOwnPtr<BulkOutPipe> m_out_pipe;

    IntrusiveListNode<BulkSCSIInterface, NonnullRefPtr<BulkSCSIInterface>> m_list_node;

public:
    using List = IntrusiveList<&BulkSCSIInterface::m_list_node>;
};

}
