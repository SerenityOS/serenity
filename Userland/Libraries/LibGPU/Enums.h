/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace GPU {

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

enum class BlendEquation {
    Add,
    Subtract,
    ReverseSubtract,
    Min,
    Max,
};

enum class BlendFactor {
    Zero,
    One,
    SrcColor,
    OneMinusSrcColor,
    DstColor,
    OneMinusDstColor,
    SrcAlpha,
    OneMinusSrcAlpha,
    DstAlpha,
    OneMinusDstAlpha,
    ConstantColor,
    OneMinusConstantColor,
    ConstantAlpha,
    OneMinusConstantAlpha,
    SrcAlphaSaturate,
};

enum class ColorControl {
    SingleColor,
    SeparateSpecularColor,
};

enum class ColorMaterialFace {
    Front,
    Back,
    FrontAndBack,
};

enum class ColorMaterialMode {
    Ambient,
    AmbientAndDiffuse,
    Diffuse,
    Emissive,
    Specular,
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

enum Face {
    Front = 0,
    Back = 1,
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
    Lines,
    LineLoop,
    LineStrip,
    Points,
    TriangleFan,
    Triangles,
    TriangleStrip,
    Quads,
};

enum StencilOperation {
    Decrement,
    DecrementWrap,
    Increment,
    IncrementWrap,
    Invert,
    Keep,
    Replace,
    Zero,
};

enum StencilTestFunction {
    Always,
    Equal,
    Greater,
    GreaterOrEqual,
    Less,
    LessOrEqual,
    Never,
    NotEqual,
};

enum TexCoordGenerationCoordinate : u8 {
    None = 0x0,
    S = 0x1,
    T = 0x2,
    R = 0x4,
    Q = 0x8,
    All = 0xF,
};

enum class TexCoordGenerationMode {
    ObjectLinear,
    EyeLinear,
    SphereMap,
    ReflectionMap,
    NormalMap,
};

}
