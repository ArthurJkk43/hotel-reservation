package com.example.hotel_reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HotelReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelReservationApplication.class, args);
	}

}
