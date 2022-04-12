/*
 * Copyright (c) 2022, Thitat Auareesuksakul <thitat@flux.ci>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Keyboard {

class Keymap {

public:
    Keymap(StringView const& keymap_name);
    static ErrorOr<Vector<Keymap>> read_all();

    String const& name() const { return m_name; }

    static char const* config_file_path() { return "/etc/Keyboard.ini"; }

private:
    String m_name;
};

}
