serenity_component(
    Browser
    RECOMMENDED
    TARGETS Browser
    DEPENDS BrowserSettings ImageDecoder RequestServer WebContent
)

stringify_gml(BrowserWindow.gml BrowserWindowGML.h browser_window_gml)
stringify_gml(EditBookmark.gml EditBookmarkGML.h edit_bookmark_gml)
stringify_gml(History/HistoryWidget.gml HistoryWidgetGML.h history_widget_gml)
stringify_gml(StorageWidget.gml StorageWidgetGML.h storage_widget_gml)
stringify_gml(Tab.gml TabGML.h tab_gml)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserWindow.cpp
    CookiesModel.cpp
    DownloadWidget.cpp
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

set(GENERATED_SOURCES
    BrowserWindowGML.h
    EditBookmarkGML.h
    HistoryWidgetGML.h
    StorageWidgetGML.h
    TabGML.h
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser PRIVATE LibCore LibWebView LibWeb LibProtocol LibGUI LibDesktop LibConfig LibGfx LibIPC LibJS LibLocale LibMain LibSyntax LibURL)
