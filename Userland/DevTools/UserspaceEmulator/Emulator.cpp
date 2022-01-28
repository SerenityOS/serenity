/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.l@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Emulator.h"
#include "MmapRegion.h"
#include "SimpleRegion.h"
#include "SoftCPU.h"
#include <AK/Debug.h>
#include <AK/FileStream.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/StringUtils.h>
#include <Kernel/API/MemoryLayout.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
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

static constexpr u32 signal_trampoline_location = 0xb0000000;

static Emulator* s_the;

Emulator& Emulator::the()
{
    VERIFY(s_the);
    return *s_the;
}

Emulator::Emulator(String const& executable_path, Vector<StringView> const& arguments, Vector<String> const& environment)
    : m_executable_path(executable_path)
    , m_arguments(arguments)
    , m_environment(environment)
    , m_mmu(*this)
    , m_cpu(*this)
    , m_editor(Line::Editor::construct())
{
    m_malloc_tracer = make<MallocTracer>(*this);

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

Vector<ELF::AuxiliaryValue> Emulator::generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, String const& executable_path, int executable_fd) const
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
    m_range_allocator.reserve_user_range(VirtualAddress(stack_location), stack_size);
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    stack_region->set_stack(true);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(shadow_wrap_as_initialized<u32>(stack_location + stack_size));

    Vector<u32> argv_entries;

    for (auto const& argument : m_arguments) {
        m_cpu.push_string(argument);
        argv_entries.append(m_cpu.esp().value());
    }

    Vector<u32> env_entries;

    for (auto const& variable : m_environment) {
        m_cpu.push_string(variable.characters());
        env_entries.append(m_cpu.esp().value());
    }

    for (auto& auxv : aux_vector) {
        if (!auxv.optional_string.is_empty()) {
            m_cpu.push_string(auxv.optional_string);
            auxv.auxv.a_un.a_ptr = (void*)m_cpu.esp().value();
        }
    }

    for (ssize_t i = aux_vector.size() - 1; i >= 0; --i) {
        auto& value = aux_vector[i].auxv;
        m_cpu.push_buffer((u8 const*)&value, sizeof(value));
    }

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** envp = { envv_entries..., nullptr }
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(env_entries[i]));
    u32 envp = m_cpu.esp().value();

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** argv = { argv_entries..., nullptr }
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(argv_entries[i]));
    u32 argv = m_cpu.esp().value();

    while ((m_cpu.esp().value() + 4) % 16 != 0)
        m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // (alignment)

    u32 argc = argv_entries.size();
    m_cpu.push32(shadow_wrap_as_initialized(envp));
    m_cpu.push32(shadow_wrap_as_initialized(argv));
    m_cpu.push32(shadow_wrap_as_initialized(argc));

    VERIFY(m_cpu.esp().value() % 16 == 0);
}

bool Emulator::load_elf()
{
    auto file_or_error = Core::MappedFile::map(m_executable_path);
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

    StringBuilder interpreter_path_builder;
    auto result_or_error = ELF::validate_program_headers(*(Elf32_Ehdr const*)elf_image_data.data(), elf_image_data.size(), elf_image_data, &interpreter_path_builder);
    if (result_or_error.is_error() || !result_or_error.value()) {
        reportln("failed to validate ELF file");
        return false;
    }
    auto interpreter_path = interpreter_path_builder.string_view();

    VERIFY(!interpreter_path.is_null());
    dbgln("interpreter: {}", interpreter_path);

    auto interpreter_file_or_error = Core::MappedFile::map(interpreter_path);
    VERIFY(!interpreter_file_or_error.is_error());
    auto interpreter_image_data = interpreter_file_or_error.value()->bytes();
    ELF::Image interpreter_image(interpreter_image_data);

    constexpr FlatPtr interpreter_load_offset = 0x08000000;
    interpreter_image.for_each_program_header([&](ELF::Image::ProgramHeader const& program_header) {
        // Loader is not allowed to have its own TLS regions
        VERIFY(program_header.type() != PT_TLS);

        if (program_header.type() == PT_LOAD) {
            auto start_address = program_header.vaddr().offset(interpreter_load_offset);
            m_range_allocator.reserve_user_range(start_address, program_header.size_in_memory());
            auto region = make<SimpleRegion>(start_address.get(), program_header.size_in_memory());
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

    size_t instructions_until_next_profile_dump = profile_instruction_interval();
    if (is_profiling() && m_loader_text_size.has_value())
        emit_profile_event(profile_stream(), "mmap", String::formatted(R"("ptr": {}, "size": {}, "name": "/usr/lib/Loader.so")", *m_loader_text_base, *m_loader_text_size));

    while (!m_shutdown) {
        if (m_steps_til_pause) [[likely]] {
            m_cpu.save_base_eip();
            auto insn = X86::Instruction::from_stream(m_cpu, true, true);
            // Exec cycle
            if constexpr (trace) {
                outln("{:p}  \033[33;1m{}\033[0m", m_cpu.base_eip(), insn.to_string(m_cpu.base_eip(), symbol_provider));
            }

            (m_cpu.*insn.handler())(insn);

            if (is_profiling()) {
                if (instructions_until_next_profile_dump == 0) {
                    instructions_until_next_profile_dump = profile_instruction_interval();
                    emit_profile_sample(profile_stream());
                } else {
                    --instructions_until_next_profile_dump;
                }
            }

            if constexpr (trace) {
                m_cpu.dump();
            }

            if (m_pending_signals) [[unlikely]] {
                dispatch_one_pending_signal();
            }
            if (m_steps_til_pause > 0)
                m_steps_til_pause--;

        } else {
            handle_repl();
        }
    }

    if (auto* tracer = malloc_tracer())
        tracer->dump_leak_report();

    return m_exit_status;
}

void Emulator::handle_repl()
{
    // Console interface
    // FIXME: Previous Instruction**s**
    // FIXME: Function names (base, call, jump)
    auto saved_eip = m_cpu.eip();
    m_cpu.save_base_eip();
    auto insn = X86::Instruction::from_stream(m_cpu, true, true);
    // FIXME: This does not respect inlining
    //        another way of getting the current function is at need
    if (auto symbol = symbol_at(m_cpu.base_eip()); symbol.has_value()) {
        outln("[{}]: {}", symbol->lib_name, symbol->symbol);
    }

    outln("==> {}", create_instruction_line(m_cpu.base_eip(), insn));
    for (int i = 0; i < 7; ++i) {
        m_cpu.save_base_eip();
        insn = X86::Instruction::from_stream(m_cpu, true, true);
        outln("    {}", create_instruction_line(m_cpu.base_eip(), insn));
    }
    // We don't want to increase EIP here, we just want the instructions
    m_cpu.set_eip(saved_eip);

    outln();
    m_cpu.dump();
    outln();

    auto line_or_error = m_editor->get_line(">> ");
    if (line_or_error.is_error())
        return;

    // FIXME: find a way to find a global symbol-address for run-until-call
    auto help = [] {
        outln("Available commands:");
        outln("continue, c: Continue the execution");
        outln("quit, q: Quit the execution (this will \"kill\" the program and run checks)");
        outln("ret, r: Run until function returns");
        outln("step, s [count]: Execute [count] instructions and then halt");
        outln("signal, sig [number:int], send signal to emulated program (default: sigint:2)");
    };
    auto line = line_or_error.release_value();
    if (line.is_empty()) {
        if (m_editor->history().is_empty()) {
            help();
            return;
        }
        line = m_editor->history().last().entry;
    }

    auto parts = line.split_view(' ', false);
    m_editor->add_to_history(line);

    if (parts[0].is_one_of("s"sv, "step"sv)) {
        if (parts.size() == 1) {
            m_steps_til_pause = 1;
            return;
        }
        auto number = AK::StringUtils::convert_to_int<i64>(parts[1]);
        if (!number.has_value()) {
            outln("usage \"step [count]\"\n\tcount can't be less than 1");
            return;
        }
        m_steps_til_pause = number.value();
    } else if (parts[0].is_one_of("c"sv, "continue"sv)) {
        m_steps_til_pause = -1;
    } else if (parts[0].is_one_of("r"sv, "ret"sv)) {
        m_run_til_return = true;
        // FIXME: This may be uninitialized
        m_watched_addr = m_mmu.read32({ 0x23, m_cpu.ebp().value() + 4 }).value();
        m_steps_til_pause = -1;
    } else if (parts[0].is_one_of("q"sv, "quit"sv)) {
        m_shutdown = true;
    } else if (parts[0].is_one_of("sig"sv, "signal"sv)) {
        if (parts.size() == 1) {
            did_receive_signal(SIGINT);
            return;
        }
        if (parts.size() == 2) {
            auto number = AK::StringUtils::convert_to_int<i32>(parts[1]);
            if (number.has_value()) {
                did_receive_signal(number.value());
                return;
            }
        }
        outln("Usage: sig [signal:int], default: SINGINT:2");
    } else {
        help();
    }
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

MmapRegion const* Emulator::find_text_region(FlatPtr address)
{
    MmapRegion const* matching_region = nullptr;
    mmu().for_each_region_of_type<MmapRegion>([&](auto& region) {
        if (!(region.is_executable() && address >= region.base() && address < region.base() + region.size()))
            return IterationDecision::Continue;
        matching_region = &region;
        return IterationDecision::Break;
    });
    return matching_region;
}

// FIXME: This interface isn't the nicest
MmapRegion const* Emulator::load_library_from_address(FlatPtr address)
{
    auto const* region = find_text_region(address);
    if (!region)
        return {};

    String lib_name = region->lib_name();
    if (lib_name.is_null())
        return {};

    String lib_path = lib_name;
    if (Core::File::looks_like_shared_library(lib_name))
        lib_path = String::formatted("/usr/lib/{}", lib_path);

    if (!m_dynamic_library_cache.contains(lib_path)) {
        auto file_or_error = Core::MappedFile::map(lib_path);
        if (file_or_error.is_error())
            return {};

        auto image = make<ELF::Image>(file_or_error.value()->bytes());
        auto debug_info = make<Debug::DebugInfo>(*image);
        m_dynamic_library_cache.set(lib_path, CachedELF { file_or_error.release_value(), move(debug_info), move(image) });
    }
    return region;
}

MmapRegion const* Emulator::first_region_for_object(StringView name)
{
    MmapRegion* ret = nullptr;
    mmu().for_each_region_of_type<MmapRegion>([&](auto& region) {
        if (region.lib_name() == name) {
            ret = &region;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return ret;
}

// FIXME: This disregards function inlining.
Optional<Emulator::SymbolInfo> Emulator::symbol_at(FlatPtr address)
{
    auto const* address_region = load_library_from_address(address);
    if (!address_region)
        return {};
    auto lib_name = address_region->lib_name();
    auto const* first_region = (lib_name.is_null() || lib_name.is_empty()) ? address_region : first_region_for_object(lib_name);
    VERIFY(first_region);
    auto lib_path = lib_name;
    if (Core::File::looks_like_shared_library(lib_name)) {
        lib_path = String::formatted("/usr/lib/{}", lib_name);
    }

    auto it = m_dynamic_library_cache.find(lib_path);
    auto const& elf = it->value.debug_info->elf();
    auto symbol = elf.symbolicate(address - first_region->base());

    auto source_position = it->value.debug_info->get_source_position(address - first_region->base());
    return { { lib_name, symbol, source_position } };
}

String Emulator::create_backtrace_line(FlatPtr address)
{
    auto maybe_symbol = symbol_at(address);
    if (!maybe_symbol.has_value()) {
        return String::formatted("=={}==    {:p}", getpid(), address);
    }
    if (!maybe_symbol->source_position.has_value()) {
        return String::formatted("=={}==    {:p}  [{}]: {}", getpid(), address, maybe_symbol->lib_name, maybe_symbol->symbol);
    }

    auto const& source_position = maybe_symbol->source_position.value();
    return String::formatted("=={}==    {:p}  [{}]: {} (\e[34;1m{}\e[0m:{})", getpid(), address, maybe_symbol->lib_name, maybe_symbol->symbol, LexicalPath::basename(source_position.file_path), source_position.line_number);
}

void Emulator::dump_backtrace(Vector<FlatPtr> const& backtrace)
{
    for (auto const& address : backtrace) {
        reportln("{}", create_backtrace_line(address));
    }
}

void Emulator::dump_backtrace()
{
    dump_backtrace(raw_backtrace());
}

void Emulator::emit_profile_sample(AK::OutputStream& output)
{
    if (!is_in_region_of_interest())
        return;
    StringBuilder builder;
    timeval tv {};
    gettimeofday(&tv, nullptr);
    builder.appendff(R"~(, {{"type": "sample", "pid": {}, "tid": {}, "timestamp": {}, "lost_samples": 0, "stack": [)~", getpid(), gettid(), tv.tv_sec * 1000 + tv.tv_usec / 1000);
    builder.join(',', raw_backtrace());
    builder.append("]}\n");
    output.write_or_error(builder.string_view().bytes());
}

void Emulator::emit_profile_event(AK::OutputStream& output, StringView event_name, String const& contents)
{
    StringBuilder builder;
    timeval tv {};
    gettimeofday(&tv, nullptr);
    builder.appendff(R"~(, {{"type": "{}", "pid": {}, "tid": {}, "timestamp": {}, "lost_samples": 0, "stack": [], {}}})~", event_name, getpid(), gettid(), tv.tv_sec * 1000 + tv.tv_usec / 1000, contents);
    builder.append('\n');
    output.write_or_error(builder.string_view().bytes());
}

String Emulator::create_instruction_line(FlatPtr address, X86::Instruction const& insn)
{
    auto symbol = symbol_at(address);
    if (!symbol.has_value() || !symbol->source_position.has_value())
        return String::formatted("{:p}: {}", address, insn.to_string(address));

    return String::formatted("{:p}: {} \e[34;1m{}\e[0m:{}", address, insn.to_string(address), LexicalPath::basename(symbol->source_position->file_path), symbol->source_position.value().line_number);
}

static void emulator_signal_handler(int signum)
{
    Emulator::the().did_receive_signal(signum);
}

static void emulator_sigint_handler(int signum)
{
    Emulator::the().did_receive_sigint(signum);
}

void Emulator::register_signal_handlers()
{
    for (int signum = 0; signum < NSIG; ++signum)
        signal(signum, emulator_signal_handler);
    signal(SIGINT, emulator_sigint_handler);
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

    if (((1 << (signum - 1)) & m_signal_mask) != 0)
        return;

    auto& handler = m_signal_handler[signum];

    if (handler.handler == 0) {
        // SIG_DFL
        auto action = default_signal_action(signum);
        if (action == DefaultSignalAction::Ignore)
            return;
        reportln("\n=={}== Got signal {} ({}), no handler registered", getpid(), signum, strsignal(signum));
        dump_backtrace();
        m_shutdown = true;
        return;
    }

    if (handler.handler == 1) {
        // SIG_IGN
        return;
    }

    reportln("\n=={}== Got signal {} ({}), handler at {:p}", getpid(), signum, strsignal(signum), handler.handler);

    auto old_esp = m_cpu.esp();

    u32 stack_alignment = (m_cpu.esp().value() - 52) % 16;
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

    VERIFY((m_cpu.esp().value() % 16) == 0);

    m_cpu.push32(shadow_wrap_as_initialized(0u));

    m_cpu.set_eip(m_signal_trampoline);
}

// Make sure the compiler doesn't "optimize away" this function:
static void signal_trampoline_dummy() __attribute__((used));
NEVER_INLINE void signal_trampoline_dummy()
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
    m_range_allocator.reserve_user_range(VirtualAddress(signal_trampoline_location), 4096);
    auto trampoline_region = make<SimpleRegion>(signal_trampoline_location, 4096);

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    u8* code_ptr = trampoline_region->data();
    memcpy(code_ptr, trampoline, trampoline_size);

    m_signal_trampoline = trampoline_region->base();
    mmu().add_region(move(trampoline_region));
}

void Emulator::dump_regions() const
{
    const_cast<SoftMMU&>(m_mmu).for_each_region([&](Region const& region) {
        reportln("{:p}-{:p}  {:c}{:c}{:c} {}  {}{}{} ",
            region.base(),
            region.end() - 1,
            region.is_readable() ? 'R' : '-',
            region.is_writable() ? 'W' : '-',
            region.is_executable() ? 'X' : '-',
            is<MmapRegion>(region) ? static_cast<MmapRegion const&>(region).name() : "",
            is<MmapRegion>(region) ? "(mmap) " : "",
            region.is_stack() ? "(stack) " : "",
            region.is_text() ? "(text) " : "");
        return IterationDecision::Continue;
    });
}

}
