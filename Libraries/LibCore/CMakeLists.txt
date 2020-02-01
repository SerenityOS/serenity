set(SOURCES
    CArgsParser.cpp
    CConfigFile.cpp
    CDirIterator.cpp
    CElapsedTimer.cpp
    CEvent.cpp
    CEventLoop.cpp
    CFile.cpp
    CGzip.cpp
    CHttpJob.cpp
    CHttpRequest.cpp
    CHttpResponse.cpp
    CIODevice.cpp
    CLocalServer.cpp
    CLocalSocket.cpp
    CNetworkJob.cpp
    CNetworkResponse.cpp
    CNotifier.cpp
    CObject.cpp
    CProcessStatisticsReader.cpp
    CSocket.cpp
    CTCPServer.cpp
    CTCPSocket.cpp
    CUdpServer.cpp
    CUdpSocket.cpp
    CTimer.cpp
    CUserInfo.cpp
    puff.c
)

serenity_lib(LibCore core)
target_link_libraries(LibCore LibC)
