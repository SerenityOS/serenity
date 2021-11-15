/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/CommonLocationsProvider.h>
#include <unistd.h>

namespace GUI {

static bool s_initialized = false;
static Vector<CommonLocationsProvider::CommonLocation> s_common_locations;

static void initialize_if_needed()
{
    if (s_initialized)
        return;

    auto user_config = String::formatted("{}/CommonLocations.json", Core::StandardPaths::config_directory());
    if (Core::File::exists(user_config)) {
        CommonLocationsProvider::load_from_json(user_config);
        return;
    }

    // Fallback : If the user doesn't have custom locations, use some default ones.
    s_common_locations.append({ "Root", "/" });
    s_common_locations.append({ "Home", Core::StandardPaths::home_directory() });
    s_common_locations.append({ "Downloads", Core::StandardPaths::downloads_directory() });
    s_initialized = true;
}

void CommonLocationsProvider::load_from_json(const String& json_path)
{
    auto file = Core::File::construct(json_path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        dbgln("Unable to open {}", file->filename());
        return;
    }

    auto json = JsonValue::from_string(file->read_all());
    if (json.is_error()) {
        dbgln("Common locations file {} is not a valid JSON file.", file->filename());
        return;
    }
    if (!json.value().is_array()) {
        dbgln("Common locations file {} should contain a JSON array.", file->filename());
        return;
    }

    s_common_locations.clear();
    auto const& contents = json.value().as_array();
    for (size_t i = 0; i < contents.size(); ++i) {
        auto entry_value = contents.at(i);
        if (!entry_value.is_object())
            continue;
        auto entry = entry_value.as_object();
        auto name = entry.get("name").to_string();
        auto path = entry.get("path").to_string();
        s_common_locations.append({ name, path });
    }

    s_initialized = true;
}

const Vector<CommonLocationsProvider::CommonLocation>& CommonLocationsProvider::common_locations()
{
    initialize_if_needed();
    return s_common_locations;
}

}
