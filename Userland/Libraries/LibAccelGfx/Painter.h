/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibAccelGfx/Context.h>
#include <LibAccelGfx/Forward.h>
#include <LibAccelGfx/GL.h>
#include <LibAccelGfx/Program.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/TextLayout.h>

namespace AccelGfx {

class Painter {
    AK_MAKE_NONCOPYABLE(Painter);
    AK_MAKE_NONMOVABLE(Painter);

public:
    static OwnPtr<Painter> create();

    Painter(Context&);
    ~Painter();

    void clear(Gfx::Color);

    void save();
    void restore();

    [[nodiscard]] Gfx::AffineTransform const& transform() const { return state().transform; }
    void set_transform(Gfx::AffineTransform const& transform) { state().transform = transform; }

    void fill_rect(Gfx::FloatRect, Gfx::Color);
    void fill_rect(Gfx::IntRect, Gfx::Color);

    enum class ScalingMode {
        NearestNeighbor,
        Bilinear,
    };

    void draw_line(Gfx::IntPoint a, Gfx::IntPoint b, float thickness, Gfx::Color color);
    void draw_line(Gfx::FloatPoint a, Gfx::FloatPoint b, float thickness, Gfx::Color color);

    void draw_scaled_bitmap(Gfx::FloatRect const& dst_rect, Gfx::Bitmap const&, Gfx::FloatRect const& src_rect, ScalingMode = ScalingMode::NearestNeighbor);
    void draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const&, Gfx::IntRect const& src_rect, ScalingMode = ScalingMode::NearestNeighbor);

    void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs);

    struct GlyphsTextureKey {
        Gfx::Font const* font;
        u32 code_point;

        bool operator==(GlyphsTextureKey const& other) const
        {
            return font == other.font && code_point == other.code_point;
        }
    };

    void draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color);

    void set_clip_rect(Gfx::IntRect);
    void clear_clip_rect();

    void set_target_bitmap(Gfx::Bitmap&);
    void flush();

    void fill_rect_with_linear_gradient(Gfx::IntRect const&, ReadonlySpan<Gfx::ColorStop>, float angle, Optional<float> repeat_length = {});
    void fill_rect_with_linear_gradient(Gfx::FloatRect const&, ReadonlySpan<Gfx::ColorStop>, float angle, Optional<float> repeat_length = {});

    struct CornerRadius {
        float horizontal_radius;
        float vertical_radius;
    };
    void fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, CornerRadius const& top_left_radius, CornerRadius const& top_right_radius, CornerRadius const& bottom_left_radius, CornerRadius const& bottom_right_radius);
    void fill_rect_with_rounded_corners(Gfx::FloatRect const& rect, Color const& color, CornerRadius const& top_left_radius, CornerRadius const& top_right_radius, CornerRadius const& bottom_left_radius, CornerRadius const& bottom_right_radius);

private:
    Context& m_context;

    struct State {
        Gfx::AffineTransform transform;
    };

    [[nodiscard]] State& state() { return m_state_stack.last(); }
    [[nodiscard]] State const& state() const { return m_state_stack.last(); }

    [[nodiscard]] Gfx::FloatRect to_clip_space(Gfx::FloatRect const& screen_rect) const;

    Vector<State, 1> m_state_stack;

    Program m_rectangle_program;
    Program m_rounded_rectangle_program;
    Program m_blit_program;
    Program m_linear_gradient_program;

    HashMap<GlyphsTextureKey, Gfx::IntRect> m_glyphs_texture_map;
    Gfx::IntSize m_glyphs_texture_size;
    GL::Texture m_glyphs_texture;

    Optional<Gfx::Bitmap&> m_target_bitmap;
    Optional<GL::Framebuffer> m_target_framebuffer;
};

}

namespace AK {

template<>
struct Traits<AccelGfx::Painter::GlyphsTextureKey> : public DefaultTraits<AccelGfx::Painter::GlyphsTextureKey> {
    static unsigned hash(AccelGfx::Painter::GlyphsTextureKey const& key)
    {
        return pair_int_hash(ptr_hash(key.font), key.code_point);
    }
};

}
