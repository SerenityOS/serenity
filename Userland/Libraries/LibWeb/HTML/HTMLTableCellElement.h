/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableCellElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTableCellElement, HTMLElement);

public:
    virtual ~HTMLTableCellElement() override;

    unsigned col_span() const;
    unsigned row_span() const;

    void set_col_span(unsigned);
    void set_row_span(unsigned);

private:
    HTMLTableCellElement(DOM::Document&, DOM::QualifiedName);
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}

WRAPPER_HACK(HTMLTableCellElement, Web::HTML)
