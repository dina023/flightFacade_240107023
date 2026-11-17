/**
 * FlightBookingFacade implements the Facade Design Pattern[cite: 1, 5].
 * It hides the complexity and strict execution order of five underlying
 * airline subsystems behind a single, clean method for the client[cite: 1, 5].
 */
public class FlightBookingFacade {

    // Private references to all five subsystems (encapsulated from the client)[cite: 5, 6]
    private final SeatInventorySystem seatSystem;
    private final BaggagePricingEngine baggageEngine;
    private final CurrencyConverter currencyConverter;
    private final PaymentProcessor paymentProcessor;
    private final TicketIssuer ticketIssuer;

    // No-argument constructor that initializes all five subsystem objects[cite: 5, 6]
    public FlightBookingFacade() {
        this.seatSystem = new SeatInventorySystem();
        this.baggageEngine = new BaggagePricingEngine();
        this.currencyConverter = new CurrencyConverter();
        this.paymentProcessor = new PaymentProcessor();
        this.ticketIssuer = new TicketIssuer();
    }

    /**
     * Phase 1 Overloaded Method (8 parameters):
     * Preserves backward compatibility for older client calls by delegating
     * directly to the 9-parameter method with a default "STANDARD" loyalty tier[cite: 8].
     */
    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber) {
        return bookFlight(passengerName, flightCode, seatNumber, baseFareUSD,
                bagCount, bagWeightKg, currency, cardNumber, "STANDARD");[cite: 8]
    }

    /**
     * Phase 2 Main Orchestration Method (9 parameters):
     * Coordinates seat locking, loyalty-based baggage pricing, currency conversion,
     * promo code application, payment charging, rollback on failure, and ticket issuance[cite: 5, 6, 8].
     */
    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber,
                             String loyaltyTier) {

        // Declared outside the try block so the catch block can release the seat if a later step fails[cite: 4, 5]
        String lockToken = null;

        // Normalize the loyalty tier string (defaults to "STANDARD" if null)
        String tier = (loyaltyTier != null) ? loyaltyTier.trim().toUpperCase() : "STANDARD";

        try {
            // Step 1: Connect to the seat server and lock the requested seat[cite: 1, 5]
            // Note: Calling lockSeat() before connectToReservationServer() would throw an IllegalStateException[cite: 1, 4].
            seatSystem.connectToReservationServer();
            lockToken = seatSystem.lockSeat(flightCode, seatNumber, passengerName);

            // Step 2: Calculate baggage fees in USD (Phase 2 Loyalty Rule)[cite: 5, 8]
            // SILVER and GOLD tiers are exempt from baggage fees, so we skip BaggagePricingEngine completely[cite: 8].
            double baggageFeeUSD = 0.0;
            if (!"SILVER".equals(tier) && !"GOLD".equals(tier)) {
                baggageFeeUSD = baggageEngine.calculateBaggageFeeUSD(bagCount, bagWeightKg);
            }
            double totalUSD = baseFareUSD + baggageFeeUSD;

            // Step 3: Convert the total USD cost into the passenger's requested billing currency[cite: 3, 5]
            double convertedTotal = currencyConverter.convertFromUSD(totalUSD, currency);

            // Step 4: Initialize the payment gateway, apply the SILVER promo if eligible, and charge the card[cite: 3, 5, 8]
            // Note: The gateway must be initialized before applying promo codes or charging the card[cite: 3, 4].
            paymentProcessor.initializeGateway();
            if ("SILVER".equals(tier)) {
                paymentProcessor.applyPromoCode("SILVER-FREE-BAG");[cite: 8]
            }
            String receiptId = paymentProcessor.chargeCard(cardNumber, convertedTotal, currency);

            // Step 5: Issue and return the final e-ticket confirmation code[cite: 4, 5]
            return ticketIssuer.generateTicket(passengerName, flightCode, lockToken, receiptId);

        } catch (RuntimeException ex) {
            // Step 6 (Rollback): If a seat was already locked before an error occurred (e.g., invalid card),
            // release the lock so the seat is not left stranded[cite: 4, 5].
            if (lockToken != null) {
                seatSystem.releaseSeatLock(lockToken);
            }
            System.out.println("[Facade] Booking aborted due to error: " + ex.getMessage());
            return "BOOKING_FAILED: " + ex.getMessage();

        } finally {
            // Step 7 (Cleanup): Always disconnect from the seat reservation server,
            // whether the booking succeeded or failed[cite: 4, 5].
            seatSystem.disconnect();
        }
    }
}