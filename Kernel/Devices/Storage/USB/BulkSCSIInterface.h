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

    ErrorOr<void> do_read(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);
    ErrorOr<void> do_write(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);

    IntrusiveListNode<BulkSCSIInterface, NonnullLockRefPtr<BulkSCSIInterface>> m_list_node;

public:
    using List = IntrusiveList<&BulkSCSIInterface::m_list_node>;
};

}
