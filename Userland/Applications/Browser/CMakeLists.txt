serenity_component(
    Browser
    RECOMMENDED
    TARGETS Browser
    DEPENDS BrowserSettings ImageDecoder RequestServer WebContent
)

stringify_gml(BrowserWindow.gml BrowserWindowGML.h browser_window_gml)
compile_gml(EditBookmarkWidget.gml EditBookmarkWidgetGML.cpp)
compile_gml(History/HistoryWidget.gml History/HistoryWidgetGML.cpp)
compile_gml(StorageWidget.gml StorageWidgetGML.cpp)
stringify_gml(Tab.gml TabGML.h tab_gml)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserWindow.cpp
    CookiesModel.cpp
    DownloadWidget.cpp
    EditBookmarkDialog.cpp
    EditBookmarkWidgetGML.cpp
    History/HistoryModel.cpp
    History/HistoryWidget.cpp
    History/HistoryWidgetGML.cpp
    IconBag.cpp
    InspectorWidget.cpp
    StorageModel.cpp
    StorageWidget.cpp
    StorageWidgetGML.cpp
    Tab.cpp
    TaskManagerWidget.cpp
    URLBox.cpp
    WindowActions.cpp
    main.cpp
)

set(GENERATED_SOURCES
    BrowserWindowGML.h
    TabGML.h
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser PRIVATE LibCore LibWebView LibWeb LibProtocol LibGUI LibDesktop LibConfig LibGfx LibIPC LibJS LibLocale LibMain LibSyntax LibURL)
