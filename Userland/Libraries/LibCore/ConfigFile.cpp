/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <pwd.h>
#include <stdio.h>

namespace Core {

NonnullRefPtr<ConfigFile> ConfigFile::open_for_lib(String const& lib_name, AllowWriting allow_altering)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/lib/{}.ini", directory, lib_name);

    return adopt_ref(*new ConfigFile(path, allow_altering));
}

NonnullRefPtr<ConfigFile> ConfigFile::open_for_app(String const& app_name, AllowWriting allow_altering)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/{}.ini", directory, app_name);
    return adopt_ref(*new ConfigFile(path, allow_altering));
}

NonnullRefPtr<ConfigFile> ConfigFile::open_for_system(String const& app_name, AllowWriting allow_altering)
{
    auto path = String::formatted("/etc/{}.ini", app_name);
    return adopt_ref(*new ConfigFile(path, allow_altering));
}

NonnullRefPtr<ConfigFile> ConfigFile::open(String const& filename, AllowWriting allow_altering)
{
    return adopt_ref(*new ConfigFile(filename, allow_altering));
}

NonnullRefPtr<ConfigFile> ConfigFile::open(String const& filename, int fd)
{
    return adopt_ref(*new ConfigFile(filename, fd));
}

ConfigFile::ConfigFile(String const& filename, AllowWriting allow_altering)
    : m_file(File::construct(filename))
{
    if (!m_file->open(allow_altering == AllowWriting::Yes ? OpenMode::ReadWrite : OpenMode::ReadOnly))
        return;

    reparse();
}

ConfigFile::ConfigFile(String const& filename, int fd)
    : m_file(File::construct(filename))
{
    if (!m_file->open(fd, OpenMode::ReadWrite, File::ShouldCloseFileDescriptor::Yes))
        return;

    reparse();
}

ConfigFile::~ConfigFile()
{
    sync();
}

void ConfigFile::reparse()
{
    m_groups.clear();

    HashMap<String, String>* current_group = nullptr;

    while (m_file->can_read_line()) {
        auto line = m_file->read_line();

        if (line.is_null()) {
            m_groups.clear();
            return;
        }

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
            auto value_string = value_builder.to_string();
            current_group->set(key_builder.to_string(), value_string.trim_whitespace(TrimMode::Right));
        }
        }
    }
}

String ConfigFile::read_entry(String const& group, String const& key, String const& default_value) const
{
    if (!has_key(group, key)) {
        return default_value;
    }
    auto it = m_groups.find(group);
    auto jt = it->value.find(key);
    return jt->value;
}

int ConfigFile::read_num_entry(String const& group, String const& key, int default_value) const
{
    if (!has_key(group, key)) {
        return default_value;
    }

    return read_entry(group, key).to_int().value_or(default_value);
}

bool ConfigFile::read_bool_entry(String const& group, String const& key, bool default_value) const
{
    auto value = read_entry(group, key, default_value ? "true" : "false");
    return value == "1" || value.equals_ignoring_case("true"sv);
}

void ConfigFile::write_entry(String const& group, String const& key, String const& value)
{
    m_groups.ensure(group).ensure(key) = value;
    m_dirty = true;
}

void ConfigFile::write_num_entry(String const& group, String const& key, int value)
{
    write_entry(group, key, String::number(value));
}
void ConfigFile::write_bool_entry(String const& group, String const& key, bool value)
{
    write_entry(group, key, value ? "true" : "false");
}
void ConfigFile::write_color_entry(String const& group, String const& key, Color value)
{
    write_entry(group, key, String::formatted("{},{},{},{}", value.red(), value.green(), value.blue(), value.alpha()));
}

bool ConfigFile::sync()
{
    if (!m_dirty)
        return true;

    m_file->truncate(0);
    m_file->seek(0);

    for (auto& it : m_groups) {
        m_file->write(String::formatted("[{}]\n", it.key));
        for (auto& jt : it.value)
            m_file->write(String::formatted("{}={}\n", jt.key, jt.value));
        m_file->write("\n");
    }

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

Vector<String> ConfigFile::keys(String const& group) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.keys();
}

bool ConfigFile::has_key(String const& group, String const& key) const
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return {};
    return it->value.contains(key);
}

bool ConfigFile::has_group(String const& group) const
{
    return m_groups.contains(group);
}

void ConfigFile::remove_group(String const& group)
{
    m_groups.remove(group);
    m_dirty = true;
}

void ConfigFile::remove_entry(String const& group, String const& key)
{
    auto it = m_groups.find(group);
    if (it == m_groups.end())
        return;
    it->value.remove(key);
    m_dirty = true;
}

}
