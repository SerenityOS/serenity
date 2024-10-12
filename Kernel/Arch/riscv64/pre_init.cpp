/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/pre_init.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/MMU.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Sections.h>

#include <LibELF/Relocation.h>

namespace Kernel {

UNMAP_AFTER_INIT void dbgln_without_mmu(StringView message)
{
    auto probe_result = SBI::Base::probe_extension(SBI::EID::DebugConsole);
    if (probe_result.is_error() || probe_result.value() == 0) {
        for (auto const ch : message.bytes())
            (void)SBI::Legacy::console_putchar(ch);
        (void)SBI::Legacy::console_putchar('\n');
    } else {
        for (auto const ch : message.bytes())
            (void)SBI::DBCN::debug_console_write_byte(ch);
        (void)SBI::DBCN::debug_console_write_byte('\n');
    }
}

[[noreturn]] UNMAP_AFTER_INIT void panic_without_mmu(StringView message)
{
    dbgln_without_mmu("KERNEL PANIC in pre_init :^("sv);
    dbgln_without_mmu(message);

    // We can't use Processor::halt() here, as that would result in an absolute jump.
    RISCV64::CSR::write(RISCV64::CSR::Address::SIE, 0);
    for (;;)
        asm volatile("wfi");
}

[[gnu::aligned(4)]] [[noreturn]] UNMAP_AFTER_INIT static void early_trap_handler()
{
    panic_without_mmu("Unexpected trap"sv);
}

static UNMAP_AFTER_INIT PhysicalPtr physical_load_base()
{
    PhysicalPtr physical_load_base;

    asm volatile(
        "lla %[physical_load_base], start_of_kernel_image\n"
        : [physical_load_base] "=r"(physical_load_base));

    return physical_load_base;
}

static UNMAP_AFTER_INIT PhysicalPtr dynamic_section_addr()
{
    PhysicalPtr dynamic_section_addr;

    // Use lla explicitly to prevent a GOT load.
    asm volatile(
        "lla %[dynamic_section_addr], _DYNAMIC\n"
        : [dynamic_section_addr] "=r"(dynamic_section_addr));

    return dynamic_section_addr;
}

extern "C" [[noreturn]] UNMAP_AFTER_INIT void pre_init(FlatPtr boot_hart_id, PhysicalPtr flattened_devicetree_paddr)
{
    // Apply relative relocations as if we were running at KERNEL_MAPPING_BASE.
    // This means that all global variables must be accessed with adjust_by_mapping_base, since we are still running identity mapped.
    // Otherwise, we would have to relocate twice: once while running identity mapped, and again when we enable the MMU.
    if (!ELF::perform_relative_relocations(physical_load_base(), KERNEL_MAPPING_BASE, dynamic_section_addr()))
        panic_without_mmu("Failed to perform relative relocations"sv);

    // Catch traps in pre_init
    RISCV64::CSR::write(RISCV64::CSR::Address::STVEC, bit_cast<FlatPtr>(&early_trap_handler));

    Memory::init_page_tables_and_jump_to_init(boot_hart_id, flattened_devicetree_paddr);
}

}
