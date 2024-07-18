/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Library/Panic.h>

namespace Kernel {

static void drop_el3_to_el2()
{
    Aarch64::SCR_EL3 secure_configuration_register_el3 = {};

    secure_configuration_register_el3.ST = 1;  // Don't trap access to Counter-timer Physical Secure registers
    secure_configuration_register_el3.RW = 1;  // Lower level to use Aarch64
    secure_configuration_register_el3.NS = 1;  // Non-secure state
    secure_configuration_register_el3.HCE = 1; // Enable Hypervisor instructions at all levels

    Aarch64::SCR_EL3::write(secure_configuration_register_el3);

    Aarch64::SPSR_EL3 saved_program_status_register_el3 = {};

    // Mask (disable) all interrupts
    saved_program_status_register_el3.A = 1;
    saved_program_status_register_el3.I = 1;
    saved_program_status_register_el3.F = 1;
    saved_program_status_register_el3.D = 1;

    // Indicate EL1 as exception origin mode (so we go back there)
    saved_program_status_register_el3.M = Aarch64::SPSR_EL3::Mode::EL2h;

    // Set the register
    Aarch64::SPSR_EL3::write(saved_program_status_register_el3);

    // This will jump into os_start() below
    Aarch64::Asm::enter_el2_from_el3();
}

static void drop_el2_to_el1()
{
    Aarch64::HCR_EL2 hypervisor_configuration_register_el2 = {};
    hypervisor_configuration_register_el2.RW = 1; // EL1 to use 64-bit mode
    Aarch64::HCR_EL2::write(hypervisor_configuration_register_el2);

    Aarch64::SPSR_EL2 saved_program_status_register_el2 = {};

    // Mask (disable) all interrupts
    saved_program_status_register_el2.A = 1;
    saved_program_status_register_el2.I = 1;
    saved_program_status_register_el2.F = 1;

    // Indicate EL1 as exception origin mode (so we go back there)
    saved_program_status_register_el2.M = Aarch64::SPSR_EL2::Mode::EL1h;

    Aarch64::SPSR_EL2::write(saved_program_status_register_el2);
    Aarch64::Asm::enter_el1_from_el2();
}

static void setup_el1()
{
    Aarch64::SCTLR_EL1 system_control_register_el1 = Aarch64::SCTLR_EL1::reset_value();

    // FIXME: Enable memory access alignment check when userspace will not execute unaligned memory accesses anymore.
    //        See: https://github.com/SerenityOS/serenity/issues/17516
    system_control_register_el1.A = 0; // Disable memory access alignment check

    Aarch64::SCTLR_EL1::write(system_control_register_el1);

    Aarch64::CPACR_EL1 cpacr_el1 = {};
    cpacr_el1.ZEN = 0;     // Trap SVE instructions at EL1 and EL0
    cpacr_el1.FPEN = 0b11; // Don't trap Advanced SIMD and floating-point instructions
    cpacr_el1.SMEN = 0;    // Trap SME instructions at EL1 and EL0
    cpacr_el1.TTA = 0;     // Don't trap access to trace registers
    Aarch64::CPACR_EL1::write(cpacr_el1);
}

void initialize_exceptions()
{
    auto base_exception_level = Aarch64::Asm::get_current_exception_level();

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL3) {
        panic_without_mmu("Started in unknown EL (Greater than EL3)"sv);
    } else if (base_exception_level < Aarch64::Asm::ExceptionLevel::EL1) {
        panic_without_mmu("Started in unsupported EL (Less than EL1)"sv);
    } else {
        if (base_exception_level == Aarch64::Asm::ExceptionLevel::EL1)
            dbgln_without_mmu("Started in EL1"sv);
        else if (base_exception_level == Aarch64::Asm::ExceptionLevel::EL2)
            dbgln_without_mmu("Started in EL2"sv);
        else if (base_exception_level == Aarch64::Asm::ExceptionLevel::EL3)
            dbgln_without_mmu("Started in EL3"sv);
    }

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL2) {
        drop_el3_to_el2();
        dbgln_without_mmu("Dropped to EL2"sv);
    }

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL1) {
        drop_el2_to_el1();
        dbgln_without_mmu("Dropped to EL1"sv);
    }

    setup_el1();
    dbgln_without_mmu("Set up EL1"sv);
}

// NOTE: The normal PANIC macro cannot be used early in the boot process when the MMU is disabled,
//       as it will access global variables, which will cause a crash since they aren't mapped yet.
void panic_without_mmu(StringView message)
{
    (void)message;
    // FIXME: Print out message to early boot console.
    Processor::halt();
}

void dbgln_without_mmu(StringView message)
{
    (void)message;
    // FIXME: Print out message to early boot console.
}

}
