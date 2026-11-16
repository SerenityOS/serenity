serenity_component(
    Browser
    RECOMMENDED
    TARGETS Browser
    DEPENDS BrowserSettings ImageDecoder RequestServer WebContent
)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserWindow.cpp
    CookiesModel.cpp
    DownloadWidget.cpp
    FindInPageWidget.cpp
    History/HistoryModel.cpp
    History/HistoryWidget.cpp
    IconBag.cpp
    InspectorWidget.cpp
    StorageModel.cpp
    StorageWidget.cpp
    Tab.cpp
    TaskManagerWidget.cpp
    URLBox.cpp
    WindowActions.cpp
    main.cpp
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser PRIVATE LibCore LibWebView LibWeb LibProtocol LibGUI LibDesktop LibConfig LibGfx LibIPC LibJS LibLocale LibMain LibSyntax LibURL)
