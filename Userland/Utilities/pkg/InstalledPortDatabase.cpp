/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InstalledPortDatabase.h"
#include <AK/Function.h>
#include <AK/StringUtils.h>
#include <LibCore/System.h>

ErrorOr<void> InstalledPortDatabase::for_each_by_type(InstalledPort::Type type, Function<ErrorOr<void>(InstalledPort const&)> callback)
{
    for (auto& port : m_installed_ports) {
        if (type == port.value.type())
            TRY(callback(port.value));
    }
    return {};
}

ErrorOr<void> InstalledPortDatabase::insert_new_port_to_ports_database(InstalledPort::Type type, String name, InstalledPort port, Vector<Port> const& dependencies)
{
    TRY(m_database_file->write_until_depleted(ByteString::formatted("{} {} {}\n", type == InstalledPort::Type::Auto ? "auto"sv : "manual"sv, name, port.version_string())));
    if (!dependencies.is_empty()) {
        TRY(m_database_file->write_until_depleted(ByteString::formatted("dependency {}", name)));
        for (auto& dependency : dependencies) {
            TRY(m_database_file->write_until_depleted(ByteString::formatted(" {}", dependency.name())));
        }
        TRY(m_database_file->write_until_depleted(ByteString::formatted("\n")));
    }

    TRY(m_installed_ports.try_set(name, port));
    return {};
}

ErrorOr<NonnullOwnPtr<InstalledPortDatabase>> InstalledPortDatabase::instantiate_ports_database(StringView path)
{
    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto appending_database_file_descriptor = TRY(Core::File::open(path, Core::File::OpenMode::Write | Core::File::OpenMode::Append));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
    int line_number = 0;

    HashMap<String, InstalledPort> ports;
    while (TRY(buffered_file->can_read_line())) {
        auto line = TRY(buffered_file->read_line(buffer));
        line_number += 1;
        if (line.is_empty())
            continue;

        auto parts = line.split_view(' ');
        if (parts.size() < 2) {
            dbgln("Invalid database entry '{}' (only {} parts) on line {}", line, parts.size(), line_number);
            continue;
        }
        auto install_type_string = parts[0];
        auto port_name = TRY(String::from_utf8(parts[1]));

        if (auto maybe_type = InstalledPort::type_from_string(install_type_string); maybe_type.has_value()) {
            auto const type = maybe_type.release_value();
            if (parts.size() < 3)
                return Error::from_string_view("Port is missing a version specification"sv);

            ports.ensure(port_name, [=] { return InstalledPort { port_name, MUST(String::from_utf8(parts[2])), type }; });
        } else if (install_type_string == "dependency"sv) {
            Vector<String> dependencies;
            TRY(dependencies.try_ensure_capacity(parts.size() - 2));
            for (auto const& dependency : parts.span().slice(2)) {
                dependencies.unchecked_append(TRY(String::from_utf8(dependency)));
            }
            // Assume the port as automatically installed if the "dependency" line occurs before the "manual"/"auto" line.
            // This is fine since these entries override the port type in any case.
            auto& port = ports.ensure(port_name, [&] { return InstalledPort { port_name, {}, InstalledPort::Type::Auto }; });
            port.m_dependencies = move(dependencies);
        } else {
            return Error::from_string_literal("Unknown installed port type");
        }
    }
    return adopt_nonnull_own_or_enomem(new (nothrow) InstalledPortDatabase(move(ports), move(appending_database_file_descriptor), TRY(String::from_utf8(path))));
}
