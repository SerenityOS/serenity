/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <LibWeb/CSS/MediaQuery.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#the-medialist-interface
class MediaList final : public RefCounted<MediaList> {
public:
    static NonnullRefPtr<MediaList> create(NonnullRefPtrVector<MediaQuery>&& media)
    {
        return adopt_ref(*new MediaList(move(media)));
    }
    ~MediaList();

    String media_text() const;
    void set_media_text(String const&);
    size_t length() const { return m_media.size(); }
    Optional<String> item(size_t index) const;
    void append_medium(String);
    void delete_medium(String);

    bool evaluate(DOM::Window const&);
    bool matches() const;

private:
    explicit MediaList(NonnullRefPtrVector<MediaQuery>&&);

    NonnullRefPtrVector<MediaQuery> m_media;
};

}
