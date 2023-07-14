/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <LibPDF/Document.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<Object>> ArrayObject::get_object_at(Document* document, size_t index) const
{
    return document->resolve_to<Object>(at(index));
}

PDFErrorOr<NonnullRefPtr<Object>> DictObject::get_object(Document* document, DeprecatedFlyString const& key) const
{
    return document->resolve_to<Object>(get_value(key));
}

#define DEFINE_ACCESSORS(class_name, snake_name)                                                                                 \
    PDFErrorOr<NonnullRefPtr<class_name>> ArrayObject::get_##snake_name##_at(Document* document, size_t index) const             \
    {                                                                                                                            \
        if (index >= m_elements.size())                                                                                          \
            return Error { Error::Type::Internal, "Out of bounds array access" };                                                \
        return document->resolve_to<class_name>(m_elements[index]);                                                              \
    }                                                                                                                            \
                                                                                                                                 \
    NonnullRefPtr<class_name> ArrayObject::get_##snake_name##_at(size_t index) const                                             \
    {                                                                                                                            \
        VERIFY(index < m_elements.size());                                                                                       \
        return cast_to<class_name>(m_elements[index]);                                                                           \
    }                                                                                                                            \
                                                                                                                                 \
    PDFErrorOr<NonnullRefPtr<class_name>> DictObject::get_##snake_name(Document* document, DeprecatedFlyString const& key) const \
    {                                                                                                                            \
        return document->resolve_to<class_name>(get_value(key));                                                                 \
    }                                                                                                                            \
                                                                                                                                 \
    NonnullRefPtr<class_name> DictObject::get_##snake_name(DeprecatedFlyString const& key) const                                 \
    {                                                                                                                            \
        return cast_to<class_name>(get_value(key));                                                                              \
    }

ENUMERATE_OBJECT_TYPES(DEFINE_ACCESSORS)
#undef DEFINE_INDEXER

static void append_indent(StringBuilder& builder, int indent)
{
    for (int i = 0; i < indent; i++)
        builder.append("  "sv);
}

DeprecatedString StringObject::to_deprecated_string(int) const
{
    if (is_binary())
        return DeprecatedString::formatted("<{}>", encode_hex(string().bytes()).to_uppercase());
    return DeprecatedString::formatted("({})", string());
}

DeprecatedString NameObject::to_deprecated_string(int) const
{
    StringBuilder builder;
    builder.appendff("/{}", this->name());
    return builder.to_deprecated_string();
}

Vector<float> ArrayObject::float_elements() const
{
    Vector<float> values;
    values.ensure_capacity(m_elements.size());
    for (auto const& value : m_elements) {
        values.append(value.to_float());
    }
    return values;
}

DeprecatedString ArrayObject::to_deprecated_string(int indent) const
{
    StringBuilder builder;
    builder.append("[\n"sv);
    bool first = true;

    for (auto& element : elements()) {
        if (!first)
            builder.append("\n"sv);
        first = false;
        append_indent(builder, indent + 1);
        builder.appendff("{}", element.to_deprecated_string(indent));
    }

    builder.append('\n');
    append_indent(builder, indent);
    builder.append(']');
    return builder.to_deprecated_string();
}

DeprecatedString DictObject::to_deprecated_string(int indent) const
{
    StringBuilder builder;
    append_indent(builder, indent);
    builder.append("<<\n"sv);
    bool first = true;

    for (auto& [key, value] : map()) {
        if (!first)
            builder.append("\n"sv);
        first = false;
        append_indent(builder, indent + 1);
        builder.appendff("/{} ", key);
        builder.appendff("{}", value.to_deprecated_string(indent + 1));
    }

    builder.append('\n');
    append_indent(builder, indent);
    builder.append(">>"sv);
    return builder.to_deprecated_string();
}

DeprecatedString StreamObject::to_deprecated_string(int indent) const
{
    StringBuilder builder;
    builder.appendff("{}\n", dict()->to_deprecated_string(indent));
    builder.append("stream\n"sv);

    size_t ascii_count = 0;
    for (auto c : bytes()) {
        if (c < 128)
            ++ascii_count;
    }

    size_t percentage_ascii = 100;
    if (bytes().size())
        percentage_ascii = ascii_count * 100 / bytes().size();
    bool is_mostly_text = percentage_ascii > 95;

    if (is_mostly_text) {
        for (auto c : bytes()) {
            if (c < 128)
                builder.append(c);
            else
                builder.appendff("\\{:03o}", c);
        }
    } else {
        auto string = encode_hex(bytes());
        while (string.length() > 60) {
            builder.appendff("{}\n", string.substring(0, 60));
            append_indent(builder, indent);
            string = string.substring(60);
        }

        builder.appendff("{}\n", string);
    }

    builder.append("endstream"sv);
    return builder.to_deprecated_string();
}

DeprecatedString IndirectValue::to_deprecated_string(int indent) const
{
    StringBuilder builder;
    builder.appendff("{} {} obj\n", index(), generation_index());
    append_indent(builder, indent + 1);
    builder.append(value().to_deprecated_string(indent + 1));
    builder.append('\n');
    append_indent(builder, indent);
    builder.append("endobj"sv);
    return builder.to_deprecated_string();
}

}
