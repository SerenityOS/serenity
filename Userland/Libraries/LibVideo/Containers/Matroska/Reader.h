/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/IntegralMath.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibVideo/DecoderError.h>

#include "Document.h"

namespace Video::Matroska {

class Streamer {
public:
    Streamer(ReadonlyBytes data)
        : m_data(data)
    {
    }

    u8 const* data() { return m_data.data() + m_position; }

    char const* data_as_chars() { return reinterpret_cast<char const*>(data()); }

    size_t octets_read() { return m_octets_read.last(); }

    void push_octets_read() { m_octets_read.append(0); }

    void pop_octets_read()
    {
        auto popped = m_octets_read.take_last();
        if (!m_octets_read.is_empty())
            m_octets_read.last() += popped;
    }

    ErrorOr<u8> read_octet();

    ErrorOr<i16> read_i16();

    ErrorOr<u64> read_variable_size_integer(bool mask_length = true);
    ErrorOr<i64> read_variable_size_signed_integer();

    ErrorOr<u64> read_u64();
    ErrorOr<double> read_float();

    ErrorOr<String> read_string();

    ErrorOr<void> read_unknown_element();

    ErrorOr<void> drop_octets(size_t num_octets);

    bool at_end() const { return remaining() == 0; }
    bool has_octet() const { return remaining() >= 1; }

    size_t remaining() const { return m_data.size() - m_position; }

private:
    ReadonlyBytes m_data;
    size_t m_position { 0 };
    Vector<size_t> m_octets_read { 0 };
};

class Reader {
public:
    Reader(ReadonlyBytes data)
        : m_streamer(data)
    {
    }

    static DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> parse_matroska_from_file(StringView path);
    static DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> parse_matroska_from_data(ReadonlyBytes data);

    DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> parse();

private:
    Streamer m_streamer;
};

}
