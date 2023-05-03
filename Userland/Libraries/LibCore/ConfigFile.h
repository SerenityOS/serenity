/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibCore/File.h>

namespace Core {

class ConfigFile : public RefCounted<ConfigFile> {
public:
    enum class AllowWriting {
        Yes,
        No,
    };

    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_lib(DeprecatedString const& lib_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_app(DeprecatedString const& app_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_system(DeprecatedString const& app_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(DeprecatedString const& filename, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(DeprecatedString const& filename, int fd);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(DeprecatedString const& filename, NonnullOwnPtr<Core::File>);
    ~ConfigFile();

    bool has_group(DeprecatedString const&) const;
    bool has_key(DeprecatedString const& group, DeprecatedString const& key) const;

    Vector<DeprecatedString> groups() const;
    Vector<DeprecatedString> keys(DeprecatedString const& group) const;

    size_t num_groups() const { return m_groups.size(); }

    DeprecatedString read_entry(DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& default_value = DeprecatedString()) const;
    bool read_bool_entry(DeprecatedString const& group, DeprecatedString const& key, bool default_value = false) const;

    template<Integral T = int>
    T read_num_entry(DeprecatedString const& group, DeprecatedString const& key, T default_value = 0) const
    {
        if (!has_key(group, key))
            return default_value;

        if constexpr (IsSigned<T>)
            return read_entry(group, key).to_int<T>().value_or(default_value);
        else
            return read_entry(group, key).to_uint<T>().value_or(default_value);
    }

    void write_entry(DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value);
    void write_bool_entry(DeprecatedString const& group, DeprecatedString const& key, bool value);

    template<Integral T = int>
    void write_num_entry(DeprecatedString const& group, DeprecatedString const& key, T value)
    {
        write_entry(group, key, DeprecatedString::number(value));
    }

    void dump() const;

    bool is_dirty() const { return m_dirty; }

    ErrorOr<void> sync();

    void add_group(DeprecatedString const& group);
    void remove_group(DeprecatedString const& group);
    void remove_entry(DeprecatedString const& group, DeprecatedString const& key);

    DeprecatedString const& filename() const { return m_filename; }

private:
    ConfigFile(DeprecatedString const& filename, OwnPtr<InputBufferedFile> open_file);

    ErrorOr<void> reparse();

    DeprecatedString m_filename;
    OwnPtr<InputBufferedFile> m_file;
    HashMap<DeprecatedString, HashMap<DeprecatedString, DeprecatedString>> m_groups;
    bool m_dirty { false };
};

}
