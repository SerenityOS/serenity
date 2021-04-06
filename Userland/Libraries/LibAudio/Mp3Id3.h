// FIXME: License
/*
 * Copyright (c) 2021, János Tóth <toth-janos@outlook.com>
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

#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>

#define MP3_SHOW_ID3 0

namespace Audio {

class Mp3LoaderPlugin;

namespace Mp3 {

// Based on: https://mutagen-specs.readthedocs.io/en/latest/Id3/Id3v2.4.0-structure.html
class Id3 {
public:
    Id3(Mp3LoaderPlugin*);
    ~Id3() {};

    bool is_valid() const { return m_valid; }

    bool has_error() const { return !m_error_string.is_empty(); }
    const char* error_string() const { return m_error_string.characters(); }

    String version() const { return m_version; }
    u8 flags() const { return m_flags; }
    size_t size() const { return m_size; }

    bool has_unsynchronisation() const { return (m_flags & 0x80) != 0; };
    bool has_extended_header() const { return (m_flags & 0x40) != 0; };
    bool has_experimental_indicator() const { return (m_flags & 0x20) != 0; };
    bool has_footer() const { return (m_flags & 0x10) != 0; };

    // extended header
    size_t extended_header_size() const { return m_extended_header_size; }
    u8 number_of_flag_bytes() const { return m_number_of_flag_bytes; };

private:
    u32 read_syncsafe_int(bool&);
    void read_flag_bytes(bool&);

    void set_version(u8, u8, bool&);
    void set_flags(u8, bool&);
    void set_size(u32);

    void set_extended_header_size(u32);
    void set_number_of_flag_bytes(u8);

    String m_error_string;

    bool m_valid { false };
    String m_version;
    u8 m_flags { 0 };
    size_t m_size { 0 };
    size_t m_extended_header_size { 0 };
    u8 m_number_of_flag_bytes { 0 };
    Vector<u8> m_extended_flags;

    Mp3LoaderPlugin* m_loader { nullptr };
};

}
}
