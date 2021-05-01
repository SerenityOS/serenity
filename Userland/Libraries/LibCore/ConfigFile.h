/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>

namespace Core {

class ConfigFile : public RefCounted<ConfigFile> {
public:
    static NonnullRefPtr<ConfigFile> get_for_lib(const String& lib_name);
    static NonnullRefPtr<ConfigFile> get_for_app(const String& app_name);
    static NonnullRefPtr<ConfigFile> get_for_system(const String& app_name);
    static NonnullRefPtr<ConfigFile> open(const String& path);
    ~ConfigFile();

    bool has_group(const String&) const;
    bool has_key(const String& group, const String& key) const;

    Vector<String> groups() const;
    Vector<String> keys(const String& group) const;

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

    String filename() const { return m_filename; }

private:
    explicit ConfigFile(const String& filename);

    void reparse();

    String m_filename;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};

}
