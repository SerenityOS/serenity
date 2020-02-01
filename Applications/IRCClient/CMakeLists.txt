set(SOURCES
    main.cpp
    IRCAppWindow.cpp
    IRCChannel.cpp
    IRCChannelMemberListModel.cpp
    IRCClient.cpp
    IRCLogBuffer.cpp
    IRCQuery.cpp
    IRCWindow.cpp
    IRCWindowListModel.cpp
)

serenity_app(IRCClient)
target_link_libraries(IRCClient LibHTML)
