#pragma once
#include <AK/Assertions.h>
#include <AK/Forward.h>
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <LibJVM/Forward.h>

namespace JVM {

enum class Type {
    Byte,
    Short,
    Int,
    Long,
    Char,
    Float,
    Double,
    ReturnAddress,
    Boolean,
    Class,
    Array,
    Interface,
    Null,
};

enum class StackType {
    Byte,
    Short,
    Int,
    Long,
    LongHighBytes,
    Char,
    Float,
    Double,
    DoubleHighBytes,
    ReturnAddress,
    Boolean,
    Class,
    Array,
    Interface,
    Null,
};

class Class {} ;
class Interface {} ;

class Value {
public:
    int as_null() const
    {
        VERIFY(m_type == Type::Null);
    }
    explicit Value(Type type)
        : m_type(type)
    {

    }
    explicit Value(int value)
        : m_type(Type::Int)
    {
        m_value.as_int = value;
    }

    Type type() const { return m_type; }
private:
    Type m_type { Type::Null };
    union {
        int as_byte;
        int as_short;
        int as_int;
        long as_long;
        unsigned char as_char;
        float as_float;
        double as_double;
        long as_ret_address;
        bool as_bool;
        //These reference types are just indexes right now, but into what I'm not sure.
        int as_array;
        int as_class;
        int as_interface;
        int as_null;
    } m_value;
};

class StackValue {
public:
    explicit StackValue(StackType type)
        : m_type(type)
    {

    }

    explicit StackValue(int value)
        : m_type(StackType::Int)
    {
        m_value.as_int = value;
    }
    StackType type() const { return m_type; }
private:
    StackType m_type { StackType::Null };
    union {
        int as_byte;
        int as_short;
        int as_int;
        int as_long;
        int as_long_high_bytes;
        unsigned char as_char;
        float as_float;
        double as_double;
        long as_ret_address;
        bool as_bool;
        //These reference types are just indexes right now, but into what I'm not sure.
        int as_array;
        int as_class;
        int as_interface;
        int as_null;
    } m_value;
};

}
