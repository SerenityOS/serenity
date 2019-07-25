#include <LibGUI/GWidget.h>

class GResizeCorner : public GWidget {
    C_OBJECT(GResizeCorner)
public:
    explicit GResizeCorner(GWidget* parent);
    virtual ~GResizeCorner() override;

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    RefPtr<GraphicsBitmap> m_bitmap;
};
