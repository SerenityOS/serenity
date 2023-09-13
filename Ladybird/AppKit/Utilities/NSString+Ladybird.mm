/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>

#import <Utilities/Conversions.h>
#import <Utilities/NSString+Ladybird.h>

static BOOL code_point_is_whitespace(u32 code_point)
{
    static auto white_space_category = Unicode::property_from_string("White_Space"sv);

    if (!white_space_category.has_value())
        return is_ascii_space(code_point);

    return Unicode::code_point_has_property(code_point, *white_space_category);
}

@implementation NSString (Ladybird)

- (NSString*)stringByCollapsingConsecutiveWhitespace
{
    auto* result = [NSMutableString string];

    auto const* string = [self UTF8String];
    Utf8View code_points { StringView { string, strlen(string) } };

    bool currently_skipping_spaces = false;

    for (auto code_point : code_points) {
        if (code_point_is_whitespace(code_point)) {
            if (!currently_skipping_spaces) {
                currently_skipping_spaces = true;
                [result appendString:@" "];
            }

            continue;
        }

        auto code_point_string = String::from_code_point(code_point);
        [result appendString:Ladybird::string_to_ns_string(code_point_string)];

        currently_skipping_spaces = false;
    }

    return result;
}

@end
