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

#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/UserInfo.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

namespace Core {

NonnullRefPtr<ConfigFile> ConfigFile::get_for_app(const String& app_name)
{
    String home_path = get_current_user_home_path();
    if (home_path == "/")
        home_path = String::format("/tmp");
    auto path = String::format("%s/%s.ini", home_path.characters(), app_name.characters());
    return adopt(*new ConfigFile(path));
}

NonnullRefPtr<ConfigFile> ConfigFile::get_for_system(const String& app_name)
{
    auto path = String::format("/etc/%s.ini", app_name.characters());
    return adopt(*new ConfigFile(path));
}

NonnullRefPtr<ConfigFile> ConfigFile::open(const String& path)
{
    return adopt(*new ConfigFile(path));
}

ConfigFile::ConfigFile(const String& file_name)
    : m_file_name(file_name)
{
    reparse();
}

ConfigFile::~ConfigFile()
{
    sync();
}

void ConfigFile::reparse()
{
    m_groups.clear();

    auto file = File::construct(m_file_name);
    if (!file->open(IODevice::OpenMode::ReadOnly))
        return;

    HashMap<String, String>* current_group = nullptr;

    while (file->can_read_line()) {
        auto line = file->read_line(BUFSIZ);
        auto* cp = (const char*)line.data();

        while (*cp && (*cp == ' ' || *cp == '\t' || *cp == '\n'))
            ++cp;

        switch (*cp) {
        case '\0': // EOL...
        case '#':  // Comment, skip entire line.
        case ';':  // -||-
            continue;
        case '[': { // Start of new group.
            StringBuilder builder;
            ++cp; // Skip the '['
            while (*cp && (*cp != ']'))
                builder.append(*(cp++));
            current_group = &m_groups.ensure(builder.to_string());
            break;
        }
        default: { // Start of key{
            StringBuilder key_builder;
            StringBuilder value_builder;
            while (*cp && (*cp != '='))
                key_builder.append(*(cp++));
            ++cp; // Skip the '='
            while (*cp && (*cp != '\n'))
                value_builder.append(*(cp++));
            if (!current_group) {
                // We're not in a group yet, create one with the name ""...
                current_group = &m_groups.ensure("");
            }
            current_group->set(key_builder.to_string(), value_builder.to_string());
        }
        }
    }
}

String ConfigFile::read_entry(const String& group, const String& key, const String& default_value) const
{
    if (!has_key(group, key)) {
        const_cast<ConfigFile&>(*this).write_entry(group, key, default_value);
        return default_value;
    }
    auto it = m_groups.find(group);
    auto jt = it->value.find(key);
    return jt->value;
}

int ConfigFile::read_num_entry(const String& group, const String& key, int default_value) const
{
    if (!has_key(group, key)) {
        const_cast<ConfigFile&>(*this).write_num_entry(group, key, default_value);
        return default_value;
    }

    bool ok;
    int value = read_entry(group, key).to_uint(ok);
    if (!ok)
        return default_value;
    return value;
}

bool ConfigFile::read_bool_entry(const String& group, const String& key, bool default_value) const
{
    return read_entry(group, key, default_value ? "1" : "0") == "1";
}

void ConfigFile::write_entry(const String& group, const String& key, const String& value)
{
    m_groups.ensure(group).ensure(key) = value;
    m_dirty = true;
}

void ConfigFile::write_num_entry(const String& group, const String& key, int value)
{
    write_entry(group, key, String::number(value));
}
void ConfigFile::write_bool_entry(const String& group, const String& key, bool value)
{
    write_entry(group, key, value ? "1" : "0");
}
void ConfigFile::write_color_entry(const String& group, const String& key, Color value)
{
    write_entry(group, key, String::format("%d,%d,%d,%d", value.red(), value.green(), value.blue(), value.alpha()));
}

bool ConfigFile::sync()
{
    if (!m_dirty)
        return true;

    FILE* fp = fopen(m_file_name.characters(), "wb");
    if (!fp)
        return false;

    for (auto& it : m_groups) {
        fprintf(fp, "[%s]\n", it.key.characters());
        for (auto& jt : it.value)
            fprintf(fp, "%s=%s\n", jt.key.characters(), jt.value.characters());
        fprintf(fp, "\n");
    }

    fclose(fp);

    m_dirty = false;
    return true;
}

void ConfigFile::dump() const
{
    for (auto& it : m_groups) {
        printf("[%s]\n", it.key.characters());
        for (auto& jt : it.value)
            printf("%s=%s\n", jt.key.characters(), jt.value.characters());
        printf("\n");
    }
}

Vector<String> ConfigFile::groups() const
{
    return m_groups.keys();
}

Vector<String> ConfigFile::keys(const String& group) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.keys();
}

bool ConfigFile::has_key(const String& group, const String& key) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.contains(key);
}

bool ConfigFile::has_group(const String& group) const
{
    return m_groups.contains(group);
}

void ConfigFile::remove_group(const String& group)
{
    m_groups.remove(group);
    m_dirty = true;
}

void ConfigFile::remove_entry(const String& group, const String& key)
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return;
    it->value.remove(key);
    m_dirty = true;
}

}
