function Link(el)
    el.target = string.gsub(el.target, "file:///bin/.*", "/cant-run-application.html")
    el.target = string.gsub(el.target, "help://man/([^/]*)/(.*)", "/man%1/%2.html")
    return el
end

function Image(el)
    -- Images that are not icons are always copied to the website root.
    if el.src:find("^/res/icons/") == nil then
        el.src = "/" .. el.src
        return el
    end

    local pattern = "/res/icons/(.*)"
    local image = string.gsub(el.src, pattern, "%1")

    el.src = "/icons/" .. image
    file = io.open("icons.txt", "a+")
    file:write(image .. "\n")
    file:close()
    return el
end
