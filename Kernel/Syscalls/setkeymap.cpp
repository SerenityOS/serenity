/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$setkeymap(Userspace<const Syscall::SC_setkeymap_params*> user_params)
{
    REQUIRE_PROMISE(setkeymap);

    if (!is_superuser())
        return -EPERM;

    Syscall::SC_setkeymap_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    Keyboard::CharacterMapData character_map_data;

    if (!copy_from_user(character_map_data.map, params.map, CHAR_MAP_SIZE * sizeof(u32)))
        return -EFAULT;
    if (!copy_from_user(character_map_data.shift_map, params.shift_map, CHAR_MAP_SIZE * sizeof(u32)))
        return -EFAULT;
    if (!copy_from_user(character_map_data.alt_map, params.alt_map, CHAR_MAP_SIZE * sizeof(u32)))
        return -EFAULT;
    if (!copy_from_user(character_map_data.altgr_map, params.altgr_map, CHAR_MAP_SIZE * sizeof(u32)))
        return -EFAULT;

    auto map_name = get_syscall_path_argument(params.map_name);
    if (map_name.is_error()) {
        return map_name.error();
    }
    constexpr size_t map_name_max_size = 50;
    if (map_name.value().length() > map_name_max_size) {
        return -ENAMETOOLONG;
    }

    KeyboardDevice::the().set_maps(character_map_data, map_name.value());
    return 0;
}

}
