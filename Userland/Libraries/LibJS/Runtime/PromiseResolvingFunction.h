/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
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

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

struct AlreadyResolved final : public Cell {
    bool value { false };

    virtual const char* class_name() const override { return "AlreadyResolved"; }

protected:
    // Allocated cells must be >= sizeof(FreelistEntry), which is 24 bytes -
    // but AlreadyResolved is only 16 bytes without this.
    u8 dummy[8];
};

class PromiseResolvingFunction final : public NativeFunction {
    JS_OBJECT(PromiseResolvingFunction, NativeFunction);

public:
    using FunctionType = AK::Function<Value(VM&, GlobalObject&, Promise&, AlreadyResolved&)>;

    static PromiseResolvingFunction* create(GlobalObject&, Promise&, AlreadyResolved&, FunctionType);

    explicit PromiseResolvingFunction(Promise&, AlreadyResolved&, FunctionType, Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~PromiseResolvingFunction() override = default;

    virtual Value call() override;

private:
    virtual void visit_edges(Visitor&) override;

    Promise& m_promise;
    AlreadyResolved& m_already_resolved;
    FunctionType m_native_function;
};

}
