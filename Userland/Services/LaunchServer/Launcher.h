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
    String name;
    String executable;
    HashTable<String> file_types {};
    HashTable<String> protocols {};

    static String name_from_executable(StringView);
    void from_executable(Type, String const&);
    String to_details_str() const;
};

class Launcher {
public:
    Launcher();
    static Launcher& the();

    void load_handlers(String const& af_dir = Desktop::AppFile::APP_FILES_DIRECTORY);
    void load_config(Core::ConfigFile const&);
    bool open_url(const URL&, String const& handler_name);
    Vector<String> handlers_for_url(const URL&);
    Vector<String> handlers_with_details_for_url(const URL&);

private:
    HashMap<String, Handler> m_handlers;
    HashMap<String, String> m_protocol_handlers;
    HashMap<String, String> m_file_handlers;

    Handler get_handler_for_executable(Handler::Type, String const&) const;
    void for_each_handler(String const& key, HashMap<String, String>& user_preferences, Function<bool(Handler const&)> f);
    void for_each_handler_for_path(String const&, Function<bool(Handler const&)> f);
    bool open_file_url(const URL&);
    bool open_with_user_preferences(HashMap<String, String> const& user_preferences, String const& key, Vector<String> const& arguments, String const& default_program = {});
    bool open_with_handler_name(const URL&, String const& handler_name);
};
}
