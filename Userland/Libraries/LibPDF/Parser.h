/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SourceLocation.h>
#include <AK/WeakPtr.h>
#include <LibPDF/Object.h>
#include <LibPDF/Operator.h>
#include <LibPDF/Reader.h>
#include <LibPDF/XRefTable.h>

namespace PDF {

template<typename T, typename... Args>
static NonnullRefPtr<T> make_object(Args... args)
requires(IsBaseOf<Object, T>)
{
    return adopt_ref(*new T(forward<Args>(args)...));
}

class Document;

class Parser {
public:
    static PDFErrorOr<Vector<Operator>> parse_operators(Document*, ReadonlyBytes);

    Parser(ReadonlyBytes);
    Parser(Document*, ReadonlyBytes);

    void set_document(WeakPtr<Document> const&);

    ByteString parse_comment();
    void consume_whitespace() { m_reader.consume_whitespace(); }

    void move_by(size_t count) { m_reader.move_by(count); }
    void move_to(size_t offset) { m_reader.move_to(offset); }

    enum class CanBeIndirectValue {
        No,
        Yes
    };

    PDFErrorOr<Value> parse_value(CanBeIndirectValue = CanBeIndirectValue::Yes);
    PDFErrorOr<Value> parse_possible_indirect_value_or_ref();
    PDFErrorOr<NonnullRefPtr<IndirectValue>> parse_indirect_value(u32 index, u32 generation);
    PDFErrorOr<NonnullRefPtr<IndirectValue>> parse_indirect_value();
    PDFErrorOr<Value> parse_number();
    PDFErrorOr<NonnullRefPtr<NameObject>> parse_name();
    PDFErrorOr<NonnullRefPtr<StringObject>> parse_string();
    PDFErrorOr<ByteString> parse_literal_string();
    PDFErrorOr<ByteString> parse_hex_string();
    PDFErrorOr<NonnullRefPtr<ArrayObject>> parse_array();
    PDFErrorOr<HashMap<DeprecatedFlyString, Value>> parse_dict_contents_until(char const*);
    PDFErrorOr<NonnullRefPtr<DictObject>> parse_dict();
    PDFErrorOr<void> unfilter_stream(NonnullRefPtr<StreamObject>);
    PDFErrorOr<NonnullRefPtr<StreamObject>> parse_stream(NonnullRefPtr<DictObject> dict);
    PDFErrorOr<Vector<Operator>> parse_operators();

    void set_filters_enabled(bool enabled)
    {
        m_enable_filters = enabled;
    }

    void set_encryption_enabled(bool enabled)
    {
        m_enable_encryption = enabled;
    }

    void push_reference(Reference const& ref) { m_current_reference_stack.append(ref); }
    void pop_reference() { m_current_reference_stack.take_last(); }

protected:
    PDFErrorOr<NonnullRefPtr<StreamObject>> parse_inline_image();

    Error error(
        ByteString const& message
#ifdef PDF_DEBUG
        ,
        SourceLocation loc = SourceLocation::current()
#endif
    ) const;

    Reader m_reader;
    WeakPtr<Document> m_document;
    Vector<Reference> m_current_reference_stack;
    bool m_enable_encryption { true };
    bool m_enable_filters { true };
};

};
