/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <string.h>
#include <sys/types.h>

namespace Archive {

enum class TarFileType : char {
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
constexpr const char* gnu_magic = "ustar ";    // gnu format magic
constexpr const char* gnu_version = " ";       // gnu format version
constexpr const char* ustar_magic = "ustar";   // ustar format magic
constexpr const char* ustar_version = "00";    // ustar format version
constexpr const char* posix1_tar_magic = "";   // POSIX.1-1988 format magic
constexpr const char* posix1_tar_version = ""; // POSIX.1-1988 format version

class [[gnu::packed]] TarFileHeader {
public:
    const StringView filename() const { return StringView(m_filename, min(__builtin_strlen(m_filename), sizeof(m_filename))); }
    mode_t mode() const { return get_tar_field(m_mode); }
    uid_t uid() const { return get_tar_field(m_uid); }
    gid_t gid() const { return get_tar_field(m_gid); }
    // FIXME: support 2001-star size encoding
    size_t size() const { return get_tar_field(m_size); }
    time_t timestamp() const { return get_tar_field(m_timestamp); }
    unsigned checksum() const { return get_tar_field(m_checksum); }
    TarFileType type_flag() const { return TarFileType(m_type_flag); }
    const StringView link_name() const { return m_link_name; }
    const StringView magic() const { return StringView(m_magic, min(__builtin_strlen(m_magic), sizeof(m_magic))); }
    const StringView version() const { return StringView(m_version, min(__builtin_strlen(m_version), sizeof(m_version))); }
    const StringView owner_name() const { return StringView(m_owner_name, min(__builtin_strlen(m_owner_name), sizeof(m_owner_name))); }
    const StringView group_name() const { return StringView(m_group_name, min(__builtin_strlen(m_group_name), sizeof(m_group_name))); }
    int major() const { return get_tar_field(m_major); }
    int minor() const { return get_tar_field(m_minor); }
    // FIXME: support ustar filename prefix
    const StringView prefix() const { return StringView(m_prefix, min(__builtin_strlen(m_prefix), sizeof(m_prefix))); }

    void set_filename(const String& filename) { VERIFY(filename.copy_characters_to_buffer(m_filename, sizeof(m_filename))); }
    void set_mode(mode_t mode) { VERIFY(String::formatted("{:o}", mode).copy_characters_to_buffer(m_mode, sizeof(m_mode))); }
    void set_uid(uid_t uid) { VERIFY(String::formatted("{:o}", uid).copy_characters_to_buffer(m_uid, sizeof(m_uid))); }
    void set_gid(gid_t gid) { VERIFY(String::formatted("{:o}", gid).copy_characters_to_buffer(m_gid, sizeof(m_gid))); }
    void set_size(size_t size) { VERIFY(String::formatted("{:o}", size).copy_characters_to_buffer(m_size, sizeof(m_size))); }
    void set_timestamp(time_t timestamp) { VERIFY(String::formatted("{:o}", timestamp).copy_characters_to_buffer(m_timestamp, sizeof(m_timestamp))); }
    void set_type_flag(TarFileType type) { m_type_flag = static_cast<char>(type); }
    void set_link_name(const String& link_name) { VERIFY(link_name.copy_characters_to_buffer(m_link_name, sizeof(m_link_name))); }
    void set_magic(const char* magic) { memcpy(m_magic, magic, sizeof(m_magic)); }           // magic doesn't necessarily include a null byte
    void set_version(const char* version) { memcpy(m_version, version, sizeof(m_version)); } // version doesn't necessarily include a null byte
    void set_owner_name(const String& owner_name) { VERIFY(owner_name.copy_characters_to_buffer(m_owner_name, sizeof(m_owner_name))); }
    void set_group_name(const String& group_name) { VERIFY(group_name.copy_characters_to_buffer(m_group_name, sizeof(m_group_name))); }
    void set_major(int major) { VERIFY(String::formatted("{:o}", major).copy_characters_to_buffer(m_major, sizeof(m_major))); }
    void set_minor(int minor) { VERIFY(String::formatted("{:o}", minor).copy_characters_to_buffer(m_minor, sizeof(m_minor))); }
    void set_prefix(const String& prefix) { VERIFY(prefix.copy_characters_to_buffer(m_prefix, sizeof(m_prefix))); }

    unsigned expected_checksum() const;
    void calculate_checksum();

private:
    char m_filename[100];
    char m_mode[8];
    char m_uid[8];
    char m_gid[8];
    char m_size[12];
    char m_timestamp[12];
    char m_checksum[8]; // an uninitialized header's checksum is filled with spaces
    char m_type_flag;
    char m_link_name[100];
    char m_magic[6];
    char m_version[2];
    char m_owner_name[32];
    char m_group_name[32];
    char m_major[8];
    char m_minor[8];
    char m_prefix[155]; // zero out the prefix for archiving

    template<size_t N>
    static size_t get_tar_field(const char (&field)[N]);
};

template<size_t N>
size_t TarFileHeader::get_tar_field(const char (&field)[N])
{
    size_t value = 0;
    for (size_t i = 0; i < N; ++i) {
        if (field[i] == 0)
            break;

        VERIFY(field[i] >= '0' && field[i] <= '7');
        value *= 8;
        value += field[i] - '0';
    }
    return value;
}

}
