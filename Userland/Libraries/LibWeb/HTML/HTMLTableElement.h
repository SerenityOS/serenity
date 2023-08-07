/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

class HTMLTableElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTableElement, HTMLElement);

public:
    virtual ~HTMLTableElement() override;

    JS::GCPtr<HTMLTableCaptionElement> caption();
    WebIDL::ExceptionOr<void> set_caption(HTMLTableCaptionElement*);
    JS::NonnullGCPtr<HTMLTableCaptionElement> create_caption();
    void delete_caption();

    JS::GCPtr<HTMLTableSectionElement> t_head();
    WebIDL::ExceptionOr<void> set_t_head(HTMLTableSectionElement* thead);
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_head();
    void delete_t_head();

    JS::GCPtr<HTMLTableSectionElement> t_foot();
    WebIDL::ExceptionOr<void> set_t_foot(HTMLTableSectionElement* tfoot);
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_foot();
    void delete_t_foot();

    JS::NonnullGCPtr<DOM::HTMLCollection> t_bodies();
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_body();

    JS::NonnullGCPtr<DOM::HTMLCollection> rows();
    WebIDL::ExceptionOr<JS::NonnullGCPtr<HTMLTableRowElement>> insert_row(long index);
    WebIDL::ExceptionOr<void> delete_row(long index);

    // https://www.w3.org/TR/html-aria/#el-table
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::table; }

private:
    HTMLTableElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    JS::GCPtr<DOM::HTMLCollection> mutable m_rows;
    JS::GCPtr<DOM::HTMLCollection> mutable m_t_bodies;
};

}
