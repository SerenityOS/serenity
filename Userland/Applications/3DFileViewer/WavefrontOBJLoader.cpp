/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavefrontOBJLoader.h"
#include <LibCore/File.h>
#include <stdlib.h>

RefPtr<Mesh> WavefrontOBJLoader::load(Core::File& file)
{
    Vector<Vertex> vertices;
    Vector<Vertex> normals;
    Vector<TexCoord> tex_coords;
    Vector<Triangle> triangles;

    dbgln("Wavefront: Loading {}...", file.name());

    // Start reading file line by line
    for (auto object_line : file.lines()) {
        // Ignore file comments
        if (object_line.starts_with("#"))
            continue;

        if (object_line.starts_with("vt")) {
            auto tex_coord_line = object_line.split_view(' ');
            if (tex_coord_line.size() != 3) {
                dbgln("Wavefront: Malformed TexCoord line. Aborting.");
                dbgln("{}", object_line);
                return nullptr;
            }

            tex_coords.append({ static_cast<GLfloat>(atof(String(tex_coord_line.at(1)).characters())),
                static_cast<GLfloat>(atof(String(tex_coord_line.at(2)).characters())) });

            continue;
        }

        if (object_line.starts_with("vn")) {
            auto normal_line = object_line.split_view(' ');
            if (normal_line.size() != 4) {
                dbgln("Wavefront: Malformed vertex normal line. Aborting.");
                return nullptr;
            }

            normals.append({ static_cast<GLfloat>(atof(String(normal_line.at(1)).characters())),
                static_cast<GLfloat>(atof(String(normal_line.at(2)).characters())),
                static_cast<GLfloat>(atof(String(normal_line.at(3)).characters())) });

            continue;
        }

        // This line describes a vertex (a position in 3D space)
        if (object_line.starts_with("v")) {
            auto vertex_line = object_line.split_view(' ');
            if (vertex_line.size() != 4) {
                dbgln("Wavefront: Malformed vertex line. Aborting.");
                return nullptr;
            }

            vertices.append({ static_cast<GLfloat>(atof(String(vertex_line.at(1)).characters())),
                static_cast<GLfloat>(atof(String(vertex_line.at(2)).characters())),
                static_cast<GLfloat>(atof(String(vertex_line.at(3)).characters())) });

            continue;
        }

        // This line describes a face (a collection of 3 vertices, aka a triangle)
        if (object_line.starts_with("f")) {
            auto face_line = object_line.split_view(' ');
            if (face_line.size() != 4) {
                dbgln("Wavefront: Malformed face line. Aborting.");
                return nullptr;
            }

            GLuint vert_index[3];
            GLuint tex_coord_index[3];
            GLuint normal_index[3];
            if (object_line.contains("/")) {
                for (int i = 1; i <= 3; ++i) {
                    auto vertex_data = face_line.at(i).split_view("/", true);

                    vert_index[i - 1] = vertex_data.at(0).to_uint().value_or(1);
                    tex_coord_index[i - 1] = vertex_data.at(1).to_uint().value_or(1);

                    if (vertex_data.size() == 3)
                        normal_index[i - 1] = vertex_data.at(2).to_uint().value_or(1);
                    else
                        normal_index[i - 1] = 1;
                }
            } else {
                vert_index[0] = (face_line.at(1).to_uint().value_or(1));
                vert_index[1] = (face_line.at(2).to_uint().value_or(1));
                vert_index[2] = (face_line.at(3).to_uint().value_or(1));
                tex_coord_index[0] = 0;
                tex_coord_index[1] = 0;
                tex_coord_index[2] = 0;
                normal_index[0] = 0;
                normal_index[1] = 0;
                normal_index[2] = 0;
            }

            // Create a new triangle
            triangles.append(
                {
                    vert_index[0] - 1,
                    vert_index[1] - 1,
                    vert_index[2] - 1,
                    tex_coord_index[0] - 1,
                    tex_coord_index[1] - 1,
                    tex_coord_index[2] - 1,
                    normal_index[0] - 1,
                    normal_index[1] - 1,
                    normal_index[2] - 1,
                });
        }
    }

    if (vertices.is_empty()) {
        dbgln("Wavefront: Failed to read any data from 3D file: {}", file.name());
        return nullptr;
    }

    dbgln("Wavefront: Done.");
    return adopt_ref(*new Mesh(vertices, tex_coords, normals, triangles));
}
