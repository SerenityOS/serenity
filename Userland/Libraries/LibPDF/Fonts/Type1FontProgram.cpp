/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
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
    HStemHM = 18,
    Hintmask,
    Cntrmask,
    RMoveTo,
    HMoveTo,
    VStemHM,
    RCurveLine,
    RLineCurve,
    VVCurveTo,
    HHCurveTo,
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
    Hflex,
    Flex,
    Hflex1,
    Flex1,
};

RefPtr<Gfx::Bitmap> Type1FontProgram::rasterize_glyph(DeprecatedFlyString const& char_name, float width, Gfx::GlyphSubpixelOffset subpixel_offset)
{
    constexpr auto base_color = Color::White;
    auto path = build_char(char_name, width, subpixel_offset);
    auto bounding_box = path.bounding_box().size();

    u32 w = (u32)ceilf(bounding_box.width()) + 2;
    u32 h = (u32)ceilf(bounding_box.height()) + 2;

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { w, h }).release_value_but_fixme_should_propagate_errors();
    Gfx::Painter painter { bitmap };
    Gfx::AntiAliasingPainter aa_painter { painter };

    aa_painter.fill_path(path, base_color);
    return bitmap;
}

Gfx::Path Type1FontProgram::build_char(DeprecatedFlyString const& char_name, float width, Gfx::GlyphSubpixelOffset subpixel_offset)
{
    auto maybe_glyph = m_glyph_map.get(char_name);
    if (!maybe_glyph.has_value())
        return {};

    auto const& glyph = maybe_glyph.value();
    auto transform = Gfx::AffineTransform()
                         .translate(subpixel_offset.to_float_point())
                         .multiply(glyph_transform_to_device_space(glyph, width));

    // Translate such that the top-left point is at [0, 0].
    auto bounding_box = glyph.path().bounding_box();
    Gfx::FloatPoint translation(-bounding_box.x(), -(bounding_box.y() + bounding_box.height()));
    transform.translate(translation);
    return glyph.path().copy_transformed(transform);
}

Gfx::FloatPoint Type1FontProgram::glyph_translation(DeprecatedFlyString const& char_name, float width) const
{
    auto maybe_glyph = m_glyph_map.get(char_name);
    if (!maybe_glyph.has_value())
        return {};

    auto& glyph = maybe_glyph.value();
    auto transform = glyph_transform_to_device_space(glyph, width);

    // Undo the translation we applied earlier.
    auto bounding_box = glyph.path().bounding_box();
    Gfx::FloatPoint translation(bounding_box.x(), bounding_box.y() + bounding_box.height());

    return transform.map(translation);
}

Gfx::AffineTransform Type1FontProgram::glyph_transform_to_device_space(Glyph const& glyph, float width) const
{
    auto scale = width / (m_font_matrix.a() * glyph.width() + m_font_matrix.e());
    auto transform = m_font_matrix;

    // Convert character space to device space.
    transform.scale(scale, -scale);

    return transform;
}

void Type1FontProgram::consolidate_glyphs()
{
    for (auto& [name, glyph] : m_glyph_map) {
        if (!glyph.is_accented_character())
            continue;
        auto maybe_base_glyph = m_glyph_map.get(glyph.accented_character().base_character);
        if (!maybe_base_glyph.has_value())
            continue;
        auto glyph_path = maybe_base_glyph.value().path();
        auto maybe_accent_glyph = m_glyph_map.get(glyph.accented_character().accent_character);
        if (maybe_accent_glyph.has_value()) {
            auto path = maybe_accent_glyph.value().path();
            glyph_path.append_path(move(path));
        }
        glyph.path() = glyph_path;
    }
}

PDFErrorOr<Type1FontProgram::Glyph> Type1FontProgram::parse_glyph(ReadonlyBytes const& data, Vector<ByteBuffer> const& subroutines, GlyphParserState& state, bool is_type2)
{
    auto push = [&](float value) -> PDFErrorOr<void> {
        if (state.sp >= state.stack.size())
            return error("Operand stack overflow");
        state.stack[state.sp++] = value;
        return {};
    };

    auto pop = [&]() -> float {
        return state.stack[--state.sp];
    };

    auto pop_front = [&]() {
        auto value = state.stack[0];
        --state.sp;
        for (size_t i = 0; i < state.sp; i++)
            state.stack[i] = state.stack[i + 1];
        return value;
    };

    auto& path = state.glyph.path();
    auto& point = state.point;

    // Core operations: move to, line to, curve to
    auto move_to = [&](float dx, float dy) {
        point.translate_by(dx, dy);
        if (is_type2)
            path.close();
        if (state.flex_feature) {
            state.flex_sequence[state.flex_index++] = state.point.x();
            state.flex_sequence[state.flex_index++] = state.point.y();
        } else {
            path.move_to(point);
        }
    };

    auto line_to = [&](float dx, float dy) {
        point.translate_by(dx, dy);
        path.line_to(point);
    };

    auto cube_bezier_curve_to = [&](float dx1, float dy1, float dx2, float dy2, float dx3, float dy3) {
        path.cubic_bezier_curve_to(
            point + Gfx::FloatPoint(dx1, dy1),
            point + Gfx::FloatPoint(dx1 + dx2, dy1 + dy2),
            point + Gfx::FloatPoint(dx1 + dx2 + dx3, dy1 + dy2 + dy3));
        point.translate_by(dx1 + dx2 + dx3, dy1 + dy2 + dy3);
    };

    // Shared operator logic
    auto rline_to = [&]() {
        auto dx = pop_front();
        auto dy = pop_front();
        line_to(dx, dy);
    };

    auto hvline_to = [&](bool horizontal) {
        while (state.sp > 0) {
            auto d = pop_front();
            float dx = horizontal ? d : 0;
            float dy = horizontal ? 0 : d;
            line_to(dx, dy);
            horizontal = !horizontal;
        }
    };

    auto rrcurve_to = [&]() {
        auto dx1 = pop_front();
        auto dy1 = pop_front();
        auto dx2 = pop_front();
        auto dy2 = pop_front();
        auto dx3 = pop_front();
        auto dy3 = pop_front();
        cube_bezier_curve_to(dx1, dy1, dx2, dy2, dx3, dy3);
    };

    auto hvcurve_to = [&](bool first_tangent_horizontal) {
        while (state.sp > 0) {
            auto d1 = pop_front();
            auto dx2 = pop_front();
            auto dy2 = pop_front();
            auto d3 = pop_front();
            float d4 = state.sp == 1 ? pop_front() : 0;

            auto dx1 = first_tangent_horizontal ? d1 : 0;
            auto dy1 = first_tangent_horizontal ? 0 : d1;
            auto dx3 = first_tangent_horizontal ? d4 : d3;
            auto dy3 = first_tangent_horizontal ? d3 : d4;
            cube_bezier_curve_to(dx1, dy1, dx2, dy2, dx3, dy3);
            first_tangent_horizontal = !first_tangent_horizontal;
        }
    };

    // Potential font width parsing for some commands (type2 only)
    bool is_first_command = true;
    enum EvenOrOdd {
        Even,
        Odd
    };
    auto maybe_read_width = [&](EvenOrOdd required_argument_count) {
        if (!is_type2 || !is_first_command || state.sp % 2 != required_argument_count)
            return;
        state.glyph.set_width(pop_front());
    };

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
            if (is_type2) {
                auto integer = float((a << 8) | b);
                auto fraction = float((c << 8) | d) / AK::NumericLimits<u16>::max();
                TRY(push(integer + fraction));
            } else
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

            // hints operators
            case HStemHM:
                state.n_hints += state.sp / 2;
                [[fallthrough]];
            case HStem:
                maybe_read_width(Odd);
                state.sp = 0;
                break;

            case VStemHM:
                state.n_hints += state.sp / 2;
                [[fallthrough]];
            case VStem:
                maybe_read_width(Odd);
                state.sp = 0;
                break;

            case Hintmask:
            case Cntrmask: {
                maybe_read_width(Odd);
                state.n_hints += state.sp / 2;
                auto hint_bytes = (state.n_hints + 8 - 1) / 8;
                TRY(require(hint_bytes));
                i += hint_bytes;
                state.sp = 0;
                break;
            }

            // move-to operators
            case RMoveTo: {
                maybe_read_width(Odd);
                auto dy = pop();
                auto dx = pop();
                move_to(dx, dy);
                state.sp = 0;
                break;
            }
            case HMoveTo: {
                maybe_read_width(Even);
                auto dx = pop();
                move_to(dx, 0);
                state.sp = 0;
                break;
            }
            case VMoveTo: {
                maybe_read_width(Even);
                auto dy = pop();
                move_to(0, dy);
                state.sp = 0;
                break;
            }

            // line-to operators
            case RLineTo: {
                while (state.sp >= 2)
                    rline_to();
                state.sp = 0;
                break;
            }
            case HLineTo: {
                hvline_to(true);
                state.sp = 0;
                break;
            }
            case VLineTo: {
                hvline_to(false);
                state.sp = 0;
                break;
            }

            case RRCurveTo: {
                while (state.sp >= 6)
                    rrcurve_to();
                VERIFY(state.sp == 0);
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
                    auto const& subr = subroutines[subr_number];
                    if (subr.is_empty())
                        return error("Empty subroutine");

                    TRY(parse_glyph(subr, subroutines, state, is_type2));
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
                    // FIXME: Do something with these?
                    state.sp = 0;
                    break;

                case Seac: {
                    auto achar = pop();
                    auto bchar = pop();
                    auto ady = pop();
                    auto adx = pop();
                    // auto asb = pop();
                    state.glyph.set_accented_character(AccentedCharacter { (u8)bchar, (u8)achar, adx, ady });
                    state.sp = 0;
                    break;
                }

                case Div: {
                    auto num2 = pop();
                    auto num1 = pop();

                    TRY(push(num2 ? num1 / num2 : 0.0f));
                    break;
                }

                case CallOtherSubr: {
                    [[maybe_unused]] auto othersubr_number = pop();
                    auto n = static_cast<int>(pop());
                    for (int i = 0; i < n; ++i)
                        state.postscript_stack[state.postscript_sp++] = pop();
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

                case Hflex:
                case Flex:
                case Hflex1:
                case Flex1:
                    // TODO: implement these
                    state.sp = 0;
                    break;

                default:
                    return error(DeprecatedString::formatted("Unhandled command: 12 {}", data[i]));
                }
                break;
            }

            case HSbW: {
                auto wx = pop();
                auto sbx = pop();

                state.glyph.set_width(wx);
                state.point = { sbx, 0.0f };
                state.sp = 0;
                break;
            }

            case EndChar: {
                maybe_read_width(Odd);
                if (is_type2)
                    path.close();
                break;
            }

            case VHCurveTo: {
                hvcurve_to(false);
                state.sp = 0;
                break;
            }

            case HVCurveTo: {
                hvcurve_to(true);
                state.sp = 0;
                break;
            }

            case VVCurveTo: {
                float dx1 = 0;
                if (state.sp % 2 == 1)
                    dx1 = pop_front();
                do {
                    auto dy1 = pop_front();
                    auto dx2 = pop_front();
                    auto dy2 = pop_front();
                    auto dy3 = pop_front();
                    cube_bezier_curve_to(dx1, dy1, dx2, dy2, 0, dy3);
                    dx1 = 0;
                } while (state.sp >= 4);
                state.sp = 0;
                break;
            }

            case HHCurveTo: {
                float dy1 = 0;
                if (state.sp % 2 == 1)
                    dy1 = pop_front();
                do {
                    auto dx1 = pop_front();
                    auto dx2 = pop_front();
                    auto dy2 = pop_front();
                    auto dx3 = pop_front();
                    cube_bezier_curve_to(dx1, dy1, dx2, dy2, dx3, 0);
                    dy1 = 0;
                } while (state.sp >= 4);
                state.sp = 0;
                break;
            }

            case RCurveLine: {
                while (state.sp >= 8) {
                    rrcurve_to();
                }
                rline_to();
                state.sp = 0;
                break;
            }

            case RLineCurve: {
                while (state.sp >= 8) {
                    rline_to();
                }
                rrcurve_to();
                break;
            }

            default:
                return error(DeprecatedString::formatted("Unhandled command: {}", v));
            }

            is_first_command = false;
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
