/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Serializer.h>
#include <LibSQL/Value.h>
#include <math.h>
#include <string.h>

namespace SQL {

Value::Value(SQLType sql_type)
{
    setup(sql_type);
}

void Value::setup(SQLType type)
{
    switch (type) {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) \
    case SQLType::type:                                        \
        m_impl.set<type##Impl>(type##Impl());                  \
        break;
        ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
    default:
        VERIFY_NOT_REACHED();
    }
}

Value::Value(SQLType sql_type, Value const& value)
    : Value(sql_type)
{
    assign(value);
}

Value::Value(SQLType sql_type, String const& string)
    : Value(sql_type)
{
    assign(string);
}

Value::Value(SQLType sql_type, char const* string)
    : Value(sql_type)
{
    assign(String(string));
}

Value::Value(SQLType sql_type, int integer)
    : Value(sql_type)
{
    assign(integer);
}

Value::Value(SQLType sql_type, double dbl)
    : Value(sql_type)
{
    assign(dbl);
}

Value::Value(SQLType sql_type, bool boolean)
    : Value(sql_type)
{
    assign(boolean);
}

Value::Value(String const& string)
    : Value(SQLType::Text)
{
    assign(string);
}

Value::Value(char const* string)
    : Value(SQLType::Text)
{
    assign(String(string));
}

Value::Value(int integer)
    : Value(SQLType::Integer)
{
    assign(integer);
}

Value::Value(u32 unsigned_integer)
    : Value(SQLType::Integer)
{
    assign(unsigned_integer);
}

Value::Value(double dbl)
    : Value(SQLType::Float)
{
    assign(dbl);
}

Value::Value(bool boolean)
    : Value(SQLType::Boolean)
{
    assign(boolean);
}

Value Value::create_tuple(NonnullRefPtr<TupleDescriptor> const& tuple_descriptor)
{
    return Value(Value::SetImplementationSingleton, TupleImpl(tuple_descriptor));
}

Value Value::create_array(SQLType element_type, Optional<size_t> const& max_size)
{
    return Value(Value::SetImplementationSingleton, ArrayImpl(element_type, max_size));
}

Value const& Value::null()
{
    static Value s_null(SQLType::Null);
    return s_null;
}

bool Value::is_null() const
{
    return m_impl.visit([&](auto& impl) { return impl.is_null(); });
}

SQLType Value::type() const
{
    return m_impl.visit([&](auto& impl) { return impl.type(); });
}

String Value::type_name() const
{
    return m_impl.visit([&](auto& impl) { return impl.type_name(); });
}

BaseTypeImpl Value::downcast_to_basetype() const
{
    return m_impl.downcast<NullImpl, TextImpl, IntegerImpl, FloatImpl, BooleanImpl>();
}

String Value::to_string() const
{
    if (is_null())
        return "(null)";
    return m_impl.visit([&](auto& impl) { return impl.to_string(); });
}

Optional<int> Value::to_int() const
{
    if (is_null())
        return {};
    return m_impl.visit([&](auto& impl) { return impl.to_int(); });
}

Optional<u32> Value::to_u32() const
{
    if (is_null())
        return {};
    auto ret = to_int();
    if (ret.has_value())
        return static_cast<u32>(ret.value());
    return {};
}

Optional<double> Value::to_double() const
{
    if (is_null())
        return {};
    return m_impl.visit([&](auto& impl) { return impl.to_double(); });
}

Optional<bool> Value::to_bool() const
{
    if (is_null())
        return {};
    return m_impl.visit([&](auto& impl) { return impl.to_bool(); });
}

Optional<Vector<Value>> Value::to_vector() const
{
    if (is_null())
        return {};
    Vector<Value> vector;
    if (m_impl.visit([&](auto& impl) { return impl.to_vector(vector); }))
        return vector;
    else
        return {};
}

Value::operator String() const
{
    return to_string();
}

Value::operator int() const
{
    auto i = to_int();
    VERIFY(i.has_value());
    return i.value();
}

Value::operator u32() const
{
    auto i = to_u32();
    VERIFY(i.has_value());
    return i.value();
}

Value::operator double() const
{
    auto d = to_double();
    VERIFY(d.has_value());
    return d.value();
}

Value::operator bool() const
{
    auto b = to_bool();
    VERIFY(b.has_value());
    return b.value();
}

void Value::assign(Value const& other_value)
{
    m_impl.visit([&](auto& impl) { impl.assign(other_value); });
}

void Value::assign(String const& string_value)
{
    m_impl.visit([&](auto& impl) { impl.assign_string(string_value); });
}

void Value::assign(int int_value)
{
    m_impl.visit([&](auto& impl) { impl.assign_int(int_value); });
}

void Value::assign(u32 unsigned_int_value)
{
    m_impl.visit([&](auto& impl) { impl.assign_int(unsigned_int_value); });
}

void Value::assign(double double_value)
{
    m_impl.visit([&](auto& impl) { impl.assign_double(double_value); });
}

void Value::assign(bool bool_value)
{
    m_impl.visit([&](auto& impl) { impl.assign_bool(bool_value); });
}

void Value::assign(Vector<Value> const& values)
{
    m_impl.visit([&](auto& impl) { impl.assign_vector(values); });
}

Value& Value::operator=(Value const& other)
{
    if (this != &other) {
        if (other.is_null()) {
            assign(null());
        } else if (is_null()) {
            assign(other);
        } else {
            VERIFY(can_cast(other));
            assign(other);
        }
    }
    return (*this);
}

Value& Value::operator=(String const& value)
{
    assign(value);
    return (*this);
}

Value& Value::operator=(char const* value)
{
    assign(String(value));
    return (*this);
}

Value& Value::operator=(int value)
{
    assign(value);
    return (*this);
}

Value& Value::operator=(u32 value)
{
    assign(static_cast<int>(value));
    return (*this);
}

Value& Value::operator=(double value)
{
    assign(value);
    return (*this);
}

Value& Value::operator=(bool value)
{
    assign(value);
    return (*this);
}

Value& Value::operator=(Vector<Value> const& vector)
{
    assign(vector);
    return (*this);
}

size_t Value::length() const
{
    return m_impl.visit([&](auto& impl) { return impl.length(); });
}

u32 Value::hash() const
{
    return (is_null()) ? 0u : m_impl.visit([&](auto& impl) { return impl.hash(); });
}

bool Value::can_cast(Value const& other_value) const
{
    if (type() == other_value.type())
        return true;
    return m_impl.visit([&](auto& impl) { return impl.can_cast(other_value); });
}

int Value::compare(Value const& other) const
{
    if (is_null())
        return -1;
    if (other.is_null())
        return 1;
    return m_impl.visit([&](auto& impl) { return impl.compare(other); });
}

bool Value::operator==(Value const& other) const
{
    return compare(other) == 0;
}

bool Value::operator==(String const& string_value) const
{
    return to_string() == string_value;
}

bool Value::operator==(int int_value) const
{
    auto i = to_int();
    if (!i.has_value())
        return false;
    return i.value() == int_value;
}

bool Value::operator==(double double_value) const
{
    auto d = to_double();
    if (!d.has_value())
        return false;
    return d.value() == double_value;
}

bool Value::operator!=(Value const& other) const
{
    return compare(other) != 0;
}

bool Value::operator<(Value const& other) const
{
    return compare(other) < 0;
}

bool Value::operator<=(Value const& other) const
{
    return compare(other) <= 0;
}

bool Value::operator>(Value const& other) const
{
    return compare(other) > 0;
}

bool Value::operator>=(Value const& other) const
{
    return compare(other) >= 0;
}

Value Value::add(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() + other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() + (double)int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    if (auto int_maybe = to_double(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(other_double_maybe.value() + (double)int_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() + other_int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

Value Value::subtract(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() - other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() - (double)int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    if (auto int_maybe = to_double(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() - other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() - other_int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

Value Value::multiply(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() * other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() * (double)int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    if (auto int_maybe = to_double(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() * other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() * other_int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

Value Value::divide(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() / other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() / (double)int_maybe.value());
        VERIFY_NOT_REACHED();
    }

    if (auto int_maybe = to_double(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() / other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() / other_int_maybe.value());
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

Value Value::modulo(Value const& other) const
{
    auto int_maybe_1 = to_int();
    auto int_maybe_2 = other.to_int();
    if (!int_maybe_1.has_value() || !int_maybe_2.has_value()) {
        // TODO Error handling
        VERIFY_NOT_REACHED();
    }
    return Value(int_maybe_1.value() % int_maybe_2.value());
}

Value Value::shift_left(Value const& other) const
{
    auto u32_maybe = to_u32();
    auto num_bytes_maybe = other.to_int();
    if (!u32_maybe.has_value() || !num_bytes_maybe.has_value()) {
        // TODO Error handling
        VERIFY_NOT_REACHED();
    }
    return Value(u32_maybe.value() << num_bytes_maybe.value());
}

Value Value::shift_right(Value const& other) const
{
    auto u32_maybe = to_u32();
    auto num_bytes_maybe = other.to_int();
    if (!u32_maybe.has_value() || !num_bytes_maybe.has_value()) {
        // TODO Error handling
        VERIFY_NOT_REACHED();
    }
    return Value(u32_maybe.value() >> num_bytes_maybe.value());
}

Value Value::bitwise_or(Value const& other) const
{
    auto u32_maybe_1 = to_u32();
    auto u32_maybe_2 = other.to_u32();
    if (!u32_maybe_1.has_value() || !u32_maybe_2.has_value()) {
        // TODO Error handling
        VERIFY_NOT_REACHED();
    }
    return Value(u32_maybe_1.value() | u32_maybe_2.value());
}

Value Value::bitwise_and(Value const& other) const
{
    auto u32_maybe_1 = to_u32();
    auto u32_maybe_2 = other.to_u32();
    if (!u32_maybe_1.has_value() || !u32_maybe_2.has_value()) {
        // TODO Error handling
        VERIFY_NOT_REACHED();
    }
    return Value(u32_maybe_1.value() & u32_maybe_2.value());
}

void Value::serialize(Serializer& serializer) const
{
    u8 type_flags = (u8)type();
    if (is_null())
        type_flags |= (u8)SQLType::Null;
    serializer.serialize<u8>(type_flags);
    if (!is_null())
        m_impl.visit([&](auto& impl) { serializer.serialize(impl); });
}

void Value::deserialize(Serializer& serializer)
{
    auto type_flags = serializer.deserialize<u8>();
    bool is_null = false;
    if ((type_flags & (u8)SQLType::Null) && (type_flags != (u8)SQLType::Null)) {
        type_flags &= ~((u8)SQLType::Null);
        is_null = true;
    }
    auto type = (SQLType)type_flags;
    VERIFY(!is_null || (type != SQLType::Tuple && type != SQLType::Array));
    setup(type);
    if (!is_null) {
        m_impl.visit([&](auto& impl) { impl.deserialize(serializer); });
    }
}

bool NullImpl::can_cast(Value const& value)
{
    return value.is_null();
}

int NullImpl::compare(Value const& other)
{
    return other.type() == SQLType::Null;
}

String TextImpl::to_string() const
{
    return value();
}

Optional<int> TextImpl::to_int() const
{
    if (!m_value.has_value())
        return {};
    return value().to_int();
}

Optional<double> TextImpl::to_double() const
{
    if (!m_value.has_value())
        return {};
    char* end_ptr;
    double ret = strtod(value().characters(), &end_ptr);
    if (end_ptr == value().characters()) {
        return {};
    }
    return ret;
}

Optional<bool> TextImpl::to_bool() const
{
    if (!m_value.has_value())
        return {};
    if (value().equals_ignoring_case("true") || value().equals_ignoring_case("t"))
        return true;
    if (value().equals_ignoring_case("false") || value().equals_ignoring_case("f"))
        return false;
    return {};
}

void TextImpl::assign(Value const& other_value)
{
    if (other_value.type() == SQLType::Null) {
        m_value = {};
    } else {
        m_value = other_value.to_string();
    }
}

void TextImpl::assign_string(String const& string_value)
{
    m_value = string_value;
}

void TextImpl::assign_int(int int_value)
{
    m_value = String::number(int_value);
}

void TextImpl::assign_double(double double_value)
{
    m_value = String::number(double_value);
}

void TextImpl::assign_bool(bool bool_value)
{
    m_value = (bool_value) ? "true" : "false";
}

size_t TextImpl::length() const
{
    return (is_null()) ? 0 : sizeof(u32) + value().length();
}

int TextImpl::compare(Value const& other) const
{
    if (is_null())
        return -1;
    auto s1 = value();
    auto s2 = other.to_string();
    if (s1 == s2)
        return 0;
    return (s1 < s2) ? -1 : 1;
}

u32 TextImpl::hash() const
{
    return value().hash();
}

String IntegerImpl::to_string() const
{
    return String::formatted("{}", value());
}

Optional<int> IntegerImpl::to_int() const
{
    return value();
}

Optional<double> IntegerImpl::to_double() const
{
    return static_cast<double>(value());
}

Optional<bool> IntegerImpl::to_bool() const
{
    return value() != 0;
}

void IntegerImpl::assign(Value const& other_value)
{
    auto i = other_value.to_int();
    if (!i.has_value())
        m_value = {};
    else
        m_value = i.value();
}

void IntegerImpl::assign_string(String const& string_value)
{
    auto i = string_value.to_int();
    if (!i.has_value())
        m_value = {};
    else
        m_value = i.value();
}

void IntegerImpl::assign_int(int int_value)
{
    m_value = int_value;
}

void IntegerImpl::assign_double(double double_value)
{
    m_value = static_cast<int>(round(double_value));
}

void IntegerImpl::assign_bool(bool bool_value)
{
    m_value = (bool_value) ? 1 : 0;
}

bool IntegerImpl::can_cast(Value const& other_value)
{
    return other_value.to_int().has_value();
}

int IntegerImpl::compare(Value const& other) const
{
    auto casted = other.to_int();
    if (!casted.has_value()) {
        return 1;
    }
    return value() - casted.value();
}

u32 IntegerImpl::hash() const
{
    return int_hash(value());
}

String FloatImpl::to_string() const
{
    return String::formatted("{}", value());
}

Optional<int> FloatImpl::to_int() const
{
    return static_cast<int>(round(value()));
}

Optional<double> FloatImpl::to_double() const
{
    return value();
}

void FloatImpl::assign(Value const& other_value)
{
    auto i = other_value.to_double();
    if (!i.has_value())
        m_value = {};
    else
        m_value = i.value();
}

void FloatImpl::assign_string(String const& string_value)
{
    char* end_ptr;
    auto dbl = strtod(string_value.characters(), &end_ptr);
    if (end_ptr == string_value.characters())
        m_value = {};
    else
        m_value = dbl;
}

void FloatImpl::assign_int(int int_value)
{
    m_value = int_value;
}

void FloatImpl::assign_double(double double_value)
{
    m_value = double_value;
}

bool FloatImpl::can_cast(Value const& other_value)
{
    return other_value.to_double().has_value();
}

int FloatImpl::compare(Value const& other) const
{
    auto casted = other.to_double();
    if (!casted.has_value()) {
        return 1;
    }
    auto diff = value() - casted.value();
    return (diff < NumericLimits<double>::epsilon()) ? 0 : ((diff > 0) ? 1 : -1);
}

String BooleanImpl::to_string() const
{
    return (value()) ? "true" : "false";
}

Optional<int> BooleanImpl::to_int() const
{
    return (value()) ? 1 : 0;
}

Optional<double> BooleanImpl::to_double()
{
    return {};
}

Optional<bool> BooleanImpl::to_bool() const
{
    return value();
}

void BooleanImpl::assign(Value const& other_value)
{
    auto b = other_value.to_bool();
    if (!b.has_value())
        m_value = {};
    else
        m_value = b.value();
}

void BooleanImpl::assign_string(String const& string_value)
{
    return assign(Value(string_value));
}

void BooleanImpl::assign_int(int int_value)
{
    m_value = (int_value != 0);
}

void BooleanImpl::assign_double(double)
{
    m_value = {};
}

void BooleanImpl::assign_bool(bool bool_value)
{
    m_value = bool_value;
}

bool BooleanImpl::can_cast(Value const& other_value)
{
    return other_value.to_bool().has_value();
}

int BooleanImpl::compare(Value const& other) const
{
    auto casted = other.to_bool();
    if (!casted.has_value()) {
        return 1;
    }
    return value() ^ casted.value(); // xor - zero if both true or both false, 1 otherwise.
}

u32 BooleanImpl::hash() const
{
    return int_hash(value());
}

void ContainerValueImpl::assign_vector(Vector<Value> const& vector_values)
{
    if (!validate_before_assignment(vector_values)) {
        m_value = {};
        return;
    }
    m_value = Vector<BaseTypeImpl>();
    for (auto& value : vector_values) {
        if (!append(value)) {
            m_value = {};
            return;
        }
    }
    if (!validate_after_assignment())
        m_value = {};
}

bool ContainerValueImpl::to_vector(Vector<Value>& vector) const
{
    vector.clear();
    for (auto& value : value()) {
        vector.empend(Value(value));
    }
    return true;
}

Vector<String> ContainerValueImpl::to_string_vector() const
{
    Vector<String> ret;
    for (auto& value : value()) {
        ret.append(Value(value).to_string());
    }
    return ret;
}

String ContainerValueImpl::to_string() const
{
    StringBuilder builder;
    builder.append("(");
    StringBuilder joined;
    joined.join(", ", to_string_vector());
    builder.append(joined.string_view());
    builder.append(")");
    return builder.build();
}

u32 ContainerValueImpl::hash() const
{
    u32 ret = 0u;
    for (auto& value : value()) {
        Value v(value);
        // This is an extension of the pair_int_hash function from AK/HashFunctions.h:
        if (!ret)
            ret = v.hash();
        else
            ret = int_hash((ret * 209) ^ (v.hash() * 413));
    }
    return ret;
}

bool ContainerValueImpl::append(Value const& value)
{
    if (value.type() == SQLType::Tuple || value.type() == SQLType::Array)
        return false;
    return append(value.downcast_to_basetype());
}

bool ContainerValueImpl::append(BaseTypeImpl const& impl)
{
    if (!validate(impl))
        return false;
    m_value.value().empend(impl);
    return true;
}

void ContainerValueImpl::serialize_values(Serializer& serializer) const
{
    serializer.serialize((u32)size());
    for (auto& impl : value()) {
        serializer.serialize<Value>(Value(impl));
    }
}

void ContainerValueImpl::deserialize_values(Serializer& serializer)
{
    auto sz = serializer.deserialize<u32>();
    m_value = Vector<BaseTypeImpl>();
    for (auto ix = 0u; ix < sz; ix++) {
        append(serializer.deserialize<Value>());
    }
}

size_t ContainerValueImpl::length() const
{
    size_t len = sizeof(u32);
    for (auto& impl : value()) {
        len += Value(impl).length();
    }
    return len;
}

void TupleImpl::assign(Value const& other)
{
    if (other.type() != SQLType::Tuple) {
        m_value = {};
        return;
    }
    auto& other_impl = other.get_impl<TupleImpl>({});
    auto other_descriptor = other_impl.m_descriptor;
    if (m_descriptor && other_descriptor && m_descriptor->compare_ignoring_names(*other_descriptor)) {
        m_value = {};
        return;
    }
    assign_vector(other.to_vector().value());
}

size_t TupleImpl::length() const
{
    return m_descriptor->length() + ContainerValueImpl::length();
}

bool TupleImpl::can_cast(Value const& other_value) const
{
    if (other_value.type() != SQLType::Tuple)
        return false;
    return (m_descriptor == other_value.get_impl<TupleImpl>({}).m_descriptor);
}

int TupleImpl::compare(Value const& other) const
{
    if (other.type() != SQLType::Tuple)
        return 1;

    auto& other_impl = other.get_impl<TupleImpl>({});
    if (m_descriptor && other_impl.m_descriptor && m_descriptor->compare_ignoring_names(*other_impl.m_descriptor))
        return 1;

    auto other_values = other_impl.value();
    if (size() != other_impl.size())
        return (int)value().size() - (int)other_impl.size();
    for (auto ix = 0u; ix < value().size(); ix++) {
        auto ret = Value(value()[ix]).compare(Value(other_impl.value()[ix]));
        if (ret != 0) {
            if (m_descriptor && (ix < m_descriptor->size()) && (*m_descriptor)[ix].order == Order::Descending)
                ret = -ret;
            return ret;
        }
    }
    return 0;
}

void TupleImpl::serialize(Serializer& serializer) const
{
    serializer.serialize<TupleDescriptor>(*m_descriptor);
    serialize_values(serializer);
}

void TupleImpl::deserialize(Serializer& serializer)
{
    m_descriptor = serializer.adopt_and_deserialize<TupleDescriptor>();
    deserialize_values(serializer);
}

void TupleImpl::infer_descriptor()
{
    if (!m_descriptor) {
        m_descriptor = adopt_ref(*new TupleDescriptor);
        m_descriptor_inferred = true;
    }
}

void TupleImpl::extend_descriptor(Value const& value)
{
    VERIFY(m_descriptor_inferred);
    m_descriptor->empend("", "", "", value.type(), Order::Ascending);
}

bool TupleImpl::validate_before_assignment(Vector<Value> const& values)
{
    if (m_descriptor_inferred)
        m_descriptor = nullptr;
    if (!m_descriptor) {
        infer_descriptor();
        if (values.size() > m_descriptor->size()) {
            for (auto ix = m_descriptor->size(); ix < values.size(); ix++) {
                extend_descriptor(values[ix]);
            }
        }
    }
    return true;
}

bool TupleImpl::validate(BaseTypeImpl const& value)
{
    if (!m_descriptor)
        infer_descriptor();
    if (m_descriptor_inferred && (this->value().size() == m_descriptor->size()))
        extend_descriptor(Value(value));
    if (m_descriptor->size() == this->value().size())
        return false;
    auto required_type = (*m_descriptor)[this->value().size()].type;
    return Value(value).type() == required_type;
}

bool TupleImpl::validate_after_assignment()
{
    for (auto ix = value().size(); ix < m_descriptor->size(); ++ix) {
        auto required_type = (*m_descriptor)[ix].type;
        append(Value(required_type));
    }
    return true;
}

void ArrayImpl::assign(Value const& other)
{
    if (other.type() != SQLType::Array) {
        m_value = {};
        return;
    }
    auto& other_impl = other.get_impl<ArrayImpl>({});
    if (m_max_size != other_impl.m_max_size || m_element_type != other_impl.m_element_type) {
        m_value = {};
        return;
    }
    assign_vector(other.to_vector().value());
}

size_t ArrayImpl::length() const
{
    return sizeof(u8) + sizeof(u32) + ContainerValueImpl::length();
}

bool ArrayImpl::can_cast(Value const& other_value) const
{
    if (other_value.type() != SQLType::Array)
        return false;
    auto& other_impl = other_value.get_impl<ArrayImpl>({});
    return (m_max_size != other_impl.m_max_size || m_element_type != other_impl.m_element_type);
}

int ArrayImpl::compare(Value const& other) const
{
    if (other.type() != SQLType::Array)
        return 1;
    auto other_impl = other.get_impl<ArrayImpl>({});
    if (other_impl.m_element_type != m_element_type)
        return 1;
    if (other_impl.m_max_size.has_value() && m_max_size.has_value() && other_impl.m_max_size != m_max_size)
        return (int)m_max_size.value() - (int)other_impl.m_max_size.value();
    if (size() != other_impl.size())
        return (int)size() - (int)other_impl.size();
    for (auto ix = 0u; ix < size(); ix++) {
        auto ret = Value(value()[ix]).compare(Value(other_impl.value()[ix]));
        if (ret != 0) {
            return ret;
        }
    }
    return 0;
}

void ArrayImpl::serialize(Serializer& serializer) const
{
    serializer.serialize((u8)m_element_type);
    if (m_max_size.has_value())
        serializer.serialize((u32)m_max_size.value());
    else
        serializer.serialize((u32)0);
    serialize_values(serializer);
}

void ArrayImpl::deserialize(Serializer& serializer)
{
    m_element_type = (SQLType)serializer.deserialize<u8>();
    auto max_sz = serializer.deserialize<u32>();
    if (max_sz)
        m_max_size = max_sz;
    else
        m_max_size = {};
    deserialize_values(serializer);
}

bool ArrayImpl::validate(BaseTypeImpl const& impl)
{
    if (m_max_size.has_value() && (size() >= m_max_size.value()))
        return false;
    return Value(impl).type() == m_element_type;
}

}
