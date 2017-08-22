package com.ge.predix.solsvc.kitservice.boot.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ge.predix.entity.asset.AssetTag;
import com.ge.predix.solsvc.kitservice.model.RegisterDevice;

/**
 * 
 * @author 212546387 -
 */
@Component
public class CSVWriter {
	 
	//Delimiter used in CSV file
	private String NEW_LINE_SEPARATOR = "\n"; //$NON-NLS-1$
	
	@Value("${kit.device.export.asset.properties}")
	private String assetProperties;
	
	@Value("${kit.device.export.assettag.properties}")
	private String assetTagProperties;
	
	@Value("${kit.device.export.assettag.timeseriesdatasource.properties}")
	private String assetTagTimeSeriesDataSourceProperties;
	
	
	private Object [] FILE_HEADER_ASSET = {"uri", //$NON-NLS-1$
			"userGroup", //$NON-NLS-1$
			"latitude", //$NON-NLS-1$
			"longitude", //$NON-NLS-1$
			"updateDate", //$NON-NLS-1$
			"expirationDate", //$NON-NLS-1$
			"deviceType", //$NON-NLS-1$
			"deviceName", //$NON-NLS-1$
			"deviceGroup", //$NON-NLS-1$
			"deviceAddress", //$NON-NLS-1$
			"createdDate", //$NON-NLS-1$
			"activationDate"}; //$NON-NLS-1$
	private Object [] FILE_HEADER_TAG = {"deviceURI", //$NON-NLS-1$
			"tagUri", //$NON-NLS-1$
			"label", //$NON-NLS-1$
			"isKpi", //$NON-NLS-1$
			"unit", //$NON-NLS-1$
			"hiQualityThreshold", //$NON-NLS-1$
			"hiAlarmThreshold", //$NON-NLS-1$
			"loAlarmThreshold", //$NON-NLS-1$
			"loQualityThreshold", //$NON-NLS-1$
			"lastCalibrated", //$NON-NLS-1$
			"locationUUID", //$NON-NLS-1$
			"alertStatusUri"}; //$NON-NLS-1$
	
	/**
	 *  -
	 */
	@PostConstruct
	public void activate() {
		this.FILE_HEADER_ASSET = this.assetProperties.replaceAll(", ", ",").split(","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.FILE_HEADER_TAG = this.assetTagProperties.replaceAll(", ", ",").split(","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	/**
	 * @param devices -
	 * @return -
	 * @throws IOException -
	 */
	public String getAssetCSV(List<RegisterDevice> devices) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a"); //$NON-NLS-1$
		StringWriter deviceWriter = new StringWriter();
		CSVFormat csvFileFormatDevice = CSVFormat.DEFAULT.withRecordSeparator(this.NEW_LINE_SEPARATOR);
		try (CSVPrinter assetPrinter = new CSVPrinter(deviceWriter,csvFileFormatDevice);){
			assetPrinter.printRecord(this.FILE_HEADER_ASSET);
			for (RegisterDevice device:devices) {
					List<Object> record = new ArrayList<Object>();
					for (Object key:this.FILE_HEADER_ASSET) {
						if ("latitude".equals(key.toString()) || "longitude".equals(key.toString()) || PropertyUtils.isReadable(device, key.toString())) { //$NON-NLS-1$ //$NON-NLS-2$
							try {
								switch(key.toString()) {
									case "updateDate" : //$NON-NLS-1$
									case "expirationDate" : //$NON-NLS-1$
									case "createdDate" : //$NON-NLS-1$
									case "activationDate" : //$NON-NLS-1$
										record.add(sdf.format(new Date(Long.valueOf(PropertyUtils.getProperty(device, key.toString()).toString()))));
										break;
									case "latitude": //$NON-NLS-1$
										record.add(device.getGeoLocation().getLatitude());
										break;
									case "longitude": //$NON-NLS-1$
										record.add(device.getGeoLocation().getLongitude());
										break;
									default:
										record.add(PropertyUtils.getProperty(device, key.toString()).toString());
								}
							} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
								throw new RuntimeException("Exception when reading Device",e); //$NON-NLS-1$
							}
						}
					}
					assetPrinter.printRecord(record);
				
			}
		} catch (IOException e) {
			throw new RuntimeException("Exception when creating csv file for Device ",e); //$NON-NLS-1$
		}
		
		
		StringWriter tagWriter = new StringWriter();
		CSVFormat csvFileFormatTag = CSVFormat.DEFAULT.withRecordSeparator(this.NEW_LINE_SEPARATOR);
		try (CSVPrinter tagPrinter = new CSVPrinter(tagWriter,csvFileFormatTag);){
			tagPrinter.printRecord(this.FILE_HEADER_TAG);
			for (RegisterDevice device:devices) {
				for (AssetTag tag:device.getTags()) {
					List<Object> record = new ArrayList<Object>();
					record.add(device.getUri());
					for (Object key:this.FILE_HEADER_TAG) {
						if ("tag".equals(key.toString()) || PropertyUtils.isReadable(tag, key.toString())) {  //$NON-NLS-1$
							try {
								switch(key.toString()) {
									case "tag" : //$NON-NLS-1$
										record.add(tag.getTimeseriesDatasource().getTag());
										break;
									default:
										Object value = PropertyUtils.getProperty(tag, key.toString());
										if (value != null) {
											record.add(value.toString());
										}else{
											record.add(""); //$NON-NLS-1$
										}
								}
							} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
								throw new RuntimeException("Exception when reading AssetTag",e); //$NON-NLS-1$
							}
						}
					}
					tagPrinter.printRecord(record);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Exception when creating csv file for Asset Tags",e); //$NON-NLS-1$
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
