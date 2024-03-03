/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/PageFault.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

void PageFault::handle(RegisterState& regs)
{
    auto fault_address = m_vaddr.get();
    bool faulted_in_kernel = regs.previous_mode() == ExecutionMode::Kernel;

    if (faulted_in_kernel && Processor::current_in_irq()) {
        // If we're faulting in an IRQ handler, first check if we failed
        // due to safe_memcpy, safe_strnlen, or safe_memset. If we did,
        // gracefully continue immediately. Because we're in an IRQ handler
        // we can't really try to resolve the page fault in a meaningful
        // way, so we need to do this before calling into
        // MemoryManager::handle_page_fault, which would just bail and
        // request a crash
        if (handle_safe_access_fault(regs, fault_address))
            return;
    }

    auto current_thread = Thread::current();

    if (current_thread) {
        current_thread->set_handling_page_fault(true);
        PerformanceManager::add_page_fault_event(*current_thread, regs);
    }

    ScopeGuard guard = [current_thread] {
        if (current_thread)
            current_thread->set_handling_page_fault(false);
    };

    if (!faulted_in_kernel) {
        VirtualAddress userspace_sp = VirtualAddress { regs.userspace_sp() };
        bool has_valid_stack_pointer = current_thread->process().address_space().with([&](auto& space) {
            return MM.validate_user_stack(*space, userspace_sp);
        });
        if (!has_valid_stack_pointer) {
            dbgln("Invalid stack pointer: {}", userspace_sp);
            return handle_crash(regs, "Bad stack on page fault", SIGSEGV);
        }
    }

    auto response = MM.handle_page_fault(*this);

    if (response == PageFaultResponse::ShouldCrash || response == PageFaultResponse::OutOfMemory || response == PageFaultResponse::BusError) {
        if (faulted_in_kernel && handle_safe_access_fault(regs, fault_address)) {
            // If this would be a ring0 (kernel) fault and the fault was triggered by
            // safe_memcpy, safe_strnlen, or safe_memset then we resume execution at
            // the appropriate _fault label rather than crashing
            return;
        }

        if (response == PageFaultResponse::BusError && current_thread->has_signal_handler(SIGBUS)) {
            current_thread->send_urgent_signal_to_self(SIGBUS);
            return;
        }

        if (response != PageFaultResponse::OutOfMemory && current_thread) {
            if (current_thread->has_signal_handler(SIGSEGV)) {
                current_thread->send_urgent_signal_to_self(SIGSEGV);
                return;
            }
        }

        dbgln("Unrecoverable page fault, {}{}{} address {}",
            is_reserved_bit_violation() ? "reserved bit violation / " : "",
            is_instruction_fetch() ? "instruction fetch / " : "",
            is_write() ? "write to" : "read from",
            VirtualAddress(fault_address));
        constexpr FlatPtr kmalloc_scrub_pattern = explode_byte(KMALLOC_SCRUB_BYTE);
        constexpr FlatPtr kfree_scrub_pattern = explode_byte(KFREE_SCRUB_BYTE);
        if (response == PageFaultResponse::BusError) {
            dbgln("Note: Address {} is an access to an undefined memory range of an Inode-backed VMObject", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (kmalloc_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be uninitialized kmalloc() memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (kfree_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be recently kfree()'d memory", VirtualAddress(fault_address));
        } else if (fault_address < 4096) {
            dbgln("Note: Address {} looks like a possible nullptr dereference", VirtualAddress(fault_address));
        } else if constexpr (SANITIZE_PTRS) {
            constexpr FlatPtr refptr_scrub_pattern = explode_byte(REFPTR_SCRUB_BYTE);
            constexpr FlatPtr nonnullrefptr_scrub_pattern = explode_byte(NONNULLREFPTR_SCRUB_BYTE);
            constexpr FlatPtr ownptr_scrub_pattern = explode_byte(OWNPTR_SCRUB_BYTE);
            constexpr FlatPtr nonnullownptr_scrub_pattern = explode_byte(NONNULLOWNPTR_SCRUB_BYTE);
            constexpr FlatPtr lockrefptr_scrub_pattern = explode_byte(LOCKREFPTR_SCRUB_BYTE);
            constexpr FlatPtr nonnulllockrefptr_scrub_pattern = explode_byte(NONNULLLOCKREFPTR_SCRUB_BYTE);

            if ((fault_address & 0xffff0000) == (refptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed LockRefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (nonnullrefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed NonnullLockRefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (ownptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed OwnPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (nonnullownptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed NonnullOwnPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (lockrefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed LockRefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (nonnulllockrefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed NonnullLockRefPtr", VirtualAddress(fault_address));
            }
        }

        if (current_thread) {
            auto& current_process = current_thread->process();
            if (current_process.is_user_process()) {
                auto fault_address_string = KString::formatted("{:p}", fault_address);
                auto fault_address_view = fault_address_string.is_error() ? ""sv : fault_address_string.value()->view();
                (void)current_process.try_set_coredump_property("fault_address"sv, fault_address_view);
                if (type() != PageFault::Type::Unknown)
                    (void)current_process.try_set_coredump_property("fault_type"sv, type() == PageFault::Type::PageNotPresent ? "NotPresent"sv : "ProtectionViolation"sv);
                StringView fault_access;
                if (is_instruction_fetch())
                    fault_access = "Execute"sv;
                else
                    fault_access = access() == PageFault::Access::Read ? "Read"sv : "Write"sv;
                (void)current_process.try_set_coredump_property("fault_access"sv, fault_access);
            }
        }

        if (response == PageFaultResponse::BusError)
            return handle_crash(regs, "Page Fault (Bus Error)", SIGBUS, false);
        return handle_crash(regs, "Page Fault", SIGSEGV, response == PageFaultResponse::OutOfMemory);
    } else if (response == PageFaultResponse::Continue) {
        dbgln_if(PAGE_FAULT_DEBUG, "Continuing after resolved page fault");
    } else {
        VERIFY_NOT_REACHED();
    }
}

}
