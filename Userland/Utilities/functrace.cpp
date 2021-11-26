/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDebug/DebugSession.h>
#include <LibELF/Image.h>
#include <LibX86/Disassembler.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
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

static void print_function_call(String function_name, size_t depth)
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
    const char* begin_color = g_should_output_color ? "\033[34;1m" : "";
    const char* end_color = g_should_output_color ? "\033[0m" : "";
#if ARCH(I386)
    outln("=> {}SC_{}({:#x}, {:#x}, {:#x}){}",
        begin_color,
        Syscall::to_string((Syscall::Function)regs.eax),
        regs.edx,
        regs.ecx,
        regs.ebx,
        end_color);
#else
    outln("=> {}SC_{}({:#x}, {:#x}, {:#x}){}",
        begin_color,
        Syscall::to_string((Syscall::Function)regs.rax),
        regs.rdx,
        regs.rcx,
        regs.rbx,
        end_color);
#endif
}

static NonnullOwnPtr<HashMap<void*, X86::Instruction>> instrument_code()
{
    auto instrumented = make<HashMap<void*, X86::Instruction>>();
    g_debug_session->for_each_loaded_library([&](const Debug::LoadedLibrary& lib) {
        lib.debug_info->elf().for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
            if (section.name() != ".text")
                return IterationDecision::Continue;

            X86::SimpleInstructionStream stream((const u8*)((uintptr_t)lib.file->data() + section.offset()), section.size());
            X86::Disassembler disassembler(stream);
            for (;;) {
                auto offset = stream.offset();
                void* instruction_address = (void*)(section.address() + offset + lib.base_address);
                auto insn = disassembler.next();
                if (!insn.has_value())
                    break;
                if (insn.value().mnemonic() == "RET" || insn.value().mnemonic() == "CALL") {
                    g_debug_session->insert_breakpoint(instruction_address);
                    instrumented->set(instruction_address, insn.value());
                }
            }
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });
    return instrumented;
}

int main(int argc, char** argv)
{
    if (pledge("stdio proc exec rpath sigaction ptrace", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (isatty(STDOUT_FILENO))
        g_should_output_color = true;

    const char* command = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command,
        "The program to be traced, along with its arguments",
        "program", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

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
    sigaction(SIGINT, &sa, nullptr);

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

#if ARCH(I386)
        const FlatPtr ip = regs.value().eip;
#else
        const FlatPtr ip = regs.value().rip;
#endif

        if (new_function) {
            auto function_name = g_debug_session->symbolicate(ip);
            print_function_call(function_name.value().symbol, depth);
            new_function = false;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }
        auto instruction = instrumented->get((void*)ip).value();

        if (instruction.mnemonic() == "RET") {
            if (depth != 0)
                --depth;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }

        // FIXME: we could miss some leaf functions that were called with a jump
        VERIFY(instruction.mnemonic() == "CALL");

        ++depth;
        new_function = true;

        return Debug::DebugSession::DebugDecision::SingleStep;
    });
}
