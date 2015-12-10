Feature: Test Feature with broken background step

  Background: This will throw an exception
    When I throw an exception

  Scenario: This will pass if reached
    When I do nothing
    Then I pass my assertion