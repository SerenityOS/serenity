/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class PluralRulesConstructor final : public NativeFunction {
    JS_OBJECT(PluralRulesConstructor, NativeFunction);

public:
    virtual void initialize(Realm&) override;
    virtual ~PluralRulesConstructor() override = default;

    virtual ThrowCompletionOr<Value> call() override;
    virtual ThrowCompletionOr<NonnullGCPtr<Object>> construct(FunctionObject& new_target) override;

private:
    explicit PluralRulesConstructor(Realm&);

    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_FUNCTION(supported_locales_of);
};

ThrowCompletionOr<PluralRules*> initialize_plural_rules(VM&, PluralRules&, Value locales_value, Value options_value);

}
