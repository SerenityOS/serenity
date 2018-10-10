#pragma once

class Widget;

class AbstractScreen {
public:
    virtual ~AbstractScreen();

    unsigned width() const { return m_width; }
    unsigned height() const { return m_height; }

    void setRootWidget(Widget*);

    static AbstractScreen& the();

protected:
    AbstractScreen(unsigned width, unsigned height);

private:
    unsigned m_width { 0 };
    unsigned m_height { 0 };

    Widget* m_rootWidget { nullptr };
};

