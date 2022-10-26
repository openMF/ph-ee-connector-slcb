Feature: SLCB integration test
  Scenario: Test the payload of the SLCB
    Given I have a batchId: "123-123-123", requestId: "3af-567-dfr", purpose: "integration test"
    And I mock transactionList with two transactions each of "1" value
    And I can start camel context
    When I call the buildPayload route
    Then the exchange should have a variable with SLCB payload
    And I can parse SLCB payload to DTO
    And total transaction amount is 2
    And total transaction count is 2, failed is 0 and completed is 0
