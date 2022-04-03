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

    HTMLTableRowElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLTableRowElement() override;

    NonnullRefPtr<DOM::HTMLCollection> cells() const;

    int row_index() const;
    int section_row_index() const;
};

}
