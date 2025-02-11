/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Demangle.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibCore/MappedFile.h>
#include <LibDebug/DebugInfo.h>
#include <LibDebug/ProcessInspector.h>
#include <signal.h>
#include <stdio.h>
#include <sys/arch/regs.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Debug {

class DebugSession : public ProcessInspector {
public:
    static OwnPtr<DebugSession> exec_and_attach(ByteString const& command, ByteString source_root = {}, Function<ErrorOr<void>()> setup_child = {}, Function<void(float)> on_initialization_progress = {});
    static OwnPtr<DebugSession> attach(pid_t pid, ByteString source_root = {}, Function<void(float)> on_initialization_progress = {});

    virtual ~DebugSession() override;

    // ^Debug::ProcessInspector
    virtual bool poke(FlatPtr address, FlatPtr data) override;
    virtual Optional<FlatPtr> peek(FlatPtr address) const override;
    virtual PtraceRegisters get_registers() const override;
    virtual void set_registers(PtraceRegisters const&) override;
    virtual void for_each_loaded_library(Function<IterationDecision(LoadedLibrary const&)>) const override;

    int pid() const { return m_debuggee_pid; }

    bool poke_debug(u32 register_index, FlatPtr data) const;
    Optional<FlatPtr> peek_debug(u32 register_index) const;

    enum class BreakPointState {
        Enabled,
        Disabled,
    };

    struct BreakPoint {
        FlatPtr address { 0 };
        FlatPtr original_first_word { 0 };
        BreakPointState state { BreakPointState::Disabled };
    };

    struct InsertBreakpointAtSymbolResult {
        ByteString library_name;
        FlatPtr address { 0 };
    };

    Optional<InsertBreakpointAtSymbolResult> insert_breakpoint(ByteString const& symbol_name);

    struct InsertBreakpointAtSourcePositionResult {
        ByteString library_name;
        ByteString filename;
        size_t line_number { 0 };
        FlatPtr address { 0 };
    };

    Optional<InsertBreakpointAtSourcePositionResult> insert_breakpoint(ByteString const& filename, size_t line_number);

    bool insert_breakpoint(FlatPtr address);
    bool disable_breakpoint(FlatPtr address);
    bool enable_breakpoint(FlatPtr address);
    bool remove_breakpoint(FlatPtr address);
    bool breakpoint_exists(FlatPtr address) const;

    struct WatchPoint {
        FlatPtr address { 0 };
        u32 debug_register_index { 0 };
        u32 ebp { 0 };
    };

    bool insert_watchpoint(FlatPtr address, u32 ebp);
    bool remove_watchpoint(FlatPtr address);
    bool disable_watchpoint(FlatPtr address);
    bool watchpoint_exists(FlatPtr address) const;

    void dump_breakpoints()
    {
        for (auto addr : m_breakpoints.keys()) {
            dbgln("{}", addr);
        }
    }

    enum class ContinueType {
        FreeRun,
        Syscall,
    };
    void continue_debuggee(ContinueType type = ContinueType::FreeRun);
    void stop_debuggee();

    // Returns the wstatus result of waitpid()
    int continue_debuggee_and_wait(ContinueType type = ContinueType::FreeRun);

    // Returns the new eip
    FlatPtr single_step();

    void detach();

    enum DesiredInitialDebugeeState {
        Running,
        Stopped
    };
    template<typename Callback>
    void run(DesiredInitialDebugeeState, Callback);

    enum DebugDecision {
        Continue,
        SingleStep,
        ContinueBreakAtSyscall,
        Detach,
        Kill,
    };

    enum DebugBreakReason {
        Breakpoint,
        Syscall,
        Exited,
    };

private:
    explicit DebugSession(pid_t, ByteString source_root, Function<void(float)> on_initialization_progress = {});

    // x86 breakpoint instruction "int3"
    static constexpr u8 BREAKPOINT_INSTRUCTION = 0xcc;

    ErrorOr<void> update_loaded_libs();

    int m_debuggee_pid { -1 };
    ByteString m_source_root;
    bool m_is_debuggee_dead { false };

    HashMap<FlatPtr, BreakPoint> m_breakpoints;
    HashMap<FlatPtr, WatchPoint> m_watchpoints;

    // Maps from library name to LoadedLibrary object
    HashMap<ByteString, NonnullOwnPtr<LoadedLibrary>> m_loaded_libraries;

    Function<void(float)> m_on_initialization_progress;
};

template<typename Callback>
void DebugSession::run(DesiredInitialDebugeeState initial_debugee_state, Callback callback)
{
    enum class State {
        FirstIteration,
        FreeRun,
        Syscall,
        ConsecutiveBreakpoint,
        SingleStep,
    };

    State state { State::FirstIteration };

    auto do_continue_and_wait = [&]() {
        int wstatus = continue_debuggee_and_wait((state == State::Syscall) ? ContinueType::Syscall : ContinueType::FreeRun);

        // FIXME: This check actually only checks whether the debuggee
        // stopped because it hit a breakpoint/syscall/is in single stepping mode or not
        if (WSTOPSIG(wstatus) != SIGTRAP && WSTOPSIG(wstatus) != SIGSTOP) {
            callback(DebugBreakReason::Exited, Optional<PtraceRegisters>());
            m_is_debuggee_dead = true;
            return true;
        }
        return false;
    };

    for (;;) {
        if ((state == State::FirstIteration && initial_debugee_state == DesiredInitialDebugeeState::Running) || state == State::FreeRun || state == State::Syscall) {
            if (do_continue_and_wait())
                break;
        }
        if (state == State::FirstIteration)
            state = State::FreeRun;

        auto regs = get_registers();

#if ARCH(X86_64)
        FlatPtr current_instruction = regs.rip;
#elif ARCH(AARCH64)
        FlatPtr current_instruction;
        TODO_AARCH64();
#elif ARCH(RISCV64)
        FlatPtr current_instruction = regs.pc;
#else
#    error Unknown architecture
#endif

        auto debug_status = peek_debug(DEBUG_STATUS_REGISTER);
        if (debug_status.has_value() && (debug_status.value() & 0b1111) > 0) {
            // Tripped a watchpoint
            auto watchpoint_index = debug_status.value() & 0b1111;
            Optional<WatchPoint> watchpoint {};
            for (auto wp : m_watchpoints) {
                if ((watchpoint_index & (1 << wp.value.debug_register_index)) == 0)
                    continue;
                watchpoint = wp.value;
                break;
            }
            if (watchpoint.has_value()) {
                auto required_ebp = watchpoint.value().ebp;
                auto found_ebp = false;

#if ARCH(X86_64)
                FlatPtr current_ebp = regs.rbp;
#elif ARCH(AARCH64)
                FlatPtr current_ebp;
                TODO_AARCH64();
#elif ARCH(RISCV64)
                FlatPtr current_ebp = regs.bp();
#else
#    error Unknown architecture
#endif

                // FIXME: Use AK::unwind_stack_from_frame_pointer
                do {
                    if (current_ebp == required_ebp) {
                        found_ebp = true;
                        break;
                    }
                    auto return_address = peek(current_ebp + sizeof(FlatPtr));
                    auto next_ebp = peek(current_ebp);
                    VERIFY(return_address.has_value());
                    VERIFY(next_ebp.has_value());
                    current_instruction = return_address.value();
                    current_ebp = next_ebp.value();
                } while (current_ebp && current_instruction);

                if (!found_ebp) {
                    dbgln("Removing watchpoint at {:p} because it went out of scope!", watchpoint.value().address);
                    remove_watchpoint(watchpoint.value().address);
                    continue;
                }
            }
        }

        Optional<BreakPoint> current_breakpoint;

        if (state == State::FreeRun || state == State::Syscall) {
            current_breakpoint = m_breakpoints.get(current_instruction - 1).copy();
            if (current_breakpoint.has_value())
                state = State::FreeRun;
        } else {
            current_breakpoint = m_breakpoints.get(current_instruction).copy();
        }

        if (current_breakpoint.has_value()) {
            // We want to make the breakpoint transparent to the user of the debugger.
            // To achieve this, we perform two rollbacks:
            // 1. Set regs.eip to point at the actual address of the instruction we broke on.
            //    regs.eip currently points to one byte after the address of the original instruction,
            //    because the cpu has just executed the INT3 we patched into the instruction.
            // 2. We restore the original first byte of the instruction,
            //    because it was patched with INT3.
            auto breakpoint_addr = bit_cast<FlatPtr>(current_breakpoint.value().address);
#if ARCH(X86_64)
            regs.rip = breakpoint_addr;
#elif ARCH(AARCH64)
            (void)breakpoint_addr;
            TODO_AARCH64();
#elif ARCH(RISCV64)
            regs.pc = breakpoint_addr;
#else
#    error Unknown architecture
#endif
            set_registers(regs);
            disable_breakpoint(current_breakpoint.value().address);
        }

        DebugBreakReason reason = (state == State::Syscall && !current_breakpoint.has_value()) ? DebugBreakReason::Syscall : DebugBreakReason::Breakpoint;

        DebugDecision decision = callback(reason, regs);

        if (reason == DebugBreakReason::Syscall) {
            // skip the exit from the syscall
            if (do_continue_and_wait())
                break;
        }

        if (decision == DebugDecision::Continue) {
            state = State::FreeRun;
        } else if (decision == DebugDecision::ContinueBreakAtSyscall) {
            state = State::Syscall;
        }

        bool did_single_step = false;

        // Re-enable the breakpoint if it wasn't removed by the user
        if (current_breakpoint.has_value()) {
            auto current_breakpoint_address = bit_cast<FlatPtr>(current_breakpoint.value().address);
            if (m_breakpoints.contains(current_breakpoint_address)) {
                // The current breakpoint was removed to make it transparent to the user.
                // We now want to re-enable it - the code execution flow could hit it again.
                // To re-enable the breakpoint, we first perform a single step and execute the
                // instruction of the breakpoint, and then redo the INT3 patch in its first byte.

                // If the user manually inserted a breakpoint at the current instruction,
                // we need to disable that breakpoint because we want to singlestep over that
                // instruction (we re-enable it again later anyways).
                if (m_breakpoints.contains(current_breakpoint_address) && m_breakpoints.get(current_breakpoint_address).value().state == BreakPointState::Enabled) {
                    disable_breakpoint(current_breakpoint.value().address);
                }
                auto stopped_address = single_step();
                enable_breakpoint(current_breakpoint.value().address);
                did_single_step = true;
                // If there is another breakpoint after the current one,
                // Then we are already on it (because of single_step)
                auto breakpoint_at_next_instruction = m_breakpoints.get(stopped_address);
                if (breakpoint_at_next_instruction.has_value()
                    && breakpoint_at_next_instruction.value().state == BreakPointState::Enabled) {
                    state = State::ConsecutiveBreakpoint;
                }
            }
        }

        if (decision == DebugDecision::SingleStep) {
            state = State::SingleStep;
        }

        if (decision == DebugDecision::Detach) {
            detach();
            break;
        }
        if (decision == DebugDecision::Kill) {
            kill(m_debuggee_pid, SIGTERM);
            break;
        }

        if (state == State::SingleStep && !did_single_step) {
            single_step();
        }
    }
}

}
