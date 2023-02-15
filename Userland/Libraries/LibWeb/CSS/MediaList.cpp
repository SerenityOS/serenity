/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MediaListPrototype.h>
#include <LibWeb/CSS/MediaList.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

MediaList* MediaList::create(JS::Realm& realm, NonnullRefPtrVector<MediaQuery>&& media)
{
    return realm.heap().allocate<MediaList>(realm, realm, move(media)).release_allocated_value_but_fixme_should_propagate_errors();
}

MediaList::MediaList(JS::Realm& realm, NonnullRefPtrVector<MediaQuery>&& media)
    : Bindings::LegacyPlatformObject(realm)
    , m_media(move(media))
{
}

JS::ThrowCompletionOr<void> MediaList::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MediaListPrototype>(realm, "MediaList"));

    return {};
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-mediatext
DeprecatedString MediaList::media_text() const
{
    return serialize_a_media_query_list(m_media).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-mediatext
void MediaList::set_media_text(DeprecatedString const& text)
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
DeprecatedString MediaList::item(u32 index) const
{
    if (!is_supported_property_index(index))
        return {};

    return m_media[index].to_string().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-appendmedium
void MediaList::append_medium(DeprecatedString medium)
{
    auto m = parse_media_query({}, medium);
    if (!m)
        return;
    if (m_media.contains_slow(*m))
        return;
    m_media.append(m.release_nonnull());
}

// https://www.w3.org/TR/cssom-1/#dom-medialist-deletemedium
void MediaList::delete_medium(DeprecatedString medium)
{
    auto m = parse_media_query({}, medium);
    if (!m)
        return;
    m_media.remove_all_matching([&](auto& existing) -> bool {
        return m->to_string().release_value_but_fixme_should_propagate_errors() == existing->to_string().release_value_but_fixme_should_propagate_errors();
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
    if (m_media.is_empty()) {
        return true;
    }

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
    return JS::PrimitiveString::create(vm(), m_media[index].to_string().release_value_but_fixme_should_propagate_errors().to_deprecated_string());
}

}
