/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/GenericLexer.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Reader {

enum class ParserBehavior : u32 {
    None = 0,
    ReadHeaders = 1,
    AllowNewlinesInFields = ReadHeaders << 1,
    TrimLeadingFieldSpaces = ReadHeaders << 2,
    TrimTrailingFieldSpaces = ReadHeaders << 3,
    QuoteOnlyInFieldStart = ReadHeaders << 4,
    Lenient = ReadHeaders << 5, // This is the typical "spreadsheet import" behavior
                                // Currently, it:
                                // - fills in missing fields with empty values
                                // - updates previous rows with extra columns
};

ParserBehavior operator&(ParserBehavior left, ParserBehavior right);
ParserBehavior operator|(ParserBehavior left, ParserBehavior right);

struct ParserTraits {
    ByteString separator;
    ByteString quote { "\"" };
    enum QuoteEscape {
        Repeat,
        Backslash,
    } quote_escape { Repeat };
};

#define ENUMERATE_READ_ERRORS()                                                   \
    E(None, "No errors")                                                          \
    E(NonConformingColumnCount, "Header count does not match given column count") \
    E(QuoteFailure, "Quoting failure")                                            \
    E(InternalError, "Internal error")                                            \
    E(DataPastLogicalEnd, "Extra data past the logical end of the rows")

enum class ReadError {
#define E(name, _) name,
    ENUMERATE_READ_ERRORS()
#undef E
};

constexpr ParserBehavior default_behaviors()
{
    return ParserBehavior::QuoteOnlyInFieldStart;
}

class XSV {
public:
    XSV(StringView source, ParserTraits traits, ParserBehavior behaviors = default_behaviors())
        : m_source(source)
        , m_lexer(m_source)
        , m_traits(traits)
        , m_behaviors(behaviors)
    {
        parse_preview();
    }

    virtual ~XSV() = default;

    void parse();
    bool has_error() const { return m_error != ReadError::None; }
    ReadError error() const { return m_error; }
    ByteString error_string() const
    {
        switch (m_error) {
#define E(x, y)        \
    case ReadError::x: \
        return y;

            ENUMERATE_READ_ERRORS();
#undef E
        }
        VERIFY_NOT_REACHED();
    }

    size_t size() const { return m_rows.size(); }
    Vector<ByteString> headers() const;
    [[nodiscard]] bool has_explicit_headers() const { return (static_cast<u32>(m_behaviors) & static_cast<u32>(ParserBehavior::ReadHeaders)) != 0; }

    class Row {
    public:
        explicit Row(XSV& xsv, size_t index)
            : m_xsv(xsv)
            , m_index(index)
        {
        }

        StringView operator[](StringView name) const;
        StringView operator[](size_t column) const;

        template<typename T>
        StringView at(T column) const { return this->operator[](column); }

        size_t index() const { return m_index; }
        size_t size() const { return m_xsv.headers().size(); }

        using ConstIterator = AK::SimpleIterator<Row const, StringView const>;
        using Iterator = AK::SimpleIterator<Row, StringView>;

        constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
        constexpr Iterator begin() { return Iterator::begin(*this); }

        constexpr ConstIterator end() const { return ConstIterator::end(*this); }
        constexpr Iterator end() { return Iterator::end(*this); }

    private:
        XSV& m_xsv;
        size_t m_index { 0 };
    };

    template<bool const_>
    class RowIterator {
    public:
        explicit RowIterator(const XSV& xsv, size_t init_index = 0)
        requires(const_)
            : m_xsv(const_cast<XSV&>(xsv))
            , m_index(init_index)
        {
        }

        explicit RowIterator(XSV& xsv, size_t init_index = 0)
        requires(!const_)
            : m_xsv(xsv)
            , m_index(init_index)
        {
        }

        Row operator*() const { return Row { m_xsv, m_index }; }
        Row operator*()
        requires(!const_)
        {
            return Row { m_xsv, m_index };
        }

        RowIterator& operator++()
        {
            ++m_index;
            return *this;
        }

        bool is_end() const { return m_index == m_xsv.m_rows.size(); }
        bool operator==(RowIterator const& other) const
        {
            return m_index == other.m_index && &m_xsv == &other.m_xsv;
        }
        bool operator==(RowIterator<!const_> const& other) const
        {
            return m_index == other.m_index && &m_xsv == &other.m_xsv;
        }

        constexpr size_t index() const { return m_index; }

    private:
        XSV& m_xsv;
        size_t m_index { 0 };
    };

    Row const operator[](size_t index) const;
    Row operator[](size_t index);

    Row at(size_t index) const;

    auto begin() { return RowIterator<false>(*this); }
    auto end() { return RowIterator<false>(*this, m_rows.size()); }

    auto begin() const { return RowIterator<true>(*this); }
    auto end() const { return RowIterator<true>(*this, m_rows.size()); }

    using ConstIterator = RowIterator<true>;
    using Iterator = RowIterator<false>;

private:
    struct Field {
        StringView as_string_view;
        ByteString as_string; // This member only used if the parser couldn't use the original source verbatim.
        bool is_string_view { true };

        bool operator==(StringView other) const
        {
            if (is_string_view)
                return other == as_string_view;
            return as_string == other;
        }
    };
    void set_error(ReadError error);
    void parse_preview();
    void read_headers();
    void reset()
    {
        m_lexer = GenericLexer { m_source };
        m_rows.clear();
        m_names.clear();
        m_error = ReadError::None;
    }
    Vector<Field> read_row(bool header_row = false);
    Field read_one_field();
    Field read_one_quoted_field();
    Field read_one_unquoted_field();

    StringView m_source;
    GenericLexer m_lexer;
    ParserTraits m_traits;
    ParserBehavior m_behaviors;
    Vector<Field> m_names;
    Vector<Vector<Field>> m_rows;
    ReadError m_error { ReadError::None };
};

}
