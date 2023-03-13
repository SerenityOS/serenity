/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Variant.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/WebIDL/CallbackType.h>

namespace Web::HTML {

class EventHandler final : public JS::Cell {
public:
    explicit EventHandler(DeprecatedString);
    explicit EventHandler(WebIDL::CallbackType&);

    // Either uncompiled source code or a callback.
    // https://html.spec.whatwg.org/multipage/webappapis.html#event-handler-value
    // NOTE: This does not contain Empty as part of the optimization of not allocating all event handler attributes up front.
    // FIXME: The string should actually be an "internal raw uncompiled handler" struct. This struct is just the uncompiled source code plus a source location for reporting parse errors.
    //        https://html.spec.whatwg.org/multipage/webappapis.html#internal-raw-uncompiled-handler
    Variant<DeprecatedString, JS::GCPtr<WebIDL::CallbackType>> value;

    // https://html.spec.whatwg.org/multipage/webappapis.html#event-handler-listener
    JS::GCPtr<DOM::DOMEventListener> listener;

private:
    virtual StringView class_name() const override { return "EventHandler"sv; }
    virtual void visit_edges(Cell::Visitor&) override;
};

}
