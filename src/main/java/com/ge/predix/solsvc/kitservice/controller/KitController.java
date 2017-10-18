package com.ge.predix.solsvc.kitservice.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ge.predix.solsvc.kitservice.boot.EventError;
import com.ge.predix.solsvc.kitservice.boot.utils.CSVWriter;
import com.ge.predix.solsvc.kitservice.error.DeviceRegistrationError;
import com.ge.predix.solsvc.kitservice.manager.DeviceManager;
import com.ge.predix.solsvc.kitservice.model.RegisterDevice;
import com.ge.predix.solsvc.kitservice.validator.RegisterDeviceValidation;
import com.ge.predix.uaa.token.lib.JsonUtils;

/**
 * Rest API controller for registration of kits
 * 
 * 
 * 
 * @author predix
 */
@RestController()
public class KitController
{

    private static final Logger log = LoggerFactory.getLogger(KitController.class);
    private static int DEVICE_REGISTRATION_ERROR = 1001;
    private static int DEVICE_EXPIRY_ERROR = 1002;
    private static int DEVICE_RESET_ERROR = 1003;
    @Autowired
    private DeviceManager       deviceManager;

    @Autowired
    private CSVWriter csvWriter;
    
    /**
     * 
     */
    public KitController()
    {
        super();
    }

    /**
     * Sample End point which returns a Welcome Message
     * 
     * @param request -
     * 
     * @param echo
     *            - the string to echo back
     * @return -
     */
    @SuppressWarnings({ "unused", "resource" })
	@RequestMapping(value = "/device", method = RequestMethod.GET,
    		produces={MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_XHTML_XML_VALUE}
    )
    public ResponseEntity<?> getDevices(HttpServletRequest request,@SuppressWarnings({
            "javadoc"
    }) @RequestHeader("Authorization") String authorization)
    {
        List<RegisterDevice> devices = null;
        String userId = getUserId(request);
        Boolean isAdmin =  (Boolean) request.getAttribute("isAdmin"); //$NON-NLS-1$
        
        if(isAdmin) { 
            devices = this.getDeviceManager().getAllAdminDevices();            
        }else {
           devices = this.getDeviceManager().getDevices(userId);
        }
        String contentType = request.getHeader("Content-Type"); //$NON-NLS-1$
        
        if (contentType != null && MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
        	String fileName = this.csvWriter.getAssetCSV(devices);
        	File file = new File(fileName);
        	InputStreamResource resource;
			try {
				resource = new InputStreamResource(new FileInputStream(file));
				return ResponseEntity.ok()
	                    .contentLength(file.length())
	                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                    .header("Content-Disposition", "attachment;filename="+fileName) //$NON-NLS-1$ //$NON-NLS-2$
	                    .body(resource);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	return null;
        }
        return new ResponseEntity<List<RegisterDevice>>(devices, HttpStatus.OK);
    }

    /**
     * Method that registers a device
     * 
     * @param device -
     * @param request -
     * @param result -
     * @return -
     */
    @SuppressWarnings("unused")
	@RequestMapping(value = "/device/register", method = RequestMethod.POST)
    public ResponseEntity<?> registerDevice(@RequestBody RegisterDevice device, HttpServletRequest request,
            BindingResult result,@SuppressWarnings("javadoc") @RequestHeader("Authorization") String authorization)
    {
        String userId = getUserId(request);
        log.info("Calling registerKit for user " + userId); //$NON-NLS-1$
       

        // validation
        RegisterDeviceValidation validation = new RegisterDeviceValidation();
        validation.validate(device, result);
        if ( result.hasErrors() )
        {
            List<EventError> errors = setErrors(result.getFieldErrors(), HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        // continue to register device
       // RegisterDevice originalDevice = this.getDeviceManager().getDevice("device", userId, device.getDeviceAddress());//$NON-NLS-1$
        // removing the user check adding check per deviceIdentifer
        RegisterDevice originalDevice = this.getDeviceManager().getDevice("device", null, device.getDeviceAddress());//$NON-NLS-1$
        try
        {
            if ( originalDevice == null )
            {
                // device not found. register it.
                log.info("Registrating device with address " + device.getDeviceAddress()); //$NON-NLS-1$
                this.getDeviceManager().registerDevice(device,userId);
            }
            else
            {
                log.info("This is a registered device with address " + device.getDeviceAddress()); //$NON-NLS-1$
                // this try-catch to check expire
                try {
                // check device expire
                    this.getDeviceManager().checkDeviceExpiry(originalDevice);
                }
                catch (DeviceRegistrationError e)
                {
                    List<EventError> errors = setErrors(e, HttpStatus.BAD_REQUEST.value(),DEVICE_EXPIRY_ERROR);
                    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
                }
                this.getDeviceManager().updateDevice(device, originalDevice,userId);
                
            }
        }
        catch (DeviceRegistrationError e)
        {
            List<EventError> errors = setErrors(e, HttpStatus.BAD_REQUEST.value(),DEVICE_REGISTRATION_ERROR);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        // setting config
        device.setDeviceConfig(this.getDeviceManager().getDeviceConfig());
        return new ResponseEntity<RegisterDevice>(device, HttpStatus.OK);
    }

    /**
     * -
     * 
     * @param list
     * @param status
     */
    private List<EventError> setErrors(List<FieldError> errors, int status)
    {
        List<EventError> eventErrors = new ArrayList<EventError>();

        if ( CollectionUtils.isNotEmpty(errors) )
        {
            for (FieldError error : errors)
            {
                Map<String, Object> errorAttributes = new HashMap<String, Object>();
                errorAttributes.put("error", error.getField()); //$NON-NLS-1$ 
                errorAttributes.put("message", error.getField() + "reject for value "+error.getRejectedValue() + error.getDefaultMessage());//$NON-NLS-1$ //$NON-NLS-2$
                EventError eventError = new EventError(status, errorAttributes);
                eventErrors.add(eventError);

            }
        }
        return eventErrors;

    }
    
    /**
     * @param allErrors
     * @param value
     * @return -
     */
    private List<EventError> setObjectErrors(List<ObjectError> errors, int status)
    {
        List<EventError> eventErrors = new ArrayList<EventError>();
        if ( CollectionUtils.isNotEmpty(errors) )
        {
            for (ObjectError error : errors)
            {
                Map<String, Object> errorAttributes = new HashMap<String, Object>();
                errorAttributes.put("error", error.getObjectName()); //$NON-NLS-1$ 
                errorAttributes.put("message", error.getDefaultMessage());//$NON-NLS-1$
                EventError eventError = new EventError(status, errorAttributes);
                eventErrors.add(eventError);

            }
        }
        return eventErrors;
    }

    /**
     * Method that updated the device
     * @param deviceId -
     * 
     * @param device -
     * @param request -
     * @param result -
     * @param authorization -
     * @return -
     */
 
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateRegisterDevice(@PathVariable String deviceId,@RequestBody RegisterDevice device, HttpServletRequest request,
            BindingResult result, @RequestHeader("Authorization") String authorization)
    {
        String userId = getUserId(request);
        log.info("Calling registerKit for user " + userId); //$NON-NLS-1$
        
        List<ObjectError> errors = new ArrayList<ObjectError>();
        if(!RegisterDeviceValidation.validate(deviceId, Pattern.compile(RegisterDeviceValidation.DEVICE_NAME_PATTERN))) { 
            ObjectError error = new ObjectError("deviceId", "Device id should match ^[A-Za-z0-9-]*");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
        
        if( RegisterDeviceValidation.DEVICE_ADDRESS_LENGTH <= deviceId.length()){
            ObjectError error = new ObjectError("deviceId", "DeviceId is limited to max 75");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
       
        if ( CollectionUtils.isNotEmpty(errors))
        {
            List<EventError> eventErrors = setObjectErrors(errors, HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
       
        device.setDeviceAddress(deviceId);

        
        RegisterDeviceValidation validation = new RegisterDeviceValidation();
        validation.validate(device, result);
        if ( result.hasErrors() )
        {
            List<EventError> eventErrors = setErrors(result.getFieldErrors(), HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
        // check if the device exists
        RegisterDevice originalDevice = this.getDeviceManager().getDevice("device", userId, device.getDeviceAddress());//$NON-NLS-1$
        if ( originalDevice == null )
        {
            // device not found.
            log.info("device not found for the user " + device.getDeviceAddress()); //$NON-NLS-1$
            ObjectError error = new ObjectError("deviceAddress", "Device with not registerted");  //$NON-NLS-1$//$NON-NLS-2$
            result.addError(error);
            List<EventError> eventErrors = setErrors(result.getFieldErrors(), HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);

        }
        try
        {
            this.getDeviceManager().updateDevice(device, originalDevice,userId);
       
        }
        catch (DeviceRegistrationError e)
        {
            List<EventError> eventErrors = setErrors(e, HttpStatus.BAD_REQUEST.value(),DEVICE_REGISTRATION_ERROR);
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
        return this.getDevice(deviceId, request, authorization);

    }

    /**
     * @param e
     * @param badRequest
     * @return -
     */
    @SuppressWarnings("unused")
	private List<EventError> setErrors(DeviceRegistrationError e, int status , int errorCode)
    {
        List<EventError> eventErrors = new ArrayList<EventError>();
        Map<String, Object> errorAttributes = new HashMap<String, Object>();
        errorAttributes.put("error", e.getMessage()); //$NON-NLS-1$
        errorAttributes.put("message", e.getMessage());//$NON-NLS-1$
        errorAttributes.put("code",String.valueOf(errorCode));//$NON-NLS-1$
        EventError eventError = new EventError(errorCode, errorAttributes);
        eventErrors.add(eventError);

        return eventErrors;
    }    
   

    /**
     * @param request
     * @return -
     */
    private String getUserId(HttpServletRequest request)
    {
        Jwt accessToken = (Jwt) request.getAttribute("userToken"); //$NON-NLS-1$
        if ( accessToken == null ) return null;
        Map<String, Object> claims = JsonUtils.readValue(accessToken.getClaims(),
                new TypeReference<Map<String, Object>>()
                {//
                });
        if ( claims == null ) return null;
        return (String) claims.get("user_id"); //$NON-NLS-1$
    }

    /**
     * Details about each Device
     * 
     * @param deviceId -
     * @param model -
     * @param request -
     * @param authorization -
     * @param result -
     * @param echo -
     * @return -
     */
    @SuppressWarnings("resource")
	@RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    public ResponseEntity<?> getDevice(@PathVariable String deviceId, HttpServletRequest request,
             @RequestHeader("Authorization") String authorization)
    {
        String userId = getUserId(request);
        
        List<ObjectError> errors = new ArrayList<ObjectError>();
        if(!RegisterDeviceValidation.validate(deviceId, Pattern.compile(RegisterDeviceValidation.DEVICE_NAME_PATTERN))) { 
            ObjectError error = new ObjectError("deviceId", "Device id should match ^[A-Za-z0-9-]*");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
        
        if( RegisterDeviceValidation.DEVICE_ADDRESS_LENGTH <= deviceId.length()){
            ObjectError error = new ObjectError("deviceId", "DeviceId is limited to max 75");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
       
        if ( CollectionUtils.isNotEmpty(errors))
        {
            List<EventError> eventErrors = setObjectErrors(errors, HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
        // continue with get
        
        RegisterDevice device = this.getDeviceManager().getDevice(deviceId, userId);

        if ( device == null )
        {
            return new ResponseEntity<>(device, HttpStatus.NOT_FOUND);
        }
        // check device activation
        try
        {
            this.getDeviceManager().checkDeviceExpiry(device);
        }
        catch (DeviceRegistrationError e)
        {
            List<EventError> eventErrors = setErrors(e, HttpStatus.BAD_REQUEST.value(),DEVICE_EXPIRY_ERROR);
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
        String contentType = request.getHeader("Content-Type"); //$NON-NLS-1$
        if (contentType == null 
        		|| (!MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)
        		&& !MediaType.APPLICATION_ATOM_XML_VALUE.equals(contentType))) {
        	device.setDeviceConfig(this.getDeviceManager().getDeviceConfig());
        }        
        
        if (contentType != null && MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
        	String fileName = this.csvWriter.getAssetCSV(device);
        	File file = new File(fileName);
        	InputStreamResource resource;
			try {
				resource = new InputStreamResource(new FileInputStream(file));
				return ResponseEntity.ok()
	                    .contentLength(file.length())
	                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                    .header("Content-Disposition", "attachment;filename=\""+fileName+"\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	                    .body(resource);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Exception when Processing zip file"); //$NON-NLS-1$
			}finally {
				file.delete();
			}
        }
		return new ResponseEntity<RegisterDevice>(device, HttpStatus.OK);
    }
    
    
    /**
     * This method resets the Device settings.
     * @param deviceId - 
     * @param device - 
     * @param request -
     * @param result - 
     * @param authorization -
     * @return -
     */
    @RequestMapping(value = "/device/reset/{deviceId}", method = RequestMethod.PUT)
    public ResponseEntity<?> resetRegisterDevice(@PathVariable String deviceId, HttpServletRequest request,
            @RequestHeader("Authorization") String authorization)
    {
        String userId = getUserId(request);
        
        List<ObjectError> errors = new ArrayList<ObjectError>();
        if(!RegisterDeviceValidation.validate(deviceId, Pattern.compile(RegisterDeviceValidation.DEVICE_NAME_PATTERN))) { 
            ObjectError error = new ObjectError("deviceId", "Device id should match ^[A-Za-z0-9-]*");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
        
        if( RegisterDeviceValidation.DEVICE_ADDRESS_LENGTH <= deviceId.length()){
            ObjectError error = new ObjectError("deviceId", "DeviceId is limited to max 75");  //$NON-NLS-1$//$NON-NLS-2$
            errors.add(error);
        }
       
        if ( CollectionUtils.isNotEmpty(errors))
        {
            List<EventError> eventErrors = setObjectErrors(errors, HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
       
        try
        {
         // continue with get device without user
           RegisterDevice device = this.getDeviceManager().getDevice(deviceId,null);
           if(device != null ){
               Boolean isAdmin =  (Boolean) request.getAttribute("isAdmin"); //$NON-NLS-1$
               this.getDeviceManager().resetDevice(device,isAdmin,userId);
           }
           
        }
        catch (DeviceRegistrationError e)
        {
            List<EventError> eventErrors = setErrors(e, HttpStatus.BAD_REQUEST.value(),DEVICE_RESET_ERROR);
            return new ResponseEntity<>(eventErrors, HttpStatus.BAD_REQUEST);
        }
      
        return new ResponseEntity<>(null, HttpStatus.OK);

    }

   

    /**
     * @return the deviceManager
     */
    public DeviceManager getDeviceManager()
    {
        return this.deviceManager;
    }

    /**
     * @param deviceManager the deviceManager to set
     */
    public void setDeviceManager(DeviceManager deviceManager)
    {
        this.deviceManager = deviceManager;
    }

}
