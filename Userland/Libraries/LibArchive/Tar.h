/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
constexpr StringView gnu_magic = "ustar ";    // gnu format magic
constexpr StringView gnu_version = " ";       // gnu format version
constexpr StringView ustar_magic = "ustar";   // ustar format magic
constexpr StringView ustar_version = "00";    // ustar format version
constexpr StringView posix1_tar_magic = "";   // POSIX.1-1988 format magic
constexpr StringView posix1_tar_version = ""; // POSIX.1-1988 format version

template<size_t N>
static size_t get_field_as_integral(const char (&field)[N])
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

template<size_t N>
static StringView get_field_as_string_view(const char (&field)[N])
{
    return { field, min(__builtin_strlen(field), N) };
}

template<size_t N, class TSource>
static void set_field(char (&field)[N], TSource&& source)
{
    if constexpr (requires { source.copy_characters_to_buffer(field, N); }) {
        VERIFY(source.copy_characters_to_buffer(field, N));
    } else {
        memcpy(field, source.characters_without_null_termination(), min(N, source.length()));
    }
}

template<class TSource, size_t N>
static void set_octal_field(char (&field)[N], TSource&& source)
{
    set_field(field, String::formatted("{:o}", forward<TSource>(source)));
}

class [[gnu::packed]] TarFileHeader {
public:
    StringView filename() const { return get_field_as_string_view(m_filename); }
    mode_t mode() const { return get_field_as_integral(m_mode); }
    uid_t uid() const { return get_field_as_integral(m_uid); }
    gid_t gid() const { return get_field_as_integral(m_gid); }
    // FIXME: support 2001-star size encoding
    size_t size() const { return get_field_as_integral(m_size); }
    time_t timestamp() const { return get_field_as_integral(m_timestamp); }
    unsigned checksum() const { return get_field_as_integral(m_checksum); }
    TarFileType type_flag() const { return TarFileType(m_type_flag); }
    StringView link_name() const { return m_link_name; }
    StringView magic() const { return get_field_as_string_view(m_magic); }
    StringView version() const { return get_field_as_string_view(m_version); }
    StringView owner_name() const { return get_field_as_string_view(m_owner_name); }
    StringView group_name() const { return get_field_as_string_view(m_group_name); }
    int major() const { return get_field_as_integral(m_major); }
    int minor() const { return get_field_as_integral(m_minor); }
    // FIXME: support ustar filename prefix
    StringView prefix() const { return get_field_as_string_view(m_prefix); }

    void set_filename(const String& filename) { set_field(m_filename, filename); }
    void set_mode(mode_t mode) { set_octal_field(m_mode, mode); }
    void set_uid(uid_t uid) { set_octal_field(m_uid, uid); }
    void set_gid(gid_t gid) { set_octal_field(m_gid, gid); }
    void set_size(size_t size) { set_octal_field(m_size, size); }
    void set_timestamp(time_t timestamp) { set_octal_field(m_timestamp, timestamp); }
    void set_type_flag(TarFileType type) { m_type_flag = to_underlying(type); }
    void set_link_name(const String& link_name) { set_field(m_link_name, link_name); }
    // magic doesn't necessarily include a null byte
    void set_magic(StringView magic) { set_field(m_magic, magic); }
    // version doesn't necessarily include a null byte
    void set_version(StringView version) { set_field(m_version, version); }
    void set_owner_name(const String& owner_name) { set_field(m_owner_name, owner_name); }
    void set_group_name(const String& group_name) { set_field(m_group_name, group_name); }
    void set_major(int major) { set_octal_field(m_major, major); }
    void set_minor(int minor) { set_octal_field(m_minor, minor); }
    void set_prefix(const String& prefix) { set_field(m_prefix, prefix); }

    unsigned expected_checksum() const;
    void calculate_checksum();

private:
    char m_filename[100] { 0 };
    char m_mode[8] { 0 };
    char m_uid[8] { 0 };
    char m_gid[8] { 0 };
    char m_size[12] { 0 };
    char m_timestamp[12] { 0 };
    char m_checksum[8] { 0 }; // an uninitialized header's checksum is filled with spaces
    char m_type_flag { 0 };
    char m_link_name[100] { 0 };
    char m_magic[6] { 0 };
    char m_version[2] { 0 };
    char m_owner_name[32] { 0 };
    char m_group_name[32] { 0 };
    char m_major[8] { 0 };
    char m_minor[8] { 0 };
    char m_prefix[155] { 0 }; // zero out the prefix for archiving
};

}
