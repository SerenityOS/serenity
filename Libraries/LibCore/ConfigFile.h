/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

    String file_name() const { return m_file_name; }

private:
    explicit ConfigFile(const String& file_name);

    void reparse();

    String m_file_name;
    HashMap<String, HashMap<String, String>> m_groups;
    bool m_dirty { false };
};

}
