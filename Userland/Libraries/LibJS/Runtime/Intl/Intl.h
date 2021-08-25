/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class Intl final : public Object {
    JS_OBJECT(Intl, Object);

public:
    explicit Intl(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~Intl() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(get_canonical_locales);
};

}
