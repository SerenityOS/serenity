#include <LibGUI/GWidget.h>

class GResizeCorner : public GWidget {
    C_OBJECT(GResizeCorner)
public:
    virtual ~GResizeCorner() override;

protected:
    explicit GResizeCorner(GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    RefPtr<GraphicsBitmap> m_bitmap;
};
