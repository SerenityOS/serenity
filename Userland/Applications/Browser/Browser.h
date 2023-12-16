/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/String.h>
#include <Applications/Browser/IconBag.h>

namespace Browser {

extern ByteString g_home_url;
extern ByteString g_new_tab_url;
extern ByteString g_search_engine;
extern Vector<String> g_content_filters;
extern bool g_content_filters_enabled;
extern Vector<String> g_autoplay_allowlist;
extern bool g_autoplay_allowed_on_all_websites;
extern Vector<ByteString> g_proxies;
extern HashMap<ByteString, size_t> g_proxy_mappings;
extern IconBag g_icon_bag;
extern ByteString g_webdriver_content_ipc_path;

}
