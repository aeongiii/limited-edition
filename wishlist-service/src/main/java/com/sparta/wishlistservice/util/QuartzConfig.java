package com.sparta.wishlistservice.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {
    // SchedulerFactoryBean : 전체 스케줄러 설정
    // SpringBeanJobFactory : 스케줄러에서 job 객체 생성할 때 Spring Bean 주입 가능하게 함

    // Quartz는 new키워드로 job객체를 생성하기 때문에 스프링 빈 주입 불가하다. -> SpringBeanFactory를 사용해서 Spring context에서 빈 주입하도록 설정해야 한다.
    // 즉, SpringBeanJobFactory에 ApplicationContext(Bean 객체 관리하는 컨테이너)를 연결해서 bean을 사용할 수 있도록 한다.
    @Bean
    public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory(); // SpringBeanFactory 생성
        jobFactory.setApplicationContext(applicationContext); // Quartz job에서도 스프링 빈을 사용할 수 있도록 Spring ApplicationContext 연결
        return jobFactory; // factory를 빈으로 등록
    }

    // Quartz 스케줄러 생성하는 SchedulerFactoryBean 빈 설정 : JobDetail과 Trigger를 연결 -> Quartz 스케줄러가 job을 주기적으로 실행하도록 한다.
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(SpringBeanJobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean(); // Quartz의 스케줄러를 생성 및 관리하는 Factory 객체 생성
        factory.setJobFactory(jobFactory); // Quartz가 job을 생성할 때 사용하는 job Factory (Spring Context와 연결시킨다)
        return factory; // SchedulerFactoryBean을 빈으로 등록
    }

}