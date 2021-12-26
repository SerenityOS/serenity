/*
 * Copyright (c) 2020, The SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSelectElement final
    : public HTMLElement
    , public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLSelectElementWrapper;

    HTMLSelectElement(DOM::Document&, QualifiedName);
    virtual ~HTMLSelectElement() override;

private:
    // ^DOM::Node
    virtual void inserted() override;
    virtual void removed_from(DOM::Node*) override;

    // ^HTML::FormAssociatedElement
    virtual HTMLElement& form_associated_element_to_html_element() override { return *this; }
};

}
