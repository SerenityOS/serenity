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
#include <LibELF/Loader.h>
#include <signal.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

class DebugSession {
public:
    static OwnPtr<DebugSession> exec_and_attach(const String& command);

    // Has to be public for OwnPtr::make
    DebugSession(int pid);
    ~DebugSession();

    int pid() const { return m_debugee_pid; }

    bool poke(u32* address, u32 data);
    Optional<u32> peek(u32* address) const;

    enum class BreakPointState {
        Enabled,
        Disabled,
    };

    struct BreakPoint {
        void* address;
        u32 original_first_word;
        BreakPointState state;
    };

    bool insert_breakpoint(void* address);
    bool disable_breakpoint(void* address);
    bool enable_breakpoint(void* address);
    bool remove_breakpoint(void* address);
    bool breakpoint_exists(void* address) const;

    void dump_breakpoints()
    {
        for (auto addr : m_breakpoints.keys()) {
            dbg() << addr;
        }
    }

    PtraceRegisters get_registers() const;
    void set_registers(const PtraceRegisters&);

    enum class ContinueType {
        FreeRun,
        Syscall,
    };
    void continue_debugee(ContinueType type = ContinueType::FreeRun);

    //returns the wstatus result of waitpid()
    int continue_debugee_and_wait(ContinueType type = ContinueType::FreeRun);

    void* single_step();

    template<typename Callback>
    void run(Callback callback);

    const ELF::Loader& elf() const { return *m_elf; }
    NonnullRefPtr<const ELF::Loader> elf_ref() const { return m_elf; }
    const MappedFile& executable() const { return m_executable; }
    const DebugInfo& debug_info() const { return m_debug_info; }

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
    // x86 breakpoint instruction "int3"
    static constexpr u8 BREAKPOINT_INSTRUCTION = 0xcc;

    int m_debugee_pid { -1 };
    bool m_is_debugee_dead { false };

    MappedFile m_executable;
    NonnullRefPtr<const ELF::Loader> m_elf;
    DebugInfo m_debug_info;

    HashMap<void*, BreakPoint> m_breakpoints;
};

template<typename Callback>
void DebugSession::run(Callback callback)
{

    enum class State {
        FreeRun,
        Syscall,
        ConsecutiveBreakpoint,
        SingleStep,
    };

    State state { State::FreeRun };

    auto do_continue_and_wait = [&]() {
        int wstatus = continue_debugee_and_wait((state == State::FreeRun) ? ContinueType::FreeRun : ContinueType::Syscall);

        // FIXME: This check actually only checks whether the debugee
        // stopped because it hit a breakpoint/syscall/is in single stepping mode or not
        if (WSTOPSIG(wstatus) != SIGTRAP) {
            callback(DebugBreakReason::Exited, Optional<PtraceRegisters>());
            m_is_debugee_dead = true;
            return true;
        }
        return false;
    };

    for (;;) {
        if (state == State::FreeRun || state == State::Syscall) {
            if (do_continue_and_wait())
                break;
        }

        auto regs = get_registers();
        Optional<BreakPoint> current_breakpoint;

        if (state == State::FreeRun || state == State::Syscall) {
            current_breakpoint = m_breakpoints.get((void*)((u32)regs.eip - 1));
            if (current_breakpoint.has_value())
                state = State::FreeRun;
        } else {
            current_breakpoint = m_breakpoints.get((void*)regs.eip);
        }

        if (current_breakpoint.has_value()) {
            // We want to make the breakpoint transparrent to the user of the debugger
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

        // Re-enable the breakpoint if it wasn't removed by the user
        if (current_breakpoint.has_value() && m_breakpoints.contains(current_breakpoint.value().address)) {
            auto stopped_address = single_step();
            enable_breakpoint(current_breakpoint.value().address);
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

        if (decision == DebugDecision::Kill || decision == DebugDecision::Detach) {
            ASSERT_NOT_REACHED(); // TODO: implement
        }

        if (state == State::SingleStep) {
            single_step();
        }
    }
}
