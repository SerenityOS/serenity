/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibACPI/Interpreter.h>
#include <LibACPI/TableStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> input_files;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("ACPI management utility.");
    args_parser.add_positional_argument(input_files, "Files containing AML table bytecode to load.", "input");
    args_parser.parse(arguments);

    if (input_files.is_empty())
        return Error::from_string_literal("Input file is required, use '-' to read from standard input");

    TRY(Core::System::pledge("stdio rpath"sv));

    for (auto input : input_files)
        TRY(Core::System::unveil(TRY(FileSystem::absolute_path(input)), "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto interpreter = TRY(ACPI::AML::Interpreter::create());
    for (auto input : input_files) {
        auto maybe_input_file = Core::MappedFile::map(input);
        if (maybe_input_file.is_error()) {
            warnln("Failed to open {}: {}", input, maybe_input_file.release_error());
            return 1;
        }

        auto input_file = maybe_input_file.release_value();
        TRY(interpreter->load(input_file->bytes()));
    }

    TRY(interpreter->print_namespace());
    return 0;
}
