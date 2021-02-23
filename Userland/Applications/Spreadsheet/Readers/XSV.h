/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Reader {

enum class ParserBehaviour : u32 {
    None = 0,
    ReadHeaders = 1,
    AllowNewlinesInFields = ReadHeaders << 1,
    TrimLeadingFieldSpaces = ReadHeaders << 2,
    TrimTrailingFieldSpaces = ReadHeaders << 3,
    QuoteOnlyInFieldStart = ReadHeaders << 4,
};

ParserBehaviour operator&(ParserBehaviour left, ParserBehaviour right);
ParserBehaviour operator|(ParserBehaviour left, ParserBehaviour right);

struct ParserTraits {
    String separator;
    String quote { "\"" };
    enum {
        Repeat,
        Backslash,
    } quote_escape { Repeat };
};

#define ENUMERATE_READ_ERRORS()                                                   \
    E(None, "No errors")                                                          \
    E(NonConformingColumnCount, "Header count does not match given column count") \
    E(QuoteFailure, "Quoting failure")                                            \
    E(InternalError, "Internal error")                                            \
    E(DataPastLogicalEnd, "Exrta data past the logical end of the rows")

enum class ReadError {
#define E(name, _) name,
    ENUMERATE_READ_ERRORS()
#undef E
};

inline constexpr ParserBehaviour default_behaviours()
{
    return ParserBehaviour::QuoteOnlyInFieldStart;
}

class XSV {
public:
    XSV(StringView source, const ParserTraits& traits, ParserBehaviour behaviours = default_behaviours())
        : m_source(source)
        , m_lexer(m_source)
        , m_traits(traits)
        , m_behaviours(behaviours)
    {
        parse();
    }

    virtual ~XSV() { }

    bool has_error() const { return m_error != ReadError::None; }
    ReadError error() const { return m_error; }
    String error_string() const
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
    Vector<String> headers() const;

    class Row {
    public:
        explicit Row(XSV& xsv, size_t index)
            : m_xsv(xsv)
            , m_index(index)
        {
        }

        StringView operator[](StringView name) const;
        StringView operator[](size_t column) const;

        size_t index() const { return m_index; }

        // FIXME: Implement begin() and end(), keeping `Field' out of the API.

    private:
        XSV& m_xsv;
        size_t m_index { 0 };
    };

    template<bool const_>
    class RowIterator {
    public:
        explicit RowIterator(const XSV& xsv, size_t init_index = 0) requires(const_)
            : m_xsv(const_cast<XSV&>(xsv))
            , m_index(init_index)
        {
        }

        explicit RowIterator(XSV& xsv, size_t init_index = 0) requires(!const_)
            : m_xsv(xsv)
            , m_index(init_index)
        {
        }

        Row operator*() const { return Row { m_xsv, m_index }; }
        Row operator*() requires(!const_) { return Row { m_xsv, m_index }; }

        RowIterator& operator++()
        {
            ++m_index;
            return *this;
        }

        bool is_end() const { return m_index == m_xsv.m_rows.size(); }
        bool operator==(const RowIterator& other) const
        {
            return m_index == other.m_index && &m_xsv == &other.m_xsv;
        }
        bool operator==(const RowIterator<!const_>& other) const
        {
            return m_index == other.m_index && &m_xsv == &other.m_xsv;
        }

    private:
        XSV& m_xsv;
        size_t m_index { 0 };
    };

    const Row operator[](size_t index) const;
    Row operator[](size_t index);

    auto begin() { return RowIterator<false>(*this); }
    auto end() { return RowIterator<false>(*this, m_rows.size()); }

    auto begin() const { return RowIterator<true>(*this); }
    auto end() const { return RowIterator<true>(*this, m_rows.size()); }

    using ConstIterator = RowIterator<true>;
    using Iterator = RowIterator<false>;

private:
    struct Field {
        StringView as_string_view;
        String as_string; // This member only used if the parser couldn't use the original source verbatim.
        bool is_string_view { true };

        bool operator==(StringView other) const
        {
            if (is_string_view)
                return other == as_string_view;
            return as_string == other;
        }
    };
    void set_error(ReadError error);
    void parse();
    void read_headers();
    Vector<Field> read_row(bool header_row = false);
    Field read_one_field();
    Field read_one_quoted_field();
    Field read_one_unquoted_field();

    StringView m_source;
    GenericLexer m_lexer;
    const ParserTraits& m_traits;
    ParserBehaviour m_behaviours;
    Vector<Field> m_names;
    Vector<Vector<Field>> m_rows;
    ReadError m_error { ReadError::None };
};

}
