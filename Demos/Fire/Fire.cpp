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

#include <LibGUI/GApplication.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define FIRE_WIDTH 320
#define FIRE_HEIGHT 168
#define FIRE_MAX 29

const Color palette[] = {
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
class Fire : public GWidget {
public:
    explicit Fire(GWidget* parent = nullptr);
    virtual ~Fire() override;
    void set_stat_label(GLabel* l) { stats = l; };

private:
    RefPtr<GraphicsBitmap> bitmap;
    GLabel* stats;

    virtual void paint_event(GPaintEvent&) override;
    virtual void timer_event(CTimerEvent&) override;
    virtual void mousedown_event(GMouseEvent& event) override;
    virtual void mousemove_event(GMouseEvent& event) override;
    virtual void mouseup_event(GMouseEvent& event) override;

    bool dragging;
    int timeAvg;
    int cycles;
    int phase;
};

Fire::Fire(GWidget* parent)
    : GWidget(parent)
{
    bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::Indexed8, { 320, 200 });

    /* Initialize fire palette */
    for (int i = 0; i < 30; i++)
        bitmap->set_palette_color(i, palette[i]);

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

void Fire::paint_event(GPaintEvent& event)
{
    CElapsedTimer timer;
    timer.start();

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    /* Blit it! */
    painter.draw_scaled_bitmap(event.rect(), *bitmap, bitmap->rect());

    timeAvg += timer.elapsed();
    cycles++;
}

void Fire::timer_event(CTimerEvent&)
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
void Fire::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left)
        dragging = true;

    return GWidget::mousedown_event(event);
}

/* FIXME: needs to account for the size of the window rect */
void Fire::mousemove_event(GMouseEvent& event)
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

    return GWidget::mousemove_event(event);
}

void Fire::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left)
        dragging = false;

    return GWidget::mouseup_event(event);
}

/*
 * Main
*/
int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_should_exit_event_loop_on_close(true);
    window->set_double_buffering_enabled(false);
    window->set_title("Fire");
    window->set_resizable(false);
    window->set_rect(100, 100, 640, 400);

    auto* fire = new Fire;
    window->set_main_widget(fire);

    auto* time = new GLabel(fire);
    time->set_relative_rect({ 0, 4, 40, 10 });
    time->move_by({ window->width() - time->width(), 0 });
    time->set_foreground_color(Color::from_rgb(0x444444));
    fire->set_stat_label(time);

    window->show();
    window->set_icon_path("/res/icons/16x16/app-demo.png");

    return app.exec();
}
