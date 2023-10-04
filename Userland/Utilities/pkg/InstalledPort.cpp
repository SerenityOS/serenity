/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InstalledPort.h"
#include <AK/Function.h>
#include <AK/StringUtils.h>
#include <LibCore/File.h>
#include <LibCore/System.h>

Optional<InstalledPort::Type> InstalledPort::type_from_string(StringView type)
{
    if (type == "auto"sv)
        return Type::Auto;
    if (type == "manual"sv)
        return Type::Manual;
    return {};
}

ErrorOr<HashMap<String, InstalledPort>> InstalledPort::read_ports_database()
{
    auto file = TRY(Core::File::open(ports_database, Core::File::OpenMode::Read));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));

    HashMap<String, InstalledPort> ports;
    while (TRY(buffered_file->can_read_line())) {
        auto line = TRY(buffered_file->read_line(buffer));
        if (line.is_empty())
            continue;

        auto parts = line.split_view(' ');
        if (parts.size() < 2) {
            dbgln("Invalid database entry {} (only {} parts)", line, parts.size());
            // FIXME: Skip over invalid entries instead?
            return Error::from_string_view("Database entry too short"sv);
        }
        auto string_type = parts[0];
        auto name = TRY(String::from_utf8(parts[1]));

        if (auto maybe_type = type_from_string(string_type); maybe_type.has_value()) {
            auto const type = maybe_type.release_value();
            if (parts.size() < 3)
                return Error::from_string_view("Port is missing a version specification"sv);
            auto version = TRY(String::from_utf8(parts[2]));

            auto& port = ports.ensure(name, [&] { return InstalledPort { name, {}, {} }; });
            port.m_type = type;
            port.m_version = move(version);
        } else if (string_type == "dependency"sv) {
            // Accept an empty dependency list.
            auto dependency_views = parts.span().slice(2);
            Vector<String> dependencies;
            TRY(dependencies.try_ensure_capacity(dependency_views.size()));
            for (auto const& view : dependency_views)
                dependencies.unchecked_append(TRY(String::from_utf8(view)));

            // Assume the port as automatically installed if the "dependency" line occurs before the "manual"/"auto" line.
            // This is fine since these entries override the port type in any case.
            auto& port = ports.ensure(name, [&] { return InstalledPort { name, Type::Auto, {} }; });
            port.m_dependencies = move(dependencies);
        } else {
            return Error::from_string_literal("Unknown installed port type");
        }
    }
    return ports;
}

ErrorOr<void> InstalledPort::for_each_by_type(HashMap<String, InstalledPort>& ports_database, InstalledPort::Type type, Function<ErrorOr<void>(InstalledPort const&)> callback)
{
    for (auto& port : ports_database) {
        if (type == port.value.type())
            TRY(callback(port.value));
    }
    return {};
}
