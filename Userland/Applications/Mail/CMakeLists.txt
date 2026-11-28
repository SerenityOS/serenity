serenity_component(
    Mail
    RECOMMENDED
    TARGETS Mail
    DEPENDS WebContent
)

set(SOURCES
    AccountHolder.cpp
    InboxModel.cpp
    MailWidget.cpp
    MailboxTreeModel.cpp
    main.cpp
)

serenity_app(Mail ICON app-mail)
target_link_libraries(Mail PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibIMAP LibWebView LibWeb LibMain LibURL)
