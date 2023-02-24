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
    WEB_PLATFORM_OBJECT(MediaList, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<MediaList>> create(JS::Realm&, NonnullRefPtrVector<MediaQuery>&& media);
    ~MediaList() = default;

    DeprecatedString media_text() const;
    void set_media_text(DeprecatedString const&);
    size_t length() const { return m_media.size(); }
    DeprecatedString item(u32 index) const;
    void append_medium(DeprecatedString);
    void delete_medium(DeprecatedString);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    bool evaluate(HTML::Window const&);
    bool matches() const;

private:
    MediaList(JS::Realm&, NonnullRefPtrVector<MediaQuery>&&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    NonnullRefPtrVector<MediaQuery> m_media;
};

}
