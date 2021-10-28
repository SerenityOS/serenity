/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class MathObject final : public Object {
    JS_OBJECT(MathObject, Object);

public:
    explicit MathObject(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~MathObject() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(abs);
    JS_DECLARE_NATIVE_FUNCTION(random);
    JS_DECLARE_NATIVE_FUNCTION(sqrt);
    JS_DECLARE_NATIVE_FUNCTION(floor);
    JS_DECLARE_NATIVE_FUNCTION(ceil);
    JS_DECLARE_NATIVE_FUNCTION(round);
    JS_DECLARE_NATIVE_FUNCTION(max);
    JS_DECLARE_NATIVE_FUNCTION(min);
    JS_DECLARE_NATIVE_FUNCTION(trunc);
    JS_DECLARE_NATIVE_FUNCTION(sin);
    JS_DECLARE_NATIVE_FUNCTION(cos);
    JS_DECLARE_NATIVE_FUNCTION(tan);
    JS_DECLARE_NATIVE_FUNCTION(pow);
    JS_DECLARE_NATIVE_FUNCTION(exp);
    JS_DECLARE_NATIVE_FUNCTION(expm1);
    JS_DECLARE_NATIVE_FUNCTION(sign);
    JS_DECLARE_NATIVE_FUNCTION(clz32);
    JS_DECLARE_NATIVE_FUNCTION(acos);
    JS_DECLARE_NATIVE_FUNCTION(acosh);
    JS_DECLARE_NATIVE_FUNCTION(asin);
    JS_DECLARE_NATIVE_FUNCTION(asinh);
    JS_DECLARE_NATIVE_FUNCTION(atan);
    JS_DECLARE_NATIVE_FUNCTION(atanh);
    JS_DECLARE_NATIVE_FUNCTION(log1p);
    JS_DECLARE_NATIVE_FUNCTION(cbrt);
    JS_DECLARE_NATIVE_FUNCTION(atan2);
    JS_DECLARE_NATIVE_FUNCTION(fround);
    JS_DECLARE_NATIVE_FUNCTION(hypot);
    JS_DECLARE_NATIVE_FUNCTION(imul);
    JS_DECLARE_NATIVE_FUNCTION(log);
    JS_DECLARE_NATIVE_FUNCTION(log2);
    JS_DECLARE_NATIVE_FUNCTION(log10);
    JS_DECLARE_NATIVE_FUNCTION(sinh);
    JS_DECLARE_NATIVE_FUNCTION(cosh);
    JS_DECLARE_NATIVE_FUNCTION(tanh);
};

}
