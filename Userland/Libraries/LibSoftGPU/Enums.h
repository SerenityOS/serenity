/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace SoftGPU {

enum class AlphaTestFunction {
    Never,
    Always,
    Less,
    LessOrEqual,
    Equal,
    NotEqual,
    GreaterOrEqual,
    Greater,
};

enum class BlendFactor {
    Zero,
    One,
    SrcAlpha,
    OneMinusSrcAlpha,
    SrcColor,
    OneMinusSrcColor,
    DstAlpha,
    OneMinusDstAlpha,
    DstColor,
    OneMinusDstColor,
    SrcAlphaSaturate,
};

enum class DepthTestFunction {
    Never,
    Always,
    Less,
    LessOrEqual,
    Equal,
    NotEqual,
    GreaterOrEqual,
    Greater,
};

enum FogMode {
    Linear,
    Exp,
    Exp2
};

enum class PolygonMode {
    Point,
    Line,
    Fill,
};

enum class WindingOrder {
    Clockwise,
    CounterClockwise,
};

enum class PrimitiveType {
    Triangles,
    TriangleStrip,
    TriangleFan,
    Quads,
};

}
