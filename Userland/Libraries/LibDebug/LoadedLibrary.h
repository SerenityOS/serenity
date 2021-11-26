/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DebugInfo.h"
#include <AK/Types.h>
#include <LibCore/MappedFile.h>
#include <LibELF/Image.h>

namespace Debug {
struct LoadedLibrary {
    String name;
    NonnullRefPtr<Core::MappedFile> file;
    NonnullOwnPtr<ELF::Image> image;
    NonnullOwnPtr<DebugInfo> debug_info;
    FlatPtr base_address {};

    LoadedLibrary(String const& name, NonnullRefPtr<Core::MappedFile> file, NonnullOwnPtr<ELF::Image> image, NonnullOwnPtr<DebugInfo>&& debug_info, FlatPtr base_address)
        : name(name)
        , file(move(file))
        , image(move(image))
        , debug_info(move(debug_info))
        , base_address(base_address)
    {
    }
};

}
