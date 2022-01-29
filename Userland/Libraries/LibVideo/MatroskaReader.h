/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MatroskaDocument.h"
#include <AK/Debug.h>
#include <AK/IntegralMath.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>

namespace Video {

class MatroskaReader {
public:
    MatroskaReader(u8 const* data, size_t size)
        : m_streamer(data, size)
    {
    }

    static OwnPtr<MatroskaDocument> parse_matroska_from_file(StringView path);
    static OwnPtr<MatroskaDocument> parse_matroska_from_data(u8 const*, size_t);

    OwnPtr<MatroskaDocument> parse();

private:
    class Streamer {
    public:
        Streamer(u8 const* data, size_t size)
            : m_data_ptr(data)
            , m_size_remaining(size)
        {
        }

        u8 const* data() { return m_data_ptr; }

        char const* data_as_chars() { return reinterpret_cast<char const*>(m_data_ptr); }

        u8 read_octet()
        {
            VERIFY(m_size_remaining >= 1);
            m_size_remaining--;
            m_octets_read.last()++;
            return *(m_data_ptr++);
        }

        i16 read_i16()
        {
            return (read_octet() << 8) | read_octet();
        }

        size_t octets_read() { return m_octets_read.last(); }

        void push_octets_read() { m_octets_read.append(0); }

        void pop_octets_read()
        {
            auto popped = m_octets_read.take_last();
            if (!m_octets_read.is_empty())
                m_octets_read.last() += popped;
        }

        Optional<u64> read_variable_size_integer(bool mask_length = true)
        {
            dbgln_if(MATROSKA_TRACE_DEBUG, "Reading from offset {:p}", m_data_ptr);
            auto length_descriptor = read_octet();
            dbgln_if(MATROSKA_TRACE_DEBUG, "Reading VINT, first byte is {:#02x}", length_descriptor);
            if (length_descriptor == 0)
                return {};
            size_t length = 0;
            while (length < 8) {
                if (length_descriptor & (1u << (8 - length)))
                    break;
                length++;
            }
            dbgln_if(MATROSKA_TRACE_DEBUG, "Reading VINT of total length {}", length);
            if (length > 8)
                return {};

            u64 result;
            if (mask_length)
                result = length_descriptor & ~(1u << (8 - length));
            else
                result = length_descriptor;
            dbgln_if(MATROSKA_TRACE_DEBUG, "Beginning of VINT is {:#02x}", result);
            for (size_t i = 1; i < length; i++) {
                if (!has_octet()) {
                    dbgln_if(MATROSKA_TRACE_DEBUG, "Ran out of stream data");
                    return {};
                }
                u8 next_octet = read_octet();
                dbgln_if(MATROSKA_TRACE_DEBUG, "Read octet of {:#02x}", next_octet);
                result = (result << 8u) | next_octet;
                dbgln_if(MATROSKA_TRACE_DEBUG, "New result is {:#010x}", result);
            }
            return result;
        }

        Optional<i64> read_variable_sized_signed_integer()
        {
            auto length_descriptor = read_octet();
            if (length_descriptor == 0)
                return {};
            size_t length = 0;
            while (length < 8) {
                if (length_descriptor & (1u << (8 - length)))
                    break;
                length++;
            }
            if (length > 8)
                return {};

            i64 result = length_descriptor & ~(1u << (8 - length));
            for (size_t i = 1; i < length; i++) {
                if (!has_octet()) {
                    return {};
                }
                u8 next_octet = read_octet();
                result = (result << 8u) | next_octet;
            }
            result -= AK::exp2<i64>(length * 7 - 1) - 1;
            return result;
        }

        void drop_octets(size_t num_octets)
        {
            VERIFY(m_size_remaining >= num_octets);
            m_size_remaining -= num_octets;
            m_octets_read.last() += num_octets;
            m_data_ptr += num_octets;
        }

        bool at_end() const { return !m_size_remaining; }
        bool has_octet() const { return m_size_remaining >= 1; }

        size_t remaining() const { return m_size_remaining; }
        void set_remaining(size_t remaining) { m_size_remaining = remaining; }

    private:
        u8 const* m_data_ptr { nullptr };
        size_t m_size_remaining { 0 };
        Vector<size_t> m_octets_read { 0 };
    };

    bool parse_master_element(StringView element_name, Function<bool(u64 element_id)> element_consumer);
    Optional<EBMLHeader> parse_ebml_header();

    bool parse_segment_elements(MatroskaDocument&);
    OwnPtr<SegmentInformation> parse_information();

    bool parse_tracks(MatroskaDocument&);
    OwnPtr<TrackEntry> parse_track_entry();
    Optional<TrackEntry::VideoTrack> parse_video_track_information();
    Optional<TrackEntry::AudioTrack> parse_audio_track_information();
    OwnPtr<Cluster> parse_cluster();
    OwnPtr<Block> parse_simple_block();

    Optional<String> read_string_element();
    Optional<u64> read_u64_element();
    bool read_unknown_element();

    Streamer m_streamer;
};

}
