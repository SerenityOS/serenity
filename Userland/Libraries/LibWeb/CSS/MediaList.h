/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/MediaQuery.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#the-medialist-interface
class MediaList final : public Bindings::LegacyPlatformObject {
    AK_MAKE_NONCOPYABLE(MediaList);
    AK_MAKE_NONMOVABLE(MediaList);
    JS_OBJECT(MediaList, Bindings::LegacyPlatformObject);

public:
    static MediaList* create(Bindings::WindowObject&, NonnullRefPtrVector<MediaQuery>&& media);
    explicit MediaList(Bindings::WindowObject&, NonnullRefPtrVector<MediaQuery>&&);
    ~MediaList() = default;

    MediaList& impl() { return *this; }

    String media_text() const;
    void set_media_text(String const&);
    size_t length() const { return m_media.size(); }
    String item(u32 index) const;
    void append_medium(String);
    void delete_medium(String);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    bool evaluate(HTML::Window const&);
    bool matches() const;

private:
    NonnullRefPtrVector<MediaQuery> m_media;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::MediaList& object) { return &object; }
using MediaListWrapper = Web::CSS::MediaList;
}
