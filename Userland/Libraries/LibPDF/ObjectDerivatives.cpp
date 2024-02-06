/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <LibPDF/CommonNames.h>
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

ByteString StringObject::to_byte_string(int) const
{
    if (is_binary())
        return ByteString::formatted("<{}>", encode_hex(string().bytes()).to_uppercase());
    return ByteString::formatted("({})", string());
}

ByteString NameObject::to_byte_string(int) const
{
    StringBuilder builder;
    builder.appendff("/{}", this->name());
    return builder.to_byte_string();
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

ByteString ArrayObject::to_byte_string(int indent) const
{
    StringBuilder builder;
    builder.append("[\n"sv);
    bool first = true;

    for (auto& element : elements()) {
        if (!first)
            builder.append("\n"sv);
        first = false;
        append_indent(builder, indent + 1);
        builder.appendff("{}", element.to_byte_string(indent));
    }

    builder.append('\n');
    append_indent(builder, indent);
    builder.append(']');
    return builder.to_byte_string();
}

ByteString DictObject::to_byte_string(int indent) const
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
        builder.appendff("{}", value.to_byte_string(indent + 1));
    }

    builder.append('\n');
    append_indent(builder, indent);
    builder.append(">>"sv);
    return builder.to_byte_string();
}

ByteString StreamObject::to_byte_string(int indent) const
{
    StringBuilder builder;
    builder.appendff("{}\n", dict()->to_byte_string(indent));
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

    if (dict()->contains(CommonNames::Subtype) && dict()->get_name(CommonNames::Subtype)->name() == "Image")
        is_mostly_text = false;

    if (is_mostly_text) {
        for (size_t i = 0; i < bytes().size(); ++i) {
            auto c = bytes()[i];
            if (c < 128) {
                bool next_is_newline = i + 1 < bytes().size() && bytes()[i + 1] == '\n';
                if (c == '\r' && !next_is_newline)
                    builder.append('\n');
                else
                    builder.append(c);
            } else {
                builder.appendff("\\{:03o}", c);
            }
        }
    } else {
        int const chars_per_line = 60;
        int const bytes_per_line = chars_per_line / 2;
        int const max_lines_to_print = 10;
        int const max_bytes_to_print = max_lines_to_print * bytes_per_line;
        auto string = encode_hex(bytes().trim(max_bytes_to_print));
        StringView view { string };
        while (view.length() > 60) {
            builder.appendff("{}\n", view.substring_view(0, chars_per_line));
            append_indent(builder, indent);
            view = view.substring_view(60);
        }
        builder.appendff("{}\n", view);

        if (bytes().size() > max_bytes_to_print)
            builder.appendff("... (and {} more bytes)\n", bytes().size() - max_bytes_to_print);
    }

    builder.append("endstream"sv);
    return builder.to_byte_string();
}

ByteString IndirectValue::to_byte_string(int indent) const
{
    StringBuilder builder;
    builder.appendff("{} {} obj\n", index(), generation_index());
    append_indent(builder, indent + 1);
    builder.append(value().to_byte_string(indent + 1));
    builder.append('\n');
    append_indent(builder, indent);
    builder.append("endobj"sv);
    return builder.to_byte_string();
}

}
