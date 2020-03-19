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

private:
    explicit CanvasRenderingContext2D(HTMLCanvasElement&);

    OwnPtr<Gfx::Painter> painter();

    WeakPtr<HTMLCanvasElement> m_element;

    Gfx::Color m_fill_style;
};

}
