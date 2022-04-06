/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

#include "AST.h"
#include "Parser.h"

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    String input_file_name;
    String mode;
    args_parser.add_positional_argument(input_file_name, "input file name", "input");
    args_parser.add_option(mode, "Mode (source/header)", "mode", 'm', "mode");
    args_parser.parse(arguments);

    // FIXME: Port to Core::Stream when it gets read_all().
    auto input = TRY(Core::File::open(input_file_name, Core::OpenMode::ReadOnly));
    auto input_data_buffer = input->read_all();
    StringView input_data = input_data_buffer;

    Parser parser(input_data, LexicalPath(input_file_name).title());
    auto maybe_config_file = parser.parse();
    if (maybe_config_file.is_error()) {
        warnln("\e[1;31merror:\e[m {}", maybe_config_file.release_error().message);
        return 1;
    }
    auto config_file = maybe_config_file.release_value();

    if (mode == "header") {
        StringBuilder header_builder;
        SourceGenerator header_generator(header_builder);
        config_file.generate_header(header_generator);
        outln("{}", header_builder.string_view());
    } else if (mode == "source") {
        StringBuilder source_builder;
        SourceGenerator source_generator(source_builder);
        config_file.generate_source(source_generator);
        outln("{}", source_builder.string_view());
    } else {
        warnln("Invalid mode: {}", mode);
        return 1;
    }
    return 0;
}
