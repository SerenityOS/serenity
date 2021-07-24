/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/URL.h>
#include <LibCore/FileWatcher.h>
#include <LibDesktop/AppFile.h>

namespace LaunchServer {

struct Handler : public RefCounted<Handler> {
    enum class Type {
        Default = 0,
        Application,
        UserPreferred,
        UserDefault
    };

    Handler(Type handler_type, const String& name, const String& executable, bool run_in_terminal = false)
        : handler_type { handler_type }
        , name { name }
        , executable { executable }
        , run_in_terminal { run_in_terminal }
    {
    }

    Type handler_type;
    String name;
    String executable;
    bool run_in_terminal { false };

    String to_details_str() const;
    static String to_details_str(Type handler_type, const String& name, const String& executable, bool run_in_terminal = false);
};

class Launcher {
public:
    Launcher(const String& app_file_dir = Desktop::AppFile::APP_FILES_DIRECTORY);
    static Launcher& the();

    bool open_url(const URL&, const String& handler_name);
    Vector<String> handlers_for_url(const URL&);
    Vector<String> handlers_with_details_for_url(const URL&);

private:
    void load_default_handlers();
    void load_config();

    bool open_file_url(const URL&);
    bool open_with_handler_name(const URL&, const String& executable);

    Vector<String> get_handlers(const URL&, bool with_details = false) const;
    bool run_in_terminal(const String& path) const;
    String get_name_for_executable(const String& executable) const;

    String m_app_file_dir;
    RefPtr<Core::FileWatcher> m_config_file_watcher;
    HashMap<String, NonnullRefPtr<Handler>> m_default_file_handlers;
    HashMap<String, NonnullRefPtr<Handler>> m_default_protocol_handlers;
    HashMap<String, NonnullRefPtr<Handler>> m_file_handlers;
    HashMap<String, NonnullRefPtr<Handler>> m_protocol_handlers;
};
}
