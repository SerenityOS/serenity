/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibThreeDee/Mesh.h>
#include <LibThreeDee/MeshLoader.h>

namespace ThreeDee {

class WavefrontOBJLoader final : public MeshLoader {
public:
    WavefrontOBJLoader() { }
    ~WavefrontOBJLoader() override { }

    RefPtr<Mesh> load(Core::File& file) override;
};

}
