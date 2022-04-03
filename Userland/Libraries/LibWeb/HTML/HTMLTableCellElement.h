/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableCellElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableCellElementWrapper;

    HTMLTableCellElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLTableCellElement() override;

    unsigned col_span() const;
    unsigned row_span() const;

    void set_col_span(unsigned);
    void set_row_span(unsigned);

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
