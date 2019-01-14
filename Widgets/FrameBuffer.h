#pragma once

#include "AbstractScreen.h"
#include "Color.h"

class GraphicsBitmap;

class FrameBuffer final : public AbstractScreen {
public:
    FrameBuffer(unsigned width, unsigned height);
    FrameBuffer(RGBA32*, unsigned width, unsigned height);
    virtual ~FrameBuffer() override;

    void show();

    static FrameBuffer& the();

    RGBA32* scanline(int y);

    static void initialize();

private:
    RGBA32* m_data { nullptr };
};

