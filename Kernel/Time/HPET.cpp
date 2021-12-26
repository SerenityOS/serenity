/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/StringView.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Time/HPET.h>
#include <Kernel/Time/HPETComparator.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

#define ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD 0x05F5E100
#define NANOSECOND_PERIOD_TO_HERTZ(x) 1000000000 / x
#define MEGAHERTZ_TO_HERTZ(x) (x / 1000000)

//#define HPET_DEBUG

namespace HPETFlags {
enum class Attributes {
    Counter64BitCapable = 1 << 13,
    LegacyReplacementRouteCapable = 1 << 15
};

enum class Configuration {
    Enable = 1 << 0,
    LegacyReplacementRoute = 1 << 1
};

enum class TimerConfiguration : u32 {
    LevelTriggered = 1 << 1,
    InterruptEnable = 1 << 2,
    GeneratePeriodicInterrupt = 1 << 3,
    PeriodicInterruptCapable = 1 << 4,
    Timer64BitsCapable = 1 << 5,
    ValueSet = 1 << 6,
    Force32BitMode = 1 << 8,
    FSBInterruptEnable = 1 << 14,
    FSBInterruptDelivery = 1 << 15
};
};

struct [[gnu::packed]] HPETRegister {
    volatile u32 low;
    volatile u32 high;
};

struct [[gnu::packed]] TimerStructure {
    volatile u32 capabilities;
    volatile u32 interrupt_routing;
    HPETRegister comparator_value;
    volatile u64 fsb_interrupt_route;
    u64 reserved;
};

struct [[gnu::packed]] HPETCapabilityRegister {
    // Note: We must do a 32 bit access to offsets 0x0, or 0x4 only, according to HPET spec.
    volatile u32 attributes;
    volatile u32 main_counter_tick_period;
    u64 reserved;
};

struct [[gnu::packed]] HPETRegistersBlock {
    HPETCapabilityRegister capabilities;
    HPETRegister configuration;
    u64 reserved1;
    HPETRegister interrupt_status;
    u8 reserved2[0xF0 - 0x28];
    HPETRegister main_counter_value;
    u64 reserved3;
    TimerStructure timers[3];
    u8 reserved4[0x400 - 0x160];
};

static_assert(__builtin_offsetof(HPETRegistersBlock, main_counter_value) == 0xf0);
static_assert(__builtin_offsetof(HPETRegistersBlock, timers[0]) == 0x100);
static_assert(__builtin_offsetof(HPETRegistersBlock, timers[1]) == 0x120);

static u64 read_register_safe64(const HPETRegister& reg)
{
    // As per 2.4.7 this reads the 64 bit value in a consistent manner
    // using only 32 bit reads
    u32 low, high = reg.high;
    for (;;) {
        low = reg.low;
        u32 new_high = reg.high;
        if (new_high == high)
            break;
        high = new_high;
    }
    return ((u64)high << 32) | (u64)low;
}

static HPET* s_hpet;
static bool hpet_initialized { false };

bool HPET::initialized()
{
    return hpet_initialized;
}

HPET& HPET::the()
{
    ASSERT(HPET::initialized());
    ASSERT(s_hpet != nullptr);
    return *s_hpet;
}

bool HPET::test_and_initialize()
{
    ASSERT(!HPET::initialized());
    hpet_initialized = true;
    auto hpet = ACPI::Parser::the()->find_table("HPET");
    if (hpet.is_null())
        return false;
    klog() << "HPET @ " << hpet;

    auto sdt = map_typed<ACPI::Structures::HPET>(hpet);

    // Note: HPET is only usable from System Memory
    ASSERT(sdt->event_timer_block.address_space == (u8)ACPI::GenericAddressStructure::AddressSpace::SystemMemory);

    if (TimeManagement::is_hpet_periodic_mode_allowed()) {
        if (!check_for_exisiting_periodic_timers()) {
            dbgln("HPET: No periodic capable timers");
            return false;
        }
    }
    new HPET(PhysicalAddress(hpet));
    return true;
}

bool HPET::check_for_exisiting_periodic_timers()
{
    auto hpet = ACPI::Parser::the()->find_table("HPET");
    if (hpet.is_null())
        return false;

    auto sdt = map_typed<ACPI::Structures::HPET>(hpet);
    ASSERT(sdt->event_timer_block.address_space == 0);
    auto registers = map_typed<HPETRegistersBlock>(PhysicalAddress(sdt->event_timer_block.address));

    size_t timers_count = ((registers->capabilities.attributes >> 8) & 0x1f) + 1;
    for (size_t index = 0; index < timers_count; index++) {
        if (registers->timers[index].capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable)
            return true;
    }
    return false;
}

void HPET::global_disable()
{
    auto& regs = registers();
    regs.configuration.low = regs.configuration.low & ~(u32)HPETFlags::Configuration::Enable;
}
void HPET::global_enable()
{
    auto& regs = registers();
    regs.configuration.low = regs.configuration.low | (u32)HPETFlags::Configuration::Enable;
}

void HPET::update_periodic_comparator_value()
{
    // According to 2.3.9.2.2 the only safe way to change the periodic timer frequency
    // is to disable all periodic timers, reset the main counter and each timer's comparator value.
    // This introduces time drift, so it should be avoided unless absolutely necessary.
    global_disable();
    auto& regs = registers();

    u64 previous_main_value = (u64)regs.main_counter_value.low | ((u64)regs.main_counter_value.high << 32);
    m_main_counter_drift += previous_main_value - m_main_counter_last_read;
    m_main_counter_last_read = 0;
    regs.main_counter_value.low = 0;
    regs.main_counter_value.high = 0;
    for (auto& comparator : m_comparators) {
        auto& timer = regs.timers[comparator.comparator_number()];
        if (!comparator.is_enabled())
            continue;
        if (comparator.is_periodic()) {
            // Note that this means we're restarting all periodic timers. There is no
            // way to resume periodic timers properly because we reset the main counter
            // and we can only write the period into the comparator value...
            timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::ValueSet;
            u64 value = frequency() / comparator.ticks_per_second();
#ifdef HPET_DEBUG
            dbg() << "HPET: Update periodic comparator " << comparator.comparator_number() << " comparator value to " << value << " main value was: " << previous_main_value;
#endif
            timer.comparator_value.low = (u32)value;
            timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::ValueSet;
            timer.comparator_value.high = (u32)(value >> 32);
        } else {
            // Set the new target comparator value to the delta to the remaining ticks
            u64 current_value = (u64)timer.comparator_value.low | ((u64)timer.comparator_value.high << 32);
            u64 value = current_value - previous_main_value;
#ifdef HPET_DEBUG
            dbg() << "HPET: Update non-periodic comparator " << comparator.comparator_number() << " comparator value from " << current_value << " to " << value << " main value was: " << previous_main_value;
#endif
            timer.comparator_value.low = (u32)value;
            timer.comparator_value.high = (u32)(value >> 32);
        }
    }

    global_enable();
}

void HPET::update_non_periodic_comparator_value(const HPETComparator& comparator)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!comparator.is_periodic());
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    auto& regs = registers();
    auto& timer = regs.timers[comparator.comparator_number()];
    u64 value = frequency() / comparator.ticks_per_second();
    // NOTE: If the main counter passes this new value before we finish writing it, we will never receive an interrupt!
    u64 new_counter_value = read_register_safe64(regs.main_counter_value) + value;
    timer.comparator_value.high = (u32)(new_counter_value >> 32);
    timer.comparator_value.low = (u32)new_counter_value;
}

u64 HPET::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = read_register_safe64(registers().main_counter_value);
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read)
        delta_ticks += current_value - m_main_counter_last_read;
    else
        delta_ticks += m_main_counter_last_read - current_value; // the counter wrapped around
    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto ticks_per_second = frequency();
    if (ticks_since_last_second >= ticks_per_second) {
        seconds_since_boot += ticks_since_last_second / ticks_per_second;
        ticks_this_second = ticks_since_last_second % ticks_per_second;
    } else {
        ticks_this_second = ticks_since_last_second;
    }

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / ticks_per_second;
}

void HPET::enable_periodic_interrupt(const HPETComparator& comparator)
{
#ifdef HPET_DEBUG
    klog() << "HPET: Set comparator " << comparator.comparator_number() << " to be periodic.";
#endif
    disable(comparator);
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    auto capabilities = timer.capabilities;
    ASSERT(capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable);
    timer.capabilities = capabilities | (u32)HPETFlags::TimerConfiguration::GeneratePeriodicInterrupt;
    if (comparator.is_enabled())
        enable(comparator);
}
void HPET::disable_periodic_interrupt(const HPETComparator& comparator)
{
#ifdef HPET_DEBUG
    klog() << "HPET: Disable periodic interrupt in comparator " << comparator.comparator_number() << ".";
#endif
    disable(comparator);
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    auto capabilities = timer.capabilities;
    ASSERT(capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable);
    timer.capabilities = capabilities & ~(u32)HPETFlags::TimerConfiguration::GeneratePeriodicInterrupt;
    if (comparator.is_enabled())
        enable(comparator);
}

void HPET::disable(const HPETComparator& comparator)
{
#ifdef HPET_DEBUG
    klog() << "HPET: Disable comparator " << comparator.comparator_number() << ".";
#endif
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    timer.capabilities = timer.capabilities & ~(u32)HPETFlags::TimerConfiguration::InterruptEnable;
}
void HPET::enable(const HPETComparator& comparator)
{
#ifdef HPET_DEBUG
    klog() << "HPET: Enable comparator " << comparator.comparator_number() << ".";
#endif
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::InterruptEnable;
}

Vector<unsigned> HPET::capable_interrupt_numbers(const HPETComparator& comparator)
{
    ASSERT(comparator.comparator_number() <= m_comparators.size());
    Vector<unsigned> capable_interrupts;
    auto& comparator_registers = registers().timers[comparator.comparator_number()];
    u32 interrupt_bitfield = comparator_registers.interrupt_routing;
    for (size_t index = 0; index < 32; index++) {
        if (interrupt_bitfield & 1)
            capable_interrupts.append(index);
        interrupt_bitfield >>= 1;
    }
    return capable_interrupts;
}

Vector<unsigned> HPET::capable_interrupt_numbers(u8 comparator_number)
{
    ASSERT(comparator_number <= m_comparators.size());
    Vector<unsigned> capable_interrupts;
    auto& comparator_registers = registers().timers[comparator_number];
    u32 interrupt_bitfield = comparator_registers.interrupt_routing;
    for (size_t index = 0; index < 32; index++) {
        if (interrupt_bitfield & 1)
            capable_interrupts.append(index);
        interrupt_bitfield >>= 1;
    }
    return capable_interrupts;
}

void HPET::set_comparator_irq_vector(u8 comparator_number, u8 irq_vector)
{
    ASSERT(comparator_number <= m_comparators.size());
    auto& comparator_registers = registers().timers[comparator_number];
    comparator_registers.capabilities = comparator_registers.capabilities | (irq_vector << 9);
}

bool HPET::is_periodic_capable(u8 comparator_number) const
{
    ASSERT(comparator_number <= m_comparators.size());
    auto& comparator_registers = registers().timers[comparator_number];
    return comparator_registers.capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable;
}

void HPET::set_comparators_to_optimal_interrupt_state(size_t)
{
    // FIXME: Implement this method for allowing to use HPET timers 2-31...
    ASSERT_NOT_REACHED();
}

PhysicalAddress HPET::find_acpi_hpet_registers_block()
{
    auto sdt = map_typed<const volatile ACPI::Structures::HPET>(m_physical_acpi_hpet_table);
    ASSERT(sdt->event_timer_block.address_space == (u8)ACPI::GenericAddressStructure::AddressSpace::SystemMemory);
    return PhysicalAddress(sdt->event_timer_block.address);
}

const HPETRegistersBlock& HPET::registers() const
{
    return *(const HPETRegistersBlock*)m_hpet_mmio_region->vaddr().offset(m_physical_acpi_hpet_registers.offset_in_page()).as_ptr();
}

HPETRegistersBlock& HPET::registers()
{
    return *(HPETRegistersBlock*)m_hpet_mmio_region->vaddr().offset(m_physical_acpi_hpet_registers.offset_in_page()).as_ptr();
}

u64 HPET::calculate_ticks_in_nanoseconds() const
{
    // ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD == 100 nanoseconds
    return ((u64)registers().capabilities.main_counter_tick_period * 100ull) / ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD;
}

HPET::HPET(PhysicalAddress acpi_hpet)
    : m_physical_acpi_hpet_table(acpi_hpet)
    , m_physical_acpi_hpet_registers(find_acpi_hpet_registers_block())
    , m_hpet_mmio_region(MM.allocate_kernel_region(m_physical_acpi_hpet_registers.page_base(), PAGE_SIZE, "HPET MMIO", Region::Access::Read | Region::Access::Write))
{
    s_hpet = this; // Make available as soon as possible so that IRQs can use it

    auto sdt = map_typed<const volatile ACPI::Structures::HPET>(m_physical_acpi_hpet_table);
    m_vendor_id = sdt->pci_vendor_id;
    m_minimum_tick = sdt->mininum_clock_tick;
    klog() << "HPET: Minimum clock tick - " << m_minimum_tick;

    auto& regs = registers();

    // Note: We must do a 32 bit access to offsets 0x0, or 0x4 only.
    size_t timers_count = ((regs.capabilities.attributes >> 8) & 0x1f) + 1;
    klog() << "HPET: Timers count - " << timers_count;
    klog() << "HPET: Main counter size: " << ((regs.capabilities.attributes & (u32)HPETFlags::Attributes::Counter64BitCapable) ? "64 bit" : "32 bit");
    for (size_t i = 0; i < timers_count; i++) {
        bool capable_64_bit = regs.timers[i].capabilities & (u32)HPETFlags::TimerConfiguration::Timer64BitsCapable;
        klog() << "HPET: Timer[" << i << "] comparator size: " << (capable_64_bit ? "64 bit" : "32 bit") << " mode: " << ((!capable_64_bit || (regs.timers[i].capabilities & (u32)HPETFlags::TimerConfiguration::Force32BitMode)) ? "32 bit" : "64 bit");
    }
    ASSERT(timers_count >= 2);

    global_disable();

    m_frequency = NANOSECOND_PERIOD_TO_HERTZ(calculate_ticks_in_nanoseconds());
    klog() << "HPET: frequency " << m_frequency << " Hz (" << MEGAHERTZ_TO_HERTZ(m_frequency) << " MHz) resolution: " << calculate_ticks_in_nanoseconds() << "ns";
    ASSERT(regs.capabilities.main_counter_tick_period <= ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD);

    // Reset the counter, just in case... (needs to match m_main_counter_last_read)
    regs.main_counter_value.high = 0;
    regs.main_counter_value.low = 0;
    if (regs.capabilities.attributes & (u32)HPETFlags::Attributes::LegacyReplacementRouteCapable)
        regs.configuration.low = regs.configuration.low | (u32)HPETFlags::Configuration::LegacyReplacementRoute;

    m_comparators.append(HPETComparator::create(0, 0, is_periodic_capable(0)));
    m_comparators.append(HPETComparator::create(1, 8, is_periodic_capable(1)));

    global_enable();
}
}
