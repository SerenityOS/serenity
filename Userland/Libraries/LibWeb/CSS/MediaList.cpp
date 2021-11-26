/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/MediaList.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

MediaList::MediaList(NonnullRefPtrVector<MediaQuery>&& media)
    : m_media(move(media))
{
}

MediaList::~MediaList()
{
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-mediatext
String MediaList::media_text() const
{
    return serialize_a_media_query_list(m_media);
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-mediatext
void MediaList::set_media_text(String const& text)
{
    m_media.clear();
    if (text.is_empty())
        return;
    m_media = parse_media_query_list({}, text);
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-item
Optional<String> MediaList::item(size_t index) const
{
    if (index >= length())
        return {};

    return m_media[index].to_string();
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-appendmedium
void MediaList::append_medium(String medium)
{
    auto m = parse_media_query({}, medium);
    if (!m)
        return;
    if (m_media.contains_slow(*m))
        return;
    m_media.append(m.release_nonnull());
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-deletemedium
void MediaList::delete_medium(String medium)
{
    auto m = parse_media_query({}, medium);
    if (!m)
        return;
    m_media.remove_all_matching([&](auto& existing) -> bool {
        return m->to_string() == existing->to_string();
    });
    // FIXME: If nothing was removed, then throw a NotFoundError exception.
}

bool MediaList::evaluate(DOM::Window const& window)
{
    for (auto& media : m_media)
        media.evaluate(window);

    return matches();
}

bool MediaList::matches() const
{
    for (auto& media : m_media) {
        if (media.matches())
            return true;
    }
    return false;
}

}
