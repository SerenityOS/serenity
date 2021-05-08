/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavefrontOBJLoader.h"
#include <LibCore/File.h>
#include <stdlib.h>

RefPtr<Mesh> WavefrontOBJLoader::load(const String& fname)
{
    auto obj_file_or_error = Core::File::open(fname, Core::IODevice::OpenMode::ReadOnly);
    Vector<Vertex> vertices;
    Vector<Triangle> triangles;

    dbgln("Wavefront: Loading {}...", fname);

    if (obj_file_or_error.is_error())
        return nullptr;

    // Start reading file line by line
    for (auto line = obj_file_or_error.value()->line_begin(); !line.at_end(); ++line) {
        auto object_line = *line;

        // This line describes a vertex (a position in 3D space)
        if (object_line.starts_with("v")) {
            auto vertex_line = object_line.split_view(' ');
            if (vertex_line.size() != 4) {
                dbgln("Wavefront: Malformed vertex line. Aborting.");
                return nullptr;
            }

            vertices.append(
                { static_cast<GLfloat>(atof(String(vertex_line.at(1)).characters())),
                    static_cast<GLfloat>(atof(String(vertex_line.at(2)).characters())),
                    static_cast<GLfloat>(atof(String(vertex_line.at(3)).characters())) });
        }
        // This line describes a face (a collection of 3 vertices, aka a triangle)
        else if (object_line.starts_with("f")) {
            auto face_line = object_line.split_view(' ');
            if (face_line.size() != 4) {
                dbgln("Wavefront: Malformed face line. Aborting.");
                return nullptr;
            }

            // Create a new triangle
            triangles.append(
                {
                    face_line.at(1).to_uint().value() - 1,
                    face_line.at(2).to_uint().value() - 1,
                    face_line.at(3).to_uint().value() - 1,
                });
        }
    }

    dbgln("Wavefront: Done.");
    return adopt_ref(*new Mesh(vertices, triangles));
}
