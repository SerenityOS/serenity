/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Panic.h>

extern "C" uintptr_t vector_table_el1;

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
    saved_program_status_register_el3.M = Aarch64::SPSR_EL3::Mode::EL2t;

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

    // Set up initial exception stack
    // FIXME: Define in linker script
    Aarch64::Asm::set_sp_el1(0x40000);

    Aarch64::SPSR_EL2 saved_program_status_register_el2 = {};

    // Mask (disable) all interrupts
    saved_program_status_register_el2.A = 1;
    saved_program_status_register_el2.I = 1;
    saved_program_status_register_el2.F = 1;

    // Indicate EL1 as exception origin mode (so we go back there)
    saved_program_status_register_el2.M = Aarch64::SPSR_EL2::Mode::EL1t;

    Aarch64::SPSR_EL2::write(saved_program_status_register_el2);
    Aarch64::Asm::enter_el1_from_el2();
}

static void setup_el1()
{
    Aarch64::SCTLR_EL1 system_control_register_el1 = Aarch64::SCTLR_EL1::reset_value();

    system_control_register_el1.UCT = 1;  // Don't trap access to CTR_EL0
    system_control_register_el1.nTWE = 1; // Don't trap WFE instructions
    system_control_register_el1.nTWI = 1; // Don't trap WFI instructions
    system_control_register_el1.DZE = 1;  // Don't trap DC ZVA instructions
    system_control_register_el1.UMA = 1;  // Don't trap access to DAIF (debugging) flags of EFLAGS register
    system_control_register_el1.SA0 = 1;  // Enable stack access alignment check for EL0
    system_control_register_el1.SA = 1;   // Enable stack access alignment check for EL1
    system_control_register_el1.A = 1;    // Enable memory access alignment check

    Aarch64::SCTLR_EL1::write(system_control_register_el1);

    Aarch64::Asm::load_el1_vector_table(&vector_table_el1);
}

void initialize_exceptions(u32 cpu)
{
    auto base_exception_level = Aarch64::Asm::get_current_exception_level();

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL3) {
        PANIC("CPU[{}]: Started in unknown EL{}", cpu, static_cast<u8>(base_exception_level));
    } else if (base_exception_level < Aarch64::Asm::ExceptionLevel::EL1) {
        PANIC("CPU[{}]: Started in unsupported EL{}", cpu, static_cast<u8>(base_exception_level));
    } else {
        dbgln("CPU[{}]: Started in EL{}", cpu, static_cast<u8>(base_exception_level));
    }

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL2) {
        drop_el3_to_el2();
        dbgln("CPU[{}]: Dropped to EL2", cpu);
    }

    if (base_exception_level > Aarch64::Asm::ExceptionLevel::EL1) {
        drop_el2_to_el1();
        dbgln("CPU[{}]: Dropped to EL1", cpu);
    }

    setup_el1();
    dbgln("CPU[{}]: Set up EL1", cpu);
}

}
