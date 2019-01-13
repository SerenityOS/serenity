#include "WindowComposer.h"
#include "Process.h"
#include <Widgets/Font.h>
#include <Widgets/FrameBuffer.h>
#include <Widgets/WindowManager.h>
#include <Widgets/EventLoop.h>
#include <Widgets/MsgBox.h>
#include <Widgets/TextBox.h>
#include <Widgets/Label.h>
#include <Widgets/ListBox.h>
#include <Widgets/Button.h>
#include <Widgets/CheckBox.h>
#include <Widgets/Window.h>

void WindowComposer_main()
{
    auto info = current->get_display_info();

    dbgprintf("Screen is %ux%ux%ubpp\n", info.width, info.height, info.bpp);

    FrameBuffer framebuffer((dword*)info.framebuffer, info.width, info.height);

    MsgBox(nullptr, "Serenity Operating System");

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

    dbgprintf("Entering WindowComposer main loop.\n");
    EventLoop::main().exec();

    ASSERT_NOT_REACHED();
}
