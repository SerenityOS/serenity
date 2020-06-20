/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class MathObject final : public Object {
public:
    explicit MathObject(GlobalObject&);
    virtual void initialize(Interpreter&, GlobalObject&) override;
    virtual ~MathObject() override;

private:
    virtual const char* class_name() const override { return "MathObject"; }

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
};

}
