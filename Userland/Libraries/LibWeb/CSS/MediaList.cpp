/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MediaListPrototype.h>
#include <LibWeb/CSS/MediaList.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

MediaList* MediaList::create(HTML::Window& window_object, NonnullRefPtrVector<MediaQuery>&& media)
{
    return window_object.heap().allocate<MediaList>(window_object.realm(), window_object, move(media));
}

MediaList::MediaList(HTML::Window& window_object, NonnullRefPtrVector<MediaQuery>&& media)
    : Bindings::LegacyPlatformObject(window_object.ensure_web_prototype<Bindings::MediaListPrototype>("MediaList"))
    , m_media(move(media))
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

bool MediaList::is_supported_property_index(u32 index) const
{
    return index < length();
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-item
String MediaList::item(u32 index) const
{
    if (!is_supported_property_index(index))
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

bool MediaList::evaluate(HTML::Window const& window)
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

JS::Value MediaList::item_value(size_t index) const
{
    if (index >= m_media.size())
        return JS::js_undefined();
    return JS::js_string(vm(), m_media[index].to_string());
}

}
