/*
 * Copyright (c) 2021, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DebuggerGlobalJSObject.h"
#include <AK/StringView.h>
#include <LibDebug/DebugInfo.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace HackStudio {

class DebuggerVariableJSObject final : public JS::Object {
    using Base = JS::Object;

public:
    static DebuggerVariableJSObject* create(DebuggerGlobalJSObject&, Debug::DebugInfo::VariableInfo const& variable_info);

    DebuggerVariableJSObject(Debug::DebugInfo::VariableInfo const& variable_info, JS::Object& prototype);
    virtual ~DebuggerVariableJSObject() override = default;

    virtual StringView class_name() const override { return m_variable_info.type_name; }

    JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value value, JS::Value receiver) override;

private:
    DebuggerGlobalJSObject& debugger_object() const;

    Debug::DebugInfo::VariableInfo const& m_variable_info;
};

}
