/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import <Utilities/Conversions.h>

namespace Ladybird {

String ns_string_to_string(NSString* string)
{
    auto const* utf8 = [string UTF8String];
    return MUST(String::from_utf8({ utf8, strlen(utf8) }));
}

NSString* string_to_ns_string(StringView string)
{
    auto* data = [NSData dataWithBytes:string.characters_without_null_termination() length:string.length()];
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

Gfx::IntRect ns_rect_to_gfx_rect(NSRect rect)
{
    return {
        static_cast<int>(rect.origin.x),
        static_cast<int>(rect.origin.y),
        static_cast<int>(rect.size.width),
        static_cast<int>(rect.size.height),
    };
}

NSRect gfx_rect_to_ns_rect(Gfx::IntRect rect)
{
    return NSMakeRect(
        static_cast<CGFloat>(rect.x()),
        static_cast<CGFloat>(rect.y()),
        static_cast<CGFloat>(rect.width()),
        static_cast<CGFloat>(rect.height()));
}

Gfx::IntSize ns_size_to_gfx_size(NSSize size)
{
    return {
        static_cast<int>(size.width),
        static_cast<int>(size.height),
    };
}

NSSize gfx_size_to_ns_size(Gfx::IntSize size)
{
    return NSMakeSize(
        static_cast<CGFloat>(size.width()),
        static_cast<CGFloat>(size.height()));
}

Gfx::IntPoint ns_point_to_gfx_point(NSPoint point)
{
    return {
        static_cast<int>(point.x),
        static_cast<int>(point.y),
    };
}

NSPoint gfx_point_to_ns_point(Gfx::IntPoint point)
{
    return NSMakePoint(
        static_cast<CGFloat>(point.x()),
        static_cast<CGFloat>(point.y()));
}

}
