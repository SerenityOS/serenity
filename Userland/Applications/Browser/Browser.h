/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <Applications/Browser/IconBag.h>
#include <Applications/Browser/WebDriverConnection.h>

namespace Browser {

extern String g_home_url;
extern String g_new_tab_url;
extern String g_search_engine;
extern Vector<String> g_content_filters;
extern Vector<String> g_proxies;
extern HashMap<String, size_t> g_proxy_mappings;
extern bool g_content_filters_enabled;
extern IconBag g_icon_bag;
extern RefPtr<Browser::WebDriverConnection> g_web_driver_connection;

}
