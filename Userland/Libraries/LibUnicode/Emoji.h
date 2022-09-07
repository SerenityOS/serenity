/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace Unicode {

enum class EmojiGroup : u8 {
    SmileysAndEmotion,
    PeopleAndBody,
    Component,
    AnimalsAndNature,
    FoodAndDrink,
    TravelAndPlaces,
    Activities,
    Objects,
    Symbols,
    Flags,
};

struct Emoji {
    StringView name;
    EmojiGroup group;
    u32 display_order { 0 };
    Span<u32 const> code_points;
};

Optional<Emoji> find_emoji_for_code_points(Span<u32 const> code_points);

template<size_t Size>
Optional<Emoji> find_emoji_for_code_points(u32 const (&code_points)[Size])
{
    return find_emoji_for_code_points(Span<u32 const> { code_points });
}

constexpr StringView emoji_group_to_string(EmojiGroup group)
{
    switch (group) {
    case EmojiGroup::SmileysAndEmotion:
        return "Smileys & Emotion"sv;
    case EmojiGroup::PeopleAndBody:
        return "People & Body"sv;
    case EmojiGroup::Component:
        return "Component"sv;
    case EmojiGroup::AnimalsAndNature:
        return "Animals & Nature"sv;
    case EmojiGroup::FoodAndDrink:
        return "Food & Drink"sv;
    case EmojiGroup::TravelAndPlaces:
        return "Travel & Places"sv;
    case EmojiGroup::Activities:
        return "Activities"sv;
    case EmojiGroup::Objects:
        return "Objects"sv;
    case EmojiGroup::Symbols:
        return "Symbols"sv;
    case EmojiGroup::Flags:
        return "Flags"sv;
    }

    VERIFY_NOT_REACHED();
}

constexpr EmojiGroup emoji_group_from_string(StringView group)
{
    if (group == "Smileys & Emotion"sv)
        return EmojiGroup::SmileysAndEmotion;
    if (group == "People & Body"sv)
        return EmojiGroup::PeopleAndBody;
    if (group == "Component"sv)
        return EmojiGroup::Component;
    if (group == "Animals & Nature"sv)
        return EmojiGroup::AnimalsAndNature;
    if (group == "Food & Drink"sv)
        return EmojiGroup::FoodAndDrink;
    if (group == "Travel & Places"sv)
        return EmojiGroup::TravelAndPlaces;
    if (group == "Activities"sv)
        return EmojiGroup::Activities;
    if (group == "Objects"sv)
        return EmojiGroup::Objects;
    if (group == "Symbols"sv)
        return EmojiGroup::Symbols;
    if (group == "Flags"sv)
        return EmojiGroup::Flags;

    VERIFY_NOT_REACHED();
}

}
