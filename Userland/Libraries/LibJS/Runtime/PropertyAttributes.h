/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
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

const PropertyAttributes default_attributes = Attribute::Configurable | Attribute::Writable | Attribute::Enumerable;

}

namespace AK {

template<>
struct Formatter<JS::PropertyAttributes> : Formatter<u8> {
    void format(FormatBuilder& builder, const JS::PropertyAttributes& attributes)
    {
        Formatter<u8>::format(builder, attributes.bits());
    }
};

}
