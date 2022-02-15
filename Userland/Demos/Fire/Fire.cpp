/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/* Fire.cpp - a (classic) graphics demo for Serenity, by pd.
 * heavily based on the Fabien Sanglard's article:
 * http://fabiensanglard.net/doom_fire_psx/index.html
 *
 * Future directions:
 *  [X] This does suggest the need for a palletized graphics surface. Thanks kling!
 *  [X] alternate column updates, or vertical interlacing. this would certainly alter
 *      the effect, but the update load would be halved.
 *  [/] scaled blit
 *  [ ] dithering?
 *  [X] inlining rand()
 *  [/] precalculating and recycling random data
 *  [ ] rework/expand palette
 *  [ ] switch to use tsc values for perf check
 *  [ ] handle mouse events differently for smoother painting (queue)
 *  [ ] handle fire bitmap edges better
 */

#include <LibCore/ElapsedTimer.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#define FIRE_WIDTH 320
#define FIRE_HEIGHT 200
#define FIRE_MAX 29

static const Color s_palette[] = {
    Color(0x07, 0x07, 0x07), Color(0x1F, 0x07, 0x07), Color(0x2F, 0x0F, 0x07),
    Color(0x47, 0x0F, 0x07), Color(0x57, 0x17, 0x07), Color(0x67, 0x1F, 0x07),
    Color(0x77, 0x1F, 0x07), Color(0x9F, 0x2F, 0x07), Color(0xAF, 0x3F, 0x07),
    Color(0xBF, 0x47, 0x07), Color(0xC7, 0x47, 0x07), Color(0xDF, 0x4F, 0x07),
    Color(0xDF, 0x57, 0x07), Color(0xD7, 0x5F, 0x07), Color(0xD7, 0x5F, 0x07),
    Color(0xD7, 0x67, 0x0F), Color(0xCF, 0x6F, 0x0F), Color(0xCF, 0x7F, 0x0F),
    Color(0xCF, 0x87, 0x17), Color(0xC7, 0x87, 0x17), Color(0xC7, 0x8F, 0x17),
    Color(0xC7, 0x97, 0x1F), Color(0xBF, 0x9F, 0x1F), Color(0xBF, 0xA7, 0x27),
    Color(0xBF, 0xAF, 0x2F), Color(0xB7, 0xAF, 0x2F), Color(0xB7, 0xB7, 0x37),
    Color(0xCF, 0xCF, 0x6F), Color(0xEF, 0xEF, 0xC7), Color(0xFF, 0xFF, 0xFF)
};

class Fire : public GUI::Frame {
    C_OBJECT(Fire);

public:
    virtual ~Fire() override = default;
    void set_stat_label(RefPtr<GUI::Label> l) { stats = l; };

private:
    Fire();
    RefPtr<Gfx::Bitmap> bitmap;
    RefPtr<GUI::Label> stats;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;

    bool dragging;
    int timeAvg;
    int cycles;
    int phase;
};

Fire::Fire()
{
    bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::Indexed8, { FIRE_WIDTH, FIRE_HEIGHT }).release_value_but_fixme_should_propagate_errors();

    /* Initialize fire palette */
    for (int i = 0; i < 30; i++)
        bitmap->set_palette_color(i, s_palette[i]);

    /* Set remaining entries to white */
    for (int i = 30; i < 256; i++)
        bitmap->set_palette_color(i, Color::White);

    dragging = false;
    timeAvg = 0;
    cycles = 0;
    phase = 0;

    srand(time(nullptr));
    stop_timer();
    start_timer(20);

    /* Draw fire "source" on bottom row of pixels */
    for (int i = 0; i < FIRE_WIDTH; i++)
        bitmap->scanline_u8(bitmap->height() - 1)[i] = FIRE_MAX;

    /* Set off initital paint event */
    // update();
}

void Fire::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    auto timer = Core::ElapsedTimer::start_new();

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(frame_inner_rect(), *bitmap, bitmap->rect());

    timeAvg += timer.elapsed();
    cycles++;
}

void Fire::timer_event(Core::TimerEvent&)
{
    /* Update only even or odd columns per frame... */
    phase++;
    if (phase > 1)
        phase = 0;

    /* Paint our palettized buffer to screen */
    for (int px = 0 + phase; px < FIRE_WIDTH; px += 2) {
        for (int py = 1; py < FIRE_HEIGHT; py++) {
            int rnd = rand() % 3;

            /* Calculate new pixel value, don't go below 0 */
            u8 nv = bitmap->scanline_u8(py)[px];
            if (nv > 0)
                nv -= (rnd & 1);

            /* ...sigh... */
            int epx = px + (1 - rnd);
            if (epx < 0)
                epx = 0;
            else if (epx > FIRE_WIDTH)
                epx = FIRE_WIDTH;

            bitmap->scanline_u8(py - 1)[epx] = nv;
        }
    }

    if ((cycles % 50) == 0) {
        dbgln("{} total cycles. finished 50 in {} ms, avg {} ms", cycles, timeAvg, timeAvg / 50);
        stats->set_text(String::formatted("{} ms", timeAvg / 50));
        timeAvg = 0;
    }

    update();
}

void Fire::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary)
        dragging = true;

    return GUI::Widget::mousedown_event(event);
}

/* FIXME: needs to account for the size of the window rect */
void Fire::mousemove_event(GUI::MouseEvent& event)
{
    if (dragging) {
        if (event.y() >= 2 && event.y() < 398 && event.x() <= 638) {
            int ypos = event.y() / 2;
            int xpos = event.x() / 2;
            bitmap->scanline_u8(ypos - 1)[xpos] = FIRE_MAX + 5;
            bitmap->scanline_u8(ypos - 1)[xpos + 1] = FIRE_MAX + 5;
            bitmap->scanline_u8(ypos)[xpos] = FIRE_MAX + 5;
            bitmap->scanline_u8(ypos)[xpos + 1] = FIRE_MAX + 5;
        }
    }

    return GUI::Widget::mousemove_event(event);
}

void Fire::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary)
        dragging = false;

    return GUI::Widget::mouseup_event(event);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(false);
    window->set_title("Fire");
    window->set_resizable(false);
    window->resize(FIRE_WIDTH * 2 + 4, FIRE_HEIGHT * 2 + 4);

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto fire = TRY(window->try_set_main_widget<Fire>());

    auto time = TRY(fire->try_add<GUI::Label>());
    time->set_relative_rect({ 0, 4, 40, 10 });
    time->move_by({ window->width() - time->width(), 0 });
    fire->set_stat_label(time);

    window->show();

    auto app_icon = GUI::Icon::default_icon("app-fire");
    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
