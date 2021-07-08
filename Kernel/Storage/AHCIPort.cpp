/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// For more information about locking in this code
// please look at Documentation/Kernel/AHCILocking.md

#include <AK/Atomic.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Storage/AHCIPort.h>
#include <Kernel/Storage/ATA.h>
#include <Kernel/Storage/SATADiskDevice.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/ScatterGatherList.h>
#include <Kernel/VM/TypedMapping.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

NonnullRefPtr<AHCIPort> AHCIPort::create(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& registers, u32 port_index)
{
    return adopt_ref(*new AHCIPort(handler, registers, port_index));
}

AHCIPort::AHCIPort(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& registers, u32 port_index)
    : m_port_index(port_index)
    , m_port_registers(registers)
    , m_parent_handler(handler)
    , m_interrupt_status((volatile u32&)m_port_registers.is)
    , m_interrupt_enable((volatile u32&)m_port_registers.ie)
    , m_io_work_queue(adopt_own_if_nonnull(new WorkQueue(String::formatted("AHCI Port #{} WorkQueue", representative_port_index()))).release_nonnull())
{
    if (is_interface_disabled()) {
        m_disabled_by_firmware = true;
        return;
    }

    m_command_list_page = MM.allocate_supervisor_physical_page();
    m_fis_receive_page = MM.allocate_supervisor_physical_page();
    if (m_command_list_page.is_null() || m_fis_receive_page.is_null())
        return;

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Command list page at {}", representative_port_index(), m_command_list_page->paddr());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: FIS receive page at {}", representative_port_index(), m_command_list_page->paddr());

    for (size_t index = 0; index < 1; index++) {
        m_dma_buffers.append(MM.allocate_supervisor_physical_page().release_nonnull());
    }
    for (size_t index = 0; index < 1; index++) {
        m_command_table_pages.append(MM.allocate_supervisor_physical_page().release_nonnull());
    }
    m_command_list_region = MM.allocate_kernel_region(m_command_list_page->paddr(), PAGE_SIZE, "AHCI Port Command List", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Command list region at {}", representative_port_index(), m_command_list_region->vaddr());
}

AHCIPort::~AHCIPort()
{
}

void AHCIPort::clear_sata_error_register() const
{
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Clearing SATA error register.", representative_port_index());
    m_port_registers.serr = m_port_registers.serr;
}

void AHCIPort::handle_interrupt()
{
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Interrupt handled, PxIS {}", representative_port_index(), m_interrupt_status.raw_value());
    if (m_interrupt_status.raw_value() == 0) {
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::PRC)) {
        clear_sata_error_register();
        m_wait_connect_for_completion = true;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::INF)) {
        // We need to defer the reset, because we can receive interrupts when
        // resetting the device.
        m_io_work_queue->queue([this]() {
            reset();
        });
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::IF) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::TFE) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::HBD) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::HBF)) {
        m_io_work_queue->queue([this]() {
            recover_from_fatal_error();
        });
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::DHR) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::PS)) {
        m_wait_for_completion = false;

        // Now schedule reading/writing the buffer as soon as we leave the irq handler.
        // This is important so that we can safely access the buffers, which could
        // trigger page faults
        if (!m_current_request) {
            dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request handled, probably identify request", representative_port_index());
        } else {
            m_io_work_queue->queue([this]() {
                dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request handled", representative_port_index());
                Locker locker(m_lock);
                VERIFY(m_current_request);
                VERIFY(m_current_scatter_list);
                if (m_current_request->request_type() == AsyncBlockDeviceRequest::Read) {
                    if (!m_current_request->write_to_buffer(m_current_request->buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * m_current_request->block_count())) {
                        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request failure, memory fault occurred when reading in data.", representative_port_index());
                        m_current_scatter_list = nullptr;
                        complete_current_request(AsyncDeviceRequest::MemoryFault);
                        return;
                    }
                }
                m_current_scatter_list = nullptr;
                dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request success", representative_port_index());
                complete_current_request(AsyncDeviceRequest::Success);
            });
        }
    }

    m_interrupt_status.clear();
}

bool AHCIPort::is_interrupts_enabled() const
{
    return !m_interrupt_enable.is_cleared();
}

void AHCIPort::recover_from_fatal_error()
{
    Locker locker(m_lock);
    ScopedSpinLock lock(m_hard_lock);
    dmesgln("{}: AHCI Port {} fatal error, shutting down!", m_parent_handler->hba_controller()->pci_address(), representative_port_index());
    dmesgln("{}: AHCI Port {} fatal error, SError {}", m_parent_handler->hba_controller()->pci_address(), representative_port_index(), (u32)m_port_registers.serr);
    stop_command_list_processing();
    stop_fis_receiving();
    m_interrupt_enable.clear();
}

void AHCIPort::eject()
{
    // FIXME: This operation (meant to be used on optical drives) doesn't work yet when I tested it on real hardware
    TODO();

    VERIFY(m_lock.is_locked());
    VERIFY(is_atapi_attached());
    VERIFY(is_operable());
    clear_sata_error_register();

    if (!spin_until_ready())
        return;

    auto unused_command_header = try_to_find_unused_command_header();
    VERIFY(unused_command_header.has_value());
    auto* command_list_entries = (volatile AHCI::CommandHeader*)m_command_list_region->vaddr().as_ptr();
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()].paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = 0;
    command_list_entries[unused_command_header.value()].prdtl = 0;

    // Note: we must set the correct Dword count in this register. Real hardware
    // AHCI controllers do care about this field! QEMU doesn't care if we don't
    // set the correct CFL field in this register, real hardware will set an
    // handshake error bit in PxSERR register if CFL is incorrect.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P | AHCI::CommandHeaderAttributes::C | AHCI::CommandHeaderAttributes::A;

    auto command_table_region = MM.allocate_kernel_region(m_command_table_pages[unused_command_header.value()].paddr().page_base(), page_round_up(sizeof(AHCI::CommandTable)), "AHCI Command Table", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    auto& command_table = *(volatile AHCI::CommandTable*)command_table_region->vaddr().as_ptr();
    memset(const_cast<u8*>(command_table.command_fis), 0, 64);
    auto& fis = *(volatile FIS::HostToDevice::Register*)command_table.command_fis;
    fis.header.fis_type = (u8)FIS::Type::RegisterHostToDevice;
    fis.command = ATA_CMD_PACKET;

    full_memory_barrier();
    memset(const_cast<u8*>(command_table.atapi_command), 0, 32);

    full_memory_barrier();
    command_table.atapi_command[0] = ATAPI_CMD_EJECT;
    command_table.atapi_command[1] = 0;
    command_table.atapi_command[2] = 0;
    command_table.atapi_command[3] = 0;
    command_table.atapi_command[4] = 0b10;
    command_table.atapi_command[5] = 0;
    command_table.atapi_command[6] = 0;
    command_table.atapi_command[7] = 0;
    command_table.atapi_command[8] = 0;
    command_table.atapi_command[9] = 0;
    command_table.atapi_command[10] = 0;
    command_table.atapi_command[11] = 0;
    fis.device = 0;
    fis.header.port_muliplier = fis.header.port_muliplier | (u8)FIS::HeaderAttributes::C;

    // The below loop waits until the port is no longer busy before issuing a new command
    if (!spin_until_ready())
        return;

    full_memory_barrier();
    mark_command_header_ready_to_process(unused_command_header.value());

    full_memory_barrier();

    while (1) {
        if (m_port_registers.serr != 0) {
            dbgln_if(AHCI_DEBUG, "AHCI Port {}: Eject Drive failed, SError 0x{:08x}", representative_port_index(), (u32)m_port_registers.serr);
            try_disambiguate_sata_error();
            VERIFY_NOT_REACHED();
        }
    }
    dbgln("AHCI Port {}: Eject Drive", representative_port_index());
    return;
}

bool AHCIPort::reset()
{
    Locker locker(m_lock);
    ScopedSpinLock lock(m_hard_lock);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Resetting", representative_port_index());

    if (m_disabled_by_firmware) {
        dmesgln("AHCI Port {}: Disabled by firmware ", representative_port_index());
        return false;
    }
    full_memory_barrier();
    m_interrupt_enable.clear();
    m_interrupt_status.clear();
    full_memory_barrier();
    start_fis_receiving();
    full_memory_barrier();
    clear_sata_error_register();
    full_memory_barrier();
    if (!initiate_sata_reset(lock)) {
        return false;
    }
    return initialize(lock);
}

bool AHCIPort::initialize_without_reset()
{
    Locker locker(m_lock);
    ScopedSpinLock lock(m_hard_lock);
    dmesgln("AHCI Port {}: {}", representative_port_index(), try_disambiguate_sata_status());
    return initialize(lock);
}

bool AHCIPort::initialize(ScopedSpinLock<SpinLock<u8>>& main_lock)
{
    VERIFY(m_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Initialization. Signature = 0x{:08x}", representative_port_index(), static_cast<u32>(m_port_registers.sig));
    if (!is_phy_enabled()) {
        // Note: If PHY is not enabled, just clear the interrupt status and enable interrupts, in case
        // we are going to hotplug a device later.
        m_interrupt_status.clear();
        m_interrupt_enable.set_all();
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Bailing initialization, Phy is not enabled.", representative_port_index());
        return false;
    }
    rebase();
    power_on();
    spin_up();
    clear_sata_error_register();
    start_fis_receiving();
    set_active_state();
    m_interrupt_status.clear();
    m_interrupt_enable.set_all();

    full_memory_barrier();
    // This actually enables the port...
    start_command_list_processing();
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
        if (is_atapi_attached()) {
            m_port_registers.cmd = m_port_registers.cmd | (1 << 24);
        }

        dmesgln("AHCI Port {}: Device found, Capacity={}, Bytes per logical sector={}, Bytes per physical sector={}", representative_port_index(), max_addressable_sector * logical_sector_size, logical_sector_size, physical_sector_size);

        // FIXME: We don't support ATAPI devices yet, so for now we don't "create" them
        if (!is_atapi_attached()) {
            m_connected_device = SATADiskDevice::create(m_parent_handler->hba_controller(), *this, logical_sector_size, max_addressable_sector);
        } else {
            dbgln("AHCI Port {}: Ignoring ATAPI devices for now as we don't currently support them.", representative_port_index());
        }
    }
    return true;
}

const char* AHCIPort::try_disambiguate_sata_status()
{
    switch (m_port_registers.ssts & 0xf) {
    case 0:
        return "Device not detected, Phy not enabled";
    case 1:
        return "Device detected, Phy disabled";
    case 3:
        return "Device detected, Phy enabled";
    case 4:
        return "interface disabled";
    }
    VERIFY_NOT_REACHED();
}

void AHCIPort::try_disambiguate_sata_error()
{
    dmesgln("AHCI Port {}: SErr breakdown:", representative_port_index());
    dmesgln("AHCI Port {}: Diagnostics:", representative_port_index());

    constexpr u32 diagnostics_bitfield = 0xFFFF0000;
    if ((m_port_registers.serr & diagnostics_bitfield) > 0) {
        if (m_port_registers.serr & AHCI::SErr::DIAG_X)
            dmesgln("AHCI Port {}: - Exchanged", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_F)
            dmesgln("AHCI Port {}: - Unknown FIS Type", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_T)
            dmesgln("AHCI Port {}: - Transport state transition error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_S)
            dmesgln("AHCI Port {}: - Link sequence error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_H)
            dmesgln("AHCI Port {}: - Handshake error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_C)
            dmesgln("AHCI Port {}: - CRC error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_D)
            dmesgln("AHCI Port {}: - Disparity error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_B)
            dmesgln("AHCI Port {}: - 10B to 8B decode error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_W)
            dmesgln("AHCI Port {}: - Comm Wake", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_I)
            dmesgln("AHCI Port {}: - Phy Internal Error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::DIAG_N)
            dmesgln("AHCI Port {}: - PhyRdy Change", representative_port_index());
    } else {
        dmesgln("AHCI Port {}: - No diagnostic information provided.", representative_port_index());
    }

    dmesgln("AHCI Port {}: Error(s):", representative_port_index());

    constexpr u32 error_bitfield = 0xFFFF;
    if ((m_port_registers.serr & error_bitfield) > 0) {
        if (m_port_registers.serr & AHCI::SErr::ERR_E)
            dmesgln("AHCI Port {}: - Internal error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::ERR_P)
            dmesgln("AHCI Port {}: - Protocol error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::ERR_C)
            dmesgln("AHCI Port {}: - Persistent communication or data integrity error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::ERR_T)
            dmesgln("AHCI Port {}: - Transient data integrity error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::ERR_M)
            dmesgln("AHCI Port {}: - Recovered communications error", representative_port_index());
        if (m_port_registers.serr & AHCI::SErr::ERR_I)
            dmesgln("AHCI Port {}: - Recovered data integrity error", representative_port_index());
    } else {
        dmesgln("AHCI Port {}: - No error information provided.", representative_port_index());
    }
}

void AHCIPort::rebase()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    VERIFY(!m_command_list_page.is_null() && !m_fis_receive_page.is_null());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Rebasing.", representative_port_index());
    full_memory_barrier();
    stop_command_list_processing();
    stop_fis_receiving();
    full_memory_barrier();
    size_t retry = 0;
    // Try to wait 1 second for HBA to clear Command List Running and FIS Receive Running
    while (retry < 1000) {
        if (!(m_port_registers.cmd & (1 << 15)) && !(m_port_registers.cmd & (1 << 14)))
            break;
        IO::delay(1000);
        retry++;
    }
    full_memory_barrier();
    m_port_registers.clbu = 0;
    m_port_registers.clb = m_command_list_page->paddr().get();
    m_port_registers.fbu = 0;
    m_port_registers.fb = m_fis_receive_page->paddr().get();
}

bool AHCIPort::is_operable() const
{
    // Note: The definition of "operable" is somewhat ambiguous, but we determine it
    // by 3 parameters as shown below.
    return (!m_command_list_page.is_null())
        && (!m_fis_receive_page.is_null())
        && ((m_port_registers.cmd & (1 << 14)) != 0);
}

void AHCIPort::set_active_state() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Switching to active state.", representative_port_index());
    m_port_registers.cmd = (m_port_registers.cmd & 0x0ffffff) | (1 << 28);
}

void AHCIPort::set_sleep_state() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    m_port_registers.cmd = (m_port_registers.cmd & 0x0ffffff) | (0b1000 << 28);
}

size_t AHCIPort::calculate_descriptors_count(size_t block_count) const
{
    VERIFY(m_connected_device);
    size_t needed_dma_regions_count = page_round_up((block_count * m_connected_device->block_size())) / PAGE_SIZE;
    VERIFY(needed_dma_regions_count <= m_dma_buffers.size());
    return needed_dma_regions_count;
}

Optional<AsyncDeviceRequest::RequestResult> AHCIPort::prepare_and_set_scatter_list(AsyncBlockDeviceRequest& request)
{
    VERIFY(m_lock.is_locked());
    VERIFY(request.block_count() > 0);

    NonnullRefPtrVector<PhysicalPage> allocated_dma_regions;
    for (size_t index = 0; index < calculate_descriptors_count(request.block_count()); index++) {
        allocated_dma_regions.append(m_dma_buffers.at(index));
    }

    m_current_scatter_list = ScatterGatherList::create(request, move(allocated_dma_regions), m_connected_device->block_size());
    if (!m_current_scatter_list)
        return AsyncDeviceRequest::Failure;
    if (request.request_type() == AsyncBlockDeviceRequest::Write) {
        if (!request.read_from_buffer(request.buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * request.block_count())) {
            return AsyncDeviceRequest::MemoryFault;
        }
    }
    return {};
}

void AHCIPort::start_request(AsyncBlockDeviceRequest& request)
{
    Locker locker(m_lock);
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request start", representative_port_index());
    VERIFY(!m_current_request);
    VERIFY(!m_current_scatter_list);

    m_current_request = request;

    auto result = prepare_and_set_scatter_list(request);
    if (result.has_value()) {
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request failure.", representative_port_index());
        locker.unlock();
        complete_current_request(result.value());
        return;
    }

    auto success = access_device(request.request_type(), request.block_index(), request.block_count());
    if (!success) {
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request failure.", representative_port_index());
        locker.unlock();
        complete_current_request(AsyncDeviceRequest::Failure);
        return;
    }
}

void AHCIPort::complete_current_request(AsyncDeviceRequest::RequestResult result)
{
    VERIFY(m_current_request);
    auto current_request = m_current_request;
    m_current_request.clear();
    current_request->complete(result);
}

bool AHCIPort::spin_until_ready() const
{
    VERIFY(m_lock.is_locked());
    size_t spin = 0;
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Spinning until ready.", representative_port_index());
    while ((m_port_registers.tfd & (ATA_SR_BSY | ATA_SR_DRQ)) && spin <= 100) {
        IO::delay(1000);
        spin++;
    }
    if (spin == 100) {
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: SPIN exceeded 100 milliseconds threshold", representative_port_index());
        return false;
    }
    return true;
}

bool AHCIPort::access_device(AsyncBlockDeviceRequest::RequestType direction, u64 lba, u8 block_count)
{
    VERIFY(m_connected_device);
    VERIFY(is_operable());
    VERIFY(m_lock.is_locked());
    VERIFY(m_current_scatter_list);
    ScopedSpinLock lock(m_hard_lock);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {}", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count);
    if (!spin_until_ready())
        return false;

    auto unused_command_header = try_to_find_unused_command_header();
    VERIFY(unused_command_header.has_value());
    auto* command_list_entries = (volatile AHCI::CommandHeader*)m_command_list_region->vaddr().as_ptr();
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()].paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = 0;
    command_list_entries[unused_command_header.value()].prdtl = m_current_scatter_list->scatters_count();

    // Note: we must set the correct Dword count in this register. Real hardware
    // AHCI controllers do care about this field! QEMU doesn't care if we don't
    // set the correct CFL field in this register, real hardware will set an
    // handshake error bit in PxSERR register if CFL is incorrect.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P | (is_atapi_attached() ? AHCI::CommandHeaderAttributes::A : 0) | (direction == AsyncBlockDeviceRequest::RequestType::Write ? AHCI::CommandHeaderAttributes::W : 0);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: CLE: ctba=0x{:08x}, ctbau=0x{:08x}, prdbc=0x{:08x}, prdtl=0x{:04x}, attributes=0x{:04x}", representative_port_index(), (u32)command_list_entries[unused_command_header.value()].ctba, (u32)command_list_entries[unused_command_header.value()].ctbau, (u32)command_list_entries[unused_command_header.value()].prdbc, (u16)command_list_entries[unused_command_header.value()].prdtl, (u16)command_list_entries[unused_command_header.value()].attributes);

    auto command_table_region = MM.allocate_kernel_region(m_command_table_pages[unused_command_header.value()].paddr().page_base(), page_round_up(sizeof(AHCI::CommandTable)), "AHCI Command Table", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    auto& command_table = *(volatile AHCI::CommandTable*)command_table_region->vaddr().as_ptr();

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Allocated command table at {}", representative_port_index(), command_table_region->vaddr());

    memset(const_cast<u8*>(command_table.command_fis), 0, 64);

    size_t scatter_entry_index = 0;
    size_t data_transfer_count = (block_count * m_connected_device->block_size());
    for (auto scatter_page : m_current_scatter_list->vmobject().physical_pages()) {
        VERIFY(data_transfer_count != 0);
        VERIFY(scatter_page);
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Add a transfer scatter entry @ {}", representative_port_index(), scatter_page->paddr());
        command_table.descriptors[scatter_entry_index].base_high = 0;
        command_table.descriptors[scatter_entry_index].base_low = scatter_page->paddr().get();
        if (data_transfer_count <= PAGE_SIZE) {
            command_table.descriptors[scatter_entry_index].byte_count = data_transfer_count - 1;
            data_transfer_count = 0;
        } else {
            command_table.descriptors[scatter_entry_index].byte_count = PAGE_SIZE - 1;
            data_transfer_count -= PAGE_SIZE;
        }
        scatter_entry_index++;
    }
    command_table.descriptors[scatter_entry_index].byte_count = (PAGE_SIZE - 1) | (1 << 31);

    memset(const_cast<u8*>(command_table.atapi_command), 0, 32);

    auto& fis = *(volatile FIS::HostToDevice::Register*)command_table.command_fis;
    fis.header.fis_type = (u8)FIS::Type::RegisterHostToDevice;
    if (is_atapi_attached()) {
        fis.command = ATA_CMD_PACKET;
        TODO();
    } else {
        if (direction == AsyncBlockDeviceRequest::RequestType::Write)
            fis.command = ATA_CMD_WRITE_DMA_EXT;
        else
            fis.command = ATA_CMD_READ_DMA_EXT;
    }

    full_memory_barrier();

    fis.device = ATA_USE_LBA_ADDRESSING;
    fis.header.port_muliplier = (u8)FIS::HeaderAttributes::C;

    fis.lba_high[0] = (lba >> 24) & 0xff;
    fis.lba_high[1] = (lba >> 32) & 0xff;
    fis.lba_high[2] = (lba >> 40) & 0xff;
    fis.lba_low[0] = lba & 0xff;
    fis.lba_low[1] = (lba >> 8) & 0xff;
    fis.lba_low[2] = (lba >> 16) & 0xff;
    fis.count = (block_count);

    // The below loop waits until the port is no longer busy before issuing a new command
    if (!spin_until_ready())
        return false;

    full_memory_barrier();
    mark_command_header_ready_to_process(unused_command_header.value());
    full_memory_barrier();

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {} @ {}, ended", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count, m_dma_buffers[0].paddr());
    return true;
}

bool AHCIPort::identify_device(ScopedSpinLock<SpinLock<u8>>& main_lock)
{
    VERIFY(m_lock.is_locked());
    VERIFY(is_operable());
    if (!spin_until_ready())
        return false;

    auto unused_command_header = try_to_find_unused_command_header();
    VERIFY(unused_command_header.has_value());
    auto* command_list_entries = (volatile AHCI::CommandHeader*)m_command_list_region->vaddr().as_ptr();
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()].paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = 512;
    command_list_entries[unused_command_header.value()].prdtl = 1;

    // Note: we must set the correct Dword count in this register. Real hardware AHCI controllers do care about this field!
    // QEMU doesn't care if we don't set the correct CFL field in this register, real hardware will set an handshake error bit in PxSERR register.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P;

    auto command_table_region = MM.allocate_kernel_region(m_command_table_pages[unused_command_header.value()].paddr().page_base(), page_round_up(sizeof(AHCI::CommandTable)), "AHCI Command Table", Region::Access::Read | Region::Access::Write);
    auto& command_table = *(volatile AHCI::CommandTable*)command_table_region->vaddr().as_ptr();
    memset(const_cast<u8*>(command_table.command_fis), 0, 64);
    command_table.descriptors[0].base_high = 0;
    command_table.descriptors[0].base_low = m_parent_handler->get_identify_metadata_physical_region(m_port_index).get();
    command_table.descriptors[0].byte_count = 512 - 1;
    auto& fis = *(volatile FIS::HostToDevice::Register*)command_table.command_fis;
    fis.header.fis_type = (u8)FIS::Type::RegisterHostToDevice;
    fis.command = m_port_registers.sig == AHCI::DeviceSignature::ATAPI ? ATA_CMD_IDENTIFY_PACKET : ATA_CMD_IDENTIFY;
    fis.device = 0;
    fis.header.port_muliplier = fis.header.port_muliplier | (u8)FIS::HeaderAttributes::C;

    // The below loop waits until the port is no longer busy before issuing a new command
    if (!spin_until_ready())
        return false;

    // FIXME: Find a better way to send IDENTIFY DEVICE and getting an interrupt!
    {
        main_lock.unlock();
        VERIFY_INTERRUPTS_ENABLED();
        full_memory_barrier();
        m_wait_for_completion = true;
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Marking command header at index {} as ready to identify device", representative_port_index(), unused_command_header.value());
        m_port_registers.ci = 1 << unused_command_header.value();
        full_memory_barrier();

        while (1) {
            if (m_port_registers.serr != 0) {
                dbgln("AHCI Port {}: Identify failed, SError 0x{:08x}", representative_port_index(), (u32)m_port_registers.serr);
                try_disambiguate_sata_error();
                return false;
            }
            if (!m_wait_for_completion)
                break;
        }
        main_lock.lock();
    }

    return true;
}

bool AHCIPort::shutdown()
{
    Locker locker(m_lock);
    ScopedSpinLock lock(m_hard_lock);
    rebase();
    set_interface_state(AHCI::DeviceDetectionInitialization::DisableInterface);
    return true;
}

Optional<u8> AHCIPort::try_to_find_unused_command_header()
{
    VERIFY(m_lock.is_locked());
    u32 commands_issued = m_port_registers.ci;
    for (size_t index = 0; index < 32; index++) {
        if (!(commands_issued & 1)) {
            dbgln_if(AHCI_DEBUG, "AHCI Port {}: unused command header at index {}", representative_port_index(), index);
            return index;
        }
        commands_issued >>= 1;
    }
    return {};
}

void AHCIPort::start_command_list_processing() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    VERIFY(is_operable());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Starting command list processing.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd | 1;
}

void AHCIPort::mark_command_header_ready_to_process(u8 command_header_index) const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    VERIFY(is_operable());
    VERIFY(!m_wait_for_completion);
    m_wait_for_completion = true;
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Marking command header at index {} as ready to process.", representative_port_index(), command_header_index);
    m_port_registers.ci = 1 << command_header_index;
}

void AHCIPort::stop_command_list_processing() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Stopping command list processing.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd & 0xfffffffe;
}

void AHCIPort::start_fis_receiving() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Starting FIS receiving.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd | (1 << 4);
}

void AHCIPort::power_on() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Power on. Cold presence detection? {}", representative_port_index(), (bool)(m_port_registers.cmd & (1 << 20)));
    if (!(m_port_registers.cmd & (1 << 20)))
        return;
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Powering on device.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd | (1 << 2);
}

void AHCIPort::spin_up() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Spin up. Staggered spin up? {}", representative_port_index(), m_parent_handler->hba_capabilities().staggered_spin_up_supported);
    if (!m_parent_handler->hba_capabilities().staggered_spin_up_supported)
        return;
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Spinning up device.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd | (1 << 1);
}

void AHCIPort::stop_fis_receiving() const
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Stopping FIS receiving.", representative_port_index());
    m_port_registers.cmd = m_port_registers.cmd & 0xFFFFFFEF;
}

bool AHCIPort::initiate_sata_reset(ScopedSpinLock<SpinLock<u8>>& main_lock)
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Initiate SATA reset", representative_port_index());
    stop_command_list_processing();
    full_memory_barrier();
    size_t retry = 0;
    // Try to wait 1 second for HBA to clear Command List Running
    while (retry < 5000) {
        if (!(m_port_registers.cmd & (1 << 15)))
            break;
        // The AHCI specification says to wait now a 500 milliseconds
        IO::delay(100);
        retry++;
    }
    full_memory_barrier();
    spin_up();
    full_memory_barrier();
    set_interface_state(AHCI::DeviceDetectionInitialization::PerformInterfaceInitializationSequence);
    // The AHCI specification says to wait now a 1 millisecond
    IO::delay(1000);
    // FIXME: Find a better way to opt-out temporarily from Scoped locking!
    {
        main_lock.unlock();
        VERIFY_INTERRUPTS_ENABLED();
        full_memory_barrier();
        set_interface_state(AHCI::DeviceDetectionInitialization::NoActionRequested);
        full_memory_barrier();
        if (m_wait_connect_for_completion) {
            retry = 0;
            while (retry < 100000) {
                if (is_phy_enabled())
                    break;

                IO::delay(10);
                retry++;
            }
        }
        main_lock.lock();
    }

    dmesgln("AHCI Port {}: {}", representative_port_index(), try_disambiguate_sata_status());

    full_memory_barrier();
    clear_sata_error_register();
    return (m_port_registers.ssts & 0xf) == 3;
}

void AHCIPort::set_interface_state(AHCI::DeviceDetectionInitialization requested_action)
{
    switch (requested_action) {
    case AHCI::DeviceDetectionInitialization::NoActionRequested:
        m_port_registers.sctl = (m_port_registers.sctl & 0xfffffff0);
        return;
    case AHCI::DeviceDetectionInitialization::PerformInterfaceInitializationSequence:
        m_port_registers.sctl = (m_port_registers.sctl & 0xfffffff0) | 1;
        return;
    case AHCI::DeviceDetectionInitialization::DisableInterface:
        m_port_registers.sctl = (m_port_registers.sctl & 0xfffffff0) | 4;
        return;
    }
    VERIFY_NOT_REACHED();
}

}
