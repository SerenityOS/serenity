/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLabelElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLLabelElement, HTMLElement);

public:
    virtual ~HTMLLabelElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    DeprecatedString for_() const { return attribute(HTML::AttributeNames::for_); }

private:
    HTMLLabelElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
