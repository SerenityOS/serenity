/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Launcher.h"
#include <AK/Function.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/Process.h>
#include <LibDesktop/AppFile.h>
#include <LibFileSystem/FileSystem.h>
#include <errno.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/stat.h>

namespace LaunchServer {

static Launcher* s_the;
static bool spawn(ByteString executable, Vector<ByteString> const& arguments);

ByteString Handler::name_from_executable(StringView executable)
{
    auto separator = executable.find_last('/');
    if (separator.has_value()) {
        auto start = separator.value() + 1;
        return executable.substring_view(start, executable.length() - start);
    }
    return executable;
}

void Handler::from_executable(Type handler_type, ByteString const& executable)
{
    this->handler_type = handler_type;
    this->name = name_from_executable(executable);
    this->executable = executable;
}

ByteString Handler::to_details_str() const
{
    StringBuilder builder;
    auto obj = MUST(JsonObjectSerializer<>::try_create(builder));
    MUST(obj.add("executable"sv, executable));
    MUST(obj.add("name"sv, name));

    auto arguments = MUST(obj.add_array("arguments"sv));
    for (auto const& argument : this->arguments)
        MUST(arguments.add(argument));
    MUST(arguments.finish());

    switch (handler_type) {
    case Type::Application:
        MUST(obj.add("type"sv, "app"));
        break;
    case Type::UserDefault:
        MUST(obj.add("type"sv, "userdefault"));
        break;
    case Type::UserPreferred:
        MUST(obj.add("type"sv, "userpreferred"));
        break;
    default:
        break;
    }

    MUST(obj.finish());
    return builder.to_byte_string();
}

Launcher::Launcher()
{
    VERIFY(s_the == nullptr);
    s_the = this;
}

Launcher& Launcher::the()
{
    VERIFY(s_the);
    return *s_the;
}

void Launcher::load_handlers(ByteString const& af_dir)
{
    Desktop::AppFile::for_each([&](auto af) {
        auto app_name = af->name();
        auto app_executable = af->executable();
        auto app_arguments = af->arguments();
        HashTable<ByteString> mime_types;
        for (auto& mime_type : af->launcher_mime_types())
            mime_types.set(mime_type);
        HashTable<ByteString> file_types;
        for (auto& file_type : af->launcher_file_types())
            file_types.set(file_type);
        HashTable<ByteString> protocols;
        for (auto& protocol : af->launcher_protocols())
            protocols.set(protocol);
        if (access(app_executable.characters(), X_OK) == 0)
            m_handlers.set(app_executable, { Handler::Type::Default, app_name, app_executable, move(app_arguments), mime_types, file_types, protocols });
    },
        af_dir);
}

void Launcher::load_config(Core::ConfigFile const& cfg)
{
    for (auto key : cfg.keys("MimeType")) {
        auto handler = cfg.read_entry("MimeType", key).trim_whitespace();
        if (handler.is_empty())
            continue;
        if (access(handler.characters(), X_OK) != 0)
            continue;
        m_mime_handlers.set(key.to_lowercase(), handler);
    }

    for (auto key : cfg.keys("FileType")) {
        auto handler = cfg.read_entry("FileType", key).trim_whitespace();
        if (handler.is_empty())
            continue;
        if (access(handler.characters(), X_OK) != 0)
            continue;
        m_file_handlers.set(key.to_lowercase(), handler);
    }

    for (auto key : cfg.keys("Protocol")) {
        auto handler = cfg.read_entry("Protocol", key).trim_whitespace();
        if (handler.is_empty())
            continue;
        if (access(handler.characters(), X_OK) != 0)
            continue;
        m_protocol_handlers.set(key.to_lowercase(), handler);
    }
}

bool Launcher::has_mime_handlers(ByteString const& mime_type)
{
    for (auto& handler : m_handlers)
        if (handler.value.mime_types.contains(mime_type))
            return true;
    return false;
}

Vector<ByteString> Launcher::handlers_for_url(const URL::URL& url)
{
    Vector<ByteString> handlers;
    if (url.scheme() == "file") {
        for_each_handler_for_path(URL::percent_decode(url.serialize_path()), [&](auto& handler) -> bool {
            handlers.append(handler.executable);
            return true;
        });
    } else {
        for_each_handler(url.scheme().to_byte_string(), m_protocol_handlers, [&](auto const& handler) -> bool {
            if (handler.handler_type != Handler::Type::Default || handler.protocols.contains(url.scheme().to_byte_string())) {
                handlers.append(handler.executable);
                return true;
            }
            return false;
        });
    }
    return handlers;
}

Vector<ByteString> Launcher::handlers_with_details_for_url(const URL::URL& url)
{
    Vector<ByteString> handlers;
    if (url.scheme() == "file") {
        for_each_handler_for_path(URL::percent_decode(url.serialize_path()), [&](auto& handler) -> bool {
            handlers.append(handler.to_details_str());
            return true;
        });
    } else {
        for_each_handler(url.scheme().to_byte_string(), m_protocol_handlers, [&](auto const& handler) -> bool {
            if (handler.handler_type != Handler::Type::Default || handler.protocols.contains(url.scheme().to_byte_string())) {
                handlers.append(handler.to_details_str());
                return true;
            }
            return false;
        });
    }
    return handlers;
}

Optional<StringView> Launcher::mime_type_for_file(ByteString path)
{
    auto file_or_error = Core::File::open(path, Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        return {};

    return Core::guess_mime_type_based_on_sniffed_bytes(*file_or_error.release_value());
}

bool Launcher::open_url(const URL::URL& url, ByteString const& handler_name)
{
    if (!handler_name.is_empty())
        return open_with_handler_name(url, handler_name);

    if (url.scheme() == "file")
        return open_file_url(url);

    return open_with_user_preferences(m_protocol_handlers, url.scheme().to_byte_string(), { url.to_byte_string() });
}

bool Launcher::open_with_handler_name(const URL::URL& url, ByteString const& handler_name)
{
    auto handler_optional = m_handlers.get(handler_name);
    if (!handler_optional.has_value())
        return false;

    auto& handler = handler_optional.value();
    ByteString argument;
    if (url.scheme() == "file")
        argument = URL::percent_decode(url.serialize_path());
    else
        argument = url.to_byte_string();
    return spawn(handler.executable, { argument });
}

bool spawn(ByteString executable, Vector<ByteString> const& arguments)
{
    return !Core::Process::spawn(executable, arguments).is_error();
}

Handler Launcher::get_handler_for_executable(Handler::Type handler_type, ByteString const& executable) const
{
    Handler handler;
    auto existing_handler = m_handlers.get(executable);
    if (existing_handler.has_value()) {
        handler = existing_handler.value();
        handler.handler_type = handler_type;
    } else {
        handler.from_executable(handler_type, executable);
    }
    return handler;
}

bool Launcher::open_with_user_preferences(HashMap<ByteString, ByteString> const& user_preferences, ByteString const& key, Vector<ByteString> const& arguments, ByteString const& default_program)
{
    auto program_path = user_preferences.get(key);
    if (program_path.has_value())
        return spawn(program_path.value(), arguments);

    ByteString executable = "";
    if (for_each_handler(key, user_preferences, [&](auto const& handler) -> bool {
            if (executable.is_empty() && (handler.mime_types.contains(key) || handler.file_types.contains(key) || handler.protocols.contains(key))) {
                executable = handler.executable;
                return true;
            }
            return false;
        })) {
        return spawn(executable, arguments);
    }

    // There wasn't a handler for this, so try the fallback instead
    program_path = user_preferences.get("*");
    if (program_path.has_value())
        return spawn(program_path.value(), arguments);

    // Absolute worst case, try the provided default program, if any
    if (!default_program.is_empty())
        return spawn(default_program, arguments);

    return false;
}

size_t Launcher::for_each_handler(ByteString const& key, HashMap<ByteString, ByteString> const& user_preference, Function<bool(Handler const&)> f)
{
    auto user_preferred = user_preference.get(key);
    if (user_preferred.has_value())
        f(get_handler_for_executable(Handler::Type::UserPreferred, user_preferred.value()));

    size_t counted = 0;
    for (auto& handler : m_handlers) {
        // Skip over the existing item in the list
        if (user_preferred.has_value() && user_preferred.value() == handler.value.executable)
            continue;
        if (f(handler.value))
            counted++;
    }

    auto user_default = user_preference.get("*");
    if (counted == 0 && user_default.has_value())
        f(get_handler_for_executable(Handler::Type::UserDefault, user_default.value()));
    // Return the number of times f() was called,
    // which can be used to know whether there were any handlers
    return counted;
}

void Launcher::for_each_handler_for_path(ByteString const& path, Function<bool(Handler const&)> f)
{
    struct stat st;
    if (lstat(path.characters(), &st) < 0) {
        perror("lstat");
        return;
    }

    if (S_ISDIR(st.st_mode)) {
        auto handler_optional = m_file_handlers.get("directory");
        if (!handler_optional.has_value())
            return;
        auto& handler = handler_optional.value();
        f(get_handler_for_executable(Handler::Type::Default, handler));
        return;
    }

    if (!S_ISREG(st.st_mode) && !S_ISLNK(st.st_mode))
        return;

    if (S_ISLNK(st.st_mode)) {
        auto link_target_or_error = FileSystem::read_link(path);
        if (link_target_or_error.is_error()) {
            perror("read_link");
            return;
        }

        auto link_target = LexicalPath { link_target_or_error.release_value() };
        LexicalPath absolute_link_target = link_target.is_absolute() ? link_target : LexicalPath::join(LexicalPath::dirname(path), link_target.string());
        auto real_path_or_error = FileSystem::real_path(absolute_link_target.string());
        if (real_path_or_error.is_error()) {
            perror("real_path");
            return;
        }
        auto real_path = real_path_or_error.release_value();

        return for_each_handler_for_path(real_path, [&](auto const& handler) -> bool {
            return f(handler);
        });
    }

    if ((st.st_mode & S_IFMT) == S_IFREG && (st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH)))
        f(get_handler_for_executable(Handler::Type::Application, path));

    auto extension = LexicalPath::extension(path).to_lowercase();
    auto mime_type = mime_type_for_file(path);

    if (mime_type.has_value()) {
        if (for_each_handler(mime_type.value(), m_mime_handlers, [&](auto const& handler) -> bool {
                if (handler.handler_type != Handler::Type::Default || handler.mime_types.contains(mime_type.value()))
                    return f(handler);
                return false;
            })) {
            return;
        }
    }

    for_each_handler(extension, m_file_handlers, [&](auto const& handler) -> bool {
        if (handler.handler_type != Handler::Type::Default || handler.file_types.contains(extension))
            return f(handler);
        return false;
    });
}

bool Launcher::open_file_url(const URL::URL& url)
{
    struct stat st;
    auto file_path = URL::percent_decode(url.serialize_path());
    if (stat(file_path.characters(), &st) < 0) {
        perror("stat");
        return false;
    }

    if (S_ISDIR(st.st_mode)) {
        Vector<ByteString> fm_arguments;
        if (!url.fragment().has_value() || url.fragment()->is_empty()) {
            fm_arguments.append(file_path);
        } else {
            fm_arguments.append("-s");
            fm_arguments.append("-r");
            fm_arguments.append(ByteString::formatted("{}/{}", file_path, *url.fragment()));
        }

        auto handler_optional = m_file_handlers.get("directory");
        if (!handler_optional.has_value())
            return false;
        auto& handler = handler_optional.value();

        return spawn(handler, fm_arguments);
    }

    if ((st.st_mode & S_IFMT) == S_IFREG && st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return spawn(file_path, {});

    auto extension = LexicalPath::extension(file_path).to_lowercase();
    auto mime_type = mime_type_for_file(file_path);

    auto mime_type_or_extension = extension;
    bool should_use_mime_type = mime_type.has_value() && has_mime_handlers(mime_type.value());
    if (should_use_mime_type)
        mime_type_or_extension = mime_type.value();

    auto handler_optional = m_file_handlers.get("txt");
    ByteString default_handler = "";
    if (handler_optional.has_value())
        default_handler = handler_optional.value();

    // Additional parameters parsing, specific for the file protocol and txt file handlers
    Vector<ByteString> additional_parameters;
    ByteString filepath = URL::percent_decode(url.serialize_path());

    if (url.query().has_value()) {
        url.query()->bytes_as_string_view().for_each_split_view('&', SplitBehavior::Nothing, [&](auto parameter) {
            auto pair = parameter.split_view('=');
            if (pair.size() == 2 && pair[0] == "line_number") {
                auto line = pair[1].template to_number<int>();
                if (line.has_value())
                    // TextEditor uses file:line:col to open a file at a specific line number
                    filepath = ByteString::formatted("{}:{}", filepath, line.value());
            }
        });
    }

    additional_parameters.append(filepath);

    return open_with_user_preferences(should_use_mime_type ? m_mime_handlers : m_file_handlers, mime_type_or_extension, additional_parameters, default_handler);
}
}
