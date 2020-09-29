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

#include <AK/String.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Reference {
public:
    Reference() { }
    Reference(Value base, const PropertyName& name, bool strict = false)
        : m_base(base)
        , m_name(name)
        , m_strict(strict)
    {
    }

    enum LocalVariableTag { LocalVariable };
    Reference(LocalVariableTag, const String& name, bool strict = false)
        : m_base(js_null())
        , m_name(name)
        , m_strict(strict)
        , m_local_variable(true)
    {
    }

    enum GlobalVariableTag { GlobalVariable };
    Reference(GlobalVariableTag, const String& name, bool strict = false)
        : m_base(js_null())
        , m_name(name)
        , m_strict(strict)
        , m_global_variable(true)
    {
    }

    Value base() const { return m_base; }
    const PropertyName& name() const { return m_name; }
    bool is_strict() const { return m_strict; }

    bool is_unresolvable() const { return m_base.is_undefined(); }
    bool is_property() const
    {
        return m_base.is_object() || has_primitive_base();
    }

    bool has_primitive_base() const
    {
        return m_base.is_boolean() || m_base.is_string() || m_base.is_number();
    }

    bool is_local_variable() const
    {
        return m_local_variable;
    }

    bool is_global_variable() const
    {
        return m_global_variable;
    }

    void put(GlobalObject&, Value);
    Value get(GlobalObject&);

private:
    void throw_reference_error(GlobalObject&);

    Value m_base { js_undefined() };
    PropertyName m_name;
    bool m_strict { false };
    bool m_local_variable { false };
    bool m_global_variable { false };
};

const LogStream& operator<<(const LogStream&, const Value&);

}
