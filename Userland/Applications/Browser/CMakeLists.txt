serenity_component(
    Browser
    RECOMMENDED
    TARGETS Browser
    DEPENDS ImageDecoder RequestServer WebContent WebSocket
)

compile_gml(BrowserWindow.gml BrowserWindowGML.h browser_window_gml)
compile_gml(CookiesTab.gml CookiesTabGML.h cookies_tab_gml)
compile_gml(EditBookmark.gml EditBookmarkGML.h edit_bookmark_gml)
compile_gml(StorageWidget.gml StorageWidgetGML.h storage_widget_gml)
compile_gml(Tab.gml TabGML.h tab_gml)

set(SOURCES
    BookmarksBarWidget.cpp
    BrowserWindow.cpp
    BrowserWindowGML.h
    ConsoleWidget.cpp
    CookieJar.cpp
    CookiesModel.cpp
    CookiesTabGML.h
    DownloadWidget.cpp
    EditBookmarkGML.h
    History.cpp
    IconBag.cpp
    InspectorWidget.cpp
    StorageWidget.cpp
    StorageWidgetGML.h
    Tab.cpp
    TabGML.h
    WindowActions.cpp
    main.cpp
)

serenity_app(Browser ICON app-browser)
target_link_libraries(Browser LibWeb LibProtocol LibGUI LibDesktop LibConfig LibMain)
link_with_unicode_data(Browser)
