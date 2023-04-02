package com.servlet;

import java.io.IOException;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.model.rptGroups;
import com.model.rptViews;
import com.operations.Email;
import com.operations.HTML;

@WebServlet("/web")
public class web extends HttpServlet {

	public web() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HTML html = new HTML();
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		try {

			Class.forName("oracle.jdbc.driver.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "system", "system");
			Statement st = con.createStatement();

			ResultSet rs = st.executeQuery(
					"select DISTINCT(v.RPTGROUP) from RPTGROUPS g INNER JOIN rptviews v on v.RPTGROUP = g.name");

			out.write("<div style=\"display:flex;margin:1em;justify-content: space-around;\">");
			out.write("<div>");
			out.write("Report Group <select name=\"rptGroups\" id='rptGroups'>");
			if (request.getParameter("rptGroups") == null) {
				out.write("<option value=\"none\" selected disabled hidden>Select an Option</option>");
			}
			while (rs.next()) {
				if (request.getParameter("rptGroups") != null) {
					if (request.getParameter("rptGroups").equals(rs.getString(1))) {
						out.write(
								"<option selected value=\"" + rs.getString(1) + "\">" + rs.getString(1) + "</option>");
					} else {
						out.write("<option value=\"" + rs.getString(1) + "\">" + rs.getString(1) + "</option>");
					}
				} else {
					out.write("<option value=\"" + rs.getString(1) + "\">" + rs.getString(1) + "</option>");
				}
			}
			out.write("</select>");
			out.write("</div>");

			out.write("<div>");
			out.write("<input type=\"submit\" id='refresh'  value=\"Refresh\" />");
			out.write("</div>");

			out.write("<div>");
		
				out.write("<input type='hidden' name='excel' id='excelData'/>");
				out.write("<input disabled type=\"submit\" id='email'  value=\"Send Email\" name=\"mail\"/><br>");
				out.write("<input disabled type=\"button\" id='excel' value=\"Export To Excel\"/><br>");
				out.write("<input disabled type=\"button\" id='pdf' value=\"Export To PDF\"/>");
			
			out.write("</div>");
			out.write("</div>");

			out.write("<div id='table'>");
			out.write("</div>");
			String s = html.generateHTML(request.getParameter("rptGroups"));
			
			out.write("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.6.3/jquery.min.js\"></script>\r\n"
					// Refresh
					+ "<script>\r\n"
					+ "$(document).ready(function(){\r\n"
					+ "	$('#refresh').click(function(){\r\n"
					+ "const value = $(\"#rptGroups\").val();"
					+ "console.log(value);"
					+ "		\r\n"
					+ "		$.ajax({\r\n"
					+ "			url:\"Refresh\",\r\n"
					+ "			method:\"GET\",\r\n"
					+ "			data:{group:value},"
					+ "			success:function(res){\r\n"
					+ "				$('#table').html(res.tableHTML);"
					+ "				$('#excelData').val(JSON.stringify(res.tableJSON));"
					+ "				$(\"#email\").attr(\"disabled\", false);"
					+ "				$(\"#excel\").attr(\"disabled\", false);"
					+ "				$(\"#pdf\").attr(\"disabled\", false);"
					+ "			}\r\n"
					+ "		});\r\n"
					+ "		\r\n"
					+ "	});\r\n"
					+ "})\r\n"
					
					// Excel
					+ "</script>"
					+ "<script>\r\n"
					+ "$(document).ready(function(){\r\n"
					+ "	$('#excel').click(function(){\r\n"
					+ "const value = $(\"#excelData\").val();"
					+ "const groupName = $(\"#rptGroups\").val();"
					+ "const excel = $(\"#excelSQL\").val();"
					+ "console.log(value);"
					+ "		\r\n"
					+ " var xhr = new XMLHttpRequest();\r\n"
					+ " xhr.open('POST', \"/DBWeb1/Excel\", true);\r\n"
					+ " xhr.responseType = 'arraybuffer';\r\n"
					+ " xhr.onload = function () {\r\n"
					+ "  if (this.status === 200) {\r\n"
					+ "   var filename = \"\";\r\n"
					+ "   var disposition = xhr.getResponseHeader('Content-Disposition');\r\n"
					+ "   if (disposition && disposition.indexOf('attachment') !== -1) {\r\n"
					+ "    var filenameRegex = /filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)/;\r\n"
					+ "    var matches = filenameRegex.exec(disposition);\r\n"
					+ "    if (matches != null && matches[1]) {\r\n"
					+ "     filename = matches[1].replace(/['\"]/g, '');\r\n"
					+ "    }\r\n"
					+ "   }\r\n"
					+ "   var type = xhr.getResponseHeader('Content-Type');\r\n"
					+ "var blob = typeof File === 'function'\r\n"
					+ "    ? new File([this.response], filename, { type: type })\r\n"
					+ "    : new Blob([this.response], { type: type });\r\n"
					+ "   if (typeof window.navigator.msSaveBlob !== 'undefined') {\r\n"
					+ "    // IE workaround for \"HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. \r\n"
					+ "    // These URLs will no longer resolve as the data backing the URL has been freed.\"\r\n"
					+ "    window.navigator.msSaveBlob(blob, filename);\r\n"
					+ "   } else {\r\n"
					+ "    var URL = window.URL || window.webkitURL;\r\n"
					+ "    var downloadUrl = URL.createObjectURL(blob);\r\n"
					+ "if (filename) {\r\n"
					+ "     // use HTML5 a[download] attribute to specify filename\r\n"
					+ "     var a = document.createElement(\"a\");\r\n"
					+ "     // safari doesn't support this yet\r\n"
					+ "     if (typeof a.download === 'undefined') {\r\n"
					+ "      window.location = downloadUrl;\r\n"
					+ "     } else {\r\n"
					+ "      a.href = downloadUrl;\r\n"
					+ "      a.download = filename;\r\n"
					+ "      document.body.appendChild(a);\r\n"
					+ "      a.click();\r\n"
					+ "     }\r\n"
					+ "    } else {\r\n"
					+ "     window.location = downloadUrl;\r\n"
					+ "    }\r\n"
					+ "    setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100); // cleanup\r\n"
					+ "   }\r\n"
					+ "  }\r\n"
					+ " };\r\n"
					+ " xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');\r\n"
					+ " xhr.send($.param({\r\n"
					+ "  json:value,group:groupName,excelSQL:excel\r\n"
					+ " }));"
//					+ "		$.ajax({\r\n"
//					+ "			url:\"Excel\",\r\n"
//					+ "			dataType: \"text\","
//					+ "			method:\"GET\",\r\n"
////					+ "			data:{group:$('#rptGroups').value},"
//+ "			data:{json:value,group:groupName,excelSQL:excel},"
//+ "			success:function(res){\r\n"
//+ "					console.log(res);"
//+ "			}\r\n"
//+ "		});\r\n"
+ "		\r\n"
+ "	});\r\n"
+ "})\r\n"
+ "</script>"
					
					
					);
			
			
			
			if(request.getParameter("mail") != null)
			{
			if(request.getParameter("rptGroups")!=null && request.getParameter("mail").equals("Send Email"))
			{
				Email e = new Email(s,html.getRecipients(),html.getEmailSubject());
				boolean flag = e.send();
				if(flag)
				{
						out.println("<script type=\"text/javascript\">");
					   out.println("alert('Email Sent Successfully');");
					   out.println("</script>");
				}
				else {
					out.println("<script type=\"text/javascript\">");
					   out.println("alert('Email not sent');");
					   out.println("</script>");
				}
			}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
