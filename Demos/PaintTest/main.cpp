#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibDraw/PNGLoader.h>

class TestWidget final : public GWidget {
public:
    TestWidget(GWidget* parent)
        : GWidget(parent)
    {
    }
    virtual ~TestWidget() override {}

    void set_bitmap(RefPtr<GraphicsBitmap>&& bitmap)
    {
        m_bitmap = move(bitmap);
        update();
    }

private:
    virtual void paint_event(GPaintEvent&) override
    {
        GPainter painter(*this);

        painter.fill_rect(rect(), Color::WarmGray);

        painter.blit_tiled({ 0, 0, 160, 160 }, *m_bitmap, m_bitmap->rect());

        painter.add_clip_rect({ 50, 50, 115, 95 });
        painter.blit_tiled({ 160, 160, 160, 160 }, *m_bitmap, m_bitmap->rect());
    }

    RefPtr<GraphicsBitmap> m_bitmap;
};

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_rect(100, 100, 400, 400);
    window->set_title("Paint test");

    auto* test_widget = new TestWidget(nullptr);
    window->set_main_widget(test_widget);

    test_widget->set_bitmap(load_png("/res/icons/gear16.png"));

    window->show();

    return app.exec();
}
