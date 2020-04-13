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
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibC/sys/arch/i386/regs.h>
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
    bool disable_breakpoint(const BreakPoint&);
    bool enable_breakpoint(const BreakPoint&);

    PtraceRegisters get_registers() const;
    void set_registers(const PtraceRegisters&);

    void continue_debugee();
    void* single_step();

    template<typename Callback>
    void run(Callback callback);

    const ELF::Loader& elf() const { return m_elf; }

    enum DebugDecision {
        Continue,
        Detach,
        Kill,
    };

    enum DebugBreakReason {
        Breakpoint,
        Exited,
    };

private:
    // x86 breakpoint instruction "int3"
    static constexpr u8 BREAKPOINT_INSTRUCTION
        = 0xcc;

    int m_debugee_pid { -1 };
    bool m_is_debugee_dead { false };

    MappedFile m_executable;
    ELF::Loader m_elf;

    HashMap<void*, BreakPoint> m_breakpoints;
};

template<typename Callback>
void DebugSession::run(Callback callback)
{
    bool in_consecutive_breakpoint = false;
    for (;;) {
        if (!in_consecutive_breakpoint) {
            continue_debugee();

            int wstatus = 0;
            if (waitpid(m_debugee_pid, &wstatus, WSTOPPED | WEXITED) != m_debugee_pid) {
                perror("waitpid");
                ASSERT_NOT_REACHED();
            }

            // FIXME: This check actually only checks whether the debugee
            // Is stopped because it hit a breakpoint or not
            if (WSTOPSIG(wstatus) != SIGTRAP) {
                callback(DebugBreakReason::Exited, Optional<PtraceRegisters>());
                m_is_debugee_dead = true;
                break;
            }
        }

        auto regs = get_registers();
        Optional<BreakPoint> current_breakpoint;

        if (in_consecutive_breakpoint) {
            current_breakpoint = m_breakpoints.get((void*)regs.eip);
        } else {
            current_breakpoint = m_breakpoints.get((void*)((u32)regs.eip - 1));
        }

        ASSERT(current_breakpoint.has_value());

        // We want to make the breakpoint transparrent to the user of the debugger

        regs.eip = reinterpret_cast<u32>(current_breakpoint.value().address);
        set_registers(regs);
        disable_breakpoint(current_breakpoint.value());

        DebugDecision decision = callback(DebugBreakReason::Breakpoint, regs);
        if (decision != DebugDecision::Continue) {
            // FIXME: implement detach & kill
            ASSERT_NOT_REACHED();
        }

        // Re-enable the breakpoint
        auto stopped_address = single_step();
        enable_breakpoint(current_breakpoint.value());

        // If there is another breakpoint after the current one,
        // Then we are already on it (because of single_step)
        auto breakpoint_at_next_instruction = m_breakpoints.get(stopped_address);
        in_consecutive_breakpoint = breakpoint_at_next_instruction.has_value()
            && breakpoint_at_next_instruction.value().state == BreakPointState::Enabled;
    }
}
