#include "FrameBuffer.h"
#include "EventLoop.h"
#include "Label.h"
#include "Button.h"
#include "WindowManager.h"
#include "Window.h"
#include "ClockWidget.h"
#include "CheckBox.h"
#include "ListBox.h"
#include "TextBox.h"
#include "MsgBox.h"
#include <cstdio>

int main(int argc, char** argv)
{
    FrameBuffer fb(800, 600);
    fb.show();

    EventLoop loop;

    auto* fontTestWindow = new Window;
    fontTestWindow->setTitle("Font test");
    fontTestWindow->setRect({ 140, 100, 300, 80 });

    auto* fontTestWindowWidget = new Widget;
    fontTestWindow->setMainWidget(fontTestWindowWidget);
    fontTestWindowWidget->setWindowRelativeRect({ 0, 0, 300, 80 });

    auto* l1 = new Label(fontTestWindowWidget);
    l1->setWindowRelativeRect({ 0, 0, 300, 20 });
    l1->setText("0123456789");

    auto* l2 = new Label(fontTestWindowWidget);
    l2->setWindowRelativeRect({ 0, 20, 300, 20 });
    l2->setText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    
    auto* l3 = new Label(fontTestWindowWidget);
    l3->setWindowRelativeRect({ 0, 40, 300, 20 });
    l3->setText("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new Label(fontTestWindowWidget);
    l4->setWindowRelativeRect({ 0, 60, 300, 20 });
    l4->setText("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    {
        auto* widgetTestWindow = new Window;
        widgetTestWindow->setTitle("Widget test");
        widgetTestWindow->setRect({ 20, 40, 100, 180 });

        auto* widgetTestWindowWidget = new Widget;
        widgetTestWindowWidget->setWindowRelativeRect({ 0, 0, 100, 100 });
        widgetTestWindow->setMainWidget(widgetTestWindowWidget);

        auto* l = new Label(widgetTestWindowWidget);
        l->setWindowRelativeRect({ 0, 0, 100, 20 });
        l->setText("Label");

        auto* b = new Button(widgetTestWindowWidget);
        b->setWindowRelativeRect({ 0, 20, 100, 20 });
        b->setCaption("Button");

        b->onClick = [] (Button& button) {
            printf("Button %p clicked!\n", &button);
        };

        auto* c = new CheckBox(widgetTestWindowWidget);
        c->setWindowRelativeRect({ 0, 40, 100, 20 });
        c->setCaption("CheckBox");

        auto *lb = new ListBox(widgetTestWindowWidget);
        lb->setWindowRelativeRect({ 0, 60, 100, 100 });
        lb->addItem("This");
        lb->addItem("is");
        lb->addItem("a");
        lb->addItem("ListBox");

        auto *tb = new TextBox(widgetTestWindowWidget);
        tb->setWindowRelativeRect({ 0, 160, 100, 20 });
        tb->setText("Hello!");
        tb->setFocus(true);

        tb->onReturnPressed = [] (TextBox& textBox) {
            printf("TextBox %p return pressed: '%s'\n", &textBox, textBox.text().characters());
            MsgBox(nullptr, textBox.text());
        };

        WindowManager::the().setActiveWindow(widgetTestWindow);
    }

#if 0
    auto* clockWin = new Window;
    clockWin->setTitle("Clock");
    clockWin->setRect({ 500, 50, 100, 40 });
    clockWin->setMainWidget(new ClockWidget);
#endif

    MsgBox(nullptr, "This is a message box!");

    return loop.exec();
}
