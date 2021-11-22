serenity_component(
    Mail
    RECOMMENDED
    TARGETS Mail
)

compile_gml(MailWindow.gml MailWindowGML.h mail_window_gml)

set(SOURCES
    AccountHolder.cpp
    InboxModel.cpp
    MailboxTreeModel.cpp
    MailWidget.cpp
    MailWindowGML.h
    main.cpp
)

serenity_app(Mail ICON app-mail)
target_link_libraries(Mail LibConfig LibCore LibDesktop LibGfx LibGUI LibIMAP LibWeb LibMain)
