/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/Printer/Printer.h>
#include <LibWasm/Types.h>

int main(int argc, char* argv[])
{
    const char* filename = nullptr;
    bool print = false;
    bool attempt_instantiate = false;
    bool attemp_execute = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(filename, "File name to parse", "file");
    parser.add_option(print, "Print the parsed module", "print", 'p');
    parser.add_option(attempt_instantiate, "Attempt to instantiate the module", "instantiate", 'i');
    parser.add_option(attemp_execute, "Attempt to execute a function from the module (implies -i)", "execute", 'e');
    parser.parse(argc, argv);

    if (attemp_execute)
        attempt_instantiate = true;

    auto result = Core::File::open(filename, Core::IODevice::OpenMode::ReadOnly);
    if (result.is_error()) {
        warnln("Failed to open {}: {}", filename, result.error());
        return 1;
    }

    auto stream = Core::InputFileStream(result.release_value());
    auto parse_result = Wasm::Module::parse(stream);
    if (parse_result.is_error()) {
        warnln("Something went wrong, either the file is invalid, or there's a bug with LibWasm!");
        warnln("The parse error was {}", Wasm::parse_error_to_string(parse_result.error()));
        return 2;
    }

    if (print && !attempt_instantiate) {
        auto out_stream = Core::OutputFileStream::standard_output();
        Wasm::Printer printer(out_stream);
        printer.print(parse_result.value());
    }

    if (attempt_instantiate) {
        Wasm::AbstractMachine machine;
        auto result = machine.instantiate(parse_result.value(), {});
        if (result.is_error()) {
            warnln("Module instantiation failed: {}", result.error().error);
            return 1;
        }

        auto stream = Core::OutputFileStream::standard_output();
        auto print_func = [&](const auto& address) {
            Wasm::FunctionInstance* fn = machine.store().get(address);
            stream.write(String::formatted("- Function with address {}, ptr = {}\n", address.value(), fn).bytes());
            if (fn) {
                stream.write(String::formatted("    wasm function? {}\n", fn->has<Wasm::WasmFunction>()).bytes());
                fn->visit(
                    [&](const Wasm::WasmFunction& func) {
                        Wasm::Printer printer { stream, 3 };
                        stream.write("    type:\n"sv.bytes());
                        printer.print(func.type());
                        stream.write("    code:\n"sv.bytes());
                        printer.print(func.code());
                    },
                    [](const Wasm::HostFunction&) {});
            }
        };
        if (print) {
            // Now, let's dump the functions!
            for (auto& address : machine.module_instance().functions()) {
                print_func(address);
            }
        }

        if (attemp_execute) {
            Optional<Wasm::FunctionAddress> run_address;
            Vector<Wasm::Value> values;
            // Pick a function that takes no args :P
            for (auto& address : machine.module_instance().functions()) {
                auto fn = machine.store().get(address);
                if (!fn)
                    continue;
                if (auto ptr = fn->get_pointer<Wasm::WasmFunction>()) {
                    const Wasm::FunctionType& ty = ptr->type();
                    for (auto& param : ty.parameters()) {
                        values.append(Wasm::Value { param, 0ull });
                    }
                    run_address = address;
                    break;
                }
            }
            if (!run_address.has_value()) {
                warnln("No nullary function, sorry :(");
                return 1;
            }
            outln("Executing ");
            print_func(*run_address);
            outln();

            auto result = machine.invoke(run_address.value(), move(values));
            if (!result.values().is_empty())
                warnln("Returned:");
            for (auto& value : result.values()) {
                value.value().visit(
                    [&](const auto& value) {
                        if constexpr (requires { value.value(); })
                            out("  -> addr{} ", value.value());
                        else
                            out("  -> {} ", value);
                    });
                Wasm::Printer printer { stream };
                printer.print(value.type());
            }
        }
    }

    return 0;
}
