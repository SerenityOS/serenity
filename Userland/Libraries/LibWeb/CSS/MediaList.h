/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/MediaQuery.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#the-medialist-interface
class MediaList final
    : public RefCounted<MediaList>
    , public Bindings::Wrappable
    , public Weakable<MediaList> {
    AK_MAKE_NONCOPYABLE(MediaList);
    AK_MAKE_NONMOVABLE(MediaList);

public:
    using WrapperType = Bindings::MediaListWrapper;

    static NonnullRefPtr<MediaList> create(NonnullRefPtrVector<MediaQuery>&& media)
    {
        return adopt_ref(*new MediaList(move(media)));
    }
    ~MediaList() = default;

    String media_text() const;
    void set_media_text(String const&);
    size_t length() const { return m_media.size(); }
    bool is_supported_property_index(u32 index) const;
    String item(u32 index) const;
    void append_medium(String);
    void delete_medium(String);

    bool evaluate(HTML::Window const&);
    bool matches() const;

private:
    explicit MediaList(NonnullRefPtrVector<MediaQuery>&&);

    NonnullRefPtrVector<MediaQuery> m_media;
};

}
