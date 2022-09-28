/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/Loader/ContentFilter.h>

namespace Web {

ContentFilter& ContentFilter::the()
{
    static ContentFilter filter;
    return filter;
}

ContentFilter::ContentFilter() = default;

ContentFilter::~ContentFilter() = default;

bool ContentFilter::is_filtered(const AK::URL& url) const
{
    if (url.scheme() == "data")
        return false;

    auto url_string = url.to_string();

    for (auto& pattern : m_patterns) {
        if (url_string.matches(pattern.text, CaseSensitivity::CaseSensitive))
            return true;
    }
    return false;
}

void ContentFilter::add_pattern(String const& pattern)
{
    StringBuilder builder;
    if (!pattern.starts_with('*'))
        builder.append('*');
    builder.append(pattern);
    if (!pattern.ends_with('*'))
        builder.append('*');
    m_patterns.empend(builder.to_string());
}

}
