/*
 * Copyright (c) 2021, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DebuggerGlobalJSObject.h"
#include <LibDebug/DebugInfo.h>
#include <LibJS/Runtime/Object.h>

namespace HackStudio {

class DebuggerVariableJSObject final : public JS::Object {
    using Base = JS::Object;

public:
    static DebuggerVariableJSObject* create(DebuggerGlobalJSObject&, const Debug::DebugInfo::VariableInfo& variable_info);

    DebuggerVariableJSObject(const Debug::DebugInfo::VariableInfo& variable_info, JS::Object& prototype);
    virtual ~DebuggerVariableJSObject() override;

    virtual const char* class_name() const override { return m_variable_info.type_name.characters(); }

    bool internal_set(JS::PropertyName const&, JS::Value value, JS::Value receiver) override;

private:
    DebuggerGlobalJSObject& debugger_object() const;

    const Debug::DebugInfo::VariableInfo& m_variable_info;
};

}
