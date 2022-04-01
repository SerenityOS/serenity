/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Keyboard {

class CharacterMapFile {

public:
    static ErrorOr<CharacterMapData> load_from_file(String const& filename);

private:
    static Vector<u32> read_map(JsonObject const& json, String const& name);
};

}
