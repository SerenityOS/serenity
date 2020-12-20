compile_gml(BrowserWindow.gml BrowserWindowGML.h browser_window_gml)
compile_gml(Tab.gml TabGML.h tab_gml)

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
    BrowserWindowGML.h
    TabGML.h
)

serenity_bin(Browser)
target_link_libraries(Browser LibWeb LibProtocol LibGUI LibDesktop)
