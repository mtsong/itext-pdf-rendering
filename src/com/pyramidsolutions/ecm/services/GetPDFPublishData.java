package com.pyramidsolutions.ecm.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfWriter;

@WebServlet(asyncSupported = true, description = "Merge received data into one PDF", urlPatterns = { "/GetPDFPublishData" })
@MultipartConfig
public class GetPDFPublishData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		JSONObject requestJSON = JSONObject.parse(request.getParameter("requestObjectJSON"));
		JSONObject contentItems = (JSONObject) requestJSON.get("contentItems");
		JSONArray pages = (JSONArray) requestJSON.get("pages");

		Document document = new Document(PageSize.LETTER);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			PdfSmartCopy copy = new PdfSmartCopy(document, baos);
			document.open();
			Iterator<Map.Entry<String, JSONObject>> it = contentItems.entrySet().iterator();
			Map<String, PdfReader> sourcePDFs = new HashMap<String, PdfReader>();
			while (it.hasNext()) {
				Map.Entry<String, JSONObject> contentItem = (Map.Entry<String, JSONObject>)it.next();
				String id = contentItem.getKey();
				JSONObject item = contentItem.getValue();
				String password = "";
				if (item.get("password") != null)
					password = item.get("password").toString();
				InputStream is = request.getPart(id).getInputStream();
				if (item.get("mimetype").toString().equals("application/pdf") || item.get("mimetype").toString().equals("image/tiff")) {
					if (!password.isEmpty())
						sourcePDFs.put(id, new PdfReader(is, password.getBytes()));
					else
						sourcePDFs.put(id, new PdfReader(is));
				} else {
					Document tempImageDoc = new Document();
					ByteArrayOutputStream tempImageDocOutputStream = new ByteArrayOutputStream();
					PdfWriter.getInstance(tempImageDoc, tempImageDocOutputStream);
					
					tempImageDoc.open();
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[16384];

					while ((nRead = is.read(data, 0, data.length)) != -1)
					  buffer.write(data, 0, nRead);

					buffer.flush();
					Image image = Image.getInstance(buffer.toByteArray());
					tempImageDoc.setPageSize(image);
					tempImageDoc.newPage();
					image.setAbsolutePosition(0, 0);
					tempImageDoc.add(image);
					tempImageDoc.close();
					sourcePDFs.put(id, new PdfReader(tempImageDocOutputStream.toByteArray()));
				}
				is.close();
				it.remove();
			}
						
			for (Object item : pages) {
				JSONObject page = (JSONObject) item;
				PdfReader reader = sourcePDFs.get(page.get("id"));
				int pageNum = Integer.parseInt(page.get("pageNum").toString());
				int rotation = Integer.parseInt(page.get("rotation").toString());
				PdfDictionary pdfDictPage = reader.getPageN(pageNum);
				if (pdfDictPage.getAsNumber(PdfName.ROTATE) != null)
					rotation = (rotation + pdfDictPage.getAsNumber(PdfName.ROTATE).intValue() % 360);
				pdfDictPage.put(PdfName.ROTATE, new PdfNumber(rotation));
				copy.addPageDictEntry(PdfName.ROTATE, pdfDictPage.getAsNumber(PdfName.ROTATE));
				copy.addPage(copy.getImportedPage(reader, pageNum));
			}
			
			for (PdfReader reader : sourcePDFs.values()) {
				copy.freeReader(reader);
				reader.close();
			}				
		    document.close();
		    copy.close();
            String encodedData = DatatypeConverter.printBase64Binary(baos.toByteArray());
            response.getWriter().write(encodedData);
            response.getWriter().flush();
            response.getWriter().close();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
}