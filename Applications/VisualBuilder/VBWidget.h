#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Weakable.h>

class GPainter;
class GWidget;
class VBForm;

enum class Direction { None, Left, UpLeft, Up, UpRight, Right, DownRight, Down, DownLeft };
template<typename Callback>
inline void for_each_direction(Callback callback)
{
    callback(Direction::Left);
    callback(Direction::UpLeft);
    callback(Direction::Up);
    callback(Direction::UpRight);
    callback(Direction::Right);
    callback(Direction::DownRight);
    callback(Direction::Down);
    callback(Direction::DownLeft);
}

enum class WidgetType {
    None,
    GWidget,
    GButton,
    GLabel,
    GSpinBox,
    GTextEditor,
    GProgressBar,
    GCheckBox,
};

class VBWidget : public Retainable<VBWidget>, public Weakable<VBWidget> {
public:
    static Retained<VBWidget> create(WidgetType type, VBForm& form) { return adopt(*new VBWidget(type, form)); }
    ~VBWidget();

    bool is_selected() const;

    Rect rect() const;
    void set_rect(const Rect&);

    Rect grabber_rect(Direction) const;
    Direction grabber_at(const Point&) const;

    GWidget* gwidget() { return m_gwidget; }

protected:
    VBWidget(WidgetType, VBForm&);

private:
    WidgetType m_type { WidgetType::None };
    VBForm& m_form;
    GWidget* m_gwidget { nullptr };
};
