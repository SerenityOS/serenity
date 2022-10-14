function Link(el)
    el.target = string.gsub(el.target, "file:///bin/.*", "../cant-run-application.html")
    el.target = string.gsub(el.target, "help://man/([^/]*)/(.*)", "../man%1/%2.html")
    return el
end

function Image(el)
    -- HACK: Handle images that are not icons separately; they're copied manually in the
    --       Meta/build-manpages-website.sh script.
    --       Ideally this would be generalized so the paths export below could handle both.
    if el.src:find("^/res/icons/") == nil then
        el.src = "../" .. el.src
        return el
    end

    local pattern = "/res/icons/(.*)"
    local image = string.gsub(el.src, pattern, "%1")

    el.src = "../icons/" .. image
    file = io.open("icons.txt", "a+")
    file:write(image .. "\n")
    file:close()
    return el
end
