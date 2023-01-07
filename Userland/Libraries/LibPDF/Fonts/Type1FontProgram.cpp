/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/PathRasterizer.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

enum Command {
    HStem = 1,
    VStem = 3,
    VMoveTo,
    RLineTo,
    HLineTo,
    VLineTo,
    RRCurveTo,
    ClosePath,
    CallSubr,
    Return,
    Extended,
    HSbW,
    EndChar,
    RMoveTo = 21,
    HMoveTo,
    VHCurveTo = 30,
    HVCurveTo
};

enum ExtendedCommand {
    DotSection,
    VStem3,
    HStem3,
    Seac = 6,
    Div = 12,
    CallOtherSubr = 16,
    Pop,
    SetCurrentPoint = 33,
};

RefPtr<Gfx::Bitmap> Type1FontProgram::rasterize_glyph(u32 char_code, float width, Gfx::GlyphSubpixelOffset subpixel_offset)
{
    auto path = build_char(char_code, width, subpixel_offset);
    auto bounding_box = path.bounding_box().size();

    u32 w = (u32)ceilf(bounding_box.width()) + 2;
    u32 h = (u32)ceilf(bounding_box.height()) + 2;

    Gfx::PathRasterizer rasterizer(Gfx::IntSize(w, h));
    rasterizer.draw_path(path);
    return rasterizer.accumulate();
}

Gfx::Path Type1FontProgram::build_char(u32 char_code, float width, Gfx::GlyphSubpixelOffset subpixel_offset)
{
    auto maybe_glyph = m_glyph_map.get(char_code);
    if (!maybe_glyph.has_value())
        return {};

    auto& glyph = maybe_glyph.value();
    auto transform = Gfx::AffineTransform()
                         .translate(subpixel_offset.to_float_point())
                         .multiply(glyph_transform_to_device_space(glyph, width));

    // Translate such that the top-left point is at [0, 0].
    auto bounding_box = glyph.path.bounding_box();
    Gfx::FloatPoint translation(-bounding_box.x(), -(bounding_box.y() + bounding_box.height()));
    transform.translate(translation);

    return glyph.path.copy_transformed(transform);
}

Gfx::FloatPoint Type1FontProgram::glyph_translation(u32 char_code, float width) const
{
    auto maybe_glyph = m_glyph_map.get(char_code);
    if (!maybe_glyph.has_value())
        return {};

    auto& glyph = maybe_glyph.value();
    auto transform = glyph_transform_to_device_space(glyph, width);

    // Undo the translation we applied earlier.
    auto bounding_box = glyph.path.bounding_box();
    Gfx::FloatPoint translation(bounding_box.x(), bounding_box.y() + bounding_box.height());

    return transform.map(translation);
}

Gfx::AffineTransform Type1FontProgram::glyph_transform_to_device_space(Glyph const& glyph, float width) const
{
    auto scale = width / (m_font_matrix.a() * glyph.width + m_font_matrix.e());
    auto transform = m_font_matrix;

    // Convert character space to device space.
    transform.scale(scale, -scale);

    return transform;
}

PDFErrorOr<Type1FontProgram::Glyph> Type1FontProgram::parse_glyph(ReadonlyBytes const& data, Vector<ByteBuffer> const& subroutines, GlyphParserState& state)
{
    auto push = [&](float value) -> PDFErrorOr<void> {
        if (state.sp >= state.stack.size())
            return error("Operand stack overflow");
        state.stack[state.sp++] = value;
        return {};
    };

    auto pop = [&]() -> float {
        return state.sp ? state.stack[--state.sp] : 0.0f;
    };

    auto& path = state.glyph.path;

    // Parse the stream of parameters and commands that make up a glyph outline.
    for (size_t i = 0; i < data.size(); ++i) {
        auto require = [&](unsigned num) -> PDFErrorOr<void> {
            if (i + num >= data.size())
                return error("Malformed glyph outline definition");
            return {};
        };

        int v = data[i];
        if (v == 255) {
            TRY(require(4));
            int a = data[++i];
            int b = data[++i];
            int c = data[++i];
            int d = data[++i];
            TRY(push((a << 24) + (b << 16) + (c << 8) + d));
        } else if (v >= 251) {
            TRY(require(1));
            auto w = data[++i];
            TRY(push(-((v - 251) * 256) - w - 108));
        } else if (v >= 247) {
            TRY(require(1));
            auto w = data[++i];
            TRY(push(((v - 247) * 256) + w + 108));
        } else if (v >= 32) {
            TRY(push(v - 139));
        } else {
            // Not a parameter but a command byte.
            switch (v) {
            case HStem:
            case VStem:
                state.sp = 0;
                break;

            case VMoveTo: {
                auto dy = pop();

                state.point.translate_by(0.0f, dy);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case RLineTo: {
                auto dy = pop();
                auto dx = pop();

                state.point.translate_by(dx, dy);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case HLineTo: {
                auto dx = pop();

                state.point.translate_by(dx, 0.0f);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case VLineTo: {
                auto dy = pop();

                state.point.translate_by(0.0f, dy);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case RRCurveTo: {
                auto dy3 = pop();
                auto dx3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dy1 = pop();
                auto dx1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(dx1, dy1),
                    point + Gfx::FloatPoint(dx1 + dx2, dy1 + dy2),
                    point + Gfx::FloatPoint(dx1 + dx2 + dx3, dy1 + dy2 + dy3));

                point.translate_by(dx1 + dx2 + dx3, dy1 + dy2 + dy3);
                state.sp = 0;
                break;
            }

            case ClosePath:
                path.close();
                state.sp = 0;
                break;

            case CallSubr: {
                auto subr_number = pop();
                if (static_cast<size_t>(subr_number) >= subroutines.size())
                    return error("Subroutine index out of range");

                // Subroutines 0-2 handle the flex feature.
                if (subr_number == 0) {
                    if (state.flex_index != 14)
                        break;

                    auto& flex = state.flex_sequence;

                    path.cubic_bezier_curve_to(
                        { flex[2], flex[3] },
                        { flex[4], flex[5] },
                        { flex[6], flex[7] });
                    path.cubic_bezier_curve_to(
                        { flex[8], flex[9] },
                        { flex[10], flex[11] },
                        { flex[12], flex[13] });

                    state.flex_feature = false;
                    state.sp = 0;
                } else if (subr_number == 1) {
                    state.flex_feature = true;
                    state.flex_index = 0;
                    state.sp = 0;
                } else if (subr_number == 2) {
                    state.sp = 0;
                } else {
                    auto subr = subroutines[subr_number];
                    if (subr.is_empty())
                        return error("Empty subroutine");

                    TRY(parse_glyph(subr, subroutines, state));
                }
                break;
            }

            case Return:
                break;

            case Extended: {
                TRY(require(1));
                switch (data[++i]) {
                case DotSection:
                case VStem3:
                case HStem3:
                case Seac:
                    // FIXME: Do something with these?
                    state.sp = 0;
                    break;

                case Div: {
                    auto num2 = pop();
                    auto num1 = pop();

                    TRY(push(num2 ? num1 / num2 : 0.0f));
                    break;
                }

                case CallOtherSubr: {
                    auto othersubr_number = pop();
                    auto n = static_cast<int>(pop());

                    if (othersubr_number == 0) {
                        state.postscript_stack[state.postscript_sp++] = pop();
                        state.postscript_stack[state.postscript_sp++] = pop();
                        pop();
                    } else if (othersubr_number == 3) {
                        state.postscript_stack[state.postscript_sp++] = 3;
                    } else {
                        for (int i = 0; i < n; ++i)
                            state.postscript_stack[state.postscript_sp++] = pop();
                    }

                    (void)othersubr_number;
                    break;
                }

                case Pop:
                    TRY(push(state.postscript_stack[--state.postscript_sp]));
                    break;

                case SetCurrentPoint: {
                    auto y = pop();
                    auto x = pop();

                    state.point = { x, y };
                    path.move_to(state.point);
                    state.sp = 0;
                    break;
                }

                default:
                    return error(DeprecatedString::formatted("Unhandled command: 12 {}", data[i]));
                }
                break;
            }

            case HSbW: {
                auto wx = pop();
                auto sbx = pop();

                state.glyph.width = wx;
                state.point = { sbx, 0.0f };
                state.sp = 0;
                break;
            }

            case EndChar:
                break;

            case RMoveTo: {
                auto dy = pop();
                auto dx = pop();

                state.point.translate_by(dx, dy);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case HMoveTo: {
                auto dx = pop();

                state.point.translate_by(dx, 0.0f);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case VHCurveTo: {
                auto dx3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dy1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(0.0f, dy1),
                    point + Gfx::FloatPoint(dx2, dy1 + dy2),
                    point + Gfx::FloatPoint(dx2 + dx3, dy1 + dy2));

                point.translate_by(dx2 + dx3, dy1 + dy2);
                state.sp = 0;
                break;
            }

            case HVCurveTo: {
                auto dy3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dx1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(dx1, 0.0f),
                    point + Gfx::FloatPoint(dx1 + dx2, dy2),
                    point + Gfx::FloatPoint(dx1 + dx2, dy2 + dy3));

                point.translate_by(dx1 + dx2, dy2 + dy3);
                state.sp = 0;
                break;
            }

            default:
                return error(DeprecatedString::formatted("Unhandled command: {}", v));
            }
        }
    }

    return state.glyph;
}

Error Type1FontProgram::error(
    DeprecatedString const& message
#ifdef PDF_DEBUG
    ,
    SourceLocation loc
#endif
)
{
#ifdef PDF_DEBUG
    dbgln("\033[31m{} Type 1 font error: {}\033[0m", loc, message);
#endif

    return Error { Error::Type::MalformedPDF, message };
}

}
