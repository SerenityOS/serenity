/*
 * Copyright (c) 2024-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/I2C/Controller/OpenCoresI2CController.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::I2C {

// FIXME: Handle bus arbitration to support systems with multiple I²C controllers connected to the same bus.

ErrorOr<NonnullOwnPtr<OpenCoresI2CController>> OpenCoresI2CController::create(DeviceTree::Device const& device)
{
    u32 register_shift = device.node().get_u32_property("reg-shift"sv).value_or(0);

    auto maybe_register_io_width = device.node().get_property("reg-io-width"sv);
    if (!maybe_register_io_width.has_value())
        return EINVAL;
    u32 register_io_width = maybe_register_io_width.value().as<u32>();

    if (register_io_width != 1 && register_io_width != 2 && register_io_width != 4)
        return EINVAL;

    auto registers_resource = TRY(device.get_resource(0));

    auto register_region_size = TRY(Memory::page_round_up(registers_resource.paddr.offset_in_page() + REGISTER_COUNT * (1 << register_shift)));
    auto register_region = TRY(MM.allocate_mmio_kernel_region(registers_resource.paddr.page_base(), register_region_size, {}, Memory::Region::Access::ReadWrite));
    auto register_address = register_region->vaddr().offset(registers_resource.paddr.offset_in_page());

    // Check that register accesses won't cause unaligned accesses.
    if (register_address.get() % register_io_width != 0)
        return EINVAL;

    return adopt_nonnull_own_or_enomem(new (nothrow) OpenCoresI2CController(move(register_region), register_address, register_shift, register_io_width));
}

OpenCoresI2CController::OpenCoresI2CController(NonnullOwnPtr<Memory::Region> register_region, VirtualAddress register_address, u32 register_shift, u32 register_io_width)
    : m_register_region(move(register_region))
    , m_register_address(register_address)
    , m_register_shift(register_shift)
    , m_register_io_width(register_io_width)
{
    // Set the core to be enabled and interrupts to be disabled.
    write_reg(RegisterOffset::Control, to_underlying(ControlRegisterFlags::Enable));

    // FIXME: Configure the prescale register based on the IP clock and wanted bus clock from the devicetree.
}

ErrorOr<void> OpenCoresI2CController::do_transfers(Span<Transfer> transfers)
{
    if (transfers.is_empty())
        return EINVAL;

    auto check_target_ack = [this] -> ErrorOr<void> {
        if ((read_reg(RegisterOffset::Status) & to_underlying(StatusRegisterFlags::NoACKReceived)) != 0) {
            write_reg(RegisterOffset::Command, to_underlying(CommandRegisterFlags::GenerateStopCondition));
            return EIO;
        }

        return {};
    };

    // FIXME: Don't busy wait. Instead, use interrupts and make transfers asynchronous.
    auto wait_until_transfer_is_done = [this] {
        while ((read_reg(RegisterOffset::Status) & to_underlying(StatusRegisterFlags::TransferInProgress)) != 0) {
            Processor::pause();
        }
    };

    for (size_t transfer_index = 0; transfer_index < transfers.size(); transfer_index++) {
        TRY(transfers[transfer_index].visit(
            [&](ReadTransfer& transfer) -> ErrorOr<void> {
                // FIXME: Support 10-bit addresses.
                if (transfer.target_address > 0x7f)
                    return ENOTIMPL;

                // Send the address.
                write_reg(RegisterOffset::Transmit, seven_bit_address_byte(transfer.target_address, DataDirection::Read));
                write_reg(RegisterOffset::Command, to_underlying(CommandRegisterFlags::WriteToSlave | CommandRegisterFlags::GenerateStartCondition));

                for (size_t byte_index = 0; byte_index < transfer.data_read.size(); byte_index++) {
                    wait_until_transfer_is_done();
                    TRY(check_target_ack());

                    bool is_last_byte_of_this_transfer = (byte_index == transfer.data_read.size() - 1);
                    bool is_last_byte_to_be_transferred = is_last_byte_of_this_transfer && (transfer_index == transfers.size() - 1);

                    auto command_reg_flags = CommandRegisterFlags::ReadFromSlave;
                    if (is_last_byte_of_this_transfer)
                        command_reg_flags |= CommandRegisterFlags::NACK;
                    if (is_last_byte_to_be_transferred)
                        command_reg_flags |= CommandRegisterFlags::GenerateStopCondition;

                    write_reg(RegisterOffset::Command, to_underlying(command_reg_flags));

                    transfer.data_read[byte_index] = read_reg(RegisterOffset::Receive);
                }

                wait_until_transfer_is_done();
                TRY(check_target_ack());

                return {};
            },
            [&](WriteTransfer const& transfer) -> ErrorOr<void> {
                // FIXME: Support 10-bit addresses.
                if (transfer.target_address > 0x7f)
                    return ENOTIMPL;

                // Send the address.
                write_reg(RegisterOffset::Transmit, seven_bit_address_byte(transfer.target_address, DataDirection::Write));
                write_reg(RegisterOffset::Command, to_underlying(CommandRegisterFlags::WriteToSlave | CommandRegisterFlags::GenerateStartCondition));

                for (size_t byte_index = 0; byte_index < transfer.data_to_write.size(); byte_index++) {
                    wait_until_transfer_is_done();
                    TRY(check_target_ack());

                    write_reg(RegisterOffset::Transmit, transfer.data_to_write[byte_index]);

                    bool is_last_byte_of_this_transfer = (byte_index == transfer.data_to_write.size() - 1);
                    bool is_last_byte_to_be_transferred = is_last_byte_of_this_transfer && (transfer_index == transfers.size() - 1);

                    auto command_reg_flags = CommandRegisterFlags::WriteToSlave;
                    if (is_last_byte_to_be_transferred)
                        command_reg_flags |= CommandRegisterFlags::GenerateStopCondition;

                    write_reg(RegisterOffset::Command, to_underlying(command_reg_flags));
                }

                wait_until_transfer_is_done();
                TRY(check_target_ack());

                return {};
            }));
    }

    return {};
}

static constinit Array const compatibles_array = {
    "opencores,i2c-ocores"sv,
};

DEVICETREE_DRIVER(OpenCoresI2CControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/i2c/opencores,i2c-ocores.yaml
ErrorOr<void> OpenCoresI2CControllerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto* i2c_controller = TRY(OpenCoresI2CController::create(device)).leak_ptr();

    TRY(DeviceTree::Management::the().register_i2c_controller(device, *i2c_controller));

    auto res = DeviceTree::Management::the().scan_node_for_devices(device.node(), DeviceTree::ShouldProbeImmediately::Yes);
    if (res.is_error())
        dbgln("{}: Failed to probe child nodes: {}", device.node_name(), res.release_error());

    return {};
}

}
