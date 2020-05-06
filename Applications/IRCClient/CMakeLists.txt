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

serenity_bin(IRCClient)
target_link_libraries(IRCClient LibWeb LibGUI)
