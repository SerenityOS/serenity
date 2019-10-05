#pragma once

#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/Layout/LayoutReplaced.h>

class HTMLImageElement;

class LayoutImage : public LayoutReplaced {
public:
    LayoutImage(const HTMLImageElement&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutImage() override;

    virtual void layout() override;
    virtual void render(RenderingContext&) override;

    const HTMLImageElement& node() const { return static_cast<const HTMLImageElement&>(LayoutReplaced::node()); }
private:
};
