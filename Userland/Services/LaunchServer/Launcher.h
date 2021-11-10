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
    void from_executable(Type, const String&);
    String to_details_str() const;
};

class Launcher {
public:
    Launcher();
    static Launcher& the();

    void load_handlers(const String& af_dir = Desktop::AppFile::APP_FILES_DIRECTORY);
    void load_config(const Core::ConfigFile&);
    bool open_url(const URL&, const String& handler_name);
    Vector<String> handlers_for_url(const URL&);
    Vector<String> handlers_with_details_for_url(const URL&);

private:
    HashMap<String, Handler> m_handlers;
    HashMap<String, String> m_protocol_handlers;
    HashMap<String, String> m_file_handlers;

    Handler get_handler_for_executable(Handler::Type, const String&) const;
    void for_each_handler(const String& key, HashMap<String, String>& user_preferences, Function<bool(const Handler&)> f);
    void for_each_handler_for_path(const String&, Function<bool(const Handler&)> f);
    bool open_file_url(const URL&);
    bool open_with_user_preferences(const HashMap<String, String>& user_preferences, const String& key, const Vector<String>& arguments, const String& default_program = {});
    bool open_with_handler_name(const URL&, const String& handler_name);
};
}
