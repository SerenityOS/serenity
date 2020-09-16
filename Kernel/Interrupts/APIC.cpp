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
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/ProcessorInfo.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/TypedMapping.h>

//#define APIC_DEBUG
//#define APIC_SMP_DEBUG

#define IRQ_APIC_IPI (0xfd - IRQ_VECTOR_BASE)
#define IRQ_APIC_ERR (0xfe - IRQ_VECTOR_BASE)
#define IRQ_APIC_SPURIOUS (0xff - IRQ_VECTOR_BASE)

#define APIC_ICR_DELIVERY_PENDING (1 << 12)

#define APIC_ENABLED (1 << 8)

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

static AK::Singleton<APIC> s_apic;

class APICIPIInterruptHandler final : public GenericInterruptHandler {
public:
    explicit APICIPIInterruptHandler(u8 interrupt_vector)
        : GenericInterruptHandler(interrupt_vector, true)
    {
    }
    virtual ~APICIPIInterruptHandler()
    {
    }

    static void initialize(u8 interrupt_number)
    {
        new APICIPIInterruptHandler(interrupt_number);
    }

    virtual void handle_interrupt(const RegisterState&) override;

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual const char* purpose() const override { return "IPI Handler"; }
    virtual const char* controller() const override { ASSERT_NOT_REACHED(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const override { return false; }

private:
};

class APICErrInterruptHandler final : public GenericInterruptHandler {
public:
    explicit APICErrInterruptHandler(u8 interrupt_vector)
        : GenericInterruptHandler(interrupt_vector, true)
    {
    }
    virtual ~APICErrInterruptHandler()
    {
    }

    static void initialize(u8 interrupt_number)
    {
        new APICErrInterruptHandler(interrupt_number);
    }

    virtual void handle_interrupt(const RegisterState&) override;

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual const char* purpose() const override { return "SMP Error Handler"; }
    virtual const char* controller() const override { ASSERT_NOT_REACHED(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const override { return false; }

private:
};

bool APIC::initialized()
{
    return s_apic.is_initialized();
}

APIC& APIC::the()
{
    ASSERT(APIC::initialized());
    return *s_apic;
}

void APIC::initialize()
{
    ASSERT(!APIC::initialized());
    s_apic.ensure_instance();
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

void APIC::set_lvt(u32 offset, u8 interrupt)
{
    write_register(offset, (read_register(offset) & 0xffffffff) | interrupt);
}

void APIC::set_siv(u32 offset, u8 interrupt)
{
    write_register(offset, (read_register(offset) & 0xffffffff) | interrupt | APIC_ENABLED);
}

void APIC::wait_for_pending_icr()
{
    while ((read_register(APIC_REG_ICR_LOW) & APIC_ICR_DELIVERY_PENDING) != 0) {
        IO::delay(200);
    }
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
extern "C" u32 ap_cpu_init_processor_info_array;
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

#define APIC_INIT_VAR_PTR(tpe, vaddr, varname)                         \
    reinterpret_cast<volatile tpe*>(reinterpret_cast<ptrdiff_t>(vaddr) \
        + reinterpret_cast<ptrdiff_t>(&varname)                        \
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
#ifdef APIC_DEBUG
    klog() << "Initializing APIC, base: " << apic_base;
#endif
    set_base(apic_base);

    m_apic_base = MM.allocate_kernel_region(apic_base.page_base(), PAGE_ROUND_UP(1), {}, Region::Access::Read | Region::Access::Write);
    if (!m_apic_base) {
        klog() << "APIC: Failed to allocate memory for APIC base";
        return false;
    }

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

    auto madt = map_typed<ACPI::Structures::MADT>(madt_address);
    size_t entry_index = 0;
    size_t entries_length = madt->h.length - sizeof(ACPI::Structures::MADT);
    auto* madt_entry = madt->entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::LocalAPIC) {
            auto* plapic_entry = (const ACPI::Structures::MADTEntries::ProcessorLocalAPIC*)madt_entry;
#ifdef APIC_DEBUG
            klog() << "APIC: AP found @ MADT entry " << entry_index << ", Processor Id: " << String::format("%02x", plapic_entry->acpi_processor_id)
                   << " APIC Id: " << String::format("%02x", plapic_entry->apic_id) << " Flags: " << String::format("%08x", plapic_entry->flags);
#endif
            m_processor_cnt++;
            if ((plapic_entry->flags & 0x1) != 0)
                m_processor_enabled_cnt++;
        }
        madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress(madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }

    if (m_processor_enabled_cnt < 1)
        m_processor_enabled_cnt = 1;
    if (m_processor_cnt < 1)
        m_processor_cnt = 1;

    klog() << "APIC Processors found: " << m_processor_cnt << ", enabled: " << m_processor_enabled_cnt;

    enable(0);
    return true;
}

void APIC::do_boot_aps()
{
    ASSERT(m_processor_enabled_cnt > 1);
    u32 aps_to_enable = m_processor_enabled_cnt - 1;

    // Copy the APIC startup code and variables to P0x00008000
    // Also account for the data appended to:
    // * aps_to_enable u32 values for ap_cpu_init_stacks
    // * aps_to_enable u32 values for ap_cpu_init_processor_info_array
    auto apic_startup_region = MM.allocate_kernel_region_identity(PhysicalAddress(0x8000), PAGE_ROUND_UP(apic_ap_start_size + (2 * aps_to_enable * sizeof(u32))), {}, Region::Access::Read | Region::Access::Write | Region::Access::Execute);
    memcpy(apic_startup_region->vaddr().as_ptr(), reinterpret_cast<const void*>(apic_ap_start), apic_ap_start_size);

    // Allocate enough stacks for all APs
    Vector<OwnPtr<Region>> apic_ap_stacks;
    for (u32 i = 0; i < aps_to_enable; i++) {
        auto stack_region = MM.allocate_kernel_region(Thread::default_kernel_stack_size, {}, Region::Access::Read | Region::Access::Write, false, true, true);
        if (!stack_region) {
            klog() << "APIC: Failed to allocate stack for AP #" << i;
            return;
        }
        stack_region->set_stack(true);
        apic_ap_stacks.append(move(stack_region));
    }

    // Store pointers to all stacks for the APs to use
    auto ap_stack_array = APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_stacks);
    ASSERT(aps_to_enable == apic_ap_stacks.size());
    for (size_t i = 0; i < aps_to_enable; i++) {
        ap_stack_array[i] = apic_ap_stacks[i]->vaddr().get() + Thread::default_kernel_stack_size;
#ifdef APIC_DEBUG
        klog() << "APIC: CPU[" << (i + 1) << "] stack at " << VirtualAddress(ap_stack_array[i]);
#endif
    }

    // Allocate Processor structures for all APs and store the pointer to the data
    m_ap_processor_info.resize(aps_to_enable);
    for (size_t i = 0; i < aps_to_enable; i++)
        m_ap_processor_info[i] = make<Processor>();
    auto ap_processor_info_array = &ap_stack_array[aps_to_enable];
    for (size_t i = 0; i < aps_to_enable; i++) {
        ap_processor_info_array[i] = FlatPtr(m_ap_processor_info[i].ptr());
#ifdef APIC_DEBUG
        klog() << "APIC: CPU[" << (i + 1) << "] Processor at " << VirtualAddress(ap_processor_info_array[i]);
#endif
    }
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_processor_info_array) = FlatPtr(&ap_processor_info_array[0]);

    // Store the BSP's CR3 value for the APs to use
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr3) = MM.kernel_page_directory().cr3();

    // Store the BSP's GDT and IDT for the APs to use
    const auto& gdtr = Processor::current().get_gdtr();
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_gdtr) = FlatPtr(&gdtr);
    const auto& idtr = get_idtr();
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_idtr) = FlatPtr(&idtr);

    // Store the BSP's CR0 and CR4 values for the APs to use
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr0) = read_cr0();
    *APIC_INIT_VAR_PTR(u32, apic_startup_region->vaddr().as_ptr(), ap_cpu_init_cr4) = read_cr4();

    // Create an idle thread for each processor. We have to do this here
    // because we won't be able to send FlushTLB messages, so we have to
    // have all memory set up for the threads so that when the APs are
    // starting up, they can access all the memory properly
    m_ap_idle_threads.resize(aps_to_enable);
    for (u32 i = 0; i < aps_to_enable; i++)
        m_ap_idle_threads[i] = Scheduler::create_ap_idle_thread(i + 1);

#ifdef APIC_DEBUG
    klog() << "APIC: Starting " << aps_to_enable << " AP(s)";
#endif

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
#ifdef APIC_DEBUG
        klog() << "APIC: Waiting for " << aps_to_enable << " AP(s) to finish initialization...";
#endif
        do {
            // Wait a little bit
            IO::delay(200);
        } while (m_apic_ap_count.load(AK::MemoryOrder::memory_order_consume) != aps_to_enable);
    }

#ifdef APIC_DEBUG
    klog() << "APIC: " << m_processor_enabled_cnt << " processors are initialized and running";
#endif
}

void APIC::boot_aps()
{
    if (m_processor_enabled_cnt <= 1)
        return;

    // We split this into another call because do_boot_aps() will cause
    // MM calls upon exit, and we don't want to call smp_enable before that
    do_boot_aps();

    // Enable SMP, which means IPIs may now be sent
    Processor::smp_enable();

#ifdef APIC_DEBUG
    dbg() << "All processors initialized and waiting, trigger all to continue";
#endif

    // Now trigger all APs to continue execution (need to do this after
    // the regions have been freed so that we don't trigger IPIs
    m_apic_ap_continue.store(1, AK::MemoryOrder::memory_order_release);
}

void APIC::enable(u32 cpu)
{
    if (cpu >= 8) {
        // TODO: x2apic support?
        klog() << "SMP support is currently limited to 8 CPUs!";
        Processor::halt();
    }

    u32 apic_id = (1u << cpu);

    write_register(APIC_REG_LD, (read_register(APIC_REG_LD) & 0x00ffffff) | (apic_id << 24)); // TODO: only if not in x2apic mode

    // read it back to make sure it's actually set
    apic_id = read_register(APIC_REG_LD) >> 24;
    Processor::current().info().set_apic_id(apic_id);

#ifdef APIC_DEBUG
    klog() << "Enabling local APIC for cpu #" << cpu << " apic id: " << apic_id;
#endif

    if (cpu == 0) {
        SpuriousInterruptHandler::initialize(IRQ_APIC_SPURIOUS);

        // set error interrupt vector
        set_lvt(APIC_REG_LVT_ERR, IRQ_APIC_ERR);
        APICErrInterruptHandler::initialize(IRQ_APIC_ERR);

        // register IPI interrupt vector
        APICIPIInterruptHandler::initialize(IRQ_APIC_IPI);
    }

    // set spurious interrupt vector
    set_siv(APIC_REG_SIV, IRQ_APIC_SPURIOUS);

    // local destination mode (flat mode)
    write_register(APIC_REG_DF, 0xf0000000);

    write_register(APIC_REG_LVT_TIMER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_THERMAL, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_PERFORMANCE_COUNTER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_LINT0, APIC_LVT(0, 7) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_LINT1, APIC_LVT(0, 0) | APIC_LVT_TRIGGER_LEVEL);

    write_register(APIC_REG_TPR, 0);
}

Thread* APIC::get_idle_thread(u32 cpu) const
{
    ASSERT(cpu > 0);
    return m_ap_idle_threads[cpu - 1];
}

void APIC::init_finished(u32 cpu)
{
    // This method is called once the boot stack is no longer needed
    ASSERT(cpu > 0);
    ASSERT(cpu < m_processor_enabled_cnt);
    // Since we're waiting on other APs here, we shouldn't have the
    // scheduler lock
    ASSERT(!g_scheduler_lock.own_lock());

    // Notify the BSP that we are done initializing. It will unmap the startup data at P8000
    m_apic_ap_count.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
#ifdef APIC_DEBUG
    klog() << "APIC: cpu #" << cpu << " initialized, waiting for all others";
#endif

    // The reason we're making all APs wait until the BSP signals them is that
    // we don't want APs to trigger IPIs (e.g. through MM) while the BSP
    // is unable to process them
    while (!m_apic_ap_continue.load(AK::MemoryOrder::memory_order_consume)) {
        IO::delay(200);
    }

#ifdef APIC_DEBUG
    klog() << "APIC: cpu #" << cpu << " continues, all others are initialized";
#endif

    // do_boot_aps() freed memory, so we need to update our tlb
    Processor::flush_entire_tlb_local();

    // Now enable all the interrupts
    APIC::the().enable(cpu);
}

void APIC::broadcast_ipi()
{
#ifdef APIC_SMP_DEBUG
    klog() << "SMP: Broadcast IPI from cpu #" << Processor::current().id();
#endif
    wait_for_pending_icr();
    write_icr(ICRReg(IRQ_APIC_IPI + IRQ_VECTOR_BASE, ICRReg::Fixed, ICRReg::Logical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf));
}

void APIC::send_ipi(u32 cpu)
{
    auto& proc = Processor::current();
#ifdef APIC_SMP_DEBUG
    klog() << "SMP: Send IPI from cpu #" << proc.id() << " to cpu #" << cpu;
#endif
    ASSERT(cpu != proc.id());
    ASSERT(cpu < 8);
    wait_for_pending_icr();
    write_icr(ICRReg(IRQ_APIC_IPI + IRQ_VECTOR_BASE, ICRReg::Fixed, ICRReg::Logical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::NoShorthand, 1u << cpu));
}

void APICIPIInterruptHandler::handle_interrupt(const RegisterState&)
{
#ifdef APIC_SMP_DEBUG
    klog() << "APIC IPI on cpu #" << Processor::current().id();
#endif
}

bool APICIPIInterruptHandler::eoi()
{
#ifdef APIC_SMP_DEBUG
    klog() << "SMP: IPI eoi";
#endif
    APIC::the().eoi();
    return true;
}

void APICErrInterruptHandler::handle_interrupt(const RegisterState&)
{
    klog() << "APIC: SMP error on cpu #" << Processor::current().id();
}

bool APICErrInterruptHandler::eoi()
{
    APIC::the().eoi();
    return true;
}

}
