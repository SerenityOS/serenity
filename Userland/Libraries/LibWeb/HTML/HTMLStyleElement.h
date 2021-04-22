/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/CSSLoader.h>

namespace Web::HTML {

class HTMLStyleElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLStyleElementWrapper;

    HTMLStyleElement(DOM::Document&, QualifiedName);
    virtual ~HTMLStyleElement() override;

    virtual void children_changed() override;
    virtual void removed_from(Node*) override;

private:
    CSSLoader m_css_loader;
};

}
