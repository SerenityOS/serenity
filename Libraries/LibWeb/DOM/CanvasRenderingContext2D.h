#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web {

class CanvasRenderingContext2D
    : public RefCounted<CanvasRenderingContext2D>
    , public Bindings::Wrappable {

    AK_MAKE_NONCOPYABLE(CanvasRenderingContext2D);
    AK_MAKE_NONMOVABLE(CanvasRenderingContext2D);

public:
    using WrapperType = Bindings::CanvasRenderingContext2DWrapper;

    static NonnullRefPtr<CanvasRenderingContext2D> create(HTMLCanvasElement& element) { return adopt(*new CanvasRenderingContext2D(element)); }
    ~CanvasRenderingContext2D();

    void set_fill_style(String);
    String fill_style() const;

    void fill_rect(int x, int y, int width, int height);
    void scale(double sx, double sy);
    void translate(double x, double y);

private:
    explicit CanvasRenderingContext2D(HTMLCanvasElement&);

    void did_draw(const Gfx::Rect&);

    OwnPtr<Gfx::Painter> painter();

    WeakPtr<HTMLCanvasElement> m_element;

    double m_scale_x { 1 };
    double m_scale_y { 1 };
    double m_translate_x { 0 };
    double m_translate_y { 0 };
    Gfx::Color m_fill_style;
};

}
