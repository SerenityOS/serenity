/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLAnchorElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLAnchorElementWrapper;

    HTMLAnchorElement(DOM::Document&, QualifiedName);
    virtual ~HTMLAnchorElement() override;

    String href() const { return attribute(HTML::AttributeNames::href); }
    String target() const { return attribute(HTML::AttributeNames::target); }

    virtual bool is_focusable() const override { return has_attribute(HTML::AttributeNames::href); }
};

}
