/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

class MapConstructor final : public NativeFunction {
    JS_OBJECT(MapConstructor, NativeFunction);

public:
    explicit MapConstructor(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~MapConstructor() override;

    virtual Value call() override;
    virtual Value construct(Function&) override;

private:
    virtual bool has_constructor() const override { return true; }

    JS_DECLARE_NATIVE_GETTER(symbol_species_getter);
};

}
