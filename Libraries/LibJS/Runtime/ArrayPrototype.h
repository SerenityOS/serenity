/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

class ArrayPrototype final : public Object {
public:
    ArrayPrototype();
    virtual ~ArrayPrototype() override;

private:
    virtual const char* class_name() const override { return "ArrayPrototype"; }

    static Value filter(Interpreter&);
    static Value for_each(Interpreter&);
    static Value map(Interpreter&);
    static Value pop(Interpreter&);
    static Value push(Interpreter&);
    static Value shift(Interpreter&);
    static Value to_string(Interpreter&);
    static Value unshift(Interpreter&);
    static Value join(Interpreter&);
    static Value concat(Interpreter&);
    static Value slice(Interpreter&);
    static Value index_of(Interpreter&);
    static Value reduce(Interpreter&);
    static Value reduce_right(Interpreter&);
    static Value reverse(Interpreter&);
    static Value last_index_of(Interpreter&);
    static Value includes(Interpreter&);
    static Value find(Interpreter&);
    static Value find_index(Interpreter&);
    static Value some(Interpreter&);
    static Value every(Interpreter&);
    static Value splice(Interpreter&);
};

}
