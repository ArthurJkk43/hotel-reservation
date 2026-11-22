package com.example.hotel_reservation.models.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring, no database): checks the validation rules on the hotel
 * request DTOs, the same rules @Valid applies in a controller.
 */
class HotelRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validCreateRequestHasNoViolations() {
        assertThat(validator.validate(validCreate())).isEmpty();
    }

    @Test
    void createRequiresNameAddressCityAndCountry() {
        var dto = new CreateHotelRequestDto(" ", null, null, null, null, "", null, null, null,
                null, null, null, null, null, null, null, null);

        assertThat(invalidFields(validator.validate(dto)))
                .containsExactlyInAnyOrder("name", "addressLine1", "city", "countryCode");
    }

    @Test
    void createRejectsOutOfRangeAndBadlyFormattedValues() {
        var dto = new CreateHotelRequestDto(
                "x".repeat(151),                  // name too long
                null,
                (short) 6,                        // star rating > 5
                "1 Test Street", null, "Bishkek", null, null,
                "kg",                             // country code must be upper case
                new BigDecimal("91"),             // latitude > 90
                new BigDecimal("-180.0000001"),   // longitude < -180 and too many decimals
                null,
                "US",                             // currency must have 3 letters
                "not-an-email",
                null, null, null);

        assertThat(invalidFields(validator.validate(dto))).containsExactlyInAnyOrder(
                "name", "starRating", "countryCode", "latitude", "longitude", "currency", "contactEmail");
    }

    @Test
    void emptyUpdateIsValidBecauseEveryFieldIsOptional() {
        var dto = new UpdateHotelRequestDto(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void updateStillValidatesTheFieldsThatAreSent() {
        var dto = new UpdateHotelRequestDto(
                "",                               // would wipe the name
                null, (short) 0, null, null, null, null, null,
                "KGZ",                            // 3 letters
                null, null, null, null, null, null, null, null);

        assertThat(invalidFields(validator.validate(dto)))
                .containsExactlyInAnyOrder("name", "starRating", "countryCode");
    }

    private static CreateHotelRequestDto validCreate() {
        return new CreateHotelRequestDto(
                "Grand Plaza", "A hotel in the centre", (short) 4,
                "1 Chui Avenue", null, "Bishkek", "Chuy", "720000", "KG",
                new BigDecimal("42.874621"), new BigDecimal("74.569762"),
                "Asia/Bishkek", "KGS", "info@grandplaza.test", "+996 555 000 000",
                null, null);
    }

    private static Set<String> invalidFields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
