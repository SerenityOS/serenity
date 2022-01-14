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

enum TexCoordGenerationCoordinate {
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
