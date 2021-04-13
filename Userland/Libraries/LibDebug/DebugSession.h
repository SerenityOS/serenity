/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#pragma once

#include <AK/Demangle.h>
#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibDebug/DebugInfo.h>
#include <signal.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Debug {

class DebugSession {
public:
    static OwnPtr<DebugSession> exec_and_attach(const String& command, String source_root = {});

    ~DebugSession();

    int pid() const { return m_debuggee_pid; }

    bool poke(u32* address, u32 data);
    Optional<u32> peek(u32* address) const;

    bool poke_debug(u32 register_index, u32 data);
    Optional<u32> peek_debug(u32 register_index) const;

    enum class BreakPointState {
        Enabled,
        Disabled,
    };

    struct BreakPoint {
        void* address { nullptr };
        u32 original_first_word { 0 };
        BreakPointState state { BreakPointState::Disabled };
    };

    struct InsertBreakpointAtSymbolResult {
        String library_name;
        FlatPtr address { 0 };
    };

    Optional<InsertBreakpointAtSymbolResult> insert_breakpoint(const String& symbol_name);

    struct InsertBreakpointAtSourcePositionResult {
        String library_name;
        String file_name;
        size_t line_number { 0 };
        FlatPtr address { 0 };
    };

    Optional<InsertBreakpointAtSourcePositionResult> insert_breakpoint(const String& file_name, size_t line_number);

    bool insert_breakpoint(void* address);
    bool disable_breakpoint(void* address);
    bool enable_breakpoint(void* address);
    bool remove_breakpoint(void* address);
    bool breakpoint_exists(void* address) const;

    struct WatchPoint {
        void* address { nullptr };
        u32 debug_register_index { 0 };
        u32 ebp { 0 };
    };

    bool insert_watchpoint(void* address, u32 ebp);
    bool remove_watchpoint(void* address);
    bool disable_watchpoint(void* address);
    bool watchpoint_exists(void* address) const;

    void dump_breakpoints()
    {
        for (auto addr : m_breakpoints.keys()) {
            dbgln("{}", addr);
        }
    }

    PtraceRegisters get_registers() const;
    void set_registers(const PtraceRegisters&);

    enum class ContinueType {
        FreeRun,
        Syscall,
    };
    void continue_debuggee(ContinueType type = ContinueType::FreeRun);

    // Returns the wstatus result of waitpid()
    int continue_debuggee_and_wait(ContinueType type = ContinueType::FreeRun);

    // Returns the new eip
    void* single_step();

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

    struct LoadedLibrary {
        String name;
        NonnullRefPtr<MappedFile> file;
        NonnullOwnPtr<DebugInfo> debug_info;
        FlatPtr base_address;

        LoadedLibrary(const String& name, NonnullRefPtr<MappedFile> file, NonnullOwnPtr<DebugInfo>&& debug_info, FlatPtr base_address)
            : name(name)
            , file(move(file))
            , debug_info(move(debug_info))
            , base_address(base_address)
        {
        }
    };

    template<typename Func>
    void for_each_loaded_library(Func f) const
    {
        for (const auto& lib_name : m_loaded_libraries.keys()) {
            const auto& lib = *m_loaded_libraries.get(lib_name).value();
            if (f(lib) == IterationDecision::Break)
                break;
        }
    }

    const LoadedLibrary* library_at(FlatPtr address) const;

    struct SymbolicationResult {
        String library_name;
        String symbol;
    };
    Optional<SymbolicationResult> symbolicate(FlatPtr address) const;

    Optional<DebugInfo::SourcePositionAndAddress> get_address_from_source_position(const String& file, size_t line) const;

    Optional<DebugInfo::SourcePosition> get_source_position(FlatPtr address) const;

private:
    explicit DebugSession(pid_t, String source_root);

    // x86 breakpoint instruction "int3"
    static constexpr u8 BREAKPOINT_INSTRUCTION = 0xcc;

    void update_loaded_libs();

    int m_debuggee_pid { -1 };
    String m_source_root;
    bool m_is_debuggee_dead { false };

    HashMap<void*, BreakPoint> m_breakpoints;
    HashMap<void*, WatchPoint> m_watchpoints;

    // Maps from base address to loaded library
    HashMap<String, NonnullOwnPtr<LoadedLibrary>> m_loaded_libraries;
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
        if (WSTOPSIG(wstatus) != SIGTRAP) {
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

                u32 current_ebp = regs.ebp;
                u32 current_instruction = regs.eip;
                do {
                    if (current_ebp == required_ebp) {
                        found_ebp = true;
                        break;
                    }
                    auto return_address = peek(reinterpret_cast<u32*>(current_ebp + sizeof(FlatPtr)));
                    auto next_ebp = peek(reinterpret_cast<u32*>(current_ebp));
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
            current_breakpoint = m_breakpoints.get((void*)((u32)regs.eip - 1));
            if (current_breakpoint.has_value())
                state = State::FreeRun;
        } else {
            current_breakpoint = m_breakpoints.get((void*)regs.eip);
        }

        if (current_breakpoint.has_value()) {
            // We want to make the breakpoint transparent to the user of the debugger.
            // To achieive this, we perform two rollbacks:
            // 1. Set regs.eip to point at the actual address of the instruction we breaked on.
            //    regs.eip currently points to one byte after the address of the original instruction,
            //    because the cpu has just executed the INT3 we patched into the instruction.
            // 2. We restore the original first byte of the instruction,
            //    because it was patched with INT3.
            regs.eip = reinterpret_cast<u32>(current_breakpoint.value().address);
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
        if (current_breakpoint.has_value() && m_breakpoints.contains(current_breakpoint.value().address)) {
            // The current breakpoint was removed to make it transparent to the user.
            // We now want to re-enable it - the code execution flow could hit it again.
            // To re-enable the breakpoint, we first perform a single step and execute the
            // instruction of the breakpoint, and then redo the INT3 patch in its first byte.

            // If the user manually inserted a breakpoint at were we breaked at originally,
            // we need to disable that breakpoint because we want to singlestep over it to execute the
            // instruction we breaked on (we re-enable it again later anyways).
            if (m_breakpoints.contains(current_breakpoint.value().address) && m_breakpoints.get(current_breakpoint.value().address).value().state == BreakPointState::Enabled) {
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
