/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>
#include <LibDeviceTree/Validation.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    DeprecatedString filename;

    Core::ArgsParser args;
    args.add_positional_argument(filename, "File to process", "file", Core::ArgsParser::Required::Yes);
    args.parse(arguments);

    // FIXME: Figure out how to do this sanely from stdin
    auto file = TRY(Core::MappedFile::map(filename));

    if (TRY(file->size()) < sizeof(DeviceTree::FlattenedDeviceTreeHeader)) {
        warnln("Not enough data in {} to contain a device tree header!", filename);
        return 1;
    }

    auto const* fdt_header = reinterpret_cast<DeviceTree::FlattenedDeviceTreeHeader const*>(file->data());
    auto bytes = file->bytes();

    TRY(DeviceTree::dump(*fdt_header, bytes));

    auto compatible = TRY(DeviceTree::slow_get_property<StringView>("/compatible"sv, *fdt_header, bytes));
    auto compatible_strings = compatible.split_view('\0');
    dbgln("compatible with: {}", compatible_strings);

    auto bootargs = TRY(DeviceTree::slow_get_property<StringView>("/chosen/bootargs"sv, *fdt_header, bytes));
    dbgln("bootargs: {}", bootargs);

    auto cpu_compatible = TRY(DeviceTree::slow_get_property<StringView>("/cpus/cpu@0/compatible"sv, *fdt_header, bytes));
    dbgln("cpu0 compatible: {}", cpu_compatible);

    return 0;
}
