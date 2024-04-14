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
    CallGsubr = 29, // Type 2 only
    VHCurveTo,
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
    auto scale = width == 0.0f ? 0.0f : (width / (m_font_matrix.a() * glyph.width() + m_font_matrix.e()));
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
            auto origin = glyph.accented_character().accent_origin;
            auto path = maybe_accent_glyph.value().path();
            Gfx::AffineTransform translation { 1, 0, 0, 1, origin.x(), origin.y() };
            glyph_path.append_path(path.copy_transformed(translation));
        }
        glyph.path() = glyph_path;
    }
}

ErrorOr<Type1FontProgram::Glyph> Type1FontProgram::parse_glyph(ReadonlyBytes const& data, Vector<ByteBuffer> const& local_subroutines, Vector<ByteBuffer> const& global_subroutines, GlyphParserState& state, bool is_type2)
{
    // Type 1 Font Format: https://adobe-type-tools.github.io/font-tech-notes/pdfs/T1_SPEC.pdf (Chapter 6: CharStrings dictionary)
    // Type 2 Charstring Format: https://adobe-type-tools.github.io/font-tech-notes/pdfs/5177.Type2.pdf

    auto push = [&](float value) -> ErrorOr<void> {
        if (state.sp >= state.stack.size())
            return AK::Error::from_string_literal("Operand stack overflow");
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

    auto flex = [&](float dx1, float dy1, float dx2, float dy2, float dx3, float dy3,
                    float dx4, float dy4, float dx5, float dy5, float dx6, float dy6,
                    float flex_depth) {
        // FIXME: The beziers are supposed to collapse to a line if the displacement is less
        //        than flex_depth. For now, we rely on antialiasing, but if we want to implement
        //        this, we'd have to add a Flex segment type to path and then look at flex_depth
        //        at rasterization time.
        (void)flex_depth;
        cube_bezier_curve_to(dx1, dy1, dx2, dy2, dx3, dy3);
        cube_bezier_curve_to(dx4, dy4, dx5, dy5, dx6, dy6);
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
    enum EvenOrOdd {
        Even,
        Odd
    };
    auto maybe_read_width = [&](EvenOrOdd required_argument_count) {
        if (!is_type2 || !state.is_first_command)
            return;
        state.is_first_command = false;
        if (state.sp % 2 == required_argument_count)
            state.glyph.set_width(pop_front());
    };

    // Parse the stream of parameters and commands that make up a glyph outline.
    for (size_t i = 0; i < data.size(); ++i) {
        auto require = [&](unsigned num) -> ErrorOr<void> {
            if (i + num >= data.size())
                return AK::Error::from_string_literal("Malformed glyph outline definition");
            return {};
        };

        int v = data[i];
        if (v == 255) {
            TRY(require(4));
            // Both Type 1 and Type 2 spec:
            // "If the charstring byte contains the value 255, the next four bytes indicate a two’s complement signed number.
            //  The first of these four bytes contains the highest order bits [...]"
            i32 a = static_cast<i32>((data[i + 1] << 24) | (data[i + 2] << 16) | (data[i + 3] << 8) | data[i + 4]);
            i += 4;
            if (is_type2) {
                // Just in the Type 2 spec: "This number is interpreted as a Fixed; that is, a signed number with 16 bits of fraction."
                TRY(push(a / (float)0x1'0000));
            } else {
                TRY(push(a));
            }
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
        } else if (v == 28) {
            if (is_type2) {
                // Type 2 spec: "In addition to the 32 to 255 range of values, a ShortInt value is specified by using the operator (28)
                // followed by two bytes which represent numbers between –32768 and +32767. The most significant byte follows the (28)."
                TRY(require(2));
                i16 a = static_cast<i16>((data[i + 1] << 8) | data[i + 2]);
                i += 2;
                TRY(push(a));
            } else {
                return AK::Error::from_string_literal("CFF Subr command 28 only valid in type2 data");
            }
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

            case CallGsubr:
                if (!is_type2)
                    return AK::Error::from_string_literal("CFF Gsubr only valid in type2 data");
                [[fallthrough]];
            case CallSubr: {
                Vector<ByteBuffer> const& subroutines = v == CallSubr ? local_subroutines : global_subroutines;
                auto subr_number = pop();

                if (is_type2) {
                    // Type 2 spec:
                    // "The numbering of subroutines is encoded more compactly by using the negative half of the number space, which effectively
                    //  doubles the number of compactly encodable subroutine numbers. The bias applied depends on the number of subrs (gsubrs).
                    //  If the number of subrs (gsubrs) is less than 1240, the bias is 107. Otherwise if it is less than 33900, it is 1131; otherwise
                    //  it is 32768. This bias is added to the encoded subr (gsubr) number to find the appropriate entry in the subr (gsubr) array."
                    if (subroutines.size() < 1240)
                        subr_number += 107;
                    else if (subroutines.size() < 33900)
                        subr_number += 1131;
                    else
                        subr_number += 32768;
                }

                if (static_cast<size_t>(subr_number) >= subroutines.size())
                    return AK::Error::from_string_literal("Subroutine index out of range");

                auto const& subr = subroutines[subr_number];
                if (subr.is_empty())
                    return AK::Error::from_string_literal("Empty subroutine");

                TRY(parse_glyph(subr, local_subroutines, global_subroutines, state, is_type2));
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
                    // Type 1 spec, 8.3 Flex:
                    // "5. Insert at the beginning of the sequence the coordinate of the reference point relative to the starting point.
                    //     There are now seven coordinate values (14 numbers) in the sequence.
                    //  6. Place a call to Subrs entry 1 at the beginning of this sequence of coordinates, and place an rmoveto command
                    //     and a call to Subrs entry 2 after each of the seven coordinate pairs in the sequence.
                    //  7. Place the Flex height control parameter and the final coordinate expressed in absolute terms (in character space)
                    //     followed by a call to Subrs entry 0 at the end."
                    // [...]
                    // Type 1 spec, 8.4 First Four Subrs Entries:
                    // "Subrs entry number 0: 3 0 callothersubr pop pop setcurrentpoint return
                    //  Subrs entry number 1: 0 1 callothersubr return
                    //  Subrs entry number 2: 0 2 callothersubr return
                    //  Subrs entry number 3: return
                    // [...]
                    //  Subrs entry 0 passes the final three arguments in the Flex mechanism into OtherSubrs entry 0.
                    //  That procedure puts the new current point on top of the Post- Script interpreter operand stack.
                    //  Subrs entry 0 then transfers these two coordinate values to the Type 1 BuildChar operand stack
                    // with two pop commands and makes them the current point coordinates with a setcurrentpoint command."
                    enum OtherSubrCommand {
                        EndFlex = 0,
                        StartFlex = 1,
                        AddFlexPoint = 2,
                    };
                    auto othersubr_number = (OtherSubrCommand)pop();

                    auto n = static_cast<int>(pop());

                    switch ((OtherSubrCommand)othersubr_number) {
                    case EndFlex: {
                        if (n != 3)
                            return AK::Error::from_string_literal("Unexpected argument code for othersubr 0");

                        auto y = pop();
                        auto x = pop();
                        [[maybe_unused]] auto flex_height = pop();

                        state.postscript_stack[state.postscript_sp++] = y;
                        state.postscript_stack[state.postscript_sp++] = x;

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
                        break;
                    }
                    case StartFlex:
                        if (n != 0)
                            return AK::Error::from_string_literal("Unexpected argument code for othersubr 1");
                        state.flex_feature = true;
                        state.flex_index = 0;
                        state.sp = 0;
                        break;
                    case AddFlexPoint:
                        if (n != 0)
                            return AK::Error::from_string_literal("Unexpected argument code for othersubr 2");
                        // We do this directly in move_to().
                        state.sp = 0;
                        break;
                    default:
                        for (int i = 0; i < n; ++i)
                            state.postscript_stack[state.postscript_sp++] = pop();
                        break;
                    }

                    break;
                }

                case Pop:
                    TRY(push(state.postscript_stack[--state.postscript_sp]));
                    break;

                case SetCurrentPoint: {
                    auto y = pop();
                    auto x = pop();

                    // FIXME: Gfx::Path behaves weirdly if a cubic_bezier_curve_to(a, b, c)
                    //        is followed by move(c). Figure out why, fix in Gfx::Path, then
                    //        remove this check here.
                    //        Run `Build/lagom/bin/pdf --render out.png Tests/LibPDF/type1.pdf`
                    //        as test -- if the output looks good, then all's good. At the moment,
                    //        the output looks broken without the if here.
                    Gfx::FloatPoint new_point { x, y };
                    if (state.point != new_point) {
                        state.point = new_point;
                        path.move_to(state.point);
                    }
                    state.sp = 0;
                    break;
                }

                case Flex: {
                    auto flex_depth = pop();
                    auto dy6 = pop();
                    auto dx6 = pop();
                    auto dy5 = pop();
                    auto dx5 = pop();
                    auto dy4 = pop();
                    auto dx4 = pop();
                    auto dy3 = pop();
                    auto dx3 = pop();
                    auto dy2 = pop();
                    auto dx2 = pop();
                    auto dy1 = pop();
                    auto dx1 = pop();
                    flex(dx1, dy1, dx2, dy2, dx3, dy3, dx4, dy4, dx5, dy5, dx6, dy6, flex_depth);
                    state.sp = 0;
                    break;
                }
                case Hflex: {
                    auto flex_depth = 50;
                    auto dx6 = pop();
                    auto dx5 = pop();
                    auto dx4 = pop();
                    auto dx3 = pop();
                    auto dy2 = pop();
                    auto dx2 = pop();
                    auto dx1 = pop();
                    flex(dx1, 0, dx2, dy2, dx3, 0, dx4, 0, dx5, -dy2, dx6, 0, flex_depth);
                    state.sp = 0;
                    break;
                }
                case Hflex1: {
                    auto flex_depth = 50;
                    auto dx6 = pop();
                    auto dy5 = pop();
                    auto dx5 = pop();
                    auto dx4 = pop();
                    auto dx3 = pop();
                    auto dy2 = pop();
                    auto dx2 = pop();
                    auto dy1 = pop();
                    auto dx1 = pop();
                    flex(dx1, dy1, dx2, dy2, dx3, 0, dx4, 0, dx5, dy5, dx6, -(dy1 + dy2 + dy5), flex_depth);
                    state.sp = 0;
                    break;
                }
                case Flex1: {
                    auto flex_depth = 50;
                    auto d6 = pop();
                    auto dy5 = pop();
                    auto dx5 = pop();
                    auto dy4 = pop();
                    auto dx4 = pop();
                    auto dy3 = pop();
                    auto dx3 = pop();
                    auto dy2 = pop();
                    auto dx2 = pop();
                    auto dy1 = pop();
                    auto dx1 = pop();

                    float dx6, dy6;
                    auto dx = dx1 + dx2 + dx3 + dx4 + dx5;
                    auto dy = dy1 + dy2 + dy3 + dy4 + dy5;
                    if (fabs(dx) > fabs(dy)) {
                        dx6 = d6;
                        dy6 = -dy;
                    } else {
                        dx6 = -dx;
                        dy6 = d6;
                    }

                    flex(dx1, dy1, dx2, dy2, dx3, dy3, dx4, dy4, dx5, dy5, dx6, dy6, flex_depth);
                    state.sp = 0;
                    break;
                }

                default:
                    dbgln("Unhandled command: 12 {}", data[i]);
                    return AK::Error::from_string_literal("Unhandled command");
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
                if (is_type2) {
                    // Type 2 spec:
                    // "In addition to the optional width (...) endchar may have four extra arguments that correspond exactly
                    //  to the last four arguments of the Type 1 charstring command “seac”"
                    if (state.sp == 4) {
                        auto achar = pop();
                        auto bchar = pop();
                        auto ady = pop();
                        auto adx = pop();
                        state.glyph.set_accented_character(AccentedCharacter { (u8)bchar, (u8)achar, adx, ady });
                    }
                    path.close();
                }
                state.sp = 0;
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
                dbgln("Unhandled command: {}", v);
                // https://adobe-type-tools.github.io/font-tech-notes/pdfs/5177.Type2.pdf
                // says "The behavior of undefined operators is unspecified." but
                // https://learn.microsoft.com/en-us/typography/opentype/spec/cff2
                // says "When an unrecognized operator is encountered, it is ignored and
                // the stack is cleared."
                //
                // Some type 0 CIDFontType0C fonts (i.e. CID-keyed non-OpenType CFF fonts)
                // depend on the latter, even though they're governed by the former spec.
                state.sp = 0;
                break;
            }
        }
    }

    return state.glyph;
}

}
