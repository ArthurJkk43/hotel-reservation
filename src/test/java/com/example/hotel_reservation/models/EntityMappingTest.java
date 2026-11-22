package com.example.hotel_reservation.models;

import com.example.hotel_reservation.models.entities.audit.AuditLog;
import com.example.hotel_reservation.models.entities.bookings.Payment;
import com.example.hotel_reservation.models.entities.bookings.Reservation;
import com.example.hotel_reservation.models.entities.bookings.ReservationRoom;
import com.example.hotel_reservation.models.entities.hotels.Amenity;
import com.example.hotel_reservation.models.entities.hotels.Hotel;
import com.example.hotel_reservation.models.entities.hotels.HotelImage;
import com.example.hotel_reservation.models.entities.hotels.HotelInvitation;
import com.example.hotel_reservation.models.entities.hotels.HotelStaff;
import com.example.hotel_reservation.models.entities.hotels.HotelStaffId;
import com.example.hotel_reservation.models.entities.pricing.CancellationPolicy;
import com.example.hotel_reservation.models.entities.pricing.PromoCode;
import com.example.hotel_reservation.models.entities.pricing.RatePlan;
import com.example.hotel_reservation.models.entities.pricing.RoomRate;
import com.example.hotel_reservation.models.entities.reviews.FavoriteHotel;
import com.example.hotel_reservation.models.entities.reviews.Review;
import com.example.hotel_reservation.models.entities.reviews.ReviewResponse;
import com.example.hotel_reservation.models.entities.rooms.Room;
import com.example.hotel_reservation.models.entities.rooms.RoomBlock;
import com.example.hotel_reservation.models.entities.rooms.RoomType;
import com.example.hotel_reservation.models.entities.users.AppUser;
import com.example.hotel_reservation.models.entities.users.OneTimeToken;
import com.example.hotel_reservation.models.entities.users.RefreshToken;
import com.example.hotel_reservation.models.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema validation (ddl-auto=validate) only compares column types. This test really
 * INSERTs one row of every entity into PostgreSQL, so DB defaults, NOT NULL, CHECK and
 * composite-FK constraints, and the special types (CHAR, CITEXT, INET, JSONB, @Version,
 * @EmbeddedId) are all exercised. Everything is rolled back at the end (@Transactional).
 */
@SpringBootTest
@Transactional
class EntityMappingTest {

    private static final String HASH = "a".repeat(64);

    @Autowired
    private EntityManager em;

    @Test
    void persistsEveryEntity() throws Exception {
        Instant now = Instant.now();
        LocalDate in = LocalDate.of(2030, 1, 10);
        LocalDate out = in.plusDays(3);

        // --- users -------------------------------------------------------------------
        AppUser owner = user("mapping.owner@example.test");
        AppUser guest = user("mapping.guest@example.test");

        RefreshToken refresh = new RefreshToken();
        refresh.setUser(guest);
        refresh.setTokenHash(HASH);
        refresh.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
        refresh.setIpAddress(InetAddress.getByName("192.168.1.10"));
        em.persist(refresh);

        OneTimeToken ott = new OneTimeToken();
        ott.setUser(guest);
        ott.setType(OneTimeTokenType.EMAIL_VERIFICATION);
        ott.setTokenHash("b".repeat(64));
        ott.setExpiresAt(now.plus(1, ChronoUnit.DAYS));
        em.persist(ott);

        // --- hotel, staff, amenities ---------------------------------------------------
        Amenity wifi = em.createQuery("select a from Amenity a where a.code = 'WIFI'", Amenity.class)
                .getSingleResult();
        Amenity minibar = em.createQuery("select a from Amenity a where a.code = 'MINIBAR'", Amenity.class)
                .getSingleResult();

        Hotel hotel = new Hotel();               // only required fields; the rest use the Java defaults
        hotel.setName("Mapping Test Hotel");
        hotel.setSlug("mapping-test-hotel");
        hotel.setAddressLine1("1 Test Street");
        hotel.setCity("Bishkek");
        hotel.setCountryCode("KG");
        hotel.getAmenities().add(wifi);
        em.persist(hotel);

        HotelStaff staff = new HotelStaff();
        staff.setHotel(hotel);
        staff.setUser(owner);
        staff.setStaffRole(StaffRole.OWNER);
        em.persist(staff);
        em.flush();                              // staff row must exist before FKs that point to it

        HotelInvitation invitation = new HotelInvitation();
        invitation.setHotel(hotel);
        invitation.setEmail("New.Manager@Example.test");
        invitation.setStaffRole(StaffRole.MANAGER);
        invitation.setTokenHash("c".repeat(64));
        invitation.setInvitedBy(owner);
        invitation.setExpiresAt(now.plus(3, ChronoUnit.DAYS));
        em.persist(invitation);

        // --- rooms ---------------------------------------------------------------------
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName("Deluxe King");
        roomType.setBedType(BedType.KING);
        roomType.setMaxAdults((short) 2);
        roomType.setBasePrice(new BigDecimal("120.00"));
        roomType.getAmenities().add(minibar);
        em.persist(roomType);

        HotelImage image = new HotelImage();
        image.setHotel(hotel);
        image.setRoomType(roomType);
        image.setUrl("https://example.test/room.jpg");
        em.persist(image);

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(roomType);
        room.setRoomNumber("204");
        room.setFloor((short) 2);
        em.persist(room);

        RoomBlock block = new RoomBlock();
        block.setRoom(room);
        block.setHotel(hotel);
        block.setStartDate(in.plusDays(30));
        block.setEndDate(in.plusDays(32));
        block.setReason(RoomBlockReason.MAINTENANCE);
        block.setCreatedBy(owner);
        em.persist(block);

        // --- pricing -------------------------------------------------------------------
        CancellationPolicy policy = new CancellationPolicy();
        policy.setHotel(hotel);
        policy.setName("Free cancellation");
        policy.setFreeCancelDays((short) 2);
        policy.setPenaltyPercent(new BigDecimal("50.00"));
        em.persist(policy);

        RatePlan ratePlan = new RatePlan();
        ratePlan.setHotel(hotel);
        ratePlan.setCancellationPolicy(policy);
        ratePlan.setName("Flexible");
        em.persist(ratePlan);

        RoomRate rate = new RoomRate();
        rate.setHotel(hotel);
        rate.setRoomType(roomType);
        rate.setRatePlan(ratePlan);
        rate.setStartDate(in);
        rate.setEndDate(out);
        rate.setPricePerNight(new BigDecimal("100.00"));
        em.persist(rate);

        PromoCode promo = new PromoCode();
        promo.setHotel(hotel);
        promo.setCode("MAPPING10");
        promo.setDiscountType(DiscountType.PERCENT);
        promo.setDiscountValue(new BigDecimal("10.00"));
        promo.setValidFrom(now);
        promo.setValidUntil(now.plus(30, ChronoUnit.DAYS));
        em.persist(promo);

        // --- booking -------------------------------------------------------------------
        Reservation reservation = new Reservation();
        reservation.setBookingRef("MAPTEST1");
        reservation.setUser(guest);
        reservation.setHotel(hotel);
        reservation.setCurrency("USD");
        reservation.setSubtotal(new BigDecimal("300.00"));
        reservation.setDiscountAmount(new BigDecimal("30.00"));
        reservation.setTotalAmount(new BigDecimal("270.00"));
        reservation.setPromoCode(promo);
        reservation.setGuestName("Mapping Guest");
        reservation.setGuestEmail("mapping.guest@example.test");
        em.persist(reservation);

        ReservationRoom reservedRoom = new ReservationRoom();
        reservedRoom.setReservation(reservation);
        reservedRoom.setHotel(hotel);
        reservedRoom.setRoom(room);
        reservedRoom.setRatePlan(ratePlan);
        reservedRoom.setCheckIn(in);
        reservedRoom.setCheckOut(out);
        reservedRoom.setAdults((short) 2);
        reservedRoom.setPriceTotal(new BigDecimal("300.00"));
        reservedRoom.setCancelFreeUntil(in.minusDays(2));
        reservedRoom.setPenaltyPercent(new BigDecimal("50.00"));
        em.persist(reservedRoom);

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setHotel(hotel);
        payment.setAmount(new BigDecimal("270.00"));
        payment.setCurrency("USD");
        payment.setMethod(PaymentMethod.CARD);
        payment.setProvider("SIMULATED");
        payment.setIdempotencyKey("mapping-test-key-1");
        em.persist(payment);

        // --- reviews, favorites, audit -------------------------------------------------
        Review review = new Review();
        review.setReservation(reservation);
        review.setHotel(hotel);
        review.setUser(guest);
        review.setRating((short) 5);
        review.setHeadline("Great stay");
        em.persist(review);

        ReviewResponse response = new ReviewResponse();
        response.setReview(review);
        response.setHotel(hotel);
        response.setStaff(owner);
        response.setBody("Thank you!");
        em.persist(response);

        FavoriteHotel favorite = new FavoriteHotel();
        favorite.setUser(guest);
        favorite.setHotel(hotel);
        em.persist(favorite);

        AuditLog audit = new AuditLog();
        audit.setActor(owner);
        audit.setHotel(hotel);
        audit.setAction("HOTEL_CREATED");
        audit.setEntityType("hotel");
        audit.setEntityId(hotel.getId());
        audit.setDetails("{\"name\": \"Mapping Test Hotel\"}");
        audit.setIpAddress(InetAddress.getByName("2001:db8::1"));
        em.persist(audit);

        em.flush();   // all INSERTs are sent here; any mapping/constraint problem fails the test
        em.clear();   // forget the Java objects so the reads below come from the database

        // --- read back the columns that need special mapping ---------------------------
        Hotel h = em.find(Hotel.class, hotel.getId());
        assertThat(h.getStatus()).isEqualTo(HotelStatus.DRAFT);
        assertThat(h.getCurrency()).isEqualTo("USD");
        assertThat(h.getCountryCode()).isEqualTo("KG");
        assertThat(h.getCheckInTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(h.getCreatedAt()).isNotNull();          // filled by the DB default
        assertThat(h.getAmenities()).extracting(Amenity::getCode).containsExactly("WIFI");

        assertThat(em.find(HotelStaff.class, new HotelStaffId(hotel.getId(), owner.getId())).getStaffRole())
                .isEqualTo(StaffRole.OWNER);
        assertThat(em.find(RoomType.class, roomType.getId()).getAmenities())
                .extracting(Amenity::getCode).containsExactly("MINIBAR");
        assertThat(em.find(RefreshToken.class, refresh.getId()).getIpAddress())
                .isEqualTo(InetAddress.getByName("192.168.1.10"));
        assertThat(em.find(Reservation.class, reservation.getId()).getVersion()).isZero();

        AuditLog a = em.find(AuditLog.class, audit.getId());
        assertThat(a.getDetails()).contains("Mapping Test Hotel");
        assertThat(a.getIpAddress()).isEqualTo(InetAddress.getByName("2001:db8::1"));
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setFirstName("Mapping");
        user.setLastName("Test");
        user.setStatus(UserStatus.ACTIVE);
        em.persist(user);
        return user;
    }
}
