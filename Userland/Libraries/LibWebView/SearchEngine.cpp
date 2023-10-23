/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <LibWebView/SearchEngine.h>

namespace WebView {

static constexpr auto builtin_search_engines = Array {
    SearchEngine { "Bing"sv, "https://www.bing.com/search?q={}"sv },
    SearchEngine { "Brave"sv, "https://search.brave.com/search?q={}"sv },
    SearchEngine { "DuckDuckGo"sv, "https://duckduckgo.com/?q={}"sv },
    SearchEngine { "GitHub"sv, "https://github.com/search?q={}"sv },
    SearchEngine { "Google"sv, "https://www.google.com/search?q={}"sv },
    SearchEngine { "Mojeek"sv, "https://www.mojeek.com/search?q={}"sv },
    SearchEngine { "Yahoo"sv, "https://search.yahoo.com/search?p={}"sv },
    SearchEngine { "Yandex"sv, "https://yandex.com/search/?text={}"sv },
};

ReadonlySpan<SearchEngine> search_engines()
{
    return builtin_search_engines;
}

SearchEngine const& default_search_engine()
{
    static auto default_engine = find_search_engine_by_name("Google"sv);
    VERIFY(default_engine.has_value());

    return *default_engine;
}

Optional<SearchEngine const&> find_search_engine_by_name(StringView name)
{
    auto it = AK::find_if(builtin_search_engines.begin(), builtin_search_engines.end(),
        [&](auto const& engine) {
            return engine.name == name;
        });

    if (it == builtin_search_engines.end())
        return {};

    return *it;
}

}
