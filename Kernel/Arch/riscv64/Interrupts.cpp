/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Types.h>

#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/PageFault.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>

namespace Kernel {

extern "C" void syscall_handler(TrapFrame const*);

// FIXME: Share this array with x86_64/aarch64 somehow and consider if this really needs to use raw pointers and not OwnPtrs
static Array<GenericInterruptHandler*, 64> s_interrupt_handlers;

void dump_registers(RegisterState const& regs)
{
    dbgln("scause:  {} ({:p})", regs.scause, to_underlying(regs.scause));
    dbgln("sepc:    {:p}", regs.sepc);
    dbgln("stval:   {:p}", regs.stval);
    dbgln("sstatus: {}", regs.sstatus);

    dbgln("ra={:p} sp={:p} gp={:p} tp={:p} fp={:p}", regs.x[0], regs.x[1], regs.x[2], regs.x[3], regs.x[7]);
    dbgln("a0={:p} a1={:p} a2={:p} a3={:p} a4={:p} a5={:p} a6={:p} a7={:p}", regs.x[9], regs.x[10], regs.x[11], regs.x[12], regs.x[13], regs.x[14], regs.x[15], regs.x[16]);
    dbgln("t0={:p} t1={:p} t2={:p} t3={:p} t4={:p} t5={:p} t6={:p}", regs.x[4], regs.x[5], regs.x[6], regs.x[27], regs.x[28], regs.x[29], regs.x[30]);
    dbgln("s1={:p} s2={:p} s3={:p} s4={:p} s5={:p} s6={:p} s7={:p} s8={:p} s9={:p} s10={:p} s11={:p}", regs.x[8], regs.x[17], regs.x[18], regs.x[19], regs.x[20], regs.x[21], regs.x[22], regs.x[23], regs.x[24], regs.x[25], regs.x[26]);
}

extern "C" void trap_handler(TrapFrame& trap_frame);
extern "C" void trap_handler(TrapFrame& trap_frame)
{
    auto scause = trap_frame.regs->scause;

    if ((to_underlying(scause) & RISCV64::CSR::SCAUSE_INTERRUPT_MASK) != 0) {
        // Interrupt

        Processor::current().enter_trap(trap_frame, true);

        auto interrupt_number = to_underlying(scause) & ~RISCV64::CSR::SCAUSE_INTERRUPT_MASK;

        auto* handler = s_interrupt_handlers[interrupt_number];
        VERIFY(handler);
        handler->increment_call_count();
        handler->handle_interrupt(*trap_frame.regs);
        handler->eoi();

        Processor::current().exit_trap(trap_frame);
    } else {
        // Exception

        Processor::current().enter_trap(trap_frame, false);
        if (trap_frame.regs->sstatus.SPIE == 1)
            Processor::enable_interrupts();

        using enum RISCV64::CSR::SCAUSE;
        switch (scause) {
        case InstructionAddressMisaligned:
        case LoadAddressMisaligned:
        case StoreOrAMOAddressMisaligned:
            handle_crash(*trap_frame.regs, "Unaligned memory access", SIGBUS, false);
            break;

        case InstructionAccessFault:
        case LoadAccessFault:
        case StoreOrAMOAccessFault:
            handle_crash(*trap_frame.regs, "Memory access fault", SIGBUS, false);
            break;

        case IllegalInstrction:
            handle_crash(*trap_frame.regs, "Illegal instruction", SIGILL, false);
            break;

        case InstructionPageFault:
        case LoadPageFault:
        case StoreOrAMOPageFault: {
            // The privileged ISA theoretically allows stval to always be zero (in which case we would report a page fault in the zero page).
            // But all implementations capable of running general purpose operating systems should probably set this CSR,
            // as otherwise you can't handle page faults.
            // We simply require that Sstvala (see RISC-V Profiles) is supported, which means stval is always set to the faulting address on a page fault.
            PageFault fault { VirtualAddress(trap_frame.regs->stval) };

            if (scause == InstructionPageFault)
                fault.set_instruction_fetch(true);
            else if (scause == LoadPageFault)
                fault.set_access(PageFault::Access::Read);
            else if (scause == StoreOrAMOPageFault)
                fault.set_access(PageFault::Access::Write);

            // RISC-V doesn't tell you the reason why a page fault occurred, so we don't use PageFault::set_type() here.
            // The RISC-V implementation of Region::handle_fault() works without a correct PageFault::type().

            fault.handle(*trap_frame.regs);
            break;
        }

        case EnvironmentCallFromUMode:
            trap_frame.regs->sepc += 4;
            syscall_handler(&trap_frame);
            break;

        case Breakpoint:
            TODO_RISCV64();
            break;

        default:
            VERIFY_NOT_REACHED();
        };

        Processor::disable_interrupts();
        Processor::current().exit_trap(trap_frame);
    }
}

// FIXME: Share the code below with Arch/x86_64/Interrupts.cpp
//        While refactoring, the interrupt handlers can also be moved into the InterruptManagement class.
GenericInterruptHandler& get_interrupt_handler(u8)
{
    TODO_RISCV64();
}

// Sets the reserved flag on `number_of_irqs` if it finds unused interrupt handler on
// a contiguous range.
ErrorOr<u8> reserve_interrupt_handlers(u8)
{
    TODO_RISCV64();
}

static void revert_to_unused_handler(u8 interrupt_number)
{
    auto* handler = new UnhandledInterruptHandler(interrupt_number);
    handler->register_interrupt_handler();
}

// FIXME: Share the code below with Arch/{x86_64,aarch64}/Interrupts.cpp
void register_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    auto*& handler_slot = s_interrupt_handlers[interrupt_number];
    if (handler_slot == nullptr) {
        handler_slot = &handler;
        return;
    }
    if (handler_slot->type() == HandlerType::UnhandledInterruptHandler) {
        auto* unhandled_handler = static_cast<UnhandledInterruptHandler*>(handler_slot);
        unhandled_handler->unregister_interrupt_handler();
        delete unhandled_handler;
        handler_slot = &handler;
        return;
    }
    if (handler_slot->is_shared_handler()) {
        VERIFY(handler_slot->type() == HandlerType::SharedIRQHandler);
        static_cast<SharedIRQHandler*>(handler_slot)->register_handler(handler);
        return;
    }
    if (!handler_slot->is_shared_handler()) {
        if (handler_slot->type() == HandlerType::SpuriousInterruptHandler) {
            // FIXME: Add support for spurious interrupts on riscv64
            TODO_RISCV64();
        }
        VERIFY(handler_slot->type() == HandlerType::IRQHandler);
        auto& previous_handler = *handler_slot;
        handler_slot = nullptr;
        SharedIRQHandler::initialize(interrupt_number);
        VERIFY(handler_slot);
        static_cast<SharedIRQHandler*>(handler_slot)->register_handler(previous_handler);
        static_cast<SharedIRQHandler*>(handler_slot)->register_handler(handler);
        return;
    }
    VERIFY_NOT_REACHED();
}

// FIXME: Share the code below with Arch/{x86_64,aarch64}/Interrupts.cpp
void unregister_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    auto*& handler_slot = s_interrupt_handlers[interrupt_number];
    VERIFY(handler_slot != nullptr);
    if (handler_slot->type() == HandlerType::UnhandledInterruptHandler)
        return;
    if (handler_slot->is_shared_handler()) {
        VERIFY(handler_slot->type() == HandlerType::SharedIRQHandler);
        auto* shared_handler = static_cast<SharedIRQHandler*>(handler_slot);
        shared_handler->unregister_handler(handler);
        if (shared_handler->sharing_devices_count() == 0) {
            handler_slot = nullptr;
            revert_to_unused_handler(interrupt_number);
        }
        return;
    }
    if (!handler_slot->is_shared_handler()) {
        VERIFY(handler_slot->type() == HandlerType::IRQHandler);
        handler_slot = nullptr;
        revert_to_unused_handler(interrupt_number);
        return;
    }
}

void initialize_interrupts()
{
    for (size_t i = 0; i < s_interrupt_handlers.size(); ++i) {
        auto* handler = new UnhandledInterruptHandler(i);
        handler->register_interrupt_handler();
    }
}

}
