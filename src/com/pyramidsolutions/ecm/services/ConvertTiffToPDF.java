package com.pyramidsolutions.ecm.services;

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
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;

/**
 * Servlet implementation class ConvertTiffToPDF
 */
@WebServlet(asyncSupported = true, description = "Converts Tiff to PDF for Rendering", urlPatterns = { "/ConvertTiffToPDF" })
@MultipartConfig
public class ConvertTiffToPDF extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Document document = new Document();
		try {
			PdfWriter.getInstance(document, response.getOutputStream());
			document.open();
					
			InputStream is = request.getPart("tiff").getInputStream();
			RandomAccessSourceFactory ras = new RandomAccessSourceFactory();
			RandomAccessFileOrArray tiff = new RandomAccessFileOrArray(ras.createSource(is));
			int numPages = TiffImage.getNumberOfPages(tiff);
			Image image;
			for (int i = 1; i <= numPages; ++i) {
				image = TiffImage.getTiffImage(tiff, i);
				document.setPageSize(image);
				document.newPage();
				image.setAbsolutePosition(0, 0);
				document.add(image);
			}
			document.close();          
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
}