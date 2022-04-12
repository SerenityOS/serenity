/*
 * Copyright (c) 2022, Thitat Auareesuksakul <thitat@flux.ci>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Keymap.h"
#include <AK/Try.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>

namespace Keyboard {

Keymap::Keymap(StringView const& keymap_name)
    : m_name(keymap_name)
{
}

ErrorOr<Vector<Keymap>> Keymap::read_all()
{
    auto mapper_config = TRY(Core::ConfigFile::open("/etc/Keyboard.ini"));
    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps", "");

    auto keymap_names = keymaps.split_view(',');
    if (keymap_names.size() == 0)
        return Error::from_string_literal("Empty list of keymaps");

    auto keymap_vector = Vector<Keymap>();
    keymap_vector.ensure_capacity(keymap_names.size());

    for (StringView const& name : keymap_names) {
        keymap_vector.append(Keymap(name));
    }

    return keymap_vector;
}

}
