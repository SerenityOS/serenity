/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/OwnPtr.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Writer {

enum class WriterBehavior : u32 {
    None = 0,
    WriteHeaders = 1,
    AllowNewlinesInFields = WriteHeaders << 1,
    QuoteOnlyInFieldStart = WriteHeaders << 2,
    QuoteAll = WriteHeaders << 3,
};

inline WriterBehavior operator&(WriterBehavior left, WriterBehavior right)
{
    return static_cast<WriterBehavior>(static_cast<u32>(left) & static_cast<u32>(right));
}

inline WriterBehavior operator|(WriterBehavior left, WriterBehavior right)
{
    return static_cast<WriterBehavior>(static_cast<u32>(left) | static_cast<u32>(right));
}

struct WriterTraits {
    String separator;
    String quote { "\"" };
    enum QuoteEscape {
        Repeat,
        Backslash,
    } quote_escape { Repeat };
};

#define ENUMERATE_WRITE_ERRORS()                                                  \
    E(None, "No errors")                                                          \
    E(NonConformingColumnCount, "Header count does not match given column count") \
    E(InternalError, "Internal error")

enum class WriteError {
#define E(name, _) name,
    ENUMERATE_WRITE_ERRORS()
#undef E
};

constexpr WriterBehavior default_behaviors()
{
    return WriterBehavior::None;
}

template<typename ContainerType, typename HeaderType = Vector<StringView>>
class XSV {
public:
    XSV(OutputStream& output, const ContainerType& data, const WriterTraits& traits, const HeaderType& headers = {}, WriterBehavior behaviors = default_behaviors())
        : m_data(data)
        , m_traits(traits)
        , m_behaviors(behaviors)
        , m_names(headers)
        , m_output(output)
    {
        if (!headers.is_empty())
            m_behaviors = m_behaviors | WriterBehavior::WriteHeaders;

        generate();
    }

    virtual ~XSV() { }

    bool has_error() const { return m_error != WriteError::None; }
    WriteError error() const { return m_error; }
    String error_string() const
    {
        switch (m_error) {
#define E(x, y)         \
    case WriteError::x: \
        return y;

            ENUMERATE_WRITE_ERRORS();
#undef E
        }
        VERIFY_NOT_REACHED();
    }

private:
    void set_error(WriteError error)
    {
        if (m_error == WriteError::None)
            m_error = error;
    }

    void generate()
    {
        auto with_headers = (m_behaviors & WriterBehavior::WriteHeaders) != WriterBehavior::None;
        if (with_headers) {
            write_row(m_names);
            if (m_output.write({ "\n", 1 }) != 1)
                set_error(WriteError::InternalError);
        }

        for (auto&& row : m_data) {
            if (with_headers) {
                if (row.size() != m_names.size())
                    set_error(WriteError::NonConformingColumnCount);
            }

            write_row(row);
            if (m_output.write({ "\n", 1 }) != 1)
                set_error(WriteError::InternalError);
        }
    }

    template<typename T>
    void write_row(T&& row)
    {
        bool first = true;
        for (auto&& entry : row) {
            if (!first) {
                if (m_output.write(m_traits.separator.bytes()) != m_traits.separator.length())
                    set_error(WriteError::InternalError);
            }
            first = false;
            write_entry(entry);
        }
    }

    template<typename T>
    void write_entry(T&& entry)
    {
        auto string = String::formatted("{}", FormatIfSupported(entry));

        auto safe_to_write_normally = (m_behaviors & WriterBehavior::QuoteAll) == WriterBehavior::None
            && !string.contains("\n")
            && !string.contains(m_traits.separator);

        if (safe_to_write_normally) {
            if ((m_behaviors & WriterBehavior::QuoteOnlyInFieldStart) == WriterBehavior::None)
                safe_to_write_normally = !string.contains(m_traits.quote);
            else
                safe_to_write_normally = !string.starts_with(m_traits.quote);
        }

        if (safe_to_write_normally) {
            if (m_output.write(string.bytes()) != string.length())
                set_error(WriteError::InternalError);
            return;
        }

        if (m_output.write(m_traits.quote.bytes()) != m_traits.quote.length())
            set_error(WriteError::InternalError);

        GenericLexer lexer(string);
        while (!lexer.is_eof()) {
            if (lexer.consume_specific(m_traits.quote)) {
                switch (m_traits.quote_escape) {
                case WriterTraits::Repeat:
                    if (m_output.write(m_traits.quote.bytes()) != m_traits.quote.length())
                        set_error(WriteError::InternalError);
                    if (m_output.write(m_traits.quote.bytes()) != m_traits.quote.length())
                        set_error(WriteError::InternalError);
                    break;
                case WriterTraits::Backslash:
                    if (m_output.write({ "\\", 1 }) != 1)
                        set_error(WriteError::InternalError);
                    if (m_output.write(m_traits.quote.bytes()) != m_traits.quote.length())
                        set_error(WriteError::InternalError);
                    break;
                }
                continue;
            }

            auto ch = lexer.consume();
            if (m_output.write({ &ch, 1 }) != 1)
                set_error(WriteError::InternalError);
        }

        if (m_output.write(m_traits.quote.bytes()) != m_traits.quote.length())
            set_error(WriteError::InternalError);
    }

    const ContainerType& m_data;
    const WriterTraits& m_traits;
    WriterBehavior m_behaviors;
    const HeaderType& m_names;
    WriteError m_error { WriteError::None };
    OutputStream& m_output;
};

}
