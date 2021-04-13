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
    JS_OBJECT(DebuggerVariableJSObject, JS::Object);

public:
    static DebuggerVariableJSObject* create(DebuggerGlobalJSObject&, const Debug::DebugInfo::VariableInfo& variable_info);

    DebuggerVariableJSObject(const Debug::DebugInfo::VariableInfo& variable_info, JS::Object& prototype);
    virtual ~DebuggerVariableJSObject() override;

    virtual bool put(const JS::PropertyName& name, JS::Value value, JS::Value) override;
    void finish_writing_properties() { m_is_writing_properties = false; }

private:
    DebuggerGlobalJSObject& debugger_object() const;

    const Debug::DebugInfo::VariableInfo& m_variable_info;
    bool m_is_writing_properties { true };
};

}
