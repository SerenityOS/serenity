/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

class ShadowRealm final : public Object {
    JS_OBJECT(ShadowRealm, Object);

public:
    ShadowRealm(Realm&, ExecutionContext, Object& prototype);
    virtual ~ShadowRealm() override = default;

    [[nodiscard]] Realm const& shadow_realm() const { return m_shadow_realm; }
    [[nodiscard]] Realm& shadow_realm() { return m_shadow_realm; }
    [[nodiscard]] ExecutionContext const& execution_context() const { return m_execution_context; }
    [[nodiscard]] ExecutionContext& execution_context() { return m_execution_context; }

private:
    virtual void visit_edges(Visitor&) override;

    // 3.5 Properties of ShadowRealm Instances, https://tc39.es/proposal-shadowrealm/#sec-properties-of-shadowrealm-instances
    Realm& m_shadow_realm;                // [[ShadowRealm]]
    ExecutionContext m_execution_context; // [[ExecutionContext]]
};

ThrowCompletionOr<Value> perform_shadow_realm_eval(GlobalObject&, StringView source_text, Realm& caller_realm, Realm& eval_realm);
ThrowCompletionOr<Value> shadow_realm_import_value(GlobalObject&, String specifier_string, String export_name_string, Realm& caller_realm, Realm& eval_realm, ExecutionContext& eval_context);
ThrowCompletionOr<Value> get_wrapped_value(GlobalObject&, Realm& caller_realm, Value);

}
