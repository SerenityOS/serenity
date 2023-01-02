/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/GenericLexer.h>
#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Stream.h>

namespace Writer {

enum class WriterBehavior : u32 {
    None = 0,
    WriteHeaders = 1,
    AllowNewlinesInFields = WriteHeaders << 1,
    QuoteOnlyInFieldStart = WriteHeaders << 2,
    QuoteAll = WriteHeaders << 3,
};
AK_ENUM_BITWISE_OPERATORS(WriterBehavior);

struct WriterTraits {
    DeprecatedString separator;
    DeprecatedString quote { "\"" };
    enum QuoteEscape {
        Repeat,
        Backslash,
    } quote_escape { Repeat };
};

constexpr WriterBehavior default_behaviors()
{
    return WriterBehavior::None;
}

template<typename ContainerType, typename HeaderType = Vector<StringView>>
class XSV {
public:
    XSV(Core::Stream::Handle<Core::Stream::Stream> output, ContainerType const& data, WriterTraits traits, HeaderType headers = {}, WriterBehavior behaviors = default_behaviors())
        : m_data(data)
        , m_traits(move(traits))
        , m_behaviors(behaviors)
        , m_names(headers)
        , m_output(move(output))
    {
        if (!headers.is_empty())
            m_behaviors = m_behaviors | WriterBehavior::WriteHeaders;
    }

    virtual ~XSV() = default;

    ErrorOr<void> generate()
    {
        auto with_headers = has_flag(m_behaviors, WriterBehavior::WriteHeaders);
        if (with_headers) {
            TRY(write_row(m_names));
            TRY(m_output->write_entire_buffer({ "\n", 1 }));
        }

        for (auto&& row : m_data) {
            if (with_headers) {
                if (row.size() != m_names.size())
                    return Error::from_string_literal("Header count does not match given column count");
            }

            TRY(write_row(row));
            TRY(m_output->write_entire_buffer({ "\n", 1 }));
        }
        return {};
    }

    ErrorOr<void> generate_preview()
    {
        auto lines_written = 0;
        constexpr auto max_preview_lines = 8;

        auto with_headers = has_flag(m_behaviors, WriterBehavior::WriteHeaders);
        if (with_headers) {
            TRY(write_row(m_names));
            TRY(m_output->write_entire_buffer({ "\n", 1 }));
            ++lines_written;
        }

        for (auto&& row : m_data) {
            if (with_headers) {
                if (row.size() != m_names.size())
                    return Error::from_string_literal("Header count does not match given column count");
            }

            TRY(write_row(row));
            TRY(m_output->write_entire_buffer({ "\n", 1 }));
            ++lines_written;

            if (lines_written >= max_preview_lines)
                break;
        }
        return {};
    }

private:
    template<typename T>
    ErrorOr<void> write_row(T&& row)
    {
        bool first = true;
        for (auto&& entry : row) {
            if (!first) {
                TRY(m_output->write_entire_buffer(m_traits.separator.bytes()));
            }
            first = false;
            TRY(write_entry(entry));
        }
        return {};
    }

    template<typename T>
    ErrorOr<void> write_entry(T&& entry)
    {
        auto string = DeprecatedString::formatted("{}", FormatIfSupported(entry));

        auto safe_to_write_normally = !has_flag(m_behaviors, WriterBehavior::QuoteAll)
            && !string.contains('\n')
            && !string.contains(m_traits.separator);

        if (safe_to_write_normally) {
            if (has_flag(m_behaviors, WriterBehavior::QuoteOnlyInFieldStart))
                safe_to_write_normally = !string.starts_with(m_traits.quote);
            else
                safe_to_write_normally = !string.contains(m_traits.quote);
        }

        if (safe_to_write_normally) {
            if (!string.is_empty())
                TRY(m_output->write_entire_buffer(string.bytes()));
            return {};
        }

        TRY(m_output->write_entire_buffer(m_traits.quote.bytes()));

        GenericLexer lexer(string);
        while (!lexer.is_eof()) {
            if (lexer.consume_specific(m_traits.quote)) {
                switch (m_traits.quote_escape) {
                case WriterTraits::Repeat:
                    TRY(m_output->write_entire_buffer(m_traits.quote.bytes()));
                    TRY(m_output->write_entire_buffer(m_traits.quote.bytes()));
                    break;
                case WriterTraits::Backslash:
                    TRY(m_output->write_entire_buffer({ "\\", 1 }));
                    TRY(m_output->write_entire_buffer(m_traits.quote.bytes()));
                    break;
                }
                continue;
            }

            auto ch = lexer.consume();
            TRY(m_output->write_entire_buffer({ &ch, 1 }));
        }

        TRY(m_output->write_entire_buffer(m_traits.quote.bytes()));
        return {};
    }

    ContainerType const& m_data;
    WriterTraits m_traits;
    WriterBehavior m_behaviors;
    HeaderType m_names;
    Core::Stream::Handle<Core::Stream::Stream> m_output;
};

}
