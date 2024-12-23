/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/PageFault.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel {

extern "C" void syscall_handler(TrapFrame const*);

static void dump_exception_syndrome_register(Aarch64::ESR_EL1 const& esr_el1)
{
    dbgln("Exception Syndrome: EC({:#b}) IL({:#b}) ISS({:#b}) ISS2({:#b})", esr_el1.EC, esr_el1.IL, esr_el1.ISS, esr_el1.ISS2);
    dbgln("    Class: {}", Aarch64::exception_class_to_string(esr_el1.EC));

    if (Aarch64::exception_class_is_data_abort(esr_el1.EC))
        dbgln("    Data Fault Status Code: {}", Aarch64::data_fault_status_code_to_string(esr_el1.ISS));
    if (Aarch64::exception_class_has_set_far(esr_el1.EC))
        dbgln("    Faulting Virtual Address: {:#x}", Aarch64::FAR_EL1::read().virtual_address);
}

void dump_registers(RegisterState const& regs)
{
    auto esr_el1 = bit_cast<Aarch64::ESR_EL1>(regs.esr_el1);
    dump_exception_syndrome_register(esr_el1);

    // Special registers
    dbgln("Saved Program Status: (NZCV({:#b}) DAIF({:#b}) M({:#b})) / {:#x}", ((regs.spsr_el1 >> 28) & 0b1111), ((regs.spsr_el1 >> 6) & 0b1111), regs.spsr_el1 & 0b1111, regs.spsr_el1);
    dbgln("Exception Link Register: {:#x}", regs.elr_el1);
    dbgln("Stack Pointer (EL0): {:#x}", regs.sp_el0);
    dbgln("Software Thread ID Register (EL0): {:#x}", regs.tpidr_el0);

    dbgln(" x0={:p}  x1={:p}  x2={:p}  x3={:p}  x4={:p}", regs.x[0], regs.x[1], regs.x[2], regs.x[3], regs.x[4]);
    dbgln(" x5={:p}  x6={:p}  x7={:p}  x8={:p}  x9={:p}", regs.x[5], regs.x[6], regs.x[7], regs.x[8], regs.x[9]);
    dbgln("x10={:p} x11={:p} x12={:p} x13={:p} x14={:p}", regs.x[10], regs.x[11], regs.x[12], regs.x[13], regs.x[14]);
    dbgln("x15={:p} x16={:p} x17={:p} x18={:p} x19={:p}", regs.x[15], regs.x[16], regs.x[17], regs.x[18], regs.x[19]);
    dbgln("x20={:p} x21={:p} x22={:p} x23={:p} x24={:p}", regs.x[20], regs.x[21], regs.x[22], regs.x[23], regs.x[24]);
    dbgln("x25={:p} x26={:p} x27={:p} x28={:p} x29={:p}", regs.x[25], regs.x[26], regs.x[27], regs.x[28], regs.x[29]);
    dbgln("x30={:p}", regs.x[30]);
}

static ErrorOr<PageFault> page_fault_from_exception_syndrome_register(VirtualAddress fault_address, Aarch64::ESR_EL1 esr_el1)
{
    PageFault fault { fault_address };

    u8 data_fault_status_code = esr_el1.ISS & 0x3f;
    if (data_fault_status_code >= 0b001100 && data_fault_status_code <= 0b001111) {
        fault.set_type(PageFault::Type::ProtectionViolation);
    } else if (data_fault_status_code >= 0b000100 && data_fault_status_code <= 0b000111) {
        fault.set_type(PageFault::Type::PageNotPresent);
    } else {
        dbgln("Unknown DFSC: {}", Aarch64::data_fault_status_code_to_string(esr_el1.ISS));
        return Error::from_errno(EFAULT);
    }

    fault.set_access((esr_el1.ISS & (1 << 6)) == (1 << 6) ? PageFault::Access::Write : PageFault::Access::Read);

    fault.set_mode(Aarch64::exception_class_is_data_or_instruction_abort_from_lower_exception_level(esr_el1.EC) ? ExecutionMode::User : ExecutionMode::Kernel);

    if (Aarch64::exception_class_is_instruction_abort(esr_el1.EC))
        fault.set_instruction_fetch(true);

    return fault;
}

extern "C" void exception_common(Kernel::TrapFrame* trap_frame);
extern "C" void exception_common(Kernel::TrapFrame* trap_frame)
{
    Processor::current().enter_trap(*trap_frame, false);

    auto esr_el1 = bit_cast<Aarch64::ESR_EL1>(trap_frame->regs->esr_el1);
    auto fault_address = Aarch64::FAR_EL1::read().virtual_address;
    Processor::enable_interrupts();

    if (Aarch64::exception_class_is_data_abort(esr_el1.EC) || Aarch64::exception_class_is_instruction_abort(esr_el1.EC)) {
        auto page_fault_or_error = page_fault_from_exception_syndrome_register(VirtualAddress(fault_address), esr_el1);
        if (page_fault_or_error.is_error()) {
            handle_crash(*trap_frame->regs, "Unknown page fault", SIGSEGV, false);
        } else {
            auto page_fault = page_fault_or_error.release_value();
            page_fault.handle(*trap_frame->regs);
        }
    } else if (Aarch64::exception_class_is_svc_instruction_execution(esr_el1.EC)) {
        syscall_handler(trap_frame);
    } else {
        handle_crash(*trap_frame->regs, "Unexpected exception", SIGSEGV, false);
    }

    Processor::disable_interrupts();
    Processor::current().exit_trap(*trap_frame);
}

// This spinlock is used to reserve IRQs that can be later used by interrupt mechanism such as MSIx
static Spinlock<LockRank::None> s_interrupt_handler_lock {};
// A GICv2 supports a maximum of 1020 interrupts.
static Array<GenericInterruptHandler*, 1020> s_interrupt_handlers;

static bool is_unused_handler(GenericInterruptHandler* handler_slot)
{
    return (handler_slot->type() == HandlerType::UnhandledInterruptHandler) && !handler_slot->reserved();
}

extern "C" void handle_interrupt(TrapFrame&);
extern "C" void handle_interrupt(TrapFrame& trap_frame)
{
    Processor::current().enter_trap(trap_frame, true);

    for (auto& interrupt_controller : InterruptManagement::the().controllers()) {
        // TODO: Add these interrupts as a source of entropy for randomness.
        for (;;) {
            auto maybe_irq = interrupt_controller->pending_interrupt();
            if (!maybe_irq.has_value())
                break;

            auto* handler = s_interrupt_handlers[maybe_irq.value()];
            VERIFY(handler);
            handler->increment_call_count();
            handler->handle_interrupt();
            handler->eoi();
        }
    }

    Processor::current().exit_trap(trap_frame);
}

// FIXME: Share the code below with Arch/x86_64/Interrupts.cpp
//        While refactoring, the interrupt handlers can also be moved into the InterruptManagement class.
GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number)
{
    auto*& handler_slot = s_interrupt_handlers[interrupt_number];
    VERIFY(handler_slot != nullptr);
    return *handler_slot;
}

static void revert_to_unused_handler(u8 interrupt_number)
{
    auto handler = new UnhandledInterruptHandler(interrupt_number);
    handler->register_interrupt_handler();
}

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
            // FIXME: Add support for spurious interrupts on aarch64
            TODO_AARCH64();
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
        if (!shared_handler->sharing_devices_count()) {
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

// Sets the reserved flag on `number_of_irqs` if it finds unused interrupt handler on
// a contiguous range.
// FIXME: Share the code below with Arch/x86_64/Interrupts.cpp.
ErrorOr<u8> reserve_interrupt_handlers(u8 number_of_irqs)
{
    bool found_range = false;
    u8 first_irq = 0;
    SpinlockLocker locker(s_interrupt_handler_lock);
    for (size_t start_irq = 0; start_irq < s_interrupt_handlers.size(); start_irq++) {
        auto*& handler_slot = s_interrupt_handlers[start_irq];
        VERIFY(handler_slot != nullptr);

        if (!is_unused_handler(handler_slot))
            continue;

        found_range = true;
        for (auto off = 1; off < number_of_irqs; off++) {
            auto*& handler = s_interrupt_handlers[start_irq + off];
            VERIFY(handler_slot != nullptr);

            if (!is_unused_handler(handler)) {
                found_range = false;
                break;
            }
        }

        if (found_range == true) {
            first_irq = start_irq;
            break;
        }
    }

    if (!found_range)
        return Error::from_errno(EAGAIN);

    for (auto irq = first_irq; irq < number_of_irqs; irq++) {
        auto*& handler_slot = s_interrupt_handlers[irq];
        handler_slot->set_reserved();
    }

    return first_irq;
}

}
