set(SOURCES
    IRCAppWindow.cpp
    IRCChannel.cpp
    IRCChannelMemberListModel.cpp
    IRCClient.cpp
    IRCLogBuffer.cpp
    IRCQuery.cpp
    IRCWindow.cpp
    IRCWindowListModel.cpp
    main.cpp
)

serenity_app(IRCClient ICON app-irc-client)
target_link_libraries(IRCClient LibWeb LibGUI)
