package org.activiti.app;

import org.activiti.app.conf.ApplicationConfiguration;
import org.activiti.app.servlet.ApiDispatcherServletConfiguration;
import org.activiti.app.servlet.AppDispatcherServletConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author xuWeiJia
 * @date 2020/1/21
 */
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        org.activiti.spring.boot.SecurityAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@Import({ApplicationConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServletRegistrationBean apiDispatcher() {
        DispatcherServlet api = new DispatcherServlet();
        api.setContextClass(AnnotationConfigWebApplicationContext.class);
        api.setContextConfigLocation(ApiDispatcherServletConfiguration.class.getName());
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        servletRegistrationBean.setServlet(api);
        servletRegistrationBean.addUrlMappings("/api/*");
        servletRegistrationBean.setLoadOnStartup(1);
        servletRegistrationBean.setAsyncSupported(true);
        servletRegistrationBean.setName("api");
        return servletRegistrationBean;
    }

    @Bean
    public ServletRegistrationBean appDispatcher() {
        DispatcherServlet app = new DispatcherServlet();
        app.setContextClass(AnnotationConfigWebApplicationContext.class);
        app.setContextConfigLocation(AppDispatcherServletConfiguration.class.getName());
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        servletRegistrationBean.setServlet(app);
        servletRegistrationBean.addUrlMappings("/app/*");
        servletRegistrationBean.setLoadOnStartup(1);
        servletRegistrationBean.setAsyncSupported(true);
        servletRegistrationBean.setName("app");
        return servletRegistrationBean;
    }
}
