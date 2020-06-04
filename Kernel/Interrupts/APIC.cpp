/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/TypedMapping.h>

#define IRQ_APIC_SPURIOUS 0x7f

#define APIC_BASE_MSR 0x1b

#define APIC_REG_EOI 0xb0
#define APIC_REG_LD 0xd0
#define APIC_REG_DF 0xe0
#define APIC_REG_SIV 0xf0
#define APIC_REG_TPR 0x80
#define APIC_REG_ICR_LOW 0x300
#define APIC_REG_ICR_HIGH 0x310
#define APIC_REG_LVT_TIMER 0x320
#define APIC_REG_LVT_THERMAL 0x330
#define APIC_REG_LVT_PERFORMANCE_COUNTER 0x340
#define APIC_REG_LVT_LINT0 0x350
#define APIC_REG_LVT_LINT1 0x360
#define APIC_REG_LVT_ERR 0x370

namespace Kernel {

static APIC *s_apic;

bool APIC::initialized()
{
    return (s_apic != nullptr);
}

APIC& APIC::the()
{
    ASSERT(APIC::initialized());
    return *s_apic;
}

void APIC::initialize()
{
    ASSERT(!APIC::initialized());
    s_apic = new APIC();
}



PhysicalAddress APIC::get_base()
{
    u32 lo, hi;
    MSR msr(APIC_BASE_MSR);
    msr.get(lo, hi);
    return PhysicalAddress(lo & 0xfffff000);
}

void APIC::set_base(const PhysicalAddress& base)
{
    u32 hi = 0;
    u32 lo = base.get() | 0x800;
    MSR msr(APIC_BASE_MSR);
    msr.set(lo, hi);
}

void APIC::write_register(u32 offset, u32 value)
{
    *reinterpret_cast<volatile u32*>(m_apic_base->vaddr().offset(offset).as_ptr()) = value;
}

u32 APIC::read_register(u32 offset)
{
    return *reinterpret_cast<volatile u32*>(m_apic_base->vaddr().offset(offset).as_ptr());
}

void APIC::write_icr(const ICRReg& icr)
{
    write_register(APIC_REG_ICR_HIGH, icr.high());
    write_register(APIC_REG_ICR_LOW, icr.low());
}

#define APIC_LVT_MASKED (1 << 16)
#define APIC_LVT_TRIGGER_LEVEL (1 << 14)
#define APIC_LVT(iv, dm) ((iv & 0xff) | ((dm & 0x7) << 8))

extern "C" void apic_ap_start(void);
extern "C" u16 apic_ap_start_size;
extern "C" u32 ap_cpu_init_stacks;
extern "C" u32 ap_cpu_init_cr0;
extern "C" u32 ap_cpu_init_cr3;
extern "C" u32 ap_cpu_init_cr4;
extern "C" u32 ap_cpu_gdtr;
extern "C" u32 ap_cpu_idtr;

void APIC::eoi()
{
    write_register(APIC_REG_EOI, 0x0);
}

u8 APIC::spurious_interrupt_vector()
{
    return IRQ_APIC_SPURIOUS;
}

#define APIC_INIT_VAR_PTR(tpe,vaddr,varname) \
    reinterpret_cast<volatile tpe*>(reinterpret_cast<ptrdiff_t>(vaddr) \
        + reinterpret_cast<ptrdiff_t>(&varname) \
        - reinterpret_cast<ptrdiff_t>(&apic_ap_start))

bool APIC::init_bsp()
{
    // FIXME: Use the ACPI MADT table
    if (!MSR::have())
        return false;

    // check if we support local apic
    CPUID id(1);
    if ((id.edx() & (1 << 9)) == 0)
        return false;

    PhysicalAddress apic_base = get_base();
    klog() << "Initializing APIC, base: " << apic_base;
    set_base(apic_base);

    m_apic_base = MM.allocate_kernel_region(apic_base.page_base(), PAGE_ROUND_UP(1), {}, Region::Access::Read | Region::Access::Write);

    auto rsdp = ACPI::StaticParsing::find_rsdp();
    if (!rsdp.has_value()) {
        klog() << "APIC: RSDP not found";
        return false;
    }
    auto madt_address = ACPI::StaticParsing::find_table(rsdp.value(), "APIC");
    if (madt_address.is_null()) {
        klog() << "APIC: MADT table not found";
        return false;
    }

    u32 processor_cnt = 0;
    u32 processor_enabled_cnt = 0;
    auto madt = map_typed<ACPI::Structures::MADT>(madt_address);
    size_t entry_index = 0;
    size_t entries_length = madt->h.length - sizeof(ACPI::Structures::MADT);
    auto* madt_entry = madt->entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::LocalAPIC) {
            auto* plapic_entry = (const ACPI::Structures::MADTEntries::ProcessorLocalAPIC*)madt_entry;
            klog() << "APIC: AP found @ MADT entry " << entry_index << ", Processor Id: " << String::format("%02x", plapic_entry->acpi_processor_id)
                << " APIC Id: " << String::format("%02x", plapic_entry->apic_id) << " Flags: " << String::format("%08x", plapic_entry->flags);
            processor_cnt++;
            if ((plapic_entry->flags & 0x1) != 0)
                processor_enabled_cnt++;
        }
        madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress(madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
    
    if (processor_enabled_cnt < 1)
        processor_enabled_cnt = 1;
    if (processor_cnt < 1)
        processor_cnt = 1;

    klog() << "APIC Processors found: "  << processor_cnt << ", enabled: " << processor_enabled_cnt;

    enable_bsp();

    if (processor_enabled_cnt > 1) {
        u32 aps_to_enable = processor_enabled_cnt - 1;
        
        // Copy the APIC startup code and variables to P0x00008000
        auto apic_startup_region = MM.allocate_kernel_region_identity(PhysicalAddress(0x8000), PAGE_ROUND_UP(apic_ap_start_size), {}, Region::Access::Read | Region::Access::Write | Region::Access::Execute);
        memcpy(apic_startup_region->vaddr().as_ptr(), reinterpret_cast<const void*>(apic_ap_start), apic_ap_start_size);

        // Allocate enough stacks for all APs
        for (u32 i = 0; i < aps_to_enable; i++) {
            auto stack_region = MM.allocate_kernel_region(Thread::default_kernel_stack_size, {}, Region::Access::Read | Region::Access::Write, false, true, true);
            if (!stack_region) {
                klog() << "APIC: Failed to allocate stack for AP #" << i;
                return false;
            }
            stack_region->set_stack(true);
            klog() << "APIC: Allocated AP #" << i << " stack at " << stack_region->vaddr();
            m_apic_ap_stacks.append(stack_region.release_nonnull());
        }

        // Store pointers to all stacks for the APs to use
        auto ap_stack_array = APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_stacks);
        for (size_t i = 0; i < m_apic_ap_stacks.size(); i++)
            ap_stack_array[i] = m_apic_ap_stacks[i].vaddr().get() + Thread::default_kernel_stack_size;

        // Store the BSP's CR3 value for the APs to use
        *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr3) = MM.kernel_page_directory().cr3();
        
        // Store the BSP's GDT and IDT for the APs to use
        const auto& gdtr = get_gdtr();
        *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_gdtr) = FlatPtr(&gdtr);
        const auto& idtr = get_idtr();
        *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_idtr) = FlatPtr(&idtr);
        
        // Store the BSP's CR0 and CR4 values for the APs to use
        *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr0) = read_cr0();
        *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr4) = read_cr4();
        
        klog() << "APIC: Starting " << aps_to_enable << " AP(s)";

        // INIT
        write_icr(ICRReg(0, ICRReg::INIT, ICRReg::Physical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf));

        IO::delay(10 * 1000);

        for (int i = 0; i < 2; i++) {
            // SIPI
            write_icr(ICRReg(0x08, ICRReg::StartUp, ICRReg::Physical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf)); // start execution at P8000

            IO::delay(200);
        }

        // Now wait until the ap_cpu_init_pending variable dropped to 0, which means all APs are initialized and no longer need these special mappings
        if (m_apic_ap_count.load(AK::MemoryOrder::memory_order_consume) != aps_to_enable) {
            klog() << "APIC: Waiting for " << aps_to_enable << " AP(s) to finish initialization...";
            do {
                // Wait a little bit
                IO::delay(200);
            } while (m_apic_ap_count.load(AK::MemoryOrder::memory_order_consume) != aps_to_enable);
        }
        
        klog() << "APIC: " << processor_enabled_cnt << " processors are initialized and running";
    }
    return true;
}

void APIC::enable_bsp()
{
    // FIXME: Ensure this method can only be executed by the BSP.
    enable(0);
}

void APIC::enable(u32 cpu)
{
    if (cpu == 0)// FIXME: once memory management can deal with it, re-enable for all
        klog() << "Enabling local APIC for cpu #" << cpu;

    if (cpu == 0) {
        // dummy read, apparently to avoid a bug in old CPUs.
        read_register(APIC_REG_SIV);
        // set spurious interrupt vector
        write_register(APIC_REG_SIV, (IRQ_APIC_SPURIOUS + IRQ_VECTOR_BASE) | 0x100);

        // local destination mode (flat mode)
        write_register(APIC_REG_DF, 0xf0000000);

        // set destination id (note that this limits it to 8 cpus)
        write_register(APIC_REG_LD, 0);

        SpuriousInterruptHandler::initialize(IRQ_APIC_SPURIOUS);

        write_register(APIC_REG_LVT_TIMER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
        write_register(APIC_REG_LVT_THERMAL, APIC_LVT(0, 0) | APIC_LVT_MASKED);
        write_register(APIC_REG_LVT_PERFORMANCE_COUNTER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
        write_register(APIC_REG_LVT_LINT0, APIC_LVT(0, 7) | APIC_LVT_MASKED);
        write_register(APIC_REG_LVT_LINT1, APIC_LVT(0, 0) | APIC_LVT_TRIGGER_LEVEL);
        write_register(APIC_REG_LVT_ERR, APIC_LVT(0, 0) | APIC_LVT_MASKED);

        write_register(APIC_REG_TPR, 0);
    } else {
        // Notify the BSP that we are done initializing. It will unmap the startup data at P8000
        m_apic_ap_count++;
    }
}

}
