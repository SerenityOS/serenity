/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableRowElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableRowElementWrapper;

    HTMLTableRowElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTableRowElement() override;
};

}
