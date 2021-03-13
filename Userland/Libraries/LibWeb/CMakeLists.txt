set(SOURCES
    Bindings/EventListenerWrapper.cpp
    Bindings/EventWrapperFactory.cpp
    Bindings/EventTargetWrapperFactory.cpp
    Bindings/LocationObject.cpp
    Bindings/MainThreadVM.cpp
    Bindings/NavigatorObject.cpp
    Bindings/NodeWrapperFactory.cpp
    Bindings/ScriptExecutionContext.cpp
    Bindings/WindowObject.cpp
    Bindings/Wrappable.cpp
    CSS/CSSImportRule.cpp
    CSS/CSSRule.cpp
    CSS/CSSStyleDeclaration.cpp
    CSS/CSSStyleRule.cpp
    CSS/CSSStyleSheet.cpp
    CSS/DefaultStyleSheetSource.cpp
    CSS/Length.cpp
    CSS/Parser/DeprecatedCSSParser.cpp
    CSS/Parser/StyleRules.cpp
    CSS/Parser/Token.cpp
    CSS/Parser/Tokenizer.cpp
    CSS/PropertyID.cpp
    CSS/PropertyID.h
    CSS/QuirksModeStyleSheetSource.cpp
    CSS/Selector.cpp
    CSS/SelectorEngine.cpp
    CSS/StyleInvalidator.cpp
    CSS/StyleProperties.cpp
    CSS/StyleResolver.cpp
    CSS/StyleSheet.cpp
    CSS/StyleSheetList.cpp
    CSS/StyleValue.cpp
    CSS/ValueID.cpp
    CSS/ValueID.h
    DOM/CharacterData.cpp
    DOM/CharacterData.idl
    DOM/Comment.cpp
    DOM/Document.cpp
    DOM/DocumentFragment.cpp
    DOM/DocumentType.cpp
    DOM/DOMImplementation.cpp
    DOM/Element.cpp
    DOM/ElementFactory.cpp
    DOM/Event.cpp
    DOM/Range.cpp
    DOM/EventDispatcher.cpp
    DOM/EventListener.cpp
    DOM/EventTarget.cpp
    DOM/Node.cpp
    DOM/ParentNode.cpp
    DOM/Position.cpp
    DOM/ShadowRoot.cpp
    DOM/Text.cpp
    DOM/Text.idl
    DOM/Timer.cpp
    DOM/Window.cpp
    DOMTreeModel.cpp
    Dump.cpp
    FontCache.cpp
    HTML/AttributeNames.cpp
    HTML/CanvasRenderingContext2D.cpp
    HTML/EventNames.cpp
    HTML/GlobalEventHandlers.cpp
    HTML/HTMLAnchorElement.cpp
    HTML/HTMLAreaElement.cpp
    HTML/HTMLAudioElement.cpp
    HTML/HTMLBRElement.cpp
    HTML/HTMLBaseElement.cpp
    HTML/HTMLBlinkElement.cpp
    HTML/HTMLBodyElement.cpp
    HTML/HTMLButtonElement.cpp
    HTML/HTMLCanvasElement.cpp
    HTML/HTMLDListElement.cpp
    HTML/HTMLDataElement.cpp
    HTML/HTMLDataListElement.cpp
    HTML/HTMLDetailsElement.cpp
    HTML/HTMLDialogElement.cpp
    HTML/HTMLDirectoryElement.cpp
    HTML/HTMLDivElement.cpp
    HTML/HTMLElement.cpp
    HTML/HTMLEmbedElement.cpp
    HTML/HTMLFieldSetElement.cpp
    HTML/HTMLFontElement.cpp
    HTML/HTMLFormElement.cpp
    HTML/HTMLFrameElement.cpp
    HTML/HTMLFrameSetElement.cpp
    HTML/HTMLHRElement.cpp
    HTML/HTMLHeadElement.cpp
    HTML/HTMLHeadingElement.cpp
    HTML/HTMLHtmlElement.cpp
    HTML/HTMLIFrameElement.cpp
    HTML/HTMLImageElement.cpp
    HTML/HTMLInputElement.cpp
    HTML/HTMLLIElement.cpp
    HTML/HTMLLabelElement.cpp
    HTML/HTMLLegendElement.cpp
    HTML/HTMLLinkElement.cpp
    HTML/HTMLMapElement.cpp
    HTML/HTMLMarqueeElement.cpp
    HTML/HTMLMediaElement.cpp
    HTML/HTMLMenuElement.cpp
    HTML/HTMLMetaElement.cpp
    HTML/HTMLMeterElement.cpp
    HTML/HTMLModElement.cpp
    HTML/HTMLOListElement.cpp
    HTML/HTMLObjectElement.cpp
    HTML/HTMLOptGroupElement.cpp
    HTML/HTMLOptionElement.cpp
    HTML/HTMLOutputElement.cpp
    HTML/HTMLParagraphElement.cpp
    HTML/HTMLParamElement.cpp
    HTML/HTMLPictureElement.cpp
    HTML/HTMLPreElement.cpp
    HTML/HTMLProgressElement.cpp
    HTML/HTMLQuoteElement.cpp
    HTML/HTMLScriptElement.cpp
    HTML/HTMLSelectElement.cpp
    HTML/HTMLSlotElement.cpp
    HTML/HTMLSourceElement.cpp
    HTML/HTMLSpanElement.cpp
    HTML/HTMLStyleElement.cpp
    HTML/HTMLTableCaptionElement.cpp
    HTML/HTMLTableCellElement.cpp
    HTML/HTMLTableColElement.cpp
    HTML/HTMLTableElement.cpp
    HTML/HTMLTableRowElement.cpp
    HTML/HTMLTableSectionElement.cpp
    HTML/HTMLTemplateElement.cpp
    HTML/HTMLTextAreaElement.cpp
    HTML/HTMLTimeElement.cpp
    HTML/HTMLTitleElement.cpp
    HTML/HTMLTrackElement.cpp
    HTML/HTMLUListElement.cpp
    HTML/HTMLUnknownElement.cpp
    HTML/HTMLVideoElement.cpp
    HTML/ImageData.cpp
    HTML/Parser/Entities.cpp
    HTML/Parser/HTMLDocumentParser.cpp
    HTML/Parser/HTMLToken.cpp
    HTML/Parser/HTMLTokenizer.cpp
    HTML/Parser/ListOfActiveFormattingElements.cpp
    HTML/Parser/StackOfOpenElements.cpp
    HTML/SubmitEvent.cpp
    HTML/TagNames.cpp
    HighResolutionTime/Performance.cpp
    InProcessWebView.cpp
    Layout/BlockBox.cpp
    Layout/BlockFormattingContext.cpp
    Layout/Box.cpp
    Layout/BoxModelMetrics.cpp
    Layout/BreakNode.cpp
    Layout/ButtonBox.cpp
    Layout/CanvasBox.cpp
    Layout/CheckBox.cpp
    Layout/FlexFormattingContext.cpp
    Layout/FormattingContext.cpp
    Layout/FrameBox.cpp
    Layout/ImageBox.cpp
    Layout/InitialContainingBlockBox.cpp
    Layout/InlineFormattingContext.cpp
    Layout/InlineNode.cpp
    Layout/LayoutPosition.cpp
    Layout/LineBox.cpp
    Layout/LineBoxFragment.cpp
    Layout/ListItemBox.cpp
    Layout/ListItemMarkerBox.cpp
    Layout/Node.cpp
    Layout/ReplacedBox.cpp
    Layout/SVGBox.cpp
    Layout/SVGGraphicsBox.cpp
    Layout/SVGPathBox.cpp
    Layout/SVGSVGBox.cpp
    Layout/TableBox.cpp
    Layout/TableCellBox.cpp
    Layout/TableFormattingContext.cpp
    Layout/TableRowBox.cpp
    Layout/TableRowGroupBox.cpp
    Layout/TextNode.cpp
    Layout/TreeBuilder.cpp
    LayoutTreeModel.cpp
    Loader/ContentFilter.cpp
    Loader/CSSLoader.cpp
    Loader/FrameLoader.cpp
    Loader/ImageLoader.cpp
    Loader/ImageResource.cpp
    Loader/Resource.cpp
    Loader/ResourceLoader.cpp
    Namespace.cpp
    NavigationTiming/PerformanceTiming.cpp
    OutOfProcessWebView.cpp
    Page/EventHandler.cpp
    Page/EditEventHandler.cpp
    Page/Frame.cpp
    Page/Page.cpp
    Painting/BorderPainting.cpp
    Painting/StackingContext.cpp
    SVG/SVGElement.cpp
    SVG/SVGGeometryElement.cpp
    SVG/SVGGraphicsElement.cpp
    SVG/SVGPathElement.cpp
    SVG/SVGSVGElement.cpp
    SVG/TagNames.cpp
    StylePropertiesModel.cpp
    UIEvents/EventNames.cpp
    UIEvents/MouseEvent.cpp
    URLEncoder.cpp
    XHR/EventNames.cpp
    XHR/XMLHttpRequest.cpp
    WebContentClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
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
    get_filename_component(basename ${class} NAME)
    add_wrapper_sources(Bindings/${basename}Wrapper.cpp Bindings/${basename}Wrapper.h)
    add_wrapper_sources(Bindings/${basename}Constructor.cpp Bindings/${basename}Constructor.h)
    add_wrapper_sources(Bindings/${basename}Prototype.cpp Bindings/${basename}Prototype.h)
    add_custom_command(
        OUTPUT Bindings/${basename}Wrapper.h
        COMMAND ${write_if_different} Bindings/${basename}Wrapper.h CodeGenerators/WrapperGenerator --header ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Wrapper.cpp
        COMMAND ${write_if_different} Bindings/${basename}Wrapper.cpp CodeGenerators/WrapperGenerator --implementation ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Constructor.h
        COMMAND ${write_if_different} Bindings/${basename}Constructor.h CodeGenerators/WrapperGenerator --constructor-header ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Constructor.cpp
        COMMAND ${write_if_different} Bindings/${basename}Constructor.cpp CodeGenerators/WrapperGenerator --constructor-implementation ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Prototype.h
        COMMAND ${write_if_different} Bindings/${basename}Prototype.h CodeGenerators/WrapperGenerator --prototype-header ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Prototype.cpp
        COMMAND ${write_if_different} Bindings/${basename}Prototype.cpp CodeGenerators/WrapperGenerator --prototype-implementation ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_target(generate_${basename}Wrapper.h DEPENDS Bindings/${class}Wrapper.h)
    add_custom_target(generate_${basename}Wrapper.cpp DEPENDS Bindings/${class}Wrapper.cpp)
    add_custom_target(generate_${basename}Constructor.h DEPENDS Bindings/${class}Constructor.h)
    add_custom_target(generate_${basename}Constructor.cpp DEPENDS Bindings/${class}Constructor.cpp)
    add_custom_target(generate_${basename}Prototype.h DEPENDS Bindings/${class}Prototype.h)
    add_custom_target(generate_${basename}Prototype.cpp DEPENDS Bindings/${class}Prototype.cpp)
endfunction()

libweb_js_wrapper(CSS/CSSStyleDeclaration)
libweb_js_wrapper(CSS/CSSStyleSheet)
libweb_js_wrapper(CSS/StyleSheet)
libweb_js_wrapper(CSS/StyleSheetList)
libweb_js_wrapper(DOM/CharacterData)
libweb_js_wrapper(DOM/Comment)
libweb_js_wrapper(DOM/Document)
libweb_js_wrapper(DOM/DocumentFragment)
libweb_js_wrapper(DOM/DocumentType)
libweb_js_wrapper(DOM/DOMException)
libweb_js_wrapper(DOM/DOMImplementation)
libweb_js_wrapper(DOM/Element)
libweb_js_wrapper(DOM/Event)
libweb_js_wrapper(DOM/EventTarget)
libweb_js_wrapper(DOM/ShadowRoot)
libweb_js_wrapper(DOM/Node)
libweb_js_wrapper(DOM/Range)
libweb_js_wrapper(DOM/Text)
libweb_js_wrapper(HTML/CanvasRenderingContext2D)
libweb_js_wrapper(HTML/HTMLAnchorElement)
libweb_js_wrapper(HTML/HTMLAreaElement)
libweb_js_wrapper(HTML/HTMLAudioElement)
libweb_js_wrapper(HTML/HTMLBaseElement)
libweb_js_wrapper(HTML/HTMLBodyElement)
libweb_js_wrapper(HTML/HTMLBRElement)
libweb_js_wrapper(HTML/HTMLButtonElement)
libweb_js_wrapper(HTML/HTMLCanvasElement)
libweb_js_wrapper(HTML/HTMLDataElement)
libweb_js_wrapper(HTML/HTMLDataListElement)
libweb_js_wrapper(HTML/HTMLDetailsElement)
libweb_js_wrapper(HTML/HTMLDialogElement)
libweb_js_wrapper(HTML/HTMLDirectoryElement)
libweb_js_wrapper(HTML/HTMLDivElement)
libweb_js_wrapper(HTML/HTMLDListElement)
libweb_js_wrapper(HTML/HTMLElement)
libweb_js_wrapper(HTML/HTMLEmbedElement)
libweb_js_wrapper(HTML/HTMLFieldSetElement)
libweb_js_wrapper(HTML/HTMLFontElement)
libweb_js_wrapper(HTML/HTMLFormElement)
libweb_js_wrapper(HTML/HTMLFrameElement)
libweb_js_wrapper(HTML/HTMLFrameSetElement)
libweb_js_wrapper(HTML/HTMLHeadElement)
libweb_js_wrapper(HTML/HTMLHeadingElement)
libweb_js_wrapper(HTML/HTMLHRElement)
libweb_js_wrapper(HTML/HTMLHtmlElement)
libweb_js_wrapper(HTML/HTMLIFrameElement)
libweb_js_wrapper(HTML/HTMLImageElement)
libweb_js_wrapper(HTML/HTMLInputElement)
libweb_js_wrapper(HTML/HTMLLabelElement)
libweb_js_wrapper(HTML/HTMLLegendElement)
libweb_js_wrapper(HTML/HTMLLIElement)
libweb_js_wrapper(HTML/HTMLLinkElement)
libweb_js_wrapper(HTML/HTMLMapElement)
libweb_js_wrapper(HTML/HTMLMarqueeElement)
libweb_js_wrapper(HTML/HTMLMediaElement)
libweb_js_wrapper(HTML/HTMLMenuElement)
libweb_js_wrapper(HTML/HTMLMetaElement)
libweb_js_wrapper(HTML/HTMLMeterElement)
libweb_js_wrapper(HTML/HTMLModElement)
libweb_js_wrapper(HTML/HTMLObjectElement)
libweb_js_wrapper(HTML/HTMLOListElement)
libweb_js_wrapper(HTML/HTMLOptGroupElement)
libweb_js_wrapper(HTML/HTMLOptionElement)
libweb_js_wrapper(HTML/HTMLOutputElement)
libweb_js_wrapper(HTML/HTMLParagraphElement)
libweb_js_wrapper(HTML/HTMLParamElement)
libweb_js_wrapper(HTML/HTMLPictureElement)
libweb_js_wrapper(HTML/HTMLPreElement)
libweb_js_wrapper(HTML/HTMLProgressElement)
libweb_js_wrapper(HTML/HTMLQuoteElement)
libweb_js_wrapper(HTML/HTMLScriptElement)
libweb_js_wrapper(HTML/HTMLSelectElement)
libweb_js_wrapper(HTML/HTMLSlotElement)
libweb_js_wrapper(HTML/HTMLSourceElement)
libweb_js_wrapper(HTML/HTMLSpanElement)
libweb_js_wrapper(HTML/HTMLStyleElement)
libweb_js_wrapper(HTML/HTMLTableCaptionElement)
libweb_js_wrapper(HTML/HTMLTableCellElement)
libweb_js_wrapper(HTML/HTMLTableColElement)
libweb_js_wrapper(HTML/HTMLTableElement)
libweb_js_wrapper(HTML/HTMLTableRowElement)
libweb_js_wrapper(HTML/HTMLTableSectionElement)
libweb_js_wrapper(HTML/HTMLTemplateElement)
libweb_js_wrapper(HTML/HTMLTextAreaElement)
libweb_js_wrapper(HTML/HTMLTimeElement)
libweb_js_wrapper(HTML/HTMLTitleElement)
libweb_js_wrapper(HTML/HTMLTrackElement)
libweb_js_wrapper(HTML/HTMLUListElement)
libweb_js_wrapper(HTML/HTMLUnknownElement)
libweb_js_wrapper(HTML/HTMLVideoElement)
libweb_js_wrapper(HTML/ImageData)
libweb_js_wrapper(HTML/SubmitEvent)
libweb_js_wrapper(HighResolutionTime/Performance)
libweb_js_wrapper(NavigationTiming/PerformanceTiming)
libweb_js_wrapper(SVG/SVGElement)
libweb_js_wrapper(SVG/SVGGeometryElement)
libweb_js_wrapper(SVG/SVGGraphicsElement)
libweb_js_wrapper(SVG/SVGPathElement)
libweb_js_wrapper(SVG/SVGSVGElement)
libweb_js_wrapper(UIEvents/MouseEvent)
libweb_js_wrapper(UIEvents/UIEvent)
libweb_js_wrapper(XHR/ProgressEvent)
libweb_js_wrapper(XHR/XMLHttpRequest)
libweb_js_wrapper(XHR/XMLHttpRequestEventTarget)

get_property(WRAPPER_SOURCES GLOBAL PROPERTY wrapper_sources)
set(SOURCES ${SOURCES} ${WRAPPER_SOURCES})

add_custom_command(
    OUTPUT CSS/PropertyID.h
    COMMAND ${write_if_different} CSS/PropertyID.h CodeGenerators/Generate_CSS_PropertyID_h ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_h
    MAIN_DEPENDENCY CSS/Properties.json
)
add_custom_target(generate_PropertyID.h DEPENDS CSS/PropertyID.h)

add_custom_command(
    OUTPUT CSS/PropertyID.cpp
    COMMAND ${CMAKE_COMMAND} -E make_directory CSS
    COMMAND ${write_if_different} CSS/PropertyID.cpp CodeGenerators/Generate_CSS_PropertyID_cpp ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_cpp
    MAIN_DEPENDENCY CSS/Properties.json
)

add_custom_command(
    OUTPUT CSS/ValueID.h
    COMMAND ${write_if_different} CSS/ValueID.h CodeGenerators/Generate_CSS_ValueID_h ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Identifiers.json
    VERBATIM
    DEPENDS Generate_CSS_ValueID_h
    MAIN_DEPENDENCY CSS/Identifiers.json
)
add_custom_target(generate_ValueID.h DEPENDS CSS/ValueID.h)

add_custom_command(
    OUTPUT CSS/ValueID.cpp
    COMMAND ${CMAKE_COMMAND} -E make_directory CSS
    COMMAND ${write_if_different} CSS/ValueID.cpp CodeGenerators/Generate_CSS_ValueID_cpp ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Identifiers.json
    VERBATIM
    DEPENDS Generate_CSS_ValueID_cpp
    MAIN_DEPENDENCY CSS/Identifiers.json
)

add_custom_command(
    OUTPUT CSS/DefaultStyleSheetSource.cpp
    COMMAND ${write_if_different} CSS/DefaultStyleSheetSource.cpp ${CMAKE_CURRENT_SOURCE_DIR}/Scripts/GenerateStyleSheetSource.sh default_stylesheet_source ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Default.css
    VERBATIM
    DEPENDS Scripts/GenerateStyleSheetSource.sh
    MAIN_DEPENDENCY CSS/Default.css
)

add_custom_command(
    OUTPUT CSS/QuirksModeStyleSheetSource.cpp
    COMMAND ${write_if_different} CSS/QuirksModeStyleSheetSource.cpp ${CMAKE_CURRENT_SOURCE_DIR}/Scripts/GenerateStyleSheetSource.sh quirks_mode_stylesheet_source ${CMAKE_CURRENT_SOURCE_DIR}/CSS/QuirksMode.css
    VERBATIM
    DEPENDS Scripts/GenerateStyleSheetSource.sh
    MAIN_DEPENDENCY CSS/Default.css
)

serenity_lib(LibWeb web)
target_link_libraries(LibWeb LibCore LibJS LibMarkdown LibGemini LibGUI LibGfx LibTextCodec LibProtocol LibImageDecoderClient)

add_subdirectory(DumpLayoutTree)
