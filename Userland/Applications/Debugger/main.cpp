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
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDebug/DebugInfo.h>
#include <LibDebug/DebugSession.h>
#include <LibDisassembly/Architecture.h>
#include <LibDisassembly/Disassembler.h>
#include <LibDisassembly/x86/Instruction.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/arch/regs.h>
#include <unistd.h>

RefPtr<Line::Editor> editor;

OwnPtr<Debug::DebugSession> g_debug_session;

static void handle_sigint(int)
{
    outln("Debugger: SIGINT");

    // The destructor of DebugSession takes care of detaching
    g_debug_session = nullptr;
}

static void handle_print_registers(PtraceRegisters const& regs)
{
#if ARCH(X86_64)
    outln("rax={:p} rbx={:p} rcx={:p} rdx={:p}", regs.rax, regs.rbx, regs.rcx, regs.rdx);
    outln("rsp={:p} rbp={:p} rsi={:p} rdi={:p}", regs.rsp, regs.rbp, regs.rsi, regs.rdi);
    outln("r8 ={:p} r9 ={:p} r10={:p} r11={:p}", regs.r8, regs.r9, regs.r10, regs.r11);
    outln("r12={:p} r13={:p} r14={:p} r15={:p}", regs.r12, regs.r13, regs.r14, regs.r15);
    outln("rip={:p} rflags={:p}", regs.rip, regs.rflags);
#elif ARCH(AARCH64)
    (void)regs;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    outln("Program counter pc={:p}", regs.pc);
    outln("ra={:p} sp={:p} gp={:p} tp={:p} fp={:p}", regs.x[0], regs.x[1], regs.x[2], regs.x[3], regs.x[7]);
    outln("a0={:p} a1={:p} a2={:p} a3={:p} a4={:p} a5={:p} a6={:p} a7={:p}", regs.x[9], regs.x[10], regs.x[11], regs.x[12], regs.x[13], regs.x[14], regs.x[15], regs.x[16]);
    outln("t0={:p} t1={:p} t2={:p} t3={:p} t4={:p} t5={:p} t6={:p}", regs.x[4], regs.x[5], regs.x[6], regs.x[27], regs.x[28], regs.x[29], regs.x[30]);
    outln("s1={:p} s2={:p} s3={:p} s4={:p} s5={:p} s6={:p} s7={:p} s8={:p} s9={:p} s10={:p} s11={:p}", regs.x[8], regs.x[17], regs.x[18], regs.x[19], regs.x[20], regs.x[21], regs.x[22], regs.x[23], regs.x[24], regs.x[25], regs.x[26]);
#else
#    error Unknown architecture
#endif
}

static bool handle_disassemble_command(ByteString const& command, FlatPtr first_instruction)
{
    auto parts = command.split(' ');
    size_t number_of_instructions_to_disassemble = 5;
    if (parts.size() == 2) {
        auto number = parts[1].to_number<unsigned>();
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

    Disassembly::SimpleInstructionStream stream(code.data(), code.size());
    Disassembly::Disassembler disassembler(stream, Disassembly::host_architecture());

    for (size_t i = 0; i < number_of_instructions_to_disassemble; ++i) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        outln("    {:p} <+{}>:\t{}", offset + first_instruction, offset, insn.value()->to_byte_string(offset));
    }

    return true;
}

static bool handle_backtrace_command(PtraceRegisters const& regs)
{
    (void)regs;
    TODO();
    return true;
}

static bool insert_breakpoint_at_address(FlatPtr address)
{
    return g_debug_session->insert_breakpoint(address);
}

static bool insert_breakpoint_at_source_position(ByteString const& file, size_t line)
{
    auto result = g_debug_session->insert_breakpoint(file, line);
    if (!result.has_value()) {
        warnln("Could not insert breakpoint at {}:{}", file, line);
        return false;
    }
    outln("Breakpoint inserted [{}:{} ({}:{:p})]", result.value().filename, result.value().line_number, result.value().library_name, result.value().address);
    return true;
}

static bool insert_breakpoint_at_symbol(ByteString const& symbol)
{
    auto result = g_debug_session->insert_breakpoint(symbol);
    if (!result.has_value()) {
        warnln("Could not insert breakpoint at symbol: {}", symbol);
        return false;
    }
    outln("Breakpoint inserted [{}:{:p}]", result.value().library_name, result.value().address);
    return true;
}

static bool handle_breakpoint_command(ByteString const& command)
{
    auto parts = command.split(' ');
    if (parts.size() != 2)
        return false;

    auto argument = parts[1];
    if (argument.is_empty())
        return false;

    if (argument.contains(":"sv)) {
        auto source_arguments = argument.split(':');
        if (source_arguments.size() != 2)
            return false;
        auto line = source_arguments[1].to_number<unsigned>();
        if (!line.has_value())
            return false;
        auto file = source_arguments[0];
        return insert_breakpoint_at_source_position(file, line.value());
    }
    if ((argument.starts_with("0x"sv))) {
        return insert_breakpoint_at_address(strtoul(argument.characters() + 2, nullptr, 16));
    }

    return insert_breakpoint_at_symbol(argument);
}

static bool handle_examine_command(ByteString const& command)
{
    auto parts = command.split(' ');
    if (parts.size() != 2)
        return false;

    auto argument = parts[1];
    if (argument.is_empty())
        return false;

    if (!(argument.starts_with("0x"sv))) {
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

static NonnullOwnPtr<Debug::DebugSession> create_debug_session(StringView command, pid_t pid_to_debug)
{
    if (!command.is_null()) {
        auto result = Debug::DebugSession::exec_and_attach(command);
        if (!result) {
            warnln("Failed to start debugging session for: \"{}\"", command);
            exit(1);
        }
        return result.release_nonnull();
    }

    if (pid_to_debug == -1) {
        warnln("Either a command or a pid must be specified");
        exit(1);
    }

    auto result = Debug::DebugSession::attach(pid_to_debug);
    if (!result) {
        warnln("Failed to attach to pid: {}", pid_to_debug);
        exit(1);
    }
    return result.release_nonnull();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    editor = Line::Editor::construct();

    TRY(Core::System::pledge("stdio proc ptrace exec rpath tty sigaction cpath unix"));

    StringView command;
    pid_t pid_to_debug = -1;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command,
        "The program to be debugged, along with its arguments",
        "program", Core::ArgsParser::Required::No);
    args_parser.add_option(pid_to_debug, "Attach debugger to running process", "pid", 'p', "PID");
    args_parser.parse(arguments);

    g_debug_session = create_debug_session(command, pid_to_debug);

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
        PtraceRegisters const& regs = optional_regs.value();
#if ARCH(X86_64)
        FlatPtr const ip = regs.rip;
#elif ARCH(AARCH64)
        FlatPtr const ip = 0; // FIXME
        TODO_AARCH64();
#elif ARCH(RISCV64)
        FlatPtr const ip = regs.pc;
#else
#    error Unknown architecture
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

            } else if (command.starts_with("dis"sv)) {
                success = handle_disassemble_command(command, ip);

            } else if (command.starts_with("bp"sv)) {
                success = handle_breakpoint_command(command);
            } else if (command.starts_with("x"sv)) {
                success = handle_examine_command(command);
            } else if (command.starts_with("bt"sv)) {
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
