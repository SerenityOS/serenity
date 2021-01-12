/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <AK/StringView.h>
#include <sys/types.h>

namespace Tar {

enum FileType {
    NormalFile = '0',
    AlternateNormalFile = '\0',
    HardLink = '1',
    SymLink = '2',
    CharacterSpecialFile = '3',
    BlockSpecialFile = '4',
    Directory = '5',
    FIFO = '6',
    ContiguousFile = '7',
    GlobalExtendedHeader = 'g',
    ExtendedHeader = 'x'
};

constexpr size_t block_size = 512;
constexpr const char* ustar_magic = "ustar ";

class Header {
public:
    // FIXME: support ustar filename prefix
    const StringView file_name() const { return m_file_name; }
    mode_t mode() const { return get_tar_field(m_mode); }
    uid_t uid() const { return get_tar_field(m_uid); }
    gid_t gid() const { return get_tar_field(m_gid); }
    // FIXME: support 2001-star size encoding
    size_t size() const { return get_tar_field(m_size); }
    time_t timestamp() const { return get_tar_field(m_timestamp); }
    FileType type_flag() const { return FileType(m_type_flag); }
    const StringView link_name() const { return m_link_name; }
    const StringView magic() const { return StringView(m_magic, sizeof(m_magic)); }
    const StringView version() const { return StringView(m_version, sizeof(m_version)); }
    const StringView owner_name() const { return m_owner_name; }
    const StringView group_name() const { return m_group_name; }
    int major() const { return get_tar_field(m_major); }
    int minor() const { return get_tar_field(m_minor); }

private:
    char m_file_name[100];
    char m_mode[8];
    char m_uid[8];
    char m_gid[8];
    char m_size[12];
    char m_timestamp[12];
    char m_checksum[8];
    char m_type_flag;
    char m_link_name[100];
    char m_magic[6];
    char m_version[2];
    char m_owner_name[32];
    char m_group_name[32];
    char m_major[8];
    char m_minor[8];
    char m_prefix[155];

    template<size_t N>
    static size_t get_tar_field(const char (&field)[N]);
};

template<size_t N>
size_t Header::get_tar_field(const char (&field)[N])
{
    size_t value = 0;
    for (size_t i = 0; i < N; ++i) {
        if (field[i] == 0)
            break;

        ASSERT(field[i] >= '0' && field[i] <= '7');
        value *= 8;
        value += field[i] - '0';
    }
    return value;
}

}
