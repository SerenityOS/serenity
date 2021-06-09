/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class SetConstructor final : public NativeFunction {
    JS_OBJECT(SetConstructor, NativeFunction);

public:
    explicit SetConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SetConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function&) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_GETTER(symbol_species_getter);
};

}
