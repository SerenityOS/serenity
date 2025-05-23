/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Gfx {

class AffineTransform;
class Bitmap;
class CMYKBitmap;
class ImmutableBitmap;
class CharacterBitmap;
class Color;

template<typename T>
class DisjointRectSet;

class Emoji;
class Font;
class GlyphBitmap;
class ImageDecoder;
struct FontPixelMetrics;
class ScaledFont;

template<typename T>
class Line;

class AntiAliasingPainter;
class Painter;
class Palette;
class PaletteImpl;
class Path;
class ShareableBitmap;
class StylePainter;
struct SystemTheme;

template<typename T>
class Triangle;

template<typename T>
class Point;

template<typename T>
class Size;

template<typename T>
class Rect;

template<typename T>
class Quad;

using DisjointIntRectSet = DisjointRectSet<int>;
using DisjointFloatRectSet = DisjointRectSet<float>;

using IntLine = Line<int>;
using FloatLine = Line<float>;

using IntRect = Rect<int>;
using FloatRect = Rect<float>;

using IntPoint = Point<int>;
using FloatPoint = Point<float>;

using IntSize = Size<int>;
using FloatSize = Size<float>;

using FloatQuad = Quad<float>;

enum class BitmapFormat;
enum class ColorRole;
enum class TextAlignment;

}

using Gfx::Color;
