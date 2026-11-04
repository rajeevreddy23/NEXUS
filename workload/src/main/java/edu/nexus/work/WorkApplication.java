package edu.nexus.work;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class WorkApplication {public static void main(String[] args){RenderDatabase.configure();SpringApplication.run(WorkApplication.class,args);}}
