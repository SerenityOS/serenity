/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibTest/TestCase.h>
#include <LibUnicode/Emoji.h>

// These emojis are the first subgroup in each Unicode-defined group of emojis, plus some interesting
// hand-picked test cases (such as keycap emoji, which begin with ASCII symbols, and country flags).
static constexpr auto s_smileys_emotion = Array { "😀"sv, "😃"sv, "😄"sv, "😁"sv, "😆"sv, "😅"sv, "🤣"sv, "😂"sv, "🙂"sv, "🙃"sv, "🫠"sv, "😉"sv, "😊"sv, "😇"sv };
static constexpr auto s_people_body = Array { "👋"sv, "🤚"sv, "🖐️"sv, "🖐"sv, "✋"sv, "🫱"sv, "🫲"sv, "🫳"sv, "🫴"sv, "🫷"sv, "🫸"sv };
static constexpr auto s_animals_nature = Array { "🐶"sv, "🐕"sv, "🐕‍🦺"sv, "🐩"sv, "🦊"sv, "🦝"sv, "🐱"sv, "🐈"sv, "🐈‍⬛"sv, "🦁"sv, "🐯"sv, "🐴"sv, "🫎"sv, "🫏"sv, "🐎"sv, "🦄"sv, "🦓"sv, "🦌"sv, "🦬"sv, "🐮"sv, "🐷"sv, "🐖"sv, "🐗"sv, "🐽"sv, "🐑"sv, "🦙"sv, "🦒"sv, "🐘"sv, "🐭"sv, "🐁"sv, "🐀"sv, "🐰"sv, "🐇"sv, "🐿️"sv, "🐿"sv, "🦔"sv, "🦇"sv, "🐻"sv, "🐻‍❄️"sv, "🐻‍❄"sv, "🐨"sv, "🐼"sv, "🦥"sv, "🦘"sv, "🦡"sv, "🐾"sv };
static constexpr auto s_food_drink = Array { "🍇"sv, "🍈"sv, "🍉"sv, "🍊"sv, "🍋"sv, "🍌"sv, "🍍"sv, "🥭"sv, "🍎"sv, "🍏"sv, "🍐"sv, "🍑"sv, "🍒"sv, "🍓"sv, "🫐"sv, "🥝"sv, "🍅"sv, "🫒"sv, "🥥"sv };
static constexpr auto s_travel_places = Array { "🌍"sv, "🌎"sv, "🌏"sv, "🌐"sv, "🗺️"sv, "🗺"sv, "🗾"sv, "🧭"sv };
static constexpr auto s_activities = Array { "🎃"sv, "🎄"sv, "🎆"sv, "🎇"sv, "🧨"sv, "✨"sv, "🎈"sv, "🎉"sv, "🎊"sv, "🎋"sv, "🎍"sv, "🎏"sv, "🎑"sv, "🎀"sv, "🎁"sv, "🎗️"sv, "🎗"sv, "🎟️"sv, "🎟"sv, "🎫"sv };
static constexpr auto s_objects = Array { "👓"sv, "🕶️"sv, "🕶"sv, "🦺"sv, "👔"sv, "👖"sv, "🧦"sv, "👗"sv, "🥻"sv, "🩱"sv, "🩲"sv, "🩳"sv, "👙"sv, "🪭"sv, "👛"sv, "👜"sv, "🛍️"sv, "🛍"sv, "🩴"sv, "👡"sv, "👢"sv, "🪮"sv, "👑"sv, "🎩"sv, "🎓"sv, "🪖"sv, "⛑️"sv, "⛑"sv, "💄"sv, "💍"sv, "💎"sv };
static constexpr auto s_symbols = Array { "🚮"sv, "🚰"sv, "♿"sv, "🚹"sv, "🚺"sv, "🚾"sv, "🛂"sv, "🛃"sv, "🛄"sv, "🛅"sv, "#️⃣"sv, "#⃣"sv, "*️⃣"sv, "*⃣"sv, "0️⃣"sv, "0⃣"sv, "1️⃣"sv, "1⃣"sv, "2️⃣"sv, "2⃣"sv, "3️⃣"sv, "3⃣"sv, "4️⃣"sv, "4⃣"sv, "5️⃣"sv, "5⃣"sv, "6️⃣"sv, "6⃣"sv, "7️⃣"sv, "7⃣"sv, "8️⃣"sv, "8⃣"sv, "9️⃣"sv, "9⃣"sv, "🔟"sv };
static constexpr auto s_flags = Array { "🏁"sv, "🚩"sv, "🎌"sv, "🏴"sv, "🏳️"sv, "🏳"sv, "🏳️‍🌈"sv, "🏳‍🌈"sv, "🏳️‍⚧️"sv, "🏳‍⚧️"sv, "🏳️‍⚧"sv, "🏳‍⚧"sv, "🏴‍☠️"sv, "🏴‍☠"sv, "🇦🇨"sv, "🇦🇩"sv, "🇦🇪"sv, "🇦🇫"sv, "🇦🇬"sv, "🇦🇮"sv, "🇦🇱"sv, "🇦🇲"sv, "🇦🇴"sv, "🇦🇶"sv, "🇦🇷"sv, "🇦🇸"sv, "🇦🇹"sv, "🇦🇺"sv, "🇦🇼"sv, "🇦🇽"sv, "🇦🇿"sv, "🇧🇦"sv, "🇧🇧"sv, "🇧🇩"sv, "🇧🇪"sv, "🇧🇫"sv, "🇧🇬"sv, "🇧🇭"sv, "🇧🇮"sv, "🇧🇯"sv, "🇧🇱"sv, "🇧🇲"sv, "🇧🇳"sv, "🇧🇴"sv, "🇧🇶"sv, "🇧🇷"sv, "🇧🇸"sv };

TEST_CASE(emoji)
{
    auto test_emojis = [](auto const& emojis) {
        for (auto emoji : emojis) {
            Utf8View view { emoji };
            EXPECT(Unicode::could_be_start_of_emoji_sequence(view.begin()));
        }
    };

    test_emojis(s_smileys_emotion);
    test_emojis(s_people_body);
    test_emojis(s_animals_nature);
    test_emojis(s_food_drink);
    test_emojis(s_travel_places);
    test_emojis(s_activities);
    test_emojis(s_objects);
    test_emojis(s_symbols);
    test_emojis(s_flags);
}

TEST_CASE(emoji_presentation_only)
{
    auto test_emoji = [](auto emoji, auto expected_result) {
        Utf8View view { emoji };
        auto is_start_of_emoji_sequence = Unicode::could_be_start_of_emoji_sequence(view.begin(), Unicode::SequenceType::EmojiPresentation);
        EXPECT_EQ(is_start_of_emoji_sequence, expected_result);
    };

    test_emoji("©️"sv, true);
    test_emoji("©"sv, false);

    test_emoji("®️"sv, true);
    test_emoji("®"sv, false);

    test_emoji("\U0001F3F3\u200D\U0001F41E"sv, true);       // SerenityOS flag
    test_emoji("\U0001F3F3\uFE0F\u200D\U0001F41E"sv, true); // SerenityOS flag
}

TEST_CASE(ascii_is_not_emoji)
{
    for (u32 code_point = 0u; is_ascii(code_point); ++code_point) {
        auto string = String::from_code_point(code_point);
        Utf8View view { string };

        EXPECT(!Unicode::could_be_start_of_emoji_sequence(view.begin()));
    }
}
