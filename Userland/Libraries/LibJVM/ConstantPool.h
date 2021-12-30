/*
 * Copyright (c) 2021, Noah Haasis <haasis_noah@yahoo.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

namespace JVM {

// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4
class ConstantPool {
public:
    struct Utf8 {
        u32 length;
        u8 const* bytes;
    };

    struct NameAndType {
        u16 name_index;
        u16 descriptor_index;

        Utf8 name(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(name_index); }
        Utf8 descriptor(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(descriptor_index); }
    };

    struct Class {
        u16 name_index;

        Utf8 name(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(name_index); }
    };

    struct Integer {
        u32 bytes;
    };

    struct Methodref {
        u16 class_index;
        u16 name_and_type_index;

        Class class_info(ConstantPool const& constant_pool) const
        {
            return constant_pool.class_at(class_index);
        }

        NameAndType name_and_type(ConstantPool const& constant_pool) const
        {
            return constant_pool.name_and_type_at(name_and_type_index);
        }
    };

    class Constant {
    public:
        enum Tag : u8 {
            Class = 7,
            Fieldref = 9,
            Methodref = 10,
            InterfaceMethodref = 11,
            String = 8,
            Integer = 3,
            Float = 4,
            Long = 5,
            Double = 6,
            NameAndType = 12,
            Utf8 = 1,
            MethodHandle = 15,
            MethodType = 16,
            Invokedynamic = 18,
        };

        Constant() {};

        Constant(struct Utf8 value)
            : m_tag(Tag::Utf8)
        {
            m_value.as_utf8 = value;
        };

        Constant(struct Class value)
            : m_tag(Tag::Class)
        {
            m_value.as_class = value;
        };

        Constant(struct Integer value)
            : m_tag(Tag::Integer)
        {
            m_value.as_integer = value;
        };

        Constant(struct NameAndType value)
            : m_tag(Tag::NameAndType)
        {
            m_value.as_name_and_type = value;
        };

        Constant(struct Methodref value)
            : m_tag(Tag::Methodref)
        {
            m_value.as_methodref = value;
        };

        struct Utf8 as_utf8() const
        {
            VERIFY(is_utf8());
            return m_value.as_utf8;
        }

        struct Class as_class() const
        {
            VERIFY(is_class());
            return m_value.as_class;
        }

        struct Integer as_integer() const
        {
            VERIFY(is_integer());
            return m_value.as_integer;
        }

        struct NameAndType as_name_and_type() const
        {
            VERIFY(is_name_and_type());
            return m_value.as_name_and_type;
        }

        struct Methodref as_methodref() const
        {
            VERIFY(is_methodref());
            return m_value.as_methodref;
        }

        bool is_utf8() const { return m_tag == Tag::Utf8; }
        bool is_class() const { return m_tag == Tag::Class; }
        bool is_integer() const { return m_tag == Tag::Integer; }
        bool is_name_and_type() const { return m_tag == Tag::NameAndType; }
        bool is_methodref() const { return m_tag == Tag::Methodref; }

    private:
        Tag m_tag;
        union {
            struct Utf8 as_utf8;
            struct Class as_class;
            struct Integer as_integer;
            struct NameAndType as_name_and_type;
            struct Methodref as_methodref;
        } m_value;
    };

    ConstantPool() = default;

    FixedArray<Constant>& constants() { return m_constants; }
    void set_constants(FixedArray<Constant> constants) { m_constants = constants; }

    Utf8 utf8_at(unsigned index) const
    {
        VERIFY(index < m_constants.size());
        return m_constants[index].as_utf8();
    }

    Class class_at(unsigned index) const
    {
        VERIFY(index < m_constants.size());
        return m_constants[index].as_class();
    }

    Integer integer_at(unsigned index) const
    {
        VERIFY(index < m_constants.size());
        return m_constants[index].as_integer();
    }

    NameAndType name_and_type_at(unsigned index) const
    {
        VERIFY(index < m_constants.size());
        return m_constants[index].as_name_and_type();
    }

    Methodref methodref_at(unsigned index) const
    {
        VERIFY(index < m_constants.size());
        return m_constants[index].as_methodref();
    }

private:
    FixedArray<Constant> m_constants;
};

}
