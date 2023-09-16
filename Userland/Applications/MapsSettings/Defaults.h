/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Maps {

static constexpr StringView default_tile_provider_url_format = "https://tile.openstreetmap.org/{}/{}/{}.png"sv;
static constexpr StringView default_tile_provider_attribution_text = "© OpenStreetMap contributors"sv;
static constexpr StringView default_tile_provider_attribution_url = "https://www.openstreetmap.org/copyright"sv;

}
