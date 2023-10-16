/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/SyscallString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDebug/DebugSession.h>
#include <LibDisassembly/Disassembler.h>
#include <LibDisassembly/x86/Instruction.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/arch/regs.h>
#include <syscall.h>
#include <unistd.h>

static OwnPtr<Debug::DebugSession> g_debug_session;
static bool g_should_output_color = false;

static void handle_sigint(int)
{
    outln("Debugger: SIGINT");

    // The destructor of DebugSession takes care of detaching
    g_debug_session = nullptr;
}

static void print_function_call(ByteString function_name, size_t depth)
{
    for (size_t i = 0; i < depth; ++i) {
        out("  ");
    }
    outln("=> {}", function_name);
}

static void print_syscall(PtraceRegisters& regs, size_t depth)
{
    for (size_t i = 0; i < depth; ++i) {
        out("  ");
    }
    StringView begin_color = g_should_output_color ? "\033[34;1m"sv : ""sv;
    StringView end_color = g_should_output_color ? "\033[0m"sv : ""sv;
#if ARCH(X86_64)
    outln("=> {}SC_{}({:#x}, {:#x}, {:#x}){}",
        begin_color,
        Syscall::to_string((Syscall::Function)regs.rax),
        regs.rdx,
        regs.rcx,
        regs.rbx,
        end_color);
#elif ARCH(AARCH64)
    (void)regs;
    (void)begin_color;
    (void)end_color;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    outln("=> {}SC_{}({:#x}, {:#x}, {:#x}){}",
        begin_color,
        Syscall::to_string((Syscall::Function)regs.x[16]),
        regs.x[9],
        regs.x[10],
        regs.x[11],
        end_color);
#else
#    error Unknown architecture
#endif
}

static NonnullOwnPtr<HashMap<FlatPtr, NonnullOwnPtr<Disassembly::Instruction>>> instrument_code()
{
    auto instrumented = make<HashMap<FlatPtr, NonnullOwnPtr<Disassembly::Instruction>>>();
    g_debug_session->for_each_loaded_library([&](Debug::LoadedLibrary const& lib) {
        lib.debug_info->elf().for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
            if (section.name() != ".text")
                return IterationDecision::Continue;

            Disassembly::SimpleInstructionStream stream((u8 const*)((uintptr_t)lib.file->data() + section.offset()), section.size());
            Disassembly::Disassembler disassembler(stream, Disassembly::architecture_from_elf_machine(lib.debug_info->elf().machine()).value_or(Disassembly::host_architecture()));
            for (;;) {
                auto offset = stream.offset();
                auto instruction_address = section.address() + offset + lib.base_address;
                auto insn = disassembler.next();
                if (!insn.has_value())
                    break;
                if (insn.value()->mnemonic() == "RET" || insn.value()->mnemonic() == "CALL") {
                    g_debug_session->insert_breakpoint(instruction_address);
                    instrumented->set(instruction_address, insn.release_value());
                }
            }
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });
    return instrumented;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio proc exec rpath sigaction ptrace"));

    if (isatty(STDOUT_FILENO))
        g_should_output_color = true;

    StringView command;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command,
        "The program to be traced, along with its arguments",
        "program", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto result = Debug::DebugSession::exec_and_attach(command);
    if (!result) {
        warnln("Failed to start debugging session for: \"{}\"", command);
        exit(1);
    }
    g_debug_session = result.release_nonnull();

    auto instrumented = instrument_code();

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    TRY(Core::System::sigaction(SIGINT, &sa, nullptr));

    size_t depth = 0;
    bool new_function = true;

    g_debug_session->run(Debug::DebugSession::DesiredInitialDebugeeState::Running, [&](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            outln("Program exited.");
            return Debug::DebugSession::DebugDecision::Detach;
        }

        if (reason == Debug::DebugSession::DebugBreakReason::Syscall) {
            print_syscall(regs.value(), depth + 1);
            return Debug::DebugSession::DebugDecision::ContinueBreakAtSyscall;
        }

#if ARCH(X86_64)
        FlatPtr const ip = regs.value().rip;
#elif ARCH(AARCH64)
        FlatPtr const ip = 0; // FIXME
        TODO_AARCH64();
#elif ARCH(RISCV64)
        FlatPtr const ip = regs.value().pc;
#else
#    error Unknown architecture
#endif

        if (new_function) {
            auto function_name = g_debug_session->symbolicate(ip);
            print_function_call(function_name.value().symbol, depth);
            new_function = false;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }
        auto instruction = instrumented->get(ip).value();

        if (instruction->mnemonic() == "RET") {
            if (depth != 0)
                --depth;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }

        // FIXME: we could miss some leaf functions that were called with a jump
        VERIFY(instruction->mnemonic() == "CALL");

        ++depth;
        new_function = true;

        return Debug::DebugSession::DebugDecision::SingleStep;
    });

    return 0;
}
