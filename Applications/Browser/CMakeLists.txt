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
)

serenity_bin(Browser)
target_link_libraries(Browser LibWeb LibProtocol LibGUI LibDesktop)
