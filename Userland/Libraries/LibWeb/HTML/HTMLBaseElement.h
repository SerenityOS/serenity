/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLBaseElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLBaseElementWrapper;

    HTMLBaseElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLBaseElement() override;

    String href() const;
    void set_href(String const& href);

    AK::URL const& frozen_base_url() const { return m_frozen_base_url; }

    virtual void inserted() override;
    virtual void parse_attribute(FlyString const& name, String const& value) override;

private:
    // https://html.spec.whatwg.org/multipage/semantics.html#frozen-base-url
    // A base element that is the first base element with an href content attribute in a document tree has a frozen base URL.
    AK::URL m_frozen_base_url;

    void set_the_frozen_base_url();
};

}
