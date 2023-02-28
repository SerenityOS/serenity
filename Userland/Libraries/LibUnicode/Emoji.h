/*
 * Copyright (c) 2022-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace Unicode {

enum class EmojiGroup : u8 {
    Unknown,

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

    // Non-standard emoji added for SerenityOS:
    SerenityOS,
};

struct Emoji {
    StringView name;
    Optional<StringView> image_path;
    EmojiGroup group { EmojiGroup::Unknown };
    u32 display_order { 0 };
    ReadonlySpan<u32> code_points;
};

Optional<Emoji> find_emoji_for_code_points(ReadonlySpan<u32> code_points);

template<size_t Size>
Optional<Emoji> find_emoji_for_code_points(u32 const (&code_points)[Size])
{
    return find_emoji_for_code_points(ReadonlySpan<u32> { code_points });
}

enum class SequenceType {
    Any,
    EmojiPresentation,
};

bool could_be_start_of_emoji_sequence(Utf8CodePointIterator const&, SequenceType = SequenceType::Any);
bool could_be_start_of_emoji_sequence(Utf32CodePointIterator const&, SequenceType = SequenceType::Any);

constexpr StringView emoji_group_to_string(EmojiGroup group)
{
    switch (group) {
    case EmojiGroup::Unknown:
        return "Unknown"sv;
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
    case EmojiGroup::SerenityOS:
        return "SerenityOS"sv;
    }

    VERIFY_NOT_REACHED();
}

constexpr EmojiGroup emoji_group_from_string(StringView group)
{
    if (group == "Unknown"sv)
        return EmojiGroup::Unknown;
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
    if (group == "SerenityOS"sv)
        return EmojiGroup::SerenityOS;

    VERIFY_NOT_REACHED();
}

}
