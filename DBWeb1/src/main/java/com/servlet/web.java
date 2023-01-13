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

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		try {

			Class.forName("oracle.jdbc.driver.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "system", "system");
			Statement st = con.createStatement();

			ResultSet rs = st.executeQuery(
					"select DISTINCT(v.RPTGROUP) from RPTGROUPS g INNER JOIN rptviews v on v.RPTGROUP = g.name");

			out.write("<form method=\"GET\" action=\"web\"");
			out.write("<div style=\"display:flex;margin:1em;justify-content: space-around;\">");
			out.write("<div>");
			out.write("Report Group <select name=\"rptGroups\">");
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
			out.write("<input type=\"submit\" value=\"Refresh\" />");
			out.write("</div>");

			out.write("<div>");
			if(request.getParameter("rptGroups")!=null)
			{
			out.write("<input type=\"submit\" value=\"Send Email\" name=\"mail\"/><br>");
			out.write("<input type=\"button\" value=\"Export To Excel\"/><br>");
			out.write("<input type=\"button\" value=\"Export To PDF\"/>");
			}
			else
			{
				out.write("<input disabled type=\"submit\" value=\"Send Email\" name=\"mail\"/><br>");
				out.write("<input disabled type=\"button\" value=\"Export To Excel\"/><br>");
				out.write("<input disabled type=\"button\" value=\"Export To PDF\"/>");
			}
			out.write("</div>");
			out.write("</div>");
			out.write("</form>");

			HTML html = new HTML();
			String s = html.generateHTML(request.getParameter("rptGroups"));
			out.write(s);
			
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
