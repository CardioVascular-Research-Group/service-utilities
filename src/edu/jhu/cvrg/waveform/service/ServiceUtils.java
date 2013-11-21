package edu.jhu.cvrg.waveform.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.log4j.Logger;

import edu.jhu.cvrg.liferay.portal.kernel.repository.model.FileEntrySoap;
import edu.jhu.cvrg.liferay.portal.service.ServiceContext;
import edu.jhu.cvrg.liferay.portlet.documentlibrary.service.http.DLAppServiceSoap;
import edu.jhu.cvrg.liferay.portlet.documentlibrary.service.http.DLAppServiceSoapServiceLocator;
import edu.jhu.cvrg.liferay.portlet.documentlibrary.service.http.Portlet_DL_DLAppServiceSoapBindingStub;

public class ServiceUtils {

	public static final String SERVER_TEMP_ANALYSIS_FOLDER = ServiceProperties.getInstance().getProperty("temp.folder")+"/a";
	public static final String SERVER_TEMP_CONVERSION_FOLDER = ServiceProperties.getInstance().getProperty("temp.folder")+"/c";
	
	private static String sep = File.separator;
	
	private static final Logger log = Logger.getLogger(ServiceUtils.class);
	
	public static boolean sendToLiferay(long groupId, long folderId, String outputPath, String fileName, long fileSize, InputStream fis){
		
		log.debug(" +++++ tranferring " + fileName + " to Liferay");
		
		boolean ret = false;
		
		DLAppServiceSoapServiceLocator locator = new DLAppServiceSoapServiceLocator();
		
		try {
			
			ServiceProperties props = ServiceProperties.getInstance();
			
			DLAppServiceSoap service = locator.getPortlet_DL_DLAppService(new URL(props.getProperty("liferay.endpoint.url")));
			
			((Portlet_DL_DLAppServiceSoapBindingStub)service).setUsername(props.getProperty("liferay.ws.user"));
			((Portlet_DL_DLAppServiceSoapBindingStub)service).setPassword(props.getProperty("liferay.ws.password"));
			
			byte[] bytes = new byte[Long.valueOf(fileSize).intValue()];
			fis.read(bytes);
			fis.close();
			
			FileEntrySoap file = service.addFileEntry(groupId, folderId, fileName, "", fileName, "", "1.0", bytes, new ServiceContext());
			
			ret = (file != null);
			
			deleteFile(outputPath, fileName);
			
			log.debug(" +++++ Done tranferring ");
			
		} catch (Exception e) {
			log.error("Error on sendToLiferay: "+e.getMessage());
		}

		return ret;
	}
	
	public static void deleteFile(String inputPath, String inputFilename) {
		deleteFile(inputPath + sep + inputFilename);
	}

	public static void deleteFile(String fullPathFileName) {
		File targetFile = new File(fullPathFileName);
		if(targetFile.exists()){
			targetFile.delete();
		}
	}
	

	public static Map<String, OMElement> extractParams(OMElement e){
		Map<String, OMElement> paramMap = new HashMap<String, OMElement>();  
		for (Iterator<?> iterator = e.getChildren(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if(type instanceof OMElement){
				OMElement node = (OMElement)type;
				paramMap.put(node.getLocalName(), node);
			}
		}
		return paramMap;
	}
	
	public static void createTempLocalFile(Map<String, OMElement> mapWServiceParam, String tagName, String inputPath, String inputFilename) {
		OMElement fileNode = mapWServiceParam.get(tagName);
		if(fileNode != null){
			OMText binaryNode = (OMText) fileNode.getFirstOMChild();
			DataHandler contentDH = (DataHandler) binaryNode.getDataHandler();
			
			File targetDirectory = new File(inputPath);
			
			File targetFile = new File(inputPath + sep + inputFilename);
			
			try {
				targetDirectory.mkdirs();
				
				InputStream fileToSave = contentDH.getInputStream();
				
				OutputStream fOutStream = new FileOutputStream(targetFile);

				int read = 0;
				byte[] bytes = new byte[1024];

				while ((read = fileToSave.read(bytes)) != -1) {
					fOutStream.write(bytes, 0, read);
				}

				fileToSave.close();
				fOutStream.flush();
				fOutStream.close();
				
			} catch (IOException e) {
				log.error("Error on sendToLiferay: "+e.getMessage());
			}finally{
				log.info("File created? " + targetFile.exists());
			}
		}
	}
	
	public static String extractPath(String sHeaderPathName){
		log.info("extractPath() from: '" + sHeaderPathName + "'");

		String sFilePath="";
		int iIndexLastSlash = sHeaderPathName.lastIndexOf("/");
		
		sFilePath = sHeaderPathName.substring(0,iIndexLastSlash+1);
		
		return sFilePath;
	}
	
	public static String extractName(String sFilePathName){
		log.info("extractName() from: '" + sFilePathName + "'");

		String sFileName="";
		int iIndexLastSlash = sFilePathName.lastIndexOf("/");
		
		sFileName = sFilePathName.substring(iIndexLastSlash+1);

		return sFileName;
	}
}
