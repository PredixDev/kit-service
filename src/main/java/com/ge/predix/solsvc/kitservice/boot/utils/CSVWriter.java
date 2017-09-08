package com.ge.predix.solsvc.kitservice.boot.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.ge.predix.solsvc.kitservice.model.RegisterDevice;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * 
 * @author 212546387 -
 */
@Component
public class CSVWriter {
	 
	
	@Autowired
    private
    ResourceLoader resourceLoader;
	
	/**
	 * Free marker Configuration 
	 */
	Configuration cfg = null;
	
	
	/**
	 *  -
	 */
	@PostConstruct
	public void activate() {
		
			// Create your Configuration instance, and specify if up to what FreeMarker
			// version (here 2.3.25) do you want to apply the fixes that are not 100%
			// backward-compatible. See the Configuration JavaDoc for details.
			this.cfg = new Configuration(Configuration.VERSION_2_3_25);

			// Specify the source where the template files come from. Here I set a
			// plain directory for it, but non-file-system sources are possible too:
			Resource resource = this.resourceLoader.getResource("classpath:export"); //$NON-NLS-1$
			try {
				File dbAsFile = resource.getFile();
				
				this.cfg.setDirectoryForTemplateLoading(dbAsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Set the preferred charset template files are stored in. UTF-8 is
			// a good choice in most applications:
			this.cfg.setDefaultEncoding("UTF-8"); //$NON-NLS-1$

			// Sets how errors will appear.
			// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
			this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

			// Don't log exceptions inside FreeMarker that it will thrown at you anyway:
			this.cfg.setLogTemplateExceptions(false);
			
			this.cfg.setDateTimeFormat("MM/dd/yyyy hh:mm:ss a"); //$NON-NLS-1$
		
	}
	/**
	 * @param devices -
	 * @return -
	 * @throws IOException -
	 */
	public String getAssetCSV(List<RegisterDevice> devices) {
		Map<String, Object> root = new HashMap<>();
		
		root.put("devices", devices); //$NON-NLS-1$
		
		StringWriter deviceWriter = new StringWriter();
		try {
			Template temp = this.cfg.getTemplate("export_asset.csv"); //$NON-NLS-1$
			temp.process(root, deviceWriter);
		} catch (IOException | TemplateException e) {
			throw new RuntimeException("Exception when exporting Asset",e); //$NON-NLS-1$
		}
		StringWriter tagWriter = new StringWriter();
		try {
			Template temp = this.cfg.getTemplate("export_assettag.csv"); //$NON-NLS-1$
			temp.process(root, tagWriter);
		} catch (IOException | TemplateException e) {
			throw new RuntimeException("Exception when exporting Asset",e); //$NON-NLS-1$
		}
		
		String zipFileName = "AssetModel.zip"; //$NON-NLS-1$
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));){
		
			zos.putNextEntry(new ZipEntry("Asset.csv")); //$NON-NLS-1$
			byte[] data = deviceWriter.toString().getBytes();
			zos.write(data, 0, data.length);
			zos.closeEntry();
			
			zos.putNextEntry(new ZipEntry("AssetTag.csv")); //$NON-NLS-1$
			data = tagWriter.toString().getBytes();
			zos.write(data, 0, data.length);
			zos.closeEntry();
			
		} catch (IOException e) {
			throw new RuntimeException("Exception when creating Zip file",e); //$NON-NLS-1$
		}
		return "AssetModel.zip"; //$NON-NLS-1$
	}
}
