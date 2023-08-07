set(TEST_SOURCES
    TestSqlBtreeIndex.cpp
    TestSqlDatabase.cpp
    TestSqlExpressionParser.cpp
    TestSqlHeap.cpp
    TestSqlStatementExecution.cpp
    TestSqlStatementParser.cpp
    TestSqlValueAndTuple.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSQL LIBS LibSQL LibIPC)
endforeach()
