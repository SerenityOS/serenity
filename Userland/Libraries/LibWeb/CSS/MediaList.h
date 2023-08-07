/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/MediaQuery.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#the-medialist-interface
class MediaList final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(MediaList, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<MediaList>> create(JS::Realm&, Vector<NonnullRefPtr<MediaQuery>>&& media);
    ~MediaList() = default;

    DeprecatedString media_text() const;
    void set_media_text(DeprecatedString const&);
    size_t length() const { return m_media.size(); }
    DeprecatedString item(u32 index) const;
    void append_medium(DeprecatedString);
    void delete_medium(DeprecatedString);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

    bool evaluate(HTML::Window const&);
    bool matches() const;

private:
    MediaList(JS::Realm&, Vector<NonnullRefPtr<MediaQuery>>&&);

    virtual void initialize(JS::Realm&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return false; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    Vector<NonnullRefPtr<MediaQuery>> m_media;
};

}
