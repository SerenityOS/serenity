/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/File.h>
#include <LibThreeDee/Mesh.h>

namespace ThreeDee {

class MeshLoader {
public:
    MeshLoader() { }
    virtual ~MeshLoader() { }

    virtual RefPtr<Mesh> load(Core::File& file) = 0;
};

}
