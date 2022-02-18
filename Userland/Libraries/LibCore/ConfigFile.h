/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/Stream.h>
#include <LibGfx/Color.h>

namespace Core {

class ConfigFile : public RefCounted<ConfigFile> {
public:
    enum class AllowWriting {
        Yes,
        No,
    };

    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_lib(String const& lib_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_app(String const& app_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open_for_system(String const& app_name, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(String const& filename, AllowWriting = AllowWriting::No);
    static ErrorOr<NonnullRefPtr<ConfigFile>> open(String const& filename, int fd);
    ~ConfigFile();

    bool has_group(String const&) const;
    bool has_key(String const& group, String const& key) const;

    Vector<String> groups() const;
    Vector<String> keys(String const& group) const;

    size_t num_groups() const { return m_groups.size(); }

    String read_entry(String const& group, String const& key, String const& default_value = String()) const;
    int read_num_entry(String const& group, String const& key, int default_value = 0) const;
    bool read_bool_entry(String const& group, String const& key, bool default_value = false) const;

    void write_entry(String const& group, String const& key, String const& value);
    void write_num_entry(String const& group, String const& key, int value);
    void write_bool_entry(String const& group, String const& key, bool value);
    void write_color_entry(String const& group, String const& key, Color value);

    void dump() const;

    bool is_dirty() const { return m_dirty; }

    ErrorOr<void> sync();

    void remove_group(String const& group);
    void remove_entry(String const& group, String const& key);

    String const& filename() const { return m_filename; }

private:
    ConfigFile(String const& filename, OwnPtr<Stream::BufferedFile> open_file);

    ErrorOr<void> reparse();

    String m_filename;
    OwnPtr<Stream::BufferedFile> m_file;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};

}
