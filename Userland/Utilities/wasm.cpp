/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
#include <LibWasm/Printer/Printer.h>
#include <LibWasm/Types.h>
#include <signal.h>
#include <unistd.h>

RefPtr<Line::Editor> g_line_editor;
static auto g_stdout = Core::OutputFileStream::standard_error();
static Wasm::Printer g_printer { g_stdout };
static bool g_continue { false };
static void (*old_signal)(int);
static Wasm::DebuggerBytecodeInterpreter g_interpreter;

static void sigint_handler(int)
{
    if (!g_continue) {
        signal(SIGINT, old_signal);
        kill(getpid(), SIGINT);
    }
    g_continue = false;
}

static bool post_interpret_hook(Wasm::Configuration&, Wasm::InstructionPointer& ip, Wasm::Instruction const& instr, Wasm::Interpreter const& interpreter)
{
    if (interpreter.did_trap()) {
        g_continue = false;
        warnln("Trapped when executing ip={}", ip);
        g_printer.print(instr);
        warnln("Trap reason: {}", interpreter.trap_reason());
        const_cast<Wasm::Interpreter&>(interpreter).clear_trap();
    }
    return true;
}

static bool pre_interpret_hook(Wasm::Configuration& config, Wasm::InstructionPointer& ip, Wasm::Instruction const& instr)
{
    static bool always_print_stack = false;
    static bool always_print_instruction = false;
    if (always_print_stack)
        config.dump_stack();
    if (always_print_instruction) {
        g_stdout.write(String::formatted("{:0>4} ", ip.value()).bytes());
        g_printer.print(instr);
    }
    if (g_continue)
        return true;
    g_stdout.write(String::formatted("{:0>4} ", ip.value()).bytes());
    g_printer.print(instr);
    String last_command = "";
    for (;;) {
        auto result = g_line_editor->get_line("> ");
        if (result.is_error()) {
            return false;
        }
        auto str = result.release_value();
        g_line_editor->add_to_history(str);
        if (str.is_empty())
            str = last_command;
        else
            last_command = str;
        auto args = str.split_view(' ');
        if (args.is_empty())
            continue;
        auto& cmd = args[0];
        if (cmd.is_one_of("h", "help")) {
            warnln("Wasm shell commands");
            warnln("Toplevel:");
            warnln("- [s]tep                     Run one instruction");
            warnln("- next                       Alias for step");
            warnln("- [c]ontinue                 Execute until a trap or the program exit point");
            warnln("- [p]rint <args...>          Print various things (see section on print)");
            warnln("- call <fn> <args...>        Call the function <fn> with the given arguments");
            warnln("- set <args...>              Set shell option (see section on settings)");
            warnln("- unset <args...>            Unset shell option (see section on settings)");
            warnln("- [h]elp                     Print this help");
            warnln();
            warnln("Print:");
            warnln("- print [s]tack              Print the contents of the stack, including frames and labels");
            warnln("- print [[m]em]ory <index>   Print the contents of the memory identified by <index>");
            warnln("- print [[i]nstr]uction      Print the current instruction");
            warnln("- print [[f]unc]tion <index> Print the function identified by <index>");
            warnln();
            warnln("Settings:");
            warnln("- set print stack            Make the shell print the stack on every instruction executed");
            warnln("- set print [instr]uction    Make the shell print the instruction that will be executed next");
            warnln();
            continue;
        }
        if (cmd.is_one_of("s", "step", "next")) {
            return true;
        }
        if (cmd.is_one_of("p", "print")) {
            if (args.size() < 2) {
                warnln("Print what?");
                continue;
            }
            auto& what = args[1];
            if (what.is_one_of("s", "stack")) {
                config.dump_stack();
                continue;
            }
            if (what.is_one_of("m", "mem", "memory")) {
                if (args.size() < 3) {
                    warnln("print what memory?");
                    continue;
                }
                auto value = args[2].to_uint<u64>();
                if (!value.has_value()) {
                    warnln("invalid memory index {}", args[2]);
                    continue;
                }
                auto mem = config.store().get(Wasm::MemoryAddress(value.value()));
                if (!mem) {
                    warnln("invalid memory index {} (not found)", args[2]);
                    continue;
                }
                warnln("{:>32hex-dump}", mem->data().bytes());
                continue;
            }
            if (what.is_one_of("i", "instr", "instruction")) {
                g_printer.print(instr);
                continue;
            }
            if (what.is_one_of("f", "func", "function")) {
                if (args.size() < 3) {
                    warnln("print what function?");
                    continue;
                }
                auto value = args[2].to_uint<u64>();
                if (!value.has_value()) {
                    warnln("invalid function index {}", args[2]);
                    continue;
                }
                auto fn = config.store().get(Wasm::FunctionAddress(value.value()));
                if (!fn) {
                    warnln("invalid function index {} (not found)", args[2]);
                    continue;
                }
                if (auto* fn_value = fn->get_pointer<Wasm::HostFunction>()) {
                    warnln("Host function at {:p}", &fn_value->function());
                    continue;
                }
                if (auto* fn_value = fn->get_pointer<Wasm::WasmFunction>()) {
                    g_printer.print(fn_value->code());
                    continue;
                }
            }
        }
        if (cmd == "call"sv) {
            if (args.size() < 2) {
                warnln("call what?");
                continue;
            }
            Optional<Wasm::FunctionAddress> address;
            auto index = args[1].to_uint<u64>();
            if (index.has_value()) {
                address = config.frame().module().functions()[index.value()];
            } else {
                auto& name = args[1];
                for (auto& export_ : config.frame().module().exports()) {
                    if (export_.name() == name) {
                        if (auto addr = export_.value().get_pointer<Wasm::FunctionAddress>()) {
                            address = *addr;
                            break;
                        }
                    }
                }
            }

            if (!address.has_value()) {
            failed_to_find:;
                warnln("Could not find a function {}", args[1]);
                continue;
            }

            auto fn = config.store().get(*address);
            if (!fn)
                goto failed_to_find;

            auto type = fn->visit([&](auto& value) { return value.type(); });
            if (type.parameters().size() + 2 != args.size()) {
                warnln("Expected {} arguments for call, but found only {}", type.parameters().size(), args.size() - 2);
                continue;
            }
            Vector<u64> values_to_push;
            Vector<Wasm::Value> values;
            for (size_t index = 2; index < args.size(); ++index)
                values_to_push.append(args[index].to_uint().value_or(0));
            for (auto& param : type.parameters())
                values.append(Wasm::Value { param, values_to_push.take_last() });

            Wasm::Result result { Wasm::Trap {} };
            {
                Wasm::BytecodeInterpreter::CallFrameHandle handle { g_interpreter, config };
                result = config.call(g_interpreter, *address, move(values));
            }
            if (result.is_trap())
                warnln("Execution trapped: {}", result.trap().reason);
            if (!result.values().is_empty())
                warnln("Returned:");
            for (auto& value : result.values()) {
                g_stdout.write("  -> "sv.bytes());
                g_printer.print(value);
            }
            continue;
        }
        if (cmd.is_one_of("set", "unset")) {
            auto value = !cmd.starts_with('u');
            if (args.size() < 3) {
                warnln("(un)set what (to what)?");
                continue;
            }
            if (args[1] == "print"sv) {
                if (args[2] == "stack"sv)
                    always_print_stack = value;
                else if (args[2].is_one_of("instr", "instruction"))
                    always_print_instruction = value;
                else
                    warnln("Unknown print category '{}'", args[2]);
                continue;
            }
            warnln("Unknown set category '{}'", args[1]);
            continue;
        }
        if (cmd.is_one_of("c", "continue")) {
            g_continue = true;
            return true;
        }
        warnln("Command not understood: {}", cmd);
    }
}

static Optional<Wasm::Module> parse(StringView filename)
{
    auto result = Core::File::open(filename, Core::OpenMode::ReadOnly);
    if (result.is_error()) {
        warnln("Failed to open {}: {}", filename, result.error());
        return {};
    }

    auto stream = Core::InputFileStream(result.release_value());
    auto parse_result = Wasm::Module::parse(stream);
    if (parse_result.is_error()) {
        warnln("Something went wrong, either the file is invalid, or there's a bug with LibWasm!");
        warnln("The parse error was {}", Wasm::parse_error_to_string(parse_result.error()));
        return {};
    }
    return parse_result.release_value();
}

static void print_link_error(Wasm::LinkError const& error)
{
    for (auto const& missing : error.missing_imports)
        warnln("Missing import '{}'", missing);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    char const* filename = nullptr;
    bool print = false;
    bool attempt_instantiate = false;
    bool debug = false;
    bool export_all_imports = false;
    bool shell_mode = false;
    String exported_function_to_execute;
    Vector<u64> values_to_push;
    Vector<String> modules_to_link_in;

    Core::ArgsParser parser;
    parser.add_positional_argument(filename, "File name to parse", "file");
    parser.add_option(debug, "Open a debugger", "debug", 'd');
    parser.add_option(print, "Print the parsed module", "print", 'p');
    parser.add_option(attempt_instantiate, "Attempt to instantiate the module", "instantiate", 'i');
    parser.add_option(exported_function_to_execute, "Attempt to execute the named exported function from the module (implies -i)", "execute", 'e', "name");
    parser.add_option(export_all_imports, "Export noop functions corresponding to imports", "export-noop", 0);
    parser.add_option(shell_mode, "Launch a REPL in the module's context (implies -i)", "shell", 's');
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Extra modules to link with, use to resolve imports",
        .long_name = "link",
        .short_name = 'l',
        .value_name = "file",
        .accept_value = [&](char const* str) {
            if (auto v = StringView { str }; !v.is_empty()) {
                modules_to_link_in.append(v);
                return true;
            }
            return false;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Supply arguments to the function (default=0) (expects u64, casts to required type)",
        .long_name = "arg",
        .short_name = 0,
        .value_name = "u64",
        .accept_value = [&](char const* str) -> bool {
            if (auto v = StringView { str }.to_uint<u64>(); v.has_value()) {
                values_to_push.append(v.value());
                return true;
            }
            return false;
        },
    });
    parser.parse(arguments);

    if (shell_mode) {
        debug = true;
        attempt_instantiate = true;
    }

    if (!shell_mode && debug && exported_function_to_execute.is_empty()) {
        warnln("Debug what? (pass -e fn)");
        return 1;
    }

    if (debug || shell_mode) {
        old_signal = signal(SIGINT, sigint_handler);
    }

    if (!exported_function_to_execute.is_empty())
        attempt_instantiate = true;

    auto parse_result = parse(filename);
    if (!parse_result.has_value())
        return 1;

    if (print && !attempt_instantiate) {
        auto out_stream = Core::OutputFileStream::standard_output();
        Wasm::Printer printer(out_stream);
        printer.print(parse_result.value());
    }

    if (attempt_instantiate) {
        Wasm::AbstractMachine machine;
        Core::EventLoop main_loop;
        if (debug) {
            g_line_editor = Line::Editor::construct();
            g_interpreter.pre_interpret_hook = pre_interpret_hook;
            g_interpreter.post_interpret_hook = post_interpret_hook;
        }

        // First, resolve the linked modules
        NonnullOwnPtrVector<Wasm::ModuleInstance> linked_instances;
        Vector<Wasm::Module> linked_modules;
        for (auto& name : modules_to_link_in) {
            auto parse_result = parse(name);
            if (!parse_result.has_value()) {
                warnln("Failed to parse linked module '{}'", name);
                return 1;
            }
            linked_modules.append(parse_result.release_value());
            Wasm::Linker linker { linked_modules.last() };
            for (auto& instance : linked_instances)
                linker.link(instance);
            auto link_result = linker.finish();
            if (link_result.is_error()) {
                warnln("Linking imported module '{}' failed", name);
                print_link_error(link_result.error());
                return 1;
            }
            auto instantiation_result = machine.instantiate(linked_modules.last(), link_result.release_value());
            if (instantiation_result.is_error()) {
                warnln("Instantiation of imported module '{}' failed: {}", name, instantiation_result.error().error);
                return 1;
            }
            linked_instances.append(instantiation_result.release_value());
        }

        Wasm::Linker linker { parse_result.value() };
        for (auto& instance : linked_instances)
            linker.link(instance);

        if (export_all_imports) {
            HashMap<Wasm::Linker::Name, Wasm::ExternValue> exports;
            for (auto& entry : linker.unresolved_imports()) {
                if (!entry.type.has<Wasm::TypeIndex>())
                    continue;
                auto type = parse_result.value().type(entry.type.get<Wasm::TypeIndex>());
                auto address = machine.store().allocate(Wasm::HostFunction(
                    [name = entry.name, type = type](auto&, auto& arguments) -> Wasm::Result {
                        StringBuilder argument_builder;
                        bool first = true;
                        for (auto& argument : arguments) {
                            DuplexMemoryStream stream;
                            Wasm::Printer { stream }.print(argument);
                            if (first)
                                first = false;
                            else
                                argument_builder.append(", "sv);
                            ByteBuffer buffer = stream.copy_into_contiguous_buffer();
                            argument_builder.append(StringView(buffer).trim_whitespace());
                        }
                        dbgln("[wasm runtime] Stub function {} was called with the following arguments: {}", name, argument_builder.to_string());
                        Vector<Wasm::Value> result;
                        result.ensure_capacity(type.results().size());
                        for (auto& result_type : type.results())
                            result.append(Wasm::Value { result_type, 0ull });
                        return Wasm::Result { move(result) };
                    },
                    type));
                exports.set(entry, *address);
            }

            linker.link(exports);
        }

        auto link_result = linker.finish();
        if (link_result.is_error()) {
            warnln("Linking main module failed");
            print_link_error(link_result.error());
            return 1;
        }
        auto result = machine.instantiate(parse_result.value(), link_result.release_value());
        if (result.is_error()) {
            warnln("Module instantiation failed: {}", result.error().error);
            return 1;
        }
        auto module_instance = result.release_value();

        auto launch_repl = [&] {
            Wasm::Configuration config { machine.store() };
            Wasm::Expression expression { {} };
            config.set_frame(Wasm::Frame {
                *module_instance,
                Vector<Wasm::Value> {},
                expression,
                0,
            });
            Wasm::Instruction instr { Wasm::Instructions::nop };
            Wasm::InstructionPointer ip { 0 };
            g_continue = false;
            pre_interpret_hook(config, ip, instr);
        };

        auto stream = Core::OutputFileStream::standard_output();
        auto print_func = [&](auto const& address) {
            Wasm::FunctionInstance* fn = machine.store().get(address);
            stream.write(String::formatted("- Function with address {}, ptr = {}\n", address.value(), fn).bytes());
            if (fn) {
                stream.write(String::formatted("    wasm function? {}\n", fn->has<Wasm::WasmFunction>()).bytes());
                fn->visit(
                    [&](Wasm::WasmFunction const& func) {
                        Wasm::Printer printer { stream, 3 };
                        stream.write("    type:\n"sv.bytes());
                        printer.print(func.type());
                        stream.write("    code:\n"sv.bytes());
                        printer.print(func.code());
                    },
                    [](Wasm::HostFunction const&) {});
            }
        };
        if (print) {
            // Now, let's dump the functions!
            for (auto& address : module_instance->functions()) {
                print_func(address);
            }
        }

        if (shell_mode) {
            launch_repl();
            return 0;
        }

        if (!exported_function_to_execute.is_empty()) {
            Optional<Wasm::FunctionAddress> run_address;
            Vector<Wasm::Value> values;
            for (auto& entry : module_instance->exports()) {
                if (entry.name() == exported_function_to_execute) {
                    if (auto addr = entry.value().get_pointer<Wasm::FunctionAddress>())
                        run_address = *addr;
                }
            }
            if (!run_address.has_value()) {
                warnln("No such exported function, sorry :(");
                return 1;
            }

            auto instance = machine.store().get(*run_address);
            VERIFY(instance);

            if (instance->has<Wasm::HostFunction>()) {
                warnln("Exported function is a host function, cannot run that yet");
                return 1;
            }

            for (auto& param : instance->get<Wasm::WasmFunction>().type().parameters()) {
                if (values_to_push.is_empty())
                    values.append(Wasm::Value { param, 0ull });
                else
                    values.append(Wasm::Value { param, values_to_push.take_last() });
            }

            if (print) {
                outln("Executing ");
                print_func(*run_address);
                outln();
            }

            auto result = machine.invoke(g_interpreter, run_address.value(), move(values));

            if (debug)
                launch_repl();

            if (result.is_trap()) {
                warnln("Execution trapped: {}", result.trap().reason);
            } else {
                if (!result.values().is_empty())
                    warnln("Returned:");
                for (auto& value : result.values()) {
                    Wasm::Printer printer { stream };
                    g_stdout.write("  -> "sv.bytes());
                    g_printer.print(value);
                }
            }
        }
    }

    return 0;
}
