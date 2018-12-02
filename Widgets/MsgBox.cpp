#include "MsgBox.h"
#include "Font.h"
#include "AbstractScreen.h"
#include "Window.h"
#include "Label.h"
#include "Button.h"

void MsgBox(Window* owner, String&& text)
{
    Font& font = Font::defaultFont();
    auto screenRect = AbstractScreen::the().rect();

    int textWidth = text.length() * font.glyphWidth() + 8;
    int textHeight = font.glyphHeight() + 8;
    int horizontalPadding = 16;
    int verticalPadding = 16;
    int buttonWidth = 60;
    int buttonHeight = 20;
    int windowWidth = textWidth + horizontalPadding * 2;
    int windowHeight = textHeight + buttonHeight + verticalPadding * 3;

    Rect windowRect(
        screenRect.center().x() - windowWidth / 2,
        screenRect.center().y() - windowHeight / 2,
        windowWidth,
        windowHeight
    );

    Rect buttonRect(
        windowWidth / 2 - buttonWidth / 2,
        windowHeight - verticalPadding - buttonHeight,
        buttonWidth,
        buttonHeight
    );

    auto* window = new Window;
    window->setTitle("MsgBox");
    window->setRect(windowRect);
    auto* widget = new Widget;
    widget->setWindowRelativeRect({ 0, 0, windowWidth, windowHeight });
    widget->setFillWithBackgroundColor(true);
    window->setMainWidget(widget);
    auto* label = new Label(widget);
    label->setWindowRelativeRect({
        horizontalPadding,
        verticalPadding,
        textWidth,
        textHeight
    });
    label->setText(std::move(text));
    auto* button = new Button(widget);
    button->setCaption("OK");
    button->setWindowRelativeRect(buttonRect);
    button->onClick = [] (Button& button) {
        printf("MsgBox button pressed, closing MsgBox :)\n");
        button.window()->close();
    };
}

