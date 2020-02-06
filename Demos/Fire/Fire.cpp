/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define FIRE_WIDTH 320
#define FIRE_HEIGHT 168
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

/* Random functions...
 * These are from musl libc's prng/rand.c
*/
static uint64_t seed;

void my_srand(unsigned s)
{
    seed = s - 1;
}

static int my_rand(void)
{
    seed = 6364136223846793005ULL * seed + 1;
    return seed >> 33;
}

/*
 * Fire Widget
*/
class Fire : public GUI::Widget {
    C_OBJECT(Fire)
public:
    virtual ~Fire() override;
    void set_stat_label(RefPtr<GUI::Label> l) { stats = l; };

private:
    explicit Fire(GUI::Widget* parent = nullptr);
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

Fire::Fire(GUI::Widget* parent)
    : GUI::Widget(parent)
{
    bitmap = Gfx::Bitmap::create(Gfx::Bitmap::Format::Indexed8, { 320, 200 });

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

    my_srand(time(nullptr));
    stop_timer();
    start_timer(20);

    /* Draw fire "source" on bottom row of pixels */
    for (int i = 0; i < FIRE_WIDTH; i++)
        bitmap->bits(bitmap->height() - 1)[i] = FIRE_MAX;

    /* Set off initital paint event */
    //update();
}

Fire::~Fire()
{
}

void Fire::paint_event(GUI::PaintEvent& event)
{
    Core::ElapsedTimer timer;
    timer.start();

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    /* Blit it! */
    painter.draw_scaled_bitmap(event.rect(), *bitmap, bitmap->rect());

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
        for (int py = 1; py < 200; py++) {
            int rnd = my_rand() % 3;

            /* Calculate new pixel value, don't go below 0 */
            u8 nv = bitmap->bits(py)[px];
            if (nv > 0)
                nv -= (rnd & 1);

            /* ...sigh... */
            int epx = px + (1 - rnd);
            if (epx < 0)
                epx = 0;
            else if (epx > FIRE_WIDTH)
                epx = FIRE_WIDTH;

            bitmap->bits(py - 1)[epx] = nv;
        }
    }

    if ((cycles % 50) == 0) {
        dbgprintf("%d total cycles. finished 50 in %d ms, avg %d ms\n", cycles, timeAvg, timeAvg / 50);
        stats->set_text(String::format("%d ms", timeAvg / 50));
        timeAvg = 0;
    }

    update();
}

/*
 * Mouse handling events
*/
void Fire::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left)
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
            bitmap->bits(ypos - 1)[xpos] = FIRE_MAX + 5;
            bitmap->bits(ypos - 1)[xpos + 1] = FIRE_MAX + 5;
            bitmap->bits(ypos)[xpos] = FIRE_MAX + 5;
            bitmap->bits(ypos)[xpos + 1] = FIRE_MAX + 5;
        }
    }

    return GUI::Widget::mousemove_event(event);
}

void Fire::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left)
        dragging = false;

    return GUI::Widget::mouseup_event(event);
}

/*
 * Main
*/
int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(false);
    window->set_title("Fire");
    window->set_resizable(false);
    window->set_rect(100, 100, 640, 400);

    auto fire = Fire::construct();
    window->set_main_widget(fire);

    auto time = GUI::Label::construct(fire);
    time->set_relative_rect({ 0, 4, 40, 10 });
    time->move_by({ window->width() - time->width(), 0 });
    time->set_foreground_color(Color::from_rgb(0x444444));
    fire->set_stat_label(time);

    window->show();
    window->set_icon(Gfx::load_png("/res/icons/16x16/app-demo.png"));

    return app.exec();
}
