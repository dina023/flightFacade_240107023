/**
 * Client driver class demonstrating the Facade Design Pattern[cite: 7].
 * Notice that Main has zero imports, fields, or references to any of the
 * five underlying subsystems—it interacts strictly with FlightBookingFacade[cite: 5, 7, 8].
 */
public class Main {
    public static void main(String[] args) {
        // Instantiate the single Facade entry point[cite: 1, 7]
        FlightBookingFacade bookingFacade = new FlightBookingFacade();

        // TEST 1: Standard valid booking using the Phase 1 (8-argument) method[cite: 7, 8]
        // Expected: Locks 14A, calculates baggage fee, converts USD -> EUR, charges card, issues ticket[cite: 2, 3, 4, 7].
        System.out.println("=== TEST 1: Standard Successful Booking ===");
        String ticket1 = bookingFacade.bookFlight(
                "Amina Suleimenova",
                "KC101",
                "14A",
                320.00,
                2,
                23.0,
                "EUR",
                "4532015112830366"
        );
        System.out.println("Result 1: " + ticket1 + "\n");

        // TEST 2: Attempting to book a blocked crew seat in Row 13[cite: 2, 7]
        // Expected: Fails immediately at lockSeat() before any lock is acquired, then disconnects cleanly[cite: 2, 5, 7].
        System.out.println("=== TEST 2: Blocked Crew Seat (Row 13) ===");
        String ticket2 = bookingFacade.bookFlight(
                "Daniyar Kassymov",
                "KC101",
                "13C",
                320.00,
                1,
                15.0,
                "USD",
                "4532015112830366"
        );
        System.out.println("Result 2: " + ticket2 + "\n");

        // TEST 3: Rollback verification when payment fails mid-transaction[cite: 6, 7]
        // Expected: Locks seat 07D, fails at chargeCard() due to short card number "9999",
        // releases LOCK-KC101-07D in the catch block, and disconnects[cite: 2, 3, 5, 7, 8].
        System.out.println("=== TEST 3: Payment Failure & Automatic Seat Rollback ===");
        String ticket3 = bookingFacade.bookFlight(
                "Timur Bekov",
                "KC101",
                "07D",
                320.00,
                1,
                18.0,
                "KZT",
                "9999"
        );
        System.out.println("Result 3: " + ticket3 + "\n");

        // TEST 4: Phase 2 loyalty tier test using the overloaded 9-argument method[cite: 8]
        // Expected: Skips BaggagePricingEngine (no baggage fee or log), applies "SILVER-FREE-BAG"
        // for a 10% discount, charges $360.00 USD, and issues ticket[cite: 3, 4, 8, 9].
        System.out.println("=== TEST 4: Phase 2 - SILVER Loyalty Tier ===");
        String ticket4 = bookingFacade.bookFlight(
                "Alina Nurlanova",
                "KZ204",
                "04B",
                400.00,
                3,
                30.0,
                "USD",
                "4532015112830366",
                "SILVER"
        );
        System.out.println("Result 4: " + ticket4 + "\n");
    }
}