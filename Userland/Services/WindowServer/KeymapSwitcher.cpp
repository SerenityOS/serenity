/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <WindowServer/KeymapSwitcher.h>
#include <spawn.h>
#include <unistd.h>

namespace WindowServer {

KeymapSwitcher::KeymapSwitcher()
{
    m_file_watcher = MUST(Core::FileWatcher::create());

    m_file_watcher->on_change = [this](auto&) {
        refresh();
    };

    MUST(m_file_watcher->add_watch(m_keyboard_config, Core::FileWatcherEvent::Type::ContentModified));

    refresh();
}

KeymapSwitcher::~KeymapSwitcher()
{
}

void KeymapSwitcher::refresh()
{
    m_keymaps.clear();

    auto mapper_config(Core::ConfigFile::open(m_keyboard_config));
    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps", "");

    auto keymaps_vector = keymaps.split(',');

    for (auto& keymap : keymaps_vector) {
        m_keymaps.append(keymap);
    }

    if (m_keymaps.is_empty()) {
        dbgln("Empty list of keymaps - adding default (en-us)");
        m_keymaps.append("en-us");
    }

    auto current_keymap = get_current_keymap();

    // Refresh might indicate that some external program has changed the keymap,
    // so better notify our clients that we may have a new keymap
    if (on_keymap_change)
        on_keymap_change(current_keymap);

    if (m_keymaps.find(current_keymap).is_end()) {
        setkeymap(m_keymaps.first());
    }
}

void KeymapSwitcher::next_keymap()
{
    if (m_keymaps.is_empty()) {
        dbgln("No keymaps loaded - leaving system keymap unchanged");
        return; // TODO: figure out what to do when there is no keymap configured
    }

    auto current_keymap_name = get_current_keymap();

    dbgln("Current system keymap: {}", current_keymap_name);

    auto it = m_keymaps.find_if([&](const auto& enumerator) {
        return enumerator == current_keymap_name;
    });

    if (it.is_end()) {
        auto first_keymap = m_keymaps.first();
        dbgln("Cannot find current keymap in the keymap list - setting first available ({})", first_keymap);
        setkeymap(first_keymap);
    } else {
        it++;

        if (it.is_end()) {
            it = m_keymaps.begin();
        }

        dbgln("Setting system keymap to: {}", *it);
        setkeymap(*it);
    }
}

String KeymapSwitcher::get_current_keymap() const
{
    auto proc_keymap = Core::File::construct("/proc/keymap");
    if (!proc_keymap->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto json = JsonValue::from_string(proc_keymap->read_all()).release_value_but_fixme_should_propagate_errors();
    auto const& keymap_object = json.as_object();
    VERIFY(keymap_object.has("keymap"));
    return keymap_object.get("keymap").to_string();
}

void KeymapSwitcher::setkeymap(const AK::String& keymap)
{
    pid_t child_pid;
    const char* argv[] = { "/bin/keymap", "-m", keymap.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        dbgln("Failed to call /bin/keymap, error: {} ({})", errno, strerror(errno));
    }
    if (on_keymap_change)
        on_keymap_change(keymap);
}

}
