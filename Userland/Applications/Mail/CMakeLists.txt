serenity_component(
    Mail
    RECOMMENDED
    TARGETS Mail
    DEPENDS WebContent
)

stringify_gml(MailWindow.gml MailWindowGML.h mail_window_gml)

set(SOURCES
    AccountHolder.cpp
    InboxModel.cpp
    MailWidget.cpp
    MailboxTreeModel.cpp
    main.cpp
)

set(GENERATED_SOURCES
    MailWindowGML.h
)

serenity_app(Mail ICON app-mail)
target_link_libraries(Mail PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibIMAP LibWebView LibWeb LibMain LibURL)
