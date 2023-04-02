package com.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import com.model.rptGroups;
import com.model.rptViews;

/**
 * Servlet implementation class Excel
 */
@WebServlet("/Excel")
public class Excel extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Excel() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public static int getExcelColumnNumber(String column) {
        int result = 0;
        for (int i = 0; i < column.length(); i++) {
            result *= 26;
            result += column.charAt(i) - 'A'+1;
        }
        return result-1;
    }
    
    private static ArrayList<String> parse(String toParse){
    	String VALID_PATTERN = "[0-9]+|[A-Za-z]+";
    	
        ArrayList<String> chunks = new ArrayList<String>();
        toParse = toParse + "$"; //Added invalid character to force the last chunk to be chopped off
        int beginIndex = 0;
        int endIndex = 0;
        while(endIndex < toParse.length()){         
            while(toParse.substring(beginIndex, endIndex + 1).matches(VALID_PATTERN)){
                endIndex++;
            }
            if(beginIndex != endIndex){
                chunks.add(toParse.substring(beginIndex, endIndex));    
            } else {
                endIndex++;
            }  
            beginIndex = endIndex;
        }               
        return chunks;
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		JSONObject json = new JSONObject(request.getParameter("json")); 
		TreeSet<String> ts = new TreeSet();
		
		for(String s:json.keySet()) {
			ts.add(s);
		}
		try {
			ArrayList table = new ArrayList();
			table.addAll(ts);
	    	HTML h = new HTML();
	    	Class.forName("oracle.jdbc.driver.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "system", "system");
			Statement st = con.createStatement();

			List<rptGroups> groups = h.getRPTGroups(con);
			List<rptViews> views = h.getRPTViews(con);
			
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet =null;
			for (int i = 0; i < groups.size(); i++) 
			{			    	
				for (int j = 0; j < views.size(); j++) 
				{
					if (groups.get(i).getName().equals(views.get(j).getRptGroup()) && views.get(j).getRptGroup().equals(request.getParameter("group"))) {
						String Excel = views.get(j).getExcel();
						String tabName = Excel.split("!")[0];
						String index = Excel.split("!")[1];
						
						if(sheet == null) {
						sheet = workbook.createSheet(tabName.replace("\"", ""));
						}
						ArrayList<String> columns = parse(index);
				    
						JSONObject obj = json.getJSONObject(String.valueOf(table.get(j)));
						String[] tableData = obj.getString("table").split("[|]{2}");
						int row = Integer.parseInt(columns.get(1))-1;
						int col  = getExcelColumnNumber(columns.get(0).toUpperCase());
						
						// Header
						Row r1 = sheet.getRow(row);
						if(r1 == null) {
							r1 = sheet.createRow(row);
						}
						Cell c1 = r1.getCell(col);
						
						if(r1 != null) {
							c1 = r1.createCell(col);
						}
						if(obj.getString("header").contains("<I>")) {
							XSSFFont font1 = workbook.createFont();
							font1.setItalic(true);
							XSSFCellStyle style1 = workbook.createCellStyle();
							style1.setFont(font1);
							c1.setCellStyle(style1);
						}
						if(obj.getString("header").contains("<U>")) {
							XSSFFont font2 = workbook.createFont();
							font2.setUnderline(FontUnderline.SINGLE);
							XSSFCellStyle style2 = workbook.createCellStyle();
							style2.setFont(font2);
							c1.setCellStyle(style2);
						}
						if(obj.getString("header").contains("<B>")) {
							XSSFFont font3 = workbook.createFont();
							font3.setBold(true);
							XSSFCellStyle style3 = workbook.createCellStyle();
							style3.setFont(font3);
							c1.setCellStyle(style3);
						}
						c1.setCellValue(obj.getString("header").replaceAll("<[^>]*>", ""));
						row++;
						
						// Table body
						for(String data:tableData) {
							col  = getExcelColumnNumber(columns.get(0).toUpperCase());
							Row r = sheet.getRow(row);
							if(r == null) {
								r = sheet.createRow(row);
							}
							String[] column = data.split("~");
							for(int l=0;l<column.length;l++) {
								Cell c = r.getCell(col);
								
								if(r != null) {
									c = r.createCell(col);
								}
								c.setCellValue(column[l]);
								col++;
							}
							
							row++;							
						}
						
						// Footer
						col  = getExcelColumnNumber(columns.get(0).toUpperCase());
						Row r2 = sheet.getRow(row);
						if(r2 == null) {
							r2 = sheet.createRow(row);
						}
						Cell c2 = r2.getCell(col);
						
						if(r2 != null) {
							c2 = r2.createCell(col);
						}
						if(obj.getString("footer").contains("<B>")) {
							 XSSFFont font = workbook.createFont();
							 font.setBold(true);
							 XSSFCellStyle style = workbook.createCellStyle();
						     style.setFont(font);
						     c2.setCellStyle(style);
						}
						if(obj.getString("footer").contains("<I>")) {
							XSSFFont font = workbook.createFont();
							font.setItalic(true);
							XSSFCellStyle style = workbook.createCellStyle();
							style.setFont(font);
							c2.setCellStyle(style);
						}
						if(obj.getString("footer").contains("<U>")) {
							XSSFFont font = workbook.createFont();
							font.setUnderline(FontUnderline.SINGLE);
							XSSFCellStyle style = workbook.createCellStyle();
							style.setFont(font);
							c2.setCellStyle(style);
						}
						c2.setCellValue(obj.getString("footer").split(";")[0].replaceAll("<[^>]*>", ""));
						row++;
						
						r2 = sheet.getRow(row);
						r2 = sheet.createRow(row);
						c2 = r2.getCell(col);
						c2 = r2.createCell(col);
						c2.setCellValue(obj.getString("footer").split(";")[1]);
						row++;
				    	
					}
				}
			}
			
			// SQL Tab
			sheet = workbook.createSheet("SQL");
			int row =0;
			int col = 0;
			
			Row r2 = sheet.getRow(row);
			if(r2 == null) {
				r2 = sheet.createRow(row);
			}
			Cell c2 = r2.getCell(col);
			
			if(r2 != null) {
				c2 = r2.createCell(col);
			}
			// header
			c2.setCellValue("name");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("rptGroup");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("excel");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("sql");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("vars");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("vals");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("excelVars");
			col++;
			c2 = r2.createCell(col);
			c2.setCellValue("excelSQL");
			
			// body
			
			
			byte[] decodedBytes = Base64.getDecoder().decode(request.getParameter("excelSQL"));
			String decodedString = new String(decodedBytes);
			System.out.println("decoded = "+decodedString);
			JSONObject js = new JSONObject(decodedString);
			
			ArrayList al = new ArrayList();
			for(String s:js.keySet()) {
				al.add(s);
			}
			
			for (int i = 0; i < al.size(); i++) {
				row++;
				col = 0;
				r2 = sheet.getRow(row);
				r2 = sheet.createRow(row);
				
				JSONObject obj = js.getJSONObject(String.valueOf(al.get(i)));
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("name"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("rptGroup"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("excel"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("sql"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("vars"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getString("vals"));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getJSONArray("execVars").getString(i));
				col++;
				c2 = r2.createCell(col);
				c2.setCellValue(obj.getJSONArray(("execVals")).getString(i));
				
			}
			
			try (FileOutputStream outputStream = new FileOutputStream("Excel.xlsx")) {
	            workbook.write(outputStream);
	            
	            String filePath = "Excel.xlsx";
	            File downloadFile = new File(filePath);
	            FileInputStream inStream = new FileInputStream(downloadFile);
	            	             
	            // obtains ServletContext
	            ServletContext context = getServletContext();
	             
	            // gets MIME type of the file
	            String mimeType = context.getMimeType(filePath);
	            if (mimeType == null) {        
	                // set to binary type if MIME mapping not found
	                mimeType = "application/octet-stream";
	            }
	             
	            // modifies response
	            response.setContentType(mimeType);
	            response.setContentLength((int) downloadFile.length());
	             
	            // forces download
	            String headerKey = "Content-Disposition";
	            String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
	            response.setHeader(headerKey, headerValue);
	             
	            // obtains response's output stream
	            OutputStream outStream = response.getOutputStream();
	             
	            byte[] buffer = new byte[10000];
	            int bytesRead = -1;
	             
	            while ((bytesRead = inStream.read(buffer)) != -1) {
	                outStream.write(buffer, 0, bytesRead);
	            }
	             
	            inStream.close();
	            outStream.close();
	            
	        } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
	
		System.out.println("Excel.doPost()");
		
		
	}

}
