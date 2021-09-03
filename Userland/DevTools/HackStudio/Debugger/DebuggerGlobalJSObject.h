/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Weakable.h>
#include <LibDebug/DebugInfo.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace HackStudio {

class DebuggerGlobalJSObject final
    : public JS::GlobalObject
    , public Weakable<DebuggerGlobalJSObject> {
    JS_OBJECT(DebuggerGlobalJSObject, JS::GlobalObject);

public:
    DebuggerGlobalJSObject();

    virtual JS::Value internal_get(JS::PropertyName const&, JS::Value receiver) const override;
    virtual bool internal_set(JS::PropertyName const&, JS::Value value, JS::Value receiver) override;

    Optional<JS::Value> debugger_to_js(const Debug::DebugInfo::VariableInfo&) const;
    Optional<u32> js_to_debugger(JS::Value value, const Debug::DebugInfo::VariableInfo&) const;

private:
    NonnullOwnPtrVector<Debug::DebugInfo::VariableInfo> m_variables;
};

}
