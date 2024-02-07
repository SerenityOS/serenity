/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Environment.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>

static ErrorOr<bool> is_dynamically_linked_executable(StringView filename)
{
    auto executable = TRY(Core::System::resolve_executable_from_environment(filename));
    auto file = TRY(Core::MappedFile::map(executable));
    ELF::Image elf_image(file->bytes());
    return elf_image.is_dynamic();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    ByteString promises;
    Vector<StringView> command;
    bool add_promises_for_dynamic_linker;

    Core::ArgsParser args_parser;
    args_parser.add_option(promises, "Space-separated list of pledge promises", "promises", 'p', "promises");
    args_parser.add_option(add_promises_for_dynamic_linker, "Add temporary promises for dynamic linker", "dynamic-linker-promises", 'd');
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    if (add_promises_for_dynamic_linker && TRY(is_dynamically_linked_executable(command[0]))) {
        auto constexpr loader_promises = "stdio rpath prot_exec"sv;
        MUST(Core::Environment::set("_LOADER_PLEDGE_PROMISES"sv, loader_promises, Core::Environment::Overwrite::Yes));
        MUST(Core::Environment::set("_LOADER_MAIN_PROGRAM_PLEDGE_PROMISES"sv, promises, Core::Environment::Overwrite::Yes));
        promises = ByteString::formatted("{} {}", promises, loader_promises);
    }

    TRY(Core::System::pledge(StringView(), promises));
    TRY(Core::System::exec(command[0], command.span(), Core::System::SearchInPath::Yes));
    return 0;
}
