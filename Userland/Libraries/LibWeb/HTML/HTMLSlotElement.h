/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Slot.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSlotElement final
    : public HTMLElement
    , public DOM::Slot {
    WEB_PLATFORM_OBJECT(HTMLSlotElement, HTMLElement);

public:
    virtual ~HTMLSlotElement() override;

private:
    HTMLSlotElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(JS::Cell::Visitor&) override;
};

}
