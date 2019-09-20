#include "SampleWidget.h"
#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <wav-file>\n", argv[0]);
        return 0;
    }

    GApplication app(argc, argv);

    String path = argv[1];
    AWavLoader loader(path);

    if (loader.has_error()) {
        fprintf(stderr, "Failed to load WAV file: %s (%s)\n", path.characters(), loader.error_string());
        return 1;
    }

    AClientConnection audio_client;
    audio_client.handshake();

    auto* window = new GWindow;
    window->set_title("SoundPlayer");
    window->set_rect(300, 300, 300, 200);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 2, 2, 2, 2 });

    auto* sample_widget = new SampleWidget(widget);

    auto* button = new GButton("Quit", widget);
    button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button->set_preferred_size(0, 20);
    button->on_click = [&](auto&) {
        app.quit();
    };

    auto next_sample_buffer = loader.get_more_samples();

    auto timer = CTimer::create(100, [&] {
        if (!next_sample_buffer) {
            sample_widget->set_buffer(nullptr);
            return;
        }
        bool enqueued = audio_client.try_enqueue(*next_sample_buffer);
        if (!enqueued)
            return;
        sample_widget->set_buffer(next_sample_buffer);
        next_sample_buffer = loader.get_more_samples(16 * KB);
        if (!next_sample_buffer) {
            dbg() << "Exhausted samples :^)";
        }
    });

    window->show();
    return app.exec();
}
