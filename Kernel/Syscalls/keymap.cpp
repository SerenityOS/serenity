/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

constexpr size_t map_name_max_size = 50;

ErrorOr<FlatPtr> Process::sys$setkeymap(Userspace<Syscall::SC_setkeymap_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::setkeymap));

    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));

    Keyboard::CharacterMapData character_map_data;

    TRY(copy_n_from_user(character_map_data.map, params.map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.shift_map, params.shift_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.alt_map, params.alt_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.altgr_map, params.altgr_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.shift_altgr_map, params.shift_altgr_map, CHAR_MAP_SIZE));

    auto map_name = TRY(get_syscall_path_argument(params.map_name));
    if (map_name->length() > map_name_max_size)
        return ENAMETOOLONG;

    InputManagement::the().set_maps(move(map_name), character_map_data);
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getkeymap(Userspace<Syscall::SC_getkeymap_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::getkeymap));
    auto params = TRY(copy_typed_from_user(user_params));

    return InputManagement::the().keymap_data().with([&](auto const& keymap_data) -> ErrorOr<FlatPtr> {
        if (params.map_name.size < keymap_data.character_map_name->length())
            return ENAMETOOLONG;
        TRY(copy_to_user(params.map_name.data, keymap_data.character_map_name->characters(), keymap_data.character_map_name->length()));

        auto const& character_maps = keymap_data.character_map;
        TRY(copy_n_to_user(params.map, character_maps.map, CHAR_MAP_SIZE));
        TRY(copy_n_to_user(params.shift_map, character_maps.shift_map, CHAR_MAP_SIZE));
        TRY(copy_n_to_user(params.alt_map, character_maps.alt_map, CHAR_MAP_SIZE));
        TRY(copy_n_to_user(params.altgr_map, character_maps.altgr_map, CHAR_MAP_SIZE));
        TRY(copy_n_to_user(params.shift_altgr_map, character_maps.shift_altgr_map, CHAR_MAP_SIZE));
        return 0;
    });
}

}
