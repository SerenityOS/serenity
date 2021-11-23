/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibDeviceTree/Validation.h>
#include <serenity.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String filename;

    Core::ArgsParser args;
    args.add_positional_argument(filename, "File to process", "file", Core::ArgsParser::Required::Yes);
    args.parse(argc, argv);

    // FIXME: Figure out how to do this sanely from stdin
    auto maybe_file = Core::MappedFile::map(filename);
    if (maybe_file.is_error()) {
        warnln("Unable to dump device tree from file {}: {}", filename, maybe_file.error());
        return 1;
    }
    auto file = maybe_file.release_value();

    if (file->size() < sizeof(DeviceTree::FlattenedDeviceTreeHeader)) {
        warnln("Not enough data in {} to contain a device tree header!", filename);
        return 1;
    }

    auto* fdt_header = reinterpret_cast<DeviceTree::FlattenedDeviceTreeHeader const*>(file->data());

    bool valid = DeviceTree::dump(*fdt_header, static_cast<u8 const*>(file->data()), file->size());

    return valid ? 0 : 1;
}
