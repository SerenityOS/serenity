/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableRowElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTableRowElement, HTMLElement);

public:
    virtual ~HTMLTableRowElement() override;

    JS::NonnullGCPtr<DOM::HTMLCollection> cells() const;

    int row_index() const;
    int section_row_index() const;
    WebIDL::ExceptionOr<JS::NonnullGCPtr<HTMLTableCellElement>> insert_cell(i32 index);
    WebIDL::ExceptionOr<void> delete_cell(i32 index);

    // https://www.w3.org/TR/html-aria/#el-tr
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::row; }

private:
    HTMLTableRowElement(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::GCPtr<DOM::HTMLCollection> mutable m_cells;
};

}
