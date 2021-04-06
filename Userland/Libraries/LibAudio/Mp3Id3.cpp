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

#include <LibAudio/Mp3Id3.h>
#include <LibAudio/Mp3Loader.h>

#define CHECK_OK(msg)         \
    if (!ok) {                \
        m_error_string = msg; \
        dbgln(msg);           \
        return;               \
    }

#define ERROR(msg)            \
    do {                      \
        ok = false;           \
        m_error_string = msg; \
        dbgln(msg);           \
        return;               \
    } while (0)

namespace Audio::Mp3 {

Id3::Id3(Mp3LoaderPlugin* loader)
    : m_loader(loader)
{
    m_valid = false;
    bool ok;

    u8 tagI = m_loader->read_byte(ok);
    CHECK_OK("Cannot read first byte.");
    u8 tagD = m_loader->read_byte(ok);
    CHECK_OK("Cannot second first byte.");
    u8 tag3 = m_loader->read_byte(ok);
    CHECK_OK("Cannot read third byte.");

    // FIXME: Support ID3v1.
    if (tagI != 'I' || tagD != 'D' || tag3 != '3') {
        dbgln("Invalid ID3v2 tag: {:c}{:c}{:c}", tagI, tagD, tag3);
        m_valid = false;
        return;
    }

    m_valid = true;

    u8 version = m_loader->read_byte(ok);
    CHECK_OK("Cannot read version byte.");
    u8 revision = m_loader->read_byte(ok);
    CHECK_OK("Cannot read revision byte.");
    set_version(version, revision, ok);
    CHECK_OK("Cannot set version.");

    u8 flags = m_loader->read_byte(ok);
    CHECK_OK("Cannot read flags byte.");
    set_flags(flags, ok);
    CHECK_OK("Cannot set flags.");

    u32 size = read_syncsafe_int(ok);
    CHECK_OK("Cannot read size.");
    set_size(size);

    if (has_extended_header()) {
        u32 ext_header_size = read_syncsafe_int(ok);
        CHECK_OK("Cannot read header size.");
        set_extended_header_size(ext_header_size);

        u8 number_of_flag_bytes = m_loader->read_byte(ok);
        CHECK_OK("Cannot read number of flags.");
        set_number_of_flag_bytes(number_of_flag_bytes);

        // FIXME: Use them somewhere.
        read_flag_bytes(ok);
        CHECK_OK("Cannot read flag.");
    }

    // FIXME: Read tags.
}

ALWAYS_INLINE void Id3::set_version(u8 version, u8 revision, bool& ok)
{
    // Version or revision will never be 0xff.
    ok = version != 0xff && revision != 0xff;

    m_version = String::format("2.%u.%u", version, revision);
    dbgln_if(MP3_SHOW_ID3, "ID3 version={}", m_version);
}

ALWAYS_INLINE void Id3::set_flags(u8 flags, bool& ok)
{
    ok = true;

    if ((flags & 0b00001111) != 0) {
        ERROR(String::format("Invalid ID3 flags: %x", flags));
    }

    m_flags = flags;
    dbgln_if(MP3_SHOW_ID3, "ID3 flags={:08b}", flags);
}

ALWAYS_INLINE void Id3::set_size(u32 size)
{
    m_size = size;
    dbgln_if(MP3_SHOW_ID3, "ID3 size={}", m_size);
}

ALWAYS_INLINE void Id3::set_extended_header_size(u32 size)
{
    m_extended_header_size = size;
    dbgln_if(MP3_SHOW_ID3, "ID3 extended_header_size={}", m_extended_header_size);
}

ALWAYS_INLINE void Id3::set_number_of_flag_bytes(u8 value)
{
    m_number_of_flag_bytes = value;
    dbgln_if(MP3_SHOW_ID3, "ID3 number_of_flag_bytes={}", m_number_of_flag_bytes);
}

u32 Id3::read_syncsafe_int(bool& ok)
{
    u32 value = 0;
    u8 part;
    for (u8 i = 0; i < 4; i++) {
        part = m_loader->read_byte(ok);
        if (!ok) {
            break;
        }

        // Not a syncsafe integer.
        if ((part & 0x80) != 0) {
            ok = false;
            break;
        }

        value = (value << 7) + part;
    }
    return value;
}

void Id3::read_flag_bytes(bool& ok)
{
    u8 byte;
    m_extended_flags.clear();

    for (u8 i = 0; i < m_number_of_flag_bytes; i++) {
        byte = m_loader->read_byte(ok);
        if (!ok) {
            break;
        }

        dbgln_if(MP3_SHOW_ID3, "ID3 flag byte #{}={:08b}", i, byte);

        m_extended_flags.append(byte);
    }
}

}

#undef ERROR
#undef CHECK_OK
