/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Atomic.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Storage/AHCIPort.h>
#include <Kernel/Storage/ATA.h>
#include <Kernel/Storage/SATADiskDevice.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

NonnullRefPtr<AHCIPort::ScatterList> AHCIPort::ScatterList::create(AsyncBlockDeviceRequest& request, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size)
{
    return adopt(*new ScatterList(request, allocated_pages, device_block_size));
}

AHCIPort::ScatterList::ScatterList(AsyncBlockDeviceRequest& request, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size)
    : m_vm_object(AnonymousVMObject::create_with_physical_pages(allocated_pages))
    , m_device_block_size(device_block_size)
{
    m_dma_region = MM.allocate_kernel_region_with_vmobject(m_vm_object, page_round_up((request.block_count() * device_block_size)), "AHCI Scattered DMA", Region::Access::Read | Region::Access::Write, Region::Cacheable::Yes);
}

NonnullRefPtr<AHCIPort> AHCIPort::create(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& registers, u32 port_index)
{
    return adopt(*new AHCIPort(handler, registers, port_index));
}

AHCIPort::AHCIPort(const AHCIPortHandler& handler, volatile AHCI::PortRegisters& registers, u32 port_index)
    : m_port_index(port_index)
    , m_port_registers(registers)
    , m_parent_handler(handler)
    , m_interrupt_status((volatile u32&)m_port_registers.is)
    , m_interrupt_enable((volatile u32&)m_port_registers.ie)
{
    if (is_interface_disabled()) {
        m_disabled_by_firmware = true;
        return;
    }

    m_command_list_page = MM.allocate_supervisor_physical_page();
    m_fis_receive_page = MM.allocate_supervisor_physical_page();
    if (m_command_list_page.is_null() || m_fis_receive_page.is_null())
        return;
    for (size_t index = 0; index < 1; index++) {
        m_dma_buffers.append(MM.allocate_supervisor_physical_page().release_nonnull());
    }
    for (size_t index = 0; index < 1; index++) {
        m_command_table_pages.append(MM.allocate_supervisor_physical_page().release_nonnull());
    }
    m_command_list_region = MM.allocate_kernel_region(m_command_list_page->paddr(), PAGE_SIZE, "AHCI Port Command List", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    m_interrupt_enable.set_all();
}

void AHCIPort::clear_sata_error_register() const
{
    m_port_registers.serr = m_port_registers.serr;
}

void AHCIPort::handle_interrupt()
{
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Interrupt handled, PxIS {}", representative_port_index(), m_interrupt_status.raw_value());
    if (m_interrupt_status.raw_value() == 0) {
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::PRC) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::PC)) {
        reset();
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::INF)) {
        reset();
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::IF)) {
        recover_from_fatal_error();
    }
    m_interrupt_status.clear();
}

bool AHCIPort::is_interrupts_enabled() const
{
    return !m_interrupt_enable.is_cleared();
}

void AHCIPort::recover_from_fatal_error()
{
    ScopedSpinLock lock(m_lock);
    dmesgln("{}: AHCI Port {} fatal error, shutting down!", m_parent_handler->hba_controller()->pci_address(), representative_port_index());
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
            dbgln_if(AHCI_DEBUG, "AHCI Port {}: Eject Drive failed, SError {}", representative_port_index(), (u32)m_port_registers.serr);
            VERIFY_NOT_REACHED();
        }
        if ((m_port_registers.ci & (1 << unused_command_header.value())) == 0)
            break;
    }
    dbgln("AHCI Port {}: Eject Drive", representative_port_index());
    return;
}

bool AHCIPort::reset()
{
    ScopedSpinLock lock(m_lock);

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
    if (!initiate_sata_reset()) {
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
    size_t max_addressable_sector = 0;
    if (identify_device()) {
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

        dmesgln("AHCI Port {}: max lba {:x}, L/P sector size - {}/{} ", representative_port_index(), max_addressable_sector, logical_sector_size, physical_sector_size);

        // FIXME: We don't support ATAPI devices yet, so for now we don't "create" them
        if (!is_atapi_attached()) {
            m_connected_device = SATADiskDevice::create(m_parent_handler->hba_controller(), *this, logical_sector_size, max_addressable_sector);
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

void AHCIPort::rebase()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!m_command_list_page.is_null() && !m_fis_receive_page.is_null());
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
    m_port_registers.cmd = (m_port_registers.cmd & 0x0ffffff) | (1 << 28);
}

void AHCIPort::set_sleep_state() const
{
    VERIFY(m_lock.is_locked());
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

    m_current_scatter_list = ScatterList::create(request, allocated_dma_regions, m_connected_device->block_size());
    if (request.request_type() == AsyncBlockDeviceRequest::Write) {
        if (!request.read_from_buffer(request.buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * request.block_count())) {
            return AsyncDeviceRequest::MemoryFault;
        }
    }
    return {};
}

void AHCIPort::start_request(AsyncBlockDeviceRequest& request)
{
    ScopedSpinLock lock(m_lock);
    VERIFY(!m_current_scatter_list);

    auto result = prepare_and_set_scatter_list(request);
    if (result.has_value()) {
        request.complete(result.value());
        return;
    }

    auto success = access_device(request.request_type(), request.block_index(), request.block_count());
    if (!success) {
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }

    if (request.request_type() == AsyncBlockDeviceRequest::Read) {
        if (!request.write_to_buffer(request.buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * request.block_count())) {
            request.complete(AsyncDeviceRequest::MemoryFault);
            m_current_scatter_list = nullptr;
            return;
        }
    }
    m_current_scatter_list = nullptr;
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Reqeust success", representative_port_index());
    request.complete(AsyncDeviceRequest::Success);
}

void AHCIPort::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY(m_lock.is_locked());
}

bool AHCIPort::spin_until_ready() const
{
    VERIFY(m_lock.is_locked());
    size_t spin = 0;
    while ((m_port_registers.tfd & (ATA_SR_BSY | ATA_SR_DRQ)) && spin <= 100) {
        IO::delay(1000);
        spin++;
    }
    if (spin == 100) {
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: SPIN exceeded 100 miliseconds threshold", representative_port_index());
        return false;
    }
    return true;
}

bool AHCIPort::access_device(AsyncBlockDeviceRequest::RequestType direction, u64 lba, u8 block_count)
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_connected_device);
    VERIFY(is_operable());
    VERIFY(m_current_scatter_list);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {}", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count);
    if (!spin_until_ready())
        return false;

    auto unused_command_header = try_to_find_unused_command_header();
    VERIFY(unused_command_header.has_value());
    auto* command_list_entries = (volatile AHCI::CommandHeader*)m_command_list_region->vaddr().as_ptr();
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()].paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = (block_count * m_connected_device->block_size());
    command_list_entries[unused_command_header.value()].prdtl = m_current_scatter_list->scatters_count();

    // Note: we must set the correct Dword count in this register. Real hardware
    // AHCI controllers do care about this field! QEMU doesn't care if we don't
    // set the correct CFL field in this register, real hardware will set an
    // handshake error bit in PxSERR register if CFL is incorrect.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P | AHCI::CommandHeaderAttributes::C | (is_atapi_attached() ? AHCI::CommandHeaderAttributes::A : 0) | (direction == AsyncBlockDeviceRequest::RequestType::Write ? AHCI::CommandHeaderAttributes::W : 0);

    auto command_table_region = MM.allocate_kernel_region(m_command_table_pages[unused_command_header.value()].paddr().page_base(), page_round_up(sizeof(AHCI::CommandTable)), "AHCI Command Table", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    auto& command_table = *(volatile AHCI::CommandTable*)command_table_region->vaddr().as_ptr();
    memset(const_cast<u8*>(command_table.command_fis), 0, 64);

    size_t scatter_entry_index = 0;
    for (auto scatter_page : m_current_scatter_list->vmobject().physical_pages()) {
        VERIFY(scatter_page);
        dbgln_if(AHCI_DEBUG, "AHCI Port {}: Add a transfer scatter entry @ {}", representative_port_index(), scatter_page->paddr());
        command_table.descriptors[scatter_entry_index].base_high = 0;
        command_table.descriptors[scatter_entry_index].base_low = scatter_page->paddr().get();
        command_table.descriptors[scatter_entry_index].byte_count = PAGE_SIZE - 1;
        scatter_entry_index++;
    }

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
    start_command_list_processing();
    full_memory_barrier();
    mark_command_header_ready_to_process(unused_command_header.value());
    full_memory_barrier();

    while (1) {
        if ((m_port_registers.ci & (1 << unused_command_header.value())) == 0)
            break;
    }
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {} @ {}, ended", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count, m_dma_buffers[0].paddr());
    return true;
}

bool AHCIPort::identify_device()
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
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P | AHCI::CommandHeaderAttributes::C;

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

    full_memory_barrier();
    mark_command_header_ready_to_process(unused_command_header.value());
    full_memory_barrier();

    while (1) {
        if (m_port_registers.serr != 0) {
            dbgln("AHCI Port {}: Identify failed, SError {}", representative_port_index(), (u32)m_port_registers.serr);
            return false;
        }
        if ((m_port_registers.ci & (1 << unused_command_header.value())) == 0)
            break;
    }
    return true;
}

bool AHCIPort::shutdown()
{
    ScopedSpinLock lock(m_lock);
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
    VERIFY(is_operable());
    m_port_registers.cmd = m_port_registers.cmd | 1;
}

void AHCIPort::mark_command_header_ready_to_process(u8 command_header_index) const
{
    VERIFY(m_lock.is_locked());
    VERIFY(is_operable());
    m_port_registers.ci = 1 << command_header_index;
}

void AHCIPort::stop_command_list_processing() const
{
    VERIFY(m_lock.is_locked());
    m_port_registers.cmd = m_port_registers.cmd & 0xfffffffe;
}

void AHCIPort::start_fis_receiving() const
{
    VERIFY(m_lock.is_locked());
    m_port_registers.cmd = m_port_registers.cmd | (1 << 4);
}

void AHCIPort::power_on() const
{
    VERIFY(m_lock.is_locked());
    if (!(m_port_registers.cmd & (1 << 20)))
        return;
    m_port_registers.cmd = m_port_registers.cmd | (1 << 2);
}

void AHCIPort::spin_up() const
{
    VERIFY(m_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}, staggered spin up? {}", representative_port_index(), m_parent_handler->hba_capabilities().staggered_spin_up_supported);
    if (!m_parent_handler->hba_capabilities().staggered_spin_up_supported)
        return;
    m_port_registers.cmd = m_port_registers.cmd | (1 << 1);
}

void AHCIPort::stop_fis_receiving() const
{
    VERIFY(m_lock.is_locked());
    m_port_registers.cmd = m_port_registers.cmd & 0xFFFFFFEF;
}

bool AHCIPort::initiate_sata_reset()
{
    VERIFY(m_lock.is_locked());
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
    full_memory_barrier();
    set_interface_state(AHCI::DeviceDetectionInitialization::NoActionRequested);
    full_memory_barrier();

    retry = 0;
    while (retry < 5000) {
        if (!((m_port_registers.ssts & 0xf) == 0))
            break;

        IO::delay(10);
        retry++;
    }

    dmesgln("AHCI Port {}: {}", representative_port_index(), try_disambiguate_sata_status());

    full_memory_barrier();
    clear_sata_error_register();
    return (m_port_registers.ssts & 0xf) == 3;
}

void AHCIPort::set_interface_state(AHCI::DeviceDetectionInitialization requested_action)
{
    VERIFY(m_lock.is_locked());
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
