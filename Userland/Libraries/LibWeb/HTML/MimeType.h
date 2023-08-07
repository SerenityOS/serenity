/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/system-state.html#mimetype
class MimeType : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(MimeType, Bindings::PlatformObject);

public:
    virtual ~MimeType() override;

    String const& type() const;
    String description() const;
    String const& suffixes() const;
    JS::NonnullGCPtr<Plugin> enabled_plugin() const;

private:
    MimeType(JS::Realm&, String type);

    virtual void initialize(JS::Realm&) override;

    // https://html.spec.whatwg.org/multipage/system-state.html#concept-mimetype-type
    String m_type;
};

}
