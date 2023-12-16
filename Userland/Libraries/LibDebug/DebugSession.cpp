/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DebugSession.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/Platform.h>
#include <LibFileSystem/FileSystem.h>
#include <LibRegex/Regex.h>
#include <stdlib.h>
#include <sys/mman.h>

namespace Debug {

DebugSession::DebugSession(pid_t pid, ByteString source_root, Function<void(float)> on_initialization_progress)
    : m_debuggee_pid(pid)
    , m_source_root(source_root)
    , m_on_initialization_progress(move(on_initialization_progress))
{
}

DebugSession::~DebugSession()
{
    if (m_is_debuggee_dead)
        return;

    for (auto const& bp : m_breakpoints) {
        disable_breakpoint(bp.key);
    }
    m_breakpoints.clear();

    for (auto const& wp : m_watchpoints) {
        disable_watchpoint(wp.key);
    }
    m_watchpoints.clear();

    if (ptrace(PT_DETACH, m_debuggee_pid, 0, 0) < 0) {
        perror("PT_DETACH");
    }
}

void DebugSession::for_each_loaded_library(Function<IterationDecision(LoadedLibrary const&)> func) const
{
    for (auto const& lib_name : m_loaded_libraries.keys()) {
        auto const& lib = *m_loaded_libraries.get(lib_name).value();
        if (func(lib) == IterationDecision::Break)
            break;
    }
}

OwnPtr<DebugSession> DebugSession::exec_and_attach(ByteString const& command,
    ByteString source_root,
    Function<ErrorOr<void>()> setup_child,
    Function<void(float)> on_initialization_progress)
{
    auto pid = fork();

    if (pid < 0) {
        perror("fork");
        exit(1);
    }

    if (!pid) {

        if (setup_child) {
            if (setup_child().is_error()) {
                perror("DebugSession::setup_child");
                exit(1);
            }
        }

        if (ptrace(PT_TRACE_ME, 0, 0, 0) < 0) {
            perror("PT_TRACE_ME");
            exit(1);
        }

        auto parts = command.split(' ');
        VERIFY(!parts.is_empty());
        char const** args = bit_cast<char const**>(calloc(parts.size() + 1, sizeof(char const*)));
        for (size_t i = 0; i < parts.size(); i++) {
            args[i] = parts[i].characters();
        }
        char const** envp = bit_cast<char const**>(calloc(2, sizeof(char const*)));
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

    // We want to continue until the exit from the 'execve' syscall.
    // This ensures that when we start debugging the process
    // it executes the target image, and not the forked image of the tracing process.
    // NOTE: we only need to do this when we are debugging a new process (i.e not attaching to a process that's already running!)

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("wait_pid");
        return {};
    }

    auto debug_session = adopt_own(*new DebugSession(pid, source_root, move(on_initialization_progress)));

    // Continue until breakpoint before entry point of main program
    int wstatus = debug_session->continue_debuggee_and_wait();
    if (WSTOPSIG(wstatus) != SIGTRAP) {
        dbgln("expected SIGTRAP");
        return {};
    }

    // At this point, libraries should have been loaded
    auto update_or_error = debug_session->update_loaded_libs();
    if (update_or_error.is_error()) {
        dbgln("update failed: {}", update_or_error.error());
        return {};
    }

    return debug_session;
}

OwnPtr<DebugSession> DebugSession::attach(pid_t pid, ByteString source_root, Function<void(float)> on_initialization_progress)
{
    if (ptrace(PT_ATTACH, pid, 0, 0) < 0) {
        perror("PT_ATTACH");
        return {};
    }

    int status = 0;
    if (waitpid(pid, &status, WSTOPPED | WEXITED) != pid || !WIFSTOPPED(status)) {
        perror("waitpid");
        return {};
    }

    auto debug_session = adopt_own(*new DebugSession(pid, source_root, move(on_initialization_progress)));
    // At this point, libraries should have been loaded
    auto update_or_error = debug_session->update_loaded_libs();
    if (update_or_error.is_error()) {
        dbgln("update failed: {}", update_or_error.error());
        return {};
    }

    return debug_session;
}

bool DebugSession::poke(FlatPtr address, FlatPtr data)
{
    if (ptrace(PT_POKE, m_debuggee_pid, bit_cast<void*>(address), bit_cast<void*>(data)) < 0) {
        perror("PT_POKE");
        return false;
    }
    return true;
}

Optional<FlatPtr> DebugSession::peek(FlatPtr address) const
{
    Optional<FlatPtr> result;
    auto rc = ptrace(PT_PEEK, m_debuggee_pid, bit_cast<void*>(address), nullptr);
    if (errno == 0)
        result = static_cast<FlatPtr>(rc);
    return result;
}

bool DebugSession::poke_debug(u32 register_index, FlatPtr data) const
{
    if (ptrace(PT_POKEDEBUG, m_debuggee_pid, bit_cast<void*>(static_cast<FlatPtr>(register_index)), bit_cast<void*>(data)) < 0) {
        perror("PT_POKEDEBUG");
        return false;
    }
    return true;
}

Optional<FlatPtr> DebugSession::peek_debug(u32 register_index) const
{
    auto rc = ptrace(PT_PEEKDEBUG, m_debuggee_pid, bit_cast<void*>(static_cast<FlatPtr>(register_index)), nullptr);
    if (errno == 0)
        return static_cast<FlatPtr>(rc);

    return {};
}

bool DebugSession::insert_breakpoint(FlatPtr address)
{
    // We insert a software breakpoint by
    // patching the first byte of the instruction at 'address'
    // with the breakpoint instruction (int3)

    if (m_breakpoints.contains(address))
        return false;

    auto original_bytes = peek(address);

    if (!original_bytes.has_value())
        return false;

    VERIFY((original_bytes.value() & 0xff) != BREAKPOINT_INSTRUCTION);

    BreakPoint breakpoint { address, original_bytes.value(), BreakPointState::Disabled };

    m_breakpoints.set(address, breakpoint);

    enable_breakpoint(breakpoint.address);

    return true;
}

bool DebugSession::disable_breakpoint(FlatPtr address)
{
    auto breakpoint = m_breakpoints.get(address);
    VERIFY(breakpoint.has_value());
    if (!poke(breakpoint.value().address, breakpoint.value().original_first_word))
        return false;

    auto bp = m_breakpoints.get(breakpoint.value().address).value();
    bp.state = BreakPointState::Disabled;
    m_breakpoints.set(bp.address, bp);
    return true;
}

bool DebugSession::enable_breakpoint(FlatPtr address)
{
    auto breakpoint = m_breakpoints.get(address);
    VERIFY(breakpoint.has_value());

    VERIFY(breakpoint.value().state == BreakPointState::Disabled);

    if (!poke(breakpoint.value().address, (breakpoint.value().original_first_word & ~static_cast<FlatPtr>(0xff)) | BREAKPOINT_INSTRUCTION))
        return false;

    auto bp = m_breakpoints.get(breakpoint.value().address).value();
    bp.state = BreakPointState::Enabled;
    m_breakpoints.set(bp.address, bp);
    return true;
}

bool DebugSession::remove_breakpoint(FlatPtr address)
{
    if (!disable_breakpoint(address))
        return false;

    m_breakpoints.remove(address);
    return true;
}

bool DebugSession::breakpoint_exists(FlatPtr address) const
{
    return m_breakpoints.contains(address);
}

bool DebugSession::insert_watchpoint(FlatPtr address, u32 ebp)
{
    auto current_register_status = peek_debug(DEBUG_CONTROL_REGISTER);
    if (!current_register_status.has_value())
        return false;
    // FIXME: 64 bit support
    u32 dr7_value = static_cast<u32>(current_register_status.value());
    u32 next_available_index;
    for (next_available_index = 0; next_available_index < 4; next_available_index++) {
        auto bitmask = 1 << (next_available_index * 2);
        if ((dr7_value & bitmask) == 0)
            break;
    }
    if (next_available_index > 3)
        return false;
    WatchPoint watchpoint { address, next_available_index, ebp };

    if (!poke_debug(next_available_index, bit_cast<FlatPtr>(address)))
        return false;

    dr7_value |= (1u << (next_available_index * 2)); // Enable local breakpoint for our index
    auto condition_shift = 16 + (next_available_index * 4);
    dr7_value &= ~(0b11u << condition_shift);
    dr7_value |= 1u << condition_shift; // Trigger on writes
    auto length_shift = 18 + (next_available_index * 4);
    dr7_value &= ~(0b11u << length_shift);
    // FIXME: take variable size into account?
    dr7_value |= 0b11u << length_shift; // 4 bytes wide
    if (!poke_debug(DEBUG_CONTROL_REGISTER, dr7_value))
        return false;

    m_watchpoints.set(address, watchpoint);
    return true;
}

bool DebugSession::remove_watchpoint(FlatPtr address)
{
    if (!disable_watchpoint(address))
        return false;
    return m_watchpoints.remove(address);
}

bool DebugSession::disable_watchpoint(FlatPtr address)
{
    VERIFY(watchpoint_exists(address));
    auto watchpoint = m_watchpoints.get(address).value();
    if (!poke_debug(watchpoint.debug_register_index, 0))
        return false;
    auto current_register_status = peek_debug(DEBUG_CONTROL_REGISTER);
    if (!current_register_status.has_value())
        return false;
    u32 dr7_value = current_register_status.value();
    dr7_value &= ~(1u << watchpoint.debug_register_index * 2);
    if (!poke_debug(watchpoint.debug_register_index, dr7_value))
        return false;
    return true;
}

bool DebugSession::watchpoint_exists(FlatPtr address) const
{
    return m_watchpoints.contains(address);
}

PtraceRegisters DebugSession::get_registers() const
{
    PtraceRegisters regs;
    if (ptrace(PT_GETREGS, m_debuggee_pid, &regs, 0) < 0) {
        perror("PT_GETREGS");
        VERIFY_NOT_REACHED();
    }
    return regs;
}

void DebugSession::set_registers(PtraceRegisters const& regs)
{
    if (ptrace(PT_SETREGS, m_debuggee_pid, bit_cast<void*>(&regs), 0) < 0) {
        perror("PT_SETREGS");
        VERIFY_NOT_REACHED();
    }
}

void DebugSession::continue_debuggee(ContinueType type)
{
    int command = (type == ContinueType::FreeRun) ? PT_CONTINUE : PT_SYSCALL;
    if (ptrace(command, m_debuggee_pid, 0, 0) < 0) {
        perror("continue");
        VERIFY_NOT_REACHED();
    }
}

int DebugSession::continue_debuggee_and_wait(ContinueType type)
{
    continue_debuggee(type);
    int wstatus = 0;
    if (waitpid(m_debuggee_pid, &wstatus, WSTOPPED | WEXITED) != m_debuggee_pid) {
        perror("waitpid");
        VERIFY_NOT_REACHED();
    }
    return wstatus;
}

FlatPtr DebugSession::single_step()
{
    // Single stepping works by setting the x86 TRAP flag bit in the eflags register.
    // This flag causes the cpu to enter single-stepping mode, which causes
    // Interrupt 1 (debug interrupt) to be emitted after every instruction.
    // To single step the program, we set the TRAP flag and continue the debuggee.
    // After the debuggee has stopped, we clear the TRAP flag.

    auto regs = get_registers();
#if ARCH(X86_64)
    constexpr u32 TRAP_FLAG = 0x100;
    regs.rflags |= TRAP_FLAG;
#elif ARCH(AARCH64)
    TODO_AARCH64();
#elif ARCH(RISCV64)
    TODO_RISCV64();
#else
#    error Unknown architecture
#endif
    set_registers(regs);

    continue_debuggee();

    if (waitpid(m_debuggee_pid, 0, WSTOPPED) != m_debuggee_pid) {
        perror("waitpid");
        VERIFY_NOT_REACHED();
    }

    regs = get_registers();
#if ARCH(X86_64)
    regs.rflags &= ~(TRAP_FLAG);
#elif ARCH(AARCH64)
    TODO_AARCH64();
#elif ARCH(RISCV64)
    TODO_RISCV64();
#else
#    error Unknown architecture
#endif
    set_registers(regs);
    return regs.ip();
}

void DebugSession::detach()
{
    for (auto& breakpoint : m_breakpoints.keys()) {
        remove_breakpoint(breakpoint);
    }
    for (auto& watchpoint : m_watchpoints.keys())
        remove_watchpoint(watchpoint);
    continue_debuggee();
}

Optional<DebugSession::InsertBreakpointAtSymbolResult> DebugSession::insert_breakpoint(ByteString const& symbol_name)
{
    Optional<InsertBreakpointAtSymbolResult> result;
    for_each_loaded_library([this, symbol_name, &result](auto& lib) {
        // The loader contains its own definitions for LibC symbols, so we don't want to include it in the search.
        if (lib.name == "Loader.so")
            return IterationDecision::Continue;

        auto symbol = lib.debug_info->elf().find_demangled_function(symbol_name);
        if (!symbol.has_value())
            return IterationDecision::Continue;

        FlatPtr breakpoint_address = symbol->value() + lib.base_address;
        bool rc = this->insert_breakpoint(breakpoint_address);
        if (!rc)
            return IterationDecision::Break;

        result = InsertBreakpointAtSymbolResult { lib.name, breakpoint_address };
        return IterationDecision::Break;
    });
    return result;
}

Optional<DebugSession::InsertBreakpointAtSourcePositionResult> DebugSession::insert_breakpoint(ByteString const& filename, size_t line_number)
{
    auto address_and_source_position = get_address_from_source_position(filename, line_number);
    if (!address_and_source_position.has_value())
        return {};

    auto address = address_and_source_position.value().address;
    bool rc = this->insert_breakpoint(address);
    if (!rc)
        return {};

    auto lib = library_at(address);
    VERIFY(lib);

    return InsertBreakpointAtSourcePositionResult { lib->name, address_and_source_position.value().file, address_and_source_position.value().line, address };
}

ErrorOr<void> DebugSession::update_loaded_libs()
{
    auto file_name = TRY(String::formatted("/proc/{}/vm", m_debuggee_pid));
    auto file = TRY(Core::File::open(file_name, Core::File::OpenMode::Read));

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));

    auto const& vm_entries = json.as_array();
    Regex<PosixExtended> segment_name_re("(.+): ");

    auto get_path_to_object = [&segment_name_re](ByteString const& vm_name) -> Optional<ByteString> {
        if (vm_name == "/usr/lib/Loader.so")
            return vm_name;
        RegexResult result;
        auto rc = segment_name_re.search(vm_name, result);
        if (!rc)
            return {};
        auto lib_name = result.capture_group_matches.at(0).at(0).view.string_view().to_byte_string();
        if (lib_name.starts_with('/'))
            return lib_name;
        return ByteString::formatted("/usr/lib/{}", lib_name);
    };

    ScopeGuard progress_guard([this]() {
        if (m_on_initialization_progress)
            m_on_initialization_progress(0);
    });

    size_t vm_entry_index = 0;

    vm_entries.for_each([&](auto& entry) {
        ++vm_entry_index;
        if (m_on_initialization_progress)
            m_on_initialization_progress(vm_entry_index / static_cast<float>(vm_entries.size()));

        // TODO: check that region is executable
        auto vm_name = entry.as_object().get_byte_string("name"sv).value();

        auto object_path = get_path_to_object(vm_name);
        if (!object_path.has_value())
            return IterationDecision::Continue;

        ByteString lib_name = object_path.value();
        if (FileSystem::looks_like_shared_library(lib_name))
            lib_name = LexicalPath::basename(object_path.value());

        FlatPtr base_address = entry.as_object().get_addr("address"sv).value_or(0);
        if (auto it = m_loaded_libraries.find(lib_name); it != m_loaded_libraries.end()) {
            // We expect the VM regions to be sorted by address.
            VERIFY(base_address >= it->value->base_address);
            return IterationDecision::Continue;
        }

        auto file_or_error = Core::MappedFile::map(object_path.value());
        if (file_or_error.is_error())
            return IterationDecision::Continue;

        auto image = make<ELF::Image>(file_or_error.value()->bytes());
        auto debug_info = make<DebugInfo>(*image, m_source_root, base_address);
        auto lib = make<LoadedLibrary>(lib_name, file_or_error.release_value(), move(image), move(debug_info), base_address);
        m_loaded_libraries.set(lib_name, move(lib));

        return IterationDecision::Continue;
    });

    return {};
}

void DebugSession::stop_debuggee()
{
    kill(pid(), SIGSTOP);
}

}
