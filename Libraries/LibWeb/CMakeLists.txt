set(SOURCES
    Bindings/CanvasRenderingContext2DWrapper.cpp
    Bindings/EventListenerWrapper.cpp
    Bindings/EventWrapper.cpp
    Bindings/LocationObject.cpp
    Bindings/MouseEventWrapper.cpp
    Bindings/NavigatorObject.cpp
    Bindings/NodeWrapperFactory.cpp
    Bindings/WindowObject.cpp
    Bindings/Wrappable.cpp
    Bindings/XMLHttpRequestConstructor.cpp
    Bindings/XMLHttpRequestPrototype.cpp
    Bindings/XMLHttpRequestWrapper.cpp
    CSS/Length.cpp
    CSS/Selector.cpp
    CSS/SelectorEngine.cpp
    CSS/StyleDeclaration.cpp
    CSS/StyleProperties.cpp
    CSS/StyleResolver.cpp
    CSS/StyleRule.cpp
    CSS/StyleSheet.cpp
    CSS/StyleSheetList.cpp
    CSS/StyleValue.cpp
    DOM/AttributeNames.cpp
    DOM/CanvasRenderingContext2D.cpp
    DOM/CharacterData.cpp
    DOM/Comment.cpp
    DOM/Document.cpp
    DOM/DocumentType.cpp
    DOM/Element.cpp
    DOM/ElementFactory.cpp
    DOM/EventListener.cpp
    DOM/EventTarget.cpp
    DOM/HTMLAnchorElement.cpp
    DOM/HTMLBlinkElement.cpp
    DOM/HTMLBodyElement.cpp
    DOM/HTMLBRElement.cpp
    DOM/HTMLCanvasElement.cpp
    DOM/HTMLElement.cpp
    DOM/HTMLFontElement.cpp
    DOM/HTMLFormElement.cpp
    DOM/HTMLHeadElement.cpp
    DOM/HTMLHeadingElement.cpp
    DOM/HTMLHRElement.cpp
    DOM/HTMLHtmlElement.cpp
    DOM/HTMLIFrameElement.cpp
    DOM/HTMLImageElement.cpp
    DOM/HTMLInputElement.cpp
    DOM/HTMLObjectElement.cpp
    DOM/HTMLLinkElement.cpp
    DOM/HTMLScriptElement.cpp
    DOM/HTMLStyleElement.cpp
    DOM/HTMLTableElement.cpp
    DOM/HTMLTableCellElement.cpp
    DOM/HTMLTableRowElement.cpp
    DOM/HTMLTitleElement.cpp
    DOM/ImageData.cpp
    DOM/Node.cpp
    DOM/ParentNode.cpp
    DOM/TagNames.cpp
    DOM/Text.cpp
    DOM/Window.cpp
    DOM/XMLHttpRequest.cpp
    DOMTreeModel.cpp
    Dump.cpp
    FontCache.cpp
    Frame/EventHandler.cpp
    Frame/Frame.cpp
    Layout/BoxModelMetrics.cpp
    Layout/LayoutBlock.cpp
    Layout/LayoutBox.cpp
    Layout/LayoutBreak.cpp
    Layout/LayoutCanvas.cpp
    Layout/LayoutDocument.cpp
    Layout/LayoutFrame.cpp
    Layout/LayoutImage.cpp
    Layout/LayoutInline.cpp
    Layout/LayoutListItem.cpp
    Layout/LayoutListItemMarker.cpp
    Layout/LayoutNode.cpp
    Layout/LayoutReplaced.cpp
    Layout/LayoutTable.cpp
    Layout/LayoutTableCell.cpp
    Layout/LayoutTableRow.cpp
    Layout/LayoutTableRowGroup.cpp
    Layout/LayoutText.cpp
    Layout/LayoutTreeBuilder.cpp
    Layout/LayoutWidget.cpp
    Layout/LineBox.cpp
    Layout/LineBoxFragment.cpp
    LayoutTreeModel.cpp
    Loader/FrameLoader.cpp
    Loader/ImageLoader.cpp
    Loader/ImageResource.cpp
    Loader/Resource.cpp
    Loader/ResourceLoader.cpp
    Page.cpp
    PageView.cpp
    Painting/StackingContext.cpp
    Parser/CSSParser.cpp
    Parser/Entities.cpp
    Parser/HTMLDocumentParser.cpp
    Parser/HTMLParser.cpp
    Parser/HTMLToken.cpp
    Parser/HTMLTokenizer.cpp
    Parser/ListOfActiveFormattingElements.cpp
    Parser/StackOfOpenElements.cpp
    StylePropertiesModel.cpp
    URLEncoder.cpp
    CSS/PropertyID.h
    CSS/PropertyID.cpp
    CSS/DefaultStyleSheetSource.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
)

set_property(GLOBAL PROPERTY wrapper_sources)
function(add_wrapper_sources)
    get_property(tmp GLOBAL PROPERTY wrapper_sources)
    foreach(arg ${ARGV})
        set(tmp ${tmp}
            ${arg}
        )
    endforeach()
    set_property(GLOBAL PROPERTY wrapper_sources "${tmp}")
endfunction(add_wrapper_sources)

function(libweb_js_wrapper class)
    add_wrapper_sources(Bindings/${class}Wrapper.cpp Bindings/${class}Wrapper.h)
    add_custom_command(
        OUTPUT Bindings/${class}Wrapper.h
        COMMAND /bin/mkdir -p Bindings
        COMMAND WrapperGenerator --header ${CMAKE_CURRENT_SOURCE_DIR}/DOM/${class}.idl > Bindings/${class}Wrapper.h
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY DOM/${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${class}Wrapper.cpp
        COMMAND /bin/mkdir -p Bindings
        COMMAND WrapperGenerator --implementation ${CMAKE_CURRENT_SOURCE_DIR}/DOM/${class}.idl > Bindings/${class}Wrapper.cpp
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY DOM/${class}.idl
    )
    add_custom_target(generate_${class}Wrapper.h DEPENDS Bindings/${class}Wrapper.h)
    add_custom_target(generate_${class}Wrapper.cpp DEPENDS Bindings/${class}Wrapper.cpp)
endfunction()

libweb_js_wrapper(EventTarget)
libweb_js_wrapper(Node)
libweb_js_wrapper(Document)
libweb_js_wrapper(Element)
libweb_js_wrapper(HTMLElement)
libweb_js_wrapper(HTMLImageElement)
libweb_js_wrapper(HTMLCanvasElement)
libweb_js_wrapper(ImageData)

get_property(WRAPPER_SOURCES GLOBAL PROPERTY wrapper_sources)
set(SOURCES ${SOURCES} ${WRAPPER_SOURCES})

add_custom_command(
    OUTPUT CSS/PropertyID.h
    COMMAND /bin/mkdir -p CSS
    COMMAND Generate_CSS_PropertyID_h ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > CSS/PropertyID.h
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_h
    MAIN_DEPENDENCY CSS/Properties.json
)
add_custom_target(generate_PropertyID.h DEPENDS CSS/PropertyID.h)

add_custom_command(
    OUTPUT CSS/PropertyID.cpp
    COMMAND /bin/mkdir -p CSS
    COMMAND Generate_CSS_PropertyID_cpp ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > CSS/PropertyID.cpp
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_cpp
    MAIN_DEPENDENCY CSS/Properties.json
)

add_custom_command(
    OUTPUT CSS/DefaultStyleSheetSource.cpp
    COMMAND /bin/mkdir -p CSS
    COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/Scripts/GenerateStyleSheetSource.sh default_stylesheet_source
            ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Default.css > CSS/DefaultStyleSheetSource.cpp
    VERBATIM
    DEPENDS Scripts/GenerateStyleSheetSource.sh
    MAIN_DEPENDENCY CSS/Default.css
)

serenity_lib(LibWeb web)
target_link_libraries(LibWeb LibCore LibJS LibMarkdown LibGemini LibGUI LibGfx LibTextCodec LibProtocol)
