/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/dom.html#domstringmap
class DOMStringMap final
    : public RefCounted<DOMStringMap>
    , public Weakable<DOMStringMap>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMStringMapWrapper;

    static NonnullRefPtr<DOMStringMap> create(DOM::Element& associated_element)
    {
        return adopt_ref(*new DOMStringMap(associated_element));
    }

    virtual ~DOMStringMap() override;

    Vector<String> supported_property_names() const;

    String determine_value_of_named_property(String const&) const;

    DOM::ExceptionOr<void> set_value_of_new_named_property(String const&, String const&);
    DOM::ExceptionOr<void> set_value_of_existing_named_property(String const&, String const&);

    bool delete_existing_named_property(String const&);

private:
    DOMStringMap(DOM::Element&);

    struct NameValuePair {
        String name;
        String value;
    };

    Vector<NameValuePair> get_name_value_pairs() const;

    // https://html.spec.whatwg.org/multipage/dom.html#concept-domstringmap-element
    NonnullRefPtr<DOM::Element> m_associated_element;
};

}
