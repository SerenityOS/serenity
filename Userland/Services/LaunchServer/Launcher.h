/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/URL.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/AppFile.h>

namespace LaunchServer {

struct Handler {
    enum class Type {
        Default = 0,
        Application,
        UserPreferred,
        UserDefault
    };
    Type handler_type;
    DeprecatedString name;
    DeprecatedString executable;
    HashTable<DeprecatedString> mime_types {};
    HashTable<DeprecatedString> file_types {};
    HashTable<DeprecatedString> protocols {};

    static DeprecatedString name_from_executable(StringView);
    void from_executable(Type, DeprecatedString const&);
    DeprecatedString to_details_str() const;
};

class Launcher {
public:
    Launcher();
    static Launcher& the();

    void load_handlers(DeprecatedString const& af_dir = Desktop::AppFile::APP_FILES_DIRECTORY);
    void load_config(Core::ConfigFile const&);
    bool open_url(const URL&, DeprecatedString const& handler_name);
    Vector<DeprecatedString> handlers_for_url(const URL&);
    Vector<DeprecatedString> handlers_with_details_for_url(const URL&);

private:
    HashMap<DeprecatedString, Handler> m_handlers;
    HashMap<DeprecatedString, DeprecatedString> m_protocol_handlers;
    HashMap<DeprecatedString, DeprecatedString> m_file_handlers;
    HashMap<DeprecatedString, DeprecatedString> m_mime_handlers;

    bool has_mime_handlers(DeprecatedString const&);
    Optional<DeprecatedString> mime_type_for_file(DeprecatedString path);
    Handler get_handler_for_executable(Handler::Type, DeprecatedString const&) const;
    size_t for_each_handler(DeprecatedString const& key, HashMap<DeprecatedString, DeprecatedString> const& user_preferences, Function<bool(Handler const&)> f);
    void for_each_handler_for_path(DeprecatedString const&, Function<bool(Handler const&)> f);
    bool open_file_url(const URL&);
    bool open_with_user_preferences(HashMap<DeprecatedString, DeprecatedString> const& user_preferences, DeprecatedString const& key, Vector<DeprecatedString> const& arguments, DeprecatedString const& default_program = {});
    bool open_with_handler_name(const URL&, DeprecatedString const& handler_name);
};
}
