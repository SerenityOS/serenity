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
public:
    using WrapperType = Bindings::HTMLTableElementWrapper;

    HTMLTableElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTableElement() override;

    RefPtr<HTMLTableCaptionElement> caption();
    void set_caption(HTMLTableCaptionElement*);
    NonnullRefPtr<HTMLTableCaptionElement> create_caption();
    void delete_caption();

    RefPtr<HTMLTableSectionElement> t_head();
    DOM::ExceptionOr<void> set_t_head(HTMLTableSectionElement* thead);
    NonnullRefPtr<HTMLTableSectionElement> create_t_head();
    void delete_t_head();

    RefPtr<HTMLTableSectionElement> t_foot();
    DOM::ExceptionOr<void> set_t_foot(HTMLTableSectionElement* tfoot);
    NonnullRefPtr<HTMLTableSectionElement> create_t_foot();
    void delete_t_foot();

    NonnullRefPtr<DOM::HTMLCollection> t_bodies();
    NonnullRefPtr<HTMLTableSectionElement> create_t_body();

    NonnullRefPtr<DOM::HTMLCollection> rows();
    DOM::ExceptionOr<NonnullRefPtr<HTMLTableRowElement>> insert_row(long index);
    DOM::ExceptionOr<void> delete_row(long index);

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
