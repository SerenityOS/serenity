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
    static ContentFilter* filter = new ContentFilter;
    return *filter;
}

ContentFilter::ContentFilter()
{
}

ContentFilter::~ContentFilter()
{
}

bool ContentFilter::is_filtered(const AK::URL& url) const
{
    if (url.protocol() == "data")
        return false;

    auto url_string = url.to_string();

    for (auto& pattern : m_patterns) {
        if (url_string.matches(pattern.text, CaseSensitivity::CaseSensitive))
            return true;
    }
    return false;
}

void ContentFilter::add_pattern(const String& pattern)
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
