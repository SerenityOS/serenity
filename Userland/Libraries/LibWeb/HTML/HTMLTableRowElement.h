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

    long row_index() const { return m_row_index; }
    long section_row_index() const { return m_section_row_index; }

    NonnullRefPtr<DOM::HTMLCollection> cells() const;

private:
    void inserted() override;
    void removed_from(Node*) override;

    long m_row_index { -1 };
    long m_section_row_index { -1 };
};

}
