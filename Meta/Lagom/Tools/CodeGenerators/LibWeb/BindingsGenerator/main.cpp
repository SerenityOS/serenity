/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Namespaces.h"
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibIDL/IDLParser.h>
#include <LibIDL/Types.h>

extern Vector<StringView> s_header_search_paths;

namespace IDL {
void generate_namespace_header(IDL::Interface const&, StringBuilder&);
void generate_namespace_implementation(IDL::Interface const&, StringBuilder&);
void generate_constructor_header(IDL::Interface const&, StringBuilder&);
void generate_constructor_implementation(IDL::Interface const&, StringBuilder&);
void generate_prototype_header(IDL::Interface const&, StringBuilder&);
void generate_prototype_implementation(IDL::Interface const&, StringBuilder&);
void generate_iterator_prototype_header(IDL::Interface const&, StringBuilder&);
void generate_iterator_prototype_implementation(IDL::Interface const&, StringBuilder&);
void generate_global_mixin_header(IDL::Interface const&, StringBuilder&);
void generate_global_mixin_implementation(IDL::Interface const&, StringBuilder&);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    StringView path;
    StringView import_base_path;
    StringView output_path = "-"sv;
    StringView depfile_path;
    StringView depfile_target;
    bool namespace_header_mode = false;
    bool namespace_implementation_mode = false;
    bool constructor_header_mode = false;
    bool constructor_implementation_mode = false;
    bool prototype_header_mode = false;
    bool prototype_implementation_mode = false;
    bool iterator_prototype_header_mode = false;
    bool iterator_prototype_implementation_mode = false;
    bool global_mixin_header_mode = false;
    bool global_mixin_implementation_mode = false;
    args_parser.add_option(namespace_header_mode, "Generate the namespace .h file", "namespace-header", 'N');
    args_parser.add_option(namespace_implementation_mode, "Generate the namespace .cpp file", "namespace-implementation", 'A');
    args_parser.add_option(constructor_header_mode, "Generate the constructor .h file", "constructor-header", 'C');
    args_parser.add_option(constructor_implementation_mode, "Generate the constructor .cpp file", "constructor-implementation", 'O');
    args_parser.add_option(prototype_header_mode, "Generate the prototype .h file", "prototype-header", 'P');
    args_parser.add_option(prototype_implementation_mode, "Generate the prototype .cpp file", "prototype-implementation", 'R');
    args_parser.add_option(iterator_prototype_header_mode, "Generate the iterator prototype .h file", "iterator-prototype-header", 0);
    args_parser.add_option(iterator_prototype_implementation_mode, "Generate the iterator prototype .cpp file", "iterator-prototype-implementation", 0);
    args_parser.add_option(global_mixin_header_mode, "Generate the global object mixin .h file", "global-mixin-header", 0);
    args_parser.add_option(global_mixin_implementation_mode, "Generate the global object mixin .cpp file", "global-mixin-implementation", 0);
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Add a header search path passed to the compiler",
        .long_name = "header-include-path",
        .short_name = 'i',
        .value_name = "path",
        .accept_value = [&](StringView s) {
            s_header_search_paths.append(s);
            return true;
        },
    });
    args_parser.add_option(output_path, "Path to output generated file into", "output-path", 'o', "output-path");
    args_parser.add_option(depfile_path, "Path to write dependency file to", "depfile", 'd', "depfile-path");
    args_parser.add_option(depfile_target, "Name of target in the depfile (default: output path)", "depfile-target", 't', "target");
    args_parser.add_positional_argument(path, "IDL file", "idl-file");
    args_parser.add_positional_argument(import_base_path, "Import base path", "import-base-path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));

    LexicalPath lexical_path(path);
    auto& namespace_ = lexical_path.parts_view().at(lexical_path.parts_view().size() - 2);

    auto data = TRY(file->read_until_eof());

    if (import_base_path.is_null())
        import_base_path = lexical_path.dirname();

    auto output_file = TRY(Core::File::open_file_or_standard_stream(output_path, Core::File::OpenMode::Write));

    IDL::Parser parser(path, data, import_base_path);
    auto& interface = parser.parse();

    if (IDL::libweb_interface_namespaces.span().contains_slow(namespace_)) {
        StringBuilder builder;
        builder.append(namespace_);
        builder.append("::"sv);
        builder.append(interface.name);
        interface.fully_qualified_name = builder.to_deprecated_string();
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

    StringBuilder output_builder;

    if (namespace_header_mode)
        IDL::generate_namespace_header(interface, output_builder);

    if (namespace_implementation_mode)
        IDL::generate_namespace_implementation(interface, output_builder);

    if (constructor_header_mode)
        IDL::generate_constructor_header(interface, output_builder);

    if (constructor_implementation_mode)
        IDL::generate_constructor_implementation(interface, output_builder);

    if (prototype_header_mode)
        IDL::generate_prototype_header(interface, output_builder);

    if (prototype_implementation_mode)
        IDL::generate_prototype_implementation(interface, output_builder);

    if (iterator_prototype_header_mode)
        IDL::generate_iterator_prototype_header(interface, output_builder);

    if (iterator_prototype_implementation_mode)
        IDL::generate_iterator_prototype_implementation(interface, output_builder);

    if (global_mixin_header_mode)
        IDL::generate_global_mixin_header(interface, output_builder);

    if (global_mixin_implementation_mode)
        IDL::generate_global_mixin_implementation(interface, output_builder);

    TRY(output_file->write_until_depleted(output_builder.string_view().bytes()));

    if (!depfile_path.is_null()) {
        auto depfile = TRY(Core::File::open_file_or_standard_stream(depfile_path, Core::File::OpenMode::Write));

        StringBuilder depfile_builder;
        depfile_builder.append(depfile_target.is_null() ? output_path : depfile_target);
        depfile_builder.append(':');
        for (auto const& path : parser.imported_files()) {
            depfile_builder.append(" \\\n "sv);
            depfile_builder.append(path);
        }
        depfile_builder.append('\n');
        TRY(depfile->write_until_depleted(depfile_builder.string_view().bytes()));
    }
    return 0;
}
