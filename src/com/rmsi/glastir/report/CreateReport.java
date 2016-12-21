/* ----------------------------------------------------------------------
 * Copyright (c) 2011 by RMSI.
 * All Rights Reserved
 *
 * Permission to use this program and its related files is at the
 * discretion of RMSI Pvt Ltd.
 *
 * The licensee of RMSI Software agrees that:
 *    - Redistribution in whole or in part is not permitted.
 *    - Modification of source is not permitted.
 *    - Use of the source in whole or in part outside of RMSI is not
 *      permitted.
 *
 * THIS SOFTWARE IS PROVIDED ''AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL RMSI OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ----------------------------------------------------------------------
 */
package com.rmsi.glastir.report;

import gistoolkit.display.Converter;
import gistoolkit.features.Envelope;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import net.opengis.context.DimensionType;
import net.opengis.context.FormatListType;
import net.opengis.context.FormatType;
import net.opengis.context.LayerType;
import net.opengis.context.ViewContextType;

import org.apache.log4j.Logger;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.elements.GridItem;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

@WebServlet("/create")
public class CreateReport extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private IReportEngine birtReportEngine = null;
	protected static Logger logger = Logger.getLogger(CreateReport.class);

	private String outputUrl = null;
	private String mapFileName = null;

	private String rptFilePath = null;
	//private String bbox = "";
	String pdfpath = null;
	int max_print_request = 2;
	private final String PRINT_COUNT = "request_count";
	String crn = null;
	Converter convert = null;
	private Properties prop = null;
	//private String watermarkImg="watermark.png";
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CreateReport() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Initialization of the servlet.
	 * 
	 * 
	 * @throws ServletException
	 *             if an error occure
	 */

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = getServletContext();

		pdfpath = context.getInitParameter("pdf-save-path");
		logger.debug("***************** PDF Path ************************* "
				+ pdfpath);
		
		max_print_request = Integer.parseInt(context.getInitParameter("print-per-session"));
		logger.debug("*************** Max Print Request **************" + max_print_request);

		rptFilePath = context.getRealPath("/") + "reports\\";
		logger.debug("REPORT FILE PATH: " + rptFilePath);
		
		//try{
			String path = getServletContext().getRealPath("/") + "messages\\";
			logger.debug("Message path: " + path);
			
			java.io.File fl = new java.io.File(path);
			prop = new Properties();
			try {
			  URL resourceURL = fl.toURI().toURL();
			  URLClassLoader urlLoader = new URLClassLoader(new java.net.URL[]{resourceURL});
				prop.load(urlLoader.getResourceAsStream("messages.properties"));
			 // urlLoader.close();
			} catch (Exception e) {
			   e.printStackTrace();                     
			}

			
		/*}catch(MalformedURLException ex){
			logger.error(ex);
		}*/

	}

	/**
	 * Destruction of the servlet.
	 */
	public void destroy() {
		super.destroy();

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String id = "";
		if(request.getSession() == null){
			 id = request.getSession(true).getId();
		}else{
			id = request.getSession().getId();
		}
		logger.debug("Session id: " + id);
		
		int count = 0;
		logger.debug("Print request count: " + request.getSession().getAttribute(PRINT_COUNT));
		if(request.getSession().getAttribute(PRINT_COUNT) != null){
			count = Integer.parseInt((String)request.getSession().getAttribute(PRINT_COUNT));
			
			if(count >= max_print_request){
				logger.error("Max print request count for the session reached. Unable to carry out print request. Aborting !!!");
				response.getOutputStream().println(prop.getProperty("msg_printcount_exceed"));
				return;
			}else{
				request.getSession().setAttribute(PRINT_COUNT, String.valueOf(++count));
			}
		}else{
			logger.debug("No print count found: " + count);
			request.getSession().setAttribute(PRINT_COUNT, "1");
		}
		
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
	
		logger.debug("========================================");
		crn = request.getParameter("crn");
		logger.debug("CRN: " + crn);
		
		String outputFilePath = null;
		ArrayList<String> imagesArr = new ArrayList<String>();
		
		String title = request.getParameter("title");
		logger.debug("Title: " + title);

		//String description = request.getParameter("desc");
		//logger.debug("Desc: " + description);

		String papersize = request.getParameter("papersize");
		logger.debug("Size: " + papersize);

		String orientation = request.getParameter("orientation");
		logger.debug("Orientation: " + orientation);

		String wmc = request.getParameter("wmc");
		logger.debug("WMC: " + wmc);
		
		String lang = request.getParameter("lang");
		logger.debug("Lang: " + lang);
		
		logger.debug("=============================================");

		StringReader wmc_text = new StringReader(wmc);
		// FileWriter f = new FileWriter("F:/softwares/OGC/schemas/WMC.xml");
		// f.write(wmc);
		// f.flush();
		// f.close();

		// StringReader wmc_text = new StringReader(getContents(new File("F:/softwares/OGC/schemas/WMC.xml")));

		String legendImgUrl = "";
		String layerImgUrl = "";

		int imgWidth = 0;
		int imgHeight = 0;
		String margeImgUrl = "";
		String margeLegendUrl = "";

		File file = new File(request.getRealPath("") + "/images");
		boolean exists = file.exists();
		if (!exists) {

			boolean success = (new File(request.getRealPath("") + "/images"))
					.mkdir();

		}

		outputFilePath = request.getRealPath("") + "/images";
		outputUrl = request.getScheme() + "://" + request.getServerName() + ":"
				+ Integer.toString(request.getServerPort())
				+ request.getContextPath() + "/images/";

		ServletContext sc = request.getSession().getServletContext();
		this.birtReportEngine = BirtEngine.getBirtEngine(sc);

		IReportRunnable design;
		//FileOutputStream fos = null;

		try {

			JAXBContext context = JAXBContext
					.newInstance(ViewContextType.class);

			Unmarshaller unmarshaller = context.createUnmarshaller();

			javax.xml.bind.JAXBElement object = (javax.xml.bind.JAXBElement) unmarshaller
					.unmarshal(wmc_text);

			ViewContextType ctx = (ViewContextType) object.getValue();

			String srs = ctx.getGeneral().getBoundingBox().getSRS();

			logger.debug("SRS: " + srs);

			double bboxMinx = ctx.getGeneral().getBoundingBox().getMinx()
					.doubleValue();
			double bboxMiny = ctx.getGeneral().getBoundingBox().getMiny()
					.doubleValue();
			double bboxMaxx = ctx.getGeneral().getBoundingBox().getMaxx()
					.doubleValue();
			double bboxMaxy = ctx.getGeneral().getBoundingBox().getMaxy()
					.doubleValue();

			logger.debug("BBOX: " + bboxMinx + "," + bboxMiny + "," + bboxMaxx
					+ "," + bboxMaxy);

			// Open report design
			String reportName = "print-tmpl-" + papersize.toLowerCase() + "-"
					+ orientation.toLowerCase() + "-" + lang.toLowerCase()+ ".rptdesign";
						
			
			logger.debug("Report template " + reportName);

			design = birtReportEngine
					.openReportDesign(rptFilePath + reportName);
			
			/**********************For Image ****************************/
			/*DesignElementHandle m = design.getDesignHandle().getDesignHandle()
					.findElement("mapimage");
			ImageItem item = (ImageItem) m.getElement();*/
			
			/**********************end****************************/
			
			/**********************For grid****************************/
			DesignElementHandle m = design.getDesignHandle().getDesignHandle()
					.findElement("mainGrid");
			GridItem item=(GridItem) m.getElement();
			/**********************end***************************/
			
			imgWidth = (int) item.handle(null).getWidth().getMeasure();
			imgHeight = (int) item.handle(null).getHeight().getMeasure();

			logger.debug("Img Width: " + imgWidth);
			logger.debug("Img Height: " + imgHeight);

			Envelope inScreenEnvelope = new Envelope(0, 0, imgWidth, imgHeight);
			Envelope inWorldEnvelope = new Envelope(bboxMinx, bboxMiny,
					bboxMaxx, bboxMaxy);
			convert = new Converter(inScreenEnvelope, inWorldEnvelope, true);
			Envelope newEnv = convert.getWorldEnvelope();
			String bbox = newEnv.getMinX() + "," + newEnv.getMinY() + ","
					+ newEnv.getMaxX() + "," + newEnv.getMaxY();
			
			logger.debug("Converted Envelope: " + bbox);

			List layerList = ctx.getLayerList().getLayer();
			Iterator layerIter = layerList.iterator();
			//ArrayList<String> imagesArr = new ArrayList<String>();
			
			String imgFormat = "";
			while (layerIter.hasNext()) {
				float opacity = 1;
				imgFormat = "image/png";
				
				/*Iterate layers to get Format Type, only pick the the first format from the FormatList*/
				LayerType ctxLayer = (LayerType) layerIter.next();
				FormatListType formatTypes = ctxLayer.getFormatList();
				List<FormatType>formatType =  formatTypes.getFormat();
				//System.out.println(" #### Length of format types: " + formatType.size());
				if(formatType.size() > 0){
					imgFormat = formatType.get(0).getValue();
				}
				
				if (!ctxLayer.isHidden()) {
					//System.out.println("layer: " + ctxLayer.getName());

					if (ctxLayer.getExtension() != null) {
						ElementNSImpl extElem = (ElementNSImpl) ctxLayer
								.getExtension().getAny();
						//System.out.println("-----Check extn: " + extElem);

						if (extElem.getNodeName().equalsIgnoreCase("env:opacity")) {
							logger.debug("**** layer opacity **** : "
									+ extElem.getTextContent());
							
							String lyrOpacity = extElem.getTextContent();

							if (lyrOpacity != null) {								
								opacity = Float.parseFloat(lyrOpacity);
							}
						}
					}
					logger.debug("-----Layer Name: " + ctxLayer.getName() + 
							" Opacity Val: "+ opacity + 
							" Format Type: " + imgFormat);
					
					String ctxLayerName = encodeURI(ctxLayer.getName());
					String ctxLayerFormat = imgFormat; //"image/gif";
					// ************** Need to comment out once Envitia WMS
					// transparency is fixed
					// ctxLayer.getFormatList()
					// .getFormat().get(0).getValue();
					// **********************

					String ctxLayerStyle = null;
					if (ctxLayer.getStyleList() != null) {
						ctxLayerStyle = encodeURI(ctxLayer.getStyleList()
								.getStyle().get(0).getName());
						logger.debug("layer style: " + ctxLayerStyle);
					}
					String wmsURL = ctxLayer.getServer().getOnlineResource()
							.getHref();
					if (!wmsURL.endsWith("?"))
						wmsURL = wmsURL + "?";

					if (ctxLayerStyle == null) {

						layerImgUrl = wmsURL
								+ "request=GetMap&transparent=true&format="
								+ ctxLayerFormat + "&srs=" + srs + "&layers="
								+ ctxLayerName;
						

					} else {
						layerImgUrl = wmsURL
								+ "request=GetMap&transparent=true&format="
								+ ctxLayerFormat + "&styles=" + ctxLayerStyle
								+ "&srs=" + srs + "&layers=" + ctxLayerName;
					}

					if (ctxLayer.getDimensionList() != null) {
						List<DimensionType> dimTypeList = ctxLayer
								.getDimensionList().getDimension();
						Iterator<DimensionType> dimTypeListIter = dimTypeList
								.iterator();
						while (dimTypeListIter.hasNext()) {
							DimensionType dimType = dimTypeListIter.next();
							String dimName = dimType.getName();
							String dimValue = dimType.getUserValue();
							logger.debug("+++ Dimension Name +++ " + dimName);
							logger.debug("+++ Dimension Value +++ " + dimValue);
							layerImgUrl = layerImgUrl + "&" + dimName + "="
									+ dimValue;
						}

					}
					
					System.out.println("Output Path: " + outputFilePath);
					synchronized(this){
						imagesArr.add(getWMSImage(layerImgUrl, imgWidth, imgHeight,
							outputFilePath, opacity, ctxLayerFormat, bbox));
					}

				}

			}

			String scale = calculateScale(imgWidth, imgHeight, newEnv);

			synchronized(this){
				margeImgUrl = mergeImages(imagesArr, imgWidth, imgHeight,
					outputFilePath, imgFormat);
			}
			

			// create task to run and render report
			IRunAndRenderTask task = birtReportEngine
					.createRunAndRenderTask(design);

			// set output options
			
			HTMLRenderOption options = new HTMLRenderOption();
			// set the image handler to a HTMLServerImageHandler if you plan on
			// using the base image url.
			options.setImageHandler(new HTMLServerImageHandler());

			options.setOutputFormat(HTMLRenderOption.OUTPUT_FORMAT_PDF);

			// options.setOutputStream(response.getOutputStream());
			SimpleDateFormat formatter = new SimpleDateFormat(
					"dd.MM.yyyy_hh.mm.ss");
			Date day = new Date();

			String pdfName = formatter.format(day)+ ".pdf";
			String pdfOutputFile = pdfpath + crn + "_" + pdfName;
			
			
			logger.debug("PDF Output file " + pdfOutputFile);
			//fos = new FileOutputStream(pdfOutputFile);
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
			response.setHeader("Pragma", "public");
			response.setHeader("Content-Disposition", "attachment;filename=" + pdfName);
			options.setOutputStream(response.getOutputStream());

			options.setBaseImageURL(request.getContextPath() + "/images");
			options.setImageDirectory(sc.getRealPath("/images"));

			task.setParameterValue("imgurl", margeImgUrl);
			//task.setParameterValue("desc", description);
			task.setParameterValue("legendurl", margeLegendUrl);
			task.setParameterValue("title", title);
			task.setParameterValue("scale", scale);
			task.setParameterValue("crn", crn);
			task.setRenderOption(options);

			logger.debug("Generating pdf......");
			
			

			// run report
			task.run();
			task.close();

			int _pos = margeImgUrl.lastIndexOf("/");
			String mergeFileName = margeImgUrl.substring(_pos);
			mergeFileName = outputFilePath + mergeFileName;
			//logger.error(mergeFileName);
			
			imagesArr.add(mergeFileName);
			
			logger.debug("Generating pdf complete......");
			
			
			logger.debug("Removing files from: " + outputFilePath);
			try{
				//Remove temporary files
				synchronized(this){
					removeTemporaryImages(imagesArr);
				}
			}catch(Exception e){
				logger.error(e);
			}
			
			response.getOutputStream().println(
					"{\"success\": true,\"file\":\"" + "file" + "\"}");
			logger.debug("Sending success response complete......");

		} catch (Exception e) {

			response.getOutputStream().println(
					"{\"success\": false,\"message\":\"" + e.getMessage()
							+ "\"}");
			logger.error(e);
			e.printStackTrace();

		} finally {

			//fos.flush();
			//fos.close();

		}

	}
	
	
	
	private void removeTemporaryImages(List<String> imagePath)throws Exception{
		
		if(! logger.isDebugEnabled()){
			for(String s:imagePath){
				File file = new File(s);
				//logger.error(s);
				file.delete();
			}
		}
		
	}

	public String getWMSImage(String wmsurl, int width, int height,
			String outputFile, float opacity, String ctxLayerFormat, String bbox) throws Exception {
		// String _bbox = bbox;
		BufferedImage img = null;
		BufferedImage margeImg = null;
		Rectangle imageBounds = new Rectangle(0, 0, width, height);
		
		margeImg = new BufferedImage(imageBounds.width,
				imageBounds.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = margeImg.createGraphics();
		g.setComposite(AlphaComposite.Clear);
		g.setPaint(Color.WHITE);
		g.fill(imageBounds);

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				opacity));
		//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT, opacity));

		URL url = new URL(wmsurl + "&BBOX=" + bbox + "&WIDTH=" + width
				+ "&HEIGHT=" + height);

		logger.debug("----WMS layer URL: " + url);

		img = ImageIO.read(url);

		g.drawImage(img, 0, 0, null);
		
		mapFileName = System.currentTimeMillis() + "_map.png"; //+ formatExtn; //"_map.gif";
		ImageIO.write(margeImg, "png", new File(outputFile + "/" + mapFileName));
		logger.debug("### " + outputFile + "/" + mapFileName );
		
		return outputFile + "/" + mapFileName;
	}

	/**
	 * Fetches map images from remote WMS Server
	 * 
	 * @param imgStr
	 * @param width
	 * @param height
	 * @param outputFile
	 * @return
	 */
	public String mergeImages(ArrayList<String> images, int width, int height,
			String outputFile, String ctxLayerFormat) throws Exception {
		// for Water mark
		//images.add("D:\\apache-tomcat-7.0.25\\webapps\\snowdoniaprow/resources/temp/images/watermark.png");
		BufferedImage img = null;
		Rectangle imageBounds = new Rectangle(0, 0, width, height);
		BufferedImage margeImg = null;
		
		margeImg = new BufferedImage(imageBounds.width,
		imageBounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = margeImg.createGraphics();
		g.setComposite(AlphaComposite.Clear);
		//g.setPaint(Color.WHITE);
		g.fill(imageBounds);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		for (int i = 0; i < images.size(); i++) {
			File f = new File(images.get(i));
			img = ImageIO.read(new File(images.get(i)));
			g.drawImage(img, 0, 0, null);
			
			/****************For watermark******************/
			/*if(f.getName().equalsIgnoreCase(watermarkImg)){
			g.drawImage(img, 260, 25, null);			
			}else{
				g.drawImage(img, 0, 0, null);
			}*/
			/**********************************/
		}

		mapFileName = System.currentTimeMillis() + "_map.png"; //+ formatExtn;
		ImageIO.write(margeImg, "png", new File(outputFile + "/" + mapFileName));
		logger.debug("### merge image" + outputFile + "/" + mapFileName);

		return outputUrl + mapFileName;
	}

	public String encodeURI(String argString) {
		final String mark = "-_.!~*'()\"";

		StringBuilder uri = new StringBuilder(); // Encoded URL
		// thanks Marco!

		char[] chars = argString.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
					|| (c >= 'A' && c <= 'Z') || mark.indexOf(c) != -1) {
				uri.append(c);
			} else {
				uri.append("%");
				uri.append(Integer.toHexString((int) c));
			}
		}
		return uri.toString();
	}

	public String getContents(File aFile) {
		// ...checks on aFile are elided
		StringBuilder contents = new StringBuilder();

		try {
			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
				String line = null; // not declared within while loop
				/*
				 * readLine is a bit quirky : it returns the content of a line
				 * MINUS the newline. it returns null only for the END of the
				 * stream. it returns an empty String if two newlines appear in
				 * a row.
				 */
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return contents.toString();
	}

	private String calculateScale(int inWidth, int inHeight, Envelope env) {

		double imageWidthInPixels = inWidth;
		double extentWidthInMeters = env.getWidth();

		// constants:
		double dpi = 72.0;
		double inchesPerPixel = 1.0 / dpi;
		double inchesPerMeter = 1.0 / 0.0254000508;

		double inchesOnScreen = inchesPerPixel * imageWidthInPixels;
		double inchesInReality = inchesPerMeter * extentWidthInMeters;

		double scaleDenominator = 1 / (inchesOnScreen / inchesInReality);

		// Rectangle tempScreenRect = new Rectangle(inWidth, inHeight);
		// double tempWorldX1 = convert.toWorldX(0);
		// double tempWidth = convert.toWorldX(tempScreenRect.width) -
		// tempWorldX1;
		// if (tempWidth < 0)
		// return "";

		return "1:" + Math.round(scaleDenominator);

	}

}
