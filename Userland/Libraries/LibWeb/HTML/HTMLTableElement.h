/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>

namespace Web::HTML {

class HTMLTableElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTableElement, HTMLElement);

public:
    virtual ~HTMLTableElement() override;

    JS::GCPtr<HTMLTableCaptionElement> caption();
    void set_caption(HTMLTableCaptionElement*);
    JS::NonnullGCPtr<HTMLTableCaptionElement> create_caption();
    void delete_caption();

    JS::GCPtr<HTMLTableSectionElement> t_head();
    DOM::ExceptionOr<void> set_t_head(HTMLTableSectionElement* thead);
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_head();
    void delete_t_head();

    JS::GCPtr<HTMLTableSectionElement> t_foot();
    DOM::ExceptionOr<void> set_t_foot(HTMLTableSectionElement* tfoot);
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_foot();
    void delete_t_foot();

    JS::NonnullGCPtr<DOM::HTMLCollection> t_bodies();
    JS::NonnullGCPtr<HTMLTableSectionElement> create_t_body();

    JS::NonnullGCPtr<DOM::HTMLCollection> rows();
    DOM::ExceptionOr<JS::NonnullGCPtr<HTMLTableRowElement>> insert_row(long index);
    DOM::ExceptionOr<void> delete_row(long index);

private:
    HTMLTableElement(DOM::Document&, DOM::QualifiedName);

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}

WRAPPER_HACK(HTMLTableElement, Web::HTML)
