/*
 * Copyright (c) 2022, Joe Petrus <joe@petrus.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Rect.h>

namespace MasterWord {

class WordGame : public GUI::Frame {
    C_OBJECT(WordGame);

public:
    virtual ~WordGame() override = default;

    void reset();
    void set_use_system_theme(bool b);
    void set_check_guesses_in_dictionary(bool b);
    void set_word_length(size_t length);
    void set_max_guesses(size_t max_guesses);
    Gfx::IntSize game_size() const;

    Optional<ByteString> random_word(size_t length);
    size_t shortest_word();
    size_t longest_word();
    bool is_checking_guesses() const;

    void add_guess(AK::StringView guess);
    bool is_in_dictionary(AK::StringView guess);

    Function<void(Optional<StringView>)> on_message;

private:
    WordGame();
    void read_words();
    void show_message(StringView message) const;
    void clear_message() const;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    void pick_font();

    Gfx::IntRect letter_rect(size_t guess_number, size_t letter_number) const;

    size_t m_max_guesses { 6 };
    size_t m_num_letters { 5 };
    bool m_check_guesses { false };
    bool m_last_word_invalid { false };
    static constexpr int m_letter_width { 40 };
    static constexpr int m_letter_spacing { 5 };
    static constexpr int m_outer_margin { 20 };
    static constexpr int m_letter_height { 60 };

    Color m_right_letter_wrong_spot_color { Color::from_rgb(0xb59f3b) };
    Color m_right_letter_right_spot_color { Color::from_rgb(0x538d4e) };
    Color m_border_color { Color::from_rgb(0x3a3a3c) };
    Color m_wrong_letter_color { m_border_color };
    Color m_background_color { Color::from_rgb(0x121213) };
    Color m_text_color { Color::White };
    Color m_word_not_in_dict_color { Color::from_argb(0x40aa0000) };

    enum LetterState {
        Correct,
        WrongSpot,
        Incorrect
    };

    struct Guess {
        AK::ByteString text;
        AK::Vector<LetterState> letter_states;
    };

    AK::Vector<Guess> m_guesses;
    AK::ByteString m_current_guess;
    AK::ByteString m_current_word;

    HashMap<size_t, AK::Vector<ByteString>> m_words;

    NonnullRefPtr<Core::Timer> m_clear_message_timer;
};

}
