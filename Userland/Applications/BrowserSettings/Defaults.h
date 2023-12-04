/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/Resource.h>

namespace Browser {

static constexpr StringView default_color_scheme = "auto"sv;
static constexpr bool default_enable_content_filters = true;
static constexpr bool default_show_bookmarks_bar = true;
static constexpr bool default_close_download_widget_on_finish = false;
static constexpr bool default_allow_autoplay_on_all_websites = false;

inline String const& default_homepage_url()
{
    // FIXME: Teach LibWeb how to load resource:// URLs, rather than converting to a file:// URL here.
    static auto default_homepage_url = []() {
        static constexpr auto url = "resource://html/misc/welcome.html"sv;
        return MUST(Core::Resource::load_from_uri(url))->file_url();
    }();

    return default_homepage_url;
}

inline String const& default_new_tab_url()
{
    // FIXME: Teach LibWeb how to load resource:// URLs, rather than converting to a file:// URL here.
    static auto default_new_tab_url = []() {
        static constexpr auto url = "resource://ladybird/new-tab.html"sv;
        return MUST(Core::Resource::load_from_uri(url))->file_url();
    }();

    return default_new_tab_url;
}

}
