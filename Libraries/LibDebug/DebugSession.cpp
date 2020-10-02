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

#include "DebugSession.h"
#include <AK/Optional.h>
#include <stdlib.h>

namespace Debug {

DebugSession::DebugSession(int pid)
    : m_debuggee_pid(pid)
    , m_executable(initialize_executable_mapped_file(pid))
    , m_elf(ELF::Loader::create(reinterpret_cast<const u8*>(m_executable->data()), m_executable->size()))
    , m_debug_info(m_elf)
{
}

NonnullOwnPtr<const MappedFile> DebugSession::initialize_executable_mapped_file(int pid)
{
    auto executable = adopt_own(*new MappedFile(String::format("/proc/%d/exe", pid)));
    ASSERT(executable->is_valid());
    return executable;
}

DebugSession::~DebugSession()
{
    if (m_is_debuggee_dead)
        return;

    for (const auto& bp : m_breakpoints) {
        disable_breakpoint(bp.key);
    }
    m_breakpoints.clear();

    if (ptrace(PT_DETACH, m_debuggee_pid, 0, 0) < 0) {
        perror("PT_DETACH");
    }
}

OwnPtr<DebugSession> DebugSession::exec_and_attach(const String& command)
{
    int pid = fork();

    if (!pid) {
        if (ptrace(PT_TRACE_ME, 0, 0, 0) < 0) {
            perror("PT_TRACE_ME");
            exit(1);
        }

        auto parts = command.split(' ');
        ASSERT(!parts.is_empty());
        const char** args = (const char**)calloc(parts.size() + 1, sizeof(const char*));
        for (size_t i = 0; i < parts.size(); i++) {
            args[i] = parts[i].characters();
        }
        int rc = execvp(args[0], const_cast<char**>(args));
        if (rc < 0) {
            perror("execvp");
        }
        ASSERT_NOT_REACHED();
    }

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("waitpid");
        return nullptr;
    }

    if (ptrace(PT_ATTACH, pid, 0, 0) < 0) {
        perror("PT_ATTACH");
        return nullptr;
    }

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("waitpid");
        return nullptr;
    }

    if (ptrace(PT_CONTINUE, pid, 0, 0) < 0) {
        perror("continue");
        return nullptr;
    }

    // We want to continue until the exit from the 'execve' sycsall.
    // This ensures that when we start debugging the process
    // it executes the target image, and not the forked image of the tracing process.
    // NOTE: we only need to do this when we are debugging a new process (i.e not attaching to a process that's already running!)

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("wait_pid");
        return nullptr;
    }

    return make<DebugSession>(pid);
}

bool DebugSession::poke(u32* address, u32 data)
{
    if (ptrace(PT_POKE, m_debuggee_pid, (void*)address, data) < 0) {
        perror("PT_POKE");
        return false;
    }
    return true;
}

Optional<u32> DebugSession::peek(u32* address) const
{
    Optional<u32> result;
    int rc = ptrace(PT_PEEK, m_debuggee_pid, (void*)address, 0);
    if (errno == 0)
        result = static_cast<u32>(rc);
    return result;
}

bool DebugSession::insert_breakpoint(void* address)
{
    // We insert a software breakpoint by
    // patching the first byte of the instruction at 'address'
    // with the breakpoint instruction (int3)

    if (m_breakpoints.contains(address))
        return false;

    auto original_bytes = peek(reinterpret_cast<u32*>(address));

    if (!original_bytes.has_value())
        return false;

    ASSERT((original_bytes.value() & 0xff) != BREAKPOINT_INSTRUCTION);

    BreakPoint breakpoint { address, original_bytes.value(), BreakPointState::Disabled };

    m_breakpoints.set(address, breakpoint);

    enable_breakpoint(breakpoint.address);

    return true;
}

bool DebugSession::disable_breakpoint(void* address)
{
    auto breakpoint = m_breakpoints.get(address);
    ASSERT(breakpoint.has_value());
    if (!poke(reinterpret_cast<u32*>(reinterpret_cast<char*>(breakpoint.value().address)), breakpoint.value().original_first_word))
        return false;

    auto bp = m_breakpoints.get(breakpoint.value().address).value();
    bp.state = BreakPointState::Disabled;
    m_breakpoints.set(bp.address, bp);
    return true;
}

bool DebugSession::enable_breakpoint(void* address)
{
    auto breakpoint = m_breakpoints.get(address);
    ASSERT(breakpoint.has_value());

    ASSERT(breakpoint.value().state == BreakPointState::Disabled);

    if (!poke(reinterpret_cast<u32*>(breakpoint.value().address), (breakpoint.value().original_first_word & ~(uint32_t)0xff) | BREAKPOINT_INSTRUCTION))
        return false;

    auto bp = m_breakpoints.get(breakpoint.value().address).value();
    bp.state = BreakPointState::Enabled;
    m_breakpoints.set(bp.address, bp);
    return true;
}

bool DebugSession::remove_breakpoint(void* address)
{
    if (!disable_breakpoint(address))
        return false;

    m_breakpoints.remove(address);
    return true;
}

bool DebugSession::breakpoint_exists(void* address) const
{
    return m_breakpoints.contains(address);
}

PtraceRegisters DebugSession::get_registers() const
{
    PtraceRegisters regs;
    if (ptrace(PT_GETREGS, m_debuggee_pid, &regs, 0) < 0) {
        perror("PT_GETREGS");
        ASSERT_NOT_REACHED();
    }
    return regs;
}

void DebugSession::set_registers(const PtraceRegisters& regs)
{
    if (ptrace(PT_SETREGS, m_debuggee_pid, reinterpret_cast<void*>(&const_cast<PtraceRegisters&>(regs)), 0) < 0) {
        perror("PT_SETREGS");
        ASSERT_NOT_REACHED();
    }
}

void DebugSession::continue_debuggee(ContinueType type)
{
    int command = (type == ContinueType::FreeRun) ? PT_CONTINUE : PT_SYSCALL;
    if (ptrace(command, m_debuggee_pid, 0, 0) < 0) {
        perror("continue");
        ASSERT_NOT_REACHED();
    }
}

int DebugSession::continue_debuggee_and_wait(ContinueType type)
{
    continue_debuggee(type);
    int wstatus = 0;
    if (waitpid(m_debuggee_pid, &wstatus, WSTOPPED | WEXITED) != m_debuggee_pid) {
        perror("waitpid");
        ASSERT_NOT_REACHED();
    }
    return wstatus;
}

void* DebugSession::single_step()
{
    // Single stepping works by setting the x86 TRAP flag bit in the eflags register.
    // This flag causes the cpu to enter single-stepping mode, which causes
    // Interrupt 1 (debug interrupt) to be emitted after every instruction.
    // To single step the program, we set the TRAP flag and continue the debuggee.
    // After the debuggee has stopped, we clear the TRAP flag.

    auto regs = get_registers();
    constexpr u32 TRAP_FLAG = 0x100;
    regs.eflags |= TRAP_FLAG;
    set_registers(regs);

    continue_debuggee();

    if (waitpid(m_debuggee_pid, 0, WSTOPPED) != m_debuggee_pid) {
        perror("waitpid");
        ASSERT_NOT_REACHED();
    }

    regs = get_registers();
    regs.eflags &= ~(TRAP_FLAG);
    set_registers(regs);
    return (void*)regs.eip;
}

void DebugSession::detach()
{
    for (auto& breakpoint : m_breakpoints.keys()) {
        remove_breakpoint(breakpoint);
    }
    continue_debuggee();
}

}
