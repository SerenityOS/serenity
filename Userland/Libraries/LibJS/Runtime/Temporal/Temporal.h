/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Temporal final : public Object {
    JS_OBJECT(Temporal, Object);

public:
    virtual void initialize(Realm&) override;
    virtual ~Temporal() override = default;

private:
    explicit Temporal(Realm&);
};

}
