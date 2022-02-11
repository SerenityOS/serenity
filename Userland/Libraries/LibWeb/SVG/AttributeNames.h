/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::SVG::AttributeNames {

#define ENUMERATE_SVG_ATTRIBUTES(E) \
    E(attributeName)                \
    E(attributeType)                \
    E(baseFrequency)                \
    E(baseProfile)                  \
    E(calcMode)                     \
    E(clipPathUnits)                \
    E(contentScriptType)            \
    E(contentStyleType)             \
    E(cx)                           \
    E(cy)                           \
    E(diffuseConstant)              \
    E(edgeMode)                     \
    E(filterUnits)                  \
    E(glyphRef)                     \
    E(gradientTransform)            \
    E(gradientUnits)                \
    E(height)                       \
    E(kernelMatrix)                 \
    E(kernelUnitLength)             \
    E(keyPoints)                    \
    E(keySplines)                   \
    E(keyTimes)                     \
    E(lengthAdjust)                 \
    E(limitingConeAngle)            \
    E(markerHeight)                 \
    E(markerUnits)                  \
    E(markerWidth)                  \
    E(maskContentUnits)             \
    E(maskUnits)                    \
    E(numOctaves)                   \
    E(pathLength)                   \
    E(patternContentUnits)          \
    E(patternTransform)             \
    E(patternUnits)                 \
    E(points)                       \
    E(pointsAtX)                    \
    E(pointsAtY)                    \
    E(pointsAtZ)                    \
    E(preserveAlpha)                \
    E(preserveAspectRatio)          \
    E(primitiveUnits)               \
    E(r)                            \
    E(refX)                         \
    E(refY)                         \
    E(repeatCount)                  \
    E(repeatDur)                    \
    E(requiredExtensions)           \
    E(rx)                           \
    E(ry)                           \
    E(requiredFeatures)             \
    E(specularConstant)             \
    E(specularExponent)             \
    E(spreadMethod)                 \
    E(startOffset)                  \
    E(stdDeviation)                 \
    E(stitchTiles)                  \
    E(surfaceScale)                 \
    E(systemLanguage)               \
    E(tableValues)                  \
    E(targetX)                      \
    E(targetY)                      \
    E(textLength)                   \
    E(version)                      \
    E(viewBox)                      \
    E(viewTarget)                   \
    E(width)                        \
    E(x)                            \
    E(x1)                           \
    E(x2)                           \
    E(xChannelSelector)             \
    E(y)                            \
    E(y1)                           \
    E(y2)                           \
    E(yChannelSelector)             \
    E(zoomAndPan)

#define __ENUMERATE_SVG_ATTRIBUTE(name) extern FlyString name;
ENUMERATE_SVG_ATTRIBUTES(__ENUMERATE_SVG_ATTRIBUTE)
#undef __ENUMERATE_SVG_ATTRIBUTE

}
