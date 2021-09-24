/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Gfx {

class Bitmap;
class CharacterBitmap;
class Color;
class DisjointRectSet;
class Emoji;
class Font;
class GlyphBitmap;
class ImageDecoder;
struct FontMetrics;

template<typename T>
class Line;

class Painter;
class Palette;
class PaletteImpl;
class Path;
class ShareableBitmap;
class StylePainter;
struct SystemTheme;
class Triangle;

template<typename T>
class Point;

template<typename T>
class Size;

template<typename T>
class Rect;

using IntLine = Line<int>;
using FloatLine = Line<float>;

using IntRect = Rect<int>;
using FloatRect = Rect<float>;

using IntPoint = Point<int>;
using FloatPoint = Point<float>;

using IntSize = Size<int>;
using FloatSize = Size<float>;

enum class BitmapFormat;
enum class ColorRole;
enum class TextAlignment;

}

using Gfx::Color;
