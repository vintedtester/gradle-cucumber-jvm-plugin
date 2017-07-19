@test
Feature: Ignored tests should not run

  @ignore
  Scenario: Failing Scenario
    When I do nothing
    Then I fail my assertion
