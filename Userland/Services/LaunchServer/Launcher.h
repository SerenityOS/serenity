/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/AppFile.h>
#include <LibURL/URL.h>

namespace LaunchServer {

struct Handler {
    enum class Type {
        Default = 0,
        Application,
        UserPreferred,
        UserDefault
    };
    Type handler_type;
    ByteString name;
    ByteString executable;
    Vector<ByteString> arguments;
    HashTable<ByteString> mime_types {};
    HashTable<ByteString> file_types {};
    HashTable<ByteString> protocols {};

    static ByteString name_from_executable(StringView);
    void from_executable(Type, ByteString const&);
    ByteString to_details_str() const;
};

class Launcher {
public:
    Launcher();
    static Launcher& the();

    void load_handlers(ByteString const& af_dir = Desktop::AppFile::APP_FILES_DIRECTORY);
    void load_config(Core::ConfigFile const&);
    bool open_url(const URL::URL&, ByteString const& handler_name);
    Vector<ByteString> handlers_for_url(const URL::URL&);
    Vector<ByteString> handlers_with_details_for_url(const URL::URL&);

private:
    HashMap<ByteString, Handler> m_handlers;
    HashMap<ByteString, ByteString> m_protocol_handlers;
    HashMap<ByteString, ByteString> m_file_handlers;
    HashMap<ByteString, ByteString> m_mime_handlers;

    bool has_mime_handlers(ByteString const&);
    Optional<StringView> mime_type_for_file(ByteString path);
    Handler get_handler_for_executable(Handler::Type, ByteString const&) const;
    size_t for_each_handler(ByteString const& key, HashMap<ByteString, ByteString> const& user_preferences, Function<bool(Handler const&)> f);
    void for_each_handler_for_path(ByteString const&, Function<bool(Handler const&)> f);
    bool open_file_url(const URL::URL&);
    bool open_with_user_preferences(HashMap<ByteString, ByteString> const& user_preferences, ByteString const& key, Vector<ByteString> const& arguments, ByteString const& default_program = {});
    bool open_with_handler_name(const URL::URL&, ByteString const& handler_name);
};
}
