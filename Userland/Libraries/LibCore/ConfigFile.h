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
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibCore/Stream.h>

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
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(DeprecatedString const& filename, NonnullOwnPtr<Core::Stream::File>);
    ~ConfigFile();

    bool has_group(DeprecatedString const&) const;
    bool has_key(DeprecatedString const& group, DeprecatedString const& key) const;

    Vector<DeprecatedString> groups() const;
    Vector<DeprecatedString> keys(DeprecatedString const& group) const;

    size_t num_groups() const { return m_groups.size(); }

    DeprecatedString read_entry(DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& default_value = DeprecatedString()) const;
    int read_num_entry(DeprecatedString const& group, DeprecatedString const& key, int default_value = 0) const;
    bool read_bool_entry(DeprecatedString const& group, DeprecatedString const& key, bool default_value = false) const;

    void write_entry(DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value);
    void write_num_entry(DeprecatedString const& group, DeprecatedString const& key, int value);
    void write_bool_entry(DeprecatedString const& group, DeprecatedString const& key, bool value);

    void dump() const;

    bool is_dirty() const { return m_dirty; }

    ErrorOr<void> sync();

    void add_group(DeprecatedString const& group);
    void remove_group(DeprecatedString const& group);
    void remove_entry(DeprecatedString const& group, DeprecatedString const& key);

    DeprecatedString const& filename() const { return m_filename; }

private:
    ConfigFile(DeprecatedString const& filename, OwnPtr<Stream::BufferedFile> open_file);

    ErrorOr<void> reparse();

    DeprecatedString m_filename;
    OwnPtr<Stream::BufferedFile> m_file;
    HashMap<DeprecatedString, HashMap<DeprecatedString, DeprecatedString>> m_groups;
    bool m_dirty { false };
};

}
