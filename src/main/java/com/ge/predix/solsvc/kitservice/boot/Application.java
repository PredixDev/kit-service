package com.ge.predix.solsvc.kitservice.boot;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 
 * @author 212421693 -
 */
@SpringBootApplication
@EnableSwagger2
@Configuration
@EnableAutoConfiguration(exclude =
{
        DataSourceAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
        PersistenceExceptionTranslationAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.ge.predix.solsvc.kitservice"})
@ImportResource(
{
        "classpath*:META-INF/application-security.xml",
        "classpath*:META-INF/spring/predix-rest-client-scan-context.xml",
        "classpath*:META-INF/spring/ext-util-scan-context.xml", 
        "classpath*:META-INF/spring/fdh-asset-handler-scan-context.xml",
        "classpath*:META-INF/spring/timeseries-bootstrap-scan-context.xml"
    })
@Controller
public class Application
{
    private static final Logger       log = LoggerFactory.getLogger(Application.class);
    @SuppressWarnings("unused")
    private static ApplicationContext context;
  
    /**
     * 
     */
    static String[] profiles ;

    @SuppressWarnings("javadoc")
    @Value("${java.docs.url:null}")
    String docsUrl ;
    
    /**
     * @param args
     *            -
     */
    @SuppressWarnings({})
    public static void main(String[] args)
    {
        SpringApplication springApplication = new SpringApplication(Application.class);
        ApplicationContext ctx = springApplication.run(args);
        context = ctx;
        log.debug("Let's inspect the beans provided by Spring Boot:"); //$NON-NLS-1$
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames)
        {
            log.debug(beanName);
        }

        log.debug("Let's inspect the profiles provided by Spring Boot:"); //$NON-NLS-1$
        profiles = ctx.getEnvironment().getActiveProfiles();
        for (int i = 0; i < profiles.length; i++)
            log.debug("profile****=" + profiles[i]); //$NON-NLS-1$

        log.info("Let's inspect the properties provided by Spring Boot:"); //$NON-NLS-1$
        MutablePropertySources propertySources = ((StandardServletEnvironment) ctx.getEnvironment())
                .getPropertySources();
        Iterator<org.springframework.core.env.PropertySource<?>> iterator = propertySources.iterator();
        while (iterator.hasNext())
        {
            Object propertySourceObject = iterator.next();
            if ( propertySourceObject instanceof org.springframework.core.env.PropertySource )
            {
                org.springframework.core.env.PropertySource<?> propertySource = (org.springframework.core.env.PropertySource<?>) propertySourceObject;
                log.info("propertySource=" + propertySource.getName() + " values=" + propertySource.getSource() //$NON-NLS-1$ //$NON-NLS-2$
                        + "class=" + propertySource.getClass()); //$NON-NLS-1$
            }
        }
    }

   

    /**
     * @param filter -
     * @return -
     */
    @Bean
    public FilterRegistrationBean registration(AuthenticationFilter filter)
    {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * @param filter - 
     * @return -
     */
    @Bean
    public FilterRegistrationBean registration2(OAuth2AuthenticationProcessingFilter filter)
    {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }
    
    /**
     * @return -
     */
    @Bean
    public Docket documentation()
    {
        return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any()).paths(paths())
                .build().pathMapping("/") //$NON-NLS-1$
                .apiInfo(metadata());
    }

    /**
     * 
     * @return -
     */

    @SuppressWarnings("unchecked")
    private Predicate<String> paths()
    {
        return or(
                regex("/device.*")); //$NON-NLS-1$
        
    }

    /**
     * @return -
     */
    @Bean
    UiConfiguration uiConfig()
    {
        return new UiConfiguration("validatorUrl", // url //$NON-NLS-1$
                "none", // docExpansion => none | list //$NON-NLS-1$
                "alpha", // apiSorter => alpha //$NON-NLS-1$
                "schema", // defaultModelRendering => schema //$NON-NLS-1$
                UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS, true, // enableJsonEditor
                                                                        // =>
                                                                        // true
                                                                        // |
                                                                        // false
                true); // showRequestHeaders => true | false
    }

    /**
     * Ensure the Tomcat container comes up, not the Jetty one.
     * 
     * @return - the factory
     */
    @Bean
    public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory()
    {
        return new TomcatEmbeddedServletContainerFactory();
    }

    private ApiInfo metadata()
    {
        return new ApiInfoBuilder().title("Kit Service") //$NON-NLS-1$
                .description("Kit Service") //$NON-NLS-1$
                .version("1.1.6") //$NON-NLS-1$
                .build();
    }

    /**
     * @param request -
     * @param name -
     * @param model -
     * @return -
     */
    @RequestMapping("/")
    public String greetings(HttpServletRequest request ,@RequestParam(value = "name", required = false, defaultValue = "Predix") String name,
            Model model)
    {   StringBuffer requesturi = request.getRequestURL();
        String applicationURl = requesturi.toString().replaceAll("http", "https");//$NON-NLS-1$ //$NON-NLS-2$ 
        if(checkProfile("local")) { //$NON-NLS-1$
            applicationURl = requesturi.toString(); 
        }
      
        model.addAttribute("api",applicationURl.toString()+"api");//$NON-NLS-1$ //$NON-NLS-2$ 
        model.addAttribute("health",applicationURl.toString()+"health");//$NON-NLS-1$ //$NON-NLS-2$ 
        model.addAttribute("docs",this.docsUrl);//$NON-NLS-1$ 
        return "index"; //$NON-NLS-1$
    }

    /**
     * @param profile - local
     * @return -
     */
    private boolean checkProfile(String profile)
    {
        return Arrays.asList(profiles).contains(profile);
    }



    /**
     * 
     * @param request
     *            - HttpServletRequest
     * @param response
     *            - HttpServletResponse
     * @return - Model View
     * @throws Exception
     *             - Exception
     */
    @RequestMapping("/docs")
    protected ModelAndView docs(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        return new ModelAndView("redirect:/javadoc/index.html"); //$NON-NLS-1$

    }
    
    
    /**
     * @param request -
     * @param response -
     * @throws IOException -
     */
    @RequestMapping("/api")
    public @ResponseBody void api(HttpServletRequest request ,HttpServletResponse response ) throws IOException
    {   String applicationURl = getApplicationUrl(request);
        response.sendRedirect(applicationURl.replace("/api", "/swagger-ui.html")); //$NON-NLS-1$//$NON-NLS-2$

    }
    
    /**
     * 
     * @param request
     * @return - Application URL
     */
    private String getApplicationUrl (final HttpServletRequest request){
   
        String applicationURl = request.getRequestURL().toString().replaceAll("http", "https");//$NON-NLS-1$ //$NON-NLS-2$ 
        if(checkProfile("local")) { //$NON-NLS-1$
            applicationURl = request.getRequestURL().toString(); // localhost support for http
        }
       return applicationURl;
    }
    

}
