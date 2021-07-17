/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibSQL/Serialize.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Type.h>
#include <string.h>

namespace SQL {

class Value;

class BaseImpl {
public:
    explicit BaseImpl(SQLType type = SQLType::Null)
        : m_type(type)
    {
    }

    [[nodiscard]] SQLType type() const { return m_type; }
    [[nodiscard]] String type_name() const { return SQLType_name(type()); }

private:
    SQLType m_type { SQLType::Null };
};

class NullImpl : public BaseImpl {
public:
    explicit NullImpl()
        : BaseImpl(SQLType::Null)
    {
    }

    [[nodiscard]] static bool is_null() { return true; }
    [[nodiscard]] static String to_string() { return "(null)"; }
    [[nodiscard]] static Optional<int> to_int() { return {}; }
    [[nodiscard]] static Optional<double> to_double() { return {}; }
    [[nodiscard]] static Optional<bool> to_bool() { return {}; }
    [[nodiscard]] static bool to_vector(Vector<Value>&) { return false; }
    static void assign(Value const&) { }
    static void assign_string(String const&) { }
    static void assign_int(int) { }
    static void assign_double(double) { }
    static void assign_bool(bool) { }
    static void assign_vector(Vector<Value> const&) { }
    [[nodiscard]] static size_t length() { return 0; }
    [[nodiscard]] static bool can_cast(Value const&);
    [[nodiscard]] static int compare(Value const&);
    static void serialize(ByteBuffer&) { }
    static void deserialize(ByteBuffer&, size_t&) { }
    [[nodiscard]] static u32 hash() { return 0; }
};

template<typename T>
class Impl : public BaseImpl {
public:
    [[nodiscard]] bool is_null() const
    {
        return !m_value.has_value();
    }

    [[nodiscard]] T const& value() const
    {
        VERIFY(m_value.has_value());
        return m_value.value();
    }

    [[nodiscard]] size_t length() const
    {
        return sizeof(T);
    }

    void serialize(ByteBuffer& buffer) const
    {
        serialize_to(buffer, value());
    }

    void deserialize(ByteBuffer& buffer, size_t& at_offset)
    {
        T value;
        deserialize_from(buffer, at_offset, value);
        m_value = value;
    }

protected:
    explicit Impl(SQLType sql_type)
        : BaseImpl(sql_type)
    {
    }

    Optional<T> m_value {};
};

class TextImpl : public Impl<String> {
public:
    explicit TextImpl()
        : Impl(SQLType::Text)
    {
    }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] static bool to_vector(Vector<Value>&) { return false; }
    void assign(Value const&);
    void assign_string(String const&);
    void assign_int(int);
    void assign_double(double);
    void assign_bool(bool);
    void assign_vector(Vector<Value> const&) { m_value = {}; }
    [[nodiscard]] size_t length() const;
    [[nodiscard]] static bool can_cast(Value const&) { return true; }
    [[nodiscard]] int compare(Value const& other) const;
    [[nodiscard]] u32 hash() const;
};

class IntegerImpl : public Impl<int> {
public:
    IntegerImpl()
        : Impl(SQLType::Integer)
    {
    }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] static bool to_vector(Vector<Value>&) { return false; }
    void assign(Value const&);
    void assign_string(String const&);
    void assign_int(int);
    void assign_double(double);
    void assign_bool(bool);
    void assign_vector(Vector<Value> const&) { m_value = {}; }
    [[nodiscard]] static bool can_cast(Value const&);
    [[nodiscard]] int compare(Value const& other) const;
    [[nodiscard]] u32 hash() const;
};

class FloatImpl : public Impl<double> {
public:
    explicit FloatImpl()
        : Impl(SQLType::Float)
    {
    }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] static Optional<bool> to_bool() { return {}; }
    [[nodiscard]] static bool to_vector(Vector<Value>&) { return false; }
    void assign(Value const&);
    void assign_string(String const&);
    void assign_int(int);
    void assign_double(double);
    void assign_bool(bool) { m_value = {}; }
    void assign_vector(Vector<Value> const&) { m_value = {}; }
    [[nodiscard]] static bool can_cast(Value const&);
    [[nodiscard]] int compare(Value const& other) const;

    // Using floats in hash functions is a bad idea. Let's disable that for now.
    [[nodiscard]] static u32 hash() { VERIFY_NOT_REACHED(); }
};

class BooleanImpl : public Impl<bool> {
public:
    explicit BooleanImpl()
        : Impl(SQLType::Boolean)
    {
    }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] static Optional<double> to_double();
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] static bool to_vector(Vector<Value>&) { return false; }
    void assign(Value const&);
    void assign_string(String const&);
    void assign_int(int);
    void assign_double(double);
    void assign_bool(bool);
    void assign_vector(Vector<Value> const&) { m_value = {}; }
    [[nodiscard]] static bool can_cast(Value const&);
    [[nodiscard]] int compare(Value const& other) const;
    [[nodiscard]] u32 hash() const;
};

using BaseTypeImpl = Variant<NullImpl, TextImpl, IntegerImpl, FloatImpl, BooleanImpl>;

class ContainerValueImpl : public Impl<Vector<BaseTypeImpl>> {
public:
    virtual ~ContainerValueImpl() = default;

    [[nodiscard]] String to_string() const;
    [[nodiscard]] static Optional<int> to_int() { return {}; }
    [[nodiscard]] static Optional<double> to_double() { return {}; }
    [[nodiscard]] static Optional<bool> to_bool() { return {}; }
    [[nodiscard]] bool to_vector(Vector<Value>&) const;
    void assign_string(String const&) { m_value = {}; }
    void assign_int(int) { m_value = {}; }
    void assign_double(double) { m_value = {}; }
    void assign_bool(bool) { m_value = {}; }
    void assign_vector(Vector<Value> const&);
    [[nodiscard]] u32 hash() const;

    virtual bool validate_before_assignment(Vector<Value> const&) { return true; }
    virtual bool validate(BaseTypeImpl const&) { return true; }
    virtual bool validate_after_assignment() { return true; }
    [[nodiscard]] Vector<String> to_string_vector() const;
    [[nodiscard]] size_t size() const { return is_null() ? 0 : value().size(); }
    bool append(Value const&);
    bool append(BaseTypeImpl const& value);
    void serialize_values(ByteBuffer& buffer) const;
    void deserialize_values(ByteBuffer&, size_t& at_offset);

protected:
    explicit ContainerValueImpl(SQLType sql_type, Optional<size_t> const& max_size = {})
        : Impl(sql_type)
        , m_max_size(max_size)
    {
    }

    Optional<size_t> m_max_size {};
};

class TupleImpl : public ContainerValueImpl {
public:
    explicit TupleImpl(NonnullRefPtr<TupleDescriptor> const& descriptor, bool is_null = true)
        : ContainerValueImpl(SQLType::Tuple, is_null)
        , m_descriptor(descriptor)
    {
        m_max_size = m_descriptor->size();
    }

    explicit TupleImpl()
        : ContainerValueImpl(SQLType::Tuple, {})
    {
    }

    void assign(Value const&);
    [[nodiscard]] size_t length() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    [[nodiscard]] int compare(Value const& other) const;

    virtual bool validate(BaseTypeImpl const&) override;
    virtual bool validate_after_assignment() override;
    void serialize(ByteBuffer& buffer) const;
    void deserialize(ByteBuffer& buffer, size_t&);

private:
    RefPtr<TupleDescriptor> m_descriptor;
};

class ArrayImpl : public ContainerValueImpl {
public:
    explicit ArrayImpl(SQLType element_type, Optional<size_t> const& max_size = {})
        : ContainerValueImpl(SQLType::Array, max_size)
        , m_element_type(element_type)
    {
    }

    explicit ArrayImpl()
        : ContainerValueImpl(SQLType::Array, {})
        , m_element_type(SQLType::Null)
    {
    }

    void assign(Value const&);
    [[nodiscard]] size_t length() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    [[nodiscard]] int compare(Value const& other) const;
    void serialize(ByteBuffer& buffer) const;
    void deserialize(ByteBuffer& buffer, size_t&);
    virtual bool validate(BaseTypeImpl const&) override;

private:
    SQLType m_element_type { SQLType::Text };
};

using ValueTypeImpl = Variant<NullImpl, TextImpl, IntegerImpl, FloatImpl, BooleanImpl, TupleImpl, ArrayImpl>;

/**
 * A `Value` is an atomic piece of SQL data. A `Value` has a basic type
 * (Text/String, Integer, Float, etc). Richer types are implemented in higher
 * level layers, but the resulting data is stored in these `Value` objects.
 */
class Value {
public:
    Value(Value&) = default;
    Value(Value const&) = default;

    explicit Value(SQLType sql_type = SQLType::Null);

    template<typename... Ts>
    explicit Value(Variant<Ts...> impl)
        : m_impl(impl)
    {
    }

    enum SetImplementation {
        SetImplementationSingleton
    };

    template<typename I>
    Value(SetImplementation, I&& impl)
    {
        m_impl.set<I>(forward<I>(impl));
    }

    Value(SQLType, Value const&);
    Value(SQLType, String const&);
    Value(SQLType, char const*);
    Value(SQLType, int);
    Value(SQLType, double);
    Value(SQLType, bool);
    explicit Value(String const&);
    explicit Value(char const*);
    explicit Value(int);
    explicit Value(double);
    explicit Value(bool);

    ~Value() = default;

    [[nodiscard]] bool is_null() const;
    [[nodiscard]] SQLType type() const;
    [[nodiscard]] String type_name() const;
    [[nodiscard]] BaseTypeImpl downcast_to_basetype() const;

    template<typename Impl>
    Impl const& get_impl(Badge<Impl>) const { return m_impl.get<Impl>(); }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<u32> to_u32() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] Optional<Vector<Value>> to_vector() const;

    explicit operator String() const;
    explicit operator int() const;
    explicit operator u32() const;
    explicit operator double() const;
    explicit operator bool() const;

    void assign(Value const& other_value);
    void assign(String const& string_value);
    void assign(int const& int_value);
    void assign(double const& double_value);
    void assign(bool const& bool_value);
    void assign(Vector<Value> const& values);

    Value& operator=(Value const& other);

    Value& operator=(String const&);
    Value& operator=(char const*);
    Value& operator=(int);
    Value& operator=(u32);
    Value& operator=(double);
    Value& operator=(bool);
    Value& operator=(Vector<Value> const&);

    [[nodiscard]] size_t length() const;
    [[nodiscard]] u32 hash() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    void serialize_to(ByteBuffer&) const;
    void deserialize(ByteBuffer&, size_t&);

    [[nodiscard]] int compare(Value const&) const;
    bool operator==(Value const&) const;
    bool operator==(String const&) const;
    bool operator==(int) const;
    bool operator==(double) const;
    bool operator!=(Value const&) const;
    bool operator<(Value const&) const;
    bool operator<=(Value const&) const;
    bool operator>(Value const&) const;
    bool operator>=(Value const&) const;

    static Value const& null();
    static Value create_tuple(NonnullRefPtr<TupleDescriptor> const&);
    static Value create_array(SQLType element_type, Optional<size_t> const& max_size = {});
    static Value deserialize_from(ByteBuffer&, size_t&);

private:
    void setup(SQLType type);

    ValueTypeImpl m_impl {};
};

}
