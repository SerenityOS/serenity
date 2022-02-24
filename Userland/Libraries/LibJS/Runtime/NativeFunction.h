/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/PropertyKey.h>

namespace JS {

class NativeFunction : public FunctionObject {
    JS_OBJECT(NativeFunction, FunctionObject);

public:
    static NativeFunction* create(GlobalObject&, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> behaviour, i32 length, PropertyKey const& name, Optional<Realm*> = {}, Optional<Object*> prototype = {}, Optional<StringView> const& prefix = {});
    static NativeFunction* create(GlobalObject&, const FlyString& name, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)>);

    NativeFunction(GlobalObject&, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)>, Object* prototype, Realm& realm);
    NativeFunction(FlyString name, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)>, Object& prototype);
    virtual void initialize(GlobalObject&) override { }
    virtual ~NativeFunction() override;

    virtual ThrowCompletionOr<Value> internal_call(Value this_argument, MarkedVector<Value> arguments_list) override;
    virtual ThrowCompletionOr<Object*> internal_construct(MarkedVector<Value> arguments_list, FunctionObject& new_target) override;

    // Used for [[Call]] / [[Construct]]'s "...result of evaluating F in a manner that conforms to the specification of F".
    // Needs to be overridden by all NativeFunctions without an m_native_function.
    virtual ThrowCompletionOr<Value> call();
    virtual ThrowCompletionOr<Object*> construct(FunctionObject& new_target);

    virtual const FlyString& name() const override { return m_name; };
    virtual bool is_strict_mode() const override;
    virtual bool has_constructor() const override { return false; }
    virtual Realm* realm() const override { return m_realm; }

    Optional<FlyString> const& initial_name() const { return m_initial_name; }
    void set_initial_name(Badge<FunctionObject>, FlyString initial_name) { m_initial_name = move(initial_name); }

protected:
    NativeFunction(FlyString name, Object& prototype);
    explicit NativeFunction(Object& prototype);

private:
    virtual bool is_native_function() const final { return true; }

    FlyString m_name;
    Optional<FlyString> m_initial_name; // [[InitialName]]
    Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> m_native_function;
    Realm* m_realm { nullptr };
};

template<>
inline bool Object::fast_is<NativeFunction>() const { return is_native_function(); }

}
