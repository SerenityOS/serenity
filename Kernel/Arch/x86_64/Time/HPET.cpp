/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/x86_64/Time/HPET.h>
#include <Kernel/Arch/x86_64/Time/HPETComparator.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

#define ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD 0x05F5E100
#define NANOSECOND_PERIOD_TO_HERTZ(x) 1000000000 / x
#define HERTZ_TO_MEGAHERTZ(x) (x / 1000000)

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
    union {
        u64 volatile full;
        struct {
            u32 volatile low;
            u32 volatile high;
        };
    };
};

struct [[gnu::packed]] TimerStructure {
    u32 volatile capabilities;
    u32 volatile interrupt_routing;
    HPETRegister comparator_value;
    u64 volatile fsb_interrupt_route;
    u64 reserved;
};

struct [[gnu::packed]] HPETCapabilityRegister {
    // Note: We must do a 32 bit access to offsets 0x0, or 0x4 only, according to HPET spec.
    u32 volatile attributes;
    u32 volatile main_counter_tick_period;
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
    TimerStructure timers[32];
};

static_assert(__builtin_offsetof(HPETRegistersBlock, main_counter_value) == 0xf0);
static_assert(__builtin_offsetof(HPETRegistersBlock, timers[0]) == 0x100);
static_assert(__builtin_offsetof(HPETRegistersBlock, timers[1]) == 0x120);

// Note: The HPET specification says it reserves the range of byte 0x160 to
// 0x400 for comparators 3-31, but for implementing all 32 comparators the HPET
// MMIO space has to be 1280 bytes and not 1024 bytes.
static_assert(AssertSize<HPETRegistersBlock, 0x500>());

static u64 read_register_safe64(HPETRegister const& reg)
{
    return reg.full;
}

static HPET* s_hpet;
static bool hpet_initialized { false };

bool HPET::initialized()
{
    return hpet_initialized;
}

HPET& HPET::the()
{
    VERIFY(HPET::initialized());
    VERIFY(s_hpet != nullptr);
    return *s_hpet;
}

UNMAP_AFTER_INIT bool HPET::test_and_initialize()
{
    VERIFY(!HPET::initialized());
    hpet_initialized = true;
    auto hpet_table = ACPI::Parser::the()->find_table("HPET"sv);
    if (!hpet_table.has_value())
        return false;
    dmesgln("HPET @ {}", hpet_table.value());

    auto sdt_or_error = Memory::map_typed<ACPI::Structures::HPET>(hpet_table.value());
    if (sdt_or_error.is_error()) {
        dbgln("Failed mapping HPET table");
        return false;
    }

    // Note: HPET is only usable from System Memory
    VERIFY(sdt_or_error.value()->event_timer_block.address_space == (u8)ACPI::GenericAddressStructure::AddressSpace::SystemMemory);

    if (TimeManagement::is_hpet_periodic_mode_allowed()) {
        if (!check_for_exisiting_periodic_timers()) {
            dbgln("HPET: No periodic capable timers");
            return false;
        }
    }
    new HPET(PhysicalAddress(hpet_table.value()));
    return true;
}

UNMAP_AFTER_INIT bool HPET::check_for_exisiting_periodic_timers()
{
    auto hpet_table = ACPI::Parser::the()->find_table("HPET"sv);
    if (!hpet_table.has_value())
        return false;

    auto sdt_or_error = Memory::map_typed<ACPI::Structures::HPET>(hpet_table.value());
    if (sdt_or_error.is_error())
        return false;
    auto sdt = sdt_or_error.release_value();
    VERIFY(sdt->event_timer_block.address_space == 0);
    auto registers_or_error = Memory::map_typed<HPETRegistersBlock>(PhysicalAddress(sdt->event_timer_block.address));
    if (registers_or_error.is_error())
        return false;
    auto registers = registers_or_error.release_value();

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
    if (m_main_counter_64bits)
        regs.main_counter_value.high = 0;
    for (auto& comparator : m_comparators) {
        auto& timer = regs.timers[comparator->comparator_number()];
        if (!comparator->is_enabled())
            continue;
        if (comparator->is_periodic()) {
            // Note that this means we're restarting all periodic timers. There is no
            // way to resume periodic timers properly because we reset the main counter
            // and we can only write the period into the comparator value...
            timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::ValueSet;
            u64 value = ns_to_raw_counter_ticks(1000000000ull / comparator->ticks_per_second());
            dbgln_if(HPET_DEBUG, "HPET: Update periodic comparator {} comparator value to {} main value was: {}",
                comparator->comparator_number(),
                value,
                previous_main_value);
            timer.comparator_value.low = (u32)value;
            if (comparator->is_64bit_capable()) {
                timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::ValueSet;
                timer.comparator_value.high = (u32)(value >> 32);
            }
        } else {
            // Set the new target comparator value to the delta to the remaining ticks
            u64 current_value = (u64)timer.comparator_value.low | ((u64)timer.comparator_value.high << 32);
            u64 value = current_value - previous_main_value;
            dbgln_if(HPET_DEBUG, "HPET: Update non-periodic comparator {} comparator value from {} to {} main value was: {}",
                comparator->comparator_number(),
                current_value,
                value,
                previous_main_value);
            timer.comparator_value.low = (u32)value;
            if (comparator->is_64bit_capable())
                timer.comparator_value.high = (u32)(value >> 32);
        }
    }

    global_enable();
}

void HPET::update_non_periodic_comparator_value(HPETComparator const& comparator)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(!comparator.is_periodic());
    VERIFY(comparator.comparator_number() <= m_comparators.size());
    auto& regs = registers();
    auto& timer = regs.timers[comparator.comparator_number()];
    u64 value = frequency() / comparator.ticks_per_second();
    // NOTE: If the main counter passes this new value before we finish writing it, we will never receive an interrupt!
    u64 new_counter_value = read_main_counter() + value;
    timer.comparator_value.high = (u32)(new_counter_value >> 32);
    timer.comparator_value.low = (u32)new_counter_value;
}

u64 HPET::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = read_main_counter();
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        if (m_main_counter_64bits) {
            delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
        } else {
            delta_ticks += (NumericLimits<u32>::max() - m_main_counter_last_read + 1) + current_value;
            m_32bit_main_counter_wraps++;
        }
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto ticks_per_second = frequency();
    seconds_since_boot += ticks_since_last_second / ticks_per_second;
    ticks_this_second = ticks_since_last_second % ticks_per_second;

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / ticks_per_second;
}

u64 HPET::read_main_counter_unsafe() const
{
    auto& main_counter = registers().main_counter_value;
    if (m_main_counter_64bits)
        return ((u64)main_counter.high << 32) | (u64)main_counter.low;

    return ((u64)m_32bit_main_counter_wraps << 32) | (u64)main_counter.low;
}

u64 HPET::read_main_counter() const
{
    if (m_main_counter_64bits)
        return read_register_safe64(registers().main_counter_value);

    auto& main_counter = registers().main_counter_value;
    u32 wraps = m_32bit_main_counter_wraps;
    u32 last_read_value = m_main_counter_last_read & 0xffffffff;
    u32 current_value = main_counter.low;
    if (current_value < last_read_value)
        wraps++;
    return ((u64)wraps << 32) | (u64)current_value;
}

void HPET::enable_periodic_interrupt(HPETComparator const& comparator)
{
    dbgln_if(HPET_DEBUG, "HPET: Set comparator {} to be periodic.", comparator.comparator_number());
    disable(comparator);
    VERIFY(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    auto capabilities = timer.capabilities;
    VERIFY(capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable);
    timer.capabilities = capabilities | (u32)HPETFlags::TimerConfiguration::GeneratePeriodicInterrupt;
    if (comparator.is_enabled())
        enable(comparator);
}
void HPET::disable_periodic_interrupt(HPETComparator const& comparator)
{
    dbgln_if(HPET_DEBUG, "HPET: Disable periodic interrupt in comparator {}", comparator.comparator_number());
    disable(comparator);
    VERIFY(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    auto capabilities = timer.capabilities;
    VERIFY(capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable);
    timer.capabilities = capabilities & ~(u32)HPETFlags::TimerConfiguration::GeneratePeriodicInterrupt;
    if (comparator.is_enabled())
        enable(comparator);
}

void HPET::disable(HPETComparator const& comparator)
{
    dbgln_if(HPET_DEBUG, "HPET: Disable comparator {}", comparator.comparator_number());
    VERIFY(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    timer.capabilities = timer.capabilities & ~(u32)HPETFlags::TimerConfiguration::InterruptEnable;
}
void HPET::enable(HPETComparator const& comparator)
{
    dbgln_if(HPET_DEBUG, "HPET: Enable comparator {}", comparator.comparator_number());
    VERIFY(comparator.comparator_number() <= m_comparators.size());
    auto& timer = registers().timers[comparator.comparator_number()];
    timer.capabilities = timer.capabilities | (u32)HPETFlags::TimerConfiguration::InterruptEnable;
}

Vector<unsigned> HPET::capable_interrupt_numbers(HPETComparator const& comparator)
{
    VERIFY(comparator.comparator_number() <= m_comparators.size());
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
    VERIFY(comparator_number <= m_comparators.size());
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
    VERIFY(comparator_number <= m_comparators.size());
    auto& comparator_registers = registers().timers[comparator_number];
    comparator_registers.capabilities = comparator_registers.capabilities | (irq_vector << 9);
}

bool HPET::is_periodic_capable(u8 comparator_number) const
{
    VERIFY(comparator_number <= m_comparators.size());
    auto& comparator_registers = registers().timers[comparator_number];
    return comparator_registers.capabilities & (u32)HPETFlags::TimerConfiguration::PeriodicInterruptCapable;
}

bool HPET::is_64bit_capable(u8 comparator_number) const
{
    VERIFY(comparator_number <= m_comparators.size());
    auto& comparator_registers = registers().timers[comparator_number];
    return comparator_registers.capabilities & (u32)HPETFlags::TimerConfiguration::Timer64BitsCapable;
}

void HPET::set_comparators_to_optimal_interrupt_state(size_t)
{
    // FIXME: Implement this method for allowing to use HPET timers 2-31...
    VERIFY_NOT_REACHED();
}

PhysicalAddress HPET::find_acpi_hpet_registers_block()
{
    auto sdt = Memory::map_typed<const volatile ACPI::Structures::HPET>(m_physical_acpi_hpet_table).release_value_but_fixme_should_propagate_errors();
    VERIFY(sdt->event_timer_block.address_space == (u8)ACPI::GenericAddressStructure::AddressSpace::SystemMemory);
    return PhysicalAddress(sdt->event_timer_block.address);
}

HPETRegistersBlock const& HPET::registers() const
{
    return *(HPETRegistersBlock const*)m_hpet_mmio_region->vaddr().offset(m_physical_acpi_hpet_registers.offset_in_page()).as_ptr();
}

HPETRegistersBlock& HPET::registers()
{
    return *(HPETRegistersBlock*)m_hpet_mmio_region->vaddr().offset(m_physical_acpi_hpet_registers.offset_in_page()).as_ptr();
}

u64 HPET::raw_counter_ticks_to_ns(u64 raw_ticks) const
{
    // ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD == 100 nanoseconds
    return (raw_ticks * (u64)registers().capabilities.main_counter_tick_period * 100ull) / ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD;
}

u64 HPET::ns_to_raw_counter_ticks(u64 ns) const
{
    return (ns * 1000000ull) / (u64)registers().capabilities.main_counter_tick_period;
}

UNMAP_AFTER_INIT HPET::HPET(PhysicalAddress acpi_hpet)
    : m_physical_acpi_hpet_table(acpi_hpet)
    , m_physical_acpi_hpet_registers(find_acpi_hpet_registers_block())
    , m_hpet_mmio_region(MM.allocate_mmio_kernel_region(m_physical_acpi_hpet_registers.page_base(), PAGE_SIZE, "HPET MMIO"sv, Memory::Region::Access::ReadWrite).release_value())
{
    s_hpet = this; // Make available as soon as possible so that IRQs can use it

    auto sdt = Memory::map_typed<const volatile ACPI::Structures::HPET>(m_physical_acpi_hpet_table).release_value_but_fixme_should_propagate_errors();
    m_vendor_id = sdt->pci_vendor_id;
    m_minimum_tick = sdt->mininum_clock_tick;
    dmesgln("HPET: Minimum clock tick - {}", m_minimum_tick);

    auto& regs = registers();

    // Note: We must do a 32 bit access to offsets 0x0, or 0x4 only.
    size_t timers_count = ((regs.capabilities.attributes >> 8) & 0x1f) + 1;
    m_main_counter_64bits = (regs.capabilities.attributes & (u32)HPETFlags::Attributes::Counter64BitCapable) != 0;
    dmesgln("HPET: Timers count - {}", timers_count);
    dmesgln("HPET: Main counter size: {}", (m_main_counter_64bits ? "64-bit" : "32-bit"));
    for (size_t i = 0; i < timers_count; i++) {
        bool capable_64_bit = regs.timers[i].capabilities & (u32)HPETFlags::TimerConfiguration::Timer64BitsCapable;
        dmesgln("HPET: Timer[{}] comparator size: {}, mode: {}", i,
            (capable_64_bit ? "64-bit" : "32-bit"),
            ((!capable_64_bit || (regs.timers[i].capabilities & (u32)HPETFlags::TimerConfiguration::Force32BitMode)) ? "32-bit" : "64-bit"));
    }
    VERIFY(timers_count >= 2);

    global_disable();

    m_frequency = NANOSECOND_PERIOD_TO_HERTZ(raw_counter_ticks_to_ns(1));
    dmesgln("HPET: frequency {} Hz ({} MHz) resolution: {} ns", m_frequency, HERTZ_TO_MEGAHERTZ(m_frequency), raw_counter_ticks_to_ns(1));

    VERIFY(regs.capabilities.main_counter_tick_period <= ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD);

    // Reset the counter, just in case... (needs to match m_main_counter_last_read)
    regs.main_counter_value.high = 0;
    regs.main_counter_value.low = 0;
    if (regs.capabilities.attributes & (u32)HPETFlags::Attributes::LegacyReplacementRouteCapable)
        regs.configuration.low = regs.configuration.low | (u32)HPETFlags::Configuration::LegacyReplacementRoute;

    m_comparators.append(HPETComparator::create(0, 0, is_periodic_capable(0), is_64bit_capable(0)));
    m_comparators.append(HPETComparator::create(1, 8, is_periodic_capable(1), is_64bit_capable(1)));

    global_enable();
}
}
