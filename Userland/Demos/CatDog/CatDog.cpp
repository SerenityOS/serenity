/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

ErrorOr<NonnullRefPtr<CatDog>> CatDog::create()
{
    struct ImageSource {
        State state;
        StringView path;
    };

    // NOTE: The order of the elements is important. Matching is done in best-match order.
    //       So items with the more bits should be placed before items with less bits to
    //       ensure correct matching order. This also means that "Frame2" has to be first.
    static constexpr Array<ImageSource, 24> const image_sources = {
        ImageSource { State::Up | State::Right | State::Frame2, "/res/graphics/catdog/nerun2.png"sv },
        { State::Up | State::Right, "/res/graphics/catdog/nerun1.png"sv },
        { State::Up | State::Left | State::Frame2, "/res/graphics/catdog/nwrun2.png"sv },
        { State::Up | State::Left, "/res/graphics/catdog/nwrun1.png"sv },
        { State::Down | State::Right | State::Frame2, "/res/graphics/catdog/serun2.png"sv },
        { State::Down | State::Right, "/res/graphics/catdog/serun1.png"sv },
        { State::Down | State::Left | State::Frame2, "/res/graphics/catdog/swrun2.png"sv },
        { State::Down | State::Left, "/res/graphics/catdog/swrun1.png"sv },
        { State::Up | State::Frame2, "/res/graphics/catdog/nrun2.png"sv },
        { State::Up, "/res/graphics/catdog/nrun1.png"sv },
        { State::Down | State::Frame2, "/res/graphics/catdog/srun2.png"sv },
        { State::Down, "/res/graphics/catdog/srun1.png"sv },
        { State::Left | State::Frame2, "/res/graphics/catdog/wrun2.png"sv },
        { State::Left, "/res/graphics/catdog/wrun1.png"sv },
        { State::Right | State::Frame2, "/res/graphics/catdog/erun2.png"sv },
        { State::Right, "/res/graphics/catdog/erun1.png"sv },
        { State::Sleeping | State::Frame2, "/res/graphics/catdog/sleep2.png"sv },
        { State::Sleeping, "/res/graphics/catdog/sleep1.png"sv },
        { State::Idle | State::Artist, "/res/graphics/catdog/artist.png"sv },
        { State::Idle | State::Inspector, "/res/graphics/catdog/inspector.png"sv },
        { State::Idle, "/res/graphics/catdog/still.png"sv },
        { State::Alert | State::Artist, "/res/graphics/catdog/artist.png"sv },
        { State::Alert | State::Inspector, "/res/graphics/catdog/inspector.png"sv },
        { State::Alert, "/res/graphics/catdog/alert.png"sv }
    };

    auto catdog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CatDog));
    for (auto const& image_source : image_sources)
        TRY(catdog->m_images.try_append({ image_source.state, *TRY(Gfx::Bitmap::load_from_file(image_source.path)) }));

    return catdog;
}

CatDog::CatDog()
    : m_proc_all(MUST(Core::File::open("/sys/kernel/processes"sv, Core::File::OpenMode::Read)))
{
    m_idle_sleep_timer.start();
}

void CatDog::update()
{
    if (m_state != m_last_state) {
        if (on_state_change)
            on_state_change();
        m_last_state = m_state;
    }
    Widget::update();
}

void CatDog::set_roaming(bool roaming)
{
    m_state = (roaming ? State::Idle : State::Alert) | special_application_states();
    update();
}

void CatDog::set_sleeping(bool sleeping)
{
    m_state = (sleeping ? State::Sleeping : State::Idle) | special_application_states();
    update();
}

CatDog::State CatDog::special_application_states() const
{
    auto maybe_proc_info = Core::ProcessStatisticsReader::get_all(*m_proc_all);
    if (maybe_proc_info.is_error())
        return State::GenericCatDog;

    auto proc_info = maybe_proc_info.release_value();
    auto maybe_paint_program = proc_info.processes.first_matching([](auto& process) {
        return process.name.equals_ignoring_ascii_case("pixelpaint"sv) || process.name.equals_ignoring_ascii_case("fonteditor"sv);
    });
    if (maybe_paint_program.has_value())
        return State::Artist;

    auto maybe_inspector_program = proc_info.processes.first_matching([](auto& process) {
        return process.name.equals_ignoring_ascii_case("systemmonitor"sv) || process.name.equals_ignoring_ascii_case("profiler"sv);
    });
    if (maybe_inspector_program.has_value())
        return State::Inspector;

    return State::GenericCatDog;
}

bool CatDog::is_artist() const
{
    return has_flag(special_application_states(), State::Artist);
}

bool CatDog::is_inspector() const
{
    return has_flag(special_application_states(), State::Inspector);
}

bool CatDog::is_sleeping() const
{
    return has_flag(m_state, State::Sleeping);
}

void CatDog::timer_event(Core::TimerEvent&)
{
    using namespace AK::TimeLiterals;

    if (has_flag(m_state, State::Alert))
        return;

    ScopeGuard update_animation_frame = [&] {
        m_frame = m_frame == State::Frame1 ? State::Frame2 : State::Frame1;
        update();
    };

    if (has_flag(m_state, State::Sleeping)) {
        // Reset idle timer while sleeping to prevent instantly going to sleep again.
        m_idle_sleep_timer.start();
        return;
    }

    m_state = special_application_states();

    auto const size = window()->size();
    Gfx::IntPoint move;

    if (m_mouse_offset.x() < 0) {
        m_state |= State::Left;
        move.set_x(max(m_mouse_offset.x(), -size.width() / 2));
    } else if (m_mouse_offset.x() > size.width()) {
        m_state |= State::Right;
        move.set_x(min(m_mouse_offset.x(), size.width() / 2));
    }

    if (m_mouse_offset.y() < 0) {
        m_state |= State::Up;
        move.set_y(max(m_mouse_offset.y(), -size.height() / 2));
    } else if (m_mouse_offset.y() > size.height()) {
        m_state |= State::Down;
        move.set_y(min(m_mouse_offset.y(), size.height() / 2));
    }

    if (has_any_flag(m_state, State::Directions)) {
        m_idle_sleep_timer.start();
    } else {
        if (m_idle_sleep_timer.elapsed_time() > 5_sec)
            m_state |= State::Sleeping;
        else
            m_state |= State::Idle;
    }

    window()->move_to(window()->position() + move);
    m_mouse_offset -= move;
}

Gfx::Bitmap& CatDog::bitmap_for_state() const
{
    auto state_with_frame = m_state | m_frame;
    auto const iter = m_images.find_if([&](auto const& image) { return (state_with_frame & image.state) == image.state; });
    return iter != m_images.end() ? *iter->bitmap : *m_images[m_images.size() - 1].bitmap;
}

void CatDog::paint_event(GUI::PaintEvent& event)
{
    auto const& bmp = bitmap_for_state();
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), Gfx::Color());
    painter.blit(Gfx::IntPoint(0, 0), bmp, bmp.rect());
}

void CatDog::track_mouse_move(Gfx::IntPoint point)
{
    if (has_flag(m_state, State::Alert))
        return;

    if (has_flag(m_state, State::Sleeping))
        return;

    Gfx::IntPoint relative_offset = point - window()->position();
    if (m_mouse_offset != relative_offset) {
        m_mouse_offset = relative_offset;
        m_idle_sleep_timer.start();
    }
}

void CatDog::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary && on_click)
        on_click();
}

void CatDog::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}
