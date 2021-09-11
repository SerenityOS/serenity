/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

class NativeFunction : public FunctionObject {
    JS_OBJECT(NativeFunction, FunctionObject);

public:
    static NativeFunction* create(GlobalObject&, const FlyString& name, Function<Value(VM&, GlobalObject&)>);

    explicit NativeFunction(FlyString name, Function<Value(VM&, GlobalObject&)>, Object& prototype);
    virtual void initialize(GlobalObject&) override { }
    virtual ~NativeFunction() override;

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;

    virtual const FlyString& name() const override { return m_name; };
    virtual bool has_constructor() const { return false; }

    virtual bool is_strict_mode() const override;

    virtual Realm* realm() const override { return m_realm; }

protected:
    NativeFunction(FlyString name, Object& prototype);
    explicit NativeFunction(Object& prototype);

private:
    virtual FunctionEnvironment* create_environment(FunctionObject&) override final;
    virtual bool is_native_function() const final { return true; }

    FlyString m_name;
    Function<Value(VM&, GlobalObject&)> m_native_function;
    Realm* m_realm { nullptr };
};

template<>
inline bool Object::fast_is<NativeFunction>() const { return is_native_function(); }

}
