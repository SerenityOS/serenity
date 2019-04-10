#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Weakable.h>

class GPainter;
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

class VBWidget : public Retainable<VBWidget>, public Weakable<VBWidget> {
public:
    static Retained<VBWidget> create(VBForm& form) { return adopt(*new VBWidget(form)); }
    virtual ~VBWidget();

    bool is_selected() const;

    Rect rect() const { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    Rect grabber_rect(Direction) const;

    void paint(GPainter&);

private:
    VBWidget(VBForm&);

    VBForm& m_form;
    Rect m_rect;
};
