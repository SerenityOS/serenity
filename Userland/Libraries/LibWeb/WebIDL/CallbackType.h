/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Cell.h>
#include <LibWeb/Forward.h>

namespace Web::WebIDL {

// https://webidl.spec.whatwg.org/#idl-callback-interface
class CallbackType final : public JS::Cell {
public:
    CallbackType(JS::Object& callback, HTML::EnvironmentSettingsObject& callback_context);

    JS::Object& callback;

    // https://webidl.spec.whatwg.org/#dfn-callback-context
    HTML::EnvironmentSettingsObject& callback_context;

private:
    virtual StringView class_name() const override;
    virtual void visit_edges(Cell::Visitor&) override;
};

}
