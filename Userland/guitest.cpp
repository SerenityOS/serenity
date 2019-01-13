#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>

int main(int argc, char** argv)
{
    GUI_CreateWindowParameters wparams;
    wparams.rect = { 200, 200, 300, 200 };
    wparams.background_color = 0xffc0c0;
    strcpy(wparams.title, "GUI test app");
    int window_id = syscall(SC_gui_create_window, &wparams);
    if (window_id < 0) {
        perror("gui_create_window");
        return 1;
    }

    GUI_CreateWidgetParameters label_params;
    label_params.type = GUI_WidgetType::Label;
    label_params.rect = { 20, 20, 260, 20 };
    label_params.opaque = false;
    strcpy(label_params.text, "Hello World!");
    int label_id = syscall(SC_gui_create_widget, window_id, &label_params);
    if (label_id < 0) {
        perror("gui_create_widget");
        return 1;
    }

    GUI_CreateWidgetParameters button_params;
    button_params.type = GUI_WidgetType::Button;
    button_params.rect = { 60, 60, 120, 20 };
    strcpy(button_params.text, "I'm a button!");
    int button_id = syscall(SC_gui_create_widget, window_id, &button_params);
    if (button_id < 0) {
        perror("gui_create_widget");
        return 1;
    }

    for (;;) {
    }
    return 0;
}
