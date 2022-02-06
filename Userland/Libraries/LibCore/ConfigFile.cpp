/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/StandardPaths.h>
#include <pwd.h>

namespace Core {

ErrorOr<NonnullRefPtr<ConfigFile>> ConfigFile::open_for_lib(String const& lib_name, AllowWriting allow_altering)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/lib/{}.ini", directory, lib_name);
    return ConfigFile::open(path, allow_altering);
}

ErrorOr<NonnullRefPtr<ConfigFile>> ConfigFile::open_for_app(String const& app_name, AllowWriting allow_altering)
{
    String directory = StandardPaths::config_directory();
    auto path = String::formatted("{}/{}.ini", directory, app_name);
    return ConfigFile::open(path, allow_altering);
}

ErrorOr<NonnullRefPtr<ConfigFile>> ConfigFile::open_for_system(String const& app_name, AllowWriting allow_altering)
{
    auto path = String::formatted("/etc/{}.ini", app_name);
    return ConfigFile::open(path, allow_altering);
}

ErrorOr<NonnullRefPtr<ConfigFile>> ConfigFile::open(String const& filename, AllowWriting allow_altering)
{
    auto maybe_file = Stream::File::open(filename, allow_altering == AllowWriting::Yes ? Stream::OpenMode::ReadWrite : Stream::OpenMode::Read);
    OwnPtr<Stream::BufferedFile> buffered_file;
    if (maybe_file.is_error()) {
        // If we attempted to open a read-only file that does not exist, we ignore the error, making it appear
        // the same as if we had opened an empty file. This behavior is a little weird, but is required by
        // user code, which does not check the config file exists before opening.
        if (!(allow_altering == AllowWriting::No && maybe_file.error().code() == ENOENT))
            return maybe_file.error();
    } else {
        buffered_file = TRY(Stream::BufferedFile::create(maybe_file.release_value()));
    }

    auto config_file = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ConfigFile(filename, move(buffered_file))));
    TRY(config_file->reparse());
    return config_file;
}

ErrorOr<NonnullRefPtr<ConfigFile>> ConfigFile::open(String const& filename, int fd)
{
    auto file = TRY(Stream::File::adopt_fd(fd, Stream::OpenMode::ReadWrite));
    auto buffered_file = TRY(Stream::BufferedFile::create(move(file)));

    auto config_file = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ConfigFile(filename, move(buffered_file))));
    TRY(config_file->reparse());
    return config_file;
}

ConfigFile::ConfigFile(String const& filename, OwnPtr<Stream::BufferedFile> open_file)
    : m_filename(filename)
    , m_file(move(open_file))
{
}

ConfigFile::~ConfigFile()
{
    MUST(sync());
}

ErrorOr<void> ConfigFile::reparse()
{
    m_groups.clear();
    if (!m_file)
        return {};

    HashMap<String, String>* current_group = nullptr;

    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (TRY(m_file->can_read_line())) {
        auto length = TRY(m_file->read_line(buffer));

        StringView line { buffer.data(), length };
        size_t i = 0;

        while (i < line.length() && (line[i] == ' ' || line[i] == '\t' || line[i] == '\n'))
            ++i;

        if (i >= line.length())
            continue;

        switch (line[i]) {
        case '#': // Comment, skip entire line.
        case ';': // -||-
            continue;
        case '[': { // Start of new group.
            StringBuilder builder;
            ++i; // Skip the '['
            while (i < line.length() && (line[i] != ']')) {
                builder.append(line[i]);
                ++i;
            }
            current_group = &m_groups.ensure(builder.to_string());
            break;
        }
        default: { // Start of key
            StringBuilder key_builder;
            StringBuilder value_builder;
            while (i < line.length() && (line[i] != '=')) {
                key_builder.append(line[i]);
                ++i;
            }
            ++i; // Skip the '='
            while (i < line.length() && (line[i] != '\n')) {
                value_builder.append(line[i]);
                ++i;
            }
            if (!current_group) {
                // We're not in a group yet, create one with the name ""...
                current_group = &m_groups.ensure("");
            }
            auto value_string = value_builder.to_string();
            current_group->set(key_builder.to_string(), value_string.trim_whitespace(TrimMode::Right));
        }
        }
    }
    return {};
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

ErrorOr<void> ConfigFile::sync()
{
    if (!m_dirty)
        return {};

    if (!m_file)
        return Error::from_errno(ENOENT);

    TRY(m_file->truncate(0));
    TRY(m_file->seek(0, Stream::SeekMode::SetPosition));

    for (auto& it : m_groups) {
        TRY(m_file->write(String::formatted("[{}]\n", it.key).bytes()));
        for (auto& jt : it.value)
            TRY(m_file->write(String::formatted("{}={}\n", jt.key, jt.value).bytes()));
        TRY(m_file->write("\n"sv.bytes()));
    }

    m_dirty = false;
    return {};
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
