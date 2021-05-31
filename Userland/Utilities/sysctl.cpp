/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>

static String read_var(const String& name)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    auto f = Core::File::construct(path);
    if (!f->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", f->name(), f->error_string());
        exit(1);
    }
    const auto& b = f->read_all();
    if (f->error() < 0) {
        warnln("Failed to read: {}", f->error_string());
        exit(1);
    }
    return String((const char*)b.data(), b.size(), Chomp);
}

static void write_var(const String& name, const String& value)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    auto f = Core::File::construct(path);
    if (!f->open(Core::OpenMode::WriteOnly)) {
        warnln("Failed to open: {}", f->error_string());
        exit(1);
    }
    f->write(value);
    if (f->error() < 0) {
        warnln("Failed to write: {}", f->error_string());
        exit(1);
    }
}

static int handle_show_all()
{
    Core::DirIterator di("/proc/sys", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        outln("DirIterator: {}", di.error_string());
        return 1;
    }

    while (di.has_next()) {
        String variable_name = di.next_path();
        outln("{} = {}", variable_name, read_var(variable_name));
    }
    return 0;
}

static int handle_var(const String& var)
{
    String spec(var.characters(), Chomp);
    auto parts = spec.split('=');
    String variable_name = parts[0];
    bool is_write = parts.size() > 1;

    if (!is_write) {
        outln("{} = {}", variable_name, read_var(variable_name));
        return 0;
    }

    out("{} = {}", variable_name, read_var(variable_name));
    write_var(variable_name, parts[1]);
    outln(" -> {}", read_var(variable_name));
    return 0;
}

int main(int argc, char** argv)
{
    bool show_all = false;
    const char* var = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Show or modify system-internal values. This requires root, and can crash your system.");
    args_parser.add_option(show_all, "Show all variables", nullptr, 'a');
    args_parser.add_positional_argument(var, "variable[=value]", "variable", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (var == nullptr) {
        // Not supplied; assume `-a`.
        show_all = true;
    }

    if (show_all) {
        // Ignore `var`, even if it was supplied. Just like the real procps does.
        return handle_show_all();
    }

    return handle_var(var);
}
