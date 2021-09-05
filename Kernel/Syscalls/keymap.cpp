/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Process.h>

namespace Kernel {

constexpr size_t map_name_max_size = 50;

KResultOr<FlatPtr> Process::sys$setkeymap(Userspace<const Syscall::SC_setkeymap_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(setkeymap);

    if (!is_superuser())
        return EPERM;

    Syscall::SC_setkeymap_params params;
    TRY(copy_from_user(&params, user_params));

    Keyboard::CharacterMapData character_map_data;

    TRY(copy_n_from_user(character_map_data.map, params.map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.shift_map, params.shift_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.alt_map, params.alt_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.altgr_map, params.altgr_map, CHAR_MAP_SIZE));
    TRY(copy_n_from_user(character_map_data.shift_altgr_map, params.shift_altgr_map, CHAR_MAP_SIZE));

    auto map_name = get_syscall_path_argument(params.map_name);
    if (map_name.is_error())
        return map_name.error();
    if (map_name.value()->length() > map_name_max_size)
        return ENAMETOOLONG;

    HIDManagement::the().set_maps(character_map_data, map_name.value()->view());
    return 0;
}

KResultOr<FlatPtr> Process::sys$getkeymap(Userspace<const Syscall::SC_getkeymap_params*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    REQUIRE_PROMISE(getkeymap);

    Syscall::SC_getkeymap_params params {};
    TRY(copy_from_user(&params, user_params));

    String keymap_name = HIDManagement::the().keymap_name();
    const Keyboard::CharacterMapData& character_maps = HIDManagement::the().character_maps();

    TRY(copy_to_user(params.map, character_maps.map, CHAR_MAP_SIZE * sizeof(u32)));
    TRY(copy_to_user(params.shift_map, character_maps.shift_map, CHAR_MAP_SIZE * sizeof(u32)));
    TRY(copy_to_user(params.alt_map, character_maps.alt_map, CHAR_MAP_SIZE * sizeof(u32)));
    TRY(copy_to_user(params.altgr_map, character_maps.altgr_map, CHAR_MAP_SIZE * sizeof(u32)));
    TRY(copy_to_user(params.shift_altgr_map, character_maps.shift_altgr_map, CHAR_MAP_SIZE * sizeof(u32)));

    if (params.map_name.size < keymap_name.length())
        return ENAMETOOLONG;
    TRY(copy_to_user(params.map_name.data, keymap_name.characters(), keymap_name.length()));
    return KSuccess;
}

}
