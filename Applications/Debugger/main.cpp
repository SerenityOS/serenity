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
#include <AK/LogStream.h>
#include <AK/StringBuilder.h>
#include <AK/kmalloc.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDebug/DebugInfo.h>
#include <LibDebug/DebugSession.h>
#include <LibLine/Editor.h>
#include <LibX86/Disassembler.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

RefPtr<Line::Editor> editor;

OwnPtr<Debug::DebugSession> g_debug_session;

static void handle_sigint(int)
{
    outln("Debugger: SIGINT");

    // The destructor of DebugSession takes care of detaching
    g_debug_session = nullptr;
}

static void handle_print_registers(const PtraceRegisters& regs)
{
    outln("eax={:08x} ebx={:08x} ecx={:08x} edx={:08x}", regs.eax, regs.ebx, regs.ecx, regs.edx);
    outln("esp={:08x} ebp={:08x} esi={:08x} edi={:08x}", regs.esp, regs.ebp, regs.esi, regs.edi);
    outln("eip={:08x} eflags={:08x}", regs.eip, regs.eflags);
}

static bool handle_disassemble_command(const String& command, void* first_instruction)
{
    auto parts = command.split(' ');
    size_t number_of_instructions_to_disassemble = 5;
    if (parts.size() == 2) {
        auto number = parts[1].to_uint();
        if (!number.has_value())
            return false;
        number_of_instructions_to_disassemble = number.value();
    }

    // FIXME: Instead of using a fixed "dump_size",
    //        we can feed instructions to the disassembler one by one
    constexpr size_t dump_size = 0x100;
    ByteBuffer code;
    for (size_t i = 0; i < dump_size / sizeof(u32); ++i) {
        auto value = g_debug_session->peek(reinterpret_cast<u32*>(first_instruction) + i);
        if (!value.has_value())
            break;
        code.append(&value, sizeof(u32));
    }

    X86::SimpleInstructionStream stream(code.data(), code.size());
    X86::Disassembler disassembler(stream);

    for (size_t i = 0; i < number_of_instructions_to_disassemble; ++i) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        outln("    {:p} <+{}>:\t{}", offset + reinterpret_cast<size_t>(first_instruction), offset, insn.value().to_string(offset));
    }

    return true;
}

static bool handle_breakpoint_command(const String& command)
{
    auto parts = command.split(' ');
    if (parts.size() != 2)
        return false;

    auto argument = parts[1];
    if (argument.is_empty())
        return false;

    u32 breakpoint_address = 0;

    if (argument.contains(":")) {
        auto source_arguments = argument.split(':');
        if (source_arguments.size() != 2)
            return false;
        auto line = source_arguments[1].to_uint();
        if (!line.has_value())
            return false;
        auto file = source_arguments[0];
        if (!file.contains("/"))
            file = String::formatted("./{}", file);
        auto result = g_debug_session->debug_info().get_instruction_from_source(file, line.value());
        if (!result.has_value()) {
            outln("No matching instruction found");
            return false;
        }
        breakpoint_address = result.value();
    } else if ((argument[0] >= '0' && argument[0] <= '9')) {
        breakpoint_address = strtoul(argument.characters(), nullptr, 16);
    } else {
        auto symbol = g_debug_session->elf().find_demangled_function(argument);
        if (!symbol.has_value()) {
            outln("symbol {} not found", parts[1]);
            return false;
        }
        breakpoint_address = reinterpret_cast<u32>(symbol.value().value());
    }

    bool success = g_debug_session->insert_breakpoint(reinterpret_cast<void*>(breakpoint_address));
    if (!success) {
        warnln("could not insert breakpoint at: {:p}", breakpoint_address);
        return false;
    }
    warnln("breakpoint inserted at: {:p}", breakpoint_address);
    return true;
}

static void print_help()
{
    new_out("Options:\n"
            "cont - Continue execution\n"
            "si - step to the next instruction\n"
            "sl - step to the next source line\n"
            "line - show the position of the current instruction in the source code\n"
            "regs - Print registers\n"
            "dis [number of instructions] - Print disassembly\n"
            "bp <address/symbol/file:line> - Insert a breakpoint\n");
}

int main(int argc, char** argv)
{
    editor = Line::Editor::construct();

    if (pledge("stdio proc exec rpath tty sigaction cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* command = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command,
        "The program to be debugged, along with its arguments",
        "program", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    auto result = Debug::DebugSession::exec_and_attach(command);
    if (!result) {
        warnln("Failed to start debugging session for: \"{}\"", command);
        exit(1);
    }
    g_debug_session = result.release_nonnull();

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    bool rc = g_debug_session->insert_breakpoint(g_debug_session->elf().entry().as_ptr());
    ASSERT(rc);

    Debug::DebugInfo::SourcePosition previous_source_position;
    bool in_step_line = false;

    g_debug_session->run([&](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> optional_regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            outln("Program exited.");
            return Debug::DebugSession::DebugDecision::Detach;
        }

        ASSERT(optional_regs.has_value());
        const PtraceRegisters& regs = optional_regs.value();

        auto symbol_at_ip = g_debug_session->elf().symbolicate(regs.eip);
        auto source_position = g_debug_session->debug_info().get_source_position(regs.eip);

        if (in_step_line) {
            bool no_source_info = !source_position.has_value();
            if (no_source_info || source_position.value() != previous_source_position) {
                if (no_source_info)
                    outln("No source information for current instruction! stoppoing.");
                in_step_line = false;
            } else {
                return Debug::DebugSession::DebugDecision::SingleStep;
            }
        }

        outln("Program is stopped at: {:p} ({})", regs.eip, symbol_at_ip);

        if (source_position.has_value()) {
            previous_source_position = source_position.value();
            outln("Source location: {}:{}", source_position.value().file_path, source_position.value().line_number);
        } else {
            outln("(No source location information for the current instruction)");
        }

        for (;;) {
            auto command_result = editor->get_line("(sdb) ");

            if (command_result.is_error())
                return Debug::DebugSession::DebugDecision::Detach;

            auto& command = command_result.value();

            bool success = false;
            Optional<Debug::DebugSession::DebugDecision> decision;

            if (command.is_empty() && !editor->history().is_empty()) {
                command = editor->history().last();
            }
            if (command == "cont") {
                decision = Debug::DebugSession::DebugDecision::Continue;
                success = true;
            } else if (command == "si") {
                decision = Debug::DebugSession::DebugDecision::SingleStep;
                success = true;
            } else if (command == "sl") {
                if (source_position.has_value()) {
                    decision = Debug::DebugSession::DebugDecision::SingleStep;
                    in_step_line = true;
                    success = true;
                } else {
                    outln("No source location information for the current instruction");
                }
            } else if (command == "regs") {
                handle_print_registers(regs);
                success = true;

            } else if (command.starts_with("dis")) {
                success = handle_disassemble_command(command, reinterpret_cast<void*>(regs.eip));

            } else if (command.starts_with("bp")) {
                success = handle_breakpoint_command(command);
            }

            if (success && !command.is_empty()) {
                // Don't add repeated commands to history
                if (editor->history().is_empty() || editor->history().last() != command)
                    editor->add_to_history(command);
            }
            if (!success) {
                print_help();
            }
            if (decision.has_value())
                return decision.value();
        }
    });
}
