package com.pyrasol.ecm.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Servlet implementation class ConvertImageToPDF
 */
@WebServlet(asyncSupported = true, description = "Convert image to PDF", urlPatterns = { "/ConvertImageToPDF" })
@MultipartConfig
public class ConvertImageToPDF extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Document document = new Document();
		try {
			PdfWriter.getInstance(document, response.getOutputStream());
			document.open();
					
			InputStream is = request.getParts().iterator().next().getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1)
              buffer.write(data, 0, nRead);

            buffer.flush();
            Image image = Image.getInstance(buffer.toByteArray());
			document.setPageSize(image);
			document.newPage();
			image.setAbsolutePosition(0, 0);
			document.add(image);
			document.close();          
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
}
