/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavefrontOBJLoader.h"
#include <AK/FixedArray.h>
#include <LibCore/File.h>
#include <stdlib.h>

static inline GLuint get_index_value(StringView& representation)
{
    return representation.to_uint().value_or(1) - 1;
}

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

        // This line describes a face (a collection of 3+ vertices, aka a triangle or polygon)
        if (object_line.starts_with("f")) {
            auto face_line = object_line.substring_view(2).split_view(' ');
            auto number_of_vertices = face_line.size();
            if (number_of_vertices < 3) {
                dbgln("Wavefront: Malformed face line. Aborting.");
                return nullptr;
            }

            auto vertex_indices = FixedArray<GLuint>::must_create_but_fixme_should_propagate_errors(number_of_vertices);
            auto tex_coord_indices = FixedArray<GLuint>::must_create_but_fixme_should_propagate_errors(number_of_vertices);
            auto normal_indices = FixedArray<GLuint>::must_create_but_fixme_should_propagate_errors(number_of_vertices);

            for (size_t i = 0; i < number_of_vertices; ++i) {
                auto vertex_parts = face_line.at(i).split_view('/', true);
                vertex_indices[i] = get_index_value(vertex_parts[0]);
                tex_coord_indices[i] = (vertex_parts.size() >= 2) ? get_index_value(vertex_parts[1]) : 0;
                normal_indices[i] = (vertex_parts.size() >= 3) ? get_index_value(vertex_parts[2]) : 0;
            }

            // Create a triangle for each part of the polygon
            for (size_t i = 0; i < number_of_vertices - 2; ++i) {
                triangles.append({
                    vertex_indices[0],
                    vertex_indices[i + 1],
                    vertex_indices[i + 2],
                    tex_coord_indices[0],
                    tex_coord_indices[i + 1],
                    tex_coord_indices[i + 2],
                    normal_indices[0],
                    normal_indices[i + 1],
                    normal_indices[i + 2],
                });
            }
        }
    }

    if (vertices.is_empty()) {
        dbgln("Wavefront: Failed to read any data from 3D file: {}", file.name());
        return nullptr;
    }

    dbgln("Wavefront: Done.");
    return adopt_ref(*new Mesh(vertices, tex_coords, normals, triangles));
}
