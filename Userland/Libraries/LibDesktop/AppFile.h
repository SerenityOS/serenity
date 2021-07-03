/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ConfigFile.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>

namespace Desktop {

class AppFile : public RefCounted<AppFile> {
public:
    static constexpr const char* APP_FILES_DIRECTORY = "/res/apps";
    static NonnullRefPtr<AppFile> get_for_app(const StringView& app_name);
    static NonnullRefPtr<AppFile> open(const StringView& path);
    static void for_each(Function<void(NonnullRefPtr<AppFile>)>, const StringView& directory = APP_FILES_DIRECTORY);
    ~AppFile();

    bool is_valid() const { return m_valid; }
    String filename() const { return m_config->filename(); }

    String name() const;
    String executable() const;
    String category() const;
    Vector<String> launcher_file_types() const;
    Vector<String> launcher_protocols() const;
    bool spawn() const;

    GUI::Icon icon() const { return GUI::FileIconProvider::icon_for_path(executable()); };

private:
    explicit AppFile(const StringView& path);

    bool validate() const;

    RefPtr<Core::ConfigFile> m_config;
    bool m_valid { false };
};

}
