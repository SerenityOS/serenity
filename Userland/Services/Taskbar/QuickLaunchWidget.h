/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibConfig/Listener.h>
#include <LibCore/FileWatcher.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>

namespace Taskbar {

class QuickLaunchEntry {
public:
    virtual ~QuickLaunchEntry() = default;
    virtual ErrorOr<void> launch() const = 0;
    virtual GUI::Icon icon() const = 0;
    virtual String name() const = 0;
    virtual String file_name_to_watch() const = 0;

    static OwnPtr<QuickLaunchEntry> create_from_config_value(StringView path);
    static OwnPtr<QuickLaunchEntry> create_from_path(StringView path);
};

class QuickLaunchEntryAppFile : public QuickLaunchEntry {
public:
    explicit QuickLaunchEntryAppFile(NonnullRefPtr<Desktop::AppFile> file)
        : m_app_file(move(file))
    {
    }

    virtual ErrorOr<void> launch() const override;
    virtual GUI::Icon icon() const override { return m_app_file->icon(); }
    virtual String name() const override { return m_app_file->name(); }
    virtual String file_name_to_watch() const override { return {}; }

private:
    NonnullRefPtr<Desktop::AppFile> m_app_file;
};

class QuickLaunchEntryExecutable : public QuickLaunchEntry {
public:
    explicit QuickLaunchEntryExecutable(String path)
        : m_path(move(path))
    {
    }

    virtual ErrorOr<void> launch() const override;
    virtual GUI::Icon icon() const override;
    virtual String name() const override;
    virtual String file_name_to_watch() const override { return m_path; }

private:
    String m_path;
};
class QuickLaunchEntryFile : public QuickLaunchEntry {
public:
    explicit QuickLaunchEntryFile(String path)
        : m_path(move(path))
    {
    }

    virtual ErrorOr<void> launch() const override;
    virtual GUI::Icon icon() const override;
    virtual String name() const override;
    virtual String file_name_to_watch() const override { return m_path; }

private:
    String m_path;
};

class QuickLaunchWidget : public GUI::Frame
    , public Config::Listener {
    C_OBJECT(QuickLaunchWidget);

public:
    virtual ~QuickLaunchWidget() override;

    virtual void config_key_was_removed(String const&, String const&, String const&) override;
    virtual void config_string_did_change(String const&, String const&, String const&, String const&) override;

    virtual void drop_event(GUI::DropEvent&) override;

private:
    QuickLaunchWidget();
    void add_or_adjust_button(String const&, NonnullOwnPtr<QuickLaunchEntry>&&);
    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<GUI::Action> m_context_menu_default_action;
    String m_context_menu_app_name;
    RefPtr<Core::FileWatcher> m_watcher;
};

}
