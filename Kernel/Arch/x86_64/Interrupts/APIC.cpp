/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Singleton.h>
#include <AK/Types.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/MSR.h>
#include <Kernel/Arch/x86_64/ProcessorInfo.h>
#include <Kernel/Arch/x86_64/Time/APICTimer.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/ACPI/StaticParsing.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

#define IRQ_APIC_TIMER (0xfc - IRQ_VECTOR_BASE)
#define IRQ_APIC_IPI (0xfd - IRQ_VECTOR_BASE)
#define IRQ_APIC_ERR (0xfe - IRQ_VECTOR_BASE)
#define IRQ_APIC_SPURIOUS (0xff - IRQ_VECTOR_BASE)

#define APIC_ICR_DELIVERY_PENDING (1 << 12)

#define APIC_ENABLED (1 << 8)

#define APIC_BASE_MSR 0x1b
#define APIC_REGS_MSR_BASE 0x800

#define APIC_REG_ID 0x20
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
#define APIC_REG_TIMER_INITIAL_COUNT 0x380
#define APIC_REG_TIMER_CURRENT_COUNT 0x390
#define APIC_REG_TIMER_CONFIGURATION 0x3e0

namespace Kernel {

static Singleton<APIC> s_apic;

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
        auto* handler = new APICIPIInterruptHandler(interrupt_number);
        handler->register_interrupt_handler();
    }

    virtual bool handle_interrupt() override;

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual StringView purpose() const override { return "IPI Handler"sv; }
    virtual StringView controller() const override { return {}; }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }

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
        auto* handler = new APICErrInterruptHandler(interrupt_number);
        handler->register_interrupt_handler();
    }

    virtual bool handle_interrupt() override;

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual StringView purpose() const override { return "SMP Error Handler"sv; }
    virtual StringView controller() const override { return {}; }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }

private:
};

bool APIC::initialized()
{
    return s_apic.is_initialized();
}

APIC& APIC::the()
{
    VERIFY(APIC::initialized());
    return *s_apic;
}

UNMAP_AFTER_INIT void APIC::initialize()
{
    VERIFY(!APIC::initialized());
    s_apic.ensure_instance();
}

PhysicalAddress APIC::get_base()
{
    MSR msr(APIC_BASE_MSR);
    auto base = msr.get();
    return PhysicalAddress(base & 0xfffff000);
}

void APIC::set_base(PhysicalAddress const& base)
{
    MSR msr(APIC_BASE_MSR);
    u64 flags = 1 << 11;
    if (m_is_x2.was_set())
        flags |= 1 << 10;
    msr.set(base.get() | flags);
}

void APIC::write_register(u32 offset, u32 value)
{
    if (m_is_x2.was_set()) {
        MSR msr(APIC_REGS_MSR_BASE + (offset >> 4));
        msr.set(value);
    } else {
        *reinterpret_cast<u32 volatile*>(m_apic_base->vaddr().offset(offset).as_ptr()) = value;
    }
}

u32 APIC::read_register(u32 offset)
{
    if (m_is_x2.was_set()) {
        MSR msr(APIC_REGS_MSR_BASE + (offset >> 4));
        return (u32)msr.get();
    }
    return *reinterpret_cast<u32 volatile*>(m_apic_base->vaddr().offset(offset).as_ptr());
}

void APIC::set_lvt(u32 offset, u8 interrupt)
{
    write_register(offset, read_register(offset) | interrupt);
}

void APIC::set_siv(u32 offset, u8 interrupt)
{
    write_register(offset, read_register(offset) | interrupt | APIC_ENABLED);
}

void APIC::wait_for_pending_icr()
{
    while ((read_register(APIC_REG_ICR_LOW) & APIC_ICR_DELIVERY_PENDING) != 0) {
        microseconds_delay(200);
    }
}

void APIC::write_icr(ICRReg const& icr)
{
    if (m_is_x2.was_set()) {
        MSR msr(APIC_REGS_MSR_BASE + (APIC_REG_ICR_LOW >> 4));
        msr.set(icr.x2_value());
    } else {
        write_register(APIC_REG_ICR_HIGH, icr.x_high());
        write_register(APIC_REG_ICR_LOW, icr.x_low());
    }
}

#define APIC_LVT_TIMER_ONESHOT 0
#define APIC_LVT_TIMER_PERIODIC (1 << 17)
#define APIC_LVT_TIMER_TSCDEADLINE (1 << 18)

#define APIC_LVT_MASKED (1 << 16)
#define APIC_LVT_TRIGGER_LEVEL (1 << 14)
#define APIC_LVT(iv, dm) (((iv) & 0xff) | (((dm) & 0x7) << 8))

extern "C" void apic_ap_start(void);
extern "C" u16 apic_ap_start_size;
extern "C" FlatPtr ap_cpu_init_stacks;
extern "C" FlatPtr ap_cpu_init_processor_info_array;
extern "C" u32 ap_cpu_init_cr0;
extern "C" FlatPtr ap_cpu_init_cr3;
extern "C" u32 ap_cpu_init_cr4;
extern "C" FlatPtr ap_cpu_gdtr;
extern "C" FlatPtr ap_cpu_idtr;
extern "C" FlatPtr ap_cpu_kernel_map_base;
extern "C" FlatPtr ap_cpu_kernel_entry_function;

extern "C" [[noreturn]] void init_ap(FlatPtr, Processor*);

void APIC::eoi()
{
    write_register(APIC_REG_EOI, 0x0);
}

u8 APIC::spurious_interrupt_vector()
{
    return IRQ_APIC_SPURIOUS;
}

#define APIC_INIT_VAR_PTR(tpe, vaddr, varname)                         \
    reinterpret_cast<tpe volatile*>(reinterpret_cast<ptrdiff_t>(vaddr) \
        + reinterpret_cast<ptrdiff_t>(&varname)                        \
        - reinterpret_cast<ptrdiff_t>(&apic_ap_start))

UNMAP_AFTER_INIT bool APIC::init_bsp()
{
    // FIXME: Use the ACPI MADT table
    if (!MSR::have())
        return false;

    // check if we support local apic
    CPUID id(1);
    if ((id.edx() & (1 << 9)) == 0)
        return false;
    if (id.ecx() & (1 << 21))
        m_is_x2.set();

    PhysicalAddress apic_base = get_base();
    dbgln_if(APIC_DEBUG, "Initializing {}APIC, base: {}", m_is_x2.was_set() ? "x2" : "x", apic_base);
    set_base(apic_base);

    if (!m_is_x2.was_set()) {
        auto region_or_error = MM.allocate_mmio_kernel_region(apic_base.page_base(), PAGE_SIZE, {}, Memory::Region::Access::ReadWrite);
        if (region_or_error.is_error()) {
            dbgln("APIC: Failed to allocate memory for APIC base");
            return false;
        }
        m_apic_base = region_or_error.release_value();
    }

    auto possible_rsdp_physical_address = ACPI::StaticParsing::find_rsdp();
    if (!possible_rsdp_physical_address.has_value()) {
        dbgln("APIC: RSDP not found");
        return false;
    }

    auto possible_apic_physical_address_or_error = ACPI::StaticParsing::find_table(possible_rsdp_physical_address.value(), "APIC"sv);
    if (possible_apic_physical_address_or_error.is_error()) {
        dbgln("APIC: Failed to map RSDT/XSDT");
        return false;
    }
    auto possible_apic_physical_address = possible_apic_physical_address_or_error.release_value();
    if (!possible_apic_physical_address.has_value()) {
        dbgln("APIC: MADT table not found");
        return false;
    }

    if (kernel_command_line().is_smp_enabled()) {
        auto madt_or_error = Memory::map_typed<ACPI::Structures::MADT>(possible_apic_physical_address.value());
        if (madt_or_error.is_error()) {
            dbgln("APIC: Failed to map MADT table");
            return false;
        }
        auto madt = madt_or_error.release_value();
        size_t entry_index = 0;
        size_t entries_length = madt->h.length - sizeof(ACPI::Structures::MADT);
        auto* madt_entry = madt->entries;
        while (entries_length > 0) {
            size_t entry_length = madt_entry->length;
            if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::LocalAPIC) {
                auto* plapic_entry = (const ACPI::Structures::MADTEntries::ProcessorLocalAPIC*)madt_entry;
                dbgln_if(APIC_DEBUG, "APIC: AP found @ MADT entry {}, processor ID: {}, xAPIC ID: {}, flags: {:#08x}", entry_index, plapic_entry->acpi_processor_id, plapic_entry->apic_id, plapic_entry->flags);
                m_processor_cnt++;
                if ((plapic_entry->flags & 0x1) != 0)
                    m_processor_enabled_cnt++;
            } else if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::Local_x2APIC) {
                // Only used for APID IDs >= 255
                auto* plx2apic_entry = (const ACPI::Structures::MADTEntries::ProcessorLocalX2APIC*)madt_entry;
                dbgln_if(APIC_DEBUG, "APIC: AP found @ MADT entry {}, processor ID: {}, x2APIC ID: {}, flags: {:#08x}", entry_index, plx2apic_entry->acpi_processor_id, plx2apic_entry->apic_id, plx2apic_entry->flags);
                m_processor_cnt++;
                if ((plx2apic_entry->flags & 0x1) != 0)
                    m_processor_enabled_cnt++;
            }
            madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress(madt_entry).offset(entry_length).get());
            entries_length -= entry_length;
            entry_index++;
        }
        dbgln("APIC processors found: {}, enabled: {}", m_processor_cnt, m_processor_enabled_cnt);
    }

    if (m_processor_enabled_cnt < 1)
        m_processor_enabled_cnt = 1;
    if (m_processor_cnt < 1)
        m_processor_cnt = 1;

    enable(0);
    return true;
}

UNMAP_AFTER_INIT void APIC::setup_ap_boot_environment()
{
    VERIFY(!m_ap_boot_environment);
    VERIFY(m_processor_enabled_cnt > 1);
    u32 aps_to_enable = m_processor_enabled_cnt - 1;

    // Copy the APIC startup code and variables to P0x00008000
    // Also account for the data appended to:
    // * aps_to_enable u32 values for ap_cpu_init_stacks
    // * aps_to_enable u32 values for ap_cpu_init_processor_info_array
    constexpr u64 apic_startup_region_base = 0x8000;
    auto apic_startup_region_size = Memory::page_round_up(apic_ap_start_size + (2 * aps_to_enable * sizeof(FlatPtr))).release_value_but_fixme_should_propagate_errors();
    VERIFY(apic_startup_region_size < USER_RANGE_BASE);
    auto apic_startup_region = MUST(MM.create_identity_mapped_region(PhysicalAddress(apic_startup_region_base), apic_startup_region_size));
    u8* apic_startup_region_ptr = apic_startup_region->vaddr().as_ptr();
    memcpy(apic_startup_region_ptr, reinterpret_cast<void const*>(apic_ap_start), apic_ap_start_size);

    // Allocate enough stacks for all APs
    m_ap_temporary_boot_stacks.ensure_capacity(aps_to_enable);
    for (u32 i = 0; i < aps_to_enable; i++) {
        auto stack_region_or_error = MM.allocate_kernel_region(Thread::default_kernel_stack_size, {}, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow);
        if (stack_region_or_error.is_error()) {
            dbgln("APIC: Failed to allocate stack for AP #{}", i);
            return;
        }
        auto stack_region = stack_region_or_error.release_value();
        stack_region->set_stack(true);
        m_ap_temporary_boot_stacks.unchecked_append(move(stack_region));
    }

    // Store pointers to all stacks for the APs to use
    auto* ap_stack_array = APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_init_stacks);
    VERIFY(aps_to_enable == m_ap_temporary_boot_stacks.size());
    for (size_t i = 0; i < aps_to_enable; i++) {
        ap_stack_array[i] = m_ap_temporary_boot_stacks[i]->vaddr().get() + Thread::default_kernel_stack_size;
        dbgln_if(APIC_DEBUG, "APIC: CPU[{}] stack at {}", i + 1, VirtualAddress { ap_stack_array[i] });
    }

    // Allocate Processor structures for all APs and store the pointer to the data
    m_ap_processor_info.resize(aps_to_enable);
    for (size_t i = 0; i < aps_to_enable; i++)
        m_ap_processor_info[i] = adopt_nonnull_own_or_enomem(new (nothrow) Processor()).release_value_but_fixme_should_propagate_errors();
    auto* ap_processor_info_array = &ap_stack_array[aps_to_enable];
    for (size_t i = 0; i < aps_to_enable; i++) {
        ap_processor_info_array[i] = FlatPtr(m_ap_processor_info[i].ptr());
        dbgln_if(APIC_DEBUG, "APIC: CPU[{}] processor at {}", i + 1, VirtualAddress { ap_processor_info_array[i] });
    }
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_init_processor_info_array) = FlatPtr(&ap_processor_info_array[0]);

    // Store the BSP's CR3 value for the APs to use
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_init_cr3) = MM.kernel_page_directory().cr3();

    // Store the BSP's GDT and IDT for the APs to use
    auto const& gdtr = Processor::current().get_gdtr();
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_gdtr) = FlatPtr(&gdtr);
    auto const& idtr = get_idtr();
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_idtr) = FlatPtr(&idtr);

    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_kernel_map_base) = FlatPtr(g_boot_info.kernel_mapping_base);
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_kernel_entry_function) = FlatPtr(&init_ap);

    // Store the BSP's CR0 and CR4 values for the APs to use
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_init_cr0) = read_cr0();
    *APIC_INIT_VAR_PTR(FlatPtr, apic_startup_region_ptr, ap_cpu_init_cr4) = read_cr4();

    m_ap_boot_environment = move(apic_startup_region);
}

UNMAP_AFTER_INIT void APIC::do_boot_aps()
{
    VERIFY(m_ap_boot_environment);
    VERIFY(m_processor_enabled_cnt > 1);
    u32 aps_to_enable = m_processor_enabled_cnt - 1;

    // Create an idle thread for each processor. We have to do this here
    // because we won't be able to send FlushTLB messages, so we have to
    // have all memory set up for the threads so that when the APs are
    // starting up, they can access all the memory properly
    m_ap_idle_threads.resize(aps_to_enable);
    for (u32 i = 0; i < aps_to_enable; i++)
        m_ap_idle_threads[i] = Scheduler::create_ap_idle_thread(i + 1);

    dbgln_if(APIC_DEBUG, "APIC: Starting {} AP(s)", aps_to_enable);

    // INIT
    write_icr({ 0, 0, ICRReg::INIT, ICRReg::Physical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf });

    microseconds_delay(10 * 1000);

    for (int i = 0; i < 2; i++) {
        // SIPI
        write_icr({ 0x08, 0, ICRReg::StartUp, ICRReg::Physical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf }); // start execution at P8000

        microseconds_delay(200);
    }

    // Now wait until the ap_cpu_init_pending variable dropped to 0, which means all APs are initialized and no longer need these special mappings
    if (m_apic_ap_count.load(AK::MemoryOrder::memory_order_consume) != aps_to_enable) {
        dbgln_if(APIC_DEBUG, "APIC: Waiting for {} AP(s) to finish initialization...", aps_to_enable);
        do {
            // Wait a little bit
            microseconds_delay(200);
        } while (m_apic_ap_count.load(AK::MemoryOrder::memory_order_consume) != aps_to_enable);
    }

    dbgln_if(APIC_DEBUG, "APIC: {} processors are initialized and running", m_processor_enabled_cnt);

    // NOTE: Since this region is identity-mapped, we have to unmap it manually to prevent the virtual
    //       address range from leaking into the general virtual range allocator.
    m_ap_boot_environment->unmap();
    m_ap_boot_environment = nullptr;
    // When the APs signal that they finished their initialization they have already switched over to their
    // idle thread's stack, so the temporary boot stack can be deallocated
    m_ap_temporary_boot_stacks.clear();
}

UNMAP_AFTER_INIT void APIC::boot_aps()
{
    if (m_processor_enabled_cnt <= 1)
        return;

    // We split this into another call because do_boot_aps() will cause
    // MM calls upon exit, and we don't want to call smp_enable before that
    do_boot_aps();

    // Enable SMP, which means IPIs may now be sent
    Processor::smp_enable();

    dbgln_if(APIC_DEBUG, "All processors initialized and waiting, trigger all to continue");

    // Now trigger all APs to continue execution (need to do this after
    // the regions have been freed so that we don't trigger IPIs
    m_apic_ap_continue.store(1, AK::MemoryOrder::memory_order_release);
}

UNMAP_AFTER_INIT void APIC::enable(u32 cpu)
{
    VERIFY(m_is_x2.was_set() || cpu < 8);

    u32 apic_id;
    if (m_is_x2.was_set()) {
        dbgln_if(APIC_DEBUG, "Enable x2APIC on CPU #{}", cpu);

        // We need to enable x2 mode on each core independently
        set_base(get_base());

        apic_id = read_register(APIC_REG_ID);
    } else {
        dbgln_if(APIC_DEBUG, "Setting logical xAPIC ID for CPU #{}", cpu);

        // Use the CPU# as logical apic id
        VERIFY(cpu <= 8);
        write_register(APIC_REG_LD, (read_register(APIC_REG_LD) & 0x00ffffff) | (cpu << 24));

        // read it back to make sure it's actually set
        apic_id = read_register(APIC_REG_LD) >> 24;
    }

    dbgln_if(APIC_DEBUG, "CPU #{} apic id: {}", cpu, apic_id);
    Processor::current().info().set_apic_id(apic_id);

    dbgln_if(APIC_DEBUG, "Enabling local APIC for CPU #{}, logical APIC ID: {}", cpu, apic_id);

    if (cpu == 0) {
        SpuriousInterruptHandler::initialize(IRQ_APIC_SPURIOUS);

        APICErrInterruptHandler::initialize(IRQ_APIC_ERR);

        // register IPI interrupt vector
        APICIPIInterruptHandler::initialize(IRQ_APIC_IPI);
    }

    if (!m_is_x2.was_set()) {
        // local destination mode (flat mode), not supported in x2 mode
        write_register(APIC_REG_DF, 0xf0000000);
    }

    // set error interrupt vector
    set_lvt(APIC_REG_LVT_ERR, IRQ_APIC_ERR);

    // set spurious interrupt vector
    set_siv(APIC_REG_SIV, IRQ_APIC_SPURIOUS);

    write_register(APIC_REG_LVT_TIMER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_THERMAL, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_PERFORMANCE_COUNTER, APIC_LVT(0, 0) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_LINT0, APIC_LVT(0, 7) | APIC_LVT_MASKED);
    write_register(APIC_REG_LVT_LINT1, APIC_LVT(0, 0) | APIC_LVT_TRIGGER_LEVEL);

    write_register(APIC_REG_TPR, 0);
}

Thread* APIC::get_idle_thread(u32 cpu) const
{
    VERIFY(cpu > 0);
    return m_ap_idle_threads[cpu - 1];
}

UNMAP_AFTER_INIT void APIC::init_finished(u32 cpu)
{
    // This method is called once the boot stack is no longer needed
    VERIFY(cpu > 0);
    VERIFY(cpu < m_processor_enabled_cnt);
    // Since we're waiting on other APs here, we shouldn't have the
    // scheduler lock
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());

    // Notify the BSP that we are done initializing. It will unmap the startup data at P8000
    m_apic_ap_count.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
    dbgln_if(APIC_DEBUG, "APIC: CPU #{} initialized, waiting for all others", cpu);

    // The reason we're making all APs wait until the BSP signals them is that
    // we don't want APs to trigger IPIs (e.g. through MM) while the BSP
    // is unable to process them
    while (!m_apic_ap_continue.load(AK::MemoryOrder::memory_order_consume)) {
        microseconds_delay(200);
    }

    dbgln_if(APIC_DEBUG, "APIC: CPU #{} continues, all others are initialized", cpu);

    // do_boot_aps() freed memory, so we need to update our tlb
    Processor::flush_entire_tlb_local();

    // Now enable all the interrupts
    APIC::the().enable(cpu);
}

void APIC::broadcast_ipi()
{
    dbgln_if(APIC_SMP_DEBUG, "SMP: Broadcast IPI from CPU #{}", Processor::current_id());
    wait_for_pending_icr();
    write_icr({ IRQ_APIC_IPI + IRQ_VECTOR_BASE, 0xffffffff, ICRReg::Fixed, ICRReg::Logical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::AllExcludingSelf });
}

void APIC::send_ipi(u32 cpu)
{
    dbgln_if(APIC_SMP_DEBUG, "SMP: Send IPI from CPU #{} to CPU #{}", Processor::current_id(), cpu);
    VERIFY(cpu != Processor::current_id());
    VERIFY(cpu < Processor::count());
    wait_for_pending_icr();
    write_icr({ IRQ_APIC_IPI + IRQ_VECTOR_BASE, m_is_x2.was_set() ? Processor::by_id(cpu).info().apic_id() : cpu, ICRReg::Fixed, m_is_x2.was_set() ? ICRReg::Physical : ICRReg::Logical, ICRReg::Assert, ICRReg::TriggerMode::Edge, ICRReg::NoShorthand });
}

UNMAP_AFTER_INIT APICTimer* APIC::initialize_timers(HardwareTimerBase& calibration_timer)
{
    if (!m_apic_base && !m_is_x2.was_set())
        return nullptr;

    // We should only initialize and calibrate the APIC timer once on the BSP!
    VERIFY(Processor::is_bootstrap_processor());
    VERIFY(!m_apic_timer);

    m_apic_timer = APICTimer::initialize(IRQ_APIC_TIMER, calibration_timer);
    return m_apic_timer;
}

void APIC::setup_local_timer(u32 ticks, TimerMode timer_mode, bool enable)
{
    // Write 0 to the initial count so we don't accidentally start the timer when writing to the divide configuration register.
    write_register(APIC_REG_TIMER_INITIAL_COUNT, 0);

    u32 flags = 0;
    switch (timer_mode) {
    case TimerMode::OneShot:
        flags |= APIC_LVT_TIMER_ONESHOT;
        break;
    case TimerMode::Periodic:
        flags |= APIC_LVT_TIMER_PERIODIC;
        break;
    case TimerMode::TSCDeadline:
        flags |= APIC_LVT_TIMER_TSCDEADLINE;
        break;
    }
    if (!enable)
        flags |= APIC_LVT_MASKED;
    write_register(APIC_REG_LVT_TIMER, APIC_LVT(IRQ_APIC_TIMER + IRQ_VECTOR_BASE, 0) | flags);

    u32 config = read_register(APIC_REG_TIMER_CONFIGURATION);
    config &= ~0xf; // clear divisor (bits 0-3)
    switch (get_timer_divisor()) {
    case 1:
        config |= (1 << 3) | 3;
        break;
    case 2:
        break;
    case 4:
        config |= 1;
        break;
    case 8:
        config |= 2;
        break;
    case 16:
        config |= 3;
        break;
    case 32:
        config |= (1 << 3);
        break;
    case 64:
        config |= (1 << 3) | 1;
        break;
    case 128:
        config |= (1 << 3) | 2;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    write_register(APIC_REG_TIMER_CONFIGURATION, config);

    if (timer_mode == TimerMode::Periodic)
        write_register(APIC_REG_TIMER_INITIAL_COUNT, ticks / get_timer_divisor());
}

u32 APIC::get_timer_current_count()
{
    return read_register(APIC_REG_TIMER_CURRENT_COUNT);
}

u32 APIC::get_timer_divisor()
{
    return 16;
}

bool APICIPIInterruptHandler::handle_interrupt()
{
    dbgln_if(APIC_SMP_DEBUG, "APIC IPI on CPU #{}", Processor::current_id());
    return true;
}

bool APICIPIInterruptHandler::eoi()
{
    dbgln_if(APIC_SMP_DEBUG, "SMP: IPI EOI");
    APIC::the().eoi();
    return true;
}

bool APICErrInterruptHandler::handle_interrupt()
{
    dbgln("APIC: SMP error on CPU #{}", Processor::current_id());
    return true;
}

bool APICErrInterruptHandler::eoi()
{
    APIC::the().eoi();
    return true;
}

bool HardwareTimer<GenericInterruptHandler>::eoi()
{
    APIC::the().eoi();
    return true;
}

}
