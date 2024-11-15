serenity_component(
    Browser
    RECOMMENDED
    TARGETS Browser
    DEPENDS BrowserSettings ImageDecoder RequestServer WebContent
)

compile_gml(BrowserWindowWidget.gml BrowserWindowWidgetGML.cpp)
compile_gml(EditBookmarkWidget.gml EditBookmarkWidgetGML.cpp)
compile_gml(History/HistoryWidget.gml History/HistoryWidgetGML.cpp)
compile_gml(StorageWidget.gml StorageWidgetGML.cpp)
compile_gml(BrowserTabWidget.gml BrowserTabWidgetGML.cpp)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserWindow.cpp
    BrowserWindowWidgetGML.cpp
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
    BrowserTabWidget.cpp
    BrowserTabWidgetGML.cpp
    TaskManagerWidget.cpp
    URLBox.cpp
    WindowActions.cpp
    main.cpp
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser PRIVATE LibCore LibWebView LibWeb LibProtocol LibGUI LibDesktop LibConfig LibGfx LibIPC LibJS LibLocale LibMain LibSyntax LibURL)
