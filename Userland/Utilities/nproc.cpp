/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    auto file = TRY(Core::File::open("/sys/kernel/cpuinfo"sv, Core::File::OpenMode::Read));

    auto buffer = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(buffer));
    auto const& cpuinfo_array = json.as_array();
    outln("{}", cpuinfo_array.size());

    return 0;
}
