set(AK_TEST_SOURCES
    TestAllOf.cpp
    TestAnyOf.cpp
    TestArray.cpp
    TestAtomic.cpp
    TestBadge.cpp
    TestBase64.cpp
    TestBinaryHeap.cpp
    TestBinarySearch.cpp
    TestBitCast.cpp
    TestBitmap.cpp
    TestByteBuffer.cpp
    TestChecked.cpp
    TestCircularDeque.cpp
    TestCircularDuplexStream.cpp
    TestCircularQueue.cpp
    TestComplex.cpp
    TestDistinctNumeric.cpp
    TestDoublyLinkedList.cpp
    TestEndian.cpp
    TestEnumBits.cpp
    TestFind.cpp
    TestFormat.cpp
    TestHashFunctions.cpp
    TestHashMap.cpp
    TestHashTable.cpp
    TestHex.cpp
    TestIPv4Address.cpp
    TestIndexSequence.cpp
    TestIntrusiveRedBlackTree.cpp
    TestJSON.cpp
    TestLexicalPath.cpp
    TestMACAddress.cpp
    TestMemMem.cpp
    TestMemoryStream.cpp
    TestNeverDestroyed.cpp
    TestNonnullRefPtr.cpp
    TestNumberFormat.cpp
    TestOptional.cpp
    TestQueue.cpp
    TestQuickSort.cpp
    TestRedBlackTree.cpp
    TestRefPtr.cpp
    TestSinglyLinkedList.cpp
    TestSourceGenerator.cpp
    TestSpan.cpp
    TestString.cpp
    TestStringUtils.cpp
    TestStringView.cpp
    TestTime.cpp
    TestTrie.cpp
    TestTypeTraits.cpp
    TestTypedTransfer.cpp
    TestURL.cpp
    TestUtf8.cpp
    TestVector.cpp
    TestWeakPtr.cpp
)

foreach(source ${AK_TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source})
    target_link_libraries(${name} LibCore)
    install(TARGETS ${name} RUNTIME DESTINATION usr/Tests/AK)
endforeach()

get_filename_component(TEST_FRM_RESOLVED ./test.frm REALPATH)
install(FILES ./4chan_catalog.json DESTINATION usr/Tests/AK)
install(FILES ${TEST_FRM_RESOLVED} DESTINATION usr/Tests/AK)
