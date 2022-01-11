/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <Applications/Browser/IconBag.h>

namespace Browser {

extern String g_home_url;
extern String g_search_engine;
extern Vector<String> g_content_filters;
extern IconBag g_icon_bag;

}
