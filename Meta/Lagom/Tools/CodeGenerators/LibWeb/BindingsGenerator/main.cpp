/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibIDL/IDLParser.h>
#include <LibIDL/Types.h>

extern Vector<StringView> s_header_search_paths;

namespace IDL {
void generate_constructor_header(IDL::Interface const&);
void generate_constructor_implementation(IDL::Interface const&);
void generate_prototype_header(IDL::Interface const&);
void generate_prototype_implementation(IDL::Interface const&);
void generate_iterator_prototype_header(IDL::Interface const&);
void generate_iterator_prototype_implementation(IDL::Interface const&);
}

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    StringView path;
    StringView import_base_path;
    bool constructor_header_mode = false;
    bool constructor_implementation_mode = false;
    bool prototype_header_mode = false;
    bool prototype_implementation_mode = false;
    bool iterator_prototype_header_mode = false;
    bool iterator_prototype_implementation_mode = false;
    args_parser.add_option(constructor_header_mode, "Generate the constructor .h file", "constructor-header", 'C');
    args_parser.add_option(constructor_implementation_mode, "Generate the constructor .cpp file", "constructor-implementation", 'O');
    args_parser.add_option(prototype_header_mode, "Generate the prototype .h file", "prototype-header", 'P');
    args_parser.add_option(prototype_implementation_mode, "Generate the prototype .cpp file", "prototype-implementation", 'R');
    args_parser.add_option(iterator_prototype_header_mode, "Generate the iterator prototype .h file", "iterator-prototype-header", 0);
    args_parser.add_option(iterator_prototype_implementation_mode, "Generate the iterator prototype .cpp file", "iterator-prototype-implementation", 0);
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Add a header search path passed to the compiler",
        .long_name = "header-include-path",
        .short_name = 'i',
        .value_name = "path",
        .accept_value = [&](char const* s) {
            s_header_search_paths.append({ s, strlen(s) });
            return true;
        },
    });
    args_parser.add_positional_argument(path, "IDL file", "idl-file");
    args_parser.add_positional_argument(import_base_path, "Import base path", "import-base-path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", path, file_or_error.error());
        return 1;
    }

    LexicalPath lexical_path(path);
    auto& namespace_ = lexical_path.parts_view().at(lexical_path.parts_view().size() - 2);

    auto data = file_or_error.value()->read_all();

    if (import_base_path.is_null())
        import_base_path = lexical_path.dirname();

    IDL::Parser parser(path, data, import_base_path);
    auto& interface = parser.parse();

    static constexpr Array libweb_interface_namespaces = {
        "CSS"sv,
        "Crypto"sv,
        "DOM"sv,
        "DOMParsing"sv,
        "Encoding"sv,
        "Fetch"sv,
        "FileAPI"sv,
        "Geometry"sv,
        "HTML"sv,
        "HighResolutionTime"sv,
        "IntersectionObserver"sv,
        "NavigationTiming"sv,
        "RequestIdleCallback"sv,
        "ResizeObserver"sv,
        "SVG"sv,
        "Selection"sv,
        "UIEvents"sv,
        "URL"sv,
        "WebGL"sv,
        "WebIDL"sv,
        "WebSockets"sv,
        "XHR"sv,
    };

    if (libweb_interface_namespaces.span().contains_slow(namespace_)) {
        StringBuilder builder;
        builder.append(namespace_);
        builder.append("::"sv);
        builder.append(interface.name);
        interface.fully_qualified_name = builder.to_string();
    } else {
        interface.fully_qualified_name = interface.name;
    }

    if constexpr (BINDINGS_GENERATOR_DEBUG) {
        dbgln("Attributes:");
        for (auto& attribute : interface.attributes) {
            dbgln("  {}{}{}{} {}",
                attribute.inherit ? "inherit " : "",
                attribute.readonly ? "readonly " : "",
                attribute.type->name(),
                attribute.type->is_nullable() ? "?" : "",
                attribute.name);
        }

        dbgln("Functions:");
        for (auto& function : interface.functions) {
            dbgln("  {}{} {}",
                function.return_type->name(),
                function.return_type->is_nullable() ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type->name(),
                    parameter.type->is_nullable() ? "?" : "",
                    parameter.name);
            }
        }

        dbgln("Static Functions:");
        for (auto& function : interface.static_functions) {
            dbgln("  static {}{} {}",
                function.return_type->name(),
                function.return_type->is_nullable() ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type->name(),
                    parameter.type->is_nullable() ? "?" : "",
                    parameter.name);
            }
        }
    }

    if (constructor_header_mode)
        IDL::generate_constructor_header(interface);

    if (constructor_implementation_mode)
        IDL::generate_constructor_implementation(interface);

    if (prototype_header_mode)
        IDL::generate_prototype_header(interface);

    if (prototype_implementation_mode)
        IDL::generate_prototype_implementation(interface);

    if (iterator_prototype_header_mode)
        IDL::generate_iterator_prototype_header(interface);

    if (iterator_prototype_implementation_mode)
        IDL::generate_iterator_prototype_implementation(interface);

    return 0;
}
