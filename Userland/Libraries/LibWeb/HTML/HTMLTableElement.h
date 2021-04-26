/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>

namespace Web::HTML {

class HTMLTableElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableElementWrapper;

    HTMLTableElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTableElement() override;

    NonnullRefPtr<DOM::HTMLCollection> rows();
    DOM::ExceptionOr<NonnullRefPtr<HTMLTableRowElement>> insert_row(long index);
    DOM::ExceptionOr<void> delete_row(long index);

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
