/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>

#include "Mesh.h"
#include "MeshLoader.h"

class WavefrontOBJLoader final : public MeshLoader {
public:
    WavefrontOBJLoader() = default;
    ~WavefrontOBJLoader() override = default;

    ErrorOr<NonnullRefPtr<Mesh>> load(ByteString const& filename, NonnullOwnPtr<Core::File> file) override;
};
