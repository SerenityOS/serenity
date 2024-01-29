/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/CommonLocationsProvider.h>
#include <unistd.h>

namespace GUI {

static bool s_initialized = false;
static Vector<CommonLocationsProvider::CommonLocation> s_common_locations;

static void initialize_if_needed()
{
    if (s_initialized)
        return;

    auto user_config = ByteString::formatted("{}/CommonLocations.json", Core::StandardPaths::config_directory());
    if (FileSystem::exists(user_config)) {
        auto maybe_error = CommonLocationsProvider::load_from_json(user_config);
        if (!maybe_error.is_error())
            return;
        dbgln("Unable to read Common Locations file: {}", maybe_error.error());
        dbgln("Using the default set instead.");
    }

    // Fallback : If the user doesn't have custom locations, use some default ones.
    s_common_locations.append({ "Root", "/" });
    s_common_locations.append({ "Home", Core::StandardPaths::home_directory() });
    s_common_locations.append({ "Desktop", Core::StandardPaths::desktop_directory() });
    s_common_locations.append({ "Documents", Core::StandardPaths::documents_directory() });
    s_common_locations.append({ "Downloads", Core::StandardPaths::downloads_directory() });
    s_common_locations.append({ "Music", Core::StandardPaths::music_directory() });
    s_common_locations.append({ "Pictures", Core::StandardPaths::pictures_directory() });
    s_common_locations.append({ "Videos", Core::StandardPaths::videos_directory() });
    s_initialized = true;
}

ErrorOr<void> CommonLocationsProvider::load_from_json(StringView json_path)
{
    auto file = TRY(Core::File::open(json_path, Core::File::OpenMode::Read));
    auto json = JsonValue::from_string(TRY(file->read_until_eof()));
    if (json.is_error())
        return Error::from_string_literal("File is not a valid JSON");
    if (!json.value().is_array())
        return Error::from_string_literal("File must contain a JSON array");

    s_common_locations.clear();
    auto const& contents = json.value().as_array();
    for (size_t i = 0; i < contents.size(); ++i) {
        auto entry_value = contents.at(i);
        if (!entry_value.is_object())
            continue;
        auto entry = entry_value.as_object();
        auto name = entry.get_byte_string("name"sv).value_or({});
        auto path = entry.get_byte_string("path"sv).value_or({});
        TRY(s_common_locations.try_append({ name, path }));
    }

    s_initialized = true;
    return {};
}

Vector<CommonLocationsProvider::CommonLocation> const& CommonLocationsProvider::common_locations()
{
    initialize_if_needed();
    return s_common_locations;
}

}
