/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <Applications/Browser/IconBag.h>

namespace Browser {

extern DeprecatedString g_home_url;
extern DeprecatedString g_new_tab_url;
extern DeprecatedString g_search_engine;
extern Vector<DeprecatedString> g_content_filters;
extern Vector<DeprecatedString> g_proxies;
extern HashMap<DeprecatedString, size_t> g_proxy_mappings;
extern bool g_content_filters_enabled;
extern IconBag g_icon_bag;
extern DeprecatedString g_webdriver_content_ipc_path;

}
