compile_json_gui(BrowserWindow.json BrowserWindowUI.h browser_window_ui_json)
compile_json_gui(Tab.json TabUI.h tab_ui_json)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserConsoleClient.cpp
    ConsoleWidget.cpp
    DownloadWidget.cpp
    History.cpp
    InspectorWidget.cpp
    main.cpp
    Tab.cpp
    WindowActions.cpp
    BrowserWindowUI.h
    TabUI.h
)

serenity_bin(Browser)
target_link_libraries(Browser LibWeb LibProtocol LibGUI LibDesktop)
