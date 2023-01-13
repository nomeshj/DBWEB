package com.operations;

import java.nio.file.Files;
import java.nio.file.Path;
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

import com.model.rptGroups;
import com.model.rptViews;

public class HTML {
static int columnsNumber = 0;
	
	private String recipients;
	private String emailSubject;
	
	public String getRecipients() {
		return recipients;
	}

	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public boolean tableExist(Connection con,String tableName) throws SQLException {
		boolean tExists = false;
		try (ResultSet rs = con.getMetaData().getTables(null, null, tableName, null)) {
			while (rs.next()) {
				String tName = rs.getString("TABLE_NAME");
				if (tName != null && tName.equalsIgnoreCase(tableName)) {
					tExists = true;
					break;
				}
			}
		}
		return tExists;
	}
	
	private String getTime() {
		String startTime = java.time.LocalDate.now().toString();
		Date date = new Date();
		SimpleDateFormat formatTime = new SimpleDateFormat("hh.mm aa");
		String time = formatTime.format(date);
		startTime = startTime.concat(" " + time);
		return startTime;
	}
	
	private String getViewRunsValues(String[] vars, List sqlVals) {
		
		StringBuilder vals = new StringBuilder();
		int length = sqlVals.size();
		for(int i=0;i<vars.length;i++)
		{
			if(i<length)
			{
				String replace = sqlVals.get(i).toString().replaceAll("'", "");
				vals.append(vars[i]+":"+replace);
			}else {
				vals.append(vars[i]+":"+"NULL");
			}
			
			if(i != vars.length-1)
			{
				vals.append("~");
			}
		}
		return vals.toString();
	}
	
	private List<rptGroups> getRPTGroups(Connection con) throws SQLException{
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("select * from rptGroups");

		List<rptGroups> groups = new ArrayList<rptGroups>();
		while (rs.next()) {
			rptGroups g = new rptGroups();
			g.setName(rs.getString(1));
			g.setDescription(rs.getString(2));
			g.setRecipients(rs.getString(3));
			g.setDestination(rs.getString(4));
			g.setTableAttributes(rs.getString(5));
			g.setCellAttributes(rs.getString(6));
			g.setHeader(rs.getString(7));
			g.setFooter(rs.getString(8));
			g.setEmilSubject(rs.getString(9));
			g.setVars(rs.getString(10));
			g.setVals(rs.getString(11));
			g.setTimeout(rs.getInt(12));
			g.setInsBy(rs.getString(13));
			//g.setInsOn(rs.getString(14));
			g.setUpdBy(rs.getString(15));
			//g.setUpOn(rs.getString(16));
			g.setHtmlTemplate(rs.getString(17));
			g.setExcelTemplate(rs.getString(18));
			g.setPdfTemplate(rs.getString(19));

			groups.add(g);
		}
		return groups;
	}
	
	private List<rptViews> getRPTViews(Connection con) throws SQLException{
		Statement st = con.createStatement();
		ResultSet rs1 = st.executeQuery("select * from rptViews");

		List<rptViews> views = new ArrayList<rptViews>();

		while (rs1.next()) {
			rptViews v = new rptViews();

			v.setName(rs1.getString(1));
			v.setRptGroup(rs1.getString(2));
			v.setHtmlRow(rs1.getInt(3));
			v.setHtmlCol(rs1.getInt(4));
			v.setExcel(rs1.getString(5));
			v.setPdf(rs1.getString(6));
			v.setActiveYN(rs1.getString(7));
			v.setHeader(rs1.getString(8));
			v.setFooter(rs1.getString(9));
			v.setSql(rs1.getString(10));
			v.setColHeader(rs1.getString(11));
			v.setVars(rs1.getString(12));
			v.setVals(rs1.getString(13));
			v.setTimeOut(rs1.getInt(14));
			v.setInsBy(rs1.getString(15));
			//v.setInsOn(rs1.getString(16));
			v.setUpdBy(rs1.getString(17));
			//v.setUpOn(rs1.getString(18));
		
			views.add(v);

		}
		return views;
	}
	
	public int getMaxRow(String rptGroup,Connection con) throws SQLException {
		int rows=0;
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("select max(htmlRow) from rptViews where rptGroup = '"+ rptGroup + "'");
		if (rs.next()) {
			rows = rs.getInt(1);
		}
		return rows;
	}
	
	public int getMaxCol(String rptGroup,Connection con) throws SQLException {
		int cols=0;
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("select max(htmlCol) from rptViews where rptGroup = '"+ rptGroup + "'");
		if (rs.next()) {
			cols = rs.getInt(1);
		}
		return cols;
	}
	
	
	public void viewsInsertMetadata(Connection con,boolean viewError,String[] vars, List sqlVals,String viewName,String viewStartTime,String viewEndTime, String viewErrorValue) throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate("ALTER SESSION SET NLS_DATE_FORMAT = 'DD-MM-RR HH:MI AM'");
		con.commit();
		if (tableExist(con,"RPTVIEWSRUN")) {
			String viewStatus;
			if (viewError) {
				viewStatus = "Fail";
			} else {
				viewStatus = "Done";
			}

			String viewValues = getViewRunsValues(vars,sqlVals);
			String insertSQL = "INSERT INTO rptViewsRun VALUES('" + viewName + "','" + viewStatus+ "',TO_DATE('" + viewStartTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),TO_DATE('" + viewEndTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),'" + viewValues + "','" + viewErrorValue + "')";
			st.execute(insertSQL);
		}
		else {
			st.executeUpdate("CREATE TABLE rptViewsRun(name varchar2(200), status varchar2(200),startTime Date,endTime Date,vals varchar2(200),err varchar2(200))");
			
			String viewStatus;
			if (viewError) {
				viewStatus = "Fail";
			} else {
				viewStatus = "Done";
			}
			String viewValues = getViewRunsValues(vars,sqlVals);
			String insertSQL = "INSERT INTO rptViewsRun VALUES('" + viewName + "','" + viewStatus+ "',TO_DATE('" + viewStartTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),TO_DATE('" + viewEndTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),'" + viewValues + "','" + viewErrorValue + "')";
	
			st.execute(insertSQL);
		}
	}
	
	public void groupsInsertMetadata(Connection con,boolean errorFlag,String groupName,String startTime,String endTime,String values,String error) throws SQLException {
		Statement st = con.createStatement();
		String status="";
		st.executeUpdate("ALTER SESSION SET NLS_DATE_FORMAT = 'DD-MM-RR HH:MI AM'");
		con.commit();
		if (tableExist(con,"RPTGROUPSRUN")) {
			if (errorFlag) {
				status = "Fail";
			} else {
				status = "Done";
			}
			String insertSQL = "INSERT INTO rptGroupsRun VALUES('" + groupName + "','" + status+ "',TO_DATE('" + startTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),TO_DATE('" + endTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),'" + values + "','" + error + "')";
			
			st.execute(insertSQL);
		} else {
			st.executeUpdate("CREATE TABLE rptGroupsRun(name varchar2(200), status varchar2(200),startTime Date,endTime Date,vals varchar2(200),err varchar2(200))");

			if (errorFlag) {
				status = "Fail";
			} else {
				status = "Done";
			}
			String insertSQL = "INSERT INTO rptGroupsRun VALUES('" + groupName + "','" + status+ "',TO_DATE('" + startTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),TO_DATE('" + endTime + "','YYYY/MM/DD HH:MI AM','NLS_DATE_LANGUAGE = AMERICAN'),'" + values + "','" + error + "')";
			st.execute(insertSQL);
		}
	}
	
	public String formatMillis(long millis)
	{
		String hms = String.format("%02d:%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
				TimeUnit.MILLISECONDS.toMinutes(millis)
						- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
				TimeUnit.MILLISECONDS.toSeconds(millis)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
				TimeUnit.MILLISECONDS.toMillis(millis)
						- TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
		
		return hms;
	}
	
	
	public String generateHTML(String group) throws Exception {
		
		// Variables 
		int tableCount=0;
		String template = null;
		boolean templateFlag = false;
		String status = "";
		String error = null;
		String values = null;
		String headerStyle = "";
		String HTML = "";
				
		boolean errorFlag = false;
		
		Class.forName("oracle.jdbc.driver.OracleDriver");
		Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "system", "system");
		Statement st = con.createStatement();

		List<rptGroups> groups = getRPTGroups(con);
		List<rptViews> views = getRPTViews(con);
		
		Path fileName;
		String templateContent = null;
		int maxrows = 0;
		int maxcols = 0;
		
		StringBuilder tableContent = new StringBuilder("");
		for (int i = 0; i < groups.size(); i++) 
		{	
			int row = 1;
			int columns = 0;
			int count = 0;
			error = null;
			boolean flag = true;
			String startTime = getTime();
			tableCount=0;
			
			
			if(groups.get(i).getHtmlTemplate()!=null)
			{
				fileName = Path.of(groups.get(i).getHtmlTemplate());
			    templateContent = Files.readString(fileName);
			}
			
			for (int j = 0; j < views.size(); j++) 
			{
				if (groups.get(i).getName().equals(views.get(j).getRptGroup()) && views.get(j).getRptGroup().equals(group)) {
					
					tableCount++;
					
					boolean viewError = false;
					String viewErrorValue = null;
					String viewStartTime = getTime();
					Instant start = Instant.now();
					
					setRecipients(groups.get(i).getRecipients());
					setEmailSubject(groups.get(i).getEmilSubject());
					
					if(groups.get(i).getHtmlTemplate() == null)
					{
					if (flag) {
						tableContent.append("<table " + groups.get(i).getTableAttributes() + ">"); // TOP table

						maxrows = getMaxRow(views.get(j).getRptGroup(), con);
						maxcols = getMaxCol(views.get(j).getRptGroup(), con);

						String header = groups.get(i).getCellAttributes();
						String head[] = header.split("~");
						boolean flagStyle = false;
						
						for (String h : head) {
							if (h.contains("header")) {
								headerStyle = h.replace("header:", "");
								flagStyle = true;
							}
						}
						if (flagStyle && !header.contains("~")) {
							headerStyle = header;
						}

						tableContent.append("<tr>");
						tableContent.append("<td " + headerStyle + "></td>");
						
						for (int c = 1; c <= maxcols; c++) {
							tableContent.append("<th " + headerStyle + " scope=\"col\">COLUMN" + c + "</th>");
						}
						
						tableContent.append("</tr>");
						tableContent.append("<tr>");
						tableContent.append("<th " + headerStyle + " scope=\"row\">ROW" + row + "</th>");
					}
			
					while (views.get(j).getHtmlRow() != row) {
				
						row++;
						columns = 0;

						tableContent.append("</tr>");
						tableContent.append("<tr>");
						tableContent.append("<th " + headerStyle + " scope=\"row\">ROW" + row + "</th>");

					}
					
					if (views.get(j).getHtmlRow() == row) {
						columns++;
					}
					
					// check for colspan 
					while(views.get(j).getHtmlCol() != columns)
					{
						tableContent.append("<td></td>");
						columns++;
					}

					tableContent.append("<td>");
					}
					tableContent.append("<div><center>" + views.get(j).getHeader() + "</center></div>");
					tableContent.append("<center>");
					tableContent.append("<table border='1' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>");
					tableContent.append("<tr>");

					String replaced = views.get(j).getColHeader().replace("~", "");
					String[] headers = replaced.split("</span>");

					for (String col : headers) {
						col = col.replace("<span", "");
						tableContent.append("<th" + col);
						// tableContent.append(col);
						tableContent.append("</th>");
					}
					
					tableContent.append("</tr>");

					String SQL = views.get(j).getSql();
					String[] vars = views.get(j).getVars().split("~");
					String[] vals = views.get(j).getVals().split("~");
					List sqlVals = new ArrayList();
					
					for (String val : vals) {
						
						val = val.replaceAll("\"", "'");
						System.out.println("val = " + val);
						
						if (val.contains("select")) {
							try {
								ResultSet rs5 = st.executeQuery(val);
							if (rs5.next()) {
								viewError = false;
								sqlVals.add(rs5.getString(1));
								}
							}
							catch(Exception e) {
								sqlVals.add(val);
								viewErrorValue = e.getMessage();
								viewError = true;
							}
						} else {
							sqlVals.add(val);
						}
					}

					for (int k = 0; k < vals.length; k++) {
						SQL = SQL.replaceAll("\\{" + vars[k] + "\\}", sqlVals.get(k).toString());

					}
					
					SQL = SQL.replaceAll("\"", "'");

					try {
						ResultSet rs6 = st.executeQuery(SQL);
						ResultSetMetaData rsmd = rs6.getMetaData();

						columnsNumber = rsmd.getColumnCount();
						while (rs6.next()) {
							tableContent.append("<tr>");
							for (int p = 1; p <= columnsNumber; p++) {
								tableContent.append("<td>");
								tableContent.append(rs6.getString(p));
								tableContent.append("</td>");
							}
							tableContent.append("</tr>");
						}
					} catch (Exception e) {
						tableContent.append("<tr>");
						tableContent.append("<td colspan=\"" + columnsNumber + "\">");
						tableContent.append(e.getMessage());
						tableContent.append("</td>");
						tableContent.append("</tr>");
						errorFlag = true;
						error = e.getMessage();
					}

					flag = false;
					
					tableContent.append("</table>");
					tableContent.append("</center>");
					tableContent.append("<div><center>" + views.get(j).getFooter() + "</center></div>");

					Instant finish = Instant.now();
					long seconds = Duration.between(start, finish).toMillis(); // in millis

					String hms = formatMillis(seconds);
					tableContent.append("<div><center>" + hms + "</center></div>");
					
					if(groups.get(i).getHtmlTemplate() == null)
					{
					tableContent.append("</td>");

					if (columns == maxcols ) {
						tableContent.append("</tr>");
					}
					}
					String viewEndTime = getTime();
					
					// insert data in RPTViewsRun table
					viewsInsertMetadata(con, viewError, vars, sqlVals, views.get(j).getName() , viewStartTime, viewEndTime, viewErrorValue);
					if(groups.get(i).getHtmlTemplate() != null)
					{
						templateFlag = true;		
				
			        String format = "{{"+views.get(j).getHtmlRow()+"~"+views.get(j).getHtmlCol()+"}}";
			        templateContent = templateContent.replace(format, tableContent);

			        template = templateContent;
			        tableContent.delete(0,tableContent.length());
					}// end of htmltemplate  block
				} else {
					count++;
					if (count == views.size()) {
						error = "Collection Not Found";
						errorFlag = true;
					}
				}
				
			}// end 2 for loop
			
			if(groups.get(i).getHtmlTemplate() == null)
			{
			if(tableCount !=0)
			{
				tableContent.append("</table>");
			}
			}
			
			String endTime = getTime();
			
			// insert data in RPTGroupsRun table
			groupsInsertMetadata(con, errorFlag, groups.get(i).getName(), startTime, endTime, values, error);
		
		} // end 1 for loop
		if(!templateFlag)
		{
			HTML = tableContent.toString();
		}
		else
		{
			HTML = template;
		}
	
		return HTML;
	}
}
