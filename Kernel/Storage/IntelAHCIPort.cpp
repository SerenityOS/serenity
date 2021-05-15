/*
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#include <Kernel/Storage/ATA.h>
#include <Kernel/Storage/IntelAHCIPort.h>
#include <Kernel/Storage/SATADiskDevice.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

NonnullRefPtr<AHCIPort> IntelAHCIPort::create(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& registers, u32 port_index)
{
    return adopt_ref(*new IntelAHCIPort(handler, registers, port_index));
}

IntelAHCIPort::IntelAHCIPort(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& regs, u32 port_index)
    : AHCIPort(handler, regs, port_index)
{
    PCI::Address controller_address = handler.hba_controller()->pci_address();
    if(PCI::get_id(controller_address).device_id == 0x3b22) {
        m_has_reset_quirk = true;
    }
}

bool IntelAHCIPort::initialize(ScopedSpinLock<SpinLock<u8>>& main_lock)
{
    VERIFY(m_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "IntelAHCI Port {}: Initialization. Signature = 0x{:08x}", AHCIPort::representative_port_index(), static_cast<u32>(m_port_registers.sig));

    AHCIResetMode reset_mode = kernel_command_line().ahci_reset_mode();


    if(reset_mode == AHCIResetMode::ControllerOnly && m_has_reset_quirk) {
        dmesgln("IntelAHCI Port {}: Controller only reset may not work for this controller, forcing port reset", AHCIPort::representative_port_index());
        reset();
    }

    if (!AHCIPort::is_phy_enabled()) {
        dbgln_if(AHCI_DEBUG, "IntelAHCI Port {}: Bailing initialization, Phy is not enabled", AHCIPort::representative_port_index());
        return false;
    }
    AHCIPort::rebase();
    AHCIPort::power_on();
    AHCIPort::spin_up();
    AHCIPort::clear_sata_error_register();
    AHCIPort::start_fis_receiving();
    AHCIPort::set_active_state();
    m_interrupt_status.clear();
    m_interrupt_enable.set_all();

    full_memory_barrier();
    // This actually enables the port...
    AHCIPort::start_command_list_processing();
    full_memory_barrier();

    size_t logical_sector_size = 512;
    size_t physical_sector_size = 512;
    u64 max_addressable_sector = 0;
    if (identify_device(main_lock)) {
        auto identify_block = map_typed<ATAIdentifyBlock>(m_parent_handler->get_identify_metadata_physical_region(m_port_index));
        // Check if word 106 is valid before using it!
        if ((identify_block->physical_sector_size_to_logical_sector_size >> 14) == 1) {
            if (identify_block->physical_sector_size_to_logical_sector_size & (1 << 12)) {
                VERIFY(identify_block->logical_sector_size != 0);
                logical_sector_size = identify_block->logical_sector_size;
            }
            if (identify_block->physical_sector_size_to_logical_sector_size & (1 << 13)) {
                physical_sector_size = logical_sector_size << (identify_block->physical_sector_size_to_logical_sector_size & 0xf);
            }
        }
        // Check if the device supports LBA48 mode
        if (identify_block->commands_and_feature_sets_supported[1] & (1 << 10)) {
            max_addressable_sector = identify_block->user_addressable_logical_sectors_count;
        } else {
            max_addressable_sector = identify_block->max_28_bit_addressable_logical_sector;
        }
        if (AHCIPort::is_atapi_attached()) {
            m_port_registers.cmd = m_port_registers.cmd | (1 << 24);
        }

        dmesgln("IntelAHCI Port {}: Device found, Capacity={}, Bytes per logical sector={}, Bytes per physical sector={}", representative_port_index(), max_addressable_sector * logical_sector_size, logical_sector_size, physical_sector_size);

        // FIXME: We don't support ATAPI devices yet, so for now we don't "create" them
        if (!AHCIPort::is_atapi_attached()) {
            m_connected_device = SATADiskDevice::create(m_parent_handler->hba_controller(), *this, logical_sector_size, max_addressable_sector);
        } else {
            dbgln("IntelAHCI Port {}: Ignoring ATAPI devices for now as we don't currently support them.", representative_port_index());
        }
    }
    return true;
}

}
