/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

namespace Core {

NonnullRefPtr<ConfigFile> ConfigFile::get_for_lib(const String& lib_name)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/lib/{}.ini", directory, lib_name);

    return adopt_ref(*new ConfigFile(path));
}

NonnullRefPtr<ConfigFile> ConfigFile::get_for_app(const String& app_name)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/{}.ini", directory, app_name);
    return adopt_ref(*new ConfigFile(path));
}

NonnullRefPtr<ConfigFile> ConfigFile::get_for_system(const String& app_name)
{
    auto path = String::formatted("/etc/{}.ini", app_name);
    return adopt_ref(*new ConfigFile(path));
}

NonnullRefPtr<ConfigFile> ConfigFile::open(const String& path)
{
    return adopt_ref(*new ConfigFile(path));
}

ConfigFile::ConfigFile(const String& filename)
    : m_filename(filename)
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

    auto file = File::construct(m_filename);
    if (!file->open(OpenMode::ReadOnly))
        return;

    HashMap<String, String>* current_group = nullptr;

    while (file->can_read_line()) {
        auto line = file->read_line();
        auto* cp = line.characters();

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
        return default_value;
    }
    auto it = m_groups.find(group);
    auto jt = it->value.find(key);
    return jt->value;
}

int ConfigFile::read_num_entry(const String& group, const String& key, int default_value) const
{
    if (!has_key(group, key)) {
        return default_value;
    }

    return read_entry(group, key).to_int().value_or(default_value);
}

bool ConfigFile::read_bool_entry(const String& group, const String& key, bool default_value) const
{
    auto value = read_entry(group, key, default_value ? "1" : "0");
    if (value == "1" || value.to_lowercase() == "true")
        return 1;
    return 0;
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
    write_entry(group, key, String::formatted("{},{},{},{}", value.red(), value.green(), value.blue(), value.alpha()));
}

bool ConfigFile::sync()
{
    if (!m_dirty)
        return true;

    FILE* fp = fopen(m_filename.characters(), "wb");
    if (!fp)
        return false;

    for (auto& it : m_groups) {
        outln(fp, "[{}]", it.key);
        for (auto& jt : it.value)
            outln(fp, "{}={}", jt.key, jt.value);
        outln(fp);
    }

    fclose(fp);

    m_dirty = false;
    return true;
}

void ConfigFile::dump() const
{
    for (auto& it : m_groups) {
        outln("[{}]", it.key);
        for (auto& jt : it.value)
            outln("{}={}", jt.key, jt.value);
        outln();
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
