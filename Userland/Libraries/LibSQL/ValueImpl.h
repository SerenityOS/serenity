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
#include <LibSQL/Forward.h>
#include <LibSQL/Serializer.h>
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
    static void serialize(Serializer&) { }
    static void deserialize(Serializer&) { }
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

    void serialize(Serializer& serializer) const
    {
        serializer.serialize(value());
    }

    void deserialize(Serializer& serializer)
    {
        T value;
        serializer.deserialize_to(value);
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
    [[nodiscard]] Optional<bool> to_bool() const;
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
    [[nodiscard]] size_t length() const;
    [[nodiscard]] size_t size() const { return is_null() ? 0 : value().size(); }
    bool append(Value const&);
    bool append(BaseTypeImpl const& value);
    void serialize_values(Serializer&) const;
    void deserialize_values(Serializer&);

protected:
    explicit ContainerValueImpl(SQLType sql_type)
        : Impl(sql_type)
    {
    }
};

class TupleImpl : public ContainerValueImpl {
public:
    explicit TupleImpl(NonnullRefPtr<TupleDescriptor> const& descriptor)
        : ContainerValueImpl(SQLType::Tuple)
        , m_descriptor(descriptor)
    {
    }

    explicit TupleImpl()
        : ContainerValueImpl(SQLType::Tuple)
    {
    }

    void assign(Value const&);
    [[nodiscard]] size_t length() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    [[nodiscard]] int compare(Value const& other) const;
    [[nodiscard]] Optional<bool> to_bool() const;

    virtual bool validate_before_assignment(Vector<Value> const&) override;
    virtual bool validate(BaseTypeImpl const&) override;
    virtual bool validate_after_assignment() override;
    void serialize(Serializer&) const;
    void deserialize(Serializer&);

private:
    void infer_descriptor();
    void extend_descriptor(Value const&);
    RefPtr<TupleDescriptor> m_descriptor;
    bool m_descriptor_inferred { false };
};

class ArrayImpl : public ContainerValueImpl {
public:
    explicit ArrayImpl(SQLType element_type, Optional<size_t> const& max_size = {})
        : ContainerValueImpl(SQLType::Array)
        , m_element_type(element_type)
        , m_max_size(max_size)
    {
    }

    explicit ArrayImpl()
        : ContainerValueImpl(SQLType::Array)
        , m_element_type(SQLType::Null)
    {
    }

    void assign(Value const&);
    [[nodiscard]] size_t length() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    [[nodiscard]] int compare(Value const& other) const;
    void serialize(Serializer&) const;
    void deserialize(Serializer&);
    virtual bool validate(BaseTypeImpl const&) override;

private:
    SQLType m_element_type { SQLType::Text };
    Optional<size_t> m_max_size {};
};

using ValueTypeImpl = Variant<NullImpl, TextImpl, IntegerImpl, FloatImpl, BooleanImpl, TupleImpl, ArrayImpl>;

}
