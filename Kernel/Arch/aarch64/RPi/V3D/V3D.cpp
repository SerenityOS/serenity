/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/V3D.h>
#include <Kernel/Arch/MemoryFences.h>
#include <Kernel/Arch/aarch64/RPi/V3D/DeviceNode.h>
#include <Kernel/Arch/aarch64/RPi/V3D/Registers.h>
#include <Kernel/Arch/aarch64/RPi/V3D/V3D.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Memory/MemoryManager.h>

// VideoCore IV 3D Architecture Reference Guide: https://docs.broadcom.com/doc/12358545
// This specification only covers an older revision of the VideoCore 3D architecture.

namespace Kernel::RPi::V3D {

ErrorOr<NonnullRefPtr<V3D>> V3D::create(DeviceTree::Device::Resource hub_registers_resource, DeviceTree::Device::Resource core_0_registers_resource, InterruptNumber hub_interrupt_number, Optional<InterruptNumber> core_interrupt_number)
{
    if (hub_registers_resource.size < sizeof(HubRegisters))
        return EINVAL;

    if (core_0_registers_resource.size < sizeof(CoreRegisters))
        return EINVAL;

    auto hub_registers = TRY(Memory::map_typed_writable<HubRegisters volatile>(hub_registers_resource.paddr));
    auto core_0_registers = TRY(Memory::map_typed_writable<CoreRegisters volatile>(core_0_registers_resource.paddr));

    auto v3d = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) V3D(move(hub_registers), move(core_0_registers), hub_interrupt_number, core_interrupt_number)));
    TRY(v3d->initialize());

    return v3d;
}

void V3D::map_buffer(PageTable& page_table, GPUVirtualAddress gpu_vaddr, Memory::VMObject const& vmobject)
{
    page_table.insert_entries_for_buffer({}, gpu_vaddr, vmobject);
}

void V3D::unmap_buffer(PageTable& page_table, GPUVirtualAddress gpu_vaddr, Memory::VMObject const& vmobject)
{
    page_table.remove_entries_for_buffer({}, gpu_vaddr, vmobject);
    flush_mmu_cache_and_tlb();
}

ErrorOr<void> V3D::submit_job(PageTable const& page_table, V3DJob const& job)
{
    Checked<u32> binning_control_list_end = job.binning_control_list_address;
    binning_control_list_end += job.binning_control_list_size;
    if (binning_control_list_end.has_overflow())
        return EOVERFLOW;

    Checked<u32> rendering_control_list_end = job.rendering_control_list_address;
    rendering_control_list_end += job.rendering_control_list_size;
    if (rendering_control_list_end.has_overflow())
        return EOVERFLOW;

    // FIXME: Make job submission asynchronous. This requires some userspace API to wait until the job is finished.
    //        Currently, we just use a Mutex to ensure that only one thread can run a job at a time.
    //        This thread will be blocked until the job is finished.
    //        Once we make job submission asynchronous, we need to ensure that the Context used by this job
    //        stays alive until this job is finished (maybe by using `RefPtr`s?).
    //        We could also ensure that buffers stay alive during runtime, but it's arguably userspace's fault
    //        to free buffers that are still in use. This would just result in a GPU page fault.
    MutexLocker locker { m_job_mutex };

    // Ensure that the bottom bits are 0 so we can set the valid bit correctly.
    if ((job.tile_state_data_array_address % V3D_PAGE_SIZE) != 0)
        return EINVAL;

    // Ensure that any writes to the page table, buffers, etc. are visible before giving them to the GPU.
    store_memory_fence();

    activate_page_table(page_table);

    flush_caches();

    m_core_0_registers->control_list_executor.thread_0_tile_allocation_memory_address = job.tile_allocation_memory_address;
    m_core_0_registers->control_list_executor.thread_0_tile_allocation_memory_size = job.tile_allocation_memory_size;
    m_core_0_registers->control_list_executor.thread_0_tile_state_data_array_address = job.tile_state_data_array_address | CoreRegisters::TILE_STATE_DATA_ARRAY_ADDRESS_VALID;

    m_core_0_registers->control_list_executor.thread_0_control_list_start_address = job.binning_control_list_address;
    m_core_0_registers->control_list_executor.thread_0_control_list_end_address = binning_control_list_end.value();

    auto binning_job_wait_result = m_current_binning_job_finished_wait_queue.wait_until(m_current_binning_job_finished, [](bool& job_finished) {
        if (!job_finished)
            return false;
        job_finished = false;
        return true;
    });

    if (binning_job_wait_result.is_error()) {
        dbgln("V3D: Binning job was interrupted!");
        dbgln("V3D: FIXME: Don't know how to cancel/abort jobs. The GPU might be in an undefined state now.");

        return binning_job_wait_result.release_error();
    }

    flush_caches();

    m_core_0_registers->control_list_executor.thread_1_control_list_start_address = job.rendering_control_list_address;
    m_core_0_registers->control_list_executor.thread_1_control_list_end_address = rendering_control_list_end.value();

    auto render_job_wait_result = m_current_render_job_finished_wait_queue.wait_until(m_current_render_job_finished, [](bool& job_finished) {
        if (!job_finished)
            return false;
        job_finished = false;
        return true;
    });

    if (render_job_wait_result.is_error()) {
        dbgln("V3D: Render job was interrupted!");
        dbgln("V3D: FIXME: Don't know how to cancel/abort jobs. The GPU might be in an undefined state now.");

        return render_job_wait_result.release_error();
    }

    return {};
}

V3D::V3D(Memory::TypedMapping<HubRegisters volatile> hub_registers, Memory::TypedMapping<CoreRegisters volatile> core_0_registers, InterruptNumber hub_interrupt_number, Optional<InterruptNumber> core_interrupt_number)
    : m_hub_registers(move(hub_registers))
    , m_core_0_registers(move(core_0_registers))
    , m_hub_interrupt_handler(*this, hub_interrupt_number)
{
    if (core_interrupt_number.has_value())
        m_core_interrupt_handler.emplace(*this, core_interrupt_number.value());
}

ErrorOr<void> V3D::initialize()
{
    m_device_node = TRY(DeviceNode::create(*this));

    m_illegal_vaddr_target_page = TRY(MM.allocate_physical_page(Memory::MemoryManager::ShouldZeroFill::Yes, nullptr, Memory::MemoryType::NonCacheable));

    // Ensure zeroing of the illegal vaddr target page is visible before giving it to the GPU.
    store_memory_fence();

    m_hub_registers->mmu_0.illegal_vaddr_target_paddr = (m_illegal_vaddr_target_page->paddr().get() >> V3D_PAGE_SHIFT)
        | HubRegisters::ILLEGAL_VADDR_TARGET_PADDR_VALID;

    m_hub_registers->mmu_0.control = HubRegisters::MMUControl::Enable
        | HubRegisters::MMUControl::WriteViolationInterrupt
        | HubRegisters::MMUControl::WriteViolationAbort
        | HubRegisters::MMUControl::InvalidPageTableEntryEnable
        | HubRegisters::MMUControl::InvalidPageTableEntryInterrupt
        | HubRegisters::MMUControl::InvalidPageTableEntryAbort
        | HubRegisters::MMUControl::CapExceededInterrupt
        | HubRegisters::MMUControl::CapExceededAbort;
    m_hub_registers->mmu_0.mmu_cache_control = HubRegisters::MMUCacheControl::Enable;

    flush_mmu_cache_and_tlb();

    m_hub_registers->interrupt_mask_clear = HubRegisters::Interrupt::MMUCapExceeded
        | HubRegisters::Interrupt::MMUPageTableEntryInvalid
        | HubRegisters::Interrupt::MMUWriteViolation;

    m_core_0_registers->interrupt_mask_clear = CoreRegisters::Interrupt::RenderModeFrameDone
        | CoreRegisters::Interrupt::BinningModeFlushDone
        | CoreRegisters::Interrupt::BinnerOutOfMemory
        | CoreRegisters::Interrupt::BinnerOverspillMemoryInUse;

    return {};
}

void V3D::flush_mmu_cache_and_tlb()
{
    m_hub_registers->mmu_0.mmu_cache_control |= HubRegisters::MMUCacheControl::Flush;
    while (has_flag(m_hub_registers->mmu_0.mmu_cache_control, HubRegisters::MMUCacheControl::Flushing))
        Processor::wait_check();

    m_hub_registers->mmu_0.control |= HubRegisters::MMUControl::TLBClear;
    while (has_flag(m_hub_registers->mmu_0.control, HubRegisters::MMUControl::TLBClearing))
        Processor::wait_check();
}

void V3D::flush_caches()
{
    m_core_0_registers->texture_cache_flush_start_addr = 0;
    m_core_0_registers->texture_cache_flush_end_addr = 0xffff'ffff;
    m_core_0_registers->texture_cache_control = CoreRegisters::TextureCacheControl::StartFlush
        | CoreRegisters::TextureCacheControl::FlushModeFlush;

    m_core_0_registers->slices_cache_control = 0xffff'ffff;
}

void V3D::activate_page_table(PageTable const& page_table)
{
    auto page_table_physical_page_index = page_table.physical_address().get() >> V3D_PAGE_SHIFT;

    if (m_hub_registers->mmu_0.page_table_base_page_index == page_table_physical_page_index) {
        // Already active, nothing to do.
        return;
    }

    m_hub_registers->mmu_0.page_table_base_page_index = page_table_physical_page_index;
    flush_mmu_cache_and_tlb();
}

bool V3D::handle_interrupt()
{
    // Handle hub interrupt(s).

    auto hub_interrupts = m_hub_registers->interrupt_status;

    m_hub_registers->interrupt_clear_pending = hub_interrupts;

    if (has_any_flag(hub_interrupts, HubRegisters::Interrupt::MMUCapExceeded | HubRegisters::Interrupt::MMUPageTableEntryInvalid | HubRegisters::Interrupt::MMUWriteViolation)) {
        dbgln("V3D: Page fault!");

        if (has_flag(hub_interrupts, HubRegisters::Interrupt::MMUCapExceeded))
            dbgln("V3D: Cap exceeded");
        if (has_flag(hub_interrupts, HubRegisters::Interrupt::MMUPageTableEntryInvalid))
            dbgln("V3D: PTE invalid");
        if (has_flag(hub_interrupts, HubRegisters::Interrupt::MMUWriteViolation))
            dbgln("V3D: Write violation");

        auto vaddr = m_hub_registers->mmu_0.fault_vaddr << 4u;

        dbgln("V3D: Fault GPU virtual address: {:#08x}", vaddr);
        dbgln("V3D: Fault AXI ID: {:#08x}", m_hub_registers->mmu_0.fault_axi_id);
    }

    // Handle core interrupt(s).

    auto core_interrupts = m_core_0_registers->interrupt_status;

    m_core_0_registers->interrupt_clear_pending = core_interrupts;

    if (has_flag(core_interrupts, CoreRegisters::Interrupt::BinningModeFlushDone)) {
        m_current_binning_job_finished.with([](bool& job_finished) { job_finished = true; });
        m_current_binning_job_finished_wait_queue.notify_one();
    }
    if (has_flag(core_interrupts, CoreRegisters::Interrupt::RenderModeFrameDone)) {
        m_current_render_job_finished.with([](bool& job_finished) { job_finished = true; });
        m_current_render_job_finished_wait_queue.notify_one();
    }
    if (has_flag(core_interrupts, CoreRegisters::Interrupt::BinnerOutOfMemory)) {
        dbgln("V3D: Binner out of memory! FIXME: Allocate overspill memory.");
    }
    if (has_flag(core_interrupts, CoreRegisters::Interrupt::BinnerOverspillMemoryInUse)) {
        dbgln("V3D: Binner overspill memory in use");
    }

    return to_underlying(hub_interrupts) != 0 || to_underlying(core_interrupts) != 0;
}

static constinit Array const compatibles_array = {
    "brcm,2712-v3d"sv,
};

DEVICETREE_DRIVER(V3DDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/gpu/brcm,bcm-v3d.yaml
ErrorOr<void> V3DDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto hub_registers_resource = TRY(device.get_resource(0));
    auto core_0_registers_resource = TRY(device.get_resource(1));

    auto hub_interrupt_number = TRY(device.get_interrupt_number(0));

    Optional<InterruptNumber> core_interrupt_number;

    auto core_interrupt_number_or_error = device.get_interrupt_number(1);
    if (!core_interrupt_number_or_error.is_error())
        core_interrupt_number = core_interrupt_number_or_error.release_value();

    auto v3d = TRY(V3D::create(hub_registers_resource, core_0_registers_resource, hub_interrupt_number, core_interrupt_number));
    (void)v3d.leak_ref();

    return {};
}

}
