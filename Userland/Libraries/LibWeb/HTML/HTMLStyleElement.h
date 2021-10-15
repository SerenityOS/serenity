/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLStyleElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLStyleElementWrapper;

    HTMLStyleElement(DOM::Document&, QualifiedName);
    virtual ~HTMLStyleElement() override;

    virtual void children_changed() override;
    virtual void removed_from(Node*) override;

    void update_a_style_block();

    RefPtr<CSS::CSSStyleSheet> sheet() const;

private:
    // https://www.w3.org/TR/cssom/#associated-css-style-sheet
    RefPtr<CSS::CSSStyleSheet> m_associated_css_style_sheet;
};

}
