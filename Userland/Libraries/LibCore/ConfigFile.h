/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/HashMap.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <YAK/String.h>
#include <YAK/Vector.h>
#include <LibCore/File.h>
#include <LibGfx/Color.h>

namespace Core {

class ConfigFile : public RefCounted<ConfigFile> {
public:
    enum class AllowWriting {
        Yes,
        No,
    };

    static NonnullRefPtr<ConfigFile> open_for_lib(String const& lib_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open_for_app(String const& app_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open_for_system(String const& app_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open(String const& filename, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open(String const& filename, int fd);
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

    bool sync();

    void remove_group(String const& group);
    void remove_entry(String const& group, String const& key);

    String filename() const { return m_file->filename(); }

private:
    explicit ConfigFile(String const& filename, AllowWriting);
    explicit ConfigFile(String const& filename, int fd);

    void reparse();

    NonnullRefPtr<File> m_file;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};

}
