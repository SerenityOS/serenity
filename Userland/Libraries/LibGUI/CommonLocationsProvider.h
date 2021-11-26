/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <sys/types.h>

namespace GUI {

class CommonLocationsProvider {
public:
    struct CommonLocation {
        String name;
        String path;
    };

    static void load_from_json(const String& json_path);
    static const Vector<CommonLocation>& common_locations();
};

}
