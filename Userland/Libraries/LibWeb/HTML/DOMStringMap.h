/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/dom.html#domstringmap
class DOMStringMap final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(DOMStringMap, Bindings::LegacyPlatformObject);

public:
    static JS::NonnullGCPtr<DOMStringMap> create(DOM::Element&);

    virtual ~DOMStringMap() override;

    String determine_value_of_named_property(String const&) const;

    WebIDL::ExceptionOr<void> set_value_of_new_named_property(String const&, String const&);
    WebIDL::ExceptionOr<void> set_value_of_existing_named_property(String const&, String const&);

    bool delete_existing_named_property(String const&);

private:
    explicit DOMStringMap(DOM::Element&);

    virtual void visit_edges(Cell::Visitor&) override;

    // ^LegacyPlatformObject
    virtual JS::Value named_item_value(FlyString const&) const override;
    virtual Vector<String> supported_property_names() const override;

    struct NameValuePair {
        String name;
        String value;
    };

    Vector<NameValuePair> get_name_value_pairs() const;

    // https://html.spec.whatwg.org/multipage/dom.html#concept-domstringmap-element
    JS::NonnullGCPtr<DOM::Element> m_associated_element;
};

}
