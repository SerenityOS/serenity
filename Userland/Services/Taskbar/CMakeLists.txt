serenity_component(
    Taskbar
    REQUIRED
    TARGETS Taskbar
)

compile_gml(SessionExitInhibitionDialog.gml SessionExitInhibitionDialogGML.h session_exit_inhibition_dialog_gml)

set(SOURCES
    main.cpp
    ClockWidget.cpp
    SessionExitInhibitionDialog.cpp
    SessionExitInhibitionDialogGML.h
    QuickLaunchWidget.cpp
    ShutdownDialog.cpp
    TaskbarButton.cpp
    TaskbarWindow.cpp
    WindowList.cpp
)

serenity_bin(Taskbar)
target_link_libraries(Taskbar LibGUI LibDesktop LibConfig LibMain LibSession)
serenity_install_headers(Services/Taskbar)
