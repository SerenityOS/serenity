/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/File.h>

// Exit code is bitwise-or of these values:
static constexpr auto EXIT_COLLISION = 0x1;
static constexpr auto EXIT_ERROR = 0x2;

int main(int argc, char** argv)
{
    if (argc < 3) {
        warnln("Usage: {} path/to/some.ipc path/to/other.ipc [more ipc files ...]", argv[0]);
        return EXIT_ERROR;
    }

    // Read files, compute their hashes, ignore collisions for now.
    HashMap<u32, Vector<String>> inverse_hashes;
    bool had_errors = false;
    for (int file_index = 1; file_index < argc; ++file_index) {
        String filename(argv[file_index]);
        auto file_or_error = Core::File::open(filename, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Error: Cannot open '{}': {}", filename, file_or_error.error());
            had_errors = true;
            continue; // next file
        }
        auto file = file_or_error.value();
        String endpoint_name;
        while (true) {
            String line = file->read_line();
            if (file->error() != 0 || line.is_null())
                break;
            if (!line.starts_with("endpoint "sv))
                continue;
            auto line_endpoint_name = line.substring("endpoint "sv.length());
            if (!endpoint_name.is_null()) {
                // Note: If there are three or more endpoints defined in a file, these errors will look a bit wonky.
                // However, that's fine, because it shouldn't happen in the first place.
                warnln("Error: Multiple endpoints in file '{}': Found {} and {}", filename, file->error());
                had_errors = true;
                continue; // next line
            }
            endpoint_name = line_endpoint_name;
        }
        if (file->error() != 0) {
            warnln("Error: Failed to read '{}': {}", filename, file->error());
            had_errors = true;
            continue; // next file
        }
        if (endpoint_name.is_null()) {
            // If this happens, this file probably needs to parse the endpoint name more carefully.
            warnln("Error: Could not detect endpoint name in file '{}'", filename);
            had_errors = true;
            continue; // next file
        }
        u32 hash = endpoint_name.hash();
        auto& files_with_hash = inverse_hashes.ensure(hash);
        files_with_hash.append(filename);
    }

    // Report any collisions
    bool had_collisions = false;
    for (auto const& specific_collisions : inverse_hashes) {
        if (specific_collisions.value.size() <= 1)
            continue;
        outln("Collision: Multiple endpoints use the magic number {}:", specific_collisions.key);
        for (auto const& colliding_file : specific_collisions.value) {
            outln("- {}", colliding_file);
        }
        had_collisions = true;
    }

    outln("Checked {} files, saw {} distinct magic numbers.", argc - 1, inverse_hashes.size());
    if (had_collisions)
        outln("Consider giving your new service a different name.");

    if (had_errors)
        warnln("Some errors were encountered. There may be endpoints with colliding magic numbers.");

    int exit_code = 0;
    if (had_collisions)
        exit_code |= EXIT_COLLISION;
    if (had_errors)
        exit_code |= EXIT_ERROR;
    return exit_code;
}
