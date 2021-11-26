/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
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
    WavefrontOBJLoader() { }
    ~WavefrontOBJLoader() override { }

    RefPtr<Mesh> load(Core::File& file) override;
};
