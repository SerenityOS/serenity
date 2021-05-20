/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavefrontOBJLoader.h"
#include <LibCore/File.h>
#include <stdlib.h>

RefPtr<Mesh> WavefrontOBJLoader::load(Core::File& file)
{
    Vector<Vertex> vertices;
    Vector<Triangle> triangles;

    dbgln("Wavefront: Loading {}...", file.name());

    // Start reading file line by line
    for (auto line = file.line_begin(); !line.at_end(); ++line) {
        auto object_line = *line;

        // FIXME: Parse texture coordinates and vertex normals
        if (object_line.starts_with("vt") || object_line.starts_with("vn")) {
            continue;
        }

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

            if (object_line.contains("/")) {
                for (int i = 1; i <= 3; ++i) {
                    face_line.at(i) = face_line.at(i).split_view("/").at(0);
                }
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

    if (vertices.is_empty()) {
        dbgln("Wavefront: Failed to read any data from 3D file: {}", file.name());
        return nullptr;
    }

    dbgln("Wavefront: Done.");
    return adopt_ref(*new Mesh(vertices, triangles));
}
