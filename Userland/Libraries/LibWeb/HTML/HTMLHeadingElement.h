/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHeadingElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLHeadingElementWrapper;

    HTMLHeadingElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLHeadingElement() override;
};

}
