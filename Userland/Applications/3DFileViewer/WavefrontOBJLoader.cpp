/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavefrontOBJLoader.h"
#include <AK/FixedArray.h>
#include <AK/String.h>
#include <LibCore/File.h>

static inline GLuint get_index_value(StringView& representation)
{
    return representation.to_number<GLuint>().value_or(1) - 1;
}

static ErrorOr<GLfloat> parse_float(StringView string)
{
    auto maybe_float = string.to_number<GLfloat>(TrimWhitespace::No);
    if (!maybe_float.has_value())
        return Error::from_string_literal("Wavefront: Expected floating point value when parsing TexCoord line");

    return maybe_float.release_value();
}

ErrorOr<NonnullRefPtr<Mesh>> WavefrontOBJLoader::load(ByteString const& filename, NonnullOwnPtr<Core::File> file)
{
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));

    Vector<Vertex> vertices;
    Vector<Vertex> normals;
    Vector<TexCoord> tex_coords;
    Vector<Triangle> triangles;

    dbgln("Wavefront: Loading {}...", filename);

    // Start reading file line by line
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
    while (TRY(buffered_file->can_read_line())) {
        auto object_line = TRY(buffered_file->read_line(buffer));

        // Ignore file comments
        if (object_line.starts_with('#'))
            continue;

        if (object_line.starts_with("vt"sv)) {
            auto tex_coord_line = object_line.split_view(' ');
            if (tex_coord_line.size() != 3) {
                return Error::from_string_literal("Wavefront: Malformed TexCoord line.");
            }

            tex_coords.append({ TRY(parse_float(tex_coord_line.at(1))),
                TRY(parse_float(tex_coord_line.at(2))) });

            continue;
        }

        if (object_line.starts_with("vn"sv)) {
            auto normal_line = object_line.split_view(' ');
            if (normal_line.size() != 4) {
                return Error::from_string_literal("Wavefront: Malformed vertex normal line.");
            }

            normals.append({ TRY(parse_float(normal_line.at(1))),
                TRY(parse_float(normal_line.at(2))),
                TRY(parse_float(normal_line.at(3))) });

            continue;
        }

        // This line describes a vertex (a position in 3D space)
        if (object_line.starts_with('v')) {
            auto vertex_line = object_line.split_view(' ');
            if (vertex_line.size() != 4) {
                return Error::from_string_literal("Wavefront: Malformed vertex line.");
            }

            vertices.append({ TRY(parse_float((vertex_line.at(1)))),
                TRY(parse_float((vertex_line.at(2)))),
                TRY(parse_float((vertex_line.at(3)))) });

            continue;
        }

        // This line describes a face (a collection of 3+ vertices, aka a triangle or polygon)
        if (object_line.starts_with('f')) {
            auto face_line = object_line.substring_view(2).split_view(' ');
            auto number_of_vertices = face_line.size();
            if (number_of_vertices < 3) {
                return Error::from_string_literal("Wavefront: Malformed face line.");
            }

            auto vertex_indices = TRY(FixedArray<GLuint>::create(number_of_vertices));
            auto tex_coord_indices = TRY(FixedArray<GLuint>::create(number_of_vertices));
            auto normal_indices = TRY(FixedArray<GLuint>::create(number_of_vertices));

            for (size_t i = 0; i < number_of_vertices; ++i) {
                auto vertex_parts = face_line.at(i).split_view('/', SplitBehavior::KeepEmpty);
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
        return Error::from_string_literal("Wavefront: Failed to read any data from 3D file");
    }

    dbgln("Wavefront: Done.");
    return adopt_ref(*new Mesh(vertices, tex_coords, normals, triangles));
}
