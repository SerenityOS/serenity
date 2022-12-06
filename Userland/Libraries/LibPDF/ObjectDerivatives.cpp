/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <LibPDF/Document.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<Object>> DictObject::get_object(Document* document, FlyString const& key) const
{
    return document->resolve_to<Object>(get_value(key));
}

#define DEFINE_ACCESSORS(class_name, snake_name)                                                                       \
    PDFErrorOr<NonnullRefPtr<class_name>> ArrayObject::get_##snake_name##_at(Document* document, size_t index) const   \
    {                                                                                                                  \
        if (index >= m_elements.size())                                                                                \
            return Error { Error::Type::Internal, "Out of bounds array access" };                                      \
        return document->resolve_to<class_name>(m_elements[index]);                                                    \
    }                                                                                                                  \
                                                                                                                       \
    PDFErrorOr<NonnullRefPtr<class_name>> DictObject::get_##snake_name(Document* document, FlyString const& key) const \
    {                                                                                                                  \
        return document->resolve_to<class_name>(get(key).value());                                                     \
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

DeprecatedString ArrayObject::to_deprecated_string(int indent) const
{
    StringBuilder builder;
    builder.append("[\n"sv);
    bool first = true;

    for (auto& element : elements()) {
        if (!first)
            builder.append(",\n"sv);
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
    builder.append("<<\n"sv);
    bool first = true;

    for (auto& [key, value] : map()) {
        if (!first)
            builder.append(",\n"sv);
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
    builder.append("stream\n"sv);
    append_indent(builder, indent);
    builder.appendff("{}\n", dict()->to_deprecated_string(indent + 1));
    append_indent(builder, indent + 1);

    auto string = encode_hex(bytes());
    while (true) {
        if (string.length() > 60) {
            builder.appendff("{}\n", string.substring(0, 60));
            append_indent(builder, indent);
            string = string.substring(60);
            continue;
        }

        builder.appendff("{}\n", string);
        break;
    }

    append_indent(builder, indent);
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
