function Header(header)
    local level = header.level
    local identifier = header.identifier
    local anchor = pandoc.RawInline('html', '<a style="margin-right: 15px" href="#' .. identifier .. '">#</a>')
  
    -- Create a list of inline elements containing the anchor and header content
    local new_content = pandoc.List({anchor})
    for _, elem in ipairs(header.content) do
      new_content:insert(elem)
    end
  
    return pandoc.Header(level, new_content, identifier)
  end
  