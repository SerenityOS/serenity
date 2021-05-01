/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
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

#include "MatroskaDocument.h"
#include <AK/Debug.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <math.h>
#include <assert.h>

namespace Video {

class MatroskaReader {
public:
    MatroskaReader(const u8* data, size_t size)
        : m_streamer(data, size)
    {
    }

    static OwnPtr<MatroskaDocument> parse_matroska_from_file(const StringView& path);
    static OwnPtr<MatroskaDocument> parse_matroska_from_data(const u8*, size_t);

    OwnPtr<MatroskaDocument> parse();

private:
    class Streamer {
    public:
        Streamer(const u8* data, size_t size)
            : m_data_ptr(data)
            , m_size_remaining(size)
        {
        }

        const u8* data() { return m_data_ptr; }

        const char* data_as_chars() { return reinterpret_cast<const char*>(m_data_ptr); }

        u8 read_octet()
        {
            assert(m_size_remaining >= 1);
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
            dbgln_if(MATROSKA_DEBUG, "Reading from offset {}", m_data_ptr);
            auto length_descriptor = read_octet();
            dbgln_if(MATROSKA_DEBUG, "Reading VINT, first byte is {}", length_descriptor);
            if (length_descriptor == 0)
                return {};
            size_t length = 0;
            while (length < 8) {
                if (length_descriptor & (1u << (8 - length)))
                    break;
                length++;
            }
            dbgln_if(MATROSKA_DEBUG, "Reading VINT of total length {}", length);
            if (length > 8)
                return {};

            u64 result;
            if (mask_length)
                result = length_descriptor & ~(1u << (8 - length));
            else
                result = length_descriptor;
            dbgln_if(MATROSKA_DEBUG, "Beginning of VINT is {}", result);
            for (size_t i = 1; i < length; i++) {
                if (!has_octet()) {
                    dbgln_if(MATROSKA_DEBUG, "Ran out of stream data");
                    return {};
                }
                u8 next_octet = read_octet();
                dbgln_if(MATROSKA_DEBUG, "Read octet of {}", next_octet);
                result = (result << 8u) | next_octet;
                dbgln_if(MATROSKA_DEBUG, "New result is {}", result);
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
            result -= pow(2, length * 7 - 1) - 1;
            return result;
        }

        void drop_octets(size_t num_octets)
        {
            assert(m_size_remaining >= num_octets);
            m_size_remaining -= num_octets;
            m_octets_read.last() += num_octets;
            m_data_ptr += num_octets;
        }

        bool at_end() const { return !m_size_remaining; }
        bool has_octet() const { return m_size_remaining >= 1; }

        size_t remaining() const { return m_size_remaining; }
        void set_remaining(size_t remaining) { m_size_remaining = remaining; }

    private:
        const u8* m_data_ptr { nullptr };
        size_t m_size_remaining { 0 };
        Vector<size_t> m_octets_read { 0 };
    };

    bool parse_master_element(const StringView& element_name, Function<bool(u64 element_id)> element_consumer);
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
