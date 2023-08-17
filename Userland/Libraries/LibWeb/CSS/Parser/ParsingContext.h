/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Realm.h>
#include <LibWeb/DOM/Document.h>

namespace Web::CSS::Parser {

class ParsingContext {
public:
    explicit ParsingContext(JS::Realm&);
    explicit ParsingContext(DOM::Document const&);
    explicit ParsingContext(DOM::Document const&, AK::URL);
    explicit ParsingContext(DOM::ParentNode&);

    bool in_quirks_mode() const;
    DOM::Document const* document() const { return m_document; }
    HTML::Window const* window() const;
    AK::URL complete_url(StringView) const;

    PropertyID current_property_id() const { return m_current_property_id; }
    void set_current_property_id(PropertyID property_id) { m_current_property_id = property_id; }

    JS::Realm& realm() const { return m_realm; }

private:
    JS::NonnullGCPtr<JS::Realm> m_realm;
    JS::GCPtr<DOM::Document const> m_document;
    PropertyID m_current_property_id { PropertyID::Invalid };
    AK::URL m_url;
};

}
