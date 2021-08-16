<html>
<head>
   <title>Temperature Table</title>
</head>
<body>

<h1>Temperature Table</h1>
<p>American tourists visiting Canada can use this handy temperature
table which converts from Fahrenheit to Celsius:
<br><br>

<table BORDER COLS=2 WIDTH="20%" >
<tr BGCOLOR="#FFFF00">
<th>Fahrenheit</th>
<th>Celsius</th>
</tr>

<% for (int i = 0; i <= 100; i += 10) { %>
<tr ALIGN=RIGHT BGCOLOR="#CCCCCC">
<td><%= i %></td>
<td><%= ((i - 32) * 5 / 9) %></td>
</tr>
<% } %>

</table>
<p><i>Created <%= new Date () %></i></p>

</body>
</html>
 