/*
 * Copyright (c) 2022, Joe Petrus <joe@petrus.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WordGame.h"
#include <AK/QuickSort.h>
#include <AK/Random.h>
#include <AK/StringView.h>
#include <LibConfig/Client.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

// TODO: Add stats
namespace MasterWord {

WordGame::WordGame()
    : m_clear_message_timer(Core::Timer::create_single_shot(5000, [this] { clear_message(); }))
{
    read_words();
    m_num_letters = Config::read_i32("MasterWord"sv, ""sv, "word_length"sv, 5);
    m_max_guesses = Config::read_i32("MasterWord"sv, ""sv, "max_guesses"sv, 6);
    m_check_guesses = Config::read_bool("MasterWord"sv, ""sv, "check_guesses_in_dictionary"sv, false);
    reset();
    pick_font();
}

void WordGame::reset()
{
    m_current_guess = {};
    m_guesses.clear();
    auto maybe_word = WordGame::random_word(m_num_letters);
    if (maybe_word.has_value())
        m_current_word = maybe_word.value();
    else {
        GUI::MessageBox::show(window(), ByteString::formatted("Could not get a random {} letter word. Defaulting to 5.", m_num_letters), "MasterWord"sv);
        if (m_num_letters != 5) {
            m_num_letters = 5;
            reset();
        }
    }
    set_fixed_size(game_size());
    clear_message();
    update();
}

void WordGame::pick_font()
{
    ByteString best_font_name;
    auto best_font_size = -1;
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](Gfx::Font const& font) {
        if (font.family() != "Liza" || font.weight() != 700)
            return;
        auto size = font.pixel_size_rounded_up();
        if (size * 2 <= m_letter_height && size > best_font_size) {
            best_font_name = font.qualified_name().to_byte_string();
            best_font_size = size;
        }
    });

    auto font = font_database.get_by_name(best_font_name);
    set_font(font);
}

void WordGame::resize_event(GUI::ResizeEvent&)
{
    pick_font();
    update();
}

void WordGame::keydown_event(GUI::KeyEvent& event)
{
    // If we can still add a letter and the key was alpha
    if (m_current_guess.length() < m_num_letters && is_ascii_alpha(event.code_point())) {
        m_current_guess = ByteString::formatted("{}{}", m_current_guess, event.text().to_uppercase());
        m_last_word_invalid = false;
    }
    // If backspace pressed and already have some letters entered
    else if (event.key() == KeyCode::Key_Backspace && m_current_guess.length() > 0) {
        m_current_guess = m_current_guess.substring(0, m_current_guess.length() - 1);
        m_last_word_invalid = false;
    }
    // If return pressed
    else if (event.key() == KeyCode::Key_Return) {
        if (m_current_guess.length() < m_num_letters) {
            show_message("Not enough letters"sv);
            m_last_word_invalid = true;
        } else if (!is_in_dictionary(m_current_guess)) {
            show_message("Not in dictionary"sv);
            m_last_word_invalid = true;
        } else {
            m_last_word_invalid = false;
            clear_message();

            add_guess(m_current_guess);
            auto won = m_current_guess == m_current_word;
            m_current_guess = {};
            if (won) {
                GUI::MessageBox::show(window(), "You win!"sv, "MasterWord"sv);
                reset();
            } else if (m_guesses.size() == m_max_guesses) {
                GUI::MessageBox::show(window(), ByteString::formatted("You lose!\nThe word was {}", m_current_word), "MasterWord"sv);
                reset();
            }
        }
    } else {
        event.ignore();
    }

    update();
}

void WordGame::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), m_background_color);

    for (size_t guess_index = 0; guess_index < m_max_guesses; ++guess_index) {
        for (size_t letter_index = 0; letter_index < m_num_letters; ++letter_index) {
            auto this_rect = letter_rect(guess_index, letter_index);

            if (guess_index < m_guesses.size()) {

                switch (m_guesses[guess_index].letter_states.at(letter_index)) {
                case Correct:
                    painter.fill_rect(this_rect, m_right_letter_right_spot_color);
                    break;
                case WrongSpot:
                    painter.fill_rect(this_rect, m_right_letter_wrong_spot_color);
                    break;
                case Incorrect:
                    painter.fill_rect(this_rect, m_wrong_letter_color);
                    break;
                }

                painter.draw_text(this_rect, m_guesses[guess_index].text.substring_view(letter_index, 1), font(), Gfx::TextAlignment::Center, m_text_color);
            } else if (guess_index == m_guesses.size()) {
                if (letter_index < m_current_guess.length())
                    painter.draw_text(this_rect, m_current_guess.substring_view(letter_index, 1), font(), Gfx::TextAlignment::Center, m_text_color);
                if (m_last_word_invalid) {
                    painter.fill_rect(this_rect, m_word_not_in_dict_color);
                }
            }

            painter.draw_rect(this_rect, m_border_color);
        }
    }
}

Gfx::IntRect WordGame::letter_rect(size_t guess_number, size_t letter_number) const
{
    auto letter_left = m_outer_margin + letter_number * m_letter_width + m_letter_spacing * letter_number;
    auto letter_top = m_outer_margin + guess_number * m_letter_height + m_letter_spacing * guess_number;
    return Gfx::IntRect(int(letter_left), int(letter_top), m_letter_width, m_letter_height);
}

bool WordGame::is_in_dictionary(AK::StringView guess)
{
    return !m_check_guesses || !m_words.ensure(guess.length()).find(guess).is_end();
}

void WordGame::read_words()
{
    m_words.clear();

    auto try_load_words = [&]() -> ErrorOr<void> {
        auto response = TRY(Core::File::open("/res/words.txt"sv, Core::File::OpenMode::Read));
        auto words_file = TRY(Core::InputBufferedFile::create(move(response)));
        Array<u8, 128> buffer;

        while (!words_file->is_eof()) {
            auto current_word = TRY(words_file->read_line(buffer));
            if (!current_word.starts_with('#') and current_word.length() > 0)
                m_words.ensure(current_word.length()).append(current_word.to_uppercase_string());
        }

        return {};
    };

    if (try_load_words().is_error()) {
        GUI::MessageBox::show(nullptr, "Could not read /res/words.txt.\nPlease ensure this file exists and restart MasterWord."sv, "MasterWord"sv);
        exit(0);
    }
}

Optional<ByteString> WordGame::random_word(size_t length)
{
    auto words_for_length = m_words.get(length);
    if (words_for_length.has_value()) {
        auto i = get_random_uniform(words_for_length->size());
        return words_for_length->at(i);
    }

    return {};
}

size_t WordGame::shortest_word()
{
    auto available_lengths = m_words.keys();
    AK::quick_sort(available_lengths);
    return available_lengths.first();
}

size_t WordGame::longest_word()
{
    auto available_lengths = m_words.keys();
    AK::quick_sort(available_lengths);
    return available_lengths.last();
}

void WordGame::set_use_system_theme(bool b)
{
    if (b) {
        auto theme = palette();
        m_right_letter_wrong_spot_color = Color::from_rgb(0xb59f3b);
        m_right_letter_right_spot_color = Color::from_rgb(0x538d4e);
        m_border_color = Color::Black;
        m_wrong_letter_color = theme.window();
        m_background_color = theme.window();
        m_text_color = theme.accent();
    } else {
        m_right_letter_wrong_spot_color = Color::from_rgb(0xb59f3b);
        m_right_letter_right_spot_color = Color::from_rgb(0x538d4e);
        m_border_color = Color::from_rgb(0x3a3a3c);
        m_wrong_letter_color = m_border_color;
        m_background_color = Color::from_rgb(0x121213);
        m_text_color = Color::White;
    }

    update();
}

void WordGame::set_word_length(size_t length)
{
    m_num_letters = length;
    reset();
}

void WordGame::set_max_guesses(size_t max_guesses)
{
    m_max_guesses = max_guesses;
    reset();
}

void WordGame::set_check_guesses_in_dictionary(bool b)
{
    m_check_guesses = b;
    update();
}

bool WordGame::is_checking_guesses() const
{
    return m_check_guesses;
}

Gfx::IntSize WordGame::game_size() const
{
    auto w = 2 * m_outer_margin + m_num_letters * m_letter_width + (m_num_letters - 1) * m_letter_spacing;
    auto h = 2 * m_outer_margin + m_max_guesses * m_letter_height + (m_max_guesses - 1) * m_letter_spacing;
    return Gfx::IntSize(w, h);
}

void WordGame::add_guess(AK::StringView guess)
{
    AK::Vector<LetterState> letter_states;

    auto number_correct_for_letter = [this, &guess](StringView letter) -> size_t {
        VERIFY(m_current_word.length() == guess.length());
        auto correct_count = 0;
        for (size_t i = 0; i < m_current_word.length(); ++i) {
            if (m_current_word.substring_view(i, 1) == letter && guess.substring_view(i, 1) == letter)
                ++correct_count;
        }
        return correct_count;
    };

    for (size_t letter_index = 0; letter_index < m_num_letters; ++letter_index) {
        auto guess_letter = guess.substring_view(letter_index, 1);

        if (m_current_word[letter_index] == guess_letter[0])
            letter_states.append(Correct);
        else if (m_current_word.contains(guess_letter)) {
            auto occurrences_in_word = m_current_word.count(guess_letter);
            auto occurrences_in_guess_already_counted = guess.substring_view(0, letter_index).count(guess_letter);
            auto correct = number_correct_for_letter(guess_letter);
            if (occurrences_in_word > correct && occurrences_in_guess_already_counted < occurrences_in_word)
                letter_states.append(WrongSpot);
            else
                letter_states.append(Incorrect);
        } else
            letter_states.append(Incorrect);
    }

    m_guesses.append({ guess, letter_states });
    update();
}

void WordGame::show_message(StringView message) const
{
    m_clear_message_timer->restart();
    if (on_message)
        on_message(message);
}

void WordGame::clear_message() const
{
    m_clear_message_timer->stop();
    if (on_message)
        on_message({});
}

}
