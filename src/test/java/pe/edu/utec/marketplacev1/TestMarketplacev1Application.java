package pe.edu.utec.marketplacev1;

import org.springframework.boot.SpringApplication;

public class TestMarketplacev1Application {

    public static void main(String[] args) {
        SpringApplication.from(Marketplacev1Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
