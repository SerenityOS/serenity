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

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Demangle.h>
#include <AK/HashMap.h>
#include <AK/LogStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/kmalloc.h>
#include <Kernel/API/Syscall.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDebug/DebugSession.h>
#include <LibELF/Image.h>
#include <LibX86/Disassembler.h>
#include <LibX86/Instruction.h>
#include <math.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static OwnPtr<Debug::DebugSession> g_debug_session;
static bool g_should_output_color = false;

static void handle_sigint(int)
{
    printf("Debugger: SIGINT\n");

    // The destructor of DebugSession takes care of detaching
    g_debug_session = nullptr;
}

static void print_function_call(String function_name, size_t depth)
{
    for (size_t i = 0; i < depth; ++i) {
        printf("  ");
    }
    printf("=> %s\n", function_name.characters());
}

static void print_syscall(PtraceRegisters& regs, size_t depth)
{
    for (size_t i = 0; i < depth; ++i) {
        printf("  ");
    }
    const char* begin_color = g_should_output_color ? "\033[34;1m" : "";
    const char* end_color = g_should_output_color ? "\033[0m" : "";
    printf("=> %sSC_%s(0x%x, 0x%x, 0x%x)%s\n",
        begin_color,
        Syscall::to_string(
            (Syscall::Function)regs.eax),
        regs.edx,
        regs.ecx,
        regs.ebx,
        end_color);
}

static NonnullOwnPtr<HashMap<void*, X86::Instruction>> instrument_code()
{
    (void)demangle("foo"); // Required for linked with __cxa_demangle
    auto instrumented = make<HashMap<void*, X86::Instruction>>();
    g_debug_session->elf().image().for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
        if (section.name() != ".text")
            return IterationDecision::Continue;

        X86::SimpleInstructionStream stream((const u8*)((u32)g_debug_session->executable().data() + section.offset()), section.size());
        X86::Disassembler disassembler(stream);
        for (;;) {
            auto offset = stream.offset();
            void* instruction_address = (void*)(section.address() + offset);
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
    return instrumented;
}

int main(int argc, char** argv)
{
    if (pledge("stdio proc exec rpath sigaction", nullptr) < 0) {
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
        fprintf(stderr, "Failed to start debugging session for: \"%s\"\n", command);
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

    g_debug_session->run([&](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            printf("Program exited.\n");
            return Debug::DebugSession::DebugDecision::Detach;
        }

        if (reason == Debug::DebugSession::DebugBreakReason::Syscall) {
            print_syscall(regs.value(), depth + 1);
            return Debug::DebugSession::DebugDecision::ContinueBreakAtSyscall;
        }

        if (new_function) {
            auto function_name = g_debug_session->elf().symbolicate(regs.value().eip);
            print_function_call(function_name, depth);
            new_function = false;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }
        auto instruction = instrumented->get((void*)regs.value().eip).value();

        if (instruction.mnemonic() == "RET") {
            if (depth != 0)
                --depth;
            return Debug::DebugSession::ContinueBreakAtSyscall;
        }

        // FIXME: we could miss some leaf functions that were called with a jump
        ASSERT(instruction.mnemonic() == "CALL");

        ++depth;
        new_function = true;

        return Debug::DebugSession::DebugDecision::SingleStep;
    });
}
