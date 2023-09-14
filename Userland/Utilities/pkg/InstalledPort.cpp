/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InstalledPort.h"
#include <AK/Function.h>
#include <LibCore/File.h>
#include <LibCore/System.h>

ErrorOr<HashMap<String, InstalledPort>> InstalledPort::read_ports_database()
{
    auto file = TRY(Core::File::open("/usr/Ports/installed.db"sv, Core::File::OpenMode::Read));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));

    HashMap<String, InstalledPort> ports;
    while (TRY(buffered_file->can_read_line())) {
        auto line = TRY(buffered_file->read_line(buffer));
        if (line.is_empty()) {
            continue;
        } else if (line.starts_with("dependency"sv)) {
            auto parts = line.split_view(' ');
            VERIFY(parts.size() == 3);
            auto type = InstalledPort::Type::Dependency;
            // FIXME: Add versioning when printing these ports!
            auto name = TRY(String::from_utf8(parts[2]));
            TRY(ports.try_set(name, InstalledPort { TRY(String::from_utf8(parts[2])), type, {} }));
        } else if (line.starts_with("auto"sv)) {
            auto parts = line.split_view(' ');
            VERIFY(parts.size() == 3);
            auto type = InstalledPort::Type::Auto;
            auto name = TRY(String::from_utf8(parts[1]));
            TRY(ports.try_set(name, InstalledPort { name, type, TRY(String::from_utf8(parts[2])) }));
        } else if (line.starts_with("manual"sv)) {
            auto parts = line.split_view(' ');
            VERIFY(parts.size() == 3);
            auto type = InstalledPort::Type::Manual;
            auto name = TRY(String::from_utf8(parts[1]));
            TRY(ports.try_set(name, InstalledPort { name, type, TRY(String::from_utf8(parts[2])) }));
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
