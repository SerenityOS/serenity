/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTitleElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTitleElementWrapper;

    HTMLTitleElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLTitleElement() override;

private:
    virtual void children_changed() override;
};

}
