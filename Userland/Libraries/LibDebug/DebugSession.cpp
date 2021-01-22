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
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>
#include <stdlib.h>

namespace Debug {

DebugSession::DebugSession(pid_t pid, String source_root)
    : m_debuggee_pid(pid)
    , m_source_root(source_root)

{
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

OwnPtr<DebugSession> DebugSession::exec_and_attach(const String& command, String source_root)
{
    auto pid = fork();

    if (pid < 0) {
        perror("fork");
        exit(1);
    }

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
        const char** envp = (const char**)calloc(2, sizeof(const char*));
        // This causes loader to stop on a breakpoint before jumping to the entry point of the program.
        envp[0] = "_LOADER_BREAKPOINT=1";
        int rc = execvpe(args[0], const_cast<char**>(args), const_cast<char**>(envp));
        if (rc < 0) {
            perror("execvp");
            exit(1);
        }
    }

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("waitpid");
        return {};
    }

    if (ptrace(PT_ATTACH, pid, 0, 0) < 0) {
        perror("PT_ATTACH");
        return {};
    }

    // We want to continue until the exit from the 'execve' sycsall.
    // This ensures that when we start debugging the process
    // it executes the target image, and not the forked image of the tracing process.
    // NOTE: we only need to do this when we are debugging a new process (i.e not attaching to a process that's already running!)

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("wait_pid");
        return {};
    }

    auto debug_session = adopt_own(*new DebugSession(pid, source_root));

    // Continue until breakpoint before entry point of main program
    int wstatus = debug_session->continue_debuggee_and_wait();
    if (WSTOPSIG(wstatus) != SIGTRAP) {
        dbgln("expected SIGTRAP");
        return {};
    }

    // At this point, libraries should have been loaded
    debug_session->update_loaded_libs();

    return move(debug_session);
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

Optional<DebugSession::InsertBreakpointAtSymbolResult> DebugSession::insert_breakpoint(const String& symbol_name)
{
    Optional<InsertBreakpointAtSymbolResult> result;
    for_each_loaded_library([this, symbol_name, &result](auto& lib) {
        // The loader contains its own definitions for LibC symbols, so we don't want to include it in the search.
        if (lib.name == "Loader.so")
            return IterationDecision::Continue;

        auto symbol = lib.debug_info->elf().find_demangled_function(symbol_name);
        if (!symbol.has_value())
            return IterationDecision::Continue;

        auto breakpoint_address = symbol.value().value() + lib.base_address;
        bool rc = this->insert_breakpoint(reinterpret_cast<void*>(breakpoint_address));
        if (!rc)
            return IterationDecision::Break;

        result = InsertBreakpointAtSymbolResult { lib.name, breakpoint_address };
        return IterationDecision::Break;
    });
    return result;
}

Optional<DebugSession::InsertBreakpointAtSourcePositionResult> DebugSession::insert_breakpoint(const String& file_name, size_t line_number)
{
    auto address_and_source_position = get_address_from_source_position(file_name, line_number);
    if (!address_and_source_position.has_value())
        return {};

    auto address = address_and_source_position.value().address;
    bool rc = this->insert_breakpoint(reinterpret_cast<void*>(address));
    if (!rc)
        return {};

    auto lib = library_at(address);
    ASSERT(lib);

    return InsertBreakpointAtSourcePositionResult { lib->name, address_and_source_position.value().file, address_and_source_position.value().line, address };
}

void DebugSession::update_loaded_libs()
{
    auto file = Core::File::construct(String::format("/proc/%u/vm", m_debuggee_pid));
    bool rc = file->open(Core::IODevice::ReadOnly);
    ASSERT(rc);

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    ASSERT(json.has_value());

    auto vm_entries = json.value().as_array();
    Regex<PosixExtended> re("(.+): \\.text");

    auto get_path_to_object = [&re](const String& vm_name) -> Optional<String> {
        if (vm_name == "/usr/lib/Loader.so")
            return vm_name;
        RegexResult result;
        auto rc = re.search(vm_name, result);
        if (!rc)
            return {};
        auto lib_name = result.capture_group_matches.at(0).at(0).view.u8view().to_string();
        if (lib_name.starts_with("/"))
            return lib_name;
        return String::format("/usr/lib/%s", lib_name.characters());
    };

    vm_entries.for_each([&](auto& entry) {
        // TODO: check that region is executable
        auto vm_name = entry.as_object().get("name").as_string();

        auto object_path = get_path_to_object(vm_name);
        if (!object_path.has_value())
            return IterationDecision::Continue;

        String lib_name = object_path.value();
        if (lib_name.ends_with(".so"))
            lib_name = LexicalPath(object_path.value()).basename();

        // FIXME: DebugInfo currently cannot parse the debug information of libgcc_s.so
        if (lib_name == "libgcc_s.so")
            return IterationDecision::Continue;

        if (m_loaded_libraries.contains(lib_name))
            return IterationDecision::Continue;

        auto file_or_error = MappedFile ::map(object_path.value());
        if (file_or_error.is_error())
            return IterationDecision::Continue;

        FlatPtr base_address = entry.as_object().get("address").as_u32();
        auto debug_info = make<DebugInfo>(make<ELF::Image>(file_or_error.value()->bytes()), m_source_root, base_address);
        auto lib = make<LoadedLibrary>(lib_name, file_or_error.release_value(), move(debug_info), base_address);
        m_loaded_libraries.set(lib_name, move(lib));

        return IterationDecision::Continue;
    });
}

const DebugSession::LoadedLibrary* DebugSession::library_at(FlatPtr address) const
{
    const LoadedLibrary* result = nullptr;
    for_each_loaded_library([&result, address](const auto& lib) {
        if (address >= lib.base_address && address < lib.base_address + lib.debug_info->elf().size()) {
            result = &lib;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

Optional<DebugSession::SymbolicationResult> DebugSession::symbolicate(FlatPtr address) const
{
    auto* lib = library_at(address);
    if (!lib)
        return {};
    //FIXME: ELF::Image symlicate() API should return String::empty() if symbol is not found (It currently returns ??)
    auto symbol = lib->debug_info->elf().symbolicate(address - lib->base_address);
    return { { lib->name, symbol } };
}

Optional<DebugInfo::SourcePositionAndAddress> DebugSession::get_address_from_source_position(const String& file, size_t line) const
{
    Optional<DebugInfo::SourcePositionAndAddress> result;
    for_each_loaded_library([this, file, line, &result](auto& lib) {
        // The loader contains its own definitions for LibC symbols, so we don't want to include it in the search.
        if (lib.name == "Loader.so")
            return IterationDecision::Continue;

        auto source_position_and_address = lib.debug_info->get_address_from_source_position(file, line);
        if (!source_position_and_address.has_value())
            return IterationDecision::Continue;

        result = source_position_and_address;
        result.value().address += lib.base_address;
        return IterationDecision::Break;
    });
    return result;
}

Optional<DebugInfo::SourcePosition> DebugSession::get_source_position(FlatPtr address) const
{
    auto* lib = library_at(address);
    if (!lib)
        return {};
    return lib->debug_info->get_source_position(address - lib->base_address);
}

}
