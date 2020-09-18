/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <AK/LogStream.h>
#include <AK/Types.h>

namespace JS {

struct Attribute {
    enum {
        Configurable = 1 << 0,
        Enumerable = 1 << 1,
        Writable = 1 << 2,
        HasGetter = 1 << 3,
        HasSetter = 1 << 4,
        HasConfigurable = 1 << 5,
        HasEnumerable = 1 << 6,
        HasWritable = 1 << 7,
    };
};

class PropertyAttributes {
public:
    PropertyAttributes(u8 bits = 0)
    {
        m_bits = bits;
        if (bits & Attribute::Configurable)
            m_bits |= Attribute::HasConfigurable;
        if (bits & Attribute::Enumerable)
            m_bits |= Attribute::HasEnumerable;
        if (bits & Attribute::Writable)
            m_bits |= Attribute::HasWritable;
    }

    bool is_empty() const { return !m_bits; }

    bool has_configurable() const { return m_bits & Attribute::HasConfigurable; }
    bool has_enumerable() const { return m_bits & Attribute::HasEnumerable; }
    bool has_writable() const { return m_bits & Attribute::HasWritable; }
    bool has_getter() const { return m_bits & Attribute::HasGetter; }
    bool has_setter() const { return m_bits & Attribute::HasSetter; }

    bool is_configurable() const { return m_bits & Attribute::Configurable; }
    bool is_enumerable() const { return m_bits & Attribute::Enumerable; }
    bool is_writable() const { return m_bits & Attribute::Writable; }

    void set_has_configurable() { m_bits |= Attribute::HasConfigurable; }
    void set_has_enumerable() { m_bits |= Attribute::HasEnumerable; }
    void set_has_writable() { m_bits |= Attribute::HasWritable; }
    void set_configurable() { m_bits |= Attribute::Configurable; }
    void set_enumerable() { m_bits |= Attribute::Enumerable; }
    void set_writable() { m_bits |= Attribute::Writable; }
    void set_has_getter() { m_bits |= Attribute::HasGetter; }
    void set_has_setter() { m_bits |= Attribute::HasSetter; }

    bool operator==(const PropertyAttributes& other) const { return m_bits == other.m_bits; }
    bool operator!=(const PropertyAttributes& other) const { return m_bits != other.m_bits; }

    u8 bits() const { return m_bits; }

private:
    u8 m_bits;
};

const LogStream& operator<<(const LogStream& stream, const PropertyAttributes& attributes);

const PropertyAttributes default_attributes = Attribute::Configurable | Attribute::Writable | Attribute::Enumerable;

}
