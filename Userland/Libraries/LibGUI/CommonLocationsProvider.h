/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <LibGUI/Forward.h>
#include <sys/types.h>

namespace GUI {

class CommonLocationsProvider {
public:
    struct CommonLocation {
        DeprecatedString name;
        DeprecatedString path;
    };

    static void load_from_json(DeprecatedString const& json_path);
    static Vector<CommonLocation> const& common_locations();
};

}
