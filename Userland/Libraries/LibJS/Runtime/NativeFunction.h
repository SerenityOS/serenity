/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/Function.h>

namespace JS {

class NativeFunction : public Function {
    JS_OBJECT(NativeFunction, Function);

public:
    static NativeFunction* create(GlobalObject&, const FlyString& name, AK::Function<Value(VM&, GlobalObject&)>);

    explicit NativeFunction(const FlyString& name, AK::Function<Value(VM&, GlobalObject&)>, Object& prototype);
    virtual void initialize(GlobalObject&) override { }
    virtual ~NativeFunction() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

    virtual const FlyString& name() const override { return m_name; };
    virtual bool has_constructor() const { return false; }

    virtual bool is_strict_mode() const override;

protected:
    NativeFunction(const FlyString& name, Object& prototype);
    explicit NativeFunction(Object& prototype);

private:
    virtual LexicalEnvironment* create_environment() override final;

    FlyString m_name;
    AK::Function<Value(VM&, GlobalObject&)> m_native_function;
};

}
