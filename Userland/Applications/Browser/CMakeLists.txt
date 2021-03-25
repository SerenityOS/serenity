compile_gml(BrowserWindow.gml BrowserWindowGML.h browser_window_gml)
compile_gml(EditBookmark.gml EditBookmarkGML.h edit_bookmark_gml)
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
    EditBookmarkGML.h
    TabGML.h
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser LibWeb LibProtocol LibGUI LibDesktop)
