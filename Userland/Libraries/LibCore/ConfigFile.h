/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibGfx/Color.h>

namespace Core {

class ConfigFile : public RefCounted<ConfigFile> {
public:
    enum class AllowWriting {
        Yes,
        No,
    };

    static NonnullRefPtr<ConfigFile> get_for_lib(String const& lib_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> get_for_app(String const& app_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> get_for_system(String const& app_name, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open(String const& filename, AllowWriting = AllowWriting::No);
    static NonnullRefPtr<ConfigFile> open(String const& filename, int fd);
    ~ConfigFile();

    bool has_group(const String&) const;
    bool has_key(const String& group, const String& key) const;

    Vector<String> groups() const;
    Vector<String> keys(const String& group) const;

    size_t num_groups() const { return m_groups.size(); }

    String read_entry(const String& group, const String& key, const String& default_value = String()) const;
    int read_num_entry(const String& group, const String& key, int default_value = 0) const;
    bool read_bool_entry(const String& group, const String& key, bool default_value = false) const;

    void write_entry(const String& group, const String& key, const String& value);
    void write_num_entry(const String& group, const String& key, int value);
    void write_bool_entry(const String& group, const String& key, bool value);
    void write_color_entry(const String& group, const String& key, Color value);

    void dump() const;

    bool is_dirty() const { return m_dirty; }

    bool sync();

    void remove_group(const String& group);
    void remove_entry(const String& group, const String& key);

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
