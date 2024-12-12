/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// For more information about locking in this code
// please look at Documentation/Kernel/AHCILocking.md

#include <AK/Atomic.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/Storage/AHCI/ATADiskDevice.h>
#include <Kernel/Devices/Storage/AHCI/Port.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/ScatterGatherList.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<AHCIPort>> AHCIPort::create(AHCIController const& controller, AHCI::HBADefinedCapabilities hba_capabilities, volatile AHCI::PortRegisters& registers, u32 port_index)
{
    auto identify_buffer_page = MUST(MM.allocate_physical_page());
    auto port = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) AHCIPort(controller, move(identify_buffer_page), hba_capabilities, registers, port_index)));
    TRY(port->allocate_resources_and_initialize_ports());
    return port;
}

ErrorOr<void> AHCIPort::allocate_resources_and_initialize_ports()
{
    if (is_interface_disabled()) {
        m_disabled_by_firmware = true;
        return {};
    }

    m_fis_receive_page = TRY(MM.allocate_physical_page());

    for (size_t index = 0; index < 1; index++) {
        auto dma_page = TRY(MM.allocate_physical_page());
        m_dma_buffers.append(move(dma_page));
    }
    for (size_t index = 0; index < 1; index++) {
        auto command_table_page = TRY(MM.allocate_physical_page());
        m_command_table_pages.append(move(command_table_page));
    }

    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    m_command_list_region = TRY(MM.allocate_dma_buffer_page("AHCI Port Command List"sv, Memory::Region::Access::ReadWrite, m_command_list_page, Memory::MemoryType::IO));

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Command list page at {}", representative_port_index(), m_command_list_page->paddr());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: FIS receive page at {}", representative_port_index(), m_fis_receive_page->paddr());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Command list region at {}", representative_port_index(), m_command_list_region->vaddr());
    return {};
}

UNMAP_AFTER_INIT AHCIPort::AHCIPort(AHCIController const& controller, NonnullRefPtr<Memory::PhysicalRAMPage> identify_buffer_page, AHCI::HBADefinedCapabilities hba_capabilities, volatile AHCI::PortRegisters& registers, u32 port_index)
    : m_port_index(port_index)
    , m_hba_capabilities(hba_capabilities)
    , m_identify_buffer_page(move(identify_buffer_page))
    , m_port_registers(registers)
    , m_parent_controller(controller)
    , m_interrupt_status((u32 volatile&)m_port_registers.is)
    , m_interrupt_enable((u32 volatile&)m_port_registers.ie)
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
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::PRC) && m_interrupt_status.is_set(AHCI::PortInterruptFlag::PC)) {
        clear_sata_error_register();
        if ((m_port_registers.ssts & 0xf) != 3 && m_connected_device) {
            m_connected_device->prepare_for_unplug();
            StorageManagement::the().remove_device(*m_connected_device);
            auto work_item_creation_result = g_io_work->try_queue([this]() {
                m_connected_device.clear();
            });
            if (work_item_creation_result.is_error()) {
                auto current_request = m_current_request;
                m_current_request.clear();
                current_request->complete(AsyncDeviceRequest::OutOfMemory);
            }
        } else {
            auto work_item_creation_result = g_io_work->try_queue([this]() {
                reset();
            });
            if (work_item_creation_result.is_error()) {
                auto current_request = m_current_request;
                m_current_request.clear();
                current_request->complete(AsyncDeviceRequest::OutOfMemory);
            }
        }
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::PRC)) {
        clear_sata_error_register();
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::INF)) {
        // We need to defer the reset, because we can receive interrupts when
        // resetting the device.
        auto work_item_creation_result = g_io_work->try_queue([this]() {
            reset();
        });
        if (work_item_creation_result.is_error()) {
            auto current_request = m_current_request;
            m_current_request.clear();
            current_request->complete(AsyncDeviceRequest::OutOfMemory);
        }
        return;
    }
    if (m_interrupt_status.is_set(AHCI::PortInterruptFlag::IF) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::TFE) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::HBD) || m_interrupt_status.is_set(AHCI::PortInterruptFlag::HBF)) {
        auto work_item_creation_result = g_io_work->try_queue([this]() {
            recover_from_fatal_error();
        });
        if (work_item_creation_result.is_error()) {
            auto current_request = m_current_request;
            m_current_request.clear();
            current_request->complete(AsyncDeviceRequest::OutOfMemory);
        }
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
            auto work_item_creation_result = g_io_work->try_queue([this]() {
                dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request handled", representative_port_index());
                MutexLocker locker(m_lock);
                VERIFY(m_current_request);
                VERIFY(m_current_scatter_list);
                if (!m_connected_device) {
                    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Request success", representative_port_index());
                    complete_current_request(AsyncDeviceRequest::Failure);
                    return;
                }
                if (m_current_request->request_type() == AsyncBlockDeviceRequest::Read) {
                    if (auto result = m_current_request->write_to_buffer(m_current_request->buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * m_current_request->block_count()); result.is_error()) {
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
            if (work_item_creation_result.is_error()) {
                auto current_request = m_current_request;
                m_current_request.clear();
                current_request->complete(AsyncDeviceRequest::OutOfMemory);
            }
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
    MutexLocker locker(m_lock);
    SpinlockLocker lock(m_hard_lock);

    dmesgln("{}: AHCI Port {} fatal error, shutting down!", m_parent_controller->device_identifier().address(), representative_port_index());
    dmesgln("{}: AHCI Port {} fatal error, SError {}", m_parent_controller->device_identifier().address(), representative_port_index(), (u32)m_port_registers.serr);
    stop_command_list_processing();
    stop_fis_receiving();
    m_interrupt_enable.clear();
}

bool AHCIPort::reset()
{
    MutexLocker locker(m_lock);
    SpinlockLocker lock(m_hard_lock);

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
    if (!initiate_sata_reset()) {
        return false;
    }
    return initialize();
}

UNMAP_AFTER_INIT bool AHCIPort::initialize_without_reset()
{
    MutexLocker locker(m_lock);
    SpinlockLocker lock(m_hard_lock);
    dmesgln("AHCI Port {}: {}", representative_port_index(), try_disambiguate_sata_status());
    return initialize();
}

bool AHCIPort::initialize()
{
    VERIFY(m_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Initialization. Signature = {:#08x}", representative_port_index(), static_cast<u32>(m_port_registers.sig));
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

    if (identify_device()) {
        auto identify_block = Memory::map_typed<ATAIdentifyBlock>(m_identify_buffer_page->paddr()).release_value_but_fixme_should_propagate_errors();
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
            m_connected_device = MUST(ATADiskDevice::create(*m_parent_controller, { m_port_index, 0 }, 0, logical_sector_size, max_addressable_sector));
        } else {
            dbgln("AHCI Port {}: Ignoring ATAPI devices as we don't support them.", representative_port_index());
        }
    }
    return true;
}

char const* AHCIPort::try_disambiguate_sata_status()
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

    // Try to wait 1 second for HBA to clear Command List Running and FIS Receive Running
    wait_until_condition_met_or_timeout(1000, 1000, [this]() -> bool {
        return !(m_port_registers.cmd & (1 << 15)) && !(m_port_registers.cmd & (1 << 14));
    });
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
    size_t needed_dma_regions_count = Memory::page_round_up((block_count * m_connected_device->block_size())).value() / PAGE_SIZE;
    VERIFY(needed_dma_regions_count <= m_dma_buffers.size());
    return needed_dma_regions_count;
}

Optional<AsyncDeviceRequest::RequestResult> AHCIPort::prepare_and_set_scatter_list(AsyncBlockDeviceRequest& request)
{
    VERIFY(m_lock.is_locked());
    VERIFY(request.block_count() > 0);

    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> allocated_dma_regions;
    for (size_t index = 0; index < calculate_descriptors_count(request.block_count()); index++) {
        allocated_dma_regions.append(m_dma_buffers.at(index));
    }

    m_current_scatter_list = Memory::ScatterGatherList::try_create(request, allocated_dma_regions.span(), m_connected_device->block_size(), "AHCI Scattered DMA"sv).release_value_but_fixme_should_propagate_errors();
    if (!m_current_scatter_list)
        return AsyncDeviceRequest::Failure;
    if (request.request_type() == AsyncBlockDeviceRequest::Write) {
        if (auto result = request.read_from_buffer(request.buffer(), m_current_scatter_list->dma_region().as_ptr(), m_connected_device->block_size() * request.block_count()); result.is_error()) {
            return AsyncDeviceRequest::MemoryFault;
        }
    }
    return {};
}

void AHCIPort::start_request(AsyncBlockDeviceRequest& request)
{
    MutexLocker locker(m_lock);
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
        microseconds_delay(1000);
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
    SpinlockLocker lock(m_hard_lock);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {}", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count);
    if (!spin_until_ready())
        return false;

    auto unused_command_header = try_to_find_unused_command_header();
    VERIFY(unused_command_header.has_value());
    auto* command_list_entries = (volatile AHCI::CommandHeader*)m_command_list_region->vaddr().as_ptr();
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()]->paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = 0;
    command_list_entries[unused_command_header.value()].prdtl = m_current_scatter_list->scatters_count();

    // Note: we must set the correct Dword count in this register. Real hardware
    // AHCI controllers do care about this field! QEMU doesn't care if we don't
    // set the correct CFL field in this register, real hardware will set an
    // handshake error bit in PxSERR register if CFL is incorrect.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P | (is_atapi_attached() ? AHCI::CommandHeaderAttributes::A : 0) | (direction == AsyncBlockDeviceRequest::RequestType::Write ? AHCI::CommandHeaderAttributes::W : 0);

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: CLE: ctba={:#08x}, ctbau={:#08x}, prdbc={:#08x}, prdtl={:#04x}, attributes={:#04x}", representative_port_index(), (u32)command_list_entries[unused_command_header.value()].ctba, (u32)command_list_entries[unused_command_header.value()].ctbau, (u32)command_list_entries[unused_command_header.value()].prdbc, (u16)command_list_entries[unused_command_header.value()].prdtl, (u16)command_list_entries[unused_command_header.value()].attributes);

    auto command_table_region = MM.allocate_kernel_region_with_physical_pages({ &m_command_table_pages[unused_command_header.value()], 1 }, "AHCI Command Table"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value();
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

    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Do a {}, lba {}, block count {} @ {}, ended", representative_port_index(), direction == AsyncBlockDeviceRequest::RequestType::Write ? "write" : "read", lba, block_count, m_dma_buffers[0]->paddr());
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
    command_list_entries[unused_command_header.value()].ctba = m_command_table_pages[unused_command_header.value()]->paddr().get();
    command_list_entries[unused_command_header.value()].ctbau = 0;
    command_list_entries[unused_command_header.value()].prdbc = 512;
    command_list_entries[unused_command_header.value()].prdtl = 1;

    // Note: we must set the correct Dword count in this register. Real hardware AHCI controllers do care about this field!
    // QEMU doesn't care if we don't set the correct CFL field in this register, real hardware will set an handshake error bit in PxSERR register.
    command_list_entries[unused_command_header.value()].attributes = (size_t)FIS::DwordCount::RegisterHostToDevice | AHCI::CommandHeaderAttributes::P;

    auto command_table_region = MM.allocate_kernel_region_with_physical_pages({ &m_command_table_pages[unused_command_header.value()], 1 }, "AHCI Command Table"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value();
    auto& command_table = *(volatile AHCI::CommandTable*)command_table_region->vaddr().as_ptr();
    memset(const_cast<u8*>(command_table.command_fis), 0, 64);
    command_table.descriptors[0].base_high = 0;
    command_table.descriptors[0].base_low = m_identify_buffer_page->paddr().get();
    command_table.descriptors[0].byte_count = 512 - 1;
    auto& fis = *(volatile FIS::HostToDevice::Register*)command_table.command_fis;
    fis.header.fis_type = (u8)FIS::Type::RegisterHostToDevice;
    fis.command = m_port_registers.sig == ATA::DeviceSignature::ATAPI ? ATA_CMD_IDENTIFY_PACKET : ATA_CMD_IDENTIFY;
    fis.device = 0;
    fis.header.port_muliplier = fis.header.port_muliplier | (u8)FIS::HeaderAttributes::C;

    // The below loop waits until the port is no longer busy before issuing a new command
    if (!spin_until_ready())
        return false;

    // Just in case we have a pending interrupt.
    m_interrupt_enable.clear();
    m_interrupt_status.clear();

    full_memory_barrier();
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Marking command header at index {} as ready to identify device", representative_port_index(), unused_command_header.value());
    m_port_registers.ci = 1 << unused_command_header.value();
    full_memory_barrier();

    size_t time_elapsed = 0;
    bool success = false;
    while (1) {
        // Note: We allow it to spin for 256 milliseconds, which should be enough for a device to respond.
        if (time_elapsed >= 256) {
            break;
        }
        if (m_port_registers.serr != 0) {
            dbgln("AHCI Port {}: Identify failed, SError {:#08x}", representative_port_index(), (u32)m_port_registers.serr);
            try_disambiguate_sata_error();
            break;
        }
        if (!(m_port_registers.ci & (1 << unused_command_header.value()))) {
            success = true;
            break;
        }
        microseconds_delay(1000); // delay with 1 milliseconds
        time_elapsed++;
    }

    // Note: We probably ended up triggering an interrupt but we don't really want to handle it,
    // so just get rid of it.
    // FIXME: Do that in a better way so we don't need to actually remember this every time
    // we need to do this.
    m_interrupt_status.clear();
    m_interrupt_enable.set_all();

    return success;
}

void AHCIPort::wait_until_condition_met_or_timeout(size_t delay_in_microseconds, size_t retries, Function<bool(void)> condition_being_met) const
{
    size_t retry = 0;
    while (retry < retries) {
        if (condition_being_met())
            break;
        microseconds_delay(delay_in_microseconds);
        retry++;
    }
}

bool AHCIPort::shutdown()
{
    MutexLocker locker(m_lock);
    SpinlockLocker lock(m_hard_lock);
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
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Spin up. Staggered spin up? {}", representative_port_index(), m_hba_capabilities.staggered_spin_up_supported);
    if (!m_hba_capabilities.staggered_spin_up_supported)
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

bool AHCIPort::initiate_sata_reset()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    dbgln_if(AHCI_DEBUG, "AHCI Port {}: Initiate SATA reset", representative_port_index());
    stop_command_list_processing();
    full_memory_barrier();

    // Note: The AHCI specification says to wait now a 500 milliseconds
    // Try to wait 1 second for HBA to clear Command List Running
    wait_until_condition_met_or_timeout(100, 5000, [this]() -> bool {
        return !(m_port_registers.cmd & (1 << 15));
    });

    full_memory_barrier();
    spin_up();
    full_memory_barrier();
    set_interface_state(AHCI::DeviceDetectionInitialization::PerformInterfaceInitializationSequence);
    // The AHCI specification says to wait now a 1 millisecond
    microseconds_delay(1000);
    full_memory_barrier();
    set_interface_state(AHCI::DeviceDetectionInitialization::NoActionRequested);
    full_memory_barrier();

    wait_until_condition_met_or_timeout(10, 1000, [this]() -> bool {
        return is_phy_enabled();
    });

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
