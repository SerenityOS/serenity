/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Device.h>

namespace SoftGPU {

using IntVector2 = Gfx::Vector2<int>;
using IntVector3 = Gfx::Vector3<int>;

constexpr static int edge_function(const IntVector2& a, const IntVector2& b, const IntVector2& c)
{
    return ((c.x() - a.x()) * (b.y() - a.y()) - (c.y() - a.y()) * (b.x() - a.x()));
}

template<typename T>
constexpr static T interpolate(const T& v0, const T& v1, const T& v2, const FloatVector3& barycentric_coords)
{
    return v0 * barycentric_coords.x() + v1 * barycentric_coords.y() + v2 * barycentric_coords.z();
}

template<typename T>
constexpr static T mix(const T& x, const T& y, float interp)
{
    return x * (1 - interp) + y * interp;
}

ALWAYS_INLINE constexpr static Gfx::RGBA32 to_rgba32(const FloatVector4& v)
{
    auto clamped = v.clamped(0, 1);
    u8 r = clamped.x() * 255;
    u8 g = clamped.y() * 255;
    u8 b = clamped.z() * 255;
    u8 a = clamped.w() * 255;
    return a << 24 | r << 16 | g << 8 | b;
}

static FloatVector4 to_vec4(Gfx::RGBA32 rgba)
{
    auto constexpr one_over_255 = 1.0f / 255;
    return {
        ((rgba >> 16) & 0xff) * one_over_255,
        ((rgba >> 8) & 0xff) * one_over_255,
        (rgba & 0xff) * one_over_255,
        ((rgba >> 24) & 0xff) * one_over_255,
    };
}

static Gfx::IntRect scissor_box_to_window_coordinates(Gfx::IntRect const& scissor_box, Gfx::IntRect const& window_rect)
{
    return scissor_box.translated(0, window_rect.height() - 2 * scissor_box.y() - scissor_box.height());
}

static constexpr void setup_blend_factors(BlendFactor mode, FloatVector4& constant, float& src_alpha, float& dst_alpha, float& src_color, float& dst_color)
{
    constant = { 0.0f, 0.0f, 0.0f, 0.0f };
    src_alpha = 0;
    dst_alpha = 0;
    src_color = 0;
    dst_color = 0;

    switch (mode) {
    case BlendFactor::Zero:
        break;
    case BlendFactor::One:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case BlendFactor::SrcColor:
        src_color = 1;
        break;
    case BlendFactor::OneMinusSrcColor:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        src_color = -1;
        break;
    case BlendFactor::SrcAlpha:
        src_alpha = 1;
        break;
    case BlendFactor::OneMinusSrcAlpha:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        src_alpha = -1;
        break;
    case BlendFactor::DstAlpha:
        dst_alpha = 1;
        break;
    case BlendFactor::OneMinusDstAlpha:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        dst_alpha = -1;
        break;
    case BlendFactor::DstColor:
        dst_color = 1;
        break;
    case BlendFactor::OneMinusDstColor:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        dst_color = -1;
        break;
    case BlendFactor::SrcAlphaSaturate:
        // FIXME: How do we implement this?
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename PS>
static void rasterize_triangle(const RasterizerOptions& options, Gfx::Bitmap& render_target, DepthBuffer& depth_buffer, const Triangle& triangle, PS pixel_shader)
{
    // Since the algorithm is based on blocks of uniform size, we need
    // to ensure that our render_target size is actually a multiple of the block size
    VERIFY((render_target.width() % RASTERIZER_BLOCK_SIZE) == 0);
    VERIFY((render_target.height() % RASTERIZER_BLOCK_SIZE) == 0);

    // Return if alpha testing is a no-op
    if (options.enable_alpha_test && options.alpha_test_func == AlphaTestFunction::Never)
        return;

    // Vertices
    Vertex const vertex0 = triangle.vertices[0];
    Vertex const vertex1 = triangle.vertices[1];
    Vertex const vertex2 = triangle.vertices[2];

    // Calculate area of the triangle for later tests
    IntVector2 const v0 { static_cast<int>(vertex0.window_coordinates.x()), static_cast<int>(vertex0.window_coordinates.y()) };
    IntVector2 const v1 { static_cast<int>(vertex1.window_coordinates.x()), static_cast<int>(vertex1.window_coordinates.y()) };
    IntVector2 const v2 { static_cast<int>(vertex2.window_coordinates.x()), static_cast<int>(vertex2.window_coordinates.y()) };

    int area = edge_function(v0, v1, v2);
    if (area == 0)
        return;

    auto const one_over_area = 1.0f / area;

    FloatVector4 src_constant {};
    float src_factor_src_alpha = 0;
    float src_factor_dst_alpha = 0;
    float src_factor_src_color = 0;
    float src_factor_dst_color = 0;

    FloatVector4 dst_constant {};
    float dst_factor_src_alpha = 0;
    float dst_factor_dst_alpha = 0;
    float dst_factor_src_color = 0;
    float dst_factor_dst_color = 0;

    if (options.enable_blending) {
        setup_blend_factors(
            options.blend_source_factor,
            src_constant,
            src_factor_src_alpha,
            src_factor_dst_alpha,
            src_factor_src_color,
            src_factor_dst_color);

        setup_blend_factors(
            options.blend_destination_factor,
            dst_constant,
            dst_factor_src_alpha,
            dst_factor_dst_alpha,
            dst_factor_src_color,
            dst_factor_dst_color);
    }

    // Obey top-left rule:
    // This sets up "zero" for later pixel coverage tests.
    // Depending on where on the triangle the edge is located
    // it is either tested against 0 or 1, effectively
    // turning "< 0" into "<= 0"
    IntVector3 zero { 1, 1, 1 };
    if (v1.y() > v0.y() || (v1.y() == v0.y() && v1.x() < v0.x()))
        zero.set_z(0);
    if (v2.y() > v1.y() || (v2.y() == v1.y() && v2.x() < v1.x()))
        zero.set_x(0);
    if (v0.y() > v2.y() || (v0.y() == v2.y() && v0.x() < v2.x()))
        zero.set_y(0);

    // This function calculates the 3 edge values for the pixel relative to the triangle.
    auto calculate_edge_values = [v0, v1, v2](const IntVector2& p) -> IntVector3 {
        return {
            edge_function(v1, v2, p),
            edge_function(v2, v0, p),
            edge_function(v0, v1, p),
        };
    };

    // This function tests whether a point as identified by its 3 edge values lies within the triangle
    auto test_point = [zero](const IntVector3& edges) -> bool {
        return edges.x() >= zero.x()
            && edges.y() >= zero.y()
            && edges.z() >= zero.z();
    };

    // Calculate block-based bounds
    auto render_bounds = render_target.rect();
    if (options.scissor_enabled)
        render_bounds.intersect(scissor_box_to_window_coordinates(options.scissor_box, render_target.rect()));
    int const block_padding = RASTERIZER_BLOCK_SIZE - 1;
    // clang-format off
    int const bx0 =  max(render_bounds.left(),   min(min(v0.x(), v1.x()), v2.x()))                  / RASTERIZER_BLOCK_SIZE;
    int const bx1 = (min(render_bounds.right(),  max(max(v0.x(), v1.x()), v2.x())) + block_padding) / RASTERIZER_BLOCK_SIZE;
    int const by0 =  max(render_bounds.top(),    min(min(v0.y(), v1.y()), v2.y()))                  / RASTERIZER_BLOCK_SIZE;
    int const by1 = (min(render_bounds.bottom(), max(max(v0.y(), v1.y()), v2.y())) + block_padding) / RASTERIZER_BLOCK_SIZE;
    // clang-format on

    u8 pixel_mask[RASTERIZER_BLOCK_SIZE];
    static_assert(RASTERIZER_BLOCK_SIZE <= sizeof(decltype(*pixel_mask)) * 8, "RASTERIZER_BLOCK_SIZE must be smaller than the pixel_mask's width in bits");

    FloatVector4 pixel_staging[RASTERIZER_BLOCK_SIZE][RASTERIZER_BLOCK_SIZE];
    float depth_staging[RASTERIZER_BLOCK_SIZE][RASTERIZER_BLOCK_SIZE];

    // Fog depths
    float const vertex0_eye_absz = fabs(vertex0.eye_coordinates.z());
    float const vertex1_eye_absz = fabs(vertex1.eye_coordinates.z());
    float const vertex2_eye_absz = fabs(vertex2.eye_coordinates.z());

    // FIXME: implement stencil testing

    // Iterate over all blocks within the bounds of the triangle
    for (int by = by0; by < by1; by++) {
        for (int bx = bx0; bx < bx1; bx++) {

            // Edge values of the 4 block corners
            // clang-format off
            auto b0 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE,                         by * RASTERIZER_BLOCK_SIZE });
            auto b1 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE, by * RASTERIZER_BLOCK_SIZE });
            auto b2 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE,                         by * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE });
            auto b3 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE, by * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE });
            // clang-format on

            // If the whole block is outside any of the triangle edges we can discard it completely
            // We test this by and'ing the relevant edge function values together for all block corners
            // and checking if the negative sign bit is set for all of them
            if ((b0.x() & b1.x() & b2.x() & b3.x()) & 0x80000000)
                continue;

            if ((b0.y() & b1.y() & b2.y() & b3.y()) & 0x80000000)
                continue;

            if ((b0.z() & b1.z() & b2.z() & b3.z()) & 0x80000000)
                continue;

            // edge value derivatives
            auto dbdx = (b1 - b0) / RASTERIZER_BLOCK_SIZE;
            auto dbdy = (b2 - b0) / RASTERIZER_BLOCK_SIZE;
            // step edge value after each horizontal span: 1 down, BLOCK_SIZE left
            auto step_y = dbdy - dbdx * RASTERIZER_BLOCK_SIZE;

            int x0 = bx * RASTERIZER_BLOCK_SIZE;
            int y0 = by * RASTERIZER_BLOCK_SIZE;

            // Generate the coverage mask
            if (!options.scissor_enabled && test_point(b0) && test_point(b1) && test_point(b2) && test_point(b3)) {
                // The block is fully contained within the triangle. Fill the mask with all 1s
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++)
                    pixel_mask[y] = -1;
            } else {
                // The block overlaps at least one triangle edge.
                // We need to test coverage of every pixel within the block.
                auto coords = b0;
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                    pixel_mask[y] = 0;

                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx) {
                        if (test_point(coords) && (!options.scissor_enabled || render_bounds.contains(x0 + x, y0 + y)))
                            pixel_mask[y] |= 1 << x;
                    }
                }
            }

            // AND the depth mask onto the coverage mask
            if (options.enable_depth_test) {
                int z_pass_count = 0;
                auto coords = b0;

                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                    if (pixel_mask[y] == 0) {
                        coords += dbdx * RASTERIZER_BLOCK_SIZE;
                        continue;
                    }

                    auto* depth = &depth_buffer.scanline(y0 + y)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx, depth++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        auto barycentric = FloatVector3(coords.x(), coords.y(), coords.z()) * one_over_area;
                        float z = interpolate(vertex0.window_coordinates.z(), vertex1.window_coordinates.z(), vertex2.window_coordinates.z(), barycentric);

                        // FIXME: Also apply depth_offset_factor which depends on the depth gradient
                        z += options.depth_offset_constant * NumericLimits<float>::epsilon();

                        bool pass = false;
                        switch (options.depth_func) {
                        case DepthTestFunction::Always:
                            pass = true;
                            break;
                        case DepthTestFunction::Never:
                            pass = false;
                            break;
                        case DepthTestFunction::Greater:
                            pass = z > *depth;
                            break;
                        case DepthTestFunction::GreaterOrEqual:
                            pass = z >= *depth;
                            break;
                        case DepthTestFunction::NotEqual:
#ifdef __SSE__
                            pass = z != *depth;
#else
                            pass = bit_cast<u32>(z) != bit_cast<u32>(*depth);
#endif
                            break;
                        case DepthTestFunction::Equal:
#ifdef __SSE__
                            pass = z == *depth;
#else
                            //
                            // This is an interesting quirk that occurs due to us using the x87 FPU when Serenity is
                            // compiled for the i386 target. When we calculate our depth value to be stored in the buffer,
                            // it is an 80-bit x87 floating point number, however, when stored into the DepthBuffer, this is
                            // truncated to 32 bits. This 38 bit loss of precision means that when x87 `FCOMP` is eventually
                            // used here the comparison fails.
                            // This could be solved by using a `long double` for the depth buffer, however this would take
                            // up significantly more space and is completely overkill for a depth buffer. As such, comparing
                            // the first 32-bits of this depth value is "good enough" that if we get a hit on it being
                            // equal, we can pretty much guarantee that it's actually equal.
                            //
                            pass = bit_cast<u32>(z) == bit_cast<u32>(*depth);
#endif
                            break;
                        case DepthTestFunction::LessOrEqual:
                            pass = z <= *depth;
                            break;
                        case DepthTestFunction::Less:
                            pass = z < *depth;
                            break;
                        }

                        if (!pass) {
                            pixel_mask[y] ^= 1 << x;
                            continue;
                        }

                        depth_staging[y][x] = z;

                        z_pass_count++;
                    }
                }

                // Nice, no pixels passed the depth test -> block rejected by early z
                if (z_pass_count == 0)
                    continue;
            }

            // Draw the pixels according to the previously generated mask
            auto coords = b0;
            for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                if (pixel_mask[y] == 0) {
                    coords += dbdx * RASTERIZER_BLOCK_SIZE;
                    continue;
                }

                auto* pixel = pixel_staging[y];
                for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx, pixel++) {
                    if (~pixel_mask[y] & (1 << x))
                        continue;

                    // Perspective correct barycentric coordinates
                    auto barycentric = FloatVector3(coords.x(), coords.y(), coords.z()) * one_over_area;
                    auto const w_coordinates = FloatVector3 {
                        vertex0.window_coordinates.w(),
                        vertex1.window_coordinates.w(),
                        vertex2.window_coordinates.w(),
                    };
                    float const interpolated_reciprocal_w = interpolate(w_coordinates.x(), w_coordinates.y(), w_coordinates.z(), barycentric);
                    float const interpolated_w = 1 / interpolated_reciprocal_w;
                    barycentric = barycentric * w_coordinates * interpolated_w;

                    // FIXME: make this more generic. We want to interpolate more than just color and uv
                    FloatVector4 vertex_color;
                    if (options.shade_smooth) {
                        vertex_color = interpolate(vertex0.color, vertex1.color, vertex2.color, barycentric);
                    } else {
                        vertex_color = vertex0.color;
                    }

                    auto uv = interpolate(vertex0.tex_coord, vertex1.tex_coord, vertex2.tex_coord, barycentric);

                    // Calculate depth of fragment for fog
                    //
                    // OpenGL 1.5 spec chapter 3.10: "An implementation may choose to approximate the
                    // eye-coordinate distance from the eye to each fragment center by |Ze|."

                    float fog_fragment_depth = interpolate(vertex0_eye_absz, vertex1_eye_absz, vertex2_eye_absz, barycentric);

                    *pixel = pixel_shader(uv, vertex_color, fog_fragment_depth);
                }
            }

            if (options.enable_alpha_test && options.alpha_test_func != AlphaTestFunction::Always) {
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    if (pixel_mask[y] == 0)
                        continue;

                    auto src = pixel_staging[y];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        bool passed = true;

                        switch (options.alpha_test_func) {
                        case AlphaTestFunction::Less:
                            passed = src->w() < options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::Equal:
                            passed = src->w() == options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::LessOrEqual:
                            passed = src->w() <= options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::Greater:
                            passed = src->w() > options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::NotEqual:
                            passed = src->w() != options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::GreaterOrEqual:
                            passed = src->w() >= options.alpha_test_ref_value;
                            break;
                        case AlphaTestFunction::Never:
                        case AlphaTestFunction::Always:
                            VERIFY_NOT_REACHED();
                        }

                        if (!passed)
                            pixel_mask[y] ^= (1 << x);
                    }
                }
            }

            // Write to depth buffer
            if (options.enable_depth_test && options.enable_depth_write) {
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    if (pixel_mask[y] == 0)
                        continue;

                    auto* depth = &depth_buffer.scanline(y0 + y)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, depth++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        *depth = depth_staging[y][x];
                    }
                }
            }

            // We will not update the color buffer at all
            if (!options.color_mask || !options.enable_color_write)
                continue;

            if (options.enable_blending) {
                // Blend color values from pixel_staging into render_target
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    auto src = pixel_staging[y];
                    auto dst = &render_target.scanline(y0 + y)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++, dst++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        auto float_dst = to_vec4(*dst);

                        auto src_factor = src_constant
                            + *src * src_factor_src_color
                            + FloatVector4(src->w(), src->w(), src->w(), src->w()) * src_factor_src_alpha
                            + float_dst * src_factor_dst_color
                            + FloatVector4(float_dst.w(), float_dst.w(), float_dst.w(), float_dst.w()) * src_factor_dst_alpha;

                        auto dst_factor = dst_constant
                            + *src * dst_factor_src_color
                            + FloatVector4(src->w(), src->w(), src->w(), src->w()) * dst_factor_src_alpha
                            + float_dst * dst_factor_dst_color
                            + FloatVector4(float_dst.w(), float_dst.w(), float_dst.w(), float_dst.w()) * dst_factor_dst_alpha;

                        *dst = (*dst & ~options.color_mask) | (to_rgba32(*src * src_factor + float_dst * dst_factor) & options.color_mask);
                    }
                }
            } else {
                // Copy color values from pixel_staging into render_target
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    auto src = pixel_staging[y];
                    auto dst = &render_target.scanline(y + y0)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++, dst++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        *dst = (*dst & ~options.color_mask) | (to_rgba32(*src) & options.color_mask);
                    }
                }
            }
        }
    }
}

static Gfx::IntSize closest_multiple(const Gfx::IntSize& min_size, size_t step)
{
    int width = ((min_size.width() + step - 1) / step) * step;
    int height = ((min_size.height() + step - 1) / step) * step;
    return { width, height };
}

Device::Device(const Gfx::IntSize& min_size)
    : m_render_target { Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, closest_multiple(min_size, RASTERIZER_BLOCK_SIZE)).release_value_but_fixme_should_propagate_errors() }
    , m_depth_buffer { adopt_own(*new DepthBuffer(closest_multiple(min_size, RASTERIZER_BLOCK_SIZE))) }
{
    m_options.scissor_box = m_render_target->rect();
}

DeviceInfo Device::info() const
{
    return {
        .vendor_name = "SerenityOS",
        .device_name = "SoftGPU",
        .num_texture_units = NUM_SAMPLERS
    };
}

static void generate_texture_coordinates(Vertex& vertex, RasterizerOptions const& options)
{
    auto generate_coordinate = [&](size_t config_index) -> float {
        auto mode = options.texcoord_generation_config[config_index].mode;

        switch (mode) {
        case TexCoordGenerationMode::ObjectLinear: {
            auto coefficients = options.texcoord_generation_config[config_index].coefficients;
            return coefficients.dot(vertex.position);
        }
        case TexCoordGenerationMode::EyeLinear: {
            auto coefficients = options.texcoord_generation_config[config_index].coefficients;
            return coefficients.dot(vertex.eye_coordinates);
        }
        case TexCoordGenerationMode::SphereMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = { eye_unit.x(), eye_unit.y(), eye_unit.z() };
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            reflection.set_z(reflection.z() + 1);
            auto const reflection_value = (config_index == 0) ? reflection.x() : reflection.y();
            return reflection_value / (2 * reflection.length()) + 0.5f;
        }
        case TexCoordGenerationMode::ReflectionMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = { eye_unit.x(), eye_unit.y(), eye_unit.z() };
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            switch (config_index) {
            case 0:
                return reflection.x();
            case 1:
                return reflection.y();
            case 2:
                return reflection.z();
            default:
                VERIFY_NOT_REACHED();
            }
        }
        case TexCoordGenerationMode::NormalMap: {
            auto const normal = vertex.normal;
            switch (config_index) {
            case 0:
                return normal.x();
            case 1:
                return normal.y();
            case 2:
                return normal.z();
            default:
                VERIFY_NOT_REACHED();
            }
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto const enabled_coords = options.texcoord_generation_enabled_coordinates;
    vertex.tex_coord = {
        ((enabled_coords & TexCoordGenerationCoordinate::S) > 0) ? generate_coordinate(0) : vertex.tex_coord.x(),
        ((enabled_coords & TexCoordGenerationCoordinate::T) > 0) ? generate_coordinate(1) : vertex.tex_coord.y(),
        ((enabled_coords & TexCoordGenerationCoordinate::R) > 0) ? generate_coordinate(2) : vertex.tex_coord.z(),
        ((enabled_coords & TexCoordGenerationCoordinate::Q) > 0) ? generate_coordinate(3) : vertex.tex_coord.w(),
    };
}

void Device::draw_primitives(PrimitiveType primitive_type, FloatMatrix4x4 const& model_view_transform, FloatMatrix3x3 const& normal_transform,
    FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<Vertex> const& vertices,
    Vector<size_t> const& enabled_texture_units)
{
    // At this point, the user has effectively specified that they are done with defining the geometry
    // of what they want to draw. We now need to do a few things (https://www.khronos.org/opengl/wiki/Rendering_Pipeline_Overview):
    //
    // 1.   Transform all of the vertices in the current vertex list into eye space by mulitplying the model-view matrix
    // 2.   Transform all of the vertices from eye space into clip space by multiplying by the projection matrix
    // 3.   If culling is enabled, we cull the desired faces (https://learnopengl.com/Advanced-OpenGL/Face-culling)
    // 4.   Each element of the vertex is then divided by w to bring the positions into NDC (Normalized Device Coordinates)
    // 5.   The vertices are sorted (for the rasteriser, how are we doing this? 3Dfx did this top to bottom in terms of vertex y coordinates)
    // 6.   The vertices are then sent off to the rasteriser and drawn to the screen

    float scr_width = m_render_target->width();
    float scr_height = m_render_target->height();

    m_triangle_list.clear_with_capacity();
    m_processed_triangles.clear_with_capacity();

    // Let's construct some triangles
    if (primitive_type == PrimitiveType::Triangles) {
        Triangle triangle;
        for (size_t i = 0; i < vertices.size(); i += 3) {
            triangle.vertices[0] = vertices.at(i);
            triangle.vertices[1] = vertices.at(i + 1);
            triangle.vertices[2] = vertices.at(i + 2);

            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == PrimitiveType::Quads) {
        // We need to construct two triangles to form the quad
        Triangle triangle;
        VERIFY(vertices.size() % 4 == 0);
        for (size_t i = 0; i < vertices.size(); i += 4) {
            // Triangle 1
            triangle.vertices[0] = vertices.at(i);
            triangle.vertices[1] = vertices.at(i + 1);
            triangle.vertices[2] = vertices.at(i + 2);
            m_triangle_list.append(triangle);

            // Triangle 2
            triangle.vertices[0] = vertices.at(i + 2);
            triangle.vertices[1] = vertices.at(i + 3);
            triangle.vertices[2] = vertices.at(i);
            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == PrimitiveType::TriangleFan) {
        Triangle triangle;
        triangle.vertices[0] = vertices.at(0); // Root vertex is always the vertex defined first

        for (size_t i = 1; i < vertices.size() - 1; i++) // This is technically `n-2` triangles. We start at index 1
        {
            triangle.vertices[1] = vertices.at(i);
            triangle.vertices[2] = vertices.at(i + 1);
            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == PrimitiveType::TriangleStrip) {
        Triangle triangle;
        for (size_t i = 0; i < vertices.size() - 2; i++) {
            if (i % 2 == 0) {
                triangle.vertices[0] = vertices.at(i);
                triangle.vertices[1] = vertices.at(i + 1);
                triangle.vertices[2] = vertices.at(i + 2);
            } else {
                triangle.vertices[0] = vertices.at(i + 1);
                triangle.vertices[1] = vertices.at(i);
                triangle.vertices[2] = vertices.at(i + 2);
            }
            m_triangle_list.append(triangle);
        }
    }

    // Now let's transform each triangle and send that to the GPU
    auto const depth_half_range = (m_options.depth_max - m_options.depth_min) / 2;
    auto const depth_halfway = (m_options.depth_min + m_options.depth_max) / 2;
    for (auto& triangle : m_triangle_list) {
        // Transform vertices into eye coordinates using the model-view transform
        triangle.vertices[0].eye_coordinates = model_view_transform * triangle.vertices[0].position;
        triangle.vertices[1].eye_coordinates = model_view_transform * triangle.vertices[1].position;
        triangle.vertices[2].eye_coordinates = model_view_transform * triangle.vertices[2].position;

        // Transform eye coordinates into clip coordinates using the projection transform
        triangle.vertices[0].clip_coordinates = projection_transform * triangle.vertices[0].eye_coordinates;
        triangle.vertices[1].clip_coordinates = projection_transform * triangle.vertices[1].eye_coordinates;
        triangle.vertices[2].clip_coordinates = projection_transform * triangle.vertices[2].eye_coordinates;

        // At this point, we're in clip space
        // Here's where we do the clipping. This is a really crude implementation of the
        // https://learnopengl.com/Getting-started/Coordinate-Systems
        // "Note that if only a part of a primitive e.g. a triangle is outside the clipping volume OpenGL
        // will reconstruct the triangle as one or more triangles to fit inside the clipping range. "
        //
        // ALL VERTICES ARE DEFINED IN A CLOCKWISE ORDER

        // Okay, let's do some face culling first

        m_clipped_vertices.clear_with_capacity();
        m_clipped_vertices.append(triangle.vertices[0]);
        m_clipped_vertices.append(triangle.vertices[1]);
        m_clipped_vertices.append(triangle.vertices[2]);
        m_clipper.clip_triangle_against_frustum(m_clipped_vertices);

        if (m_clipped_vertices.size() < 3)
            continue;

        for (auto& vec : m_clipped_vertices) {
            // To normalized device coordinates (NDC)
            auto const one_over_w = 1 / vec.clip_coordinates.w();
            auto const ndc_coordinates = FloatVector4 {
                vec.clip_coordinates.x() * one_over_w,
                vec.clip_coordinates.y() * one_over_w,
                vec.clip_coordinates.z() * one_over_w,
                one_over_w,
            };

            // To window coordinates
            // FIXME: implement viewport functionality
            vec.window_coordinates = {
                scr_width / 2 + ndc_coordinates.x() * scr_width / 2,
                scr_height / 2 - ndc_coordinates.y() * scr_height / 2,
                depth_half_range * ndc_coordinates.z() + depth_halfway,
                ndc_coordinates.w(),
            };
        }

        Triangle tri;
        tri.vertices[0] = m_clipped_vertices[0];
        for (size_t i = 1; i < m_clipped_vertices.size() - 1; i++) {
            tri.vertices[1] = m_clipped_vertices[i];
            tri.vertices[2] = m_clipped_vertices[i + 1];
            m_processed_triangles.append(tri);
        }
    }

    for (auto& triangle : m_processed_triangles) {
        // Let's calculate the (signed) area of the triangle
        // https://cp-algorithms.com/geometry/oriented-triangle-area.html
        float dxAB = triangle.vertices[0].window_coordinates.x() - triangle.vertices[1].window_coordinates.x(); // A.x - B.x
        float dxBC = triangle.vertices[1].window_coordinates.x() - triangle.vertices[2].window_coordinates.x(); // B.X - C.x
        float dyAB = triangle.vertices[0].window_coordinates.y() - triangle.vertices[1].window_coordinates.y();
        float dyBC = triangle.vertices[1].window_coordinates.y() - triangle.vertices[2].window_coordinates.y();
        float area = (dxAB * dyBC) - (dxBC * dyAB);

        if (area == 0.0f)
            continue;

        if (m_options.enable_culling) {
            bool is_front = (m_options.front_face == WindingOrder::CounterClockwise ? area < 0 : area > 0);

            if (!is_front && m_options.cull_back)
                continue;

            if (is_front && m_options.cull_front)
                continue;
        }

        if (area > 0)
            swap(triangle.vertices[0], triangle.vertices[1]);

        // Transform normals
        triangle.vertices[0].normal = normal_transform * triangle.vertices[0].normal;
        triangle.vertices[1].normal = normal_transform * triangle.vertices[1].normal;
        triangle.vertices[2].normal = normal_transform * triangle.vertices[2].normal;
        if (m_options.normalization_enabled) {
            triangle.vertices[0].normal.normalize();
            triangle.vertices[1].normal.normalize();
            triangle.vertices[2].normal.normalize();
        }

        // Generate texture coordinates if at least one coordinate is enabled
        if (m_options.texcoord_generation_enabled_coordinates != TexCoordGenerationCoordinate::None) {
            generate_texture_coordinates(triangle.vertices[0], m_options);
            generate_texture_coordinates(triangle.vertices[1], m_options);
            generate_texture_coordinates(triangle.vertices[2], m_options);
        }

        // Apply texture transformation
        // FIXME: implement multi-texturing: texcoords should be stored per texture unit
        triangle.vertices[0].tex_coord = texture_transform * triangle.vertices[0].tex_coord;
        triangle.vertices[1].tex_coord = texture_transform * triangle.vertices[1].tex_coord;
        triangle.vertices[2].tex_coord = texture_transform * triangle.vertices[2].tex_coord;

        submit_triangle(triangle, enabled_texture_units);
    }
}

void Device::submit_triangle(const Triangle& triangle, Vector<size_t> const& enabled_texture_units)
{
    rasterize_triangle(m_options, *m_render_target, *m_depth_buffer, triangle, [this, &enabled_texture_units](FloatVector4 const& uv, FloatVector4 const& color, float fog_depth) -> FloatVector4 {
        FloatVector4 fragment = color;

        for (size_t i : enabled_texture_units) {
            // FIXME: implement GL_TEXTURE_1D, GL_TEXTURE_3D and GL_TEXTURE_CUBE_MAP
            auto const& sampler = m_samplers[i];

            FloatVector4 texel = sampler.sample_2d({ uv.x(), uv.y() });

            // FIXME: Implement more blend modes
            switch (sampler.config().fixed_function_texture_env_mode) {
            case TextureEnvMode::Modulate:
                fragment = fragment * texel;
                break;
            case TextureEnvMode::Replace:
                fragment = texel;
                break;
            case TextureEnvMode::Decal: {
                float src_alpha = fragment.w();
                float one_minus_src_alpha = 1 - src_alpha;
                fragment.set_x(texel.x() * src_alpha + fragment.x() * one_minus_src_alpha);
                fragment.set_y(texel.y() * src_alpha + fragment.y() * one_minus_src_alpha);
                fragment.set_z(texel.z() * src_alpha + fragment.z() * one_minus_src_alpha);
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }
        }

        // Calculate fog
        // Math from here: https://opengl-notes.readthedocs.io/en/latest/topics/texturing/aliasing.html
        if (m_options.fog_enabled) {
            float factor = 0.0f;
            switch (m_options.fog_mode) {
            case FogMode::Linear:
                factor = (m_options.fog_end - fog_depth) / (m_options.fog_end - m_options.fog_start);
                break;
            case FogMode::Exp:
                factor = expf(-m_options.fog_density * fog_depth);
                break;
            case FogMode::Exp2:
                factor = expf(-((m_options.fog_density * fog_depth) * (m_options.fog_density * fog_depth)));
                break;
            default:
                VERIFY_NOT_REACHED();
            }

            // Mix texel's RGB with fog's RBG - leave alpha alone
            fragment.set_x(mix(m_options.fog_color.x(), fragment.x(), factor));
            fragment.set_y(mix(m_options.fog_color.y(), fragment.y(), factor));
            fragment.set_z(mix(m_options.fog_color.z(), fragment.z(), factor));
        }

        return fragment;
    });
}

void Device::resize(const Gfx::IntSize& min_size)
{
    wait_for_all_threads();

    m_render_target = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, closest_multiple(min_size, RASTERIZER_BLOCK_SIZE)).release_value_but_fixme_should_propagate_errors();
    m_depth_buffer = adopt_own(*new DepthBuffer(m_render_target->size()));
}

void Device::clear_color(const FloatVector4& color)
{
    wait_for_all_threads();

    uint8_t r = static_cast<uint8_t>(clamp(color.x(), 0.0f, 1.0f) * 255);
    uint8_t g = static_cast<uint8_t>(clamp(color.y(), 0.0f, 1.0f) * 255);
    uint8_t b = static_cast<uint8_t>(clamp(color.z(), 0.0f, 1.0f) * 255);
    uint8_t a = static_cast<uint8_t>(clamp(color.w(), 0.0f, 1.0f) * 255);
    auto const fill_color = Gfx::Color(r, g, b, a);

    if (m_options.scissor_enabled) {
        auto fill_rect = m_render_target->rect();
        fill_rect.intersect(scissor_box_to_window_coordinates(m_options.scissor_box, fill_rect));
        Gfx::Painter painter { *m_render_target };
        painter.fill_rect(fill_rect, fill_color);
        return;
    }

    m_render_target->fill(fill_color);
}

void Device::clear_depth(float depth)
{
    wait_for_all_threads();

    if (m_options.scissor_enabled) {
        m_depth_buffer->clear(scissor_box_to_window_coordinates(m_options.scissor_box, m_render_target->rect()), depth);
        return;
    }

    m_depth_buffer->clear(depth);
}

void Device::blit(Gfx::Bitmap const& source, int x, int y)
{
    wait_for_all_threads();

    Gfx::Painter painter { *m_render_target };
    painter.blit({ x, y }, source, source.rect(), 1.0f, true);
}

void Device::blit_to(Gfx::Bitmap& target)
{
    wait_for_all_threads();

    Gfx::Painter painter { target };
    painter.blit({ 0, 0 }, *m_render_target, m_render_target->rect(), 1.0f, false);
}

void Device::wait_for_all_threads() const
{
    // FIXME: Wait for all render threads to finish when multithreading is being implemented
}

void Device::set_options(const RasterizerOptions& options)
{
    wait_for_all_threads();

    m_options = options;

    // FIXME: Recreate or reinitialize render threads here when multithreading is being implemented
}

Gfx::RGBA32 Device::get_backbuffer_pixel(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 0;

    return m_render_target->scanline(y)[x];
}

float Device::get_depthbuffer_value(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 1.0f;

    return m_depth_buffer->scanline(y)[x];
}

NonnullRefPtr<Image> Device::create_image(ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers)
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(levels > 0);
    VERIFY(layers > 0);

    return adopt_ref(*new Image(format, width, height, depth, levels, layers));
}

void Device::set_sampler_config(unsigned sampler, SamplerConfig const& config)
{
    m_samplers[sampler].set_config(config);
}

}
