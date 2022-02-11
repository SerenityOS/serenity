/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDebug/DebugInfo.h>
#include <LibDebug/DebugSession.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <LibX86/Disassembler.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
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
#if ARCH(I386)
    outln("eax={:p} ebx={:p} ecx={:p} edx={:p}", regs.eax, regs.ebx, regs.ecx, regs.edx);
    outln("esp={:p} ebp={:p} esi={:p} edi={:p}", regs.esp, regs.ebp, regs.esi, regs.edi);
    outln("eip={:p} eflags={:p}", regs.eip, regs.eflags);
#else
    outln("rax={:p} rbx={:p} rcx={:p} rdx={:p}", regs.rax, regs.rbx, regs.rcx, regs.rdx);
    outln("rsp={:p} rbp={:p} rsi={:p} rdi={:p}", regs.rsp, regs.rbp, regs.rsi, regs.rdi);
    outln("r8 ={:p} r9 ={:p} r10={:p} r11={:p}", regs.r8, regs.r9, regs.r10, regs.r11);
    outln("r12={:p} r13={:p} r14={:p} r15={:p}", regs.r12, regs.r13, regs.r14, regs.r15);
    outln("rip={:p} rflags={:p}", regs.rip, regs.rflags);
#endif
}

static bool handle_disassemble_command(const String& command, FlatPtr first_instruction)
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
        auto value = g_debug_session->peek(first_instruction + i * sizeof(u32));
        if (!value.has_value())
            break;
        if (code.try_append(&value, sizeof(u32)).is_error())
            break;
    }

    X86::SimpleInstructionStream stream(code.data(), code.size());
    X86::Disassembler disassembler(stream);

    for (size_t i = 0; i < number_of_instructions_to_disassemble; ++i) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        outln("    {:p} <+{}>:\t{}", offset + first_instruction, offset, insn.value().to_string(offset));
    }

    return true;
}

static bool handle_backtrace_command(const PtraceRegisters& regs)
{
#if ARCH(I386)
    auto ebp_val = regs.ebp;
    auto eip_val = regs.eip;
    outln("Backtrace:");
    while (g_debug_session->peek(eip_val).has_value() && g_debug_session->peek(ebp_val).has_value()) {
        auto eip_symbol = g_debug_session->symbolicate(eip_val);
        auto source_position = g_debug_session->get_source_position(eip_val);
        String symbol_location = (eip_symbol.has_value() && eip_symbol->symbol != "") ? eip_symbol->symbol : "???";
        if (source_position.has_value()) {
            outln("{:p} in {} ({}:{})", eip_val, symbol_location, source_position->file_path, source_position->line_number);
        } else {
            outln("{:p} in {}", eip_val, symbol_location);
        }
        auto next_eip = g_debug_session->peek(ebp_val + 4);
        auto next_ebp = g_debug_session->peek(ebp_val);
        eip_val = (u32)next_eip.value();
        ebp_val = (u32)next_ebp.value();
    }
#else
    (void)regs;
    TODO();
#endif
    return true;
}

static bool insert_breakpoint_at_address(FlatPtr address)
{
    return g_debug_session->insert_breakpoint(address);
}

static bool insert_breakpoint_at_source_position(const String& file, size_t line)
{
    auto result = g_debug_session->insert_breakpoint(file, line);
    if (!result.has_value()) {
        warnln("Could not insert breakpoint at {}:{}", file, line);
        return false;
    }
    outln("Breakpoint inserted [{}:{} ({}:{:p})]", result.value().filename, result.value().line_number, result.value().library_name, result.value().address);
    return true;
}

static bool insert_breakpoint_at_symbol(const String& symbol)
{
    auto result = g_debug_session->insert_breakpoint(symbol);
    if (!result.has_value()) {
        warnln("Could not insert breakpoint at symbol: {}", symbol);
        return false;
    }
    outln("Breakpoint inserted [{}:{:p}]", result.value().library_name, result.value().address);
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

    if (argument.contains(":")) {
        auto source_arguments = argument.split(':');
        if (source_arguments.size() != 2)
            return false;
        auto line = source_arguments[1].to_uint();
        if (!line.has_value())
            return false;
        auto file = source_arguments[0];
        return insert_breakpoint_at_source_position(file, line.value());
    }
    if ((argument.starts_with("0x"))) {
        return insert_breakpoint_at_address(strtoul(argument.characters() + 2, nullptr, 16));
    }

    return insert_breakpoint_at_symbol(argument);
}

static bool handle_examine_command(const String& command)
{
    auto parts = command.split(' ');
    if (parts.size() != 2)
        return false;

    auto argument = parts[1];
    if (argument.is_empty())
        return false;

    if (!(argument.starts_with("0x"))) {
        return false;
    }
    FlatPtr address = strtoul(argument.characters() + 2, nullptr, 16);
    auto res = g_debug_session->peek(address);
    if (!res.has_value()) {
        outln("Could not examine memory at address {:p}", address);
        return true;
    }
    outln("{:#x}", res.value());
    return true;
}

static void print_help()
{
    out("Options:\n"
        "cont - Continue execution\n"
        "si - step to the next instruction\n"
        "sl - step to the next source line\n"
        "line - show the position of the current instruction in the source code\n"
        "regs - Print registers\n"
        "dis [number of instructions] - Print disassembly\n"
        "bp <address/symbol/file:line> - Insert a breakpoint\n"
        "bt - show backtrace for current thread\n"
        "x <address> - examine dword in memory\n");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    editor = Line::Editor::construct();

    TRY(Core::System::pledge("stdio proc ptrace exec rpath tty sigaction cpath unix", nullptr));

    const char* command = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command,
        "The program to be debugged, along with its arguments",
        "program", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto result = Debug::DebugSession::exec_and_attach(command);
    if (!result) {
        warnln("Failed to start debugging session for: \"{}\"", command);
        exit(1);
    }
    g_debug_session = result.release_nonnull();

    struct sigaction sa {
    };
    sa.sa_handler = handle_sigint;
    TRY(Core::System::sigaction(SIGINT, &sa, nullptr));

    Debug::DebugInfo::SourcePosition previous_source_position;
    bool in_step_line = false;

    g_debug_session->run(Debug::DebugSession::DesiredInitialDebugeeState::Stopped, [&](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> optional_regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            outln("Program exited.");
            return Debug::DebugSession::DebugDecision::Detach;
        }

        VERIFY(optional_regs.has_value());
        const PtraceRegisters& regs = optional_regs.value();
#if ARCH(I386)
        const FlatPtr ip = regs.eip;
#else
        const FlatPtr ip = regs.rip;
#endif

        auto symbol_at_ip = g_debug_session->symbolicate(ip);

        auto source_position = g_debug_session->get_source_position(ip);

        if (in_step_line) {
            bool no_source_info = !source_position.has_value();
            if (no_source_info || source_position.value() != previous_source_position) {
                if (no_source_info)
                    outln("No source information for current instruction! stopping.");
                in_step_line = false;
            } else {
                return Debug::DebugSession::DebugDecision::SingleStep;
            }
        }

        if (symbol_at_ip.has_value())
            outln("Program is stopped at: {:p} ({}:{})", ip, symbol_at_ip.value().library_name, symbol_at_ip.value().symbol);
        else
            outln("Program is stopped at: {:p}", ip);

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
                command = editor->history().last().entry;
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
                success = handle_disassemble_command(command, ip);

            } else if (command.starts_with("bp")) {
                success = handle_breakpoint_command(command);
            } else if (command.starts_with("x")) {
                success = handle_examine_command(command);
            } else if (command.starts_with("bt")) {
                success = handle_backtrace_command(regs);
            }

            if (success && !command.is_empty()) {
                // Don't add repeated commands to history
                if (editor->history().is_empty() || editor->history().last().entry != command)
                    editor->add_to_history(command);
            }
            if (!success) {
                print_help();
            }
            if (decision.has_value())
                return decision.value();
        }
    });

    return 0;
}
