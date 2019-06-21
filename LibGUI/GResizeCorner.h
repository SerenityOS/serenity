#include <LibGUI/GWidget.h>

class GResizeCorner : public GWidget {
public:
    explicit GResizeCorner(GWidget* parent);
    virtual ~GResizeCorner() override;

    virtual const char* class_name() const override { return "GResizeCorner"; }

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    RefPtr<GraphicsBitmap> m_bitmap;
};
