/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibGfx/Font/Emoji.h>
#include <LibTest/TestCase.h>

// These emojis are the first subgroup in each Unicode-defined group of emojis, plus some interesting
// hand-picked test cases (such as keycap emoji, which begin with ASCII symbols, and country flags).
constexpr auto s_emojis = Array {
    // Smileys & Emotion
    "😀"sv,
    "😃"sv,
    "😄"sv,
    "😁"sv,
    "😆"sv,
    "😅"sv,
    "🤣"sv,
    "😂"sv,
    "🙂"sv,
    "🙃"sv,
    "🫠"sv,
    "😉"sv,
    "😊"sv,
    "😇"sv,

    // Smileys & Emotion
    "👋"sv,
    "🤚"sv,
    "🖐️"sv,
    "🖐"sv,
    "✋"sv,
    "🫱"sv,
    "🫲"sv,
    "🫳"sv,
    "🫴"sv,
    "🫷"sv,
    "🫸"sv,

    // Animals & Nature
    "🐶"sv,
    "🐕"sv,
    "🐕‍🦺"sv,
    "🐩"sv,
    "🦊"sv,
    "🦝"sv,
    "🐱"sv,
    "🐈"sv,
    "🐈‍⬛"sv,
    "🦁"sv,
    "🐯"sv,
    "🐴"sv,
    "🫎"sv,
    "🫏"sv,
    "🐎"sv,
    "🦄"sv,
    "🦓"sv,
    "🦌"sv,
    "🦬"sv,
    "🐮"sv,
    "🐷"sv,
    "🐖"sv,
    "🐗"sv,
    "🐽"sv,
    "🐑"sv,
    "🦙"sv,
    "🦒"sv,
    "🐘"sv,
    "🐭"sv,
    "🐁"sv,
    "🐀"sv,
    "🐰"sv,
    "🐇"sv,
    "🐿️"sv,
    "🐿"sv,
    "🦔"sv,
    "🦇"sv,
    "🐻"sv,
    "🐻‍❄️"sv,
    "🐻‍❄"sv,
    "🐨"sv,
    "🐼"sv,
    "🦥"sv,
    "🦘"sv,
    "🦡"sv,
    "🐾"sv,

    // Food & Drink
    "🍇"sv,
    "🍈"sv,
    "🍉"sv,
    "🍊"sv,
    "🍋"sv,
    "🍌"sv,
    "🍍"sv,
    "🥭"sv,
    "🍎"sv,
    "🍏"sv,
    "🍐"sv,
    "🍑"sv,
    "🍒"sv,
    "🍓"sv,
    "🫐"sv,
    "🥝"sv,
    "🍅"sv,
    "🫒"sv,
    "🥥"sv,

    // Travel & Places
    "🌍"sv,
    "🌎"sv,
    "🌏"sv,
    "🌐"sv,
    "🗺️"sv,
    "🗺"sv,
    "🗾"sv,
    "🧭"sv,

    // Activities
    "🎃"sv,
    "🎄"sv,
    "🎆"sv,
    "🎇"sv,
    "🧨"sv,
    "✨"sv,
    "🎈"sv,
    "🎉"sv,
    "🎊"sv,
    "🎋"sv,
    "🎍"sv,
    "🎏"sv,
    "🎑"sv,
    "🎀"sv,
    "🎁"sv,
    "🎗️"sv,
    "🎗"sv,
    "🎟️"sv,
    "🎟"sv,
    "🎫"sv,

    // Objects
    "👓"sv,
    "🕶️"sv,
    "🕶"sv,
    "🦺"sv,
    "👔"sv,
    "👖"sv,
    "🧦"sv,
    "👗"sv,
    "🥻"sv,
    "🩱"sv,
    "🩲"sv,
    "🩳"sv,
    "👙"sv,
    "🪭"sv,
    "👛"sv,
    "👜"sv,
    "🛍️"sv,
    "🛍"sv,
    "🩴"sv,
    "👡"sv,
    "👢"sv,
    "🪮"sv,
    "👑"sv,
    "🎩"sv,
    "🎓"sv,
    "🪖"sv,
    "⛑️"sv,
    "⛑"sv,
    "💄"sv,
    "💍"sv,
    "💎"sv,

    // Symbols
    "🚮"sv,
    "🚰"sv,
    "♿"sv,
    "🚹"sv,
    "🚺"sv,
    "🚾"sv,
    "🛂"sv,
    "🛃"sv,
    "🛄"sv,
    "🛅"sv,
    "#️⃣"sv,
    "#⃣"sv,
    "*️⃣"sv,
    "*⃣"sv,
    "0️⃣"sv,
    "0⃣"sv,
    "1️⃣"sv,
    "1⃣"sv,
    "2️⃣"sv,
    "2⃣"sv,
    "3️⃣"sv,
    "3⃣"sv,
    "4️⃣"sv,
    "4⃣"sv,
    "5️⃣"sv,
    "5⃣"sv,
    "6️⃣"sv,
    "6⃣"sv,
    "7️⃣"sv,
    "7⃣"sv,
    "8️⃣"sv,
    "8⃣"sv,
    "9️⃣"sv,
    "9⃣"sv,
    "🔟"sv,

    // Flags
    "🏁"sv,
    "🚩"sv,
    "🎌"sv,
    "🏴"sv,
    "🏳️"sv,
    "🏳"sv,
    "🏳️‍🌈"sv,
    "🏳‍🌈"sv,
    "🏳️‍⚧️"sv,
    "🏳‍⚧️"sv,
    "🏳️‍⚧"sv,
    "🏳‍⚧"sv,
    "🏴‍☠️"sv,
    "🏴‍☠"sv,
    "🇦🇨"sv,
    "🇦🇩"sv,
    "🇦🇪"sv,
    "🇦🇫"sv,
    "🇦🇬"sv,
    "🇦🇮"sv,
    "🇦🇱"sv,
    "🇦🇲"sv,
    "🇦🇴"sv,
    "🇦🇶"sv,
    "🇦🇷"sv,
    "🇦🇸"sv,
    "🇦🇹"sv,
    "🇦🇺"sv,
    "🇦🇼"sv,
    "🇦🇽"sv,
    "🇦🇿"sv,
    "🇧🇦"sv,
    "🇧🇧"sv,
    "🇧🇩"sv,
    "🇧🇪"sv,
    "🇧🇫"sv,
    "🇧🇬"sv,
    "🇧🇭"sv,
    "🇧🇮"sv,
    "🇧🇯"sv,
    "🇧🇱"sv,
    "🇧🇲"sv,
    "🇧🇳"sv,
    "🇧🇴"sv,
    "🇧🇶"sv,
    "🇧🇷"sv,
    "🇧🇸"sv,
};

TEST_CASE(load_emoji)
{
    for (auto emoji : s_emojis) {
        Utf8View view { emoji };
        auto it = view.begin();

        auto const* bitmap = Gfx::Emoji::emoji_for_code_point_iterator(it);
        EXPECT_NE(bitmap, nullptr);

        EXPECT(!it.done());
        ++it;
        EXPECT(it.done());
    }
}

TEST_CASE(ascii_is_not_emoji)
{
    for (u32 code_point = 0; is_ascii(code_point); code_point++) {
        auto string = String::from_code_point(code_point);

        Utf8View view { string };
        auto it = view.begin();

        auto const* bitmap = Gfx::Emoji::emoji_for_code_point_iterator(it);
        EXPECT_EQ(bitmap, nullptr);
    }
}
