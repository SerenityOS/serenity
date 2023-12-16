/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

static bool s_set_variable = false;

static Optional<ByteString> get_variable(StringView name)
{
    auto path = ByteString::formatted("/sys/kernel/conf/{}", name);
    auto file = Core::File::open(path, Core::File::OpenMode::Read);
    if (file.is_error()) {
        warnln("Failed to open {}: {}", path, file.error());
        return {};
    }
    auto buffer = file.value()->read_until_eof();
    if (buffer.is_error()) {
        warnln("Failed to read {}: {}", path, buffer.error());
        return {};
    }
    return ByteString { (char const*)buffer.value().data(), buffer.value().size(), Chomp };
}

static bool read_variable(StringView name)
{
    auto value = get_variable(name);
    if (!value.has_value())
        return false;
    outln("{} = {}", name, *value);
    return true;
}

static bool write_variable(StringView name, StringView value)
{
    auto old_value = get_variable(name);
    if (!old_value.has_value())
        return false;
    auto path = ByteString::formatted("/sys/kernel/conf/{}", name);
    auto file = Core::File::open(path, Core::File::OpenMode::Write);
    if (file.is_error()) {
        warnln("Failed to open {}: {}", path, file.error());
        return false;
    }
    if (auto result = file.value()->write_until_depleted(value.bytes()); result.is_error()) {
        warnln("Failed to write {}: {}", path, result.error());
        return false;
    }
    outln("{}: {} -> {}", name, *old_value, value);
    return true;
}

static int handle_variables(Vector<StringView> const& variables)
{
    bool success = false;
    for (auto const& variable : variables) {
        auto maybe_index = variable.find('=');
        if (!maybe_index.has_value()) {
            success = read_variable(variable);
            continue;
        }
        auto equal_index = maybe_index.release_value();
        auto name = variable.substring_view(0, equal_index);
        auto value = variable.substring_view(equal_index + 1, variable.length() - equal_index - 1);
        if (name.is_empty())
            warnln("Malformed setting '{}'", variable);
        else if (!s_set_variable)
            warnln("Must specify '-w' to set variables");
        else
            success = write_variable(name, value);
    }
    return success ? 0 : 1;
}

static int handle_show_all()
{
    Core::DirIterator di("/sys/kernel/conf", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        outln("DirIterator: {}", di.error());
        return 1;
    }

    bool success = false;
    while (di.has_next()) {
        auto name = di.next_path();
        success = read_variable(name);
    }
    return success ? 0 : 1;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool show_all = false;
    Vector<StringView> variables;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show or modify system-internal values. This requires root, and can crash your system.");
    args_parser.add_option(show_all, "Show all variables", "all", 'a');
    args_parser.add_option(s_set_variable, "Set variables", "write", 'w');
    args_parser.add_positional_argument(variables, "variable[=value]", "variables", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!show_all && variables.is_empty()) {
        args_parser.print_usage(stdout, arguments.strings[0]);
        return 1;
    }

    if (show_all) {
        // Ignore `variables`, even if they are supplied. Just like the real procps does.
        return handle_show_all();
    }

    return handle_variables(variables);
}
