/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibDeviceTree/Validation.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    String filename;

    Core::ArgsParser args;
    args.add_positional_argument(filename, "File to process", "file", Core::ArgsParser::Required::Yes);
    args.parse(arguments);

    // FIXME: Figure out how to do this sanely from stdin
    auto file = TRY(Core::MappedFile::map(filename));

    if (file->size() < sizeof(DeviceTree::FlattenedDeviceTreeHeader)) {
        warnln("Not enough data in {} to contain a device tree header!", filename);
        return 1;
    }

    auto* fdt_header = reinterpret_cast<DeviceTree::FlattenedDeviceTreeHeader const*>(file->data());

    bool valid = DeviceTree::dump(*fdt_header, static_cast<u8 const*>(file->data()), file->size());

    return valid ? 0 : 1;
}
