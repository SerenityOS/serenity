#pragma once

#include <LibGUI/GFrame.h>

class GLabel;
class QSLabel;

class QSWidget final : public GFrame {
    C_OBJECT(QSWidget)
public:
    virtual ~QSWidget() override;

    void set_bitmap(NonnullRefPtr<GraphicsBitmap>);
    const GraphicsBitmap* bitmap() const { return m_bitmap.ptr(); }

    void set_path(const String&);
    const String& path() const { return m_path; }

    Function<void(int)> on_scale_change;

private:
    explicit QSWidget(GWidget* parent = nullptr);
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mousewheel_event(GMouseEvent&) override;
    virtual void drop_event(GDropEvent&) override;

    void relayout();

    RefPtr<GraphicsBitmap> m_bitmap;
    Rect m_bitmap_rect;
    int m_scale { 100 };
    Point m_pan_origin;
    Point m_pan_bitmap_origin;
    String m_path;
};
