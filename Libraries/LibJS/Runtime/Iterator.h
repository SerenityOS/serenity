/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Function.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Iterator final : public Object {
public:
    struct IteratorResult {
        bool finished { false };
        Value value;
    };

    Iterator(Object& iterable, AK::Function<IteratorResult(Object&, const Vector<Value>&)>&& next_function);

    virtual ~Iterator() override;

    Object& iterable() { return m_iterable; }
    AK::Function<IteratorResult(Object&, const Vector<Value>&)>& next_function() { return m_next_function; }
    bool done() const { return m_done; }

    virtual bool is_iterator() const override { return true; }

private:
    virtual void visit_children(Visitor&) override;
    virtual const char* class_name() const override { return "Iterator"; }

    static Value next(Interpreter&);

    Object& m_iterable;
    bool m_done { false };
    AK::Function<IteratorResult(Object&, const Vector<Value>&)> m_next_function;
};

}
