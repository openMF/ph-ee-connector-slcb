Feature: SLCB integration test
  Scenario: Test the payload of the SLCB
    Given I have a batchId: "123-123-123", requestId: "3af-567-dfr", purpose: "integration test"
    And I mock transactionList with two transactions each of "1" value
    And camel context is not null
    When I call the buildPayload route
    Then the exchange should have a variable with SLCB payload
    And I can parse SLCB payload to DTO
    And total transaction amount is 2
    And total transaction count is 2, failed is 0 and completed is 0


  Scenario: Test the auth flow
    Given I can mock external API response with token: "123-123-123" and expiry: 2000
    When I call get-token route
    Then the exchange should have a variable with token "123-123-123"
