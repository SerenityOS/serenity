/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Set.h>

namespace JS {

class SetPrototype final : public Set {
    JS_OBJECT(SetPrototype, Set);

public:
    SetPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SetPrototype() override;

private:
    JS_DECLARE_NATIVE_GETTER(size_getter);
};

}
