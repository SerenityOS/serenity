#include "TaskbarWindow.h"
#include <LibGUI/GApplication.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    TaskbarWindow window;
    window.show();
    return app.exec();
}
