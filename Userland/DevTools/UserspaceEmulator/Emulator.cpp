/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

#include "Emulator.h"
#include "MmapRegion.h"
#include "SimpleRegion.h"
#include "SoftCPU.h"
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <LibX86/ELFSymbolProvider.h>
#include <fcntl.h>
#include <syscall.h>
#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 1 * MiB;

static Emulator* s_the;

Emulator& Emulator::the()
{
    VERIFY(s_the);
    return *s_the;
}

Emulator::Emulator(const String& executable_path, const Vector<String>& arguments, const Vector<String>& environment)
    : m_executable_path(executable_path)
    , m_arguments(arguments)
    , m_environment(environment)
    , m_mmu(*this)
    , m_cpu(*this)
{
    m_malloc_tracer = make<MallocTracer>(*this);

    static constexpr FlatPtr userspace_range_base = 0x00800000;
    static constexpr FlatPtr userspace_range_ceiling = 0xbe000000;
#ifdef UE_ASLR
    static constexpr FlatPtr page_mask = 0xfffff000u;
    size_t random_offset = (get_random<u8>() % 32 * MiB) & page_mask;
    FlatPtr base = userspace_range_base + random_offset;
#else
    FlatPtr base = userspace_range_base;
#endif

    m_range_allocator.initialize_with_range(VirtualAddress(base), userspace_range_ceiling - base);

    VERIFY(!s_the);
    s_the = this;
    // setup_stack(arguments, environment);
    register_signal_handlers();
    setup_signal_trampoline();
}

Vector<ELF::AuxiliaryValue> Emulator::generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, String executable_path, int executable_fd) const
{
    // FIXME: This is not fully compatible with the auxiliary vector the kernel generates, this is just the bare
    //        minimum to get the loader going.
    Vector<ELF::AuxiliaryValue> auxv;
    // PHDR/EXECFD
    // PH*
    auxv.append({ ELF::AuxiliaryValue::PageSize, PAGE_SIZE });
    auxv.append({ ELF::AuxiliaryValue::BaseAddress, (void*)load_base });

    auxv.append({ ELF::AuxiliaryValue::Entry, (void*)entry_eip });

    // FIXME: Don't hard code this? We might support other platforms later.. (e.g. x86_64)
    auxv.append({ ELF::AuxiliaryValue::Platform, "i386" });

    auxv.append({ ELF::AuxiliaryValue::ExecFilename, executable_path });

    auxv.append({ ELF::AuxiliaryValue::ExecFileDescriptor, executable_fd });

    auxv.append({ ELF::AuxiliaryValue::Null, 0L });
    return auxv;
}

void Emulator::setup_stack(Vector<ELF::AuxiliaryValue> aux_vector)
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    stack_region->set_stack(true);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(shadow_wrap_as_initialized<u32>(stack_location + stack_size));

    Vector<u32> argv_entries;

    for (auto& argument : m_arguments) {
        m_cpu.push_string(argument.characters());
        argv_entries.append(m_cpu.esp().value());
    }

    Vector<u32> env_entries;

    for (auto& variable : m_environment) {
        m_cpu.push_string(variable.characters());
        env_entries.append(m_cpu.esp().value());
    }

    for (auto& auxv : aux_vector) {
        if (!auxv.optional_string.is_empty()) {
            m_cpu.push_string(auxv.optional_string.characters());
            auxv.auxv.a_un.a_ptr = (void*)m_cpu.esp().value();
        }
    }

    for (ssize_t i = aux_vector.size() - 1; i >= 0; --i) {
        auto& value = aux_vector[i].auxv;
        m_cpu.push_buffer((const u8*)&value, sizeof(value));
    }

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** envp = { envv_entries..., nullptr }
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(env_entries[i]));
    u32 envp = m_cpu.esp().value();

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** argv = { argv_entries..., nullptr }
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(argv_entries[i]));
    u32 argv = m_cpu.esp().value();

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // (alignment)

    u32 argc = argv_entries.size();
    m_cpu.push32(shadow_wrap_as_initialized(envp));
    m_cpu.push32(shadow_wrap_as_initialized(argv));
    m_cpu.push32(shadow_wrap_as_initialized(argc));
    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // (alignment)
}

bool Emulator::load_elf()
{
    auto file_or_error = MappedFile::map(m_executable_path);
    if (file_or_error.is_error()) {
        reportln("Unable to map {}: {}", m_executable_path, file_or_error.error());
        return false;
    }

    auto elf_image_data = file_or_error.value()->bytes();
    ELF::Image executable_elf(elf_image_data);

    if (!executable_elf.is_dynamic()) {
        // FIXME: Support static objects
        VERIFY_NOT_REACHED();
    }

    String interpreter_path;
    if (!ELF::validate_program_headers(*(const Elf32_Ehdr*)elf_image_data.data(), elf_image_data.size(), (const u8*)elf_image_data.data(), elf_image_data.size(), &interpreter_path)) {
        reportln("failed to validate ELF file");
        return false;
    }

    VERIFY(!interpreter_path.is_null());
    dbgln("interpreter: {}", interpreter_path);

    auto interpreter_file_or_error = MappedFile::map(interpreter_path);
    VERIFY(!interpreter_file_or_error.is_error());
    auto interpreter_image_data = interpreter_file_or_error.value()->bytes();
    ELF::Image interpreter_image(interpreter_image_data);

    constexpr FlatPtr interpreter_load_offset = 0x08000000;
    interpreter_image.for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        // Loader is not allowed to have its own TLS regions
        VERIFY(program_header.type() != PT_TLS);

        if (program_header.type() == PT_LOAD) {
            auto region = make<SimpleRegion>(program_header.vaddr().offset(interpreter_load_offset).get(), program_header.size_in_memory());
            if (program_header.is_executable() && !program_header.is_writable())
                region->set_text(true);
            memcpy(region->data(), program_header.raw_data(), program_header.size_in_image());
            memset(region->shadow_data(), 0x01, program_header.size_in_memory());
            if (program_header.is_executable()) {
                m_loader_text_base = region->base();
                m_loader_text_size = region->size();
            }
            mmu().add_region(move(region));
            return IterationDecision::Continue;
        }

        return IterationDecision::Continue;
    });

    auto entry_point = interpreter_image.entry().offset(interpreter_load_offset).get();
    m_cpu.set_eip(entry_point);

    // executable_fd will be used by the loader
    int executable_fd = open(m_executable_path.characters(), O_RDONLY);
    if (executable_fd < 0)
        return false;

    auto aux_vector = generate_auxiliary_vector(interpreter_load_offset, entry_point, m_executable_path, executable_fd);
    setup_stack(move(aux_vector));

    return true;
}

int Emulator::exec()
{
    // X86::ELFSymbolProvider symbol_provider(*m_elf);
    X86::ELFSymbolProvider* symbol_provider = nullptr;

    constexpr bool trace = false;

    while (!m_shutdown) {
        m_cpu.save_base_eip();

        auto insn = X86::Instruction::from_stream(m_cpu, true, true);

        if constexpr (trace) {
            outln("{:p}  \033[33;1m{}\033[0m", m_cpu.base_eip(), insn.to_string(m_cpu.base_eip(), symbol_provider));
        }

        (m_cpu.*insn.handler())(insn);

        if constexpr (trace) {
            m_cpu.dump();
        }

        if (m_pending_signals) [[unlikely]] {
            dispatch_one_pending_signal();
        }
    }

    if (auto* tracer = malloc_tracer())
        tracer->dump_leak_report();

    return m_exit_status;
}

Vector<FlatPtr> Emulator::raw_backtrace()
{
    Vector<FlatPtr, 128> backtrace;
    backtrace.append(m_cpu.base_eip());

    // FIXME: Maybe do something if the backtrace has uninitialized data in the frame chain.

    u32 frame_ptr = m_cpu.ebp().value();
    while (frame_ptr) {
        u32 ret_ptr = m_mmu.read32({ 0x23, frame_ptr + 4 }).value();
        if (!ret_ptr)
            break;
        backtrace.append(ret_ptr);
        frame_ptr = m_mmu.read32({ 0x23, frame_ptr }).value();
    }
    return backtrace;
}

const MmapRegion* Emulator::find_text_region(FlatPtr address)
{
    const MmapRegion* matching_region = nullptr;
    mmu().for_each_region([&](auto& region) {
        if (!is<MmapRegion>(region))
            return IterationDecision::Continue;
        const auto& mmap_region = static_cast<const MmapRegion&>(region);
        if (!(mmap_region.is_executable() && address >= mmap_region.base() && address < mmap_region.base() + mmap_region.size()))
            return IterationDecision::Continue;
        matching_region = &mmap_region;
        return IterationDecision::Break;
    });
    return matching_region;
}

String Emulator::create_backtrace_line(FlatPtr address)
{
    String minimal = String::format("=={%d}==    %p", getpid(), (void*)address);
    const auto* region = find_text_region(address);
    if (!region)
        return minimal;
    auto separator_index = region->name().index_of(":");
    if (!separator_index.has_value())
        return minimal;

    String lib_name = region->name().substring(0, separator_index.value());
    String lib_path = lib_name;
    if (region->name().contains(".so"))
        lib_path = String::formatted("/usr/lib/{}", lib_path);

    if (!m_dynamic_library_cache.contains(lib_path)) {
        auto file_or_error = MappedFile::map(lib_path);
        if (file_or_error.is_error())
            return minimal;

        auto debug_info = make<Debug::DebugInfo>(make<ELF::Image>(file_or_error.value()->bytes()));
        m_dynamic_library_cache.set(lib_path, CachedELF { file_or_error.release_value(), move(debug_info) });
    }

    auto it = m_dynamic_library_cache.find(lib_path);
    auto& elf = it->value.debug_info->elf();
    String symbol = elf.symbolicate(address - region->base());

    auto line_without_source_info = String::format("=={%d}==    %p  [%s]: %s", getpid(), (void*)address, lib_name.characters(), symbol.characters());

    auto source_position = it->value.debug_info->get_source_position(address - region->base());
    if (source_position.has_value())
        return String::format("=={%d}==    %p  [%s]: %s (\033[34;1m%s\033[0m:%zu)", getpid(), (void*)address, lib_name.characters(), symbol.characters(), LexicalPath(source_position.value().file_path).basename().characters(), source_position.value().line_number);

    return line_without_source_info;
}

void Emulator::dump_backtrace(const Vector<FlatPtr>& backtrace)
{
    for (auto& address : backtrace) {
        reportln("{}", create_backtrace_line(address));
    }
}

void Emulator::dump_backtrace()
{
    dump_backtrace(raw_backtrace());
}

static void emulator_signal_handler(int signum)
{
    Emulator::the().did_receive_signal(signum);
}

void Emulator::register_signal_handlers()
{
    for (int signum = 0; signum < NSIG; ++signum)
        signal(signum, emulator_signal_handler);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

static DefaultSignalAction default_signal_action(int signal)
{
    VERIFY(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
    case SIGINFO:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    VERIFY_NOT_REACHED();
}

void Emulator::dispatch_one_pending_signal()
{
    int signum = -1;
    for (signum = 1; signum < NSIG; ++signum) {
        int mask = 1 << signum;
        if (m_pending_signals & mask)
            break;
    }
    VERIFY(signum != -1);
    m_pending_signals &= ~(1 << signum);

    auto& handler = m_signal_handler[signum];

    if (handler.handler == 0) {
        // SIG_DFL
        auto action = default_signal_action(signum);
        if (action == DefaultSignalAction::Ignore)
            return;
        reportln("\n=={}== Got signal {} ({}), no handler registered", getpid(), signum, strsignal(signum));
        m_shutdown = true;
        return;
    }

    if (handler.handler == 1) {
        // SIG_IGN
        return;
    }

    reportln("\n=={}== Got signal {} ({}), handler at {:p}", getpid(), signum, strsignal(signum), handler.handler);

    auto old_esp = m_cpu.esp();

    u32 stack_alignment = (m_cpu.esp().value() - 56) % 16;
    m_cpu.set_esp(shadow_wrap_as_initialized(m_cpu.esp().value() - stack_alignment));

    m_cpu.push32(shadow_wrap_as_initialized(m_cpu.eflags()));
    m_cpu.push32(shadow_wrap_as_initialized(m_cpu.eip()));
    m_cpu.push32(m_cpu.eax());
    m_cpu.push32(m_cpu.ecx());
    m_cpu.push32(m_cpu.edx());
    m_cpu.push32(m_cpu.ebx());
    m_cpu.push32(old_esp);
    m_cpu.push32(m_cpu.ebp());
    m_cpu.push32(m_cpu.esi());
    m_cpu.push32(m_cpu.edi());

    // FIXME: Push old signal mask here.
    m_cpu.push32(shadow_wrap_as_initialized(0u));

    m_cpu.push32(shadow_wrap_as_initialized((u32)signum));
    m_cpu.push32(shadow_wrap_as_initialized(handler.handler));
    m_cpu.push32(shadow_wrap_as_initialized(0u));

    VERIFY((m_cpu.esp().value() % 16) == 0);

    m_cpu.set_eip(m_signal_trampoline);
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy();
void signal_trampoline_dummy()
{
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // necessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        "asm_signal_trampoline:\n"
        "push ebp\n"
        "mov ebp, esp\n"
        "push eax\n"          // we have to store eax 'cause it might be the return value from a syscall
        "sub esp, 4\n"        // align the stack to 16 bytes
        "mov eax, [ebp+12]\n" // push the signal code
        "push eax\n"
        "call [ebp+8]\n" // call the signal handler
        "add esp, 8\n"
        "mov eax, %P0\n"
        "int 0x82\n" // sigreturn syscall
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
}

extern "C" void asm_signal_trampoline(void);
extern "C" void asm_signal_trampoline_end(void);

void Emulator::setup_signal_trampoline()
{
    auto trampoline_region = make<SimpleRegion>(0xb0000000, 4096);

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    u8* code_ptr = trampoline_region->data();
    memcpy(code_ptr, trampoline, trampoline_size);

    m_signal_trampoline = trampoline_region->base();
    mmu().add_region(move(trampoline_region));
}

bool Emulator::find_malloc_symbols(const MmapRegion& libc_text)
{
    auto file_or_error = MappedFile::map("/usr/lib/libc.so");
    if (file_or_error.is_error())
        return false;

    ELF::Image image(file_or_error.value()->bytes());
    auto malloc_symbol = image.find_demangled_function("malloc");
    auto free_symbol = image.find_demangled_function("free");
    auto realloc_symbol = image.find_demangled_function("realloc");
    auto calloc_symbol = image.find_demangled_function("calloc");
    auto malloc_size_symbol = image.find_demangled_function("malloc_size");
    if (!malloc_symbol.has_value() || !free_symbol.has_value() || !realloc_symbol.has_value() || !malloc_size_symbol.has_value())
        return false;

    m_malloc_symbol_start = malloc_symbol.value().value() + libc_text.base();
    m_malloc_symbol_end = m_malloc_symbol_start + malloc_symbol.value().size();
    m_free_symbol_start = free_symbol.value().value() + libc_text.base();
    m_free_symbol_end = m_free_symbol_start + free_symbol.value().size();
    m_realloc_symbol_start = realloc_symbol.value().value() + libc_text.base();
    m_realloc_symbol_end = m_realloc_symbol_start + realloc_symbol.value().size();
    m_calloc_symbol_start = calloc_symbol.value().value() + libc_text.base();
    m_calloc_symbol_end = m_calloc_symbol_start + calloc_symbol.value().size();
    m_malloc_size_symbol_start = malloc_size_symbol.value().value() + libc_text.base();
    m_malloc_size_symbol_end = m_malloc_size_symbol_start + malloc_size_symbol.value().size();
    return true;
}

void Emulator::dump_regions() const
{
    const_cast<SoftMMU&>(m_mmu).for_each_region([&](const Region& region) {
        reportln("{:p}-{:p}  {:c}{:c}{:c} {}  {}{}{} ",
            region.base(),
            region.end() - 1,
            region.is_readable() ? 'R' : '-',
            region.is_writable() ? 'W' : '-',
            region.is_executable() ? 'X' : '-',
            is<MmapRegion>(region) ? static_cast<const MmapRegion&>(region).name() : "",
            is<MmapRegion>(region) ? "(mmap) " : "",
            region.is_stack() ? "(stack) " : "",
            region.is_text() ? "(text) " : "");
        return IterationDecision::Continue;
    });
}

}
